from pathlib import Path
import base64, hashlib, shutil

repo=Path.cwd()
tmp=repo/'.tmp0342'
app=repo/'mode-convoi-clean/android/app'
res=app/'src/main/res'
java=app/'src/main/java/ch/piiwii/modeconvoi'

def decode_parts(prefix):
    parts=sorted(tmp.glob(prefix+'_*.b64'))
    if not parts:
        raise SystemExit(f'missing chunks for {prefix}')
    raw=base64.b64decode(''.join(p.read_text().strip() for p in parts), validate=True)
    return raw

vw=decode_parts('vw')
icon=decode_parts('icon')
assert hashlib.sha256(vw).hexdigest()=='205b5d61bb54cdc35d07d9d717fc0655fa25e07fbd6a64cdeb5107ece3390cf8'
assert hashlib.sha256(icon).hexdigest()=='9b7c06a43b42fd89dc2393836733195040630da9d9f3a6548e861073c1b0810a'
(res/'drawable-nodpi/vw_sprite_192.webp').write_bytes(vw)
(res/'drawable-nodpi/app_icon_convoi.webp').write_bytes(icon)

pack='''package ch.piiwii.modeconvoi;\n\nimport android.content.res.Resources;\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\n\npublic final class VolkswagenIconPack {\n    private static final int CELL=192;\n    private static final int COLS=5;\n    private static Bitmap spriteCache;\n    private static final Bitmap[] bitmapCache=new Bitmap[20];\n\n    public static final class Item {\n        public final String id;\n        public final String label;\n        private final int index;\n        Item(String id,String label,int index){this.id=id;this.label=label;this.index=index;}\n        public Bitmap bitmap(Resources resources){return bitmapAt(resources,index);}\n    }\n\n    private static final Item[] ITEMS={\n        new Item("vw:beetle","Coccinelle",0),\n        new Item("vw:t1","Combi T1",1),\n        new Item("vw:golf1","Golf 1 GTI",2),\n        new Item("vw:golf2","Golf 2 GTI",3),\n        new Item("vw:golf3","Golf 3",4),\n        new Item("vw:golf4","Golf 4",5),\n        new Item("vw:polo6n2","Polo 6N2 GTI",6),\n        new Item("vw:passatb5","Passat B5 Variant",7),\n        new Item("vw:jetta2","Jetta 2",8),\n        new Item("vw:newbeetle","New Beetle",9),\n        new Item("vw:karmannghia","Karmann Ghia",10),\n        new Item("vw:type3fastback","Type 3 Fastback",11),\n        new Item("vw:thing181","Type 181",12),\n        new Item("vw:t2","Combi T2",13),\n        new Item("vw:t3","Combi T3",14),\n        new Item("vw:scirocco1","Scirocco 1",15),\n        new Item("vw:scirocco2","Scirocco 2",16),\n        new Item("vw:corrado","Corrado VR6",17),\n        new Item("vw:lupogti","Lupo GTI",18),\n        new Item("vw:polog40","Polo G40",19)\n    };\n\n    private VolkswagenIconPack(){}\n    public static Item[] items(){return ITEMS.clone();}\n    public static Item find(String id){if(id==null)return null;for(Item item:ITEMS)if(item.id.equals(id))return item;return null;}\n    public static boolean isVolkswagen(String id){return find(id)!=null;}\n    public static int indexOf(String id){Item item=find(id);return item==null?-1:item.index;}\n    public static Bitmap bitmapFor(Resources resources,String id){Item item=find(id);return item==null?null:bitmapAt(resources,item.index);}\n\n    private static synchronized Bitmap bitmapAt(Resources resources,int index){\n        if(index<0||index>=bitmapCache.length||resources==null)return null;\n        if(bitmapCache[index]!=null&&!bitmapCache[index].isRecycled())return bitmapCache[index];\n        if(spriteCache==null||spriteCache.isRecycled())spriteCache=BitmapFactory.decodeResource(resources,R.drawable.vw_sprite_192);\n        if(spriteCache==null)return null;\n        int x=(index%COLS)*CELL,y=(index/COLS)*CELL;\n        if(x+CELL>spriteCache.getWidth()||y+CELL>spriteCache.getHeight())return null;\n        bitmapCache[index]=Bitmap.createBitmap(spriteCache,x,y,CELL,CELL);\n        return bitmapCache[index];\n    }\n}\n'''
(java/'VolkswagenIconPack.java').write_text(pack)

