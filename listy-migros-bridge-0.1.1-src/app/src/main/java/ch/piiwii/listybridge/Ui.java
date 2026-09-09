package ch.piiwii.listybridge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.TextView;

public final class Ui {
    public static final int NAVY = Color.rgb(8,44,102);
    public static final int CYAN = Color.rgb(40,183,255);
    public static final int BG = Color.rgb(244,247,251);
    private Ui() {}
    public static int dp(Context c, int n){ return Math.round(n*c.getResources().getDisplayMetrics().density); }
    public static GradientDrawable rounded(int color, float radiusDp, Context c){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(c,(int)radiusDp)); return g;
    }
    public static Button button(Context c,String text,boolean primary){
        Button b=new Button(c); b.setText(text); b.setTextSize(16); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(primary?Color.WHITE:NAVY); b.setBackground(rounded(primary?NAVY:Color.WHITE,14,c));
        b.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12)); b.setElevation(dp(c,2)); return b;
    }
    public static TextView text(Context c,String value,float sp,int color,boolean bold){
        TextView t=new TextView(c); t.setText(value); t.setTextSize(sp); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
}
