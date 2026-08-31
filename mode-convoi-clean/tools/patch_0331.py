from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
main_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java"
build_path = root / "android/app/build.gradle"
api_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoyApi.java"
manager_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoySessionManager.java"

main = main_path.read_text(encoding="utf-8")
manager = manager_path.read_text(encoding="utf-8")

for token in (
    "public JSONObject sendStatus(",
    "public JSONObject sendCustomStatus(",
    "public JSONObject syncProfile()",
    "private JSONObject authenticatedParticipantBody()",
    "private void requireActiveSession()",
):
    if token not in manager:
        raise SystemExit(f"0.3.31: manager token missing: {token}")

if "sessionManager.sendStatus(status)" not in main:
    status_pattern = re.compile(r'^    private void sendStatus\(String status\)\{.*\}$', re.M)
    status_replacement = '    private void sendStatus(String status){runBusy("Envoi…",()->sessionManager.sendStatus(status),r->pollOnce());}'
    main, count = status_pattern.subn(status_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.31: sendStatus method not found exactly once")

    custom_pattern = re.compile(
        r"    private void sendCustomStatus\(String raw\)\{.*?\n"
        r"    private JSONObject authBody\(\) throws JSONException\{.*?\}\n",
        re.S,
    )
    custom_replacement = """    private void sendCustomStatus(String raw){
        String message=raw==null?"":raw.trim(); if(message.isEmpty()){toast("Message vide");return;}
        runBusy("Envoi…",()->sessionManager.sendCustomStatus(message),r->pollOnce());
    }
"""
    main, count = custom_pattern.subn(custom_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.31: custom status/authBody block not found exactly once")

    profile_pattern = re.compile(r'^    private void syncProfileToServer\(\)\{.*\}$', re.M)
    profile_replacement = '    private void syncProfileToServer(){if(!prefs.hasActiveConvoy())return;io.execute(()->{try{sessionManager.syncProfile();ui.post(()->{toast("Profil mis à jour");pollOnce();});}catch(Exception e){ui.post(()->toast("Profil local enregistré · serveur à mettre à jour"));}});}'
    main, count = profile_pattern.subn(profile_replacement, main, count=1)
    if count != 1:
        raise SystemExit("0.3.31: syncProfileToServer method not found exactly once")

    main = main.replace('cardTitle(content,"Mode Convoi 0.3.30",', 'cardTitle(content,"Mode Convoi 0.3.31",', 1)

required = (
    "sessionManager.sendStatus(status)",
    "sessionManager.sendCustomStatus(message)",
    "sessionManager.syncProfile()",
    "Mode Convoi 0.3.31",
)
for token in required:
    if token not in main:
        raise SystemExit(f"0.3.31: required MainActivity token missing: {token}")

for forbidden in (
    "private JSONObject authBody(",
    '"/status",b,null',
    '"/profile",b,null',
):
    if forbidden in main:
        raise SystemExit(f"0.3.31: stale participant transport remains in MainActivity: {forbidden}")

main_path.write_text(main, encoding="utf-8")

build = build_path.read_text(encoding="utf-8")
build = build.replace("versionCode 33", "versionCode 34")
build = build.replace("versionName '0.3.30'", "versionName '0.3.31'")
if "versionCode 34" not in build or "versionName '0.3.31'" not in build:
    raise SystemExit("0.3.31: build version migration failed")
build_path.write_text(build, encoding="utf-8")

api = api_path.read_text(encoding="utf-8")
api = api.replace("ModeConvoi-Android/0.3.30", "ModeConvoi-Android/0.3.31")
if "ModeConvoi-Android/0.3.31" not in api:
    raise SystemExit("0.3.31: API user-agent migration failed")
api_path.write_text(api, encoding="utf-8")

print("Mode Convoi 0.3.31 participant action/profile refactor validated")
