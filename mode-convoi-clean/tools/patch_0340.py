from pathlib import Path
import base64

root=Path(__file__).resolve().parents[1]
android=root/'android'
java=android/'app/src/main/java/ch/piiwii/modeconvoi'
main_path=java/'MainActivity.java'
pack_path=java/'VolkswagenIconPack.java'
session_path=java/'ConvoySessionManager.java'
map_path=android/'app/src/main/assets/convoy_map.html'
gradle_path=android/'app/build.gradle'
api_path=java/'ConvoyApi.java'
sprite_path=android/'app/src/main/res/drawable-nodpi/vw_sprite_192.webp'


def once(text, old, new, label):
    n=text.count(old)
    if n!=1:
        raise SystemExit(f'{label}: expected 1 anchor, found {n}')
    return text.replace(old,new,1)

# Decode the one-time textual base64 transport into a real WebP resource.
raw=sprite_path.read_text().strip()
try:
    decoded=base64.b64decode(raw,validate=True)
except Exception as exc:
    raise SystemExit(f'Volkswagen sprite base64 decode failed: {exc}')
if not (decoded.startswith(b'RIFF') and decoded[8:12]==b'WEBP'):
    raise SystemExit('Volkswagen sprite is not a valid WEBP RIFF payload')
sprite_path.write_bytes(decoded)
for temp_name in ('vw_sprite_192_binary.txt','.keep-vw-binary'):
    p=sprite_path.parent/temp_name
    if p.exists(): p.unlink()

pack='''package ch.piiwii.modeconvoi;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public final class VolkswagenIconPack {
    private static final int CELL=192;
    private static final int COLS=5;
    private static Bitmap spriteCache;
    private static final Bitmap[] bitmapCache=new Bitmap[10];

    public static final class Item {
        public final String id;
        public final String label;
        private final int index;
        Item(String id,String label,int index){this.id=id;this.label=label;this.index=index;}
        public Bitmap bitmap(Resources resources){return bitmapAt(resources,index);}
    }

    private static final Item[] ITEMS={
        new Item("vw:beetle","Coccinelle",0),
        new Item("vw:t1","Combi T1",1),
        new Item("vw:golf1","Golf 1 GTI",2),
        new Item("vw:golf2","Golf 2 GTI",3),
        new Item("vw:golf3","Golf 3",4),
        new Item("vw:golf4","Golf 4",5),
        new Item("vw:polo6n2","Polo 6N2 GTI",6),
        new Item("vw:passatb5","Passat B5 Variant",7),
        new Item("vw:jetta2","Jetta 2",8),
        new Item("vw:newbeetle","New Beetle",9)
    };

    private VolkswagenIconPack(){}
    public static Item[] items(){return ITEMS.clone();}
    public static Item find(String id){if(id==null)return null;for(Item item:ITEMS)if(item.id.equals(id))return item;return null;}
    public static boolean isVolkswagen(String id){return find(id)!=null;}
    public static int indexOf(String id){Item item=find(id);return item==null?-1:item.index;}
    public static Bitmap bitmapFor(Resources resources,String id){Item item=find(id);return item==null?null:bitmapAt(resources,item.index);}

    private static synchronized Bitmap bitmapAt(Resources resources,int index){
        if(index<0||index>=bitmapCache.length||resources==null)return null;
        if(bitmapCache[index]!=null&&!bitmapCache[index].isRecycled())return bitmapCache[index];
        if(spriteCache==null||spriteCache.isRecycled())spriteCache=BitmapFactory.decodeResource(resources,R.drawable.vw_sprite_192);
        if(spriteCache==null)return null;
        int x=(index%COLS)*CELL,y=(index/COLS)*CELL;
        if(x+CELL>spriteCache.getWidth()||y+CELL>spriteCache.getHeight())return null;
        bitmapCache[index]=Bitmap.createBitmap(spriteCache,x,y,CELL,CELL);
        return bitmapCache[index];
    }
}
'''
pack_path.write_text(pack)
for i in range(1,11):
    old=java/f'VwIcon{i:02d}.java'
    if old.exists(): old.unlink()

