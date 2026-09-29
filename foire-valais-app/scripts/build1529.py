import requests, json, os
from urllib.parse import urlencode, urljoin
from datetime import datetime

h={'User-Agent':'Mozilla/5.0','Accept':'application/json'}
pairs=[('site','2'),('locale','fr'),('types[]','programs'),('date_start','2026-10-02'),('date_end','2026-10-11'),('date_search_on_period','true'),('count','500'),('page','1'),('orderByProperty','dateFrom'),('orderByDirection','ASC'),('embed[]','spaces'),('embed[]','categories'),('embed[]','tags')]
api='https://www.foireduvalais.ch/api/search?'+urlencode(pairs)
rr=requests.get(api,headers=h,timeout=60)
rr.raise_for_status()
data=rr.json()
items=data.get('hydra:member',[])
base=datetime(2026,10,2)
space_map={973:('Innothèque','INNOTHÈQUE'),1527:('Salle Bonne de Bourbon','BONNE DE BOURBON'),2008:('Espace Conférences','CONFÉRENCES'),520:('Halles commerciales','HALLES')}

def category(x):
    vals=[]
    for c in x.get('categories') or []:
        if not isinstance(c,dict): continue
        t=(c.get('aio:seo') or {}).get('title') or c.get('title') or c.get('name')
        if t and t not in vals: vals.append(t)
    return ' / '.join(vals) if vals else 'Animation'

def source(x):
    try:return x['aio:urls']['2']['langs']['fr'][0]
    except Exception:return None

def image(x):
    imgs=x.get('aio:images') or []
    if not imgs:return None
    fm=imgs[0].get('formats') or {}
    for key in ('medium_16_9','normal_16_9','medium_3_2','medium_1_1','normal_3_2','normal_1_1'):
        u=((fm.get(key) or {}).get('url'))
        if u:return urljoin('https://www.foireduvalais.ch',u)
    return None

def jesc(value):
    if value is None:return 'null'
    s=str(value).replace('\\','\\\\').replace('"','\\"').replace('\r',' ').replace('\n','\\n')
    return '"'+s+'"'

rows=[]
for x in items:
    spaces=[sp for sp in (x.get('spaces') or []) if isinstance(sp,dict)]
    ids={sp.get('id') for sp in spaces}
    if 967 in ids or 975 in ids: continue
    chosen=None
    for sid,(place,zone) in space_map.items():
        if sid in ids:
            chosen=(sid,place,zone); break
    if chosen is None:
        if spaces: continue
        chosen=(0,'Foire du Valais','AUTRES')
    dfraw=x.get('aio:dateFrom'); dtraw=x.get('aio:dateTo')
    if not dfraw or not dtraw: continue
    df=datetime.fromisoformat(dfraw); dt=datetime.fromisoformat(dtraw)
    day=(df.replace(tzinfo=None)-base).days
    if day<0 or day>9: continue
    seo=x.get('aio:seo') or {}
    rows.append({'id':x['id'],'day':day,'date':df.date().isoformat(),'start':df.strftime('%H:%M'),'end':dt.strftime('%H:%M'),'name':seo.get('title') or x.get('title') or f'Événement {x["id"]}','place':chosen[1],'zone':chosen[2],'spaceId':chosen[0],'category':category(x),'desc':seo.get('description') or '','sourceUrl':source(x),'imageUrl':image(x)})
rows.sort(key=lambda z:(z['day'],z['start'],z['name']))
assert len(rows)>=42, f'Expected at least 42 remaining official occurrences, got {len(rows)}'

manifest={'source':'Foire du Valais API','dateRange':'2026-10-02/2026-10-11','totalOfficialApiOccurrences':len(items),'otherOfficialOccurrences':len(rows),'spaces':{},'events':rows}
for z in rows:
    manifest['spaces'].setdefault(z['zone'],0); manifest['spaces'][z['zone']]+=1
with open('buildsrc/RESTE-PROGRAMME-2026-OFFICIEL.json','w',encoding='utf-8') as f:json.dump(manifest,f,ensure_ascii=False,indent=2)

