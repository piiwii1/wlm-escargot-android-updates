from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "android/app/src/main/java/ch/piiwii/modeconvoi"
main_path = java / "MainActivity.java"
live_path = java / "LiveTalkieManager.java"
notif_path = java / "NotificationHelper.java"
api_path = java / "ConvoyApi.java"
gradle_path = root / "android/app/build.gradle"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 anchor, found {count}")
    return text.replace(old, new, 1)


# ---- MainActivity: driving layout + foreground speaker banner ----
main = main_path.read_text()
main = replace_once(
    main,
    '    private FrameLayout bottomNav;\n',
    '    private FrameLayout bottomNav, screenRoot;\n'
    '    private TextView talkieSpeakerBanner;\n'
    '    private boolean activityForeground=false;\n'
    '    private String activeTalkieSpeaker="";\n'
    '    private Runnable talkieSpeakerClearRunnable;\n',
    'main fields')

main = replace_once(
    main,
    '            }\n        });\n        ParticipantDefaults.ensure(prefs);',
    '            }\n            handleTalkieSpeakerState(label,receiving);\n        });\n        ParticipantDefaults.ensure(prefs);',
    'talkie callback hook')

main = replace_once(
    main,
    '    @Override protected void onResume() { super.onResume(); if(visualAlertController!=null)ConvoyForegroundAlertBus.register(visualAlertController); if (prefs.hasActiveConvoy()) { startPolling(); startShareServiceIfPermitted(); if(liveTalkie!=null)liveTalkie.ensureStarted(); } }\n'
    '    @Override protected void onPause() { if(visualAlertController!=null)ConvoyForegroundAlertBus.unregister(visualAlertController); super.onPause(); stopPolling(); }\n'
    '    @Override protected void onDestroy() { if(visualAlertController!=null){ConvoyForegroundAlertBus.unregister(visualAlertController);visualAlertController.close();} if(mapController!=null)mapController.close(); if(pollingController!=null)pollingController.close(); if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }',
    '    @Override protected void onResume() { super.onResume(); activityForeground=true; NotificationHelper.clearTalkieSpeaker(this); if(visualAlertController!=null)ConvoyForegroundAlertBus.register(visualAlertController); if (prefs.hasActiveConvoy()) { startPolling(); startShareServiceIfPermitted(); if(liveTalkie!=null)liveTalkie.ensureStarted(); } }\n'
    '    @Override protected void onPause() { activityForeground=false; if(visualAlertController!=null)ConvoyForegroundAlertBus.unregister(visualAlertController); super.onPause(); stopPolling(); }\n'
    '    @Override protected void onDestroy() { NotificationHelper.clearTalkieSpeaker(this); if(talkieSpeakerClearRunnable!=null)ui.removeCallbacks(talkieSpeakerClearRunnable); if(visualAlertController!=null){ConvoyForegroundAlertBus.unregister(visualAlertController);visualAlertController.close();} if(mapController!=null)mapController.close(); if(pollingController!=null)pollingController.close(); if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }',
    'lifecycle')

main = replace_once(
    main,
    '        talkieState = null;\n        talkiePttButton = null;\n        applyPalette();',
    '        talkieState = null;\n        talkiePttButton = null;\n        talkieSpeakerBanner = null;\n        screenRoot = null;\n        applyPalette();',
    'render reset')

main = replace_once(
    main,
    '        FrameLayout screen = new FrameLayout(this);\n        screen.setBackgroundColor(bg);',
    '        FrameLayout screen = new FrameLayout(this);\n        screenRoot=screen;\n        screen.setBackgroundColor(bg);',
    'screen root')

main = replace_once(
    main,
    '        bottomNav = buildBottomNav();\n        shell.addView(bottomNav,new LinearLayout.LayoutParams(-1,-2));\n\n        screen.setOnApplyWindowInsetsListener((v,insets)->{',
    '        bottomNav = buildBottomNav();\n        shell.addView(bottomNav,new LinearLayout.LayoutParams(-1,-2));\n        installTalkieSpeakerBanner(screen);\n\n        screen.setOnApplyWindowInsetsListener((v,insets)->{',
    'speaker banner install')

