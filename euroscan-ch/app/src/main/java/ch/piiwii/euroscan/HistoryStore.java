package ch.piiwii.euroscan;
import android.content.*;import org.json.*;
public class HistoryStore {
    private final SharedPreferences p; public HistoryStore(Context c){p=c.getSharedPreferences("history",Context.MODE_PRIVATE);}
    public void add(String date,String grids,String result,double total,boolean exact){try{JSONArray a=new JSONArray(p.getString("items","[]"));JSONObject o=new JSONObject();o.put("date",date);o.put("grids",grids);o.put("result",result);o.put("total",total);o.put("exact",exact);o.put("time",System.currentTimeMillis());JSONArray n=new JSONArray();n.put(o);for(int i=0;i<a.length()&&i<49;i++)n.put(a.get(i));p.edit().putString("items",n.toString()).apply();}catch(Exception ignored){}}
    public JSONArray all(){try{return new JSONArray(p.getString("items","[]"));}catch(Exception e){return new JSONArray();}} public void clear(){p.edit().remove("items").apply();}
}
