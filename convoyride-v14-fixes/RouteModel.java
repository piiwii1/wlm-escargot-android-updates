package ch.piiwii.convoyride;

import android.util.Xml;
import org.json.*;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;

public final class RouteModel {
    public static final class Point {
        public final double lat,lng;
        public Point(double lat,double lng){this.lat=lat;this.lng=lng;}
    }
    public static final class Projection {
        public double alongMeters, offRouteMeters, lat, lng, progress;
        public int segment;
    }

    public final ArrayList<Point> points=new ArrayList<>();
    private double[] cumulative=new double[0];
    public double totalMeters=0;
    public boolean loop=false;
    public String name="Parcours";

    public RouteModel(){}
    public RouteModel(List<Point> pts){if(pts!=null)points.addAll(pts);rebuild();}

    public void rebuild(){
        cumulative=new double[points.size()];totalMeters=0;
        for(int i=1;i<points.size();i++){totalMeters+=distanceMeters(points.get(i-1).lat,points.get(i-1).lng,points.get(i).lat,points.get(i).lng);cumulative[i]=totalMeters;}
        loop=points.size()>3 && distanceMeters(points.get(0).lat,points.get(0).lng,points.get(points.size()-1).lat,points.get(points.size()-1).lng)<180;
    }
    public boolean usable(){return points.size()>=2&&totalMeters>20;}

    public Projection project(double lat,double lng){
        if(points.size()<2)return null;
        Projection best=new Projection();best.offRouteMeters=Double.MAX_VALUE;
        double refLat=Math.toRadians(lat),mx=Math.cos(refLat)*111320.0,my=110540.0;
        for(int i=0;i<points.size()-1;i++){
            Point a=points.get(i),b=points.get(i+1);
            double ax=(a.lng-lng)*mx, ay=(a.lat-lat)*my, bx=(b.lng-lng)*mx, by=(b.lat-lat)*my;
            double dx=bx-ax,dy=by-ay,den=dx*dx+dy*dy,t=den<=0?0:-(ax*dx+ay*dy)/den;t=Math.max(0,Math.min(1,t));
            double x=ax+t*dx,y=ay+t*dy,off=Math.sqrt(x*x+y*y);
            if(off<best.offRouteMeters){
                double segLen=distanceMeters(a.lat,a.lng,b.lat,b.lng);
                best.offRouteMeters=off;best.alongMeters=cumulative[i]+segLen*t;best.segment=i;
                best.lat=a.lat+(b.lat-a.lat)*t;best.lng=a.lng+(b.lng-a.lng)*t;
            }
        }
        best.progress=totalMeters<=0?0:best.alongMeters/totalMeters;return best;
    }

    public double alongGap(Projection a,Projection b){
        if(a==null||b==null)return Double.NaN;double d=Math.abs(a.alongMeters-b.alongMeters);
        if(loop&&totalMeters>0)d=Math.min(d,totalMeters-d);return d;
    }

    public JSONArray toJson(){JSONArray a=new JSONArray();for(Point p:points){JSONArray q=new JSONArray();try{q.put(round6(p.lat));q.put(round6(p.lng));a.put(q);}catch(JSONException ignored){}}return a;}
    public static RouteModel fromJson(JSONArray a,String name){RouteModel r=new RouteModel();r.name=name==null?"Parcours":name;if(a!=null)for(int i=0;i<a.length();i++){JSONArray q=a.optJSONArray(i);if(q!=null&&q.length()>=2)r.points.add(new Point(q.optDouble(0),q.optDouble(1)));}r.rebuild();return r;}

    public static RouteModel parseGpx(InputStream in)throws Exception{
        XmlPullParser x=Xml.newPullParser();x.setInput(new InputStreamReader(in,"UTF-8"));RouteModel r=new RouteModel();String routeName=null;boolean inTrack=false,inRoute=false;
        int ev=x.getEventType();while(ev!=XmlPullParser.END_DOCUMENT){
            if(ev==XmlPullParser.START_TAG){String n=x.getName();if("trk".equals(n))inTrack=true;if("rte".equals(n))inRoute=true;
                if(("trkpt".equals(n)||"rtept".equals(n))){String la=x.getAttributeValue(null,"lat"),lo=x.getAttributeValue(null,"lon");if(la!=null&&lo!=null){double lat=Double.parseDouble(la),lng=Double.parseDouble(lo);if(lat>=-90&&lat<=90&&lng>=-180&&lng<=180)r.points.add(new Point(lat,lng));}}
                else if("name".equals(n)&&(inTrack||inRoute)&&routeName==null){try{routeName=x.nextText().trim();}catch(Exception ignored){}}
            } else if(ev==XmlPullParser.END_TAG){if("trk".equals(x.getName()))inTrack=false;if("rte".equals(x.getName()))inRoute=false;}
            ev=x.next();
        }
        if(routeName!=null&&!routeName.isEmpty())r.name=routeName;r.simplify(2200);r.rebuild();if(!r.usable())throw new IOException("GPX sans parcours exploitable");return r;
    }

    private void simplify(int max){if(points.size()<=max)return;ArrayList<Point> keep=new ArrayList<>();double step=(points.size()-1.0)/(max-1.0);for(int i=0;i<max;i++)keep.add(points.get((int)Math.round(i*step)));points.clear();points.addAll(keep);}
    private static double round6(double v){return Math.round(v*1000000d)/1000000d;}
    public static double distanceMeters(double la1,double lo1,double la2,double lo2){double r=6371000,dla=Math.toRadians(la2-la1),dlo=Math.toRadians(lo2-lo1),a=Math.sin(dla/2)*Math.sin(dla/2)+Math.cos(Math.toRadians(la1))*Math.cos(Math.toRadians(la2))*Math.sin(dlo/2)*Math.sin(dlo/2);return r*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));}
}
