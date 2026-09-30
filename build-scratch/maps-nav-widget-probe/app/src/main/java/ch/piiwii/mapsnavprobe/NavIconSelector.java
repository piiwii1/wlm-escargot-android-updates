package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;

public final class NavIconSelector {
    private NavIconSelector() {}

    public static Bitmap render(String arrow, String instruction, int sizePx) {
        Bitmap reference = ReferenceArrowRenderer.render(arrow, instruction, sizePx);
        return reference != null ? reference : NavIconRenderer.render(arrow, instruction, sizePx);
    }
}
