from pathlib import Path

root = Path('/tmp/piiwii-nfc/PiiWii-NFC-1.0.6')

p = root / 'app/build.gradle'
s = p.read_text()
s = s.replace("applicationId 'ch.piiwii.nfc.v105'", "applicationId 'ch.piiwii.nfc.v106'")
s = s.replace('targetSdk 35', 'targetSdk 34')
s = s.replace('versionCode 6', 'versionCode 7')
s = s.replace("versionName '1.0.5'", "versionName '1.0.6'")
p.write_text(s)

p = root / 'app/src/main/res/layout/activity_main.xml'
s = p.read_text()
s = s.replace('<LinearLayout\n        android:layout_width="match_parent"', '<LinearLayout\n        android:id="@+id/safeContent"\n        android:layout_width="match_parent"', 1)
s = s.replace('PiiWii NFC 1.0.5 •', 'PiiWii NFC 1.0.6 •')
p.write_text(s)

p = root / 'app/src/main/java/ch/piiwii/nfc/MainActivity.java'
s = p.read_text()
start = s.index('    private void configureSystemBarsAndInsets() {')
end = s.index('    private void prepareWrite() {', start)
method = '''    private void configureSystemBarsAndInsets() {\n        final View safe = findViewById(R.id.safeContent);\n        if (safe == null) return;\n\n        final int baseLeft = safe.getPaddingLeft();\n        final int baseTop = safe.getPaddingTop();\n        final int baseRight = safe.getPaddingRight();\n        final int baseBottom = safe.getPaddingBottom();\n\n        safe.setOnApplyWindowInsetsListener((v, insets) -> {\n            int left = 0, top = 0, right = 0, bottom = 0;\n            if (android.os.Build.VERSION.SDK_INT >= 30) {\n                int types = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();\n                android.graphics.Insets bars = insets.getInsets(types);\n                left = bars.left;\n                top = bars.top;\n                right = bars.right;\n                bottom = bars.bottom;\n            } else {\n                left = insets.getSystemWindowInsetLeft();\n                top = insets.getSystemWindowInsetTop();\n                right = insets.getSystemWindowInsetRight();\n                bottom = insets.getSystemWindowInsetBottom();\n            }\n            v.setPadding(baseLeft + left, baseTop + top, baseRight + right, baseBottom + bottom);\n            return insets;\n        });\n        safe.post(safe::requestApplyInsets);\n    }\n\n'''
s = s[:start] + method + s[end:]
p.write_text(s)

p = root / 'README.md'
s = p.read_text().replace('PiiWii NFC 1.0.5', 'PiiWii NFC 1.0.6').replace('versionName: 1.0.5', 'versionName: 1.0.6').replace('versionCode: 6', 'versionCode: 7')
p.write_text(s)

p = root / 'CHANGELOG.md'
s = p.read_text()
if '## 1.0.6' not in s:
    s = s.replace('# Changelog\n', '# Changelog\n\n## 1.0.6 — 2026-09-29\n- Correction renforcée des zones système Android haut/bas.\n- targetSdk ramené à 34 pour éviter le edge-to-edge forcé d’Android 15 sur cette version sideload.\n- Insets appliqués au conteneur de contenu avec prise en compte de la découpe écran.\n- Aucun changement du moteur NFC.\n')
p.write_text(s)
