#!/usr/bin/env python3
"""WidgEvent 1.5.40: discreet bottom-right image test action; icon refresh in top bar."""
from pathlib import Path
import sys
root = Path(sys.argv[1])
main = root / 'app/src/main/java/ch/piiwii/events/ui/MainActivity.kt'
s = main.read_text(encoding='utf-8')
def replace_once(old,new,label):
    global s
    n = s.count(old)
    assert n == 1, (label, n)
    s = s.replace(old,new,1)

replace_once(
'''        topBar.addView(screenTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val settingsButton = ImageView(this).apply {''',
'''        topBar.addView(screenTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        // Small refresh action in the shared app bar, immediately before Settings.
        val refreshButton = ImageView(this).apply {
            setImageResource(ch.piiwii.events.R.drawable.nav_refresh)
            setColorFilter(UiFactory.TEXT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Actualiser les événements et l’image du widget"
            background = UiFactory.rounded(Color.rgb(241, 246, 253), 50f, this@MainActivity)
            setPadding(UiFactory.dp(this@MainActivity, 10), UiFactory.dp(this@MainActivity, 10),
                UiFactory.dp(this@MainActivity, 10), UiFactory.dp(this@MainActivity, 10))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                WidgetRenderer.updateAll(this@MainActivity, "header_refresh")
                showTab(selectedTab)
            }
        }
        topBar.addView(refreshButton, LinearLayout.LayoutParams(UiFactory.dp(this, 42),
            UiFactory.dp(this, 42)).apply {
            marginEnd = UiFactory.dp(this@MainActivity, 7)
        })
        val settingsButton = ImageView(this).apply {''','top refresh icon')

replace_once(
'''            val testButton = UiFactory.button(this, "Tester l’image", true).apply {
                setOnClickListener {
                    WidgetRenderer.setTestEvent(this@MainActivity, event.id)
                    showTab(0)
                }
            }
            photo.addView(testButton, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = UiFactory.dp(this@MainActivity, 13)
            })''',
'''            val testButton = TextView(this).apply {
                text = "Tester l’image"
                textSize = 11.5f
                setTextColor(UiFactory.TEXT)
                gravity = Gravity.CENTER
                includeFontPadding = false
                minimumHeight = UiFactory.dp(this@MainActivity, 31)
                setPadding(UiFactory.dp(this@MainActivity, 11), UiFactory.dp(this@MainActivity, 5),
                    UiFactory.dp(this@MainActivity, 11), UiFactory.dp(this@MainActivity, 5))
                background = UiFactory.rounded(Color.rgb(247, 249, 253), 10f,
                    this@MainActivity, Color.rgb(222, 230, 240))
                isClickable = true
                isFocusable = true
                contentDescription = "Tester l’image affichée sur le widget"
                setOnClickListener {
                    WidgetRenderer.setTestEvent(this@MainActivity, event.id)
                    showTab(0)
                }
            }
            photo.addView(testButton, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.RIGHT).apply {
                bottomMargin = UiFactory.dp(this@MainActivity, 11)
                rightMargin = UiFactory.dp(this@MainActivity, 11)
            })''','subtle bottom right image test')

replace_once(
'''        val actions = UiFactory.horizontal(this)
        actions.addView(UiFactory.secondaryButton(this, "⟳  Actualiser").apply {
            setOnClickListener { WidgetRenderer.updateAll(this@MainActivity, "home_refresh"); showTab(0) }
        }, LinearLayout.LayoutParams(0, UiFactory.dp(this, 53), 1f))
        actions.addView(UiFactory.button(this, "Ajouter le widget").apply {
            setOnClickListener { requestAddWidget() }
        }, LinearLayout.LayoutParams(0, UiFactory.dp(this, 53), 1f).apply {
            leftMargin = UiFactory.dp(this@MainActivity, 10)
        })
        body.addView(actions)''',
'''        // Refresh moved to the top bar; keep Add Widget here as the only full-width action.
        body.addView(UiFactory.button(this, "Ajouter le widget").apply {
            setOnClickListener { requestAddWidget() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            UiFactory.dp(this, 48)))''','remove bottom refresh')

main.write_text(s,encoding='utf-8')
drawable = root / 'app/src/main/res/drawable/nav_refresh.xml'
assert not drawable.exists()
drawable.write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M17.65,6.35C16.2,4.9 14.21,4 12,4C7.58,4 4,7.58 4,12C4,16.42 7.58,20 12,20C15.73,20 18.84,17.45 19.73,14L17.65,14C16.83,16.33 14.61,18 12,18C8.69,18 6,15.31 6,12C6,8.69 8.69,6 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11L20,11L20,4L17.65,6.35Z"/>
</vector>
''',encoding='utf-8')
build=root/'app/build.gradle.kts'
v=build.read_text(encoding='utf-8')
for old,new in [('versionCode = 57','versionCode = 58'),('versionName = "1.5.39"','versionName = "1.5.40"')]:
    assert v.count(old)==1,(old,v.count(old))
    v=v.replace(old,new,1)
assert 'applicationId = "ch.piiwii.eventb"' in v
build.write_text(v,encoding='utf-8')
print('PASS WidgEvent 1.5.40 minimal image test action + global refresh icon')
