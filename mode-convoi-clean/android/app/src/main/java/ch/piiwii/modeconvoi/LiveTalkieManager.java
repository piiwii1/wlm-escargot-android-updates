package ch.piiwii.modeconvoi;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Audio-only WebRTC mesh for Mode Convoi.
 * WordPress is used only for signalling. Once ICE/DTLS is connected, audio is peer-to-peer.
 */
public final class LiveTalkieManager {
    public interface Listener {
        void onLiveTalkieState(String label, int connectedPeers, int totalPeers, boolean transmitting);
    }

    private static final Object INIT_LOCK = new Object();
    private static boolean WEBRTC_INITIALIZED = false;

    private final Context context;
    private final AppPrefs prefs;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ScheduledThreadPoolExecutor net = new ScheduledThreadPoolExecutor(3);
    private final Map<String, PeerState> peers = new HashMap<>();
    private final Map<String, Long> lastRepairByPeer = new HashMap<>();
    private final Object peerLock = new Object();

    private PeerConnectionFactory factory;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private AudioDeviceModule audioDeviceModule;
    private AudioManager audioManager;
    // Audio routing is owned only while Mode Convoi is actually playing/recording talkie audio.
    // Keeping MODE_IN_COMMUNICATION active while the app is in the background hijacks Android's
    // volume keys/mixer (for example while watching TikTok), so the previous route is captured
    // immediately before use and restored as soon as the talkie becomes idle/backgrounded.
    private int previousAudioMode = AudioManager.MODE_NORMAL;
    private boolean previousSpeakerphoneOn = false;
    private AudioDeviceInfo previousCommunicationDevice = null;
    private volatile boolean audioRouteOwned = false;
    private volatile boolean hostForeground = true;

    private volatile boolean running = false;
    private volatile boolean transmitting = false;
    private volatile boolean receiveEnabled = true;
    private volatile long lastSignalId = 0;
    private volatile String sessionKey = "";
    private volatile boolean pollScheduled = false;
    private volatile String lastError = "";
    private volatile boolean receivingAudio = false;
    private volatile String receivingPeerName = "";
    private volatile double receiveLevel = 0.0;
    private volatile double micLevel = 0.0;
    private volatile boolean meterScheduled = false;
    private volatile long transmittingSince = 0L;

    public LiveTalkieManager(Context context, AppPrefs prefs, Listener listener) {
        this.context = context.getApplicationContext();
        this.prefs = prefs;
        this.listener = listener;
        this.receiveEnabled = prefs.getBool("talkieReceive", true);
        net.setRemoveOnCancelPolicy(true);
    }

    public void ensureStarted() {
        if (!prefs.hasActiveConvoy()) {
            stopSession();
            return;
        }
        String key = prefs.get("code", "") + ":" + prefs.get("participantId", "");
        if (running && key.equals(sessionKey)) return;
        stopSession();
        try {
            initWebRtc();
            sessionKey = key;
            lastSignalId = prefs.getLong("liveSignalLastId", 0);
            running = true;
            lastError = "";
            scheduleSignalPoll(0);
            scheduleAudioMeter(250);
            notifyState("Connexion live…");
        } catch (Throwable t) {
            lastError = safeMessage(t, "Initialisation WebRTC impossible");
            notifyState("Live indisponible : " + lastError);
        }
    }

    public boolean isRunning() { return running; }
    public boolean isTransmitting() { return transmitting; }

    /**
     * The WebRTC signalling/peer mesh may stay alive in background so we can still identify
     * who is speaking, but background Mode Convoi must never keep Android's communication
     * audio route or volume stream captive.
     */
    public void setHostForeground(boolean foreground) {
        hostForeground = foreground;
        if (!foreground) {
            transmitting = false;
            transmittingSince = 0L;
            micLevel = 0.0;
            try { if (localAudioTrack != null) localAudioTrack.setEnabled(false); } catch (Throwable ignored) {}
            muteTalkieOutputAndReleaseRoute();
        } else if (receivingAudio && receiveEnabled) {
            ensureReceivePath();
        } else {
            muteTalkieOutputAndReleaseRoute();
        }
        notifyState(foreground ? stateLabel() : "● Live en arrière-plan");
    }

    public int connectedPeerCount() {
        synchronized (peerLock) {
            int n = 0;
            for (PeerState p : peers.values()) if (p.connected) n++;
            return n;
        }
    }

    public int totalPeerCount() {
        synchronized (peerLock) { return peers.size(); }
    }

