from pathlib import Path
import base64, hashlib, shutil, subprocess

repo=Path.cwd()
tmp=repo/'.tmp0342'
app=repo/'mode-convoi-clean/android/app'
res=app/'src/main/res'
java=app/'src/main/java/ch/piiwii/modeconvoi'

def decode_parts(prefix):
    parts=sorted(tmp.glob(prefix+'_*.b64'))
    if not parts:
        raise SystemExit(f'missing chunks for {prefix}')
    return base64.b64decode(''.join(p.read_text().strip() for p in parts), validate=True)

vwmore=decode_parts('vwmore')
icon=decode_parts('icon')
assert hashlib.sha256(vwmore).hexdigest()=='a04dd15f758843d13de20b721fece26bd3748e9d6b77b34a6254829f6336c2af'
assert hashlib.sha256(icon).hexdigest()=='d4f14a1f7f3a884fbb16506199750358c18fc5b54979932a9434a173b4673cc1'
(res/'drawable-nodpi/vw_sprite_more_192.webp').write_bytes(vwmore)
(res/'drawable-nodpi/app_icon_convoi.webp').write_bytes(icon)

pack='''package ch.piiwii.modeconvoi;\n\nimport android.content.res.Resources;\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\n\npublic final class VolkswagenIconPack {\n    private static final int CELL=192;\n    private static final int COLS=5;\n    private static Bitmap spriteCache;\n    private static Bitmap spriteMoreCache;\n    private static final Bitmap[] bitmapCache=new Bitmap[20];\n\n    public static final class Item {\n        public final String id;\n        public final String label;\n        private final int index;\n        Item(String id,String label,int index){this.id=id;this.label=label;this.index=index;}\n        public Bitmap bitmap(Resources resources){return bitmapAt(resources,index);}\n    }\n\n    private static final Item[] ITEMS={\n        new Item("vw:beetle","Coccinelle",0),\n        new Item("vw:t1","Combi T1",1),\n        new Item("vw:golf1","Golf 1 GTI",2),\n        new Item("vw:golf2","Golf 2 GTI",3),\n        new Item("vw:golf3","Golf 3",4),\n        new Item("vw:golf4","Golf 4",5),\n        new Item("vw:polo6n2","Polo 6N2 GTI",6),\n        new Item("vw:passatb5","Passat B5 Variant",7),\n        new Item("vw:jetta2","Jetta 2",8),\n        new Item("vw:newbeetle","New Beetle",9),\n        new Item("vw:karmannghia","Karmann Ghia",10),\n        new Item("vw:type3fastback","Type 3 Fastback",11),\n        new Item("vw:thing181","Type 181",12),\n        new Item("vw:t2","Combi T2",13),\n        new Item("vw:t3","Combi T3",14),\n        new Item("vw:scirocco1","Scirocco 1",15),\n        new Item("vw:scirocco2","Scirocco 2",16),\n        new Item("vw:corrado","Corrado VR6",17),\n        new Item("vw:lupogti","Lupo GTI",18),\n        new Item("vw:polog40","Polo G40",19)\n    };\n\n    private VolkswagenIconPack(){}\n    public static Item[] items(){return ITEMS.clone();}\n    public static Item find(String id){if(id==null)return null;for(Item item:ITEMS)if(item.id.equals(id))return item;return null;}\n    public static boolean isVolkswagen(String id){return find(id)!=null;}\n    public static int indexOf(String id){Item item=find(id);return item==null?-1:item.index;}\n    public static Bitmap bitmapFor(Resources resources,String id){Item item=find(id);return item==null?null:bitmapAt(resources,item.index);}\n\n    private static synchronized Bitmap bitmapAt(Resources resources,int index){\n        if(index<0||index>=bitmapCache.length||resources==null)return null;\n        if(bitmapCache[index]!=null&&!bitmapCache[index].isRecycled())return bitmapCache[index];\n        Bitmap sprite; int localIndex;\n        if(index<10){\n            if(spriteCache==null||spriteCache.isRecycled())spriteCache=BitmapFactory.decodeResource(resources,R.drawable.vw_sprite_192);\n            sprite=spriteCache;localIndex=index;\n        }else{\n            if(spriteMoreCache==null||spriteMoreCache.isRecycled())spriteMoreCache=BitmapFactory.decodeResource(resources,R.drawable.vw_sprite_more_192);\n            sprite=spriteMoreCache;localIndex=index-10;\n        }\n        if(sprite==null)return null;\n        int x=(localIndex%COLS)*CELL,y=(localIndex/COLS)*CELL;\n        if(x+CELL>sprite.getWidth()||y+CELL>sprite.getHeight())return null;\n        bitmapCache[index]=Bitmap.createBitmap(sprite,x,y,CELL,CELL);\n        return bitmapCache[index];\n    }\n}\n'''
(java/'VolkswagenIconPack.java').write_text(pack)

main=java/'MainActivity.java'
s=main.read_text()
old="10 modèles Volkswagen HD · appuie sur une voiture pour l'utiliser sur la carte."
new="20 modèles Volkswagen HD · appuie sur une voiture pour l'utiliser sur la carte."
if old not in s: raise SystemExit('VW hint anchor missing')
main.write_text(s.replace(old,new,1))

for p in java.glob('*.java'):
    t=p.read_text()
    if '0.3.41' in t:p.write_text(t.replace('0.3.41','0.3.42'))

build=app/'build.gradle'; b=build.read_text()
if "versionCode 44" not in b or "versionName '0.3.41'" not in b: raise SystemExit('version anchors missing')
build.write_text(b.replace('versionCode 44','versionCode 45',1).replace("versionName '0.3.41'","versionName '0.3.42'",1))

legacy='<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@drawable/app_icon_convoi" android:gravity="fill" />\n'
(res/'mipmap-anydpi/ic_launcher.xml').write_text(legacy)
(res/'mipmap-anydpi/ic_launcher_round.xml').write_text(legacy)
(res/'drawable/ic_launcher_foreground.xml').write_text('<inset xmlns:android="http://schemas.android.com/apk/res/android" android:drawable="@drawable/app_icon_convoi" android:inset="14dp" />\n')
(res/'values/colors.xml').write_text('<resources><color name="launcher_bg">#00000000</color></resources>\n')

# Restore the proven read-only workflow exactly from the last clean 0.3.41 source.
workflow=repo/'.github/workflows/mode-convoi-clean-release.yml'
normal=subprocess.check_output(['git','show','fbd30341c663bb0831088af7d1d25fbcc178a10f:.github/workflows/mode-convoi-clean-release.yml'])
workflow.write_bytes(normal)
shutil.rmtree(tmp)
print('Mode Convoi 0.3.42 migration applied')
