from pathlib import Path
p=Path('euroscan-ch')

def replace_once(s, old, new, label):
    if old not in s:
        raise SystemExit('missing anchor: '+label)
    return s.replace(old,new,1)

# VERSION
f=p/'app/build.gradle'
s=f.read_text()
s=s.replace('versionCode 9','versionCode 10').replace("versionName '1.3.4'","versionName '1.3.5'")
f.write_text(s)

# TICKET PARSER: capture ticket stake when clearly printed
f=p/'app/src/main/java/ch/piiwii/euroscan/TicketParser.java'
s=f.read_text()
s=s.replace('public static class ParsedTicket{public String date;public final List<TicketGrid> grids=new ArrayList<>();}',
            'public static class ParsedTicket{public String date;public Double stake;public final List<TicketGrid> grids=new ArrayList<>();}')
old="""public static ParsedTicket parse(String text){ParsedTicket o=new ParsedTicket();if(text==null)return o;String z=text.replace('\\r','\\n').replace('â€¢',' ').replace('|',' ');Matcher d=DATE.matcher(z);"""
new="""public static ParsedTicket parse(String text){ParsedTicket o=new ParsedTicket();if(text==null)return o;String z=text.replace('\\r','\\n').replace('â€¢',' ').replace('|',' ');o.stake=extractStake(z);Matcher d=DATE.matcher(z);"""
s=replace_once(s,old,new,'parser parse')
anchor=''' public static int countLotteryNumbers(String t){int c=0;for(int v:ints(t==null?"":t))if(v>=1&&v<=50)c++;return c;}'''
insert=r''' static Double extractStake(String z){Pattern p=Pattern.compile("(?im)(?:TOTAL|MONTANT|MISE|PRIX)[^\\n]{0,28}?(?:CHF\\s*)?([0-9]{1,4}(?:[.,][0-9]{1,2})?)");Matcher m=p.matcher(z);if(m.find())try{double v=Double.parseDouble(m.group(1).replace(',','.'));if(v>0&&v<5000)return v;}catch(Exception e){}Pattern q=Pattern.compile("(?im)CHF\\s*([0-9]{1,3}(?:[.,][0-9]{1,2})?)\\s*(?:$|\\n)");Matcher n=q.matcher(z);Double last=null;while(n.find())try{double v=Double.parseDouble(n.group(1).replace(',','.'));if(v>0&&v<500)last=v;}catch(Exception e){}return last;}
'''
s=replace_once(s,anchor,insert+anchor,'parser stake helper')
f.write_text(s)

# HISTORY STORE: structured cards + stake/gain/loss totals
f=p/'app/src/main/java/ch/piiwii/euroscan/HistoryStore.java'
f.write_text(r'''package ch.piiwii.euroscan;
import android.content.*;import org.json.*;import java.util.*;
public class HistoryStore {
    private final SharedPreferences p; public HistoryStore(Context c){p=c.getSharedPreferences("history",Context.MODE_PRIVATE);}
    public void add(String date,List<TicketGrid> grids,DrawResult draw,double stake,double total,boolean exact,boolean won,String result){
        try{
            JSONArray a=new JSONArray(p.getString("items","[]"));JSONObject o=new JSONObject();
            o.put("date",date);o.put("stake",stake);o.put("total",total);o.put("exact",exact);o.put("won",won);o.put("result",result);o.put("time",System.currentTimeMillis());
            JSONArray pg=new JSONArray();for(TicketGrid g:grids){JSONObject x=new JSONObject();x.put("numbers",ints(g.numbers));x.put("stars",ints(g.stars));pg.put(x);}o.put("playedGrids",pg);
            o.put("drawNumbers",ints(draw.numbers));o.put("drawStars",ints(draw.stars));o.put("swissWinNumbers",ints(draw.swissWinNumbers));
            StringBuilder compact=new StringBuilder();for(TicketGrid g:grids)compact.append(g.compact()).append(" | ");o.put("grids",compact.toString());
            JSONArray n=new JSONArray();n.put(o);for(int i=0;i<a.length()&&i<149;i++)n.put(a.get(i));p.edit().putString("items",n.toString()).apply();
        }catch(Exception ignored){}
    }
    private JSONArray ints(List<Integer> v){JSONArray a=new JSONArray();for(int x:v)a.put(x);return a;}
    public JSONArray all(){try{return new JSONArray(p.getString("items","[]"));}catch(Exception e){return new JSONArray();}}
    public double totalConfirmedGains(){double t=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)t+=Math.max(0,o.optDouble("total",0));}return t;}
    public double totalKnownStake(){double t=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)t+=Math.max(0,o.optDouble("stake",0));}return t;}
    public double totalConfirmedLosses(){double t=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||!o.optBoolean("exact",false))continue;double stake=Math.max(0,o.optDouble("stake",0)),gain=Math.max(0,o.optDouble("total",0));t+=Math.max(0,stake-gain);}return t;}
    public int ticketCount(){return all().length();}
    public int winCount(){int c=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&(o.optBoolean("won",false)||o.optDouble("total",0)>0))c++;}return c;}
    public int lossCount(){int c=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optBoolean("exact",false)&&!o.optBoolean("won",false)&&o.optDouble("total",0)<=0)c++;}return c;}
    public int pendingCount(){int c=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&!o.optBoolean("exact",false))c++;}return c;}
    public void clear(){p.edit().remove("items").apply();}
}
''')

