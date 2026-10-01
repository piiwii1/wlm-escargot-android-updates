package ch.piiwii.mapsnavprobe;

import android.graphics.Bitmap;

public final class NavIconSelector {
    private NavIconSelector() {}

    public static Bitmap render(String arrow, String instruction, int sizePx) {
        Bitmap reference = ReferenceArrowRenderer.render(arrow, instruction, sizePx);
        if (reference != null) return reference;
        Bitmap atlas = AtlasVectorRenderer.render(arrow, instruction, sizePx);
        return atlas != null ? atlas : NavIconRenderer.render(arrow, instruction, sizePx);
    }
}