    public void setReceiveEnabled(boolean enabled) {
        receiveEnabled = enabled;
        prefs.putBool("talkieReceive", enabled);
        synchronized (peerLock) {
            for (PeerState p : peers.values()) {
                try { if (p.remoteTrack != null) p.remoteTrack.setEnabled(enabled); } catch (Throwable ignored) {}
            }
        }
        if (!enabled) {
            receivingAudio = false;
            receivingPeerName = "";
            receiveLevel = 0.0;
            muteTalkieOutputAndReleaseRoute();
        } else if (hostForeground && receivingAudio) {
            ensureReceivePath();
        } else {
            // Keep RTP alive for speaker detection, but do not own Android audio while idle/background.
            muteTalkieOutputAndReleaseRoute();
        }
        notifyState(enabled ? stateLabel() : "Réception live coupée");
    }

    /** Enables/disables the already-established live microphone track. No file is created. */
    public boolean setTransmitting(boolean enabled) {
        ensureStarted();
        if (!running || localAudioTrack == null) return false;
        if (enabled && !hostForeground) return false;
        transmitting = enabled;
        transmittingSince = enabled ? System.currentTimeMillis() : 0L;
        try {
            if (enabled) {
                activateTalkieAudioRoute();
                try { if (audioDeviceModule != null) audioDeviceModule.setSpeakerMute(true); } catch (Throwable ignored) {}
            }
            localAudioTrack.setEnabled(enabled);
            ensureLocalSendersAttached();
            if (enabled) prepareTransmitHealthCheck();
            else if (hostForeground && receiveEnabled && receivingAudio) ensureReceivePath();
            else muteTalkieOutputAndReleaseRoute();
        } catch (Throwable t) {
            lastError = safeMessage(t, "Micro WebRTC indisponible");
            transmitting = false;
            muteTalkieOutputAndReleaseRoute();
            notifyState("Micro live indisponible");
            return false;
        }
        if (!enabled) micLevel = 0.0;
        if (enabled) notifyState(connectedPeerCount() > 0 ? "● EN DIRECT" : "● EN DIRECT · connexion aux autres…");
        else notifyState(stateLabel());
        return true;
    }

    public String stateLabel() {
        if (!lastError.isEmpty() && connectedPeerCount() == 0) return "⚠ " + lastError;
        int connected = connectedPeerCount(), total = totalPeerCount();
        if (total == 0) return "● Live prêt · seul dans le convoi";
        if (connected == total) return "● Live prêt · " + connected + " connecté" + (connected > 1 ? "s" : "");
        if (connected > 0) return "● Live · " + connected + "/" + total + " connectés";
        return "◌ Connexion live…";
    }

    public void close() {
        stopSession();
        net.shutdownNow();
    }