main = replace_once(
    main,
    '            if(bottomNav!=null) bottomNav.setPadding(dp(4),dp(4),dp(4),bottomInset+dp(4));\n            return insets;',
    '            if(bottomNav!=null) bottomNav.setPadding(dp(4),dp(4),dp(4),bottomInset+dp(4));\n'
    '            if(talkieSpeakerBanner!=null){FrameLayout.LayoutParams blp=(FrameLayout.LayoutParams)talkieSpeakerBanner.getLayoutParams();blp.bottomMargin=bottomInset+dp(94);talkieSpeakerBanner.setLayoutParams(blp);}\n'
    '            return insets;',
    'speaker banner inset')

insert_anchor = '    private void header() {'
if main.count(insert_anchor) != 1:
    raise SystemExit('header insertion anchor missing')
speaker_methods = r'''    private void installTalkieSpeakerBanner(FrameLayout screen){
        TextView banner=text("",14,true,Color.WHITE);
        banner.setGravity(Gravity.CENTER);
        banner.setMaxLines(1);
        banner.setPadding(dp(16),0,dp(16),0);
        banner.setMaxWidth(Math.max(dp(260),getResources().getDisplayMetrics().widthPixels-dp(32)));
        banner.setBackground(roundBg(Color.rgb(24,82,116),Color.rgb(70,180,255),18,2));
        banner.setElevation(dp(12));
        banner.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,dp(44),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin=dp(94);
        screen.addView(banner,lp);
        talkieSpeakerBanner=banner;
    }

    private void handleTalkieSpeakerState(String label,boolean receiving){
        if(!receiving||label==null)return;
        String speaker=talkieSpeakerName(label);
        if(speaker.isEmpty())speaker="Un participant";
        boolean changed=!speaker.equals(activeTalkieSpeaker);
        activeTalkieSpeaker=speaker;
        if(talkieSpeakerClearRunnable!=null)ui.removeCallbacks(talkieSpeakerClearRunnable);
        if(activityForeground){
            NotificationHelper.clearTalkieSpeaker(this);
            if(talkieSpeakerBanner!=null){
                talkieSpeakerBanner.setText(label.toUpperCase(Locale.ROOT));
                talkieSpeakerBanner.setVisibility(View.VISIBLE);
            }
        }else if(changed){
            NotificationHelper.notifyTalkieSpeaker(this,speaker);
        }
        talkieSpeakerClearRunnable=()->{
            activeTalkieSpeaker="";
            if(talkieSpeakerBanner!=null)talkieSpeakerBanner.setVisibility(View.GONE);
            NotificationHelper.clearTalkieSpeaker(this);
        };
        ui.postDelayed(talkieSpeakerClearRunnable,1100);
    }

    private String talkieSpeakerName(String label){
        if(label==null||!label.startsWith("🔊"))return "";
        String value=label.substring("🔊".length()).trim();
        int talks=value.toLowerCase(Locale.ROOT).indexOf(" parle");
        if(talks>0)return value.substring(0,talks).trim();
        if(value.toLowerCase(Locale.ROOT).startsWith("réception audio"))return "Un participant";
        int meter=value.indexOf("  ");
        return (meter>0?value.substring(0,meter):value).trim();
    }

'''
main = main.replace(insert_anchor, speaker_methods + insert_anchor, 1)

main = replace_once(main, '        GridLayout primary=new GridLayout(this);primary.setColumnCount(4);', '        GridLayout primary=new GridLayout(this);primary.setColumnCount(2);', 'primary columns')
main = replace_once(main, '            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(66);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));primary.addView(b,lp);', '            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(64);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));primary.addView(b,lp);', 'primary height')

start = main.find('        boolean moreOpen=prefs.getBool("homeMoreActionsExpanded",false);')
end_marker = '        moreHeader.setOnClickListener(v->{boolean open=secondary.getVisibility()==View.VISIBLE;secondary.setVisibility(open?View.GONE:View.VISIBLE);moreChevron.setText(open?"⌄":"⌃");prefs.putBool("homeMoreActionsExpanded",!open);});\n'
end = main.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit('more actions block anchors missing')
end += len(end_marker)
more_compact = r'''        LinearLayout moreHeader=new LinearLayout(this);moreHeader.setGravity(Gravity.CENTER_VERTICAL);moreHeader.setPadding(dp(10),dp(4),dp(8),dp(4));moreHeader.setBackground(roundBg(control,border,13,1));
        LinearLayout.LayoutParams mhLp=new LinearLayout.LayoutParams(-1,dp(42));mhLp.setMargins(dp(3),dp(5),dp(3),dp(2));moreHeader.setLayoutParams(mhLp);
        TextView moreLabel=text("PLUS D’ACTIONS",11,true,muted);moreHeader.addView(moreLabel,new LinearLayout.LayoutParams(0,-1,1));
        TextView moreChevron=text("›",22,true,accent);moreChevron.setGravity(Gravity.CENTER);moreHeader.addView(moreChevron,new LinearLayout.LayoutParams(dp(38),-1));content.addView(moreHeader);
        moreHeader.setOnClickListener(v->showDrivingMoreActionsDialog());
'''
main = main[:start] + more_compact + main[end:]

