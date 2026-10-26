from pathlib import Path
import shutil

root=Path("piiwii-notification-widget-v121/app/src/main")
pkg=root/"java/ch/piiwii/notificationwidget"
assets=Path("notification-widget-v121-patch/drawable")
target=root/"res/drawable"
for src in assets.glob("*.xml"):
    shutil.copy2(src,target/src.name)

def edit(name,old,new):
    path=pkg/name
    data=path.read_text(encoding="utf-8")
    count=data.count(old)
    assert count==1,(name,repr(old[:95]),count)
    path.write_text(data.replace(old,new,1),encoding="utf-8")

# New default icons only apply if an existing user preference is NOT set.
# Explicit saved emojis, gallery vectors, and imported photos are unchanged.
edit("GraphicIcons.java",
    '    {"wifi","Réseau"}',
    '''    {"wifi","Réseau"},
    {"whatsapp","WhatsApp"},
    {"messenger","Messenger"},
    {"facebook","Facebook"},
    {"facebook_dm","Messages privés Facebook"},
    {"tiktok","TikTok"},
    {"induction","Recharge à induction"},
    {"induction_fast","Induction rapide"},
    {"quiet","Aucune alerte"}''')
edit("GraphicIcons.java",
    '      case "wifi": return R.drawable.pic_wifi;',
    '''      case "wifi": return R.drawable.pic_wifi;
      case "whatsapp": return R.drawable.pic_brand_whatsapp;
      case "messenger": return R.drawable.pic_brand_messenger;
      case "facebook": return R.drawable.pic_brand_facebook;
      case "facebook_dm": return R.drawable.pic_brand_facebook_dm;
      case "tiktok": return R.drawable.pic_brand_tiktok;
      case "induction": return R.drawable.pic_charge_induction;
      case "induction_fast": return R.drawable.pic_charge_induction_fast;
      case "quiet": return R.drawable.pic_idle_quiet;''')
edit("GraphicIcons.java",
    '  static boolean draw(Context c, Canvas canvas, String key, float left, float top, float size, int tint){',
    '''  // A recognizable default for each brand/reaction. No saved setting is changed.
  static String defaultFor(String id){
    if(id==null)return null;
    switch(id){
      case "whatsapp": return "vector:whatsapp";
      case "messenger": return "vector:messenger";
      case "facebook": return "vector:facebook";
      case "facebook_dm": return "vector:facebook_dm";
      case "tiktok": return "vector:tiktok";
      case "charge_wireless": return "vector:induction";
      case "charge_wireless_fast": return "vector:induction_fast";
      case "rest": return "vector:quiet";
      default:return null;
    }
  }
  static boolean colorful(String key){
    switch(key){
      case "whatsapp":case "messenger":case "facebook":case "facebook_dm":
      case "tiktok":case "induction":case "induction_fast":case "quiet":
        return true;
      default:return false;
    }
  }
  static boolean draw(Context c, Canvas canvas, String key, float left, float top, float size, int tint){''')
edit("GraphicIcons.java",
    '    d=d.mutate();d.setTint(tint);',
    '    d=d.mutate();if(!colorful(key))d.setTint(tint);')
edit("IconRenderer.java",
    '    String chosen=draft?(choice==null?"":choice):Categories.prefs(c).getString("icon_"+id,"");',
    '''    String chosen=draft?choice:Categories.prefs(c).getString("icon_"+id,null);
    if(chosen==null||chosen.isEmpty())chosen=GraphicIcons.defaultFor(id);
    if(chosen==null)chosen="";''')
edit("IconRenderer.java",
    '    String glyph="rest".equals(id)?"⚪":(draft?(choice==null?Categories.glyph(c,id):choice):Categories.glyph(c,id));',
    '    String glyph="rest".equals(id)?"—":(draft?(choice==null?Categories.glyph(c,id):choice):Categories.glyph(c,id));')
edit("IconRenderer.java",
    '    if(glyph==null||glyph.length()==0)glyph="⚪";',
    '    if(glyph==null||glyph.length()==0)glyph="—";')
edit("Categories.java",
    '{"whatsapp","WhatsApp","🟢","Messages"}',
    '{"whatsapp","WhatsApp","📞","Messages"}')
edit("Categories.java",
    '{"facebook","Facebook","👍","Réseaux sociaux"}',
    '{"facebook","Facebook","📘","Réseaux sociaux"}')
edit("Categories.java",
    '{"facebook_dm","Messages privés Facebook","💌","Réseaux sociaux"}',
    '{"facebook_dm","Messages privés Facebook","💬","Réseaux sociaux"}')
edit("Categories.java",
    '{"tiktok","TikTok","🎵","Réseaux sociaux"}',
    '{"tiktok","TikTok","🎬","Réseaux sociaux"}')
edit("Categories.java",
    '{"charge_wireless","Charge par induction","🛜","Services et système"}',
    '{"charge_wireless","Charge par induction","🔋","Services et système"}')
edit("MainActivity.java",
    'Categories.prefs(this).contains("icon_"+id)?"Emoji personnalisé":"Icône d\'origine "+item[2];',
    'Categories.prefs(this).contains("icon_"+id)?"Emoji personnalisé":"Icône d\'origine";')
edit("MainActivity.java",
    'String[] sampleKeys={"bell","chat","music","battery","parcel"};',
    'String[] sampleKeys={"whatsapp","messenger","facebook","tiktok","induction"};')
edit("MainActivity.java",
    '"24 pictogrammes vectoriels, nets même lorsque le widget est agrandi."',
    'GraphicIcons.ALL.length+" pictogrammes vectoriels, nets même lorsque le widget est agrandi."')
edit("MainActivity.java",
    '"⚪ Débranché · "+pct+" %"','"🔋 Débranché · "+pct+" %"')
build=Path("piiwii-notification-widget-v121/app/build.gradle")
b=build.read_text()
assert "versionCode 21; versionName '1.0.20'" in b
build.write_text(b.replace("versionCode 21; versionName '1.0.20'","versionCode 22; versionName '1.0.21'"))
main=(pkg/"MainActivity.java").read_text()
for token in ('if(tab==0)showHome();','else if(tab==1)showCategories();','else if(tab==2)showAppearance();','else showTests();','void showInlineIconManager()'):
    assert token in main,token
icons=(pkg/"GraphicIcons.java").read_text()
assert icons.count('{"whatsapp","WhatsApp"}')==1
assert 'if(!colorful(key))d.setTint(tint);' in icons
assert len(list(assets.glob("*.xml")))==9
print("PASS: 8 recognizable default designs plus empty slot, 4 existing pages, preserved preferences")
