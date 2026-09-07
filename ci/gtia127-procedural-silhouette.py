from pathlib import Path
import sys
p=Path(sys.argv[1])
s=p.read_text()
old='        int skinRes = silhouette ? R.drawable.altimeter_skin_silhouette : R.drawable.altimeter_skin_reference;\n        Bitmap skin = BitmapFactory.decodeResource(context.getResources(), skinRes);\n        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);\n        if (skin != null) canvas.drawBitmap(skin, null, panel, bitmapPaint);\n        canvas.save();\n        canvas.translate(panel.left, panel.top);\n        canvas.scale(panel.width() / designW, panel.height() / designH);\n        if (silhouette) drawSilhouetteLiveData(canvas, context);\n        else drawLiveData(canvas, context);\n'
new='        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);\n        if (!silhouette) {\n            Bitmap skin = BitmapFactory.decodeResource(context.getResources(), R.drawable.altimeter_skin_reference);\n            if (skin != null) canvas.drawBitmap(skin, null, panel, bitmapPaint);\n        }\n        canvas.save();\n        canvas.translate(panel.left, panel.top);\n        canvas.scale(panel.width() / designW, panel.height() / designH);\n        if (silhouette) {\n            drawSilhouetteBase(canvas, context);\n            drawSilhouetteLiveData(canvas, context);\n        } else {\n            drawLiveData(canvas, context);\n        }\n'
if old not in s: raise SystemExit("render block not found")
s=s.replace(old,new)
marker="    private static void drawSilhouetteLiveData(Canvas c, Context context) {"
block='''    /**
     * Skin 2: sticker / silhouette style. No panel, no outer rectangle and no opaque black fill.
     * Everything not explicitly painted here remains fully transparent so the launcher wallpaper
     * shows through. The compass is derived from the premium artwork, but all dark/grey fill,
     * the red sun and the Swiss flag are removed.
     */
    private static void drawSilhouetteBase(Canvas c, Context context) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

        Bitmap compass = buildSilhouetteCompass(context);
        if (compass != null) {
            RectF dst = new RectF(38f, 132f, 688f, 782f);
            c.drawBitmap(compass, null, dst, p);
        }

        // Vertical separator: thin red/chrome stroke, not a filled panel edge.
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(Color.rgb(26, 26, 29));
        line.setStrokeWidth(13f);
        c.drawLine(688f, 154f, 688f, 720f, line);
        line.setColor(RED);
        line.setStrokeWidth(5.5f);
        line.setShadowLayer(5f, 0f, 0f, Color.argb(95, 255, 18, 26));
        c.drawLine(688f, 160f, 688f, 714f, line);
        line.clearShadowLayer();
        line.setColor(Color.rgb(236, 236, 240));
        line.setStrokeWidth(1.2f);
        c.drawLine(686.5f, 163f, 686.5f, 711f, line);

        // ALTITUDE title: same white/chrome visual family as Skin 1, with restrained red.
        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setTextSize(82f);
        title.setLetterSpacing(0.13f);
        title.setShader(new LinearGradient(0f, 205f, 0f, 292f,
                new int[]{Color.WHITE, Color.rgb(249,249,251), Color.rgb(181,184,190)},
                new float[]{0f,.62f,1f}, Shader.TileMode.CLAMP));
        title.setShadowLayer(7f, 0f, 4f, Color.argb(205, 0, 0, 0));
        c.drawText("ALTITUDE", 748f, 294f, title);
        title.clearShadowLayer();
        title.setShader(null);

        // Under-title and lower separators. No rectangular background.
        line.setColor(Color.rgb(28, 28, 31));
        line.setStrokeWidth(12f);
        c.drawLine(720f, 317f, 1378f, 317f, line);
        line.setColor(RED);
        line.setStrokeWidth(4.5f);
        line.setShadowLayer(4f, 0f, 0f, Color.argb(90, 255, 18, 26));
        c.drawLine(720f, 317f, 1378f, 317f, line);
        c.drawLine(718f, 604f, 1640f, 604f, line);
        line.clearShadowLayer();
        line.setColor(Color.rgb(232, 232, 236));
        line.setStrokeWidth(1.1f);
        c.drawLine(720f, 314.5f, 1378f, 314.5f, line);
        c.drawLine(720f, 601.5f, 1640f, 601.5f, line);

        drawSilhouettePin(c, 760f, 663f);

        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        label.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        label.setColor(WHITE);
        label.setTextSize(49f);
        label.setShadowLayer(6f, 0f, 3f, Color.argb(210, 0, 0, 0));
        c.drawText("GPS", 805f, 682f, label);

        // Vertical separator between signal and precision.
        line.setColor(Color.rgb(20,20,23));
        line.setStrokeWidth(12f);
        c.drawLine(1084f, 626f, 1084f, 707f, line);
        line.setColor(Color.rgb(235,235,238));
        line.setStrokeWidth(2.2f);
        c.drawLine(1084f, 629f, 1084f, 704f, line);

        drawCrosshair(c, 1172f, 664f, 40f);
        label.setTextSize(45f);
        c.drawText("PRÉCISION", 1224f, 682f, label);
        label.clearShadowLayer();
    }

    private static Bitmap buildSilhouetteCompass(Context context) {
        Bitmap src = BitmapFactory.decodeResource(context.getResources(), R.drawable.altimeter_skin_reference);
        if (src == null) return null;
        // Crop only the round compass from the premium artwork.
        int x0 = Math.max(0, Math.round(src.getWidth() * 0.020f));
        int y0 = Math.max(0, Math.round(src.getHeight() * 0.015f));
        int x1 = Math.min(src.getWidth(), Math.round(src.getWidth() * 0.392f));
        int y1 = Math.min(src.getHeight(), Math.round(src.getHeight() * 0.985f));
        int w = Math.max(1, x1 - x0), h = Math.max(1, y1 - y0);
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            src.getPixels(row, 0, w, x0, y0 + y, w, 1);
            for (int x = 0; x < w; x++) {
                int color = row[x];
                int a = Color.alpha(color), r = Color.red(color), g = Color.green(color), b = Color.blue(color);
                float fx = (x0 + x) / (float) src.getWidth();
                float fy = (y0 + y) / (float) src.getHeight();

                // Explicitly erase the Swiss flag area.
                if (fx > 0.095f && fx < 0.145f && fy > 0.315f && fy < 0.470f) {
                    row[x] = Color.TRANSPARENT;
                    continue;
                }
                // Erase the red sun/halo behind the mountain while preserving white mountain strokes.
                float dx = fx - 0.225f, dy = fy - 0.355f;
                boolean sunZone = dx*dx + dy*dy < 0.0043f;
                int lum = (r * 3 + g * 6 + b) / 10;
                boolean redAccent = r > 125 && r > g * 1.38f && r > b * 1.28f;
                boolean bright = lum > 122;
                if (a < 24 || (!bright && !redAccent) || (sunZone && redAccent && lum < 205)) {
                    row[x] = Color.TRANSPARENT;
                } else {
                    // Keep white/chrome line work and red compass accents; dark pixels become holes.
                    int na = Math.min(255, Math.max(80, (a * (bright ? 245 : 225)) / 255));
                    row[x] = Color.argb(na, r, g, b);
                }
            }
            out.setPixels(row, 0, w, 0, y, w, 1);
        }
        return out;
    }

    private static void drawSilhouettePin(Canvas c, float cx, float cy) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(RED);
        p.setShadowLayer(6f, 0f, 0f, Color.argb(105,255,20,28));
        Path path = new Path();
        path.moveTo(cx, cy + 31f);
        path.cubicTo(cx - 8f, cy + 14f, cx - 24f, cy - 4f, cx - 24f, cy - 20f);
        path.cubicTo(cx - 24f, cy - 35f, cx - 13f, cy - 46f, cx, cy - 46f);
        path.cubicTo(cx + 13f, cy - 46f, cx + 24f, cy - 35f, cx + 24f, cy - 20f);
        path.cubicTo(cx + 24f, cy - 4f, cx + 8f, cy + 14f, cx, cy + 31f);
        path.close();
        c.drawPath(path, p);
        p.clearShadowLayer();
        p.setColor(Color.rgb(18,18,21));
        c.drawCircle(cx, cy - 20f, 8f, p);
    }

    private static void drawCrosshair(Canvas c, float cx, float cy, float radius) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(WHITE);
        p.setStrokeWidth(4.5f);
        p.setShadowLayer(6f, 0f, 3f, Color.argb(190,0,0,0));
        c.drawCircle(cx, cy, radius * .72f, p);
        c.drawLine(cx-radius,cy,cx-radius*.45f,cy,p);
        c.drawLine(cx+radius*.45f,cy,cx+radius,cy,p);
        c.drawLine(cx,cy-radius,cx,cy-radius*.45f,p);
        c.drawLine(cx,cy+radius*.45f,cx,cy+radius,p);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx,cy,6f,p);
        p.clearShadowLayer();
    }

'''
if marker not in s: raise SystemExit("silhouette marker not found")
s=s.replace(marker,block+marker)
p.write_text(s)