method_anchor = '    private String snapshotCountText(){'
if main.count(method_anchor) != 1:
    raise SystemExit('snapshotCountText anchor missing')
more_dialog = r'''    private void showDrivingMoreActionsDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);
        final Dialog[] ref=new Dialog[1];
        String[][] extra={{"☕","Pause","pause"},{"🚗","Voiture","car_problem"},{"↗️","Je rejoins","joining"},{"👍","OK","ok"}};
        for(String[] st:extra){
            View b=quickActionTile(st[0],st[1]);
            b.setOnClickListener(v->{if(ref[0]!=null)ref[0].dismiss();sendStatus(st[2]);});
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(58);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));grid.addView(b,lp);
        }
        box.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        Button custom=outlinedButton("✎   AUTRE MESSAGE",Color.rgb(94,99,104));custom.setOnClickListener(v->{if(ref[0]!=null)ref[0].dismiss();customStatusDialog();});box.addView(custom);
        Button clear=ghostButton("ANNULER MON STATUT");clear.setOnClickListener(v->{if(ref[0]!=null)ref[0].dismiss();sendStatus("clear");});box.addView(clear);
        Button receive=ghostButton(prefs.getBool("talkieReceive",true)?"🔊  RÉCEPTION TALKIE : OUI":"🔇  RÉCEPTION TALKIE : NON");
        receive.setOnClickListener(v->{boolean on=!prefs.getBool("talkieReceive",true);prefs.putBool("talkieReceive",on);if(liveTalkie!=null)liveTalkie.setReceiveEnabled(on);receive.setText(on?"🔊  RÉCEPTION TALKIE : OUI":"🔇  RÉCEPTION TALKIE : NON");toast(on?"Réception live activée":"Réception live coupée");});box.addView(receive);
        ref[0]=styledDialog("Plus d’actions",box,"FERMER",null,null,null,false);
    }

'''
main = main.replace(method_anchor, more_dialog + method_anchor, 1)

if '0.3.36' not in main:
    raise SystemExit('MainActivity version marker 0.3.36 missing')
main = main.replace('0.3.36', '0.3.37')

# ---- LiveTalkieManager: identify the actual peer currently producing inbound audio ----
live = live_path.read_text()
live = replace_once(live, '    private volatile boolean receivingAudio = false;\n    private volatile double receiveLevel = 0.0;', '    private volatile boolean receivingAudio = false;\n    private volatile String receivingPeerName = "";\n    private volatile double receiveLevel = 0.0;', 'receiving peer field')
live = replace_once(live, '        transmittingSince = 0L;\n        receivingAudio = false;\n        receiveLevel = 0.0;\n        micLevel = 0.0;', '        transmittingSince = 0L;\n        receivingAudio = false;\n        receivingPeerName = "";\n        receiveLevel = 0.0;\n        micLevel = 0.0;', 'stop receiving peer')

poll_start = live.find('    private void pollAudioMeters() {')
poll_end = live.find('    private AudioStats audioStats(RTCStatsReport report) {', poll_start)
if poll_start < 0 or poll_end < 0:
    raise SystemExit('audio meter method anchors missing')
new_poll = r'''    private void pollAudioMeters() {
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
                        receivingAudio = receiveEnabled && !transmitting && receiveLevel >= 0.012;
                        if (receivingAudio && loudest[0] != null) {
                            String n = loudest[0].displayName == null ? "" : loudest[0].displayName.trim();
                            receivingPeerName = n.isEmpty() ? "Un participant" : n;
                        } else receivingPeerName = "";
                        if (receivingAudio) ensureReceivePath();
                        notifyState(stateLabel());
                        scheduleAudioMeter(180);
                    }
                });
            } catch (Throwable ignored) {
                if (pending.decrementAndGet() == 0) scheduleAudioMeter(250);
            }
        }
    }

'''
live = live[:poll_start] + new_poll + live[poll_end:]

