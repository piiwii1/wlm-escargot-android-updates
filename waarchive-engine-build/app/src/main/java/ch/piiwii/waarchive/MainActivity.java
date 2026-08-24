package ch.piiwii.waarchive;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        return v;
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private void addMessage(LinearLayout list, String message, String time, boolean sent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(sent ? Gravity.END : Gravity.START);
        row.setPadding(dp(8), dp(2), dp(8), dp(2));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(11), dp(7), dp(9), dp(5));
        bubble.setBackground(rounded(sent ? Color.rgb(217,253,211) : Color.WHITE, 10));

        TextView body = text(message, 16, Color.rgb(17,27,33));
        body.setMaxWidth(dp(310));
        bubble.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView meta = text(time + (sent ? "  ✓✓" : ""), 11, Color.rgb(102,119,129));
        meta.setGravity(Gravity.END);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaLp.gravity = Gravity.END;
        metaLp.leftMargin = dp(24);
        bubble.addView(meta, metaLp);

        row.addView(bubble, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        list.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private TextView centerChip(String value) {
        TextView chip = text(value, 12, Color.rgb(84,101,111));
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        chip.setBackground(rounded(Color.rgb(231,240,244), 8));
        return chip;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(7,94,84));
        w.setNavigationBarColor(Color.rgb(11,20,26));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(239,234,226));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), 0, dp(8), 0);
        header.setBackgroundColor(Color.rgb(0,128,105));

        TextView back = text("‹", 38, Color.WHITE);
        back.setGravity(Gravity.CENTER);
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(60)));

        TextView avatar = text("A", 19, Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setBackground(rounded(Color.rgb(96,125,139), 30));
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        avatarLp.rightMargin = dp(10);
        header.addView(avatar, avatarLp);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Archive WhatsApp", 18, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = text("archive locale • lecture seule", 12, Color.rgb(213,238,238));
        names.addView(title);
        names.addView(subtitle);
        header.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView search = text("⌕", 28, Color.WHITE);
        search.setGravity(Gravity.CENTER);
        header.addView(search, new LinearLayout.LayoutParams(dp(44), dp(60)));
        TextView menu = text("⋮", 28, Color.WHITE);
        menu.setGravity(Gravity.CENTER);
        header.addView(menu, new LinearLayout.LayoutParams(dp(40), dp(60)));
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, dp(12));

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER);
        dateRow.addView(centerChip("25 AOÛT 2026"));
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = dp(4);
        dateLp.bottomMargin = dp(7);
        list.addView(dateRow, dateLp);

        TextView info = text("Moteur 0.3.2 • vraie Activity Android • aucun JNI", 12, Color.rgb(84,101,111));
        info.setGravity(Gravity.CENTER);
        info.setPadding(dp(10), dp(7), dp(10), dp(7));
        info.setBackground(rounded(Color.rgb(255,244,199), 8));
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams infoBoxLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoBoxLp.leftMargin = dp(18);
        infoBoxLp.rightMargin = dp(18);
        infoBoxLp.bottomMargin = dp(8);
        infoRow.addView(info, infoBoxLp);
        list.addView(infoRow);

        addMessage(list, "Salut, tu vois bien la conversation ?", "00:18", false);
        addMessage(list, "Oui. Cette version est compilée avec Gradle et le SDK Android officiels.", "00:18", true);
        addMessage(list, "Plus de DEX fabriqué à la main ?", "00:19", false);
        addMessage(list, "Non. Plus de bibliothèque JNI non plus.", "00:19", true);
        addMessage(list, "La base doit d'abord rester ouverte et être stable.", "00:20", false);
        addMessage(list, "Ensuite on branche le vrai lecteur WhatsApp, SQLite et les médias.", "00:20", true);
        addMessage(list, "Et le futur scellage gardera tout dans un seul APK.", "00:21", false);
        addMessage(list, "Exactement.", "00:21", true);

        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView footer = text("Archive en lecture seule — aucun envoi possible", 13, Color.rgb(102,119,129));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(16), dp(12), dp(16), dp(12));
        footer.setBackgroundColor(Color.rgb(240,242,245));
        root.addView(footer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }
}
