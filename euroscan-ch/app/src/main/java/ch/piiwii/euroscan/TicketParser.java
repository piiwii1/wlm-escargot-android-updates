package ch.piiwii.euroscan;
import java.text.*;import java.util.*;import java.util.regex.*;
public final class TicketParser {
    public static class ParsedTicket{public String date;public final List<TicketGrid> grids=new ArrayList<>();}
    private static final Pattern DATE=Pattern.compile("(?<!\\d)([0-3]?\\d)[./-]([01]?\\d)[./-](20\\d{2})(?!\\d)"),NUMBER=Pattern.compile("(?<!\\d)(\\d{1,2})(?!\\d)");
    public static ParsedTicket parse(String text){ParsedTicket o=new ParsedTicket();if(text==null)return o;Matcher d=DATE.matcher(text);if(d.find())o.date=normalizeDate(d.group(1)+"."+d.group(2)+"."+d.group(3));Set<String> seen=new LinkedHashSet<>();for(String line:text.replace('\r','\n').split("\\n+")){TicketGrid g=from(ints(line));if(g!=null&&seen.add(g.compact()))o.grids.add(g);}if(o.grids.isEmpty()){List<Integer> v=ints(text);for(int i=0;i+6<v.size();i++){TicketGrid g=from(v.subList(i,Math.min(v.size(),i+7)));if(g!=null&&seen.add(g.compact()))o.grids.add(g);}}return o;}
    private static List<Integer> ints(String s){List<Integer> r=new ArrayList<>();Matcher m=NUMBER.matcher(s);while(m.find())try{r.add(Integer.parseInt(m.group(1)));}catch(Exception ignored){}return r;}
    private static TicketGrid from(List<Integer> v){if(v.size()<7)return null;for(int st=0;st+6<v.size();st++){List<Integer> n=new ArrayList<>(),s=new ArrayList<>();boolean ok=true;for(int i=0;i<5;i++){int x=v.get(st+i);if(x<1||x>50||n.contains(x)){ok=false;break;}n.add(x);}if(!ok)continue;for(int i=5;i<7;i++){int x=v.get(st+i);if(x<1||x>12||s.contains(x)){ok=false;break;}s.add(x);}if(ok)return new TicketGrid(n,s);}return null;}
    public static String normalizeDate(String raw){for(String p:new String[]{"d.M.yyyy","dd.MM.yyyy","yyyy-MM-dd"})try{SimpleDateFormat in=new SimpleDateFormat(p,Locale.FRANCE);in.setLenient(false);Date d=in.parse(raw);return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(d);}catch(Exception ignored){}return raw;}
}
