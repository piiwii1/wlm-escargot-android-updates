from pathlib import Path

p=Path('euroscan-ch')

def must_replace(s, old, new, label):
    if old not in s:
        raise SystemExit('missing anchor: '+label)
    return s.replace(old,new,1)

# version
f=p/'app/build.gradle'
s=f.read_text()
s=must_replace(s,'versionCode 15','versionCode 16','versionCode')
s=must_replace(s,"versionName '1.3.10'","versionName '1.3.11'",'versionName')
f.write_text(s)

# HistoryStore: persist official draw into legacy entries once fetched.
f=p/'app/src/main/java/ch/piiwii/euroscan/HistoryStore.java'
s=f.read_text()
anchor='''    private JSONArray ints(List<Integer> v){JSONArray a=new JSONArray();for(Integer n:v)a.put(n);return a;}'''
insert='''    public boolean backfillDraw(String date,List<Integer> drawNumbers,List<Integer> drawStars){\n        if(date==null||date.trim().isEmpty()||drawNumbers==null||drawNumbers.size()!=5||drawStars==null||drawStars.size()!=2)return false;\n        try{\n            JSONArray a=new JSONArray(p.getString("items","[]"));boolean changed=false;\n            for(int i=0;i<a.length();i++){\n                JSONObject o=a.optJSONObject(i);if(o==null||!date.equals(o.optString("date")))continue;\n                JSONArray dn=o.optJSONArray("drawNumbers");\n                if(dn!=null&&dn.length()==5)continue;\n                o.put("drawNumbers",ints(drawNumbers));o.put("drawStars",ints(drawStars));changed=true;\n            }\n            if(changed)p.edit().putString("items",a.toString()).apply();\n            return changed;\n        }catch(Exception e){return false;}\n    }\n'''
if anchor not in s: raise SystemExit('missing anchor: HistoryStore ints')
s=s.replace(anchor,insert+anchor,1)
f.write_text(s)

# MainActivity: fetch missing official draw by saved date, cache it, and refresh history.
f=p/'app/src/main/java/ch/piiwii/euroscan/MainActivity.java'
s=f.read_text().replace('1.3.10','1.3.11')

field_anchor='''    private HistoryStore history;'''
field_new='''    private HistoryStore history;\n    private final Set<String> historyDrawsLoading = Collections.synchronizedSet(new HashSet<>());\n    private final Set<String> historyDrawsFailed = Collections.synchronizedSet(new HashSet<>());'''
s=must_replace(s,field_anchor,field_new,'history fields')

old='''            } else {\n                c.addView(gap(13));\n                c.addView(txt("Tirage officiel non enregistré pour cet ancien contrôle.",12,MUTED,false));\n            }'''
new='''            } else {\n                c.addView(gap(13));\n                String drawDate=o.optString("date","");\n                if(historyDrawsFailed.contains(drawDate)){\n                    c.addView(txt("Tirage officiel temporairement indisponible. Il sera retenté à la prochaine ouverture.",12,MUTED,false));\n                } else {\n                    c.addView(txt("Chargement du tirage officiel…",12,MUTED,false));\n                    backfillHistoryDraw(drawDate);\n                }\n            }'''
s=must_replace(s,old,new,'missing draw history block')

method_anchor='''    private JSONArray legacyGrids(String compact) {'''
method_insert='''    private void backfillHistoryDraw(String date) {\n        if(date==null||date.trim().isEmpty()||historyDrawsLoading.contains(date))return;\n        historyDrawsLoading.add(date);\n        worker.execute(() -> {\n            try {\n                DrawResult d=repo.fetch(date);\n                if(d!=null&&d.numbers.size()==5&&d.stars.size()==2){\n                    history.backfillDraw(date,d.numbers,d.stars);\n                    historyDrawsFailed.remove(date);\n                } else historyDrawsFailed.add(date);\n            } catch(Exception e){\n                historyDrawsFailed.add(date);\n            } finally {\n                historyDrawsLoading.remove(date);\n                runOnUiThread(() -> history());\n            }\n        });\n    }\n\n'''
if method_anchor not in s: raise SystemExit('missing anchor: legacyGrids')
s=s.replace(method_anchor,method_insert+method_anchor,1)

f.write_text(s)
print('EuroScan CH 1.3.11 patched')