jp='buildsrc/app/src/main/java/ch/piiwii/foirevalais/MainActivity.java'
s=open(jp,encoding='utf-8').read()
old='final String[] zones={"TOUT","LIVE","TRIBUS","RENDEZ-VOUS","FOIRE & ANIM.","DÉGUSTATIONS","★ MES CHOIX"};'
new='final String[] zones={"TOUT","LIVE","TRIBUS","RENDEZ-VOUS","FOIRE & ANIM.","DÉGUSTATIONS","INNOTHÈQUE","BONNE DE BOURBON","CONFÉRENCES","HALLES","AUTRES","★ MES CHOIX"};'
assert old in s;s=s.replace(old,new,1)
old='selectedZone=Math.max(0,Math.min(zones.length-1,prefs.getInt("lastZone",0)));'
new='int savedZone=prefs.getInt("lastZone",0);\n        if(!prefs.getBoolean("zones1529Migrated",false)){if(savedZone==6)savedZone=zones.length-1;prefs.edit().putBoolean("zones1529Migrated",true).putInt("lastZone",savedZone).apply();}\n        selectedZone=Math.max(0,Math.min(zones.length-1,savedZone));'
assert old in s;s=s.replace(old,new,1)
old='        addOfficialTribus(events);\n        removeTribusAnnouncements(announced);'
new=old+'\n        // Tous les autres espaces officiels 2026 (Innothèque, Bonne de Bourbon, Conférences, Halles et programmes sans espace).\n        addOfficialOtherSpaces(events);'
assert old in s;s=s.replace(old,new,1)

lines=['    void addOfficialOtherSpaces(ArrayList<Event> target){','        for(Iterator<Event> it=target.iterator();it.hasNext();){Event e=it.next(); String p=e.place==null?"":e.place.toLowerCase(Locale.ROOT); if("INNOTHÈQUE".equals(e.zone)||"BONNE DE BOURBON".equals(e.zone)||"CONFÉRENCES".equals(e.zone)||"HALLES".equals(e.zone)||"AUTRES".equals(e.zone)||p.contains("innothèque")||p.contains("bonne de bourbon")||p.contains("espace conférences")||p.contains("halles commerciales"))it.remove();}']
for z in rows:
    lines.append('        target.add(new Event(%d,%d,%s,%s,%s,%s,%s,%s,%s,true,%s));' % (z['id'],z['day'],jesc(z['start']),jesc(z['end']),jesc(z['name']),jesc(z['place']),jesc(z['zone']),jesc(z['category']),jesc(z['desc']),jesc(z['sourceUrl'])))
lines += ['    }','']
block='\n'.join(lines)
marker='    void buildShell(){'
assert marker in s;s=s.replace(marker,block+'\n'+marker,1)
old='            addOfficialTribus(next);\n            removeTribusAnnouncements(ann);'
new=old+'\n            addOfficialOtherSpaces(next);'
assert old in s;s=s.replace(old,new,1)

old='''    View eventThumb(Event e,int widthDp,int heightDp){
        if(e!=null && ("LIVE".equals(e.zone) || "TRIBUS".equals(e.zone))){
            String prefix="TRIBUS".equals(e.zone)?"tribus_":"live_";
            int res=getResources().getIdentifier(prefix+e.id,"drawable",getPackageName());
            if(res!=0){
                ImageView im=new ImageView(this);
                im.setImageResource(res);
                im.setScaleType(ImageView.ScaleType.CENTER_CROP);
                im.setBackground(round(darkMode()?0xFF252936:0xFFF0EDF3,14));
                im.setClipToOutline(true);
                im.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp),dp(heightDp)));
                return im;
            }
        }
        EventThumbView v=new EventThumbView(this,e);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp),dp(heightDp)));
        return v;
    }'''
new='''    View eventThumb(Event e,int widthDp,int heightDp){
        if(e!=null){
            int res=getResources().getIdentifier("program_"+e.id,"drawable",getPackageName());
            if(res==0 && ("LIVE".equals(e.zone) || "TRIBUS".equals(e.zone))){
                String prefix="TRIBUS".equals(e.zone)?"tribus_":"live_";
                res=getResources().getIdentifier(prefix+e.id,"drawable",getPackageName());
            }
            if(res!=0){
                ImageView im=new ImageView(this);
                im.setImageResource(res);
                im.setScaleType(ImageView.ScaleType.CENTER_CROP);
                im.setBackground(round(darkMode()?0xFF252936:0xFFF0EDF3,14));
                im.setClipToOutline(true);
                im.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp),dp(heightDp)));
                return im;
            }
        }
        EventThumbView v=new EventThumbView(this,e);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp),dp(heightDp)));
        return v;
    }'''
