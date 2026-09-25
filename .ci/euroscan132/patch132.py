from pathlib import Path

# Version bump directly from 1.3.0 to 1.3.2.
gradle = Path('euroscan-ch/app/build.gradle')
s = gradle.read_text()
s = s.replace('versionCode 5', 'versionCode 7')
s = s.replace("versionName '1.3.0'", "versionName '1.3.2'")
gradle.write_text(s)

main = Path('euroscan-ch/app/src/main/java/ch/piiwii/euroscan/MainActivity.java')
s = main.read_text()

# 1.3.1 startup safety: system bars inside the startup safety net.
old = '''        super.onCreate(b);\n        configureSystemBars();\n        try {\n            history = new HistoryStore(this);'''
new = '''        super.onCreate(b);\n        try {\n            configureSystemBars();\n            history = new HistoryStore(this);'''
if old not in s:
    raise SystemExit('MainActivity startup anchor not found')
s = s.replace(old, new, 1)

# Conservative Samsung/Android system bars.
start = s.index('    private void configureSystemBars() {')
end = s.index('\n    private void applySafeInsets', start)
safe_bars = '''    private void configureSystemBars() {\n        try { getWindow().setStatusBarColor(BG); } catch (Throwable ignored) {}\n        try { getWindow().setNavigationBarColor(BG); } catch (Throwable ignored) {}\n        if (Build.VERSION.SDK_INT >= 26) {\n            try {\n                int flags = getWindow().getDecorView().getSystemUiVisibility();\n                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;\n                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;\n                getWindow().getDecorView().setSystemUiVisibility(flags);\n            } catch (Throwable ignored) {}\n        }\n    }\n'''
s = s[:start] + safe_bars + s[end:]

start = s.index('    private void applySafeInsets(View v, int leftDp, int topDp, int rightDp, int bottomDp) {')
end = s.index('\n    private void applyBottomInset', start)
simple_insets = '''    private void applySafeInsets(View v, int leftDp, int topDp, int rightDp, int bottomDp) {\n        v.setPadding(dp(leftDp), dp(topDp), dp(rightDp), dp(bottomDp));\n    }\n'''
s = s[:start] + simple_insets + s[end:]

start = s.index('    private void applyBottomInset(View v, int baseBottomDp) {')
end = s.index('\n    private void showSafeHome', start)
simple_bottom = '''    private void applyBottomInset(View v, int baseBottomDp) {\n        v.setPadding(dp(8), dp(8), dp(8), dp(baseBottomDp));\n    }\n'''
s = s[:start] + simple_bottom + s[end:]

# Emergency fallback.
old = '''        } catch (Throwable startupError) {\n            showSafeHome(startupError);\n        }'''
new = '''        } catch (Throwable startupError) {\n            try {\n                showSafeHome(startupError);\n            } catch (Throwable emergencyError) {\n                TextView emergency = new TextView(this);\n                emergency.setText("EuroScan CH\\n\\nDémarrage de secours actif.\\nRéouvre l'application ou utilise la version précédente si nécessaire.");\n                emergency.setTextColor(Color.WHITE);\n                emergency.setTextSize(18);\n                emergency.setGravity(Gravity.CENTER);\n                emergency.setPadding(dp(24), dp(24), dp(24), dp(24));\n                emergency.setBackgroundColor(BG);\n                setContentView(emergency);\n            }\n        }'''
if old not in s:
    raise SystemExit('MainActivity fallback anchor not found')
s = s.replace(old, new, 1)

# 1.3.2: persistent bottom navigation on all internal pages.
old = '''    private void page(String title, String sub) {\n        ScrollView sc = new ScrollView(this);\n        sc.setFillViewport(true);\n        sc.setClipToPadding(false);\n        sc.setBackgroundColor(BG);\n\n        root = new LinearLayout(this);\n        root.setOrientation(LinearLayout.VERTICAL);\n        root.setBackgroundColor(BG);\n        sc.addView(root, new ScrollView.LayoutParams(-1, -2));\n        setContentView(sc);\n        applySafeInsets(root, 18, 18, 18, 30);\n\n        LinearLayout hero = new LinearLayout(this);\n'''
new = '''    private void page(String title, String sub) {\n        page(title, sub, "");\n    }\n\n    private void page(String title, String sub, String activeNav) {\n        FrameLayout shell = new FrameLayout(this);\n        shell.setBackgroundColor(BG);\n\n        ScrollView sc = new ScrollView(this);\n        sc.setFillViewport(true);\n        sc.setClipToPadding(false);\n        sc.setBackgroundColor(BG);\n        shell.addView(sc, new FrameLayout.LayoutParams(-1, -1));\n\n        root = new LinearLayout(this);\n        root.setOrientation(LinearLayout.VERTICAL);\n        root.setBackgroundColor(BG);\n        sc.addView(root, new ScrollView.LayoutParams(-1, -2));\n\n        LinearLayout nav = bottomNav(activeNav);\n        nav.setMinimumHeight(dp(72));\n        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);\n        navLp.setMargins(dp(14), 0, dp(14), 0);\n        shell.addView(nav, navLp);\n\n        setContentView(shell);\n        applySafeInsets(root, 18, 18, 18, 112);\n        applyBottomInset(nav, 8);\n\n        LinearLayout hero = new LinearLayout(this);\n'''
if old not in s:
    raise SystemExit('page method anchor not found')
