from pathlib import Path

root = Path('/tmp/piiwii-nfc/PiiWii-NFC-1.0.5')

# Version
p = root / 'app/build.gradle'
s = p.read_text()
s = s.replace("versionCode 5", "versionCode 6")
s = s.replace("versionName '1.0.4'", "versionName '1.0.5'")np = p.write_text(s)

# README
p = root / 'README.md'
s = p.read_text()
s = s.replace('PiiWii NFC 1.0.4', 'PiiWii NFC 1.0.5')
s = s.replace('versionName: 1.0.4', 'versionName: 1.0.5')
s = s.replace('versionCode: 5', 'versionCode: 6')
p.write_text(s)

# Safe-area layout
p = root / 'app/src/main/res/layout/activity_main.xml'
s = p.read_text()
s = s.replace('<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"\n    android:layout_width="match_parent"', '<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"\n    android:id="@+id/rootScroll"\n    android:layout_width="match_parent"')
s = s.replace('android:fillViewport="true">', 'android:clipToPadding="false"\n    android:fillViewport="true">')
s = s.replace('PiiWii NFC 1.0.4 •', 'PiiWii NFC 1.0.5 •')
p.write_text(s)

# Android system bars / insets
p = root / 'app/src/main/java/ch/piiwii/nfc/MainActivity.java'
s = p.read_text()
s = s.replace('import android.view.View;\n', 'import android.view.View;\nimport android.view.WindowInsets;\nimport android.view.Window;\n')
s = s.replace('        setContentView(R.layout.activity_main);\n\n', '        setContentView(R.layout.activity_main);\n        configureSystemBarsAndInsets();\n\n')
method = '''\n    private void configureSystemBarsAndInsets() {\n        Window window = getWindow();\n        window.setStatusBarColor(getColor(R.color.bg));\n        window.setNavigationBarColor(getColor(R.color.bg));\n\n        View root = findViewById(R.id.rootScroll);\n        if (root == null) return;\n\n        root.setOnApplyWindowInsetsListener((v, insets) -> {\n            int left;\n            int top;\n            int right;\n            int bottom;\n            if (android.os.Build.VERSION.SDK_INT >= 30) {\n                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());\n                left = bars.left;\n                top = bars.top;\n                right = bars.right;\n                bottom = bars.bottom;\n            } else {\n                left = insets.getSystemWindowInsetLeft();\n                top = insets.getSystemWindowInsetTop();\n                right = insets.getSystemWindowInsetRight();\n                bottom = insets.getSystemWindowInsetBottom();\n            }\n            v.setPadding(left, top, right, bottom);\n            return insets;\n        });\n        root.requestApplyInsets();\n    }\n\n'''
s = s.replace('    private void prepareWrite() {', method + '    private void prepareWrite() {')
p.write_text(s)

# Changelog
p = root / 'CHANGELOG.md'
s = p.read_text()
if '## 1.0.5' not in s:
    s = s.replace('# Changelog\n', '# Changelog\n\n## 1.0.5 — 2026-09-29\n- Correction de l’interface sous la barre d’état Android (heure/batterie).\n- Correction de l’interface sous la barre de navigation/gestes en bas.\n- Insets système dynamiques pour Android récent et anciennes versions compatibles.\n- Couleur cohérente des barres système avec le thème sombre.\n- Aucun changement du moteur NFC.\n')
p.write_text(s)
