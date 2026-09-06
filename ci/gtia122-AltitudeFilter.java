package ch.piiwii.gtialtimeter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

final class AltitudeFilter {
    static final class Result {
        final boolean accepted;
        final double filtered;
        final String reason;
        Result(boolean accepted, double filtered, String reason) {
            this.accepted = accepted; this.filtered = filtered; this.reason = reason;
        }
    }

    private final Deque<Double> window = new ArrayDeque<>();
    private double filtered = Double.NaN;
    private double lastRaw = Double.NaN;
    private long lastTime = 0L;

    void reset() {
        window.clear();
        filtered = Double.NaN;
        lastRaw = Double.NaN;
        lastTime = 0L;
    }

    Result push(double raw, float horizontalAccuracy, float verticalAccuracy, long timeMs) {
        if (Double.isNaN(raw) || Double.isInfinite(raw)) return new Result(false, filtered, "altitude invalide");
        if (!Float.isNaN(horizontalAccuracy) && horizontalAccuracy > 220f) return new Result(false, filtered, "précision horizontale > 220 m");
        if (!Float.isNaN(verticalAccuracy) && verticalAccuracy > 160f) return new Result(false, filtered, "précision verticale > 160 m");

        if (!Double.isNaN(lastRaw) && lastTime > 0L && timeMs > lastTime) {
            double dt = Math.max(0.25, (timeMs - lastTime) / 1000.0);
            double allowed = 35.0 + 20.0 * dt;
            if (Math.abs(raw - lastRaw) > allowed) {
                lastRaw = raw;
                lastTime = timeMs;
                return new Result(false, filtered, "variation GPS aberrante");
            }
        }
        lastRaw = raw;
        lastTime = timeMs;

        window.addLast(raw);
        while (window.size() > 5) window.removeFirst();
        List<Double> sorted = new ArrayList<>(window);
        Collections.sort(sorted);
        double median = sorted.get(sorted.size() / 2);

        if (Double.isNaN(filtered)) {
            filtered = median;
        } else {
            double delta = Math.abs(median - filtered);
            double alpha = delta > 20.0 ? 0.58 : (delta > 7.0 ? 0.36 : 0.20);
            if (!Float.isNaN(horizontalAccuracy) && horizontalAccuracy > 60f) alpha *= 0.75;
            filtered += alpha * (median - filtered);
        }
        return new Result(true, filtered, "OK");
    }
}