s = s.replace(old, new, 1)

# Home uses same three-item persistent nav.
s = s.replace('LinearLayout nav = bottomNav();', 'LinearLayout nav = bottomNav("home");', 1)

old = '''    private LinearLayout bottomNav() {\n        LinearLayout nav = new LinearLayout(this);\n        nav.setOrientation(LinearLayout.HORIZONTAL);\n        nav.setGravity(Gravity.CENTER);\n        nav.setBackground(round(Color.rgb(8, 29, 57), 22));\n        nav.addView(navItem("⌂", "Accueil", true, v -> home()), new LinearLayout.LayoutParams(0, -1, 1));\n        nav.addView(navItem("⌗", "Scanner", false, v -> startLiveScanner()), new LinearLayout.LayoutParams(0, -1, 1));\n        nav.addView(navItem("▥", "Résultats", false, v -> resultsScreen()), new LinearLayout.LayoutParams(0, -1, 1));\n        nav.addView(navItem("⚙", "Réglages", false, v -> settingsScreen()), new LinearLayout.LayoutParams(0, -1, 1));\n        return nav;\n    }\n'''
new = '''    private LinearLayout bottomNav(String active) {\n        LinearLayout nav = new LinearLayout(this);\n        nav.setOrientation(LinearLayout.HORIZONTAL);\n        nav.setGravity(Gravity.CENTER);\n        nav.setBackground(round(Color.rgb(8, 29, 57), 22));\n        nav.addView(navItem("⌂", "Accueil", "home".equals(active), v -> home()), new LinearLayout.LayoutParams(0, -1, 1));\n        nav.addView(navItem("⌗", "Scanner", "scanner".equals(active), v -> startLiveScanner()), new LinearLayout.LayoutParams(0, -1, 1));\n        nav.addView(navItem("▥", "Résultats", "results".equals(active), v -> resultsScreen()), new LinearLayout.LayoutParams(0, -1, 1));\n        return nav;\n    }\n'''
if old not in s:
    raise SystemExit('bottom nav anchor not found')
s = s.replace(old, new, 1)

# Keep Results selected on both result views.
s = s.replace('page("Résultats", "Dernier tirage EuroMillions disponible.");', 'page("Résultats", "Dernier tirage EuroMillions disponible.", "results");', 1)
s = s.replace('page("Résultat du ticket", "Tirage " + d.date + " · comparaison terminée.");', 'page("Résultat du ticket", "Tirage " + d.date + " · comparaison terminée.", "results");', 1)

s = s.replace('v1.3.0', 'v1.3.2').replace('EuroScan CH 1.3.0', 'EuroScan CH 1.3.2')
main.write_text(s)

scanner = Path('euroscan-ch/app/src/main/java/ch/piiwii/euroscan/ScannerActivity.java')
s = scanner.read_text()
old = '''        getWindow().setStatusBarColor(Color.rgb(5, 18, 39));\n        getWindow().setNavigationBarColor(Color.rgb(5, 18, 39));\n        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);\n        if (Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null) {\n            getWindow().getInsetsController().setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);\n        }'''
new = '''        try { getWindow().setStatusBarColor(Color.rgb(5, 18, 39)); } catch (Throwable ignored) {}\n        try { getWindow().setNavigationBarColor(Color.rgb(5, 18, 39)); } catch (Throwable ignored) {}'''
if old not in s:
    raise SystemExit('Scanner system-bars anchor not found')
s = s.replace(old, new, 1)
start = s.index('    private void applySafeInsets(View root, FrameLayout.LayoutParams topLp, FrameLayout.LayoutParams bottomLp) {')
end = s.index('\n\n    @Override\n    public void onRequestPermissionsResult', start)
simple = '''    private void applySafeInsets(View root, FrameLayout.LayoutParams topLp, FrameLayout.LayoutParams bottomLp) {\n        topLp.setMargins(dp(10), dp(10), dp(10), 0);\n        bottomLp.setMargins(dp(16), 0, dp(16), dp(18));\n        topBar.setLayoutParams(topLp);\n        bottomCard.setLayoutParams(bottomLp);\n    }\n'''
s = s[:start] + simple + s[end:]
scanner.write_text(s)