assert old in s;s=s.replace(old,new,1)
old='''            if(style.equals("TRIBUS"))drawTribus(c,w,h);
            else if(style.equals("DÉGUSTATIONS"))drawWine(c,w,h);
            else if(style.equals("FOIRE & ANIM.")||style.equals("RENDEZ-VOUS"))drawFair(c,w,h);
            else drawConcert(c,w,h);'''
new='''            if(style.equals("TRIBUS"))drawTribus(c,w,h);
            else if(style.equals("DÉGUSTATIONS")||style.equals("INNOTHÈQUE"))drawWine(c,w,h);
            else if(style.equals("LIVE"))drawConcert(c,w,h);
            else drawFair(c,w,h);'''
assert old in s;s=s.replace(old,new,1)

old='''    String shortZone(Event e){
        if(e.zone.equals("FOIRE & ANIM."))return "FOIRE & ANIM.";
        if(e.zone.equals("DÉGUSTATIONS"))return "DÉGUSTATION";
        return e.zone;
    }
    int zoneColor(Event e){
        if(e.zone.equals("TRIBUS"))return 0xFFE56D00;
        if(e.zone.equals("FOIRE & ANIM."))return 0xFF2563C9;
        if(e.zone.equals("DÉGUSTATIONS"))return 0xFF7A2DC7;
        if(e.zone.equals("RENDEZ-VOUS"))return 0xFF9A355A;
        return PINK;
    }
    int lightZoneColor(Event e){
        if(e.zone.equals("TRIBUS"))return 0xFFFFE9D7;
        if(e.zone.equals("FOIRE & ANIM."))return 0xFFE7F0FF;
        if(e.zone.equals("DÉGUSTATIONS"))return 0xFFF1E5FF;
        if(e.zone.equals("RENDEZ-VOUS"))return 0xFFFFE7F0;
        return 0xFFFFE6EA;
    }'''
new='''    String shortZone(Event e){
        if(e.zone.equals("FOIRE & ANIM."))return "FOIRE & ANIM.";
        if(e.zone.equals("DÉGUSTATIONS"))return "DÉGUSTATION";
        if(e.zone.equals("BONNE DE BOURBON"))return "BONNE DE BOURBON";
        return e.zone;
    }
    int zoneColor(Event e){
        if(e.zone.equals("TRIBUS")||e.zone.equals("HALLES"))return 0xFFE56D00;
        if(e.zone.equals("FOIRE & ANIM.")||e.zone.equals("CONFÉRENCES"))return 0xFF2563C9;
        if(e.zone.equals("DÉGUSTATIONS")||e.zone.equals("INNOTHÈQUE"))return 0xFF7A2DC7;
        if(e.zone.equals("RENDEZ-VOUS")||e.zone.equals("BONNE DE BOURBON"))return 0xFF9A355A;
        if(e.zone.equals("AUTRES"))return 0xFF6B7280;
        return PINK;
    }
    int lightZoneColor(Event e){
        if(e.zone.equals("TRIBUS")||e.zone.equals("HALLES"))return 0xFFFFE9D7;
        if(e.zone.equals("FOIRE & ANIM.")||e.zone.equals("CONFÉRENCES"))return 0xFFE7F0FF;
        if(e.zone.equals("DÉGUSTATIONS")||e.zone.equals("INNOTHÈQUE"))return 0xFFF1E5FF;
        if(e.zone.equals("RENDEZ-VOUS")||e.zone.equals("BONNE DE BOURBON"))return 0xFFFFE7F0;
        if(e.zone.equals("AUTRES"))return 0xFFF4F4F7;
        return 0xFFFFE6EA;
    }'''
assert old in s;s=s.replace(old,new,1)
old='''    boolean matchesZone(Event e,String z){
        if(z.equals("LIVE"))return e.zone.equals("LIVE")||e.place.contains("Live");
        if(z.equals("TRIBUS"))return e.zone.equals("TRIBUS")||e.place.contains("Tribus");
        if(z.equals("RENDEZ-VOUS"))return e.zone.equals("RENDEZ-VOUS");
        if(z.equals("DÉGUSTATIONS"))return e.zone.equals("DÉGUSTATIONS");
        if(z.equals("FOIRE & ANIM."))return e.zone.equals("FOIRE & ANIM.");
        if(z.equals("★ MES CHOIX"))return prefs.getBoolean("fav"+e.id,false);
        return true;
    }'''