main=main_path.read_text()
old_avatar='''    private View participantAvatar(JSONObject p,int size,int fallbackColor){
        String image=p==null?prefs.get("profileVehicleImage",""):p.optString("vehicleImage","");
        if(image!=null&&!image.isEmpty()){
            try{
                Bitmap b=VehicleImageCache.decode(image);
                if(b!=null){
                    boolean transparentVehicle=image.startsWith("iVBOR");
                    ImageView iv=new ImageView(this);
                    iv.setScaleType(transparentVehicle?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_CROP);
                    iv.setPadding(transparentVehicle?dp(3):0,transparentVehicle?dp(3):0,transparentVehicle?dp(3):0,transparentVehicle?dp(3):0);
                    iv.setImageBitmap(b);
                    iv.setBackground(roundBg(control,participantMarkerColor(p,fallbackColor),size/2,1));
                    iv.setClipToOutline(!transparentVehicle);
                    return iv;
                }
            }catch(Exception ignored){}
        }
        String icon=p==null?prefs.get("profileVehicleIcon","🚗"):p.optString("vehicleIcon","🚗");if(icon.isEmpty()||VolkswagenIconPack.isVolkswagen(icon))icon="🚗";
        TextView v=text(icon,size>=46?24:19,false,participantMarkerColor(p,fallbackColor));v.setGravity(Gravity.CENTER);v.setBackground(roundBg(control,participantMarkerColor(p,fallbackColor),size/2,1));return v;
    }
'''
new_avatar='''    private View participantAvatar(JSONObject p,int size,int fallbackColor){
        String icon=p==null?prefs.get("profileVehicleIcon","🚗"):p.optString("vehicleIcon","🚗");
        if(VolkswagenIconPack.isVolkswagen(icon)){
            try{
                Bitmap b=VolkswagenIconPack.bitmapFor(getResources(),icon);
                if(b!=null){
                    ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.FIT_CENTER);iv.setPadding(dp(2),dp(2),dp(2),dp(2));iv.setImageBitmap(b);
                    iv.setBackground(roundBg(control,participantMarkerColor(p,fallbackColor),size/2,1));iv.setClipToOutline(false);return iv;
                }
            }catch(Exception ignored){}
        }
        String image=p==null?prefs.get("profileVehicleImage",""):p.optString("vehicleImage","");
        if(image!=null&&!image.isEmpty()){
            try{Bitmap b=VehicleImageCache.decode(image);if(b!=null){ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);iv.setImageBitmap(b);iv.setBackground(roundBg(control,participantMarkerColor(p,fallbackColor),size/2,1));iv.setClipToOutline(true);return iv;}}catch(Exception ignored){}
        }
        if(icon.isEmpty())icon="🚗";
        TextView v=text(icon,size>=46?24:19,false,participantMarkerColor(p,fallbackColor));v.setGravity(Gravity.CENTER);v.setBackground(roundBg(control,participantMarkerColor(p,fallbackColor),size/2,1));return v;
    }
'''
main=once(main,old_avatar,new_avatar,'participant avatar HD Volkswagen')

main=once(main,
'''        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),dp(14));scroll.addView(box);
        vehiclePreview=new FrameLayout(this);vehiclePreview.setPadding(dp(8),dp(8),dp(8),dp(8));box.addView(vehiclePreview,new LinearLayout.LayoutParams(-1,dp(92)));refreshVehiclePreview();
''',
'''        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),dp(14));scroll.addView(box);
        if(VolkswagenIconPack.isVolkswagen(prefs.get("profileVehicleIcon","")))prefs.remove("profileVehicleImage");
        final ArrayList<View> vehicleChoiceViews=new ArrayList<>();
        vehiclePreview=new FrameLayout(this);vehiclePreview.setPadding(dp(8),dp(8),dp(8),dp(8));box.addView(vehiclePreview,new LinearLayout.LayoutParams(-1,dp(92)));refreshVehiclePreview();
''','appearance dialog init')

old_generic='''            final String ic=all[i];TextView b=text(ic,25,false,fg);b.setGravity(Gravity.CENTER);
            boolean selected=ic.equals(prefs.get("profileVehicleIcon","🚗")) && prefs.get("profileVehicleImage","").isEmpty();
            b.setBackground(roundBg(control,selected?accent:border,12,selected?2:1));
            b.setOnClickListener(v->{prefs.put("profileVehicleIcon",ic);prefs.remove("profileVehicleImage");refreshVehiclePreview();});
'''
new_generic='''            final String ic=all[i];TextView b=text(ic,25,false,fg);b.setGravity(Gravity.CENTER);b.setTag("vehicle-choice:"+ic);vehicleChoiceViews.add(b);
            boolean selected=ic.equals(prefs.get("profileVehicleIcon","🚗")) && prefs.get("profileVehicleImage","").isEmpty();
            b.setBackground(roundBg(control,selected?accent:border,12,selected?2:1));
            b.setOnClickListener(v->{prefs.put("profileVehicleIcon",ic);prefs.remove("profileVehicleImage");refreshVehicleChoiceSelection(vehicleChoiceViews);refreshVehiclePreview();});
'''
main=once(main,old_generic,new_generic,'generic vehicle selection refresh')