main=java/'MainActivity.java'
s=main.read_text()
old='10 modèles Volkswagen HD · appuie sur une voiture pour l\'utiliser sur la carte.'
new='20 modèles Volkswagen HD · appuie sur une voiture pour l\'utiliser sur la carte.'
if old not in s:
    raise SystemExit('VW hint anchor missing')
s=s.replace(old,new,1)
main.write_text(s)

# Keep any hard-coded User-Agent/version strings aligned.
for p in java.glob('*.java'):
    t=p.read_text()
    if '0.3.41' in t:
        p.write_text(t.replace('0.3.41','0.3.42'))

build=app/'build.gradle'
b=build.read_text()
if "versionCode 44" not in b or "versionName '0.3.41'" not in b:
    raise SystemExit('version anchors missing')
b=b.replace('versionCode 44','versionCode 45',1).replace("versionName '0.3.41'","versionName '0.3.42'",1)
build.write_text(b)

legacy='<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@drawable/app_icon_convoi" android:gravity="fill" />\n'
(res/'mipmap-anydpi/ic_launcher.xml').write_text(legacy)
(res/'mipmap-anydpi/ic_launcher_round.xml').write_text(legacy)
(res/'drawable/ic_launcher_foreground.xml').write_text('<inset xmlns:android="http://schemas.android.com/apk/res/android" android:drawable="@drawable/app_icon_convoi" android:inset="14dp" />\n')
(res/'values/colors.xml').write_text('<resources><color name="launcher_bg">#00000000</color></resources>\n')

# Restore the normal read-only release workflow in the canonical commit.
workflow=repo/'.github/workflows/mode-convoi-clean-release.yml'
workflow.write_text('''name: Mode Convoi Clean Release Build\n\non:\n  push:\n    branches: [mode-convoi-apk-build]\n    paths:\n      - '.github/workflows/mode-convoi-clean-release.yml'\n      - 'mode-convoi-clean/**'\n  workflow_dispatch:\n\npermissions:\n  contents: read\n\njobs:\n  build-release:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n        with:\n          ref: mode-convoi-apk-build\n\n      - uses: actions/setup-java@v4\n        with:\n          distribution: temurin\n          java-version: '17'\n\n      - uses: android-actions/setup-android@v3\n\n      - name: Install Android SDK 35\n        run: sdkmanager 'platforms;android-35' 'build-tools;35.0.0'\n\n      - uses: gradle/actions/setup-gradle@v4\n        with:\n          gradle-version: '8.9'\n\n      - name: Build release directly from canonical source\n        run: gradle -p mode-convoi-clean/android --no-daemon assembleRelease\n\n      - name: Package unsigned release\n        shell: bash\n        run: |\n          set -euo pipefail\n          cp mode-convoi-clean/android/app/build/outputs/apk/release/app-release-unsigned.apk ModeConvoi-clean-release-unsigned.apk\n          sha256sum ModeConvoi-clean-release-unsigned.apk > ModeConvoi-clean-release-unsigned.apk.sha256\n\n      - uses: actions/upload-artifact@v4\n        with:\n          name: ModeConvoi-clean-release-unsigned\n          if-no-files-found: error\n          path: |\n            ModeConvoi-clean-release-unsigned.apk\n            ModeConvoi-clean-release-unsigned.apk.sha256\n\n      - name: Package official Android apksigner\n        shell: bash\n        run: |\n          set -euo pipefail\n          mkdir -p apksigner-tool/lib\n          cp "$ANDROID_HOME/build-tools/35.0.0/apksigner" apksigner-tool/apksigner\n          cp "$ANDROID_HOME/build-tools/35.0.0/lib/apksigner.jar" apksigner-tool/lib/apksigner.jar\n          chmod +x apksigner-tool/apksigner\n\n      - uses: actions/upload-artifact@v4\n        with:\n          name: Android-apksigner-35.0.0\n          if-no-files-found: error\n          path: apksigner-tool/\n''')

# Remove all one-time transport files before the canonical commit.
shutil.rmtree(tmp)
print('Mode Convoi 0.3.42 migration applied')
