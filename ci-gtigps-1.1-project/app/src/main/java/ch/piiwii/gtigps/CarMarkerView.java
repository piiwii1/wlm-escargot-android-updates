package ch.piiwii.gtigps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class CarMarkerView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CarMarkerView(Context context) {
        super(context);
        fill.setColor(0xFFD71920);
        fill.setStyle(Paint.Style.FILL);
        stroke.setColor(0xFFF3F3F3);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        Path p = new Path();
        p.moveTo(w / 2f, 3f);
        p.lineTo(w - 5f, h - 5f);
        p.lineTo(w / 2f, h * 0.72f);
        p.lineTo(5f, h - 5f);
        p.close();
        canvas.drawPath(p, fill);
        canvas.drawPath(p, stroke);
        canvas.drawCircle(w / 2f, h * 0.56f, 3.5f, stroke);
    }
}
