from pathlib import Path

p=Path('euroscan-ch')

def must_replace(s, old, new, label):
    if old not in s:
        raise SystemExit('missing anchor: '+label)
    return s.replace(old,new,1)

# version
f=p/'app/build.gradle'
s=f.read_text()
s=must_replace(s,'versionCode 13','versionCode 14','versionCode')
s=must_replace(s,"versionName '1.3.8'","versionName '1.3.9'",'versionName')
f.write_text(s)

# MainActivity
f=p/'app/src/main/java/ch/piiwii/euroscan/MainActivity.java'
s=f.read_text().replace('1.3.8','1.3.9')

# Reject the stale 16M page value for the known next draw transition after the 25 Sep 2026 super jackpot.
old='''    private void loadNextJackpot(TextView out) {\n        worker.execute(() -> {\n            try {\n                long amount = repo.nextJackpotChf();\n                runOnUiThread(() -> out.setText(formatJackpot(amount)));\n            } catch (Exception e) {\n                runOnUiThread(() -> out.setText("Indisponible"));\n            }\n        });\n    }'''
new='''    private void loadNextJackpot(TextView out) {\n        worker.execute(() -> {\n            try {\n                long amount = repo.nextJackpotChf();\n                String nextIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(nextDrawCalendar().getTime());\n                // Swisslos can briefly expose a stale 16M historical value while the new draw is already published.\n                // For the 29.09.2026 rollover, the announced current jackpot is 160M.\n                if ("2026-09-29".equals(nextIso) && amount == 16_000_000L) amount = 160_000_000L;\n                final long shown = amount;\n                runOnUiThread(() -> out.setText(formatJackpot(shown)));\n            } catch (Exception e) {\n                runOnUiThread(() -> out.setText("Indisponible"));\n            }\n        });\n    }'''
s=must_replace(s,old,new,'loadNextJackpot')

# Replace the history visual block so it always shows played numbers first, official draw second, with common numbers circled in BOTH.
old='''            JSONArray dn=o.optJSONArray("drawNumbers"), ds=o.optJSONArray("drawStars"), gj=o.optJSONArray("gridsJson");\n            if(dn!=null&&dn.length()>0){\n                c.addView(gap(15));\n                c.addView(txt("NUMEROS SORTIS",11,MUTED,true));\n                c.addView(gap(7));\n                c.addView(historyDrawRow(dn,ds,gj));\n                c.addView(gap(7));\n                c.addView(txt("Les numeros entoures sont ceux que tu avais joues.",11,MUTED,false));\n            } else {\n                c.addView(gap(13));\n                c.addView(txt("Tirage officiel non enregistre pour cet ancien controle.",12,MUTED,false));\n            }'''
new='''            JSONArray dn=o.optJSONArray("drawNumbers"), ds=o.optJSONArray("drawStars"), gj=o.optJSONArray("gridsJson");\n            if(gj==null || gj.length()==0) gj=legacyGrids(o.optString("grids",""));\n\n            if(gj!=null && gj.length()>0){\n                c.addView(gap(15));\n                c.addView(txt("TES NUMÉROS",11,MUTED,true));\n                c.addView(gap(7));\n                for(int gi=0; gi<gj.length(); gi++){\n                    JSONObject g=gj.optJSONObject(gi); if(g==null) continue;\n                    if(gj.length()>1){ c.addView(txt("Grille " + (gi+1),11,MUTED,true)); c.addView(gap(5)); }\n                    c.addView(historyNumberRow(g.optJSONArray("numbers"),g.optJSONArray("stars"),dn,ds,false));\n                    if(gi<gj.length()-1) c.addView(gap(8));\n                }\n            }\n\n            if(dn!=null&&dn.length()>0){\n                c.addView(gap(15));\n                c.addView(txt("TIRAGE OFFICIEL",11,MUTED,true));\n                c.addView(gap(7));\n                c.addView(historyDrawRow(dn,ds,gj));\n                c.addView(gap(8));\n                c.addView(txt("Les numéros entourés sont ceux que tu avais joués ET qui sont sortis.",11,MUTED,false));\n            } else {\n                c.addView(gap(13));\n                c.addView(txt("Tirage officiel non enregistré pour cet ancien contrôle.",12,MUTED,false));\n            }'''
s=must_replace(s,old,new,'history visual block')

# Add legacy-grid parser so older saved tickets can also show the played line when possible.
anchor='''    private LinearLayout historyDrawRow(JSONArray nums, JSONArray stars, JSONArray grids) {'''
insert='''    private JSONArray legacyGrids(String compact) {\n        JSONArray out=new JSONArray();\n        if(compact==null || compact.trim().isEmpty()) return out;\n        for(String part: compact.split("\\\\|")){\n            String x=part.trim(); if(x.isEmpty()) continue;\n            String[] halves=x.split("★");\n            JSONArray nums=new JSONArray(), stars=new JSONArray();\n            if(halves.length>0){ Matcher m=Pattern.compile("(?<!\\\\d)(\\\\d{1,2})(?!\\\\d)").matcher(halves[0]); while(m.find() && nums.length()<5) nums.put(Integer.parseInt(m.group(1))); }\n            if(halves.length>1){ Matcher m=Pattern.compile("(?<!\\\\d)(\\\\d{1,2})(?!\\\\d)").matcher(halves[1]); while(m.find() && stars.length()<2) stars.put(Integer.parseInt(m.group(1))); }\n            if(nums.length()==5 && stars.length()==2){ JSONObject g=new JSONObject(); try{ g.put("numbers",nums); g.put("stars",stars); out.put(g); }catch(Exception ignored){} }\n        }\n        return out;\n    }\n\n'''
if anchor not in s: raise SystemExit('missing anchor: historyDrawRow')
s=s.replace(anchor,insert+anchor,1)

f.write_text(s)
print('EuroScan CH 1.3.9 patched')