    private void initWebRtc() {
        synchronized (INIT_LOCK) {
            if (!WEBRTC_INITIALIZED) {
                PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(context)
                                .setEnableInternalTracer(false)
                                .createInitializationOptions());
                WEBRTC_INITIALIZED = true;
            }
        }
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // Do not switch Android to MODE_IN_COMMUNICATION merely because a convoy session exists.
        // Routing is activated lazily only for actual foreground talkie audio.
        audioDeviceModule = JavaAudioDeviceModule.builder(context)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule();
        factory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory();
        audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("mc_live_audio", audioSource);
        localAudioTrack.setEnabled(false);
        // The WebRTC track itself controls push-to-talk. Keeping the ADM microphone
        // hard-muted can leave some Android audio stacks in a one-way state after
        // negotiation, so we do not use that second mute layer here.
        // Start muted. Incoming RTP is still measured; output is unmuted only when foreground audio exists.
        audioDeviceModule.setSpeakerMute(true);
    }

    private void stopSession() {
        running = false;
        pollScheduled = false;
        meterScheduled = false;
        transmitting = false;
        transmittingSince = 0L;
        receivingAudio = false;
        receivingPeerName = "";
        receiveLevel = 0.0;
        micLevel = 0.0;
        synchronized (peerLock) {
            for (PeerState p : peers.values()) closePeerInternal(p);
            peers.clear();
            lastRepairByPeer.clear();
        }
        try { if (audioDeviceModule != null) audioDeviceModule.setSpeakerMute(true); } catch (Throwable ignored) {}
        restoreSystemAudioRoute();
        try { if (localAudioTrack != null) localAudioTrack.dispose(); } catch (Throwable ignored) {}
        try { if (audioSource != null) audioSource.dispose(); } catch (Throwable ignored) {}
        try { if (factory != null) factory.dispose(); } catch (Throwable ignored) {}
        try { if (audioDeviceModule != null) audioDeviceModule.release(); } catch (Throwable ignored) {}
        localAudioTrack = null;
        audioSource = null;
        factory = null;
        audioDeviceModule = null;
        sessionKey = "";
        lastSignalId = 0;
    }

    private synchronized void activateTalkieAudioRoute() {
        if (audioManager == null || !hostForeground) return;
        if (!audioRouteOwned) {
            try { previousAudioMode = audioManager.getMode(); } catch (Throwable ignored) { previousAudioMode = AudioManager.MODE_NORMAL; }
            try { previousSpeakerphoneOn = audioManager.isSpeakerphoneOn(); } catch (Throwable ignored) { previousSpeakerphoneOn = false; }
            previousCommunicationDevice = null;
            if (Build.VERSION.SDK_INT >= 31) {
                try { previousCommunicationDevice = audioManager.getCommunicationDevice(); } catch (Throwable ignored) {}
            }
            audioRouteOwned = true;
        }
        try { audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION); } catch (Throwable ignored) {}
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                AudioDeviceInfo speaker = null;
                for (AudioDeviceInfo d : audioManager.getAvailableCommunicationDevices()) {
                    if (d != null && d.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) { speaker = d; break; }
                }
                if (speaker != null) audioManager.setCommunicationDevice(speaker);
            } catch (Throwable ignored) {}
        }
        try { audioManager.setSpeakerphoneOn(true); } catch (Throwable ignored) {}
        // Never change STREAM_VOICE_CALL volume here: volume belongs to the user/system mixer.
    }

    private synchronized void restoreSystemAudioRoute() {
        if (audioManager == null || !audioRouteOwned) return;
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                if (previousCommunicationDevice != null) audioManager.setCommunicationDevice(previousCommunicationDevice);
                else audioManager.clearCommunicationDevice();
            } catch (Throwable ignored) {}
        }
        try { audioManager.setSpeakerphoneOn(previousSpeakerphoneOn); } catch (Throwable ignored) {}
        try { audioManager.setMode(previousAudioMode); } catch (Throwable ignored) {}
        previousCommunicationDevice = null;
        audioRouteOwned = false;
    }

    private void muteTalkieOutputAndReleaseRoute() {
        try { if (audioDeviceModule != null) audioDeviceModule.setSpeakerMute(true); } catch (Throwable ignored) {}
        restoreSystemAudioRoute();
    }

    private void scheduleAudioMeter(long delayMs) {
        if (!running || meterScheduled || net.isShutdown()) return;
        meterScheduled = true;
        net.schedule(() -> {
            meterScheduled = false;
            pollAudioMeters();
        }, Math.max(0, delayMs), TimeUnit.MILLISECONDS);
    }

    private void pollAudioMeters() {
        if (!running) return;
        List<PeerState> list = new ArrayList<>();
        synchronized (peerLock) {
            for (PeerState p : peers.values()) if (p.pc != null && p.connected) list.add(p);
        }
        if (list.isEmpty()) {
            receivingAudio = false;
            receivingPeerName = "";
            receiveLevel = 0.0;
            micLevel = 0.0;
            if (!transmitting) muteTalkieOutputAndReleaseRoute();
            notifyState(stateLabel());
            scheduleAudioMeter(350);
            return;
        }
        final AtomicInteger pending = new AtomicInteger(list.size());
        final Object meterLock = new Object();
        final double[] levels = new double[]{0.0, 0.0}; // inbound, microphone
        final PeerState[] loudest = new PeerState[]{null};
        for (PeerState p : list) {
            try {
                p.pc.getStats(report -> {
                    AudioStats one = audioStats(report);
                    evaluateMediaHealth(p, one);
                    synchronized (meterLock) {
                        if (one.inboundLevel > levels[0]) {
                            levels[0] = one.inboundLevel;
                            loudest[0] = p;
                        }
                        levels[1] = Math.max(levels[1], one.micLevel);
                    }
                    if (pending.decrementAndGet() == 0) {
                        receiveLevel = levels[0];
                        micLevel = levels[1];
                        boolean wasReceiving = receivingAudio;
                        receivingAudio = receiveEnabled && !transmitting && receiveLevel >= 0.012;
                        if (receivingAudio && loudest[0] != null) {
                            String n = loudest[0].displayName == null ? "" : loudest[0].displayName.trim();
                            receivingPeerName = n.isEmpty() ? "Un participant" : n;
                        } else receivingPeerName = "";
                        if (receivingAudio && hostForeground) ensureReceivePath();
                        else if (!transmitting && (wasReceiving || !hostForeground)) muteTalkieOutputAndReleaseRoute();
                        notifyState(stateLabel());
                        scheduleAudioMeter(180);
                    }
                });
            } catch (Throwable ignored) {
                if (pending.decrementAndGet() == 0) scheduleAudioMeter(250);
            }
        }
    }

    private AudioStats audioStats(RTCStatsReport report) {
        AudioStats result = new AudioStats();
        if (report == null || report.getStatsMap() == null) return result;
        for (RTCStats stat : report.getStatsMap().values()) {
            if (stat == null || stat.getMembers() == null) continue;
            Map<String,Object> m = stat.getMembers();
            String kind = String.valueOf(m.get("kind"));
            if ("null".equals(kind)) kind = String.valueOf(m.get("mediaType"));
            if (!"audio".equalsIgnoreCase(kind)) continue;
            Object raw = m.get("audioLevel");
            if (raw instanceof Number) {
                double level = Math.max(0.0, Math.min(1.0, ((Number) raw).doubleValue()));
                if ("inbound-rtp".equals(stat.getType())) result.inboundLevel = Math.max(result.inboundLevel, level);
                else if ("media-source".equals(stat.getType())) result.micLevel = Math.max(result.micLevel, level);
            }
            if ("inbound-rtp".equals(stat.getType())) result.inboundBytes = statLong(m.get("bytesReceived"));
            else if ("outbound-rtp".equals(stat.getType())) result.outboundBytes = statLong(m.get("bytesSent"));
        }
        return result;
    }

    private long statLong(Object value) {
        return value instanceof Number ? Math.max(0L, ((Number) value).longValue()) : -1L;
    }

    private void prepareTransmitHealthCheck() {
        long now = System.currentTimeMillis();
        synchronized (peerLock) {
            for (PeerState state : peers.values()) {
                if (state == null) continue;
                state.txStartedAt = now;
                state.txStartOutboundBytes = state.lastOutboundBytes;
                state.txProgress = false;
            }
        }
    }

    private void evaluateMediaHealth(PeerState state, AudioStats stats) {
        if (!isCurrent(state) || !state.connected || stats == null) return;
        long now = System.currentTimeMillis();
        boolean missingRemoteTrack;
        boolean stalledTransmit;
        synchronized (state) {
            if (stats.outboundBytes >= 0) {
                if (stats.outboundBytes > state.lastOutboundBytes) state.lastOutboundBytes = stats.outboundBytes;
                if (transmitting && state.txStartedAt > 0 && stats.outboundBytes > state.txStartOutboundBytes) state.txProgress = true;
            }
            if (stats.inboundBytes >= 0 && stats.inboundBytes > state.lastInboundBytes) state.lastInboundBytes = stats.inboundBytes;
            missingRemoteTrack = state.remoteTrack == null && state.connectedAt > 0 && now - state.connectedAt > 2500L;
            stalledTransmit = transmitting && state.txStartedAt > 0 && now - state.txStartedAt > 1800L
                    && stats.outboundBytes >= 0 && stats.micLevel >= 0.008 && !state.txProgress;
        }
        if (missingRemoteTrack) repairPeer(state, "Réception audio absente");
        else if (stalledTransmit) repairPeer(state, "Émission audio bloquée");
    }

    private void ensureReceivePath() {
        if (!receiveEnabled || !hostForeground) {
            muteTalkieOutputAndReleaseRoute();
            return;
        }
        activateTalkieAudioRoute();
        try { if (audioDeviceModule != null) audioDeviceModule.setSpeakerMute(false); } catch (Throwable ignored) {}
        synchronized (peerLock) {
            for (PeerState state : peers.values()) {
                try { if (state.remoteTrack != null) state.remoteTrack.setEnabled(true); } catch (Throwable ignored) {}
            }
        }
    }

    private String meter(double level) {
        if (level < 0.008) return "▁▁▁▁";
        if (level < 0.025) return "▂▁▁▁";
        if (level < 0.060) return "▂▄▁▁";
        if (level < 0.140) return "▂▄▆▁";
        return "▂▄▆█";
    }

    private void scheduleSignalPoll(long delayMs) {
        if (!running || pollScheduled || net.isShutdown()) return;
        pollScheduled = true;
        net.schedule(() -> {
            pollScheduled = false;
            pollSignals();
        }, Math.max(0, delayMs), TimeUnit.MILLISECONDS);
    }

    private void pollSignals() {
        if (!running || !prefs.hasActiveConvoy()) { stopSession(); return; }
        String base = prefs.get("serverUrl", "");
        String code = prefs.get("code", "");
        String me = prefs.get("participantId", "");
        String token = prefs.get("token", "");
        try {
            String path = "/api/convoys/" + code + "/signal?participantId=" + URLEncoder.encode(me, "UTF-8") +
                    "&token=" + URLEncoder.encode(token, "UTF-8") + "&after=" + lastSignalId;
            JSONObject r = ConvoyApi.get(base, path);
            JSONArray participants = r.optJSONArray("participants");
            syncPeersFromParticipants(participants, me);
            JSONArray messages = r.optJSONArray("messages");
            if (messages != null) {
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject m = messages.optJSONObject(i);
                    if (m != null) processSignal(m);
                }
            }
            long serverLast = r.optLong("lastId", lastSignalId);
            if (serverLast > lastSignalId) {
                lastSignalId = serverLast;
                prefs.putLong("liveSignalLastId", lastSignalId);
            }
            lastError = "";
            notifyState(stateLabel());
            scheduleSignalPoll(allPeersConnected() ? 1400 : 550);
        } catch (Throwable t) {
            lastError = safeMessage(t, "Signalisation live indisponible");
            notifyState("⚠ " + lastError);
            scheduleSignalPoll(2500);
        }
    }

    private boolean allPeersConnected() {
        synchronized (peerLock) {
            if (peers.isEmpty()) return true;
            for (PeerState p : peers.values()) if (!p.connected) return false;
            return true;
        }
    }

    private void syncPeersFromParticipants(JSONArray participants, String me) {
        if (participants == null) return;
        Set<String> seen = new HashSet<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < participants.length(); i++) {
            JSONObject p = participants.optJSONObject(i);
            if (p == null) continue;
            String id = p.optString("id", "");
            if (id.isEmpty() || id.equals(me)) continue;
            long lastSeen = p.optLong("lastSeen", now);
            // Do not keep creating WebRTC sessions for phones that have been absent for several minutes.
            if (lastSeen > 0 && now - lastSeen > 180_000L) continue;
            seen.add(id);
            String displayName = p.optString("name", "").trim();
            PeerState ps;
            synchronized (peerLock) { ps = peers.get(id); }
            if (ps == null) ps = createPeer(id);
            if (ps != null && !displayName.isEmpty()) ps.displayName = displayName;
        }
        List<String> remove = new ArrayList<>();
        synchronized (peerLock) {
            for (String id : peers.keySet()) if (!seen.contains(id)) remove.add(id);
        }
        for (String id : remove) removePeer(id);
    }

    private PeerState createPeer(String peerId) {
        if (!running || factory == null || localAudioTrack == null) return null;
        synchronized (peerLock) {
            PeerState existing = peers.get(peerId);
            if (existing != null) return existing;
        }
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer());
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        config.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        config.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;

        PeerState state = new PeerState(peerId);
        PeerConnection pc = factory.createPeerConnection(config, new PeerObserver(state));
        if (pc == null) {
            lastError = "Création de la liaison audio impossible";
            notifyState(lastError);
            return null;
        }
        state.pc = pc;
        String me = prefs.get("participantId", "");
        state.initiator = !me.isEmpty() && me.compareTo(peerId) < 0;

        // Unified Plan rule: only the deterministic offerer creates the local
        // audio transceiver up front. The answerer waits for the remote offer
        // and then attaches its microphone to the transceiver created by that
        // offer. Pre-creating one on both sides can produce two mismatched audio
        // m-lines and a connection that is technically CONNECTED but one-way.
        if (state.initiator) addOffererAudioTransceiver(state);

        synchronized (peerLock) { peers.put(peerId, state); }

        if (state.initiator) {
            net.schedule(() -> createOffer(state), 120, TimeUnit.MILLISECONDS);
        } else {
            net.schedule(() -> requestOfferIfNeeded(state), 1400, TimeUnit.MILLISECONDS);
            net.schedule(() -> requestOfferIfNeeded(state), 4200, TimeUnit.MILLISECONDS);
        }
        notifyState(stateLabel());
        return state;
    }

    private void addOffererAudioTransceiver(PeerState state) {
        if (state == null || state.pc == null || localAudioTrack == null) return;
        try {
            state.pc.addTransceiver(localAudioTrack, new RtpTransceiver.RtpTransceiverInit(
                    RtpTransceiver.RtpTransceiverDirection.SEND_RECV, Collections.singletonList("modeconvoi")));
        } catch (Throwable first) {
            try { state.pc.addTrack(localAudioTrack, Collections.singletonList("modeconvoi")); }
            catch (Throwable ignored) {}
        }
    }

    /**
     * Attach our microphone to the audio transceiver negotiated by SDP. This is
     * especially important on the answerer: after setRemoteDescription() the
     * remote offer owns the m-line, so we reuse that exact transceiver instead
     * of creating another one.
     */
    private boolean bindLocalTrackToNegotiatedAudio(PeerState state) {
        if (state == null || state.pc == null || localAudioTrack == null) return false;
        boolean bound = false;
        try {
            for (RtpTransceiver tr : state.pc.getTransceivers()) {
                if (tr == null || tr.isStopped()) continue;
                boolean audio = false;
                try {
                    if (tr.getReceiver() != null && tr.getReceiver().track() instanceof AudioTrack) audio = true;
                } catch (Throwable ignored) {}
                try {
                    if (tr.getSender() != null && tr.getSender().track() instanceof AudioTrack) audio = true;
                } catch (Throwable ignored) {}
                if (!audio) continue;
                try { tr.setDirection(RtpTransceiver.RtpTransceiverDirection.SEND_RECV); } catch (Throwable ignored) {}
                try {
                    if (tr.getSender() != null) {
                        if (tr.getSender().track() == localAudioTrack) bound = true;
                        else if (tr.getSender().setTrack(localAudioTrack, false)) bound = true;
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        state.localTrackBound = bound;
        return bound;
    }

    private void ensureLocalSendersAttached() {
        List<PeerState> list = new ArrayList<>();
        synchronized (peerLock) { list.addAll(peers.values()); }
        for (PeerState p : list) {
            if (p == null || p.pc == null) continue;
            boolean bound = bindLocalTrackToNegotiatedAudio(p);
            if (!bound && p.initiator && p.pc.getLocalDescription() == null) {
                addOffererAudioTransceiver(p);
                bound = bindLocalTrackToNegotiatedAudio(p);
            }
            if (!bound && p.connected) repairPeer(p, "Émetteur audio non attaché");
        }
    }

    private void requestOfferIfNeeded(PeerState state) {
        if (!isCurrent(state) || state.connected || state.initiator) return;
        sendSignal(state.peerId, "need-offer", "");
    }

    private void createOffer(PeerState state) {
        if (!isCurrent(state) || state.offerSent || state.connected) return;
        state.offerSent = true;
        MediaConstraints c = new MediaConstraints();
        state.pc.createOffer(new SimpleSdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sdp) {
                if (!isCurrent(state)) return;
                state.pc.setLocalDescription(new SimpleSdpObserver() {
                    @Override public void onSetSuccess() {
                        sendSignal(state.peerId, "offer", sdp.description);
                    }
                    @Override public void onSetFailure(String error) { failPeer(state, "Offre locale : " + error); }
                }, sdp);
            }
            @Override public void onCreateFailure(String error) { failPeer(state, "Offre : " + error); }
        }, c);
    }

    private void processSignal(JSONObject m) {
        String from = m.optString("from", "");
        String type = m.optString("type", "");
        String payload = m.optString("payload", "");
        if (from.isEmpty() || from.equals(prefs.get("participantId", ""))) return;
        PeerState state;
        synchronized (peerLock) { state = peers.get(from); }
        if (state == null) state = createPeer(from);
        if (state == null || !isCurrent(state)) return;

        if ("offer".equals(type)) {
            SessionDescription remote = new SessionDescription(SessionDescription.Type.OFFER, payload);
            PeerState finalState = state;
            state.pc.setRemoteDescription(new SimpleSdpObserver() {
                @Override public void onSetSuccess() {
                    finalState.remoteDescriptionSet = true;
                    if (!bindLocalTrackToNegotiatedAudio(finalState)) {
                        failPeer(finalState, "Piste audio distante sans émetteur local");
                        return;
                    }
                    flushPendingIce(finalState);
                    finalState.pc.createAnswer(new SimpleSdpObserver() {
                        @Override public void onCreateSuccess(SessionDescription answer) {
                            finalState.pc.setLocalDescription(new SimpleSdpObserver() {
                                @Override public void onSetSuccess() { sendSignal(finalState.peerId, "answer", answer.description); }
                                @Override public void onSetFailure(String error) { failPeer(finalState, "Réponse locale : " + error); }
                            }, answer);
                        }
                        @Override public void onCreateFailure(String error) { failPeer(finalState, "Réponse : " + error); }
                    }, new MediaConstraints());
                }
                @Override public void onSetFailure(String error) { failPeer(finalState, "Offre distante : " + error); }
            }, remote);
        } else if ("answer".equals(type)) {
            SessionDescription remote = new SessionDescription(SessionDescription.Type.ANSWER, payload);
            PeerState finalState = state;
            state.pc.setRemoteDescription(new SimpleSdpObserver() {
                @Override public void onSetSuccess() {
                    finalState.remoteDescriptionSet = true;
                    bindLocalTrackToNegotiatedAudio(finalState);
                    flushPendingIce(finalState);
                }
                @Override public void onSetFailure(String error) { failPeer(finalState, "Réponse distante : " + error); }
            }, remote);
        } else if ("ice".equals(type)) {
            try {
                JSONObject j = new JSONObject(payload);
                IceCandidate candidate = new IceCandidate(j.optString("mid", null), j.optInt("line", 0), j.optString("candidate", ""));
                if (state.remoteDescriptionSet || state.pc.getRemoteDescription() != null) {
                    state.pc.addIceCandidate(candidate);
                } else {
                    synchronized (state.pendingIce) { state.pendingIce.add(candidate); }
                }
            } catch (Throwable ignored) {}
        } else if ("need-offer".equals(type)) {
            if (!state.initiator) return;
            if (state.connected) {
                reconnectPeer(state, 120);
            } else if (state.offerSent && state.pc != null && state.pc.getLocalDescription() != null) {
                sendSignal(state.peerId, "offer", state.pc.getLocalDescription().description);
            } else {
                createOffer(state);
            }
        } else if ("repair".equals(type)) {
            notifyState("◌ Réparation audio…");
            reconnectPeer(state, 180);
        } else if ("hangup".equals(type)) {
            reconnectPeer(state, 250);
        }
    }

    private void flushPendingIce(PeerState state) {
        synchronized (state.pendingIce) {
            for (IceCandidate c : state.pendingIce) {
                try { state.pc.addIceCandidate(c); } catch (Throwable ignored) {}
            }
            state.pendingIce.clear();
        }
    }

    private void sendSignal(String to, String type, String payload) {
        if (!running || !prefs.hasActiveConvoy()) return;
        net.execute(() -> {
            try {
                JSONObject b = new JSONObject();
                b.put("participantId", prefs.get("participantId", ""));
                b.put("token", prefs.get("token", ""));
                b.put("to", to);
                b.put("type", type);
                b.put("payload", payload == null ? "" : payload);
                ConvoyApi.post(prefs.get("serverUrl", ""), "/api/convoys/" + prefs.get("code", "") + "/signal", b, null);
            } catch (Throwable t) {
                lastError = safeMessage(t, "Signal live non envoyé");
                notifyState("⚠ " + lastError);
            }
        });
    }

    private void removePeer(String id) {
        PeerState state;
        synchronized (peerLock) { state = peers.remove(id); }
        if (state != null) closePeerInternal(state);
        notifyState(stateLabel());
    }

    private void reconnectPeer(PeerState old, long delayMs) {
        if (!isCurrent(old)) return;
        String id = old.peerId;
        synchronized (peerLock) {
            PeerState current = peers.get(id);
            if (current != old) return;
            peers.remove(id);
        }
        closePeerInternal(old);
        if (running) net.schedule(() -> createPeer(id), Math.max(100, delayMs), TimeUnit.MILLISECONDS);
    }

    private void repairPeer(PeerState state, String reason) {
        if (!isCurrent(state)) return;
        long now = System.currentTimeMillis();
        boolean notifyRemote = false;
        synchronized (peerLock) {
            long last = lastRepairByPeer.containsKey(state.peerId) ? lastRepairByPeer.get(state.peerId) : 0L;
            if (now - last >= 4500L) {
                lastRepairByPeer.put(state.peerId, now);
                notifyRemote = true;
            }
        }
        lastError = "";
        notifyState("◌ Réparation audio…");
        if (notifyRemote) sendSignal(state.peerId, "repair", reason == null ? "" : reason);
        reconnectPeer(state, 220);
    }

    private void failPeer(PeerState state, String message) {
        lastError = message;
        notifyState("⚠ " + message);
        repairPeer(state, message);
    }

    private boolean isCurrent(PeerState state) {
        if (state == null || !running) return false;
        synchronized (peerLock) { return peers.get(state.peerId) == state; }
    }

    private void closePeerInternal(PeerState p) {
        p.connected = false;
        try { if (p.pc != null) p.pc.close(); } catch (Throwable ignored) {}
        try { if (p.pc != null) p.pc.dispose(); } catch (Throwable ignored) {}
        p.pc = null;
    }

    private void notifyState(String label) {
        if (listener == null) return;
        int connected = connectedPeerCount(), total = totalPeerCount();
        boolean tx = transmitting;
        String shown = label;
        if (tx) shown = "🎙️ EN DIRECT  " + meter(micLevel);
        else if (receivingAudio) shown = "🔊 " + (receivingPeerName.isEmpty()?"Un participant":receivingPeerName) + " parle  " + meter(receiveLevel);
        final String finalLabel = shown;
        main.post(() -> listener.onLiveTalkieState(finalLabel, connected, total, tx));
    }

    private String safeMessage(Throwable t, String fallback) {
        String m = t == null ? null : t.getMessage();
        return m == null || m.trim().isEmpty() ? fallback : m.trim();
    }

    private void configureRemoteTrack(PeerState state, AudioTrack track) {
        if (track == null) return;
        try { track.setEnabled(receiveEnabled); } catch (Throwable ignored) {}
        try { track.setVolume(2.5); } catch (Throwable ignored) {}
        state.remoteTrack = track;
        state.remoteTrackAt = System.currentTimeMillis();
        if (receiveEnabled) ensureReceivePath();
    }

    private final class PeerObserver implements PeerConnection.Observer {
        private final PeerState state;
        PeerObserver(PeerState state) { this.state = state; }
        @Override public void onSignalingChange(PeerConnection.SignalingState newState) {}
        @Override public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {}
        @Override public void onIceConnectionReceivingChange(boolean receiving) {}
        @Override public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {}
        @Override public void onIceCandidate(IceCandidate candidate) {
            if (!isCurrent(state) || candidate == null) return;
            try {
                JSONObject j = new JSONObject();
                j.put("mid", candidate.sdpMid == null ? JSONObject.NULL : candidate.sdpMid);
                j.put("line", candidate.sdpMLineIndex);
                j.put("candidate", candidate.sdp);
                sendSignal(state.peerId, "ice", j.toString());
            } catch (Throwable ignored) {}
        }
        @Override public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
        @Override public void onAddStream(MediaStream stream) {
            if (stream != null && stream.audioTracks != null) for (AudioTrack t : stream.audioTracks) configureRemoteTrack(state, t);
        }
        @Override public void onRemoveStream(MediaStream stream) {}
        @Override public void onDataChannel(DataChannel dataChannel) {}
        @Override public void onRenegotiationNeeded() {}
        @Override public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
            if (receiver != null && receiver.track() instanceof AudioTrack) configureRemoteTrack(state, (AudioTrack) receiver.track());
        }
        @Override public void onTrack(RtpTransceiver transceiver) {
            try {
                if (transceiver != null && transceiver.getReceiver() != null && transceiver.getReceiver().track() instanceof AudioTrack)
                    configureRemoteTrack(state, (AudioTrack) transceiver.getReceiver().track());
            } catch (Throwable ignored) {}
        }
        @Override public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
            if (!isCurrent(state)) return;
            if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                state.connected = true;
                state.connectedAt = System.currentTimeMillis();
                state.txStartedAt = transmitting ? state.connectedAt : 0L;
                state.txStartOutboundBytes = state.lastOutboundBytes;
                state.txProgress = false;
                boolean bound = bindLocalTrackToNegotiatedAudio(state);
                lastError = "";
                ensureReceivePath();
                if (!bound) net.schedule(() -> { if (isCurrent(state) && state.connected && !bindLocalTrackToNegotiatedAudio(state)) repairPeer(state, "Émetteur audio absent après connexion"); }, 700, TimeUnit.MILLISECONDS);
                notifyState(stateLabel());
            } else if (newState == PeerConnection.PeerConnectionState.FAILED) {
                state.connected = false;
                notifyState("◌ Reconnexion live…");
                repairPeer(state, "Connexion WebRTC en échec");
            } else if (newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                state.connected = false;
                notifyState("◌ Liaison live interrompue…");
                net.schedule(() -> { if (isCurrent(state) && !state.connected) repairPeer(state, "Liaison WebRTC interrompue"); }, 4500, TimeUnit.MILLISECONDS);
            } else if (newState == PeerConnection.PeerConnectionState.CLOSED) {
                state.connected = false;
                notifyState(stateLabel());
            }
        }
    }

    private static class PeerState {
        final String peerId;
        PeerConnection pc;
        boolean offerSent = false;
        volatile boolean initiator = false;
        volatile boolean localTrackBound = false;
        volatile boolean connected = false;
        volatile String displayName = "";
        volatile boolean remoteDescriptionSet = false;
        volatile AudioTrack remoteTrack = null;
        volatile long connectedAt = 0L;
        volatile long remoteTrackAt = 0L;
        volatile long lastInboundBytes = 0L;
        volatile long lastOutboundBytes = 0L;
        volatile long txStartedAt = 0L;
        volatile long txStartOutboundBytes = 0L;
        volatile boolean txProgress = false;
        final List<IceCandidate> pendingIce = new ArrayList<>();
        PeerState(String peerId) { this.peerId = peerId; }
    }

    private static final class AudioStats {
        double inboundLevel = 0.0;
        double micLevel = 0.0;
        long inboundBytes = -1L;
        long outboundBytes = -1L;
    }

    private abstract static class SimpleSdpObserver implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sdp) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String error) {}
        @Override public void onSetFailure(String error) {}
    }
}