live = replace_once(
    live,
    '            PeerState ps;\n            synchronized (peerLock) { ps = peers.get(id); }\n            if (ps == null) createPeer(id);',
    '            String displayName = p.optString("name", "").trim();\n            PeerState ps;\n            synchronized (peerLock) { ps = peers.get(id); }\n            if (ps == null) ps = createPeer(id);\n            if (ps != null && !displayName.isEmpty()) ps.displayName = displayName;',
    'peer display name')

live = replace_once(
    live,
    '        else if (receivingAudio) shown = "🔊 RÉCEPTION AUDIO  " + meter(receiveLevel);',
    '        else if (receivingAudio) shown = "🔊 " + (receivingPeerName.isEmpty()?"Un participant":receivingPeerName) + " parle  " + meter(receiveLevel);',
    'speaker label')

live = replace_once(
    live,
    '        volatile boolean connected = false;\n        volatile boolean remoteDescriptionSet = false;',
    '        volatile boolean connected = false;\n        volatile String displayName = "";\n        volatile boolean remoteDescriptionSet = false;',
    'peer name storage')

# ---- NotificationHelper: transient heads-up while app is backgrounded ----
notif = notif_path.read_text()
notif = replace_once(
    notif,
    '    public static final String ALERTS_CHANNEL = "convoy_alerts_v2";\n    private static final int ACCENT = Color.rgb(255,181,20);',
    '    public static final String ALERTS_CHANNEL = "convoy_alerts_v2";\n    public static final String TALKIE_CHANNEL = "convoy_talkie_v1";\n    private static final int TALKIE_NOTIFICATION_ID = 22001;\n    private static final int ACCENT = Color.rgb(255,181,20);',
    'talkie notification constants')

notif_anchor = '    public static void notifyEvent(Context context, JSONObject event) {'
if notif.count(notif_anchor) != 1:
    raise SystemExit('notifyEvent anchor missing')
notif_methods = r'''    public static void ensureTalkieChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel c = new NotificationChannel(
                TALKIE_CHANNEL,
                "Talkie-walkie Mode Convoi",
                NotificationManager.IMPORTANCE_HIGH);
        c.setDescription("Affiche brièvement le nom de la personne qui parle dans le convoi");
        c.enableVibration(false);
        c.setSound(null, null);
        c.enableLights(false);
        c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        c.setShowBadge(false);
        nm.createNotificationChannel(c);
    }

    public static void notifyTalkieSpeaker(Context context, String speakerName) {
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        ensureTalkieChannel(context);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        String who = speakerName == null || speakerName.trim().isEmpty() ? "Un participant" : speakerName.trim();
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                TALKIE_NOTIFICATION_ID,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(context, TALKIE_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(70,180,255))
                .setContentTitle("🔊 " + who + " parle")
                .setContentText("Talkie-walkie du convoi")
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(false)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setShowWhen(false)
                .setTimeoutAfter(3500)
                .build();
        nm.notify(TALKIE_NOTIFICATION_ID, n);
    }

    public static void clearTalkieSpeaker(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(TALKIE_NOTIFICATION_ID);
    }

'''
notif = notif.replace(notif_anchor, notif_methods + notif_anchor, 1)

# ---- Version ----
api = api_path.read_text()
api = replace_once(api, 'ModeConvoi-Android/0.3.36', 'ModeConvoi-Android/0.3.37', 'user agent version')

gradle = gradle_path.read_text()
gradle = replace_once(gradle, '        versionCode 39\n        versionName \'0.3.36\'', '        versionCode 40\n        versionName \'0.3.37\'', 'gradle version')

# Final validation
checks = [
    ('main popup actions', 'showDrivingMoreActionsDialog()' in main),
    ('main speaker banner', 'handleTalkieSpeakerState(label,receiving)' in main),
    ('main 2-column driving grid', 'primary.setColumnCount(2)' in main),
    ('live speaker name', 'receivingPeerName' in live and 'displayName' in live),
    ('talkie notification', 'notifyTalkieSpeaker' in notif and 'TALKIE_CHANNEL' in notif),
    ('version', "versionCode 40" in gradle and "versionName '0.3.37'" in gradle),
]
for label, ok in checks:
    if not ok:
        raise SystemExit(f'validation failed: {label}')

main_path.write_text(main)
live_path.write_text(live)
notif_path.write_text(notif)
api_path.write_text(api)
gradle_path.write_text(gradle)
print('Mode Convoi 0.3.37 driving actions + speaker visibility migration applied')
