from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "android/app/src/main/java/ch/piiwii/modeconvoi"
main_path = java / "MainActivity.java"
talkie_path = java / "LiveTalkieManager.java"
event_path = java / "ConvoyEventProcessor.java"
api_path = java / "ConvoyApi.java"
gradle_path = root / "android/app/build.gradle"
manifest_path = root / "android/app/src/main/AndroidManifest.xml"
alert_bus_path = java / "ConvoyForegroundAlertBus.java"
visual_path = java / "ConvoyVisualAlertController.java"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 anchor, found {count}")
    return text.replace(old, new, 1)


# --- MainActivity: foreground visual alert lifecycle ---
main = main_path.read_text()
main = replace_once(main,
    "    private ConvoyMapController mapController;\n    private ConvoyPositionResolver positionResolver;\n",
    "    private ConvoyMapController mapController;\n    private ConvoyPositionResolver positionResolver;\n    private ConvoyVisualAlertController visualAlertController;\n",
    "MainActivity alert controller field")
main = replace_once(main,
    "        mapController = new ConvoyMapController(prefs,()->snapshot);\n        positionResolver = new ConvoyPositionResolver(prefs);\n        pollingController = new ConvoyPollingController",
    "        mapController = new ConvoyMapController(prefs,()->snapshot);\n        positionResolver = new ConvoyPositionResolver(prefs);\n        visualAlertController = new ConvoyVisualAlertController(this);\n        pollingController = new ConvoyPollingController",
    "MainActivity alert controller init")
main = replace_once(main,
    "    @Override protected void onResume() { super.onResume(); if (prefs.hasActiveConvoy()) { startPolling(); startShareServiceIfPermitted(); if(liveTalkie!=null)liveTalkie.ensureStarted(); } }\n    @Override protected void onPause() { super.onPause(); stopPolling(); }\n    @Override protected void onDestroy() { if(mapController!=null)mapController.close(); if(pollingController!=null)pollingController.close(); if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }",
    "    @Override protected void onResume() { super.onResume(); if(visualAlertController!=null)ConvoyForegroundAlertBus.register(visualAlertController); if (prefs.hasActiveConvoy()) { startPolling(); startShareServiceIfPermitted(); if(liveTalkie!=null)liveTalkie.ensureStarted(); } }\n    @Override protected void onPause() { if(visualAlertController!=null)ConvoyForegroundAlertBus.unregister(visualAlertController); super.onPause(); stopPolling(); }\n    @Override protected void onDestroy() { if(visualAlertController!=null){ConvoyForegroundAlertBus.unregister(visualAlertController);visualAlertController.close();} if(mapController!=null)mapController.close(); if(pollingController!=null)pollingController.close(); if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }",
    "MainActivity lifecycle")
main = replace_once(main, "Mode Convoi 0.3.33", "Mode Convoi 0.3.34", "MainActivity about version")
main_path.write_text(main)


# --- Event processor: visual foreground delivery, notification fallback ---
event = event_path.read_text()
event = replace_once(event,
    "            if (NOTIFIED_TYPES.contains(event.optString(\"type\"))) {\n                NotificationHelper.notifyEvent(context, event);\n            }",
    "            if (NOTIFIED_TYPES.contains(event.optString(\"type\"))) {\n                boolean handledForeground = ConvoyForegroundAlertBus.dispatch(event);\n                if (!handledForeground) NotificationHelper.notifyEvent(context, event);\n            }",
    "ConvoyEventProcessor dispatch")
event_path.write_text(event)


# --- LiveTalkieManager: media-health detection + coordinated peer repair ---
talkie = talkie_path.read_text()
talkie = replace_once(talkie,
    "    private final Map<String, PeerState> peers = new HashMap<>();\n    private final Object peerLock = new Object();\n",
    "    private final Map<String, PeerState> peers = new HashMap<>();\n    private final Map<String, Long> lastRepairByPeer = new HashMap<>();\n    private final Object peerLock = new Object();\n",
    "LiveTalkie repair map")
talkie = replace_once(talkie,
    "    private volatile double micLevel = 0.0;\n    private volatile boolean meterScheduled = false;\n",
    "    private volatile double micLevel = 0.0;\n    private volatile boolean meterScheduled = false;\n    private volatile long transmittingSince = 0L;\n",
    "LiveTalkie transmitting timestamp")

talkie = replace_once(talkie,
    "        transmitting = enabled;\n        try {\n            routeAudioToSpeaker();\n            ensureLocalSendersAttached();\n            localAudioTrack.setEnabled(enabled);\n        } catch (Throwable t) {",
    "        transmitting = enabled;\n        transmittingSince = enabled ? System.currentTimeMillis() : 0L;\n        try {\n            routeAudioToSpeaker();\n            localAudioTrack.setEnabled(enabled);\n            ensureLocalSendersAttached();\n            if (enabled) prepareTransmitHealthCheck();\n            else ensureReceivePath();\n        } catch (Throwable t) {",
    "LiveTalkie setTransmitting")
