package ch.piiwii.modeconvoi;

import java.util.Random;
import java.util.UUID;

public final class ParticipantDefaults {
    private static final String[] MARKER_COLORS = {
            "#EF4444", "#3B82F6", "#22C55E", "#A855F7", "#F97316",
            "#14B8A6", "#EC4899", "#EAB308", "#64748B", "#06B6D4"
    };

    private static final String[] FUNNY_NAMES = {
            "TurboMarmotte", "PapyNitro", "TartifletteRacing", "ChamoisPressé", "BiscotteTurbo",
            "KlaxonSauvage", "PatateGTI", "CroissantRacing", "PneuMou", "GPSPerdu", "VirageMystère",
            "CaféTurbo", "CornichonSport", "BaguetteExpress", "MoustacheRacing", "PistonRigolo",
            "RacletteFurieuse", "MarmotteGTI", "CactusTurbo", "FromagePressé", "CapitaineKlaxon",
            "BiberonRacing", "ChaussetteTurbo", "EscargotNitro", "PandaPressé", "FriteSport",
            "BananeRacing", "PouletTurbo", "PneuCarré", "ChamoisNitro", "RacletteExpress", "BiscotteGTI"
    };

    private ParticipantDefaults() {}

    public static void ensure(AppPrefs prefs) {
        if (prefs.getBool("participantDefaultsV0312", false) || prefs.hasActiveConvoy()) return;

        String oldName = prefs.get("profileName", "").trim();
        if (oldName.isEmpty() || "PiiWii".equalsIgnoreCase(oldName) || "Conducteur".equalsIgnoreCase(oldName)
                || oldName.matches("(?i)user\\s*\\d+")) {
            prefs.put("profileName", FUNNY_NAMES[new Random().nextInt(FUNNY_NAMES.length)]);
        }

        String oldVehicle = prefs.get("profileVehicle", "").trim();
        if ("VW Polo GTI".equalsIgnoreCase(oldVehicle) || "Véhicule".equalsIgnoreCase(oldVehicle)) {
            prefs.put("profileVehicle", "");
        }

        String current = prefs.get("profileVehicleMarkerColor", "");
        if (current.isEmpty() || "#FFB514".equalsIgnoreCase(current)) {
            int idx = Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), MARKER_COLORS.length);
            prefs.put("profileVehicleMarkerColor", MARKER_COLORS[idx]);
        }

        prefs.putBool("participantDefaultsV0312", true);
    }
}