new='''    boolean matchesZone(Event e,String z){
        if(z.equals("LIVE"))return e.zone.equals("LIVE")||e.place.contains("Live");
        if(z.equals("TRIBUS"))return e.zone.equals("TRIBUS")||e.place.contains("Tribus");
        if(z.equals("RENDEZ-VOUS"))return e.zone.equals("RENDEZ-VOUS")||(e.category!=null&&e.category.toLowerCase(Locale.ROOT).contains("rendez-vous"));
        if(z.equals("DÉGUSTATIONS"))return e.zone.equals("DÉGUSTATIONS");
        if(z.equals("FOIRE & ANIM."))return e.zone.equals("FOIRE & ANIM.");
        if(z.equals("INNOTHÈQUE"))return e.zone.equals("INNOTHÈQUE");
        if(z.equals("BONNE DE BOURBON"))return e.zone.equals("BONNE DE BOURBON");
        if(z.equals("CONFÉRENCES"))return e.zone.equals("CONFÉRENCES");
        if(z.equals("HALLES"))return e.zone.equals("HALLES");
        if(z.equals("AUTRES"))return e.zone.equals("AUTRES");
        if(z.equals("★ MES CHOIX"))return prefs.getBoolean("fav"+e.id,false);
        return true;
    }'''
assert old in s;s=s.replace(old,new,1)
old='''    String zoneGlyph(Event e){
        if(e.zone.equals("LIVE"))return "♪";
        if(e.zone.equals("TRIBUS"))return "●";
        if(e.zone.equals("DÉGUSTATIONS"))return "♨";
        if(e.zone.equals("RENDEZ-VOUS"))return "◎";
        return "▦";
    }'''
new='''    String zoneGlyph(Event e){
        if(e.zone.equals("LIVE"))return "♪";
        if(e.zone.equals("TRIBUS"))return "●";
        if(e.zone.equals("DÉGUSTATIONS")||e.zone.equals("INNOTHÈQUE"))return "♨";
        if(e.zone.equals("RENDEZ-VOUS")||e.zone.equals("BONNE DE BOURBON"))return "◎";
        if(e.zone.equals("CONFÉRENCES"))return "▤";
        if(e.zone.equals("HALLES"))return "▦";
        return "•";
    }'''
assert old in s;s=s.replace(old,new,1)

s=s.replace('Version 1.5.28','Version 1.5.29').replace('versionCode 34','versionCode 35')
open(jp,'w',encoding='utf-8').write(s)
gp='buildsrc/app/build.gradle.kts'
g=open(gp,encoding='utf-8').read();assert 'versionCode = 34' in g and 'versionName = "1.5.28"' in g
g=g.replace('versionCode = 34','versionCode = 35').replace('versionName = "1.5.28"','versionName = "1.5.29"')
open(gp,'w',encoding='utf-8').write(g)

out='buildsrc/app/src/main/res/drawable-nodpi';os.makedirs(out,exist_ok=True)
done=set();downloaded=0;missing=[]
for z in rows:
    if z['id'] in done:continue
    done.add(z['id'])
    if not z['imageUrl']:
        missing.append(z['id']);continue
    r=requests.get(z['imageUrl'],headers=h,timeout=60);r.raise_for_status()
    ctype=r.headers.get('content-type','').lower();ext='.png' if 'png' in ctype else '.jpg'
    open(os.path.join(out,f'program_{z["id"]}{ext}'),'wb').write(r.content);downloaded+=1
print('Total official API occurrences:',len(items))
print('Remaining official occurrences integrated:',len(rows))
print('By app space:',manifest['spaces'])
print('Unique remaining programmes:',len(done),'photos downloaded:',downloaded,'missing photos:',len(missing),missing)
assert any(z['zone']=='INNOTHÈQUE' for z in rows)
assert any(z['zone']=='BONNE DE BOURBON' for z in rows)
assert any(z['zone']=='CONFÉRENCES' for z in rows)
assert any(z['zone']=='HALLES' for z in rows)