old_vw='''            final VolkswagenIconPack.Item item=vwItems[i];
            FrameLayout slot=new FrameLayout(this);slot.setContentDescription(item.label);
            boolean selected=item.id.equals(selectedVehicleIcon);
            slot.setBackground(roundBg(control,selected?accent:border,12,selected?2:1));
            ImageView carIcon=new ImageView(this);carIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);carIcon.setPadding(dp(4),dp(4),dp(4),dp(4));carIcon.setImageBitmap(item.bitmap());
            slot.addView(carIcon,new FrameLayout.LayoutParams(-1,-1));
            slot.setOnClickListener(v->{prefs.put("profileVehicleIcon",item.id);prefs.put("profileVehicleImage",item.base64());refreshVehiclePreview();});
'''
new_vw='''            final VolkswagenIconPack.Item item=vwItems[i];
            FrameLayout slot=new FrameLayout(this);slot.setContentDescription(item.label);slot.setTag("vehicle-choice:"+item.id);vehicleChoiceViews.add(slot);
            boolean selected=item.id.equals(selectedVehicleIcon);
            slot.setBackground(roundBg(control,selected?accent:border,12,selected?2:1));
            ImageView carIcon=new ImageView(this);carIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);carIcon.setPadding(dp(2),dp(2),dp(2),dp(2));carIcon.setImageBitmap(item.bitmap(getResources()));
            slot.addView(carIcon,new FrameLayout.LayoutParams(-1,-1));
            slot.setOnClickListener(v->{prefs.put("profileVehicleIcon",item.id);prefs.remove("profileVehicleImage");refreshVehicleChoiceSelection(vehicleChoiceViews);refreshVehiclePreview();});
'''
main=once(main,old_vw,new_vw,'Volkswagen picker HD selection')
main=once(main,'10 modèles Volkswagen · appuie sur une voiture pour l\'utiliser sur la carte.','10 modèles Volkswagen HD · appuie sur une voiture pour l\'utiliser sur la carte.','Volkswagen hint')

old_colors='''        GridLayout colors=new GridLayout(this);colors.setColumnCount(4);String[] cs={"#FFB514","#EF4444","#3B82F6","#22C55E","#A855F7","#F97316","#E5E7EB","#111827"};
        for(String c:cs){
            boolean selected=c.equalsIgnoreCase(prefs.get("profileVehicleMarkerColor","#FFB514"));
            TextView dot=text("●",38,false,Color.parseColor(c));dot.setGravity(Gravity.CENTER);dot.setBackground(roundBg(control,selected?accent:border,14,selected?2:1));
            dot.setOnClickListener(v->{prefs.put("profileVehicleMarkerColor",c);refreshVehiclePreview();});
'''
new_colors='''        GridLayout colors=new GridLayout(this);colors.setColumnCount(4);String[] cs={"#FFB514","#EF4444","#3B82F6","#22C55E","#A855F7","#F97316","#E5E7EB","#111827"};
        final ArrayList<View> markerColorViews=new ArrayList<>();
        for(String c:cs){
            boolean selected=c.equalsIgnoreCase(prefs.get("profileVehicleMarkerColor","#FFB514"));
            TextView dot=text("●",38,false,Color.parseColor(c));dot.setGravity(Gravity.CENTER);dot.setTag("marker-color:"+c);markerColorViews.add(dot);dot.setBackground(roundBg(control,selected?accent:border,14,selected?2:1));
            dot.setOnClickListener(v->{prefs.put("profileVehicleMarkerColor",c);refreshMarkerColorSelection(markerColorViews);refreshVehiclePreview();});
'''
main=once(main,old_colors,new_colors,'marker color live selection')

