package ch.piiwii.euroscan;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int GALLERY = 1002;
    private static final int SCANNER = 1003;

    private static final int PURPLE = Color.rgb(48, 27, 92);
    private static final int DEEP = Color.rgb(30, 17, 63);
    private static final int ORANGE = Color.rgb(246, 165, 0);
    private static final int BG = Color.rgb(246, 245, 249);
    private static final int GREEN = Color.rgb(22, 142, 90);
    private static final int RED = Color.rgb(190, 45, 45);
    private static final int TEXT = Color.rgb(43, 40, 50);
    private static final int MUTED = Color.rgb(108, 103, 118);

    private LinearLayout root, gridsBox;
    private EditText dateEdit;
    private final List<GridRow> rows = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ResultsRepository repo = new ResultsRepository();
    private HistoryStore history;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        history = new HistoryStore(this);
        configureSystemBars();
        home();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(DEEP);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            if (getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().setSystemBarsAppearance(
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS |
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private void page(String title, String sub) {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setClipToPadding(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        sc.addView(root, new ScrollView.LayoutParams(-1, -2));
        setContentView(sc);
        applySafeInsets(root);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        hero.setBackground(gradient(new int[]{DEEP, PURPLE}, 24));
        hero.setElevation(dp(3));

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView chip = txt("EUROSCAN CH  ·  v1.2.0", 11, Color.rgb(235, 229, 248), true);
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        chip.setBackground(round(Color.argb(55, 255, 255, 255), 20));
        chipRow.addView(chip);
        hero.addView(chipRow);
        hero.addView(gap(12));
        hero.addView(txt(title, 27, Color.WHITE, true));
        hero.addView(gap(5));
        hero.addView(txt(sub, 14, Color.rgb(224, 218, 240), false));
        root.addView(hero, lp(-1, -2, 0, 0, 0, 18));
    }

    private void applySafeInsets(View v) {
        v.setPadding(dp(18), dp(18), dp(18), dp(30));
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            int top = 0, bottom = 0, left = 0, right = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars());
                top = i.top; bottom = i.bottom; left = i.left; right = i.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(dp(18) + left, dp(18) + top, dp(18) + right, dp(30) + bottom);
            return insets;
        });
        v.requestApplyInsets();
    }

    private void home() {
        page("Contrôle ton ticket", "Un scan automatique, puis une vérification claire grille par grille.");

        LinearLayout scanCard = box(Color.WHITE, 22, 16);
        scanCard.setElevation(dp(2));
        LinearLayout scanTitle = new LinearLayout(this);
        scanTitle.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot = txt("●", 18, GREEN, true);
        TextView auto = txt(" Scanner automatique", 18, TEXT, true);
        scanTitle.addView(dot);
        scanTitle.addView(auto);
        scanCard.addView(scanTitle);
        scanCard.addView(gap(6));
        scanCard.addView(txt("Présente simplement le reçu devant la caméra. Dès que le texte et les grilles sont lisibles et stables, l'app valide toute seule.", 13, MUTED, false));
        scanCard.addView(gap(14));
        Button scan = btn("▣  Ouvrir le scanner", PURPLE, Color.WHITE);
        scanCard.addView(scan, new LinearLayout.LayoutParams(-1, dp(62)));
        root.addView(scanCard, lp(-1, -2, 0, 0, 0, 14));
        scan.setOnClickListener(v -> startLiveScanner());

        LinearLayout todayCard = box(Color.rgb(255, 248, 230), 20, 16);
        todayCard.setElevation(dp(1));
        todayCard.addView(txt("Ticket joué aujourd'hui ?", 17, TEXT, true));
        todayCard.addView(gap(4));
        todayCard.addView(txt("Le bouton test vérifie le tirage du jour et la connexion aux résultats sans inventer de gain si le tirage n'est pas encore publié.", 13, MUTED, false));
        todayCard.addView(gap(11));
        Button test = btn("🧪  Test ticket d'aujourd'hui", ORANGE, Color.BLACK);
        todayCard.addView(test, new LinearLayout.LayoutParams(-1, dp(54)));
        root.addView(todayCard, lp(-1, -2, 0, 0, 0, 14));
        test.setOnClickListener(v -> testToday(test));

        LinearLayout latestCard = card();
        TextView latestText = txt("Appuie pour vérifier le dernier tirage disponible.", 14, MUTED, false);
        latestCard.addView(txt("Dernier tirage", 17, TEXT, true));
        latestCard.addView(gap(6));
        latestCard.addView(latestText);
        latestCard.addView(gap(10));
        Button refresh = secondary("Actualiser");
        latestCard.addView(refresh, new LinearLayout.LayoutParams(-1, dp(48)));
        root.addView(latestCard, lp(-1, -2, 0, 0, 0, 14));
        refresh.setOnClickListener(v -> latest(latestText, refresh));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button gallery = secondary("🖼  Photo");
        Button manual = secondary("✍  Manuel");
        row.addView(gallery, new LinearLayout.LayoutParams(0, dp(54), 1));
        row.addView(gapH(10));
        row.addView(manual, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(row, lp(-1, -2, 0, 0, 0, 10));

        Button hist = btn("🕘  Historique des contrôles", Color.rgb(231, 228, 239), PURPLE);
        root.addView(hist, lp(-1, dp(54), 0, 0, 0, 18));

        gallery.setOnClickListener(v -> gallery());
        manual.setOnClickListener(v -> editor(new TicketParser.ParsedTicket(), "Saisie manuelle"));
        hist.setOnClickListener(v -> history());

        root.addView(txt("EuroScan CH est indépendante de la Loterie Romande, Swisslos et EuroMillions. Le reçu original et la validation officielle de l'opérateur font foi.", 11, Color.GRAY, false));
    }

    private void startLiveScanner() {
        try {
            startActivityForResult(new Intent(this, ScannerActivity.class), SCANNER);
        } catch (Exception e) {
            toast("Impossible d'ouvrir le scanner automatique.");
        }
    }

    private void testToday(Button b) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String pretty = new SimpleDateFormat("dd.MM.yyyy", Locale.FRANCE).format(new Date());
        b.setEnabled(false);
        b.setText("Test en cours…");

        worker.execute(() -> {
            try {
                DrawResult d = repo.fetch(today);
                runOnUiThread(() -> {
                    b.setEnabled(true); b.setText("🧪  Test ticket d'aujourd'hui");
                    String extra = d.swissWinNumbers.isEmpty() ? "" : "\nSWISS WIN : " + join(d.swissWinNumbers);
                    new AlertDialog.Builder(this)
                            .setTitle("Tirage du " + pretty + " disponible ✓")
                            .setMessage("Connexion aux résultats : OK\n\nEuroMillions : " + drawLine(d) + extra +
                                    "\n\nTu peux maintenant scanner ton vrai ticket pour vérifier les grilles.")
                            .setNegativeButton("Fermer", null)
                            .setPositiveButton("Scanner mon ticket", (dialog, which) -> startLiveScanner())
                            .show();
                });
            } catch (Exception noToday) {
                try {
                    DrawResult last = repo.latest();
                    runOnUiThread(() -> {
                        b.setEnabled(true); b.setText("🧪  Test ticket d'aujourd'hui");
                        new AlertDialog.Builder(this)
                                .setTitle("Le tirage du jour n'est pas encore disponible")
                                .setMessage("Connexion aux résultats : OK\n\nLe tirage du " + pretty + " n'est pas encore publié dans la source de résultats.\n\nDernier tirage disponible : " + last.date + "\n" + drawLine(last) +
                                        "\n\nTu peux quand même tester dès maintenant la lecture automatique de ton ticket.")
                                .setNegativeButton("Fermer", null)
                                .setPositiveButton("Tester le scanner", (dialog, which) -> startLiveScanner())
                                .show();
                    });
                } catch (Exception network) {
                    runOnUiThread(() -> {
                        b.setEnabled(true); b.setText("🧪  Test ticket d'aujourd'hui");
                        new AlertDialog.Builder(this)
                                .setTitle("Test impossible")
                                .setMessage("La source de résultats ne répond pas pour l'instant. Le scanner du ticket reste utilisable, mais le contrôle du gain demande une connexion aux résultats.")
                                .setNegativeButton("Fermer", null)
                                .setPositiveButton("Tester le scanner", (dialog, which) -> startLiveScanner())
                                .show();
                    });
                }
            }
        });
    }

    private void latest(TextView out, Button b) {
        b.setEnabled(false); out.setText("Chargement…");
        worker.execute(() -> {
            try {
                DrawResult d = repo.latest();
                runOnUiThread(() -> {
                    String extra = d.swissWinNumbers.isEmpty() ? "" : "\nSWISS WIN : " + join(d.swissWinNumbers);
                    out.setText(d.date + "\n" + drawLine(d) + extra +
                            (d.exactSwissPrizes ? "\nCotes CHF détectées." : "\nRésultat chargé · certaines cotes peuvent encore manquer."));
                    b.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> { out.setText("Impossible de charger le tirage. Vérifie la connexion Internet."); b.setEnabled(true); });
            }
        });
    }

    private void gallery() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, GALLERY);
        } catch (Exception e) { toast("Impossible d'ouvrir les photos."); }
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == GALLERY && result == RESULT_OK && data != null && data.getData() != null) ocr(data.getData());
        if (req == SCANNER && result == RESULT_OK && data != null) {
            String raw = data.getStringExtra("ocr_text");
            TicketParser.ParsedTicket parsed = TicketParser.parse(raw);
            editor(parsed, "Scanner automatique");
        }
    }

    private void ocr(Uri uri) {
        page("Lecture de la photo", "Analyse de la date, des numéros et des étoiles…");
        ProgressBar progress = new ProgressBar(this);
        root.addView(progress, lp(-1, dp(68), 0, 16, 0, 8));
        root.addView(txt("Analyse en cours…", 15, MUTED, false));
        try {
            InputImage im = InputImage.fromFilePath(this, uri);
            TextRecognizer r = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            r.process(im)
                    .addOnSuccessListener(t -> { r.close(); editor(TicketParser.parse(t.getText()), "Photo analysée"); })
                    .addOnFailureListener(e -> { r.close(); editor(new TicketParser.ParsedTicket(), "Lecture à corriger manuellement"); toast("Lecture automatique insuffisante."); });
        } catch (Exception e) { editor(new TicketParser.ParsedTicket(), "Lecture à corriger manuellement"); }
    }

    private void editor(TicketParser.ParsedTicket p, String origin) {
        page("Vérifier le ticket", origin + " · une vérification rapide évite toute erreur OCR.");
        rows.clear();

        LinearLayout info = box(Color.rgb(239, 245, 255), 16, 13);
        info.addView(txt("Avant le contrôle", 14, PURPLE, true));
        info.addView(txt("Vérifie la date et les chiffres détectés. Le scanner ne calcule rien tant que cette étape n'est pas validée.", 12, MUTED, false));
        root.addView(info, lp(-1, -2, 0, 0, 0, 14));

        root.addView(txt("Date du tirage", 13, MUTED, true));
        dateEdit = new EditText(this);
        dateEdit.setText(p.date == null ? lastDraw() : p.date);
        dateEdit.setHint("AAAA-MM-JJ");
        dateEdit.setTextSize(18);
        dateEdit.setTextColor(TEXT);
        dateEdit.setSingleLine();
        dateEdit.setPadding(dp(14), dp(10), dp(14), dp(10));
        dateEdit.setBackground(stroke(Color.rgb(220, 216, 230), Color.WHITE, 12));
        root.addView(dateEdit, lp(-1, dp(52), 0, 5, 0, 18));

        LinearLayout title = new LinearLayout(this);
        title.setGravity(Gravity.CENTER_VERTICAL);
        TextView gt = txt("Grilles détectées", 19, TEXT, true);
        Button add = small("+ Ajouter");
        title.addView(gt, new LinearLayout.LayoutParams(0, -2, 1));
        title.addView(add);
        root.addView(title, lp(-1, -2, 0, 0, 0, 10));

        gridsBox = new LinearLayout(this);
        gridsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(gridsBox);
        if (p.grids.isEmpty()) addGrid(null); else for (TicketGrid g : p.grids) addGrid(g);
        add.setOnClickListener(v -> addGrid(null));

        Button check = btn("✓  Contrôler ce ticket", ORANGE, Color.BLACK);
        Button back = secondary("Retour à l'accueil");
        root.addView(check, lp(-1, dp(60), 0, 18, 0, 10));
        root.addView(back, lp(-1, dp(50), 0, 0, 0, 0));
        back.setOnClickListener(v -> home());
        check.setOnClickListener(v -> check(check));
    }

    private void addGrid(TicketGrid g) {
        GridRow gr = new GridRow();
        LinearLayout c = card();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = txt("Grille " + (rows.size() + 1), 16, TEXT, true);
        Button del = small("Supprimer");
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(del);
        c.addView(top);
        c.addView(gap(9));
        c.addView(txt("5 numéros", 12, MUTED, false));

        LinearLayout ns = new LinearLayout(this);
        for (int i = 0; i < 5; i++) {
            EditText e = numBox(); gr.n.add(e);
            ns.addView(e, new LinearLayout.LayoutParams(0, dp(48), 1));
            if (i < 4) ns.addView(gapH(5));
        }
        c.addView(ns);
        c.addView(gap(9));
        c.addView(txt("2 étoiles", 12, MUTED, false));

        LinearLayout ss = new LinearLayout(this);
        for (int i = 0; i < 2; i++) {
            EditText e = numBox(); gr.s.add(e);
            ss.addView(e, new LinearLayout.LayoutParams(0, dp(48), 1));
            if (i == 0) ss.addView(gapH(6));
        }
        c.addView(ss);

        if (g != null) {
            for (int i = 0; i < 5; i++) gr.n.get(i).setText(String.valueOf(g.numbers.get(i)));
            for (int i = 0; i < 2; i++) gr.s.get(i).setText(String.valueOf(g.stars.get(i)));
        }
        rows.add(gr);
        gridsBox.addView(c, lp(-1, -2, 0, 0, 0, 10));
        del.setOnClickListener(v -> {
            if (rows.size() == 1) { toast("Il faut au moins une grille."); return; }
            int x = rows.indexOf(gr); rows.remove(gr); gridsBox.removeViewAt(x); renumber();
        });
    }

    private void renumber() {
        for (int i = 0; i < gridsBox.getChildCount(); i++) {
            View v = gridsBox.getChildAt(i);
            if (v instanceof LinearLayout) {
                LinearLayout c = (LinearLayout) v;
                if (c.getChildCount() > 0 && c.getChildAt(0) instanceof LinearLayout) {
                    LinearLayout t = (LinearLayout) c.getChildAt(0);
                    if (t.getChildCount() > 0 && t.getChildAt(0) instanceof TextView)
                        ((TextView) t.getChildAt(0)).setText("Grille " + (i + 1));
                }
            }
        }
    }

    private void check(Button b) {
        hideKeyboard();
        String date = TicketParser.normalizeDate(dateEdit.getText().toString().trim());
        if (!date.matches("20\\d{2}-\\d{2}-\\d{2}")) { toast("Date invalide. Format : AAAA-MM-JJ"); return; }
        List<TicketGrid> gs = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            TicketGrid g = rows.get(i).grid();
            if (g == null) { toast("Vérifie la grille " + (i + 1) + "."); return; }
            gs.add(g);
        }
        b.setEnabled(false); b.setText("Contrôle en cours…");
        worker.execute(() -> {
            try {
                DrawResult d = repo.fetch(date);
                runOnUiThread(() -> result(d, gs));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    b.setEnabled(true); b.setText("✓  Contrôler ce ticket");
                    new AlertDialog.Builder(this)
                            .setTitle("Résultat indisponible")
                            .setMessage("Le tirage demandé n'est pas encore publié ou la connexion aux résultats est indisponible. Aucun gain n'a été calculé.")
                            .setPositiveButton("OK", null).show();
                });
            }
        });
    }

    private void result(DrawResult d, List<TicketGrid> gs) {
        page("Résultat du ticket", "Tirage " + d.date + " · comparaison terminée.");
        DecimalFormat f = new DecimalFormat("#,##0.00");
        double total = 0;
        boolean allExact = true;
        StringBuilder hist = new StringBuilder();

        LinearLayout draw = box(Color.rgb(240, 237, 247), 18, 15);
        draw.addView(txt("Numéros gagnants", 14, MUTED, true));
        draw.addView(txt(drawLine(d), 22, PURPLE, true));
        if (!d.swissWinNumbers.isEmpty()) draw.addView(txt("SWISS WIN  ·  " + join(d.swissWinNumbers), 15, PURPLE, true));
        root.addView(draw, lp(-1, -2, 0, 0, 0, 14));

        for (int i = 0; i < gs.size(); i++) {
            TicketGrid g = gs.get(i);
            int n = d.matchingNumbers(g), s = d.matchingStars(g), swm = d.matchingSwissWin(g);
            Double euro = d.euroPrizeFor(n, s), swPrize = d.swissWinPrizeFor(swm);
            boolean euroWinning = isEuroWinningTier(n, s);
            boolean swWinning = swm >= 3;

            LinearLayout c = card();
            c.addView(txt("Grille " + (i + 1), 17, TEXT, true));
            c.addView(txt(g.compact(), 15, PURPLE, true));
            c.addView(gap(8));
            c.addView(txt("EuroMillions : " + n + " numéro" + (n > 1 ? "s" : "") + " + " + s + " étoile" + (s > 1 ? "s" : ""), 14, MUTED, false));

            if (euro != null) {
                total += euro;
                c.addView(txt("CHF " + f.format(euro), 20, GREEN, true));
            } else if (euroWinning) {
                allExact = false;
                c.addView(txt("Rang gagnant détecté · montant exact à valider", 16, ORANGE, true));
            } else c.addView(txt("Aucun gain EuroMillions", 14, Color.GRAY, true));

            c.addView(gap(7));
            if (d.swissWinNumbers.isEmpty()) {
                allExact = false;
                c.addView(txt("SWISS WIN : résultat non récupéré", 13, ORANGE, false));
            } else if (swWinning) {
                if (swPrize != null) {
                    total += swPrize;
                    c.addView(txt("SWISS WIN : " + swm + " bons numéros · CHF " + f.format(swPrize), 16, GREEN, true));
                } else {
                    allExact = false;
                    c.addView(txt("SWISS WIN : gain détecté · montant exact à valider", 14, ORANGE, true));
                }
            } else c.addView(txt("SWISS WIN : " + swm + " bon" + (swm > 1 ? "s" : "") + " numéro" + (swm > 1 ? "s" : "") + " · aucun gain", 13, Color.GRAY, false));

            hist.append("G").append(i + 1).append(": EML ").append(n).append("+").append(s).append(", SW ").append(swm).append("; ");
            root.addView(c, lp(-1, -2, 0, 0, 0, 10));
        }

        LinearLayout totalCard = box(allExact ? Color.rgb(231, 248, 239) : Color.rgb(255, 247, 228), 20, 17);
        totalCard.addView(txt(allExact ? "Gain total confirmé" : "Total confirmé disponible", 14, TEXT, true));
        totalCard.addView(gap(5));
        totalCard.addView(txt("CHF " + f.format(total), 34, allExact ? GREEN : ORANGE, true));
        totalCard.addView(txt(allExact ? "Le calcul utilise les cotes récupérées pour ce tirage." : "Les éventuels rangs gagnants sans cote exacte ne sont pas ajoutés au total. Validation LoRo indispensable.", 12, MUTED, false));
        root.addView(totalCard, lp(-1, -2, 0, 8, 0, 14));

        Button again = btn("▣  Scanner un autre ticket", PURPLE, Color.WHITE);
        Button h = secondary("Accueil");
        root.addView(again, lp(-1, dp(56), 0, 0, 0, 10));
        root.addView(h, lp(-1, dp(50), 0, 0, 0, 0));
        again.setOnClickListener(v -> startLiveScanner());
        h.setOnClickListener(v -> home());

        StringBuilder all = new StringBuilder();
        for (TicketGrid g : gs) all.append(g.compact()).append(" | ");
        history.add(d.date, all.toString(), hist.toString(), total, allExact);
    }

    private boolean isEuroWinningTier(int n, int s) {
        return n >= 2 || (n == 1 && s == 2);
    }

    private void history() {
        page("Historique", "Les contrôles sont enregistrés uniquement sur ce téléphone.");
        JSONArray a = history.all();
        if (a.length() == 0) root.addView(txt("Aucun ticket contrôlé pour l'instant.", 15, MUTED, false));
        for (int i = 0; i < a.length(); i++) try {
            JSONObject o = a.getJSONObject(i);
            LinearLayout c = card();
            c.addView(txt(o.optString("date"), 17, TEXT, true));
            c.addView(txt(o.optString("grids"), 12, MUTED, false));
            c.addView(gap(5));
            c.addView(txt(o.optString("result"), 12, MUTED, false));
            boolean ex = o.optBoolean("exact", false);
            c.addView(txt("Total confirmé : CHF " + new DecimalFormat("#,##0.00").format(o.optDouble("total")) + (ex ? "" : " · contrôle partiel"), 15, ex ? GREEN : ORANGE, true));
            root.addView(c, lp(-1, -2, 0, 0, 0, 10));
        } catch (Exception ignored) {}

        Button clear = secondary("Effacer l'historique");
        clear.setTextColor(RED);
        Button h = btn("Accueil", PURPLE, Color.WHITE);
        root.addView(clear, lp(-1, dp(50), 0, 12, 0, 10));
        root.addView(h, lp(-1, dp(50), 0, 0, 0, 0));
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Effacer l'historique ?").setNegativeButton("Annuler", null).setPositiveButton("Effacer", (d, w) -> { history.clear(); history(); }).show());
        h.setOnClickListener(v -> home());
    }

    private String drawLine(DrawResult d) { return join(d.numbers) + "  ★ " + join(d.stars); }
    private String join(List<Integer> l) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < l.size(); i++) { if (i > 0) b.append(' '); b.append(String.format(Locale.US, "%02d", l.get(i))); }
        return b.toString();
    }

    private String lastDraw() {
        Calendar c = Calendar.getInstance();
        int d = c.get(Calendar.DAY_OF_WEEK);
        int back = d == Calendar.FRIDAY || d == Calendar.TUESDAY ? 0 :
                d == Calendar.SATURDAY ? 1 : d == Calendar.SUNDAY ? 2 : d == Calendar.MONDAY ? 3 :
                        d == Calendar.WEDNESDAY ? 1 : d == Calendar.THURSDAY ? 2 : 0;
        c.add(Calendar.DAY_OF_MONTH, -back);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private static class GridRow {
        final List<EditText> n = new ArrayList<>(), s = new ArrayList<>();
        TicketGrid grid() {
            try {
                List<Integer> a = new ArrayList<>(), b = new ArrayList<>();
                for (EditText e : n) { int v = Integer.parseInt(e.getText().toString()); if (v < 1 || v > 50 || a.contains(v)) return null; a.add(v); }
                for (EditText e : s) { int v = Integer.parseInt(e.getText().toString()); if (v < 1 || v > 12 || b.contains(v)) return null; b.add(v); }
                return new TicketGrid(a, b);
            } catch (Exception e) { return null; }
        }
    }

    private EditText numBox() {
        EditText e = new EditText(this);
        e.setGravity(Gravity.CENTER);
        e.setTextSize(18);
        e.setTextColor(TEXT);
        e.setSingleLine();
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setBackground(stroke(Color.rgb(220, 216, 230), Color.WHITE, 10));
        return e;
    }

    private LinearLayout card() {
        LinearLayout c = box(Color.WHITE, 18, 16);
        c.setElevation(dp(1));
        return c;
    }

    private LinearLayout box(int color, int radius, int pad) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(pad), dp(pad), dp(pad), dp(pad));
        l.setBackground(round(color, radius));
        return l;
    }

    private TextView txt(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    private Button btn(String s, int bg, int fg) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(16); b.setTextColor(fg); b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD); b.setBackground(round(bg, 16));
        return b;
    }

    private Button secondary(String s) {
        Button b = btn(s, Color.WHITE, PURPLE);
        b.setBackground(stroke(Color.rgb(211, 206, 222), Color.WHITE, 14));
        return b;
    }

    private Button small(String s) {
        Button b = btn(s, Color.rgb(235, 232, 243), PURPLE);
        b.setTextSize(12);
        return b;
    }

    private View gap(int h) { Space s = new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); return s; }
    private View gapH(int w) { Space s = new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(dp(w), 1)); return s; }
    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p;
    }

    private GradientDrawable round(int color, int r) {
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }

    private GradientDrawable gradient(int[] colors, int r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors); g.setCornerRadius(dp(r)); return g;
    }

    private GradientDrawable stroke(int line, int fill, int r) {
        GradientDrawable g = round(fill, r); g.setStroke(dp(1), line); return g;
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
