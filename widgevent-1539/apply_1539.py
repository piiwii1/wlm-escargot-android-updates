#!/usr/bin/env python3
"""WidgEvent 1.5.39: live countdown for next image change on Aujourd'hui."""
from pathlib import Path
import sys
r=Path(sys.argv[1])
p=r/'app/src/main/java/ch/piiwii/events/ui/MainActivity.kt'
s=p.read_text(encoding='utf-8')
def once(old,new,name):
 global s
 assert s.count(old)==1,(name,s.count(old))
 s=s.replace(old,new,1)
once('import android.os.Bundle','import android.os.Bundle\nimport android.os.Handler\nimport android.os.Looper','imports')
once('''    private var settingsSection = SettingsSection.ROOT
''','''    private var settingsSection = SettingsSection.ROOT
    /** Exactly one lightweight UI timer; release it on navigation/background. */
    private val countdownHandler = Handler(Looper.getMainLooper())
    private var nextChangeCountdown: Runnable? = null

    private fun stopNextChangeCountdown() {
        nextChangeCountdown?.let(countdownHandler::removeCallbacks)
        nextChangeCountdown = null
    }

    private fun nextChangeTimeLabel(boundary: ZonedDateTime): String {
        val remainingMillis = java.time.Duration.between(
            java.time.Instant.now(), boundary.toInstant()).toMillis()
        if (remainingMillis <= 0L) return "Changement en cours…"
        val seconds = remainingMillis / 1000L + if (remainingMillis % 1000L > 0) 1L else 0L
        val days = seconds / 86400L
        val hours = (seconds / 3600L) % 24L
        val minutes = (seconds / 60L) % 60L
        val secs = seconds % 60L
        return if (days > 0L) {
            "Dans ¤{days} j ¤{hours.toString().padStart(2, '0')} h " +
                "¤{minutes.toString().padStart(2, '0')} min ¤{secs.toString().padStart(2, '0')} s"
        } else {
            "Dans ¤{hours.toString().padStart(2, '0')} h " +
                "¤{minutes.toString().padStart(2, '0')} min ¤{secs.toString().padStart(2, '0')} s"
        }
    }

    private fun runNextChangeCountdown(label: TextView, boundary: ZonedDateTime) {
        stopNextChangeCountdown()
        label.text = nextChangeTimeLabel(boundary)
        val ticker = object : Runnable {
            override fun run() {
                if (selectedTab != 0 || isFinishing || !label.isAttachedToWindow) {
                    stopNextChangeCountdown()
                    return
                }
                if (!java.time.Instant.now().isBefore(boundary.toInstant())) {
                    stopNextChangeCountdown()
                    showTab(0)
                    return
                }
                label.text = nextChangeTimeLabel(boundary)
                countdownHandler.postDelayed(this, 1000L)
            }
        }
        nextChangeCountdown = ticker
        countdownHandler.postDelayed(ticker, 1000L)
    }
''','field and helpers')
once('''    override fun onResume() {
        super.onResume()
        if (::contentHost.isInitialized) showTab(selectedTab)
    }
''','''    override fun onResume() {
        super.onResume()
        if (::contentHost.isInitialized) showTab(selectedTab)
    }

    override fun onPause() {
        stopNextChangeCountdown()
        super.onPause()
    }

    override fun onDestroy() {
        stopNextChangeCountdown()
        super.onDestroy()
    }
''','lifecycle')
once('''    private fun showTab(index: Int) {
        selectedTab = index.coerceIn(0, 4)
        contentHost.removeAllViews()
''','''    private fun showTab(index: Int) {
        stopNextChangeCountdown()
        selectedTab = index.coerceIn(0, 4)
        contentHost.removeAllViews()
''','navigation')
once('''        nextInfo.addView(UiFactory.muted(this, formatDateTime(state.nextChange)))
        nextRow.addView(nextInfo,''','''        nextInfo.addView(UiFactory.muted(this, formatDateTime(state.nextChange)))
        if (state.nextChange != null) {
            val timer = UiFactory.body(this, nextChangeTimeLabel(state.nextChange), 13f).apply {
                setTextColor(UiFactory.BLUE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(UiFactory.dp(this@MainActivity, 9), UiFactory.dp(this@MainActivity, 4),
                    UiFactory.dp(this@MainActivity, 9), UiFactory.dp(this@MainActivity, 4))
                background = UiFactory.rounded(Color.rgb(235, 243, 255), 11f, this@MainActivity)
                contentDescription = "Temps restant avant le prochain changement"
            }
            nextInfo.addView(timer, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = UiFactory.dp(this@MainActivity, 6)
            })
            runNextChangeCountdown(timer, state.nextChange)
        }
        nextRow.addView(nextInfo,''','live counter')
s=s.replace('¤','$')
p.write_text(s,encoding='utf-8')
b=r/'app/build.gradle.kts'
v=b.read_text(encoding='utf-8')
for old,new in [('versionCode = 56','versionCode = 57'),('versionName = "1.5.38"','versionName = "1.5.39"')]:
 assert v.count(old)==1,(old,v.count(old))
 v=v.replace(old,new,1)
assert 'applicationId = "ch.piiwii.eventb"' in v
b.write_text(v,encoding='utf-8')
print('PASS WidgEvent 1.5.39 countdown and lifecycle')