talkie = replace_once(talkie,
    "        transmitting = false;\n        receivingAudio = false;",
    "        transmitting = false;\n        transmittingSince = 0L;\n        receivingAudio = false;",
    "LiveTalkie stop transmitting timestamp")
talkie = replace_once(talkie,
    "            for (PeerState p : peers.values()) closePeerInternal(p);\n            peers.clear();\n",
    "            for (PeerState p : peers.values()) closePeerInternal(p);\n            peers.clear();\n            lastRepairByPeer.clear();\n",
    "LiveTalkie repair map clear")

talkie = replace_once(talkie,
    "                    double[] one = audioLevels(report);\n                    synchronized (meterLock) {\n                        levels[0] = Math.max(levels[0], one[0]);\n                        levels[1] = Math.max(levels[1], one[1]);\n                    }",
    "                    AudioStats one = audioStats(report);\n                    evaluateMediaHealth(p, one);\n                    synchronized (meterLock) {\n                        levels[0] = Math.max(levels[0], one.inboundLevel);\n                        levels[1] = Math.max(levels[1], one.micLevel);\n                    }",
    "LiveTalkie meter stats callback")
talkie = replace_once(talkie,
    "                        receivingAudio = receiveEnabled && !transmitting && receiveLevel >= 0.012;\n                        notifyState(stateLabel());",
    "                        receivingAudio = receiveEnabled && !transmitting && receiveLevel >= 0.012;\n                        if (receivingAudio) ensureReceivePath();\n                        notifyState(stateLabel());",
    "LiveTalkie receive path health")

old_audio_levels = '''    private double[] audioLevels(RTCStatsReport report) {
        double inbound = 0.0, mic = 0.0;
        if (report == null || report.getStatsMap() == null) return new double[]{0.0, 0.0};
        for (RTCStats stat : report.getStatsMap().values()) {
            if (stat == null || stat.getMembers() == null) continue;
            Map<String,Object> m = stat.getMembers();
            String kind = String.valueOf(m.get("kind"));
            if ("null".equals(kind)) kind = String.valueOf(m.get("mediaType"));
            if (!"audio".equalsIgnoreCase(kind)) continue;
            Object raw = m.get("audioLevel");
            if (!(raw instanceof Number)) continue;
            double level = Math.max(0.0, Math.min(1.0, ((Number) raw).doubleValue()));
            if ("inbound-rtp".equals(stat.getType())) inbound = Math.max(inbound, level);
            else if ("media-source".equals(stat.getType())) mic = Math.max(mic, level);
        }
        return new double[]{inbound, mic};
    }
'''
new_audio_levels = '''    private AudioStats audioStats(RTCStatsReport report) {
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
        if (!receiveEnabled) return;
        try { if (audioDeviceModule != null) audioDeviceModule.setSpeakerMute(false); } catch (Throwable ignored) {}
        synchronized (peerLock) {
            for (PeerState state : peers.values()) {
                try { if (state.remoteTrack != null) state.remoteTrack.setEnabled(true); } catch (Throwable ignored) {}
            }
        }
        routeAudioToSpeaker();
    }
'''
talkie = replace_once(talkie, old_audio_levels, new_audio_levels, "LiveTalkie audio stats block")

talkie = replace_once(talkie,
    "        if (enabled) routeAudioToSpeaker();\n        else { receivingAudio = false; receiveLevel = 0.0; }",
    "        if (enabled) ensureReceivePath();\n        else { receivingAudio = false; receiveLevel = 0.0; }",
    "LiveTalkie receive enable")

talkie = replace_once(talkie,
    "        if (state.initiator) {\n            net.schedule(() -> createOffer(state), 120, TimeUnit.MILLISECONDS);\n        }\n        notifyState(stateLabel());",
    "        if (state.initiator) {\n            net.schedule(() -> createOffer(state), 120, TimeUnit.MILLISECONDS);\n        } else {\n            net.schedule(() -> requestOfferIfNeeded(state), 1400, TimeUnit.MILLISECONDS);\n            net.schedule(() -> requestOfferIfNeeded(state), 4200, TimeUnit.MILLISECONDS);\n        }\n        notifyState(stateLabel());",
    "LiveTalkie answerer offer requests")

ensure_old = '''    private void ensureLocalSendersAttached() {
        List<PeerState> list = new ArrayList<>();
        synchronized (peerLock) { list.addAll(peers.values()); }
        for (PeerState p : list) {
            if (p == null || p.pc == null) continue;
            boolean bound = bindLocalTrackToNegotiatedAudio(p);
            if (!bound && p.initiator && p.pc.getLocalDescription() == null) {
                addOffererAudioTransceiver(p);
                bindLocalTrackToNegotiatedAudio(p);
            }
        }
    }
'''
ensure_new = '''    private void ensureLocalSendersAttached() {
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
'''
talkie = replace_once(talkie, ensure_old, ensure_new, "LiveTalkie sender attachment")