# MAIN ACTIVITY
f=p/'app/src/main/java/ch/piiwii/euroscan/MainActivity.java'
s=f.read_text()
s=s.replace('v1.3.4','v1.3.5').replace('EuroScan CH 1.3.4','EuroScan CH 1.3.5')
s=s.replace('private EditText dateEdit;','private EditText dateEdit, stakeEdit;')

# HOME
a=s.index('    private void home() {')
b=s.index('    private LinearLayout shortcutTile',a)
home=r'''    private void home() {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackgroundColor(BG);
        ScrollView sc = new ScrollView(this); sc.setFillViewport(true); sc.setClipToPadding(false); sc.setBackgroundColor(BG);
        shell.addView(sc, new FrameLayout.LayoutParams(-1, -1));
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        sc.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout nav = bottomNav("home"); nav.setMinimumHeight(dp(72));
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM); navLp.setMargins(dp(14),0,dp(14),0); shell.addView(nav,navLp);
        setContentView(shell); applySafeInsets(root,18,18,18,112); applyBottomInset(nav,8);

        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView appIcon=new ImageView(this); appIcon.setImageResource(R.drawable.euroscan_icon);
        header.addView(appIcon,new LinearLayout.LayoutParams(dp(54),dp(54))); header.addView(gapH(12));
        LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL);
        LinearLayout titleRow=new LinearLayout(this); titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(txt("EuroScan",25,TEXT,true)); titleRow.addView(txt(" CH",25,GOLD,true)); names.addView(titleRow);
        names.addView(txt("Scannez vos tickets simplement",12,MUTED,false)); header.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView gear=txt("âš™",28,TEXT,false); gear.setGravity(Gravity.CENTER); gear.setBackground(round(CARD,18));
        header.addView(gear,new LinearLayout.LayoutParams(dp(50),dp(50))); gear.setOnClickListener(v->settingsScreen());
        root.addView(header,lp(-1,-2,0,0,0,20));

        LinearLayout next=box(CARD,24,18); next.setBackground(gradient(new int[]{Color.rgb(13,43,80),Color.rgb(36,29,72)},24));
       ²È="25±…•}½¹”¡Ì°ÁÉ¥Ù…Ñ”Ù½¥É•ÍÕ±Ð¡É…ÝI•ÍÕ±Ð°1¥ÍÐñQ¥­•ÑÉ¥øÌ¤ìœ°ÁÉ¥Ù…Ñ”Ù½¥É•ÍÕ±Ð¡É…ÝI•ÍÕ±Ð°1¥ÍÐñQ¥­•ÑÉ¥øÌ°‘½Õ‰±”ÍÑ…­”¤ìœ°É•ÍÕ±ÐÍ¥œœ¤)ÌõÉ•Á±…•}½¹”¡Ì°Á…” ‰K¥ÍÕ±Ñ…Ð‘ÔÑ¥­•Ðˆ°€‰Q¥É…”€ˆ€¬¹‘…Ñ”€¬€ˆƒ
Ü½µÁ…É…¥Í½¸Ñ•Éµ¥»¥”¸ˆ°€‰É•ÍÕ±ÑÌˆ¤ìœ°(€€€€€€€€€€€€€€€Á…” ‰K¥ÍÕ±Ñ…Ð‘ÔÑ¥­•Ðˆ°€‰Q¥É…”€ˆ€¬¹‘…Ñ”€¬€ˆƒ
Ü½µÁ…É…¥Í½¸Ñ•Éµ¥»¥”¸ˆ°€‰¡¥ÍÑ½Éäˆ¤ìœ°É•ÍÕ±Ð¹…Øœ¤((Œ!%MQ=IdMY)½±ôœœœ€€€€€€€MÑÉ¥¹	Õ¥±‘•È…±°€ô¹•ÜMÑÉ¥¹	Õ¥±‘•È ¤ì(€€€€€€€™½È€¡Q¥­•ÑÉ¥œ€èÌ¤…±°¹…ÁÁ•¹¡œ¹½µÁ…Ð ¤¤¹…ÁÁ•¹ ˆð€ˆ¤ì(€€€€€€€¡¥ÍÑ½Éä¹…‘¡¹‘…Ñ”°…±°¹Ñ½MÑÉ¥¹œ ¤°¡¥ÍÐ¹Ñ½MÑÉ¥¹œ ¤°Ñ½Ñ…°°…±±á…Ð°…¹å]¥¸¤ìœœœ)ÌõÉ•Á±…•}½¹”¡Ì±½±°œ€€€€€€€¡¥ÍÑ½Éä¹…‘¡¹‘…Ñ”°Ì°°ÍÑ…­”°Ñ½Ñ…°°…±±á…Ð°…¹å]¥¸°¡¥ÍÐ¹Ñ½MÑÉ¥¹œ ¤¤ìœ°¡¥ÍÑ½ÉäÍ…Ù”œ¤((Œ!%MQ=IdA)„õÌ¹¥¹‘•à œ€€€ÁÉ¥Ù…Ñ”Ù½¥¡¥ÍÑ½Éä ¤ìœ¤)ˆõÌ¹¥¹‘•à œ€€€ÁÉ¥Ù…Ñ”1¥¹•…É1…å½ÕÐµ…Ñ¡•‘É¥‘I½Üœ±„¤)¡¥ÍÑ½ÉäõÈœœœ€€€ÁÉ¥Ù…Ñ”Ù½¥¡¥ÍÑ½Éä ¤ì(€€€€€€€Á…” ‰!¥ÍÑ½É¥ÅÕ”ˆ°‰Q•ÌÑ¥­•ÑÌ°Ñ•Ìµ¥Í•Ì•ÐÑ•Ì…¥¹Ì•¸Õ¸½ÕÀŸM¥°¸ˆ°‰¡¥ÍÑ½Éäˆ¤ì(€€€€€€€)M=9ÉÉ…ä„õ¡¥ÍÑ½Éä¹…±° ¤ì(€€€€€€€¥˜¡„¹±•¹Ñ  ¤ôôÀ¥í1¥¹•…É1…å½ÕÐ•µÁÑäõ…É ¤í•µÁÑä¹…‘‘Y¥•Ü¡ÑáÐ ‰ÕÕ¸Ñ¥­•Ð•¹É•¥ÍÑË¤ˆ°Äà±QaP±ÑÉÕ”¤¤í•µÁÑä¹…‘‘Y¥•Ü¡…À Ô¤¤í•µÁÑä¹…‘‘Y¥•Ü¡ÑáÐ ‰M…¹¹”Õ¸Ñ¥­•Ð½ÔÕÑ¥±¥Í”±„Í…¥Í¥”µ…¹Õ•±±”€è±”½¹ÑËÑ±”…ÁÁ…É‡¹ÑÉ„¥¤¸ˆ°ÄÌ±5UQ±™…±Í”¤¤íÉ½½Ð¹…‘‘Y¥•Ü¡•µÁÑä±±À ´Ä°´È°À°À°À°ÄØ¤¤íô((€€€€€€€™½È¡¥¹Ð¤ôÀí¤ñ„¹±•¹Ñ  ¤í¤¬¬¤ÑÉåì(€€€€€€€€€€€)M=9=‰©•Ð¼õ„¹•Ñ)M=9=‰©•Ð¡¤¤ì‘½Õ‰±”…¥¸õ¼¹½ÁÑ½Õ‰±” ‰Ñ½Ñ…°ˆ°À¤±ÍÑ…­”õ¼¹½ÁÑ½Õ‰±” ‰ÍÑ…­”ˆ°À¤ì‰½½±•…¸Ý½¸õ¼¹½ÁÑ	½½±•…¸ ‰Ý½¸ˆ±…¥¸øÀ¤±•àõ¼¹½ÁÑ	½½±•…¸ ‰•á…Ðˆ±™…±Í”¤ì(€€€€€€€€€€€MÑÉ¥¹œÍÑ…ÑÕÌõÝ½¸ü¡•àü‰;$ˆè‰%8ƒ Y1%Hˆ¤è¡•àü‰AITˆè‹ Y1%Hˆ¤ì¥¹ÐÍÑ…ÑÕÍ½±½ÈõÝ½¸ýI8è¡•àýIé=I9¤ì(€€€€€€€€€€€1¥¹•…É1…å½ÕÐŒõ…É ¤ì1¥¹•…É1…å½ÕÐÑ½Àõ¹•Ü1¥¹•…É1…å½ÕÐ¡Ñ¡¥Ì¤ìÑ½À¹Í•ÑÉ…Ù¥Ñä¡É…Ù¥Ñä¹9QI}YIQ%0¤ì(€€€€€€€€€€€1¥¹•…É1…å½ÕÐ‘…Ñ•½°õ¹•Ü1¥¹•…É1…å½ÕÐ¡Ñ¡¥Ì¤í‘…Ñ•½°¹Í•Ñ=É¥•¹Ñ…Ñ¥½¸¡1¥¹•…É1…å½ÕÐ¹YIQ%0¤í‘…Ñ•½°¹…‘‘Y¥•Ü¡ÑáÐ¡ÁÉ•ÑÑå…Ñ”¡¼¹½ÁÑMÑÉ¥¹œ ‰‘…Ñ”ˆ¤¤°ÄÜ±QaP±ÑÉÕ”¤¤í‘…Ñ•½°¹…‘‘Y¥•Ü¡ÑáÐ ‰Q¥­•Ð½¹ÑËÑ³¤ˆ°ÄÄ±5UQ±™…±Í”¤¤ì(€€€€€€€€€€€Ñ½À¹…‘‘Y¥•Ü¡‘…Ñ•½°±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ À°´È°Ä¤¤ìQ•áÑY¥•Ü‰…‘”õÑáÐ¡ÍÑ…ÑÕÌ°ÄÄ±ÍÑ…ÑÕÍ½±½È±ÑÉÕ”¤í‰…‘”¹Í•ÑA…‘‘¥¹œ¡‘À ä¤±‘À Ô¤±‘À ä¤±‘À Ô¤¤í‰…‘”¹Í•Ñ	…­É½Õ¹¡ÍÑÉ½­”¡ÍÑ…ÑÕÍ½±½È±I|È°ÄÐ¤¤íÑ½À¹…‘‘Y¥•Ü¡‰…‘”¤íŒ¹…‘‘Y¥•Ü¡Ñ½À¤ì((€€€€€€€€€€€)M=9ÉÉ…ä‘É…Ý8õ¼¹½ÁÑ)M=9ÉÉ…ä ‰‘É…Ý9Õµ‰•ÉÌˆ¤±‘É…ÝLõ¼¹½ÁÑ)M=9ÉÉ…ä ‰‘É…ÝMÑ…ÉÌˆ¤±É¥‘Ìõ¼¹½ÁÑ)M=9ÉÉ…ä ‰Á±…å•‘É¥‘Ìˆ¤ì(€€€€€€€€€€€¥˜¡‘É…Ý8„õ¹Õ±°˜™‘É…ÝL„õ¹Õ±°˜™É¥‘Ì„õ¹Õ±°¥ì(€€€€€€€€€€€€€€€Œ¹…‘‘Y¥•Ü¡…À ÄÐ¤¤íŒ¹…‘‘Y¥•Ü¡ÑáÐ ‰9U7%I=LM=IQ%Lˆ°ÄÄ±5UQ±ÑÉÕ”¤¤íŒ¹…‘‘Y¥•Ü¡…À Ø¤¤íŒ¹…‘‘Y¥•Ü¡¡¥ÍÑ½ÉåÉ…ÝI½Ü¡‘É…Ý8±‘É…ÝL¤¤ì(€€€€€€€€€€€€€€€Œ¹…‘‘Y¥•Ü¡…À ÄÐ¤¤íŒ¹…‘‘Y¥•Ü¡ÑáÐ ‰QLI%11Lˆ°ÄÄ±5UQ±ÑÉÕ”¤¤ì(€€€€€€€€€€€€€€€™½È¡¥¹ÐœôÀíœñÉ¥‘Ì¹±•¹Ñ  ¤íœ¬¬¥í)M=9=‰©•Ð¼õÉ¥‘Ì¹½ÁÑ)M=9=‰©•Ð¡œ¤í¥˜¡¼ôõ¹Õ±°¥½¹Ñ¥¹Õ”í)M=9ÉÉ…äÁ¸õ¼¹½ÁÑ)M=9ÉÉ…ä ‰¹Õµ‰•ÉÌˆ¤±ÁÌõ¼¹½ÁÑ)M=9ÉÉ…ä ‰ÍÑ…ÉÌˆ¤íŒ¹…‘‘Y¥•Ü¡…À Ü¤¤íŒ¹…‘‘Y¥•Ü¡ÑáÐ ‰É¥±±”€ˆ¬¡œ¬Ä¤°ÄÈ±5UQ±ÑÉÕ”¤¤íŒ¹…‘‘Y¥•Ü¡…À Ô¤¤íŒ¹…‘‘Y¥•Ü¡¡¥ÍÑ½ÉåA±…å•‘I½Ü¡Á¸±ÁÌ±‘É…Ý8±‘É…ÝL¤¤íô(€€€€€€€€€€€õ•±Í•íŒ¹…‘‘Y¥•Ü¡…À ÄÀ¤¤íŒ¹…‘‘Y¥•Ü¡ÑáÐ¡¼¹½ÁÑMÑÉ¥¹œ ‰É¥‘Ìˆ°‰¹¥•¸½¹ÑËÑ±”ˆ¤°ÄÈ±5UQ±™…±Í”¤¤íŒ¹…‘‘Y¥•Ü¡…À Ð¤¤íŒ¹…‘‘Y¥•Ü¡ÑáÐ ‰¹¥•¸¡¥ÍÑ½É¥ÅÕ”€è“¥Ñ…¥°‘ÔÑ¥É…”½Ôµ¥Í”¹½¸•¹É•¥ÍÑË¤¸ˆ°ÄÄ±=I9±™…±Í”¤¤íô((€€€€€€€€€€€Œ¹…‘‘Y¥•Ü¡…À ÄÐ¤¤í1¥¹•…É1…å½ÕÐµ½¹•åI½Üõ¹•Ü1¥¹•…É1…å½ÕÐ¡Ñ¡¥Ì¤íµ½¹•åI½Ü¹Í•Ñ=É¥•¹Ñ…Ñ¥½¸¡1¥¹•…É1…å½ÕÐ¹!=I%i=9Q0¤ì(€€€€€€€€€€€1¥¹•…É1…å½ÕÐÁ±…å•‘	½àõ‰½à¡I|È°ÄÐ°ÄÀ¤íÁ±…å•‘	½à¹…‘‘Y¥•Ü¡ÑáÐ ‰)=W$ˆ°ÄÀ±5UQ±ÑÉÕ”¤¤íÁ±…å•‘	½à¹…‘‘Y¥•Ü¡ÑáÐ¡ÍÑ…­”øÀü‰!€ˆ­µ½¹•ä¡ÍÑ…­”¤è‹ŠPˆ°Äà±QaP±ÑÉÕ”¤¤ì(€€€€€€€€€€€1¥¹•…É1…å½ÕÐÝ½¹	½àõ‰½à¡Ý½¸ý½±½È¹Éˆ ÄÀ°ØÔ°ÔÈ¤éI|È°ÄÐ°ÄÀ¤íÝ½¹	½à¹…‘‘Y¥•Ü¡ÑáÐ ‰;$ˆ°ÄÀ±5UQ±ÑÉÕ”¤¤íÝ½¹	½à¹…‘‘Y¥•Ü¡ÑáÐ ‰!€ˆ­µ½¹•ä¡…¥¸¤°Äà±Ý½¸ýI8éQaP±ÑÉÕ”¤¤ì(€€€€€€€€€€€µ½¹•åI½Ü¹…‘‘Y¥•Ü¡Á±…å•‘	½à±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ À°´È°Ä¤¤íµ½¹•åI½Ü¹…‘‘Y¥•Ü¡…Á  à¤¤íµ½¹•åI½Ü¹…‘‘Y¥•Ü¡Ý½¹	½à±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ À°´È°Ä¤¤íŒ¹…‘‘Y¥•Ü¡µ½¹•åI½Ü¤ì(€€€€€€€€€€€É½½Ð¹…‘‘Y¥•Ü¡Œ±±À ´Ä°´È°À°À°À°ÄÈ¤¤ì(€€€€€€€õ…Ñ ¡á•ÁÑ¥½¸¥¹½É•¥íô((€€€€€€€	ÕÑÑ½¸Í…¸õ‰Ñ¸ ‹Š2\€M…¹¹•Èµ½¸Ñ¥­•Ðˆ±=1±½±½È¹Éˆ Äà°ÈÌ°ÌÈ¤¤í	ÕÑÑ½¸¡½µ”õÍ•½¹‘…Éä ‹Š2€Õ•¥°ˆ¤ì(€€€€€€€É½½Ð¹…‘‘Y¥•Ü¡Í…¸±±À ´Ä±‘À ÔØ¤°À°Ð°À°ÄÀ¤¤íÉ½½Ð¹…‘‘Y¥•Ü¡¡½µ”±±À ´Ä±‘À ÔÈ¤°À°À°À°ÄØ¤¤íÍ…¸¹Í•Ñ=¹±¥­1¥ÍÑ•¹•È¡Ø´ùÍÑ…ÉÑ1¥Ù•M…¹¹•È ¤¤í¡½µ”¹Í•Ñ=¹±¥­1¥ÍÑ•¹•È¡Ø´ù¡½µ” ¤¤ì((€€€€€€€1¥¹•…É1…å½ÕÐÑ½Ñ…±Ìõ‰½à¡I°ÈÈ°ÄÜ¤íÑ½Ñ…±Ì¹…‘‘Y¥•Ü¡ÑáÐ ‰	¥±…¸ˆ°Äà±QaP±ÑÉÕ”¤¤íÑ½Ñ…±Ì¹…‘‘Y¥•Ü¡…À ÄÈ¤¤í1¥¹•…É1…å½ÕÐÍÕµI½Üõ¹•Ü1¥¹•…É1…å½ÕÐ¡Ñ¡¥Ì¤íÍÕµI½Ü¹Í•Ñ=É¥•¹Ñ…Ñ¥½¸¡1¥¹•…É1…å½ÕÐ¹!=I%i=9Q0¤ì(€€€€€€€1¥¹•…É1…å½ÕÐ…¥¹Ìõ‰½à¡½±½È¹Éˆ ÄÀ°ØÔ°ÔÈ¤°ÄØ°ÄÈ¤í…¥¹Ì¹…‘‘Y¥•Ü¡ÑáÐ ‰Q=Q0%9Lˆ°ÄÄ±5UQ±ÑÉÕ”¤¤í…¥¹Ì¹…‘‘Y¥•Ü¡ÑáÐ ‰!€ˆ­µ½¹•ä¡¡¥ÍÑ½Éä¹Ñ½Ñ…±½¹™¥Éµ•‘…¥¹Ì ¤¤°ÈÐ±I8±ÑÉÕ”¤¤ì(€€€€€€€1¥¹•…É1…å½ÕÐ±½ÍÍ•Ìõ‰½à¡½±½È¹Éˆ ØÔ°ÌÄ°Ìà¤°ÄØ°ÄÈ¤í±½ÍÍ•Ì¹…‘‘Y¥•Ü¡ÑáÐ ‰Q=Q0AIQLˆ°ÄÄ±5UQ±ÑÉÕ”¤¤í±½ÍÍ•Ì¹…‘‘Y¥•Ü¡ÑáÐ ‰!€ˆ­µ½¹•ä¡¡¥ÍÑ½Éä¹Ñ½Ñ…±½¹™¥Éµ•‘1½ÍÍ•Ì ¤¤°ÈÐ±I±ÑÉÕ”¤¤ì(€€€€€€€ÍÕµI½Ü¹…‘‘Y¥•Ü¡…¥¹Ì±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ À°´È°Ä¤¤íÍÕµI½Ü¹…‘‘Y¥•Ü¡…Á  à¤¤íÍÕµI½Ü¹…‘‘Y¥•Ü¡±½ÍÍ•Ì±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ À°´È°Ä¤¤íÑ½Ñ…±Ì¹…‘‘Y¥•Ü¡ÍÕµI½Ü¤íÑ½Ñ…±Ì¹…‘‘Y¥•Ü¡…À à¤¤ì(€€€€€€€Ñ½Ñ…±Ì¹…‘‘Y¥•Ü¡ÑáÐ ‰Q½Ñ…°©½×¤€è!€ˆ­µ½¹•ä¡¡¥ÍÑ½Éä¹Ñ½Ñ…±-¹½Ý¹MÑ…­” ¤¤¬ˆƒ
Ü1•ÌÑ¥­•ÑÌƒ€Ù…±¥‘•È¹”Í½¹ÐÁ…Ì½µÁÓ¥Ì½µµ”Á•ÉÑ•Ì¸ˆ°ÄÄ±5UQ±™…±Í”¤¤íÉ½½Ð¹…‘‘Y¥•Ü¡Ñ½Ñ…±Ì±±À ´Ä°´È°À°À°À°ÄÐ¤¤ì(€€€€€€€	ÕÑÑ½¸±•…ÈõÍ•½¹‘…Éä ‰™™…•È°¡¥ÍÑ½É¥ÅÕ”ˆ¤í±•…È¹Í•ÑQ•áÑ½±½È¡I¤íÉ½½Ð¹…‘‘Y¥•Ü¡±•…È±±À ´Ä±‘À Ðà¤°À°À°À°À¤¤í±•…È¹Í•Ñ=¹±¥­1¥ÍÑ•¹•È¡Ø´ù¹•Ü±•ÉÑ¥…±½œ¹	Õ¥±‘•È¡Ñ¡¥Ì¤¹Í•ÑQ¥Ñ±” ‰™™…•È°¡¥ÍÑ½É¥ÅÕ”€üˆ¤¹Í•Ñ9•…Ñ¥Ù•	ÕÑÑ½¸ ‰¹¹Õ±•Èˆ±¹Õ±°¤¹Í•ÑA½Í¥Ñ¥Ù•	ÕÑÑ½¸ ‰™™…•Èˆ°¡±Ü¤´ùí¡¥ÍÑ½Éä¹±•…È ¤í¡¥ÍÑ½Éä ¤íô¤¹Í¡½Ü ¤¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”1¥¹•…É1…å½ÕÐ¡¥ÍÑ½ÉåÉ…ÝI½Ü¡)M=9ÉÉ…ä¹ÕµÌ±)M=9ÉÉ…äÍÑ…ÉÌ¥ì(€€€€€€€1¥¹•…É1…å½ÕÐÉ½Üõ¹•Ü1¥¹•…É1…å½ÕÐ¡Ñ¡¥Ì¤íÉ½Ü¹Í•ÑÉ…Ù¥Ñä¡É…Ù¥Ñä¹9QI}YIQ%0¤ì(€€€€€€€¥˜¡¹ÕµÌ„õ¹Õ±°¥™½È¡¥¹Ð¤ôÀí¤ñ¹ÕµÌ¹±•¹Ñ  ¤í¤¬¬¥íÉ½Ü¹…‘‘Y¥•Ü¡¡¥ÍÑ½Éå	…±°¡¹ÕµÌ¹½ÁÑ%¹Ð¡¤¤±™…±Í”±™…±Í”¤±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ À±‘À ÐÀ¤°Ä¤¤í¥˜¡¤ñ¹ÕµÌ¹±•¹Ñ  ¤´Ä¥É½Ü¹…‘‘Y¥•Ü¡…Á  Ô¤¤íô(€€€€€€€É½Ü¹…‘‘Y¥•Ü¡…Á  à¤¤í¥˜¡ÍÑ…ÉÌ„õ¹Õ±°¥™½È¡¥¹Ð¤ôÀí¤ñÍÑ…ÉÌ¹±•¹Ñ  ¤í¤¬¬¥íÉ½Ü¹…‘‘Y¥•Ü¡¡¥ÍÑ½Éå	…±°¡ÍÑ…ÉÌ¹½ÁÑ%¹Ð¡¤¤±™…±Í”±ÑÉÕ”¤±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ¡‘À ÐÀ¤±‘À ÐÀ¤¤¤í¥˜¡¤ñÍÑ…ÉÌ¹±•¹Ñ  ¤´Ä¥É½Ü¹…‘‘Y¥•Ü¡…Á  Ô¤¤íõÉ•ÑÕÉ¸É½Üì(€€€ô(€€€ÁÉ¥Ù…Ñ”1¥¹•…É1…å½ÕÐ¡¥ÍÑ½ÉåA±…å•‘I½Ü¡)M=9ÉÉ…ä¹ÕµÌ±)M=9ÉÉ…äÍÑ…ÉÌ±)M=9ÉÉ…ä‘É…Ý8±)M=9ÉÉ…ä‘É…ÝL¥ì(€€€€€€€1¥¹•…É1…å½ÕÐÉ½Üõ¹•Ü1¥¹•…É1…å½ÕÐ¡Ñ¡¥Ì¤íÉ½Ü¹Í•ÑÉ…Ù¥Ñä¡É…Ù¥Ñä¹9QI}YIQ%0¤ì(€€€€€€€¥˜¡¹ÕµÌ„õ¹Õ±°¥™½È¡¥¹Ð¤ôÀí¤ñ¹ÕµÌ¹±•¹Ñ  ¤í¤¬¬¥í¥¹Ð¸õ¹ÕµÌ¹½ÁÑ%¹Ð¡¤¤íÉ½Ü¹…‘‘Y¥•Ü¡¡¥ÍÑ½Éå	…±°¡¸±©Í½¹½¹Ñ…¥¹Ì¡‘É…Ý8±¸¤±™…±Í”¤±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ À±‘À ÐÈ¤°Ä¤¤í¥˜¡¤ñ¹ÕµÌ¹±•¹Ñ  ¤´Ä¥É½Ü¹…‘‘Y¥•Ü¡…Á  Ô¤¤íô(€€€€€€€É½Ü¹…‘‘Y¥•Ü¡…Á  à¤¤í¥˜¡ÍÑ…ÉÌ„õ¹Õ±°¥™½È¡¥¹Ð¤ôÀí¤ñÍÑ…ÉÌ¹±•¹Ñ  ¤í¤¬¬¥í¥¹Ð¸õÍÑ…ÉÌ¹½ÁÑ%¹Ð¡¤¤íÉ½Ü¹…‘‘Y¥•Ü¡¡¥ÍÑ½Éå	…±°¡¸±©Í½¹½¹Ñ…¥¹Ì¡‘É…ÝL±¸¤±ÑÉÕ”¤±¹•Ü1¥¹•…É1…å½ÕÐ¹1…å½ÕÑA…É…µÌ¡‘À ÐÈ¤±‘À ÐÈ¤¤¤í¥˜¡¤ñÍÑ…ÉÌ¹±•¹Ñ  ¤´Ä¥É½Ü¹…‘‘Y¥•Ü¡…Á  Ô¤¤íõÉ•ÑÕÉ¸É½Üì(€€€ô(€€€ÁÉ¥Ù…Ñ”Q•áÑY¥•Ü¡¥ÍÑ½Éå	…±°¡¥¹Ð¸±‰½½±•…¸µ…Ñ¡•±‰½½±•…¸ÍÑ…È¥ì(€€€€€€€Q•áÑY¥•ÜØõÑáÐ¡MÑÉ¥¹œ¹™½Éµ…Ð¡1½…±”¹UL°ˆ”ÀÉˆ±¸¤°ÄÐ±µ…Ñ¡•ýQaPè¡ÍÑ…Èý=1é5UQ¤±ÑÉÕ”¤íØ¹Í•ÑÉ…Ù¥Ñä¡É…Ù¥Ñä¹9QH¤íÉ…‘¥•¹ÑÉ…Ý…‰±”œõ¹•ÜÉ…‘¥•¹ÑÉ…Ý…‰±” ¤íœ¹Í•ÑM¡…Á”¡É…‘¥•¹ÑÉ…Ý…‰±”¹=Y0¤íœ¹Í•Ñ½±½È¡ÍÑ…Èý½±½È¹Éˆ Ðà°ÐÈ°Èà¤éI|È¤íœ¹Í•ÑMÑÉ½­”¡‘À¡µ…Ñ¡•üÌèÄ¤±µ…Ñ¡•ü¡ÍÑ…Èý=1éI8¤è¡ÍÑ…Èý½±½È¹Éˆ ÄÌà°ÄÀØ°ÌÐ¤é1%9¤¤íØ¹Í•Ñ	…­É½Õ¹¡œ¤íÉ•ÑÕÉ¸Øì(€€€ô(€€€ÁÉ¥Ù…Ñ”‰½½±•…¸©Í½¹½¹Ñ…¥¹Ì¡)M=9ÉÉ…ä„±¥¹Ð¸¥í¥˜¡„ôõ¹Õ±°¥É•ÑÕÉ¸™…±Í”í™½È¡¥¹Ð¤ôÀí¤ñ„¹±•¹Ñ  ¤í¤¬¬¥¥˜¡„¹½ÁÑ%¹Ð¡¤¤ôõ¸¥É•ÑÕÉ¸ÑÉÕ”íÉ•ÑÕÉ¸™…±Í”íô((œœœ)ÌõÍlé…t­¡¥ÍÑ½Éä­Ímˆét()˜¹ÝÉ¥Ñ•}Ñ•áÐ¡Ì¤)ÁÉ¥¹Ð ÕÉ½M…¸ €Ä¸Ì¸ÔÁ…Ñ¡•œ¤(