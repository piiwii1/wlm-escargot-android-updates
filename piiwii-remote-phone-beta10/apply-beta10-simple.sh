#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:?root required}"
python3 - "$ROOT" <<'PY2'
from pathlib import Path
import sys
root=Path(sys.argv[1])
p=root/'remote/build.gradle.kts'
s=p.read_text()
s=s.replace('versionCode = 39','versionCode = 40').replace('versionName = "2.0.0-beta9"','versionName = "2.0.0-beta10"')
p.write_text(s)
p=root/'remote/src/main/java/ch/piiwii/remote2/ui/MainShellView.kt'
s=p.read_text()
s=s.replace('import android.widget.TextView\n','import android.widget.TextView\nimport android.widget.ImageView\n')
s=s.replace('import android.widget.ViewFlipper\n','import android.widget.ViewFlipper\nimport ch.piiwii.remote2.R\n')
old='        addView(TextView(context).apply {\n            text = "⚙"\n            textSize = 23f\n            gravity = Gravity.CENTER\n            setTextColor(UiKit.TEXT)\n            isClickable = true\n            isFocusable = true\n            contentDescription = "Paramètres"\n            background = UiKit.interactive(context, 14, UiKit.SURFACE)\n            setOnClickListener { settingsClick?.invoke() }\n        }, LayoutParams(UiKit.dp(context, 52), UiKit.dp(context, 52)).apply {\n            marginStart = UiKit.dp(context, 8)\n        })'
new='        addView(ImageView(context).apply {\n            setImageResource(R.drawable.ic_settings)\n            scaleType = ImageView.ScaleType.CENTER_INSIDE\n            isClickable = true\n            isFocusable = true\n            contentDescription = "Paramètres"\n            background = UiKit.interactive(context, 14, UiKit.SURFACE)\n            val pad = UiKit.dp(context, 7)\n            setPadding(pad, pad, pad, pad)\n            setOnClickListener { settingsClick?.invoke() }\n        }, LayoutParams(UiKit.dp(context, 52), UiKit.dp(context, 52)).apply {\n            marginStart = UiKit.dp(context, 8)\n        })'
assert old in s
s=s.replace(old,new)
old='                addView(TextView(context).apply {\n                    text = section.glyph\n                    textSize = 21f\n                    gravity = Gravity.CENTER\n                })'
new='                addView(ImageView(context).apply {\n                    setImageResource(when (section) {\n                        RemoteSection.REMOTE, RemoteSection.NAVIGATION -> R.drawable.ic_remote\n                        RemoteSection.TOUCHPAD -> R.drawable.ic_touchpad\n                        RemoteSection.KEYBOARD, RemoteSection.KEYBOARD_ADVANCED -> R.drawable.ic_keyboard\n                        RemoteSection.APPS -> R.drawable.ic_apps\n                    })\n                    scaleType = ImageView.ScaleType.CENTER_INSIDE\n                    contentDescription = section.label\n                }, LayoutParams(UiKit.dp(context, 32), UiKit.dp(context, 32)))'
assert old in s
s=s.replace(old,new)
p.write_text(s)
p=root/'remote/src/main/java/ch/piiwii/remote2/ui/screens/SectionScreenFactory.kt'
s=p.read_text()
s=s.replace('import android.widget.LinearLayout\n','import android.widget.LinearLayout\nimport android.widget.ImageView\n')
s=s.replace('import android.widget.TextView\n','import android.widget.TextView\nimport ch.piiwii.remote2.R\n')
old='                        addView(TextView(context).apply {\n                            text = if (shortcut.type == ShortcutType.URL) "🌐" else "▣"\n                            textSize = 25f\n                            gravity = Gravity.CENTER\n                            setTextColor(UiKit.ACCENT)\n                        })'
new='                        addView(ImageView(context).apply {\n                            setImageResource(if (shortcut.type == ShortcutType.URL) R.drawable.ic_browser else R.drawable.ic_apps)\n                            scaleType = ImageView.ScaleType.CENTER_INSIDE\n                            contentDescription = shortcut.name\n                        }, LinearLayout.LayoutParams(UiKit.dp(context, 44), UiKit.dp(context, 44)))'
assert old in s
s=s.replace(old,new)
marker='    private fun appGrid(context: Context, apps: List<RemoteApp>, onLaunch: (String) -> Unit): GridLayout = GridLayout(context).apply {'
helper='    private fun appIconRes(id: String): Int? = when (id.lowercase()) {\n        "youtube" -> R.drawable.ic_youtube\n        "spotify" -> R.drawable.ic_spotify\n        "vlc" -> R.drawable.ic_vlc\n        "browser", "navigateur" -> R.drawable.ic_browser\n        else -> null\n    }\n\n'
assert marker in s
s=s.replace(marker,helper+marker)
old='                addView(TextView(context).apply {\n                    text = app.glyph\n                    textSize = if (app.glyph.length > 2) 18f else 27f\n                    gravity = Gravity.CENTER\n                    setTextColor(UiKit.ACCENT)\n                    setTypeface(typeface, Typeface.BOLD)\n                })'
new='                val iconRes = appIconRes(app.id)\n                if (iconRes != null) {\n                    addView(ImageView(context).apply {\n                        setImageResource(iconRes)\n                        scaleType = ImageView.ScaleType.CENTER_INSIDE\n                        contentDescription = app.name\n                    }, LinearLayout.LayoutParams(UiKit.dp(context, 48), UiKit.dp(context, 48)))\n                } else {\n                    addView(TextView(context).apply {\n                        text = app.glyph\n                        textSize = if (app.glyph.length > 2) 18f else 27f\n                        gravity = Gravity.CENTER\n                        setTextColor(UiKit.ACCENT)\n                        setTypeface(typeface, Typeface.BOLD)\n                    })\n                }'
assert old in s
s=s.replace(old,new)
p.write_text(s)
PY2
RES="$ROOT/remote/src/main/res/drawable-nodpi"
mkdir -p "$RES"
for n in remote settings apps keyboard touchpad browser vlc youtube spotify; do
  base64 -d "$GITHUB_WORKSPACE/piiwii-remote-phone-beta10/icons48/${n}.b64" > "$RES/ic_${n}.png"
done
echo "beta10 simple icons applied"