signal_anchor = '''        } else if ("hangup".equals(type)) {
            reconnectPeer(state, 250);
        }
'''
signal_new = '''        } else if ("need-offer".equals(type)) {
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
'''
talkie = replace_once(talkie, signal_anchor, signal_new, "LiveTalkie signalling repair")

reconnect_anchor = '''    private void failPeer(PeerState state, String message) {
        lastError = message;
        notifyState("⚠ " + message);
        reconnectPeer(state, 1600);
    }
'''
reconnect_new = '''    private void repairPeer(PeerState state, String reason) {
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
'''
talkie = replace_once(talkie, reconnect_anchor, reconnect_new, "LiveTalkie repair method")

talkie = replace_once(talkie,
    "        state.remoteTrack = track;\n        if (receiveEnabled) routeAudioToSpeaker();",
    "        state.remoteTrack = track;\n        state.remoteTrackAt = System.currentTimeMillis();\n        if (receiveEnabled) ensureReceivePath();",
    "LiveTalkie remote track setup")

connection_anchor = '''            if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                state.connected = true;
                bindLocalTrackToNegotiatedAudio(state);
                lastError = "";
                routeAudioToSpeaker();
                notifyState(stateLabel());
            } else if (newState == PeerConnection.PeerConnectionState.FAILED) {
                state.connected = false;
                notifyState("◌ Reconnexion live…");
                reconnectPeer(state, 900);
            } else if (newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                state.connected = false;
                notifyState("◌ Liaison live interrompue…");
                net.schedule(() -> { if (isCurrent(state) && !state.connected) reconnectPeer(state, 100); }, 4500, TimeUnit.MILLISECONDS);
'''
connection_new = '''            if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
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
'''
talkie = replace_once(talkie, connection_anchor, connection_new, "LiveTalkie connection health")

peer_anchor = '''        volatile boolean connected = false;
        volatile boolean remoteDescriptionSet = false;
        volatile AudioTrack remoteTrack = null;
        final List<IceCandidate> pendingIce = new ArrayList<>();
        PeerState(String peerId) { this.peerId = peerId; }
    }
'''
peer_new = '''        volatile boolean connected = false;
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
'''
talkie = replace_once(talkie, peer_anchor, peer_new, "LiveTalkie PeerState media health")
talkie_path.write_text(talkie)


# --- Version / manifest ---
gradle = gradle_path.read_text()
gradle = replace_once(gradle, "versionCode 36", "versionCode 37", "Gradle versionCode")
gradle = replace_once(gradle, "versionName '0.3.33'", "versionName '0.3.34'", "Gradle versionName")
gradle_path.write_text(gradle)

api = api_path.read_text()
api = replace_once(api, "ModeConvoi-Android/0.3.33", "ModeConvoi-Android/0.3.34", "ConvoyApi User-Agent")
api_path.write_text(api)

manifest = manifest_path.read_text()
manifest = replace_once(manifest,
    '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n',
    '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n    <uses-permission android:name="android.permission.VIBRATE" />\n',
    "Manifest vibrate permission")
manifest_path.write_text(manifest)

# --- Post-conditions ---
main = main_path.read_text()
talkie = talkie_path.read_text()
event = event_path.read_text()
gradle = gradle_path.read_text()
api = api_path.read_text()
manifest = manifest_path.read_text()

checks = {
    "Main visual controller": "new ConvoyVisualAlertController(this)" in main,
    "Main foreground registration": "ConvoyForegroundAlertBus.register(visualAlertController)" in main,
    "Event foreground dispatch": "handledForeground = ConvoyForegroundAlertBus.dispatch(event)" in event,
    "Talkie request offer": 'sendSignal(state.peerId, "need-offer", "")' in talkie,
    "Talkie repair signal": 'sendSignal(state.peerId, "repair"' in talkie,
    "Talkie RTP health": "evaluateMediaHealth(p, one)" in talkie and "outboundBytes" in talkie,
    "Talkie receive repair": "Réception audio absente" in talkie,
    "Version code": "versionCode 37" in gradle,
    "Version name": "versionName '0.3.34'" in gradle,
    "User agent": "ModeConvoi-Android/0.3.34" in api,
    "About": "Mode Convoi 0.3.34" in main,
    "Vibrate permission": "android.permission.VIBRATE" in manifest,
    "Alert bus file": alert_bus_path.exists(),
    "Visual controller file": visual_path.exists(),
}
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("0.3.34 post-condition failed: " + ", ".join(failed))

print("Mode Convoi 0.3.34 guarded migration complete")
