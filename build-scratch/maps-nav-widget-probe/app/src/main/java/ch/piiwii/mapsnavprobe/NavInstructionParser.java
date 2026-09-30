package ch.piiwii.mapsnavprobe;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NavInstructionParser {
    private NavInstructionParser() {}

    public static final class Result {
        public final String arrow;
        public final String distance;
        public final String instruction;
        public final String road;
        public final String source;

        Result(String arrow, String distance, String instruction, String road, String source) {
            this.arrow = arrow;
            this.distance = distance;
            this.instruction = instruction;
            this.road = road;
            this.source = source;
        }
    }

    private static final Pattern DISTANCE = Pattern.compile(
            "(?iu)(?:\\b(?:dans|à|a|in)\\s+)?(\\d+(?:[.,]\\d+)?\\s*(?:m|km|mètre(?:s)?|metre(?:s)?|kilomètre(?:s)?|kilometre(?:s)?|meter(?:s)?|metre(?:s)?|kilometer(?:s)?|kilometre(?:s)?))\\b");
    private static final Pattern FRENCH_EXIT = Pattern.compile("(?iu)\\b(\\d{1,2})(?:re|er|e|ème|eme)?\\s+sortie\\b");
    private static final Pattern ENGLISH_EXIT = Pattern.compile("(?iu)\\b(?:take\\s+)?(?:the\\s+)?(1st|2nd|3rd|[4-9]th|1[0-9]th)\\s+exit\\b");
    private static final Pattern ETA = Pattern.compile(
            "(?iu)^(?:arrivée|arrivee|arrival|eta)\\s*(?:à|a|at)?\\s*\\d{1,2}(?::|h)\\d{2}(?:\\s.*)?$");
    private static final Pattern CARDINAL_START = Pattern.compile(
            "(?iu)^(?:prenez|prendre|continuez|continue|head|proceed)\\s+(?:en\\s+)?(?:la\\s+)?(?:direction\\s+)?(?:nord|sud|est|ouest|nord[- ]est|nord[- ]ouest|sud[- ]est|sud[- ]ouest|north|south|east|west|northeast|northwest|southeast|southwest)\\b.*");

    public static Result parse(String title, String text, String bigText, String subText, String summary, String[] textLines) {
        List<String> values = new ArrayList<>();
        add(values, text);
        add(values, bigText);
        if (textLines != null) for (String line : textLines) add(values, line);
        add(values, title);
        add(values, subText);
        add(values, summary);

        String best = "";
        int bestScore = Integer.MIN_VALUE;
        for (String candidate : values) {
            int score = score(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (TextUtils.isEmpty(best)) {
            return new Result("·", "", "Aucune consigne", "", "");
        }

        String lower = norm(best).toLowerCase(Locale.ROOT);
        String distance = extractDistance(best);
        String road = extractRoad(best);
        if (TextUtils.isEmpty(road)) road = chooseRoad(title, subText, summary, best);

        String arrow;
        String instruction;

        if (containsAny(lower, "vous êtes arrivé", "vous etes arrive", "arrivé à destination", "arrive a destination", "destination atteinte", "you have arrived", "arrived at your destination")) {
            arrow = "⚑";
            distance = "";
            instruction = "Vous êtes arrivé";
        } else if (containsAny(lower, "demi-tour", "demi tour", "u-turn", "u turn")) {
            arrow = "↶";
            instruction = "Faites demi-tour";
        } else if (containsAny(lower, "rond-point", "rond point", "roundabout", "traffic circle")) {
            arrow = "⟳";
            String exit = extractExit(best);
            instruction = TextUtils.isEmpty(exit) ? "Entrez dans le rond-point" : "Prenez la " + exit + " sortie";
        } else if (containsAny(lower, "légèrement à gauche", "legerement a gauche", "slight left", "keep slightly left")) {
            arrow = "↖";
            instruction = "Légèrement à gauche";
        } else if (containsAny(lower, "légèrement à droite", "legerement a droite", "slight right", "keep slightly right")) {
            arrow = "↗";
            instruction = "Légèrement à droite";
        } else if (containsAny(lower, "fortement à gauche", "fortement a gauche", "sharp left")) {
            arrow = "↙";
            instruction = "Tournez fortement à gauche";
        } else if (containsAny(lower, "fortement à droite", "fortement a droite", "sharp right")) {
            arrow = "↘";
            instruction = "Tournez fortement à droite";
        } else if (containsAny(lower, "tournez à gauche", "tourner à gauche", "tournez a gauche", "tourner a gauche", "turn left")) {
            arrow = "←";
            instruction = "Tournez à gauche";
        } else if (containsAny(lower, "tournez à droite", "tourner à droite", "tournez a droite", "tourner a droite", "turn right")) {
            arrow = "→";
            instruction = "Tournez à droite";
        } else if (containsAny(lower, "restez à gauche", "restez a gauche", "serrez à gauche", "serrez a gauche", "keep left", "stay left")) {
            arrow = "↖";
            instruction = "Restez à gauche";
        } else if (containsAny(lower, "restez à droite", "restez a droite", "serrez à droite", "serrez a droite", "keep right", "stay right")) {
            arrow = "↗";
            instruction = "Restez à droite";
        } else if (containsAny(lower, "sortie à gauche", "sortie a gauche", "exit left")) {
            arrow = "↖";
            instruction = "Prenez la sortie à gauche";
        } else if (containsAny(lower, "sortie à droite", "sortie a droite", "exit right")) {
            arrow = "↗";
            instruction = "Prenez la sortie à droite";
        } else if (containsAny(lower, "prenez la sortie", "take the exit", "take exit")) {
            arrow = "↗";
            String exit = extractExit(best);
            instruction = TextUtils.isEmpty(exit) ? "Prenez la sortie" : "Prenez la sortie " + exit;
        } else if (containsAny(lower, "fusionnez", "insérez-vous", "inserez-vous", "merge")) {
            arrow = "↗";
            instruction = "Rejoignez la voie";
        } else if (containsAny(lower, "continuez tout droit", "allez tout droit", "tout droit", "continue straight", "go straight")) {
            arrow = "↑";
            instruction = "Continuez tout droit";
        } else if (CARDINAL_START.matcher(lower).matches() || containsAny(lower, "direction nord", "direction sud", "direction est", "direction ouest", "head north", "head south", "head east", "head west")) {
            arrow = "↑";
            instruction = cardinalInstruction(lower);
        } else if (containsAny(lower, "continuez", "continuer", "continue", "follow", "suivez")) {
            arrow = "↑";
            instruction = "Continuez";
        } else {
            arrow = "↑";
            instruction = simplifyFallback(best, distance);
        }

        return new Result(arrow, normalizeDistance(distance), instruction, cleanRoad(road), best);
    }

    private static int score(String s) {
        if (TextUtils.isEmpty(s)) return -1000;
        String l = norm(s).toLowerCase(Locale.ROOT);
        if (isGeneric(l)) return -500;
        if (isEta(l)) return -400;
        int score = 0;
        if (containsAny(l, "vous êtes arrivé", "vous etes arrive", "destination atteinte", "arrivé à destination", "arrive a destination", "demi-tour", "u-turn", "rond-point", "roundabout")) score += 120;
        if (containsAny(l, "tournez", "tourner", "turn left", "turn right", "restez", "keep left", "keep right", "sortie", "exit", "fusionnez", "merge")) score += 100;
        if (containsAny(l, "continuez tout droit", "tout droit", "continue straight")) score += 80;
        if (CARDINAL_START.matcher(l).matches() || containsAny(l, "direction nord", "direction sud", "direction est", "direction ouest", "head north", "head south", "head east", "head west")) score += 75;
        if (!TextUtils.isEmpty(extractDistance(s))) score += 45;
        if (s.length() > 120) score -= 10;
        return score;
    }

    private static String chooseRoad(String title, String subText, String summary, String chosen) {
        String[] candidates = { title, subText, summary };
        for (String s : candidates) {
            if (TextUtils.isEmpty(s) || s.equals(chosen)) continue;
            String l = norm(s).toLowerCase(Locale.ROOT);
            if (isGeneric(l) || isEta(l) || score(s) >= 70 || !TextUtils.isEmpty(extractDistance(s))) continue;
            if (s.length() <= 80) return s;
        }
        return "";
    }

    private static String extractRoad(String s) {
        if (TextUtils.isEmpty(s)) return "";
        String n = norm(s);
        Pattern[] patterns = new Pattern[] {
                Pattern.compile("(?iu)\\b(?:sur|vers)\\s+(.+)$"),
                Pattern.compile("(?iu)\\b(?:onto|toward|towards)\\s+(.+)$")
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(n);
            if (m.find()) {
                String r = m.group(1).trim();
                r = DISTANCE.matcher(r).replaceAll("").replaceAll("^[,·\\-\\s]+|[,·\\-\\s]+$", "").trim();
                if (!TextUtils.isEmpty(r) && r.length() <= 100) return r;
            }
        }
        return "";
    }

    private static String extractDistance(String s) {
        if (TextUtils.isEmpty(s)) return "";
        Matcher m = DISTANCE.matcher(norm(s));
        return m.find() ? m.group(1).trim() : "";
    }

    private static String extractExit(String s) {
        Matcher fr = FRENCH_EXIT.matcher(norm(s));
        if (fr.find()) return fr.group(1) + suffixFrench(fr.group(1));
        Matcher en = ENGLISH_EXIT.matcher(norm(s));
        if (en.find()) {
            String raw = en.group(1).toLowerCase(Locale.ROOT);
            String num = raw.replaceAll("[^0-9]", "");
            return num + suffixFrench(num);
        }
        return "";
    }

    private static String cardinalInstruction(String lower) {
        if (containsAny(lower, "nord-est", "nord est", "northeast")) return "Prenez la direction nord-est";
        if (containsAny(lower, "nord-ouest", "nord ouest", "northwest")) return "Prenez la direction nord-ouest";
        if (containsAny(lower, "sud-est", "sud est", "southeast")) return "Prenez la direction sud-est";
        if (containsAny(lower, "sud-ouest", "sud ouest", "southwest")) return "Prenez la direction sud-ouest";
        if (containsAny(lower, "nord", "north")) return "Prenez la direction nord";
        if (containsAny(lower, "sud", "south")) return "Prenez la direction sud";
        if (containsAny(lower, "ouest", "west")) return "Prenez la direction ouest";
        if (containsAny(lower, "est", "east")) return "Prenez la direction est";
        return "Continuez";
    }

    private static boolean isEta(String l) {
        return ETA.matcher(norm(l)).matches();
    }

    private static String suffixFrench(String n) { return "1".equals(n) ? "re" : "e"; }

    private static String normalizeDistance(String d) {
        if (TextUtils.isEmpty(d)) return "";
        return d.replaceAll("(?iu)mètres?|metres?|meters?|metres?", "m")
                .replaceAll("(?iu)kilomètres?|kilometres?|kilometers?|kilometres?", "km")
                .replaceAll("\\s+", " ").trim();
    }

    private static String simplifyFallback(String s, String distance) {
        String x = norm(s);
        if (!TextUtils.isEmpty(distance)) x = DISTANCE.matcher(x).replaceAll("");
        x = x.replaceAll("(?iu)^(?:dans|à|a|in)\\s*[,·:\\-]?\\s*", "")
                .replaceAll("^[,·:\\-\\s]+|[,·:\\-\\s]+$", "").trim();
        if (x.length() > 72) x = x.substring(0, 69).trim() + "…";
        return TextUtils.isEmpty(x) ? "Continuez" : x;
    }

    private static String cleanRoad(String r) {
        if (TextUtils.isEmpty(r)) return "";
        String x = norm(r).replaceAll("^[,·:\\-\\s]+|[,·:\\-\\s]+$", "").trim();
        if (x.length() > 70) x = x.substring(0, 67).trim() + "…";
        return x;
    }

    private static boolean isGeneric(String l) {
        return TextUtils.isEmpty(l) || "google maps".equals(l) || "maps".equals(l) || "navigation".equals(l);
    }

    private static boolean containsAny(String source, String... needles) {
        for (String n : needles) if (source.contains(n)) return true;
        return false;
    }

    private static void add(List<String> values, String s) {
        if (TextUtils.isEmpty(s)) return;
        String n = norm(s);
        for (String existing : values) if (existing.equals(n)) return;
        values.add(n);
    }

    private static String norm(String s) {
        return s == null ? "" : s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }
}
