from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
main_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java"
build_path = root / "android/app/build.gradle"
api_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoyApi.java"
manager_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoySessionManager.java"

main = main_path.read_text(encoding="utf-8")

if not manager_path.exists():
    raise SystemExit("0.3.30: ConvoySessionManager.java missing")
manager = manager_path.read_text(encoding="utf-8")
for token in (
    "public final class ConvoySessionManager",
    "public JSONObject create(",
    "public JSONObject join(",
    "public void leaveBestEffort()",
    "public JSONObject close()",
    "public JSONObject rename(",
    "public JSONObject setParticipantRole(",
    "public JSONObject removeParticipant(",
    "public JSONObject setRally(",
    "public JSONObject sendGeneralStop()",
    "ConvoySnapshotRepository.invalidate();",
):
    if token not in manager:
        raise SystemExit(f"0.3.30: manager token missing: {token}")

if "private ConvoySessionManager sessionManager;" not in main:
    field_anchor = "    private ConvoyPollingController pollingController;\n"
    if main.count(field_anchor) != 1:
        raise SystemExit("0.3.30: polling controller field anchor mismatch")
    main = main.replace(field_anchor, field_anchor + "    private ConvoySessionManager sessionManager;\n", 1)

    init_anchor = "        applyPalette();\n        pollingController = new ConvoyPollingController"
    if main.count(init_anchor) != 1:
        raise SystemExit("0.3.30: session manager initialization anchor mismatch")
    main = main.replace(
        init_anchor,
        "        applyPalette();\n        sessionManager = new ConvoySessionManager(prefs,DEFAULT_SERVER);\n        pollingController = new ConvoyPollingController",
        1,
    )

    session_block = re.compile(
        r"    private void createConvoy\(String name\) \{.*?\n"
        r"    private void renderConvoyHome\(\) \{",
        re.S,
    )
    session_replacement = """    private void createConvoy(String name) {
        runBusy("Création…",()->sessionManager.create(name), r->{ ensurePermissionsAndService(); render(); startPolling(); ui.postDelayed(this::showConvoyQr,250); });
    }
    private void joinConvoy(String raw) {
        try{sessionManager.normalizeJoinCode(raw);}catch(Exception e){toast("Code invalide");return;}
        runBusy("Connexion…",()->sessionManager.join(raw), r->{ ensurePermissionsAndService(); render(); startPolling(); });
    }

    private void renderConvoyHome() {"""
    main, count = session_block.subn(session_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.30: create/join/saveSession block not found exactly once")

    rename_pattern = re.compile(
        r"    private void renameConvoy\(String raw\)\{.*?\n    \}",
        re.S,
    )
    rename_replacement = """    private void renameConvoy(String raw){
        String name=raw==null?"":raw.trim(); if(name.length()<2){toast("Nom trop court");return;}
        runBusy("Mise à jour…",()->sessionManager.rename(name),r->render());
    }"""
    main, count = rename_pattern.subn(rename_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.30: renameConvoy method not found exactly once")

    role_pattern = re.compile(r"    private void setParticipantRole\(String targetId,String role\)\{.*?\n    \}", re.S)
    role_replacement = """    private void setParticipantRole(String targetId,String role){
        runBusy("Mise à jour du rôle…",()->sessionManager.setParticipantRole(targetId,role),r->pollOnce());
    }"""
    main, count = role_pattern.subn(role_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.30: setParticipantRole method not found exactly once")

    remove_pattern = re.compile(r"    private void removeParticipant\(String targetId\)\{.*?\n    \}", re.S)
    remove_replacement = """    private void removeParticipant(String targetId){
        runBusy("Suppression…",()->sessionManager.removeParticipant(targetId),r->pollOnce());
    }"""
    main, count = remove_pattern.subn(remove_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.30: removeParticipant method not found exactly once")

    rally_pattern = re.compile(r"    private void setRally\(String name,String desiredTime,String latS,String lonS\)\{.*?\n", re.S)
    rally_replacement = """    private void setRally(String name,String desiredTime,String latS,String lonS){try{double lat=Double.parseDouble(latS.replace(',','.')),lon=Double.parseDouble(lonS.replace(',','.'));runBusy("Partage…",()->sessionManager.setRally(name,desiredTime,lat,lon),r->pollOnce());}catch(Exception e){toast("Coordonnées invalides");}}
"""
    main, count = rally_pattern.subn(rally_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.30: setRally method not found exactly once")

    stop_pattern = re.compile(
        r"    private void sendGeneralStop\(\)\{.*?\n    \}\n",
        re.S,
    )
    stop_replacement = """    private void sendGeneralStop(){
        runBusy("Envoi de l’arrêt général…",()->sessionManager.sendGeneralStop(),r->pollOnce());
    }
"""
    main, count = stop_pattern.subn(stop_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.30: sendGeneralStop method not found exactly once")

    close_pattern = re.compile(
        r"    private void confirmClose\(\)\{.*?\n    private void endSession\(\)",
        re.S,
    )
    close_replacement = """    private void confirmClose(){LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.addView(text("Tous les participants seront déconnectés et le partage GPS sera arrêté.",14,false,fg));styledDialog("Fermer le convoi ?",body,"ANNULER",null,"FERMER",d->{runBusy("Fermeture…",()->sessionManager.close(),r->endSession());return true;},true);}
    private void endSession()"""
    main, count = close_pattern.subn(close_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.30: confirmClose block not found exactly once")

    leave_pattern = re.compile(
        r"    private void leaveConvoyNow\(\)\{.*?\n    \}\n\n    private boolean hasLocationPermission\(\)",
        re.S,
    )
    leave_replacement = """    private void leaveConvoyNow(){
        io.execute(()->{sessionManager.leaveBestEffort();ui.post(this::endSession);});
    }

    private boolean hasLocationPermission()"""
    main, count = leave_pattern.subn(leave_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.30: leaveConvoyNow method not found exactly once")

    main = main.replace('cardTitle(content,"Mode Convoi 0.3.29",', 'cardTitle(content,"Mode Convoi 0.3.30",', 1)

# Validate that session/admin transport no longer leaks into MainActivity.
required = (
    "private ConvoySessionManager sessionManager;",
    "sessionManager = new ConvoySessionManager",
    "sessionManager.create(name)",
    "sessionManager.join(raw)",
    "sessionManager.rename(name)",
    "sessionManager.setParticipantRole(targetId,role)",
    "sessionManager.removeParticipant(targetId)",
    "sessionManager.setRally(name,desiredTime,lat,lon)",
    "sessionManager.sendGeneralStop()",
    "sessionManager.close()",
    "sessionManager.leaveBestEffort()",
    "Mode Convoi 0.3.30",
)
for token in required:
    if token not in main:
        raise SystemExit(f"0.3.30: required MainActivity token missing: {token}")

for forbidden in (
    "private void saveSession(",
    '"/api/convoys",body,null',
    '"/join",body,null',
    '"/name",b,prefs.get("adminKey"',
    '"/role",b,prefs.get("adminKey"',
    '"/remove",b,prefs.get("adminKey"',
    '"/rally",b,prefs.get("adminKey"',
    '"/emergency-stop",b,prefs.get("adminKey"',
    '"/close",b,prefs.get("adminKey"',
    '"/leave",b,null',
):
    if forbidden in main:
        raise SystemExit(f"0.3.30: stale session/admin transport remains in MainActivity: {forbidden}")

main_path.write_text(main, encoding="utf-8")

build = build_path.read_text(encoding="utf-8")
build = build.replace("versionCode 32", "versionCode 33")
build = build.replace("versionName '0.3.29'", "versionName '0.3.30'")
if "versionCode 33" not in build or "versionName '0.3.30'" not in build:
    raise SystemExit("0.3.30: build version migration failed")
build_path.write_text(build, encoding="utf-8")

api = api_path.read_text(encoding="utf-8")
api = api.replace("ModeConvoi-Android/0.3.29", "ModeConvoi-Android/0.3.30")
if "ModeConvoi-Android/0.3.30" not in api:
    raise SystemExit("0.3.30: API user-agent migration failed")
api_path.write_text(api, encoding="utf-8")

print("Mode Convoi 0.3.30 session/admin refactor validated")
