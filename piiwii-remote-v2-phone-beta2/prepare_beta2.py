from pathlib import Path
import sys

root = Path(sys.argv[1])
main = root / "remote/src/main/java/ch/piiwii/remote2/ui/MainActivity.kt"
gradle = root / "remote/build.gradle.kts"

s = main.read_text(encoding="utf-8")
if "import android.text.method.DigitsKeyListener" not in s:
    s = s.replace(
        "import android.text.InputType\n",
        "import android.text.InputType\nimport android.text.method.DigitsKeyListener\n",
    )

old_host = 'val host = field("Adresse IP (ex. 192.168.1.50)").apply { setText(existing?.host.orEmpty()) }'
new_host = '''val host = field("Adresse IP (ex. 192.168.1.50)").apply {
            keyListener = DigitsKeyListener.getInstance("0123456789.,")
            setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            setText(existing?.host.orEmpty())
        }'''
if old_host not in s:
    raise SystemExit("IP field anchor not found")
s = s.replace(old_host, new_host)

old_values = "val n = name.text.toString().trim(); val h = host.text.toString().trim(); val p = port.text.toString().toIntOrNull()"
new_values = "val n = name.text.toString().trim(); val h = host.text.toString().trim().replace(',', '.'); val p = port.text.toString().toIntOrNull()"
if old_values not in s:
    raise SystemExit("values anchor not found")
s = s.replace(old_values, new_values)
main.write_text(s, encoding="utf-8")

g = gradle.read_text(encoding="utf-8")
if 'versionCode = 31' not in g or 'versionName = "2.0.0-beta1"' not in g:
    raise SystemExit("beta1 version anchor not found")
g = g.replace("versionCode = 31", "versionCode = 32")
g = g.replace('versionName = "2.0.0-beta1"', 'versionName = "2.0.0-beta2"')
gradle.write_text(g, encoding="utf-8")

assert 'DigitsKeyListener.getInstance("0123456789.,")' in s
assert 'setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)' in s
assert "trim().replace(',', '.')" in s
assert 'WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()' in s
print("Phone beta2 IP keypad patch applied")