old_preview='''    private void refreshVehiclePreview(){if(vehiclePreview==null)return;vehiclePreview.removeAllViews();View av=participantAvatar(null,72,accent);FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(72),dp(72),Gravity.CENTER);vehiclePreview.addView(av,ap);}
'''
new_preview='''    private void refreshVehicleChoiceSelection(List<View> views){
        String selected=prefs.get("profileVehicleIcon","🚗");boolean custom=!prefs.get("profileVehicleImage","").isEmpty();
        for(View v:views){Object tag=v.getTag();if(!(tag instanceof String)||!((String)tag).startsWith("vehicle-choice:"))continue;String choice=((String)tag).substring("vehicle-choice:".length());boolean active=!custom&&choice.equals(selected);v.setBackground(roundBg(control,active?accent:border,12,active?2:1));}
    }
    private void refreshMarkerColorSelection(List<View> views){
        String selected=prefs.get("profileVehicleMarkerColor","#FFB514");
        for(View v:views){Object tag=v.getTag();if(!(tag instanceof String)||!((String)tag).startsWith("marker-color:"))continue;String value=((String)tag).substring("marker-color:".length());boolean active=value.equalsIgnoreCase(selected);v.setBackground(roundBg(control,active?accent:border,14,active?2:1));}
    }
    private void refreshVehiclePreview(){if(vehiclePreview==null)return;vehiclePreview.removeAllViews();View av=participantAvatar(null,72,accent);FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(72),dp(72),Gravity.CENTER);vehiclePreview.addView(av,ap);}
'''
main=once(main,old_preview,new_preview,'selection helper methods')
main=once(main,'Mode Convoi 0.3.39','Mode Convoi 0.3.40','about version')
main_path.write_text(main)

session=session_path.read_text()
old_participant='''    private JSONObject participantBody() throws Exception {
        return new JSONObject()
                .put("name", prefs.get("profileName", "Conducteur"))
                .put("vehicle", prefs.get("profileVehicle", "Véhicule"))
                .put("vehicleColor", prefs.get("profileColor", ""))
                .put("vehicleIcon", prefs.get("profileVehicleIcon", "🚗"))
                .put("vehicleMarkerColor", prefs.get("profileVehicleMarkerColor", "#FFB514"))
                .put("vehicleImage", prefs.get("profileVehicleImage", ""));
    }
'''
new_participant='''    private JSONObject participantBody() throws Exception {
        String icon=prefs.get("profileVehicleIcon", "🚗");
        String image=VolkswagenIconPack.isVolkswagen(icon)?"":prefs.get("profileVehicleImage", "");
        return new JSONObject()
                .put("name", prefs.get("profileName", "Conducteur"))
                .put("vehicle", prefs.get("profileVehicle", "Véhicule"))
                .put("vehicleColor", prefs.get("profileColor", ""))
                .put("vehicleIcon", icon)
                .put("vehicleMarkerColor", prefs.get("profileVehicleMarkerColor", "#FFB514"))
                .put("vehicleImage", image);
    }
'''
session=once(session,old_participant,new_participant,'profile Volkswagen image transport')
session_path.write_text(session)

