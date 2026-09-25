from pathlib import Path

p=Path('euroscan-ch')

def must_replace(s, old, new, label):
    if old not in s:
        raise SystemExit('missing anchor: '+label)
    return s.replace(old,new,1)

# version
f=p/'app/build.gradle'
s=f.read_text()
s=must_replace(s,'versionCode 14','versionCode 15','versionCode')
s=must_replace(s,"versionName '1.3.9'","versionName '1.3.10'",'versionName')
f.write_text(s)

f=p/'app/src/main/java/ch/piiwii/euroscan/MainActivity.java'
s=f.read_text().replace('1.3.9','1.3.10')

# Clarify legend: matches are yellow now.
s=s.replace('Les numéros entourés sont ceux que tu avais joués ET qui sont sortis.','Les numéros en jaune sont ceux que tu avais joués ET qui sont sortis.')

old='''    private LinearLayout historyDrawRow(JSONArray nums, JSONArray stars, JSONArray grids) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        if(nums!=null) for(int i=0;i<nums.length();i++){
            int n=nums.optInt(i); boolean matched=playedContains(grids,n,false);
            row.addView(historyDrawCircle(n,matched,false),new LinearLayout.LayoutParams(0,dp(46),1));
            if(i<nums.length()-1) row.addView(gapH(4));
        }
        row.addView(gapH(8));
        if(stars!=null) for(int i=0;i<stars.length();i++){
            int n=stars.optInt(i); boolean matched=playedContains(grids,n,true);
            row.addView(historyDrawCircle(n,matched,true),new LinearLayout.LayoutParams(dp(46),dp(46)));
            if(i<stars.length()-1) row.addView(gapH(4));
        }
        return row;
    }'''
new='''    private LinearLayout historyDrawRow(JSONArray nums, JSONArray stars, JSONArray grids) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        if(nums!=null) for(int i=0;i<nums.length();i++){
            int n=nums.optInt(i); boolean matched=playedContains(grids,n,false);
            row.addView(historyDrawCircle(n,matched,false),new LinearLayout.LayoutParams(0,dp(46),1));
            if(i<nums.length()-1) row.addView(gapH(4));
        }
        if(stars!=null && stars.length()>0){
            row.addView(gapH(7));
            row.addView(historyStarSeparator(),new LinearLayout.LayoutParams(dp(26),dp(46)));
            row.addView(gapH(7));
            for(int i=0;i<stars.length();i++){
                int n=stars.optInt(i); boolean matched=playedContains(grids,n,true);
                row.addView(historyDrawCircle(n,matched,true),new LinearLayout.LayoutParams(dp(46),dp(46)));
                if(i<stars.length()-1) row.addView(gapH(4));
            }
        }
        return row;
    }'''
s=must_replace(s,old,new,'historyDrawRow')

old='''    private TextView historyDrawCircle(int n,boolean matched,boolean star){
        int line=matched?(star?GOLD:GREEN):(star?GOLD:Color.rgb(77,135,219));
        int fill=star?Color.rgb(80,61,26):CARD_2;
        TextView v=txt(String.format(Locale.US,"%02d",n),15,TEXT,true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(oval(line,fill,matched?4:1));
        return v;
    }'''
new='''    private TextView historyDrawCircle(int n,boolean matched,boolean star){
        int line=matched?GOLD:(star?GOLD:Color.rgb(77,135,219));
        int fill=matched?GOLD:(star?Color.rgb(80,61,26):CARD_2);
        int text=matched?NAVY:TEXT;
        TextView v=txt(String.format(Locale.US,"%02d",n),15,text,true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(oval(line,fill,matched?2:1));
        return v;
    }'''
s=must_replace(s,old,new,'historyDrawCircle')

old='''    private LinearLayout historyNumberRow(JSONArray nums, JSONArray stars, JSONArray drawNums, JSONArray drawStars, boolean official) {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        if(nums!=null)for(int i=0;i<nums.length();i++){int n=nums.optInt(i);boolean m=!official&&jsonContains(drawNums,n);row.addView(historyCircle(n,m,false,official),new LinearLayout.LayoutParams(0,dp(43),1));if(i<nums.length()-1)row.addView(gapH(4));}
        row.addView(gapH(8));
        if(stars!=null)for(int i=0;i<stars.length();i++){int n=stars.optInt(i);boolean m=!official&&jsonContains(drawStars,n);row.addView(historyCircle(n,m,true,official),new LinearLayout.LayoutParams(dp(43),dp(43)));if(i<stars.length()-1)row.addView(gapH(4));}
        return row;
    }'''
new='''    private LinearLayout historyNumberRow(JSONArray nums, JSONArray stars, JSONArray drawNums, JSONArray drawStars, boolean official) {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        if(nums!=null)for(int i=0;i<nums.length();i++){int n=nums.optInt(i);boolean m=!official&&jsonContains(drawNums,n);row.addView(historyCircle(n,m,false,official),new LinearLayout.LayoutParams(0,dp(43),1));if(i<nums.length()-1)row.addView(gapH(4));}
        if(stars!=null && stars.length()>0){
            row.addView(gapH(7));
            row.addView(historyStarSeparator(),new LinearLayout.LayoutParams(dp(26),dp(43)));
            row.addView(gapH(7));
            for(int i=0;i<stars.length();i++){int n=stars.optInt(i);boolean m=!official&&jsonContains(drawStars,n);row.addView(historyCircle(n,m,true,official),new LinearLayout.LayoutParams(dp(43),dp(43)));if(i<stars.length()-1)row.addView(gapH(4));}
        }
        return row;
    }'''
s=must_replace(s,old,new,'historyNumberRow')

old='''    private TextView historyCircle(int n, boolean matched, boolean star, boolean official){int stroke=official?(star?GOLD:Color.rgb(77,135,219)):(matched?(star?GOLD:GREEN):LINE);int fill=official?(star?Color.rgb(80,61,26):CARD_2):CARD_2;TextView v=txt(String.format(Locale.US,"%02d",n),14,matched?TEXT:(official?TEXT:MUTED),true);v.setGravity(Gravity.CENTER);v.setBackground(oval(stroke,fill,matched?3:1));return v;}'''
new='''    private TextView historyCircle(int n, boolean matched, boolean star, boolean official){int stroke=matched?GOLD:(official?(star?GOLD:Color.rgb(77,135,219)):LINE);int fill=matched?GOLD:(official&&star?Color.rgb(80,61,26):CARD_2);int text=matched?NAVY:(official?TEXT:MUTED);TextView v=txt(String.format(Locale.US,"%02d",n),14,text,true);v.setGravity(Gravity.CENTER);v.setBackground(oval(stroke,fill,matched?2:1));return v;}'''
s=must_replace(s,old,new,'historyCircle')

anchor='''    private boolean jsonContains(JSONArray a,int n){if(a==null)return false;for(int i=0;i<a.length();i++)if(a.optInt(i)==n)return true;return false;}'''
insert='''    private TextView historyStarSeparator(){TextView v=txt("★",22,GOLD,true);v.setGravity(Gravity.CENTER);return v;}\n'''
if anchor not in s:
    raise SystemExit('missing anchor: jsonContains')
s=s.replace(anchor,insert+anchor,1)

f.write_text(s)
print('EuroScan CH 1.3.10 patched')
