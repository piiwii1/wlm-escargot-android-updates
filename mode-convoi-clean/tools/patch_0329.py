from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
main_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java"
build_path = root / "android/app/build.gradle"
api_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoyApi.java"

main = main_path.read_text(encoding="utf-8")

# Already migrated: only validate and keep the script idempotent.
if "private ConvoyPollingController pollingController;" not in main:
    main = main.replace("import java.net.URLEncoder;\n", "", 1)

    fields_pattern = re.compile(
        r"    private boolean polling;\n"
        r"    private boolean pollInFlight=false;\n"
        r"    private boolean pollScheduled=false;\n"
        r"    private boolean busyOperation=false;\n"
        r"    private final Runnable pollRunnable=\(\)->\{ pollScheduled=false; pollOnce\(\); \};\n"
        r"    private int consecutivePollFailures = 0;\n"
        r"    private long lastSuccessfulSyncAt = 0;\n"
    )
    replacement = (
        "    private boolean busyOperation=false;\n"
        "    private ConvoyPollingController pollingController;\n"
    )
    main, count = fields_pattern.subn(replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.29: polling field block not found exactly once")

    init_anchor = "        applyPalette();\n        NotificationHelper.ensureAlertChannel(this);"
    init_replacement = """        applyPalette();
        pollingController = new ConvoyPollingController(this,prefs,DEFAULT_SERVER,new ConvoyPollingController.Listener(){
            @Override public void onSnapshot(JSONObject s,boolean renamed,long synchronizedAt){
                snapshot=s;
                if(liveTalkie!=null)liveTalkie.ensureStarted();
                if(renamed&&\"home\".equals(currentPage))render();
                else if(mapView!=null)pushMap();
                else if(\"home\".equals(currentPage))refreshSnapshotArea();
                else if(\"participants\".equals(currentPage))renderParticipantsPage();
            }
            @Override public void onConnectionState(ConvoyPollingController.ConnectionState state,int failures){
                if(connectionBadge==null)return;
                if(state==ConvoyPollingController.ConnectionState.CONNECTED){
                    connectionBadge.setText(\"● CONNECTÉ\");connectionBadge.setTextColor(Color.rgb(90,200,120));
                }else if(state==ConvoyPollingController.ConnectionState.RECONNECTING){
                    connectionBadge.setText(\"● RECONNEXION\");connectionBadge.setTextColor(accent);
                }else{
                    connectionBadge.setText(\"● HORS LIGNE\");connectionBadge.setTextColor(danger);
                }
            }
            @Override public void onSessionInvalidated(int statusCode){
                toast(statusCode==404?\"Convoi fermé ou introuvable\":\"Accès au convoi retiré\");
                endSession();
            }
        });
        NotificationHelper.ensureAlertChannel(this);"""
    if init_anchor not in main:
        raise SystemExit("0.3.29: controller initialization anchor not found")
    main = main.replace(init_anchor, init_replacement, 1)

    old_destroy = "    @Override protected void onDestroy() { if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }"
    new_destroy = "    @Override protected void onDestroy() { if(pollingController!=null)pollingController.close(); if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }"
    if old_destroy not in main:
        raise SystemExit("0.3.29: onDestroy anchor not found")
    main = main.replace(old_destroy, new_destroy, 1)

    polling_pattern = re.compile(
        r"    private void startPolling\(\)\{.*?\n"
        r"    private View participantAvatar\(JSONObject p,int size,int fallbackColor\)\{",
        re.S,
    )
    polling_replacement = """    private void startPolling(){if(pollingController!=null)pollingController.start();}
    private void stopPolling(){if(pollingController!=null)pollingController.stop();}
    private void pollOnce(){if(pollingController!=null)pollingController.refreshNow();}

    private View participantAvatar(JSONObject p,int size,int fallbackColor){"""
    main, count = polling_pattern.subn(polling_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.29: polling method block not found exactly once")

    old_sync = "        TextView sync=text(lastSuccessfulSyncAt>0?\"Dernière synchronisation : \"+ageText(Math.max(0,System.currentTimeMillis()-lastSuccessfulSyncAt)):\"Aucune synchronisation récente\",12,false,muted); sync.setPadding(0,dp(4),0,dp(6)); diagnostic.addView(sync);"
    new_sync = "        long lastSyncAt=pollingController==null?0L:pollingController.lastSuccessfulSyncAt();\n        TextView sync=text(lastSyncAt>0?\"Dernière synchronisation : \"+ageText(Math.max(0,System.currentTimeMillis()-lastSyncAt)):\"Aucune synchronisation récente\",12,false,muted); sync.setPadding(0,dp(4),0,dp(6)); diagnostic.addView(sync);"
    if old_sync not in main:
        raise SystemExit("0.3.29: synchronization diagnostic anchor not found")
    main = main.replace(old_sync, new_sync, 1)

    main = main.replace('cardTitle(content,"Mode Convoi 0.3.17",', 'cardTitle(content,"Mode Convoi 0.3.29",', 1)

# Hard validation: old polling implementation must be gone.
for forbidden in (
    "private boolean polling;",
    "pollInFlight",
    "pollScheduled",
    "pollRunnable",
    "consecutivePollFailures",
    "lastSuccessfulSyncAt",
    "URLEncoder.encode(prefs.get(\"participantId\"",
):
    if forbidden in main:
        raise SystemExit(f"0.3.29: stale MainActivity polling token remains: {forbidden}")

required = (
    "private ConvoyPollingController pollingController;",
    "pollingController = new ConvoyPollingController",
    "pollingController.refreshNow()",
    "pollingController.lastSuccessfulSyncAt()",
    "Mode Convoi 0.3.29",
)
for token in required:
    if token not in main:
        raise SystemExit(f"0.3.29: required MainActivity token missing: {token}")

main_path.write_text(main, encoding="utf-8")

build = build_path.read_text(encoding="utf-8")
build = build.replace("versionCode 31", "versionCode 32")
build = build.replace("versionName '0.3.28'", "versionName '0.3.29'")
if "versionCode 32" not in build or "versionName '0.3.29'" not in build:
    raise SystemExit("0.3.29: build version migration failed")
build_path.write_text(build, encoding="utf-8")

api = api_path.read_text(encoding="utf-8")
api = api.replace("ModeConvoi-Android/0.3.28", "ModeConvoi-Android/0.3.29")
if "ModeConvoi-Android/0.3.29" not in api:
    raise SystemExit("0.3.29: API user-agent migration failed")
api_path.write_text(api, encoding="utf-8")

print("Mode Convoi 0.3.29 polling refactor validated")
