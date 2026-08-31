package ch.piiwii.modeconvoi;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Owns convoy session lifecycle and authenticated administration requests.
 *
 * UI classes keep dialogs/navigation only. Session JSON construction,
 * authentication bodies, server calls and persisted session state live here.
 */
public final class ConvoySessionManager {
    private final AppPrefs prefs;
    private final String fallbackServer;

    public ConvoySessionManager(AppPrefs prefs, String fallbackServer) {
        this.prefs = prefs;
        this.fallbackServer = fallbackServer == null ? "" : fallbackServer;
    }

    public JSONObject create(String rawName) throws Exception {
        ensureOfficialServerForNewSession();
        ConvoyApi.get(server(), "/health");
        JSONObject body = new JSONObject()
                .put("convoyName", rawName == null ? "" : rawName.trim())
                .put("participant", participantBody());
        JSONObject response = ConvoyApi.post(server(), "/api/convoys", body, null);
        saveSession(response);
        ConvoySnapshotRepository.invalidate();
        return response;
    }

    public JSONObject join(String rawCode) throws Exception {
        String code = normalizeJoinCode(rawCode);
        ensureOfficialServerForNewSession();
        ConvoyApi.get(server(), "/health");
        JSONObject response = ConvoyApi.post(
                server(),
                "/api/convoys/" + code + "/join",
                participantBody(),
                null);
        saveSession(response);
        prefs.remove("pendingJoin");
        ConvoySnapshotRepository.invalidate();
        return response;
    }

    public void leaveBestEffort() {
        if (!prefs.hasActiveConvoy()) return;
        try {
            ConvoyApi.post(server(), convoyPath("/leave"), authBody(), null);
        } catch (Exception ignored) {
            // Leaving the local session must remain possible even if the network is gone.
        } finally {
            ConvoySnapshotRepository.invalidate();
        }
    }

    public JSONObject close() throws Exception {
        JSONObject response = ConvoyApi.post(server(), convoyPath("/close"), authBody(), adminKey());
        ConvoySnapshotRepository.invalidate();
        return response;
    }

    public JSONObject rename(String rawName) throws Exception {
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() < 2) throw new IllegalArgumentException("Nom trop court");
        JSONObject response = ConvoyApi.post(
                server(), convoyPath("/name"), authBody().put("name", name), adminKey());
        prefs.put("convoyName", response.optString("convoyName", name));
        ConvoySnapshotRepository.invalidate();
        return response;
    }

    public JSONObject setParticipantRole(String targetParticipantId, String role) throws Exception {
        JSONObject response = ConvoyApi.post(
                server(), convoyPath("/role"),
                authBody().put("targetParticipantId", targetParticipantId).put("role", role == null ? "" : role),
                adminKey());
        ConvoySnapshotRepository.invalidate();
        return response;
    }

    public JSONObject removeParticipant(String targetParticipantId) throws Exception {
        JSONObject response = ConvoyApi.post(
                server(), convoyPath("/remove"),
                authBody().put("targetParticipantId", targetParticipantId),
                adminKey());
        ConvoySnapshotRepository.invalidate();
        return response;
    }

    public JSONObject setRally(String name, String desiredTime, double lat, double lon) throws Exception {
        JSONObject response = ConvoyApi.post(
                server(), convoyPath("/rally"),
                authBody()
                        .put("name", name == null ? "" : name.trim())
                        .put("desiredTime", desiredTime == null ? "" : desiredTime.trim())
                        .put("lat", lat)
                        .put("lon", lon),
                adminKey());
        ConvoySnapshotRepository.invalidate();
        return response;
    }

    public JSONObject sendGeneralStop() throws Exception {
        JSONObject response = ConvoyApi.post(
                server(), convoyPath("/emergency-stop"), authBody(), adminKey());
        ConvoySnapshotRepository.invalidate();
        return response;
    }

    public String normalizeJoinCode(String raw) {
        String code = raw == null ? "" : raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9]{6}")) throw new IllegalArgumentException("Code invalide");
        return code;
    }

    private JSONObject participantBody() throws Exception {
        return new JSONObject()
                .put("name", prefs.get("profileName", "Conducteur"))
                .put("vehicle", prefs.get("profileVehicle", "Véhicule"))
                .put("vehicleColor", prefs.get("profileColor", ""))
                .put("vehicleIcon", prefs.get("profileVehicleIcon", "🚗"))
                .put("vehicleMarkerColor", prefs.get("profileVehicleMarkerColor", "#FFB514"))
                .put("vehicleImage", prefs.get("profileVehicleImage", ""));
    }

    private JSONObject authBody() throws Exception {
        return new JSONObject()
                .put("participantId", prefs.get("participantId", ""))
                .put("token", prefs.get("token", ""));
    }

    private void saveSession(JSONObject response) {
        prefs.put("code", response.optString("code"));
        prefs.put("convoyName", response.optString("convoyName"));
        prefs.put("participantId", response.optString("participantId"));
        prefs.put("token", response.optString("token"));
        prefs.remove("talkieLastError");
        prefs.putLong("lastVoiceId", 0);
        prefs.putLong("liveSignalLastId", 0);
        if (response.has("participantName") && !response.optString("participantName", "").isEmpty()) {
            prefs.put("profileName", response.optString("participantName"));
        }
        if (response.has("adminKey")) prefs.put("adminKey", response.optString("adminKey"));
        if (response.has("lastEventId")) prefs.putLong("lastEventId", response.optLong("lastEventId"));
    }

    private void ensureOfficialServerForNewSession() {
        if (!prefs.hasActiveConvoy()) prefs.put("serverUrl", fallbackServer);
    }

    private String server() {
        return prefs.get("serverUrl", fallbackServer);
    }

    private String adminKey() {
        return prefs.get("adminKey", "");
    }

    private String convoyPath(String suffix) {
        return "/api/convoys/" + prefs.get("code", "") + suffix;
    }
}
