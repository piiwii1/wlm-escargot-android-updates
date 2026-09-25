from pathlib import Path

# Version
p = Path('app/build.gradle')
s = p.read_text()
s = s.replace('versionCode 3', 'versionCode 4').replace("versionName '1.2.0'", "versionName '1.2.1'")
p.write_text(s)

# Main activity: remove forced edge-to-edge and add safe startup fallback.
p = Path('app/src/main/java/ch/piiwii/euroscan/MainActivity.java')
s = p.read_text()
s = s.replace('EUROSCAN CH  ·  v1.2.0', 'EUROSCAN CH  ·  v1.2.1')
old_on = '''    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        history = new HistoryStore(this);
        configureSystemBars();
        home();
    }'''
new_on = '''    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        configureSystemBars();
        try {
            history = new HistoryStore(this);
            home();
        } catch (Throwable startupError) {
            showSafeHome(startupError);
        }
    }'''
if old_on not in s:
    raise SystemExit('MainActivity onCreate block not found')
s = s.replace(old_on, new_on)
start = s.index('    private void configureSystemBars() {')
end = s.index('\n    private void page(', start)
replacement = '''    private void configureSystemBars() {
        getWindow().setStatusBarColor(DEEP);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private void showSafeHome(Throwable error) {
        LinearLayout safe = new LinearLayout(this);
        safe.setOrientation(LinearLayout.VERTICAL);
        safe.setPadding(dp(20), dp(20), dp(20), dp(24));
        safe.setBackgroundColor(BG);
        safe.addView(txt("EuroScan CH", 27, PURPLE, true));
        safe.addView(gap(8));
        safe.addView(txt("Mode de démarrage sécurisé", 16, TEXT, true));
        safe.addView(gap(8));
        safe.addView(txt("L'interface principale a rencontré un problème, mais l'application reste ouverte.", 14, MUTED, false));
        safe.addView(gap(18));
        Button scan = btn("Ouvrir le scanner", PURPLE, Color.WHITE);
        Button manual = secondary("Saisie manuelle");
        safe.addView(scan, new LinearLayout.LayoutParams(-1, dp(58)));
        safe.addView(gap(10));
        safe.addView(manual, new LinearLayout.LayoutParams(-1, dp(54)));
        safe.addView(gap(14));
        safe.addView(txt("Diagnostic : " + error.getClass().getSimpleName(), 11, Color.GRAY, false));
        setContentView(safe);
        scan.setOnClickListener(v -> startLiveScanner());
        manual.setOnClickListener(v -> editor(new TicketParser.ParsedTicket(), "Saisie manuelle"));
    }
'''
s = s[:start] + replacement + s[end:]
s = s.replace('''        setContentView(sc);
        applySafeInsets(root);''', '''        root.setPadding(dp(18), dp(18), dp(18), dp(30));
        setContentView(sc);''')
marker = '    private void applySafeInsets(View v) {'
if marker in s:
    a = s.index(marker)
    b = s.index('\n    private void home() {', a)
    s = s[:a] + '''    private void applySafeInsets(View v) {
        v.setPadding(dp(18), dp(18), dp(18), dp(30));
    }\n''' + s[b:]
p.write_text(s)

# Scanner activity: let Android reserve status/navigation bars normally.
p = Path('app/src/main/java/ch/piiwii/euroscan/ScannerActivity.java')
s = p.read_text()
s = s.replace('''        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        buildUi();''', '''        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        buildUi();''')
s = s.replace('''        setContentView(root);
        applySafeInsets(root, topLp, bottomLp);''', '''        setContentView(root);''')
marker = '    private void applySafeInsets(View root, FrameLayout.LayoutParams topLp, FrameLayout.LayoutParams bottomLp) {'
if marker in s:
    a = s.index(marker)
    b = s.index('\n    @Override\n    public void onRequestPermissionsResult', a)
    s = s[:a] + s[b:]
p.write_text(s)

Path('README.md').write_text('# EuroScan CH 1.2.1\n\nCorrectif de démarrage sûr. La signature finale est appliquée hors du dépôt public.\n')
print('EuroScan CH 1.2.1 hotfix applied')
