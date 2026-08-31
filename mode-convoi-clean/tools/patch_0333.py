from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
main_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java"
gradle_path = root / "android/app/build.gradle"
api_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoyApi.java"
resolver_path = root / "android/app/src/main/java/ch/piiwii/modeconvoi/ConvoyPositionResolver.java"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 anchor, found {count}")
    return text.replace(old, new, 1)


def regex_once(text, pattern, replacement, label):
    out, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 regex match, found {count}")
    return out

if not resolver_path.exists():
    raise SystemExit("ConvoyPositionResolver.java is missing")
resolver = resolver_path.read_text()
for required in [
    "public final class ConvoyPositionResolver",
    "public Relative resolveRelative(JSONObject snapshot)",
    "public RallyInfo rallyInfo(JSONObject snapshot)",
    "public JSONObject ownLocation(JSONObject snapshot)",
]:
    if required not in resolver:
        raise SystemExit(f"resolver validation failed: {required}")

main = main_path.read_text()
main = replace_once(
    main,
    "    private ConvoySessionManager sessionManager;\n    private ConvoyMapController mapController;",
    "    private ConvoySessionManager sessionManager;\n    private ConvoyMapController mapController;\n    private ConvoyPositionResolver positionResolver;",
    "position resolver field",
)
main = replace_once(
    main,
    "        sessionManager = new ConvoySessionManager(prefs,DEFAULT_SERVER);\n        mapController = new ConvoyMapController(prefs,()->snapshot);",
    "        sessionManager = new ConvoySessionManager(prefs,DEFAULT_SERVER);\n        mapController = new ConvoyMapController(prefs,()->snapshot);\n        positionResolver = new ConvoyPositionResolver(prefs);",
    "position resolver init",
)
main = replace_once(
    main,
    "        JSONObject me=findMe(); Relative rel=computeRelative();",
    "        JSONObject me=positionResolver.findMe(snapshot); ConvoyPositionResolver.Relative rel=positionResolver.resolveRelative(snapshot);",
    "home relative resolution",
)
main = replace_once(
    main,
    "        JSONObject rally=snapshot.optJSONObject(\"rally\");if(rally!=null){LinearLayout box=cardBox();box.addView(text(\"📍  POINT DE REGROUPEMENT\",11,true,accent));box.addView(text(rally.optString(\"name\",\"Point de regroupement\"),18,true,fg));\n            String desired=rally.optString(\"desiredTime\",\"\"); if(!desired.isEmpty()) box.addView(text(\"Heure souhaitée : \"+desired,12,true,accent));target.addView(box);}",
    "        ConvoyPositionResolver.RallyInfo rallyInfo=positionResolver.rallyInfo(snapshot);if(rallyInfo!=null){JSONObject rally=rallyInfo.rally;LinearLayout box=cardBox();box.addView(text(\"📍  POINT DE REGROUPEMENT\",11,true,accent));box.addView(text(rally.optString(\"name\",\"Point de regroupement\"),18,true,fg));\n            if(!rallyInfo.desiredTime.isEmpty()) box.addView(text(\"Heure souhaitée : \"+rallyInfo.desiredTime,12,true,accent));target.addView(box);}",
    "home rally info",
)
main = regex_once(
    main,
    r"\n\n    private static class Relative \{ JSONObject ahead, behind; \}\n    private Relative computeRelative\(\) \{.*?\n    private JSONObject findMe\(\)\{.*?\}\n\n    private void renderMapPage\(\) \{",
    "\n\n    private void renderMapPage() {",
    "legacy relative resolver removal",
)
main = replace_once(
    main,
    "        if(snapshot!=null){\n            JSONObject rally=snapshot.optJSONObject(\"rally\");\n            if(rally!=null){",
    "        if(snapshot!=null){\n            ConvoyPositionResolver.RallyInfo rallyInfo=positionResolver.rallyInfo(snapshot);\n            if(rallyInfo!=null){\n                JSONObject rally=rallyInfo.rally;",
    "map rally resolution",
)
main = regex_once(
    main,
    r"                StringBuilder sub=new StringBuilder\(\);\n                String desired=rally\.optString\(\"desiredTime\",\"\"\);\n                JSONObject me=findMe\(\);\n                JSONObject ml=me==null\?null:me\.optJSONObject\(\"location\"\);\n                if\(ml!=null\)\{.*?                labels\.addView\(text\(sub\.toString\(\),11,false,muted\)\);",
    "                labels.addView(text(rallyInfo.subtitle,11,false,muted));",
    "map rally subtitle",
)
main = replace_once(
    main,
    "        JSONObject me=findMe();JSONObject loc=me==null?null:me.optJSONObject(\"location\");if(loc!=null){lat.setText(String.valueOf(loc.optDouble(\"lat\")));lon.setText(String.valueOf(loc.optDouble(\"lon\")));}",
    "        JSONObject loc=positionResolver.ownLocation(snapshot);if(loc!=null){lat.setText(String.valueOf(loc.optDouble(\"lat\")));lon.setText(String.valueOf(loc.optDouble(\"lon\")));}",
    "rally dialog own location",
)
main = replace_once(main, "Mode Convoi 0.3.32", "Mode Convoi 0.3.33", "about version")

for forbidden in [
    "private static class Relative",
    "private Relative computeRelative()",
    "private JSONObject findMe()",
    "StringBuilder sub=new StringBuilder();",
]:
    if forbidden in main:
        raise SystemExit(f"legacy position logic still present: {forbidden}")
for required in [
    "private ConvoyPositionResolver positionResolver;",
    "positionResolver = new ConvoyPositionResolver(prefs);",
    "positionResolver.resolveRelative(snapshot)",
    "positionResolver.rallyInfo(snapshot)",
    "positionResolver.ownLocation(snapshot)",
    "Mode Convoi 0.3.33",
]:
    if required not in main:
        raise SystemExit(f"main validation failed: {required}")
main_path.write_text(main)

gradle = gradle_path.read_text()
gradle = replace_once(gradle, "versionCode 35", "versionCode 36", "versionCode")
gradle = replace_once(gradle, "versionName '0.3.32'", "versionName '0.3.33'", "versionName")
gradle_path.write_text(gradle)

api = api_path.read_text()
api = replace_once(api, "ModeConvoi-Android/0.3.32", "ModeConvoi-Android/0.3.33", "User-Agent")
api_path.write_text(api)

print("Mode Convoi 0.3.33 guarded position refactor applied")
