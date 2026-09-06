package ch.piiwii.gtigps;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public final class NavigationEngine {
    private static final String USER_AGENT = "GTI-GPS-Widget/1.2.0 (PiiWii)";

    public interface DestinationCallback {
        void onResolved(String name, double lat, double lon);
        void onError(String message);
    }

    public interface RouteCallback {
        void onRoute(double distanceMeters, double durationSeconds, String instruction);
        void onError(String message);
    }

    private NavigationEngine() {}

    public static void resolveDestination(Context context, String raw, DestinationCallback callback) {
        final String query = raw == null ? "" : raw.trim();
        if (query.length() < 2) {
            postError(callback, "Saisis une destination.");
            return;
        }

        double[] direct = parseCoordinates(query);
        if (direct != null) {
            postResolved(callback, String.format(Locale.US, "%.5f, %.5f", direct[0], direct[1]), direct[0], direct[1]);
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&addressdetails=0&q="
                        + URLEncoder.encode(query, "UTF-8");
                connection = open(url);
                JSONArray results = new JSONArray(read(connection));
                if (results.length() == 0) throw new Exception("Destination introuvable");
                JSONObject item = results.getJSONObject(0);
                double lat = Double.parseDouble(item.getString("lat"));
                double lon = Double.parseDouble(item.getString("lon"));
                String name = item.optString("display_name", query);
                postResolved(callback, name, lat, lon);
            } catch (Exception e) {
                postError(callback, cleanError(e, "Impossible de trouver cette destination."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "GTI-GPS-geocode").start();
    }

    public static void requestRoute(Context context, double startLat, double startLon,
                                    double destLat, double destLon, RouteCallback callback) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String url = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/driving/%.7f,%.7f;%.7f,%.7f" +
                                "?overview=full&geometries=geojson&steps=true&alternatives=false",
                        startLon, startLat, destLon, destLat);
                connection = open(url);
                JSONObject root = new JSONObject(read(connection));
                if (!"Ok".equalsIgnoreCase(root.optString("code", ""))) {
                    throw new Exception("Aucun itinéraire routier disponible");
                }
                JSONArray routes = root.getJSONArray("routes");
                if (routes.length() == 0) throw new Exception("Aucun itinéraire routier disponible");
                JSONObject route = routes.getJSONObject(0);
                double distance = route.optDouble("distance", 0d);
                double duration = route.optDouble("duration", 0d);
                String geometry = encodeGeometry(route.getJSONObject("geometry").getJSONArray("coordinates"));
                StepInfo step = parseNextStep(route);

                GpsState.prefs(app).edit()
                        .putString(GpsState.KEY_ROUTE_GEOMETRY, geometry)
                        .putFloat(GpsState.KEY_ROUTE_DISTANCE_M, (float) distance)
                        .putFloat(GpsState.KEY_ROUTE_DURATION_S, (float) duration)
                        .putLong(GpsState.KEY_ROUTE_UPDATED, System.currentTimeMillis())
                        .putString(GpsState.KEY_NEXT_INSTRUCTION, step.instruction)
                        .putFloat(GpsState.KEY_NEXT_DISTANCE_M, (float) step.distance)
                        .remove(GpsState.KEY_ROUTE_ERROR)
                        .apply();
                GTIGpsWidgetProvider.updateAll(app);
                postRoute(callback, distance, duration, step.instruction);
            } catch (Exception e) {
                String message = cleanError(e, "Calcul d’itinéraire indisponible.");
                GpsState.prefs(app).edit().putString(GpsState.KEY_ROUTE_ERROR, message).apply();
                postRouteError(callback, message);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "GTI-GPS-route").start();
    }

    private static HttpURLConnection open(String rawUrl) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(rawUrl).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setRequestMethod("GET");
        c.setRequestProperty("User-Agent", USER_AGENT);
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new Exception("Serveur HTTP " + code);
        return c;
    }

    private static String read(HttpURLConnection c) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private static double[] parseCoordinates(String raw) {
        String normalized = raw.replace(';', ',').replaceAll("\\s+", "");
        String[] p = normalized.split(",");
        if (p.length != 2) return null;
        try {
            double lat = Double.parseDouble(p[0]);
            double lon = Double.parseDouble(p[1]);
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null;
            return new double[]{lat, lon};
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String encodeGeometry(JSONArray coordinates) throws Exception {
        StringBuilder sb = new StringBuilder();
        int max = Math.min(coordinates.length(), 1800);
        int stride = Math.max(1, coordinates.length() / max);
        for (int i = 0; i < coordinates.length(); i += stride) {
            JSONArray point = coordinates.getJSONArray(i);
            if (sb.length() > 0) sb.append(';');
            sb.append(String.format(Locale.US, "%.6f,%.6f", point.getDouble(1), point.getDouble(0)));
        }
        if (coordinates.length() > 1 && (coordinates.length() - 1) % stride != 0) {
            JSONArray point = coordinates.getJSONArray(coordinates.length() - 1);
            sb.append(';').append(String.format(Locale.US, "%.6f,%.6f", point.getDouble(1), point.getDouble(0)));
        }
        return sb.toString();
    }

    private static StepInfo parseNextStep(JSONObject route) {
        try {
            JSONArray legs = route.getJSONArray("legs");
            if (legs.length() == 0) return new StepInfo("Suivez l’itinéraire", 0);
            JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");
            if (steps.length() == 0) return new StepInfo("Suivez l’itinéraire", 0);

            JSONObject chosen = steps.getJSONObject(0);
            for (int i = 0; i < steps.length(); i++) {
                JSONObject candidate = steps.getJSONObject(i);
                String type = candidate.optJSONObject("maneuver") != null
                        ? candidate.getJSONObject("maneuver").optString("type", "") : "";
                if (!"depart".equals(type) && candidate.optDouble("distance", 0d) > 5d) {
                    chosen = candidate;
                    break;
                }
            }
            JSONObject maneuver = chosen.optJSONObject("maneuver");
            String type = maneuver != null ? maneuver.optString("type", "continue") : "continue";
            String modifier = maneuver != null ? maneuver.optString("modifier", "") : "";
            String road = chosen.optString("name", "");
            double dist = chosen.optDouble("distance", 0d);
            return new StepInfo(instruction(type, modifier, road), dist);
        } catch (Exception ignored) {
            return new StepInfo("Suivez l’itinéraire", 0);
        }
    }

    private static String instruction(String type, String modifier, String road) {
        String action;
        if ("arrive".equals(type)) action = "Arrivée";
        else if ("roundabout".equals(type) || "rotary".equals(type)) action = "Prenez le giratoire";
        else if ("merge".equals(type)) action = "Insérez-vous";
        else if ("fork".equals(type)) action = "À l’embranchement";
        else if ("on ramp".equals(type)) action = "Prenez la bretelle";
        else if ("off ramp".equals(type)) action = "Prenez la sortie";
        else if (modifier.contains("left")) action = modifier.contains("slight") ? "Légèrement à gauche" : "Tournez à gauche";
        else if (modifier.contains("right")) action = modifier.contains("slight") ? "Légèrement à droite" : "Tournez à droite";
        else if (modifier.contains("uturn")) action = "Faites demi-tour";
        else action = "Continuez";
        if (road != null && road.trim().length() > 0 && !"Arrivée".equals(action)) action += " · " + road.trim();
        return action;
    }

    private static String cleanError(Exception e, String fallback) {
        String msg = e.getMessage();
        return msg == null || msg.trim().isEmpty() ? fallback : msg;
    }

    private static void postResolved(DestinationCallback cb, String name, double lat, double lon) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(() -> cb.onResolved(name, lat, lon));
    }

    private static void postError(DestinationCallback cb, String msg) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(() -> cb.onError(msg));
    }

    private static void postRoute(RouteCallback cb, double d, double t, String instruction) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(() -> cb.onRoute(d, t, instruction));
    }

    private static void postRouteError(RouteCallback cb, String msg) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(() -> cb.onError(msg));
    }

    private static final class StepInfo {
        final String instruction;
        final double distance;
        StepInfo(String instruction, double distance) {
            this.instruction = instruction;
            this.distance = distance;
        }
    }
}
