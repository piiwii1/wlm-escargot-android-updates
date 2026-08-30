package ch.piiwii.modeconvoi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class QrCodeView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean[][] modules;
    public QrCodeView(Context context) { super(context); setBackgroundColor(Color.WHITE); }
    public void setPayload(String payload) { modules=QrCode.encodeText(payload); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(modules==null)return;
        int quiet=4, cells=QrCode.SIZE+quiet*2;
        float side=Math.min(getWidth(),getHeight());
        float cell=side/cells;
        float ox=(getWidth()-side)/2f, oy=(getHeight()-side)/2f;
        paint.setColor(Color.WHITE); canvas.drawRect(0,0,getWidth(),getHeight(),paint);
        paint.setColor(Color.BLACK);
        for(int y=0;y<QrCode.SIZE;y++)for(int x=0;x<QrCode.SIZE;x++)if(modules[y][x]){
            float l=ox+(x+quiet)*cell,t=oy+(y+quiet)*cell;
            canvas.drawRect(l,t,l+cell+0.25f,t+cell+0.25f,paint);
        }
    }
}
