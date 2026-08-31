package ch.piiwii.modeconvoi;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Resolves participant-relative positions and rally presentation data from a convoy snapshot.
 *
 * UI code should only render the resolved values. This keeps snapshot interpretation,
 * stale-position filtering and ahead/behind decisions consistent across Home, Map and
 * administration screens.
 */
public final class ConvoyPositionResolver {
    private static final long MAX_POSITION_AGE_MS = 120_000L;

    public static final class Relative {
        public final JSONObject ahead;
        public final JSONObject behind;

        Relative(JSONObject ahead, JSONObject behind) {
            this.ahead = ahead;
            this.behind = behind;
        }
    }

    public static final class RallyInfo {
        public final JSONObject rally;
        public final String desiredTime;
        public final String distanceText;
        public final String subtitle;

        RallyInfo(JSONObject rally, String desiredTime, String distanceText, String subtitle) {
            this.rally = rally;
            this.desiredTime = desiredTime;
            this.distanceText = distanceText;
            this.subtitle = subtitle;
        }
    }

    private final AppPrefs prefs;

    public ConvoyPositionResolver(AppPrefs prefs) {
        this.prefs = prefs;
    }

    public JSONObject findMe(JSONObject snapshot) {
        JSONArray participants = snapshot == null ? null : snapshot.optJSONArray("participants");
        if (participants == null) return null;
        String participantId = prefs.get("participantId", "");
        for (int i = 0; i < participants.length(); i++) {
            JSONObject participant = participants.optJSONObject(i);
            if (participant != null && participantId.equals(participant.optString("id"))) return participant;
        }
        return null;
    }

    public JSONObject ownLocation(JSONObject snapshot) {
        JSONObject me = findMe(snapshot);
        return me == null ? null : me.optJSONObject("location");
    }

    public Relative resolveRelative(JSONObject snapshot) {
        if (snapshot == null) return new Relative(null, null);
        JSONObject me = findMe(snapshot);
        if (me == null) return new Relative(null, null);
        JSONObject myLocation = me.optJSONObject("location");
        if (myLocation == null) return new Relative(null, null);

        long serverTime = snapshot.optLong("serverTime", System.currentTimeMillis());
        long myPositionAt = myLocation.optLong("receivedAt", 0);
        if (!isFresh(serverTime, myPositionAt)) return new Relative(null, null);

        double myLat = myLocation.optDouble("lat");
        double myLon = myLocation.optDouble("lon");
        JSONObject rally = snapshot.optJSONObject("rally");
        double myRallyDistance = Double.NaN;
        if (rally != null) {
            myRallyDistance = GeoUtils.distanceMeters(
                    myLat, myLon, rally.optDouble("lat"), rally.optDouble("lon"));
        }

        Double heading = myLocation.has("bearing") && !myLocation.isNull("bearing")
                ? myLocation.optDouble("bearing") : null;
        double bestAhead = Double.POSITIVE_INFINITY;
        double bestBehind = Double.POSITIVE_INFINITY;
        JSONObject ahead = null;
        JSONObject behind = null;

        JSONArray participants = snapshot.optJSONArray("participants");
        if (participants == null) return new Relative(null, null);
        String meId = prefs.get("participantId", "");

        for (int i = 0; i < participants.length(); i++) {
            JSONObject participant = participants.optJSONObject(i);
            if (participant == null || meId.equals(participant.optString("id"))) continue;
            JSONObject location = participant.optJSONObject("location");
            if (location == null) continue;
            long positionAt = location.optLong("receivedAt", 0);
            if (!isFresh(serverTime, positionAt)) continue;

            double lat = location.optDouble("lat");
            double lon = location.optDouble("lon");
            double distance = GeoUtils.distanceMeters(myLat, myLon, lat, lon);
            boolean isAhead;

            if (Double.isFinite(myRallyDistance)) {
                double otherRallyDistance = GeoUtils.distanceMeters(
                        lat, lon, rally.optDouble("lat"), rally.optDouble("lon"));
                isAhead = otherRallyDistance < myRallyDistance;
            } else if (heading != null) {
                double bearing = GeoUtils.bearing(myLat, myLon, lat, lon);
                isAhead = GeoUtils.signedProjection(distance, bearing, heading) > 0;
            } else {
                continue;
            }

            JSONObject displayParticipant = copyParticipant(participant);
            try {
                displayParticipant.put("_relative", "≈ " + GeoUtils.humanDistance(distance)
                        + (isAhead ? " devant" : " derrière"));
            } catch (Exception ignored) {}

            if (isAhead && distance < bestAhead) {
                bestAhead = distance;
                ahead = displayParticipant;
            } else if (!isAhead && distance < bestBehind) {
                bestBehind = distance;
                behind = displayParticipant;
            }
        }
        return new Relative(ahead, behind);
    }

    public RallyInfo rallyInfo(JSONObject snapshot) {
        JSONObject rally = snapshot == null ? null : snapshot.optJSONObject("rally");
        if (rally == null) return null;

        String desiredTime = rally.optString("desiredTime", "");
        String distanceText = "";
        JSONObject location = ownLocation(snapshot);
        if (location != null) {
            double distance = GeoUtils.distanceMeters(
                    location.optDouble("lat"), location.optDouble("lon"),
                    rally.optDouble("lat"), rally.optDouble("lon"));
            distanceText = GeoUtils.humanDistance(distance);
        }

        StringBuilder subtitle = new StringBuilder();
        if (!distanceText.isEmpty()) subtitle.append(distanceText);
        if (!desiredTime.isEmpty()) {
            if (subtitle.length() > 0) subtitle.append("  ·  ");
            subtitle.append(desiredTime);
        }
        if (subtitle.length() == 0) subtitle.append("Point de regroupement actif");

        return new RallyInfo(rally, desiredTime, distanceText, subtitle.toString());
    }

    private boolean isFresh(long serverTime, long positionAt) {
        return positionAt > 0 && serverTime - positionAt <= MAX_POSITION_AGE_MS;
    }

    private JSONObject copyParticipant(JSONObject participant) {
        try {
            return new JSONObject(participant.toString());
        } catch (Exception ignored) {
            return participant;
        }
    }
}
