package ch.piiwii.mapsnavprobe;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(5,9,14);
    private static final int TEXT = Color.rgb(248,251,255);
    private static final int MUTED = Color.rgb(135,154,172);
    private static final int ACCENT = Color.rgb(139,244,255);
    private static final int ACCENT_DARK = Color.rgb(14,135,226);

    private TextView accessStatus, distanceView, instruction, detail, meta, raw;
    private ImageView arrowView;
    private int testIndex = 0;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureSystemBars();

        ScrollView screen = new ScrollView(this);
        screen.setFillViewport(true);
        screen.setBackgroundColor(BG);
        screen.setClipToPadding(false);
        screen.setVerticalScrollBarEnabled(true);
        applySystemBarInsets(screen);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0,0,0,dp(8));
        screen.addView(content, new ScrollView.LayoutParams(-1,-2));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0,0,0,dp(12));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Maps Nav Probe",27,true,TEXT);
        TextView intro = text("Navigation Google Maps pour Launcher GTI",12,false,MUTED);
        intro.setPadding(0,dp(2),0,0);
        headerText.addView(title);
        headerText.addView(intro);
        header.addView(headerText,new LinearLayout.LayoutParams(0,-2,1f));

        TextView version = text("v1.10",11,true,ACCENT);
        version.setGravity(Gravity.CENTER);
        version.setBackground(rounded(Color.rgb(8,26,39),14,Color.rgb(42,96,124)));
        version.setPadding(dp(10),dp(6),dp(10),dp(6));
        header.addView(version);
        content.addView(header);

        accessStatus = text("",13,true,MUTED);
        accessStatus.setPadding(dp(13),dp(10),dp(13),dp(10));
        accessStatus.setBackground(rounded(Color.rgb(9,22,32),16,Color.rgb(35,64,83)));
        content.addView(accessStatus,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0,dp(9),0,dp(9));
        Button permission = button("Accès notifications");
        permission.setOnClickListener(v -> openNotificationAccess());
        Button maps = button("Ouvrir Maps");
        maps.setOnClickListener(v -> openMaps());
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,dp(46),1f);
        lp1.setMarginEnd(dp(5));
        actions.addView(permission,lp1);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,dp(46),1f);
        lp2.setMarginStart(dp(5));
        actions.addView(maps,lp2);
        content.addView(actions);

        TextView previewLabel = sectionLabel("APERÇU DU WIDGET");
        previewLabel.setPadding(dp(2),dp(2),0,dp(6));
        content.addView(previewLabel);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(12),dp(14),dp(13));
        card.setBackground(gradientCard());

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        meta = text("GOOGLE MAPS",9,true,Color.rgb(146,168,186));
        meta.setLetterSpacing(.08f);
        metaRow.addView(meta,new LinearLayout.LayoutParams(0,-2,1f));
        TextView live = text("LIVE",8,true,Color.rgb(165,247,255));
        live.setGravity(Gravity.CENTER);
        live.setPadding(dp(8),dp(3),dp(8),dp(3));
        live.setBackground(rounded(Color.rgb(12,28,39),10,Color.rgb(38,84,102)));
        metaRow.addView(live);
        card.addView(metaRow);

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER_VERTICAL);
        navRow.setPadding(0,dp(10),0,0);

        arrowView = new ImageView(this);
        arrowView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        arrowView.setPadding(dp(7),dp(7),dp(7),dp(7));
        arrowView.setBackground(iconPlate());
        arrowView.setImageBitmap(NavIconSelector.render("↑","Continuez tout droit",dp(180)));
        navRow.addView(arrowView,new LinearLayout.LayoutParams(dp(82),dp(82)));

        LinearLayout navText = new LinearLayout(this);
        navText.setOrientation(LinearLayout.VERTICAL);
        navText.setPadding(dp(13),0,0,0);
        distanceView = text("",28,true,ACCENT);
        instruction = text("En attente d’un trajet",18,true,TEXT);
        instruction.setMaxLines(2);
        detail = text("Ouvre Google Maps et démarre la navigation",11,false,MUTED);
        detail.setPadding(0,dp(5),0,0);
        navText.addView(distanceView);
        navText.addView(instruction);
        navText.addView(detail);
        navRow.addView(navText,new LinearLayout.LayoutParams(0,-2,1f));
        card.addView(navRow);
        content.addView(card);

        Button simulate = secondaryButton("Tester la manœuvre suivante");
        simulate.setOnClickListener(v -> simulateNext());
        LinearLayout.LayoutParams simLp = new LinearLayout.LayoutParams(-1,dp(46));
        simLp.setMargins(0,dp(9),0,0);
        content.addView(simulate,simLp);

        TextView diagLabel = sectionLabel("DIAGNOSTIC GOOGLE MAPS");
        diagLabel.setPadding(dp(2),dp(14),0,dp(6));
        content.addView(diagLabel);

        raw = text("Aucune donnée reçue.",12,false,Color.rgb(190,204,217));
        raw.setTextIsSelectable(true);
        raw.setPadding(dp(12),dp(11),dp(12),dp(11));
        raw.setMinHeight(dp(130));
        raw.setBackground(rounded(Color.rgb(8,15,22),15,Color.rgb(31,55,72)));
        content.addView(raw,new LinearLayout.LayoutParams(-1,-2));

        Button clear = secondaryButton("Effacer les données du test");
        clear.setOnClickListener(v -> {
            getSharedPreferences(MapsNotificationListener.PREFS,MODE_PRIVATE).edit().clear().apply();
            MapsNavWidget.updateAll(this);
            refresh();
        });
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(-1,dp(44));
        clearLp.setMargins(0,dp(9),0,dp(4));
        content.addView(clear,clearLp);

        setContentView(screen);
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT >= 28) getWindow().setNavigationBarDividerColor(BG);
    }

    private void applySystemBarInsets(final View root) {
        final int baseLeft=dp(18),baseTop=dp(14),baseRight=dp(18),baseBottom=dp(14);
        root.setOnApplyWindowInsetsListener((v,insets) -> {
            int l,t,r,b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                l=bars.left;t=bars.top;r=bars.right;b=bars.bottom;
            } else {
                l=insets.getSystemWindowInsetLeft();t=insets.getSystemWindowInsetTop();
                r=insets.getSystemWindowInsetRight();b=insets.getSystemWindowInsetBottom();
            }
            v.setPadding(baseLeft+l,baseTop+t,baseRight+r,baseBottom+b);
            return insets;
        });
        root.requestApplyInsets();
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter f=new IntentFilter(MapsNotificationListener.ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver,f,RECEIVER_NOT_EXPORTED); else registerReceiver(receiver,f);
        refresh();
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(receiver); } catch (Throwable ignored) {}
    }

    private void refresh() {
        boolean enabled=listenerEnabled();
        accessStatus.setText(enabled ? "● Prêt · Google Maps peut envoyer ses consignes" : "● Accès aux notifications requis");
        accessStatus.setTextColor(enabled ? ACCENT : Color.rgb(255,188,92));

        SharedPreferences p=getSharedPreferences(MapsNotificationListener.PREFS,MODE_PRIVATE);
        String arrow=p.getString("arrow","");
        String distance=p.getString("distance","");
        String primary=p.getString("primary","");
        String secondary=p.getString("secondary","");
        String rawText=p.getString("raw","");
        boolean simulated=p.getBoolean("simulated",false);

        if (TextUtils.isEmpty(primary)) {
            arrowView.setImageBitmap(NavIconSelector.render("↑","Continuez tout droit",dp(180)));
            distanceView.setText("");
            instruction.setText("En attente d’un trajet");
            detail.setText("Ouvre Google Maps et démarre la navigation");
            meta.setText("GOOGLE MAPS");
        } else {
            arrowView.setImageBitmap(NavIconSelector.render(arrow,primary,dp(180)));
            distanceView.setText(distance);
            instruction.setText(primary);
            detail.setText(TextUtils.isEmpty(secondary) ? "Google Maps" : secondary);
            meta.setText(simulated ? "MODE TEST" : "GOOGLE MAPS");
        }
        raw.setText(TextUtils.isEmpty(rawText) ? "Aucune donnée reçue de Google Maps." : rawText);
    }

    private boolean listenerEnabled() {
        String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        if (enabled == null) return false;
        ComponentName me=new ComponentName(this,MapsNotificationListener.class);
        return enabled.contains(me.flattenToString()) || enabled.contains(getPackageName());
    }

    private void openNotificationAccess() {
        try { startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); }
        catch (Throwable t) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void openMaps() {
        try {
            Intent i=getPackageManager().getLaunchIntentForPackage(MapsNotificationListener.MAPS_PACKAGE);
            if (i != null) startActivity(i);
            else startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://maps.google.com")));
        } catch (Throwable t) {
            Toast.makeText(this,"Google Maps introuvable",Toast.LENGTH_LONG).show();
        }
    }

    private void simulateNext() {
        String[][] tests={
                {"↑","600 m","Continuez tout droit","Route de Lausanne"},
                {"←","100 m","Tournez à gauche","Rue du Rhône"},
                {"→","200 m","Tournez à droite","Avenue de France"},
                {"↖","350 m","Légèrement à gauche","Route cantonale"},
                {"↗","450 m","Restez à droite","Direction Sion"},
                {"↶","80 m","Faites demi-tour","Route principale"},
                {"⟳","120 m","Prenez la 1re sortie","Rond-point"},
                {"⟳","120 m","Prenez la 2e sortie","Rond-point"},
                {"⟳","120 m","Prenez la 3e sortie","Rond-point"},
                {"⚑","","Vous êtes arrivé","Destination"}
        };
        String[] t=tests[testIndex % tests.length];
        testIndex++;
        getSharedPreferences(MapsNotificationListener.PREFS,MODE_PRIVATE).edit()
                .putString("arrow",t[0]).putString("distance",t[1]).putString("primary",t[2]).putString("secondary",t[3])
                .putString("timestamp",MapsNotificationListener.now())
                .putString("raw","SIMULATION v1.10.0\narrow="+t[0]+"\ndistance="+t[1]+"\ninstruction="+t[2]+"\nroad="+t[3])
                .putBoolean("simulated",true).apply();
        MapsNavWidget.updateAll(this);
        refresh();
    }

    private TextView sectionLabel(String value) {
        TextView t=text(value,10,true,Color.rgb(103,130,153));
        t.setLetterSpacing(.10f);
        return t;
    }

    private TextView text(String value,float size,boolean bold,int color) {
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b=new Button(this);
        b.setText(value);b.setAllCaps(false);b.setTextColor(Color.rgb(3,17,25));b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(133,244,255),ACCENT_DARK});
        g.setCornerRadius(dp(15));g.setStroke(dp(1),Color.rgb(167,247,255));b.setBackground(g);
        return b;
    }

    private Button secondaryButton(String value) {
        Button b=new Button(this);
        b.setText(value);b.setAllCaps(false);b.setTextColor(Color.rgb(206,232,248));b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(14,31,44),Color.rgb(8,18,27)});
        g.setCornerRadius(dp(15));g.setStroke(dp(1),Color.rgb(38,76,100));b.setBackground(g);
        return b;
    }

    private GradientDrawable iconPlate() {
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(15,35,49),Color.rgb(6,14,22)});
        g.setCornerRadius(dp(20));g.setStroke(dp(1),Color.rgb(31,61,78));
        return g;
    }

    private GradientDrawable gradientCard() {
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(12,21,30),Color.rgb(4,8,12)});
        g.setCornerRadius(dp(26));g.setStroke(dp(1),Color.rgb(30,55,68));
        return g;
    }

    private GradientDrawable rounded(int fill,int radiusDp,int stroke) {
        GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radiusDp));g.setStroke(dp(1),stroke);return g;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
