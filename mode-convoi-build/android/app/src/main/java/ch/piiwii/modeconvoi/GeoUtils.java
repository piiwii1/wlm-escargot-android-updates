package ch.piiwii.modeconvoi;

public final class GeoUtils {
    private static final double R = 6371000.0;
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2-lat1), dl = Math.toRadians(lon2-lon1);
        double a = Math.sin(dp/2)*Math.sin(dp/2) + Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double p1=Math.toRadians(lat1), p2=Math.toRadians(lat2), dl=Math.toRadians(lon2-lon1);
        double y=Math.sin(dl)*Math.cos(p2);
        double x=Math.cos(p1)*Math.sin(p2)-Math.sin(p1)*Math.cos(p2)*Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y,x))+360)%360;
    }
    public static double signedProjection(double distance, double bearingToOther, double travelBearing) {
        double delta = Math.toRadians((((bearingToOther-travelBearing)+540)%360)-180);
        return distance * Math.cos(delta);
    }
    public static String humanDistance(double m) {
        if (!Double.isFinite(m)) return "—";
        if (m < 1000) return "≈ " + Math.max(10, Math.round(m/10.0)*10) + " m";
        if (m < 10000) return String.format(java.util.Locale.getDefault(), "≈ %.1f km", m/1000.0);
        return "≈ " + Math.round(m/1000.0) + " km";
    }
}
