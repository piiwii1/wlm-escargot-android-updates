from pathlib import Path
import base64,re
p=Path('euroscan-ch')
# version
f=p/'app/build.gradle'; s=f.read_text(); s=s.replace('versionCode 7','versionCode 8').replace("versionName '1.3.2'","versionName '1.3.3'"); f.write_text(s)
# icon manifest
f=p/'app/src/main/AndroidManifest.xml'; s=f.read_text().replace('android:icon="@mipmap/ic_launcher"','android:icon="@drawable/euroscan_icon"').replace('android:roundIcon="@mipmap/ic_launcher_round"','android:roundIcon="@drawable/euroscan_icon"'); f.write_text(s)
# parser: support adjacent OCR lines + looser ticket detection
f=p/'app/src/main/java/ch/piiwii/euroscan/TicketParser.java'
f.write_text(r'''package ch.piiwii.euroscan;
import java.text.*;import java.util.*;import java.util.regex.*;
public final class TicketParser{
 public static class ParsedTicket{public String date;public final List<TicketGrid> grids=new ArrayList<>();}
 static final Pattern DATE=Pattern.compile("(?<!\\d)([0-3]?\\d)[./-]([01]?\\d)[./-](20\\d{2}|\\d{2})(?!\\d)"),NUMBER=Pattern.compile("(?<!\\d)(\\d{1,2})(?!\\d)");
 public static ParsedTicket parse(String text){ParsedTicket o=new ParsedTicket();if(text==null)return o;String z=text.replace('\r','\n').replace('•',' ').replace('|',' ');Matcher d=DATE.matcher(z);if(d.find()){String y=d.group(3);if(y.length()==2)y="20"+y;o.date=normalizeDate(d.group(1)+"."+d.group(2)+"."+y);}Set<String> seen=new LinkedHashSet<>();String[] l=z.split("\\n+");for(String x:l)add(ints(x),o,seen);for(int i=0;i<l.length;i++){String b=l[i];for(int j=i+1;j<l.length&&j<=i+2;j++){b+=" "+l[j];add(ints(b),o,seen);}}if(o.grids.isEmpty()){String q=DATE.matcher(z).replaceAll(" ").replaceAll("(?i)CHF\\s*\\d+(?:[.,]\\d{1,2})?"," ");List<Integer>a=ints(q);for(int i=0;i+6<a.size()&&o.grids.size()<20;i++){TicketGrid g=from(a.subList(i,Math.min(a.size(),i+9)));if(g!=null&&seen.add(g.compact()))o.grids.add(g);}}return o;}
 public static int countLotteryNumbers(String t){int c=0;for(int v:ints(t==null?"":t))if(v>=1&&v<=50)c++;return c;}
 static void add(List<Integer>v,ParsedTicket o,Set<String>s){for(int i=0;i+6<v.size()&&o.grids.size()<20;i++){TicketGrid g=from(v.subList(i,Math.min(v.size(),i+9)));if(g!=null&&s.add(g.compact()))o.grids.add(g);}}
 static List<Integer>ints(String s){List<Integer>r=new ArrayList<>();Matcher m=NUMBER.matcher(s);while(m.find())try{r.add(Integer.parseInt(m.group(1)));}catch(Exception e){}return r;}
 static TicketGrid from(List<Integer>v){if(v.size()<7)return null;for(int st=0;st+6<v.size();st++){List<Integer>n=new ArrayList<>(),q=new ArrayList<>();boolean ok=true;for(int i=0;i<5;i++){int x=v.get(st+i);if(x<1||x>50||n.contains(x)){ok=false;break;}n.add(x);}if(!ok)continue;for(int i=5;i<7;i++){int x=v.get(st+i);if(x<1||x>12||q.contains(x)){ok=false;break;}q.add(x);}if(ok)return new TicketGrid(n,q);}return null;}
 public static String normalizeDate(String r){for(String p:new String[]{"d.M.yyyy","dd.MM.yyyy","yyyy-MM-dd"})try{SimpleDateFormat i=new SimpleDateFormat(p,Locale.FRANCE);i.setLenient(false);Date d=i.parse(r);return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(d);}catch(Exception e){}return r;}
}''')
# scanner patches
f=p/'app/src/main/java/ch/piiwii/euroscan/ScannerActivity.java'; s=f.read_text()
s=s.replace('import android.widget.FrameLayout;','import android.widget.FrameLayout;\nimport android.widget.Button;')
s=s.replace('private String lastGoodText = "";','private String lastGoodText = "";\n    private String lastObservedText = "";')
needle='''        bottomCard.addView(status);\n        bottomCard.addView(space(5));\n        bottomCard.addView(detail);'''
rep='''        bottomCard.addView(status);\n        bottomCard.addView(space(5));\n        bottomCard.addView(detail);\n        bottomCard.addView(space(10));\n        Button analyseNow = new Button(this); analyseNow.setText("Analyser maintenant"); analyseNow.setAllCaps(false); analyseNow.setTextColor(Color.rgb(5,18,39)); analyseNow.setTypeface(null,Typeface.BOLD); analyseNow.setBackground(round(Color.rgb(255,198,65),16)); analyseNow.setOnClickListener(v -> forceAnalyse()); bottomCard.addView(analyseNow,new LinearLayout.LayoutParams(-1,dp(46)));'''
s=s.replace(needle,rep)
a=s.index('    private void evaluate(String text) {'); b=s.index('    private void finishScan() {',a)
new=r'''    private void evaluate(String text) {
        if(completed||text==null)return; String clean=text.trim(); if(!clean.isEmpty())lastObservedText=clean;
        String u=clean.toUpperCase(Locale.ROOT); boolean word=u.contains("EUROMILLION")||u.contains("EURO MILLION")||u.contains("SWISS WIN")||u.contains("LOTERIE")||u.contains("ETOILE")||u.contains("ÉTOILE");
        TicketParser.ParsedTicket p=TicketParser.parse(clean); boolean grid=!p.grids.isEmpty(); int clues=TicketParser.countLotteryNumbers(clean);
        if(clean.length()<20||(!word&&!grid&&clues<7)){stableFrames=0;lastSignature="";main.post(()->{status.setText(clean.length()>8?"Lecture en cours…":"Recherche du ticket…");detail.setText("Cadre le ticket entier, rapproche-le et évite les reflets.");overlay.setReady(false);});return;}
        if(!grid){stableFrames=0;lastSignature="";main.post(()->{status.setText("Ticket vu — je cherche les grilles…");detail.setText(clues+" éléments numériques repérés. Garde le billet immobile ou touche « Analyser maintenant ».");overlay.setReady(false);});return;}
        StringBuilder q=new StringBuilder(p.date==null?"?":p.date);for(int i=0;i<Math.min(p.grids.size(),12);i++)q.append('|').append(p.grids.get(i).compact());String sig=q.toString();if(sig.equals(lastSignature))stableFrames++;else{lastSignature=sig;stableFrames=1;}lastGoodText=clean;
        main.post(()->{overlay.setReady(true);status.setText(stableFrames>=2?"Lecture stable ✓":"Grille détectée — confirme la lecture…");detail.setText(p.grids.size()+" grille"+(p.grids.size()>1?"s":"")+" repérée"+(p.grids.size()>1?"s":"")+".");}); if(stableFrames>=2)finishScan();
    }
    private void forceAnalyse(){if(completed)return;String t=!lastGoodText.isEmpty()?lastGoodText:lastObservedText;if(t==null||t.trim().length()<8){status.setText("Rien de lisible pour l'instant");return;}TicketParser.ParsedTicket p=TicketParser.parse(t);if(p.grids.isEmpty()&&TicketParser.countLotteryNumbers(t)<7){status.setText("Numéros pas encore lisibles");detail.setText("Essaie sans reflet, bien à plat, puis réessaie.");return;}lastGoodText=t;finishScan();}

'''
s=s[:a]+new+s[b:]; f.write_text(s)
# icon placeholder; workflow replaces it with native vector resource
parts=''.join((Path('.ci/euroscan133')/f'icon.part{i:02d}').read_text() for i in range(12))
out=p/'app/src/main/res/drawable-nodpi/euroscan_icon.png';out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(base64.b64decode(parts))
print('1.3.3 patched')
