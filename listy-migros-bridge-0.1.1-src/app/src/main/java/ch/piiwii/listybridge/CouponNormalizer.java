package ch.piiwii.listybridge;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;

public final class CouponNormalizer {
    private CouponNormalizer() {}
    public static JSONArray normalize(String json) throws Exception {
        String t=json==null?"":json.trim(); JSONArray src;
        if(t.startsWith("[")) src=new JSONArray(t); else {JSONObject o=new JSONObject(t);src=o.optJSONArray("coupons");if(src==null)src=new JSONArray();}
        JSONArray out=new JSONArray();
        for(int i=0;i<src.length();i++){Object v=src.opt(i);if(!(v instanceof JSONObject))continue;JSONObject c=(JSONObject)v,n=new JSONObject();String id=first(c,"id","couponId","couponID","uuid","code","key");String title=first(c,"title","name","couponTitle","promotionName","headline","shortDescription");String sub=first(c,"subtitle","description","text","body","longDescription");String benefit=first(c,"benefit","discountText","discount","value","label","advantage","pointsText");String mult=first(c,"pointsMultiplier","multiplier","factor");if(benefit.isEmpty()&&!mult.isEmpty())benefit="x"+mult+" points";if(id.isEmpty())id="migros-"+Integer.toHexString(c.toString().hashCode())+"-"+i;if(title.isEmpty())title=!sub.isEmpty()?sub:"Coupon Cumulus";n.put("id",id);n.put("title",clip(title,240));n.put("subtitle",clip(sub,240));n.put("benefit",clip(benefit,100));String until=first(c,"validUntil","valid_until","expirationDate","expiryDate","endDate","validTo","to");if(!until.isEmpty())n.put("valid_until",until);String from=first(c,"validFrom","valid_from","startDate","validFromDate","from");if(!from.isEmpty())n.put("valid_from",from);Boolean act=firstBoolean(c,"activated","isActivated","selected","active");if(act!=null)n.put("activated",act.booleanValue());JSONArray terms=new JSONArray();terms.put(title);if(!sub.isEmpty())terms.put(sub);collectTerms(c,terms,0);n.put("match_terms",terms);out.put(n);}return out;
    }
    private static void collectTerms(Object node,JSONArray out,int depth)throws Exception{if(depth>3||out.length()>=12||node==null)return;if(node instanceof JSONObject){JSONObject o=(JSONObject)node;Iterator<String>it=o.keys();while(it.hasNext()&&out.length()<12){String k=it.next();Object v=o.opt(k);String kl=k.toLowerCase();if(v instanceof String&&(kl.contains("product")||kl.contains("category")||kl.contains("name")||kl.contains("brand"))){String x=((String)v).trim();if(x.length()>2&&x.length()<100)out.put(x);}else if(v instanceof JSONObject||v instanceof JSONArray)collectTerms(v,out,depth+1);}}else if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<a.length()&&out.length()<12;i++)collectTerms(a.opt(i),out,depth+1);}}
    private static String first(JSONObject o,String...keys){for(String k:keys){Object v=find(o,k,0);if(v!=null&&!(v instanceof JSONObject)&&!(v instanceof JSONArray)){String s=String.valueOf(v).trim();if(!s.equals("null")&&!s.isEmpty())return s;}}return"";}
    private static Boolean firstBoolean(JSONObject o,String...keys){for(String k:keys){Object v=find(o,k,0);if(v instanceof Boolean)return(Boolean)v;if(v!=null){String s=String.valueOf(v);if("true".equalsIgnoreCase(s)||"1".equals(s))return true;if("false".equalsIgnoreCase(s)||"0".equals(s))return false;}}return null;}
    private static Object find(Object node,String wanted,int depth){if(depth>4||node==null)return null;if(node instanceof JSONObject){JSONObject o=(JSONObject)node;if(o.has(wanted))return o.opt(wanted);Iterator<String>it=o.keys();while(it.hasNext()){Object r=find(o.opt(it.next()),wanted,depth+1);if(r!=null)return r;}}else if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<a.length();i++){Object r=find(a.opt(i),wanted,depth+1);if(r!=null)return r;}}return null;}
    private static String clip(String s,int n){return s.length()<=n?s:s.substring(0,n);}
}