m=map_path.read_text()
m=once(m,
'.marker{position:absolute;transform:translate(-18px,-18px);z-index:5;pointer-events:auto}.car{font-size:25px;filter:drop-shadow(0 2px 3px #000)}.photo{width:34px;height:34px;border-radius:50%;object-fit:cover;border:2px solid #fff;box-shadow:0 2px 5px #000a;background:#222}.vehicle-art{width:42px;height:42px;object-fit:contain;filter:drop-shadow(0 2px 3px #000b);background:transparent}',
'.marker{position:absolute;transform:translate(-18px,-18px);z-index:5;pointer-events:auto}.car{font-size:25px;filter:drop-shadow(0 2px 3px #000)}.photo{width:34px;height:34px;border-radius:50%;object-fit:cover;border:2px solid #fff;box-shadow:0 2px 5px #000a;background:#222}.vehicle-art{width:42px;height:42px;object-fit:contain;filter:drop-shadow(0 2px 3px #000b);background:transparent}.vw-art{width:42px;height:42px;background-image:url("file:///android_res/drawable/vw_sprite_192.webp");background-size:210px 84px;background-repeat:no-repeat;filter:drop-shadow(0 2px 3px #000b)}',
'map Volkswagen sprite css')
m=once(m,
"function setStatus(t,kind=''){statusEl.textContent=t;statusEl.className=kind}\n",
"function setStatus(t,kind=''){statusEl.textContent=t;statusEl.className=kind}\nconst VW_ICON_INDEX={'vw:beetle':0,'vw:t1':1,'vw:golf1':2,'vw:golf2':3,'vw:golf3':4,'vw:golf4':5,'vw:polo6n2':6,'vw:passatb5':7,'vw:jetta2':8,'vw:newbeetle':9};\nfunction vwVisual(icon){const idx=VW_ICON_INDEX[icon];if(idx===undefined)return '';const col=idx%5,row=Math.floor(idx/5);return `<div class=\"vw-art\" style=\"background-position:${-col*42}px ${-row*42}px\"></div>`}\n",
'map Volkswagen sprite lookup')
old_add='''function addMarker(lat,lon,label,kind='',stale=false,icon='🚗',image='',markerColor=''){if(!validPoint(lat,lon))return;const tl=topLeft(),p=project(lat,lon),m=document.createElement('div');m.className='marker '+kind+(stale?' stale':'');m.style.left=(p.x-tl.x)+'px';m.style.top=(p.y-tl.y)+'px';const safeImg=(image&&/^[A-Za-z0-9+/=]+$/.test(image)&&image.length<30000)?image:'';const png=safeImg.startsWith('iVBOR');const visual=kind.includes('rally')?'<div class="car">📍</div>':(safeImg?`<img class="${png?'vehicle-art':'photo'}" src="data:image/${png?'png':'jpeg'};base64,${safeImg}">`:`<div class="car">${esc(icon||'🚗')}</div>`);m.innerHTML=`${visual}<div class="label">${esc(label)}</div>`;if(markerColor&&/^#[0-9A-Fa-f]{6}$/.test(markerColor)){const lab=m.querySelector('.label');if(lab)lab.style.borderColor=markerColor;const ph=m.querySelector('.photo');if(ph)ph.style.borderColor=markerColor;}markersEl.appendChild(m)}'''
new_add='''function addMarker(lat,lon,label,kind='',stale=false,icon='🚗',image='',markerColor=''){if(!validPoint(lat,lon))return;const tl=topLeft(),p=project(lat,lon),m=document.createElement('div');m.className='marker '+kind+(stale?' stale':'');m.style.left=(p.x-tl.x)+'px';m.style.top=(p.y-tl.y)+'px';const safeImg=(image&&/^[A-Za-z0-9+/=]+$/.test(image)&&image.length<30000)?image:'';const png=safeImg.startsWith('iVBOR');const vw=vwVisual(icon);const visual=kind.includes('rally')?'<div class="car">📍</div>':(vw|| (safeImg?`<img class="${png?'vehicle-art':'photo'}" src="data:image/${png?'png':'jpeg'};base64,${safeImg}">`:`<div class="car">${esc(icon||'🚗')}</div>`));m.innerHTML=`${visual}<div class="label">${esc(label)}</div>`;if(markerColor&&/^#[0-9A-Fa-f]{6}$/.test(markerColor)){const lab=m.querySelector('.label');if(lab)lab.style.borderColor=markerColor;const ph=m.querySelector('.photo');if(ph)ph.style.borderColor=markerColor;}markersEl.appendChild(m)}'''
m=once(m,old_add,new_add,'map prioritizes local Volkswagen sprite')
map_path.write_text(m)

g=gradle_path.read_text();g=once(g,'versionCode 42','versionCode 43','versionCode');g=once(g,"versionName '0.3.39'","versionName '0.3.40'",'versionName');gradle_path.write_text(g)
a=api_path.read_text();a=once(a,'ModeConvoi-Android/0.3.39','ModeConvoi-Android/0.3.40','user agent');api_path.write_text(a)

# post conditions
assert sprite_path.stat().st_size>100000 and sprite_path.read_bytes().startswith(b'RIFF')
assert 'item.bitmap(getResources())' in main
assert 'refreshVehicleChoiceSelection(vehicleChoiceViews)' in main
assert 'refreshMarkerColorSelection(markerColorViews)' in main
assert 'VolkswagenIconPack.isVolkswagen(icon)?""' in session
assert 'file:///android_res/drawable/vw_sprite_192.webp' in m
assert 'versionCode 43' in g and "0.3.40" in g
print('Mode Convoi 0.3.40 Volkswagen HD + selection migration ready')
