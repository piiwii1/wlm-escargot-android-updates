package ch.piiwii.euroscan;
import java.util.*;
public final class SwissPrizeFallback {
    private static final Map<String,Double> AVG=new HashMap<>();
    static{AVG.put("5+1",400000d);AVG.put("5+0",50000d);AVG.put("4+2",5000d);AVG.put("4+1",250d);AVG.put("3+2",150d);AVG.put("4+0",90d);AVG.put("2+2",30d);AVG.put("3+1",21d);AVG.put("3+0",18d);AVG.put("1+2",15d);AVG.put("2+1",12d);AVG.put("2+0",7d);}
    private SwissPrizeFallback(){} public static Double average(int n,int s){if(n==5&&s==2)return null;return AVG.get(n+"+"+s);}
}
