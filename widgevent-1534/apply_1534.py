#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1])
p=root/'app/src/main/java/ch/piiwii/events/ui/MainActivity.kt'
s=p.read_text(encoding='utf-8')
def change(old,new):
 global s
 count=s.count(old)
 assert count==1, (old[:100], count)
 s=s.replace(old,new,1)
change('import android.graphics.Color\n','import android.graphics.Color\nimport android.graphics.drawable.ColorDrawable\n')
change('import android.widget.PopupMenu\n','import android.widget.PopupMenu\nimport android.widget.PopupWindow\n')
change('''                    contentDescription = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)) +
                        (if (hasOverlap) ", plusieurs événements concurrents" else if (hasEvents) ", événement programmé" else "") +
                        (if (isSelected) ", sélectionné" else "")
                    setOnClickListener {
                        selectedCalendarDate = date
                        showTab(3)
                    }''','''                    tag = "widgevent.calendar.day.$date"
                    contentDescription = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)) +
                        (if (hasOverlap) ", plusieurs événements gérés automatiquement" else if (hasEvents) ", événement programmé" else "") +
                        (if (isSelected) ", sélectionné" else "")
                    setOnClickListener {
                        selectedCalendarDate = date
                        showTab(3)
                        val anchor = contentHost.findViewWithTag<View>("widgevent.calendar.day.$date")
                        anchor?.post {
                            if (!isFinishing && anchor.isAttachedToWindow) {
                                showCalendarDayPopup(anchor, date, eventDays[date].orEmpty())
                            }
                        }
                    }''')
change('''                // Pastille d'alerte distincte lorsqu'il y a plusieurs événements le même jour.
                item.addView(TextView(this).apply {
                    text = if (hasOverlap) "!" else if (hasEvents) "●" else ""
                    textSize = if (hasOverlap) 14f else 10f
                    gravity = Gravity.CENTER
                    setTypeface(null, if (hasOverlap) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                    setTextColor(if (hasOverlap) Color.rgb(184, 93, 25) else UiFactory.BLUE)
                    contentDescription = if (hasOverlap) "Chevauchement d'événements" else null
                }, LinearLayout.LayoutParams(d(15), d(15)))''','''                // Multiple events are managed automatically, not considered a conflict.
                item.addView(TextView(this).apply {
                    text = if (hasEvents) "●" else ""
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setTextColor(UiFactory.BLUE)
                    contentDescription = if (hasOverlap) "Plusieurs événements classés automatiquement" else null
                }, LinearLayout.LayoutParams(d(15), d(15)))''')
popup='''    /** Small, anchored calendar preview without changing app navigation. */
    private fun showCalendarDayPopup(anchor: View, date: LocalDate, events: List<EventDefinition>) {
        val d = { n: Int -> UiFactory.dp(this, n) }
        val card = UiFactory.vertical(this, 5).apply {
            setPadding(d(13), d(11), d(13), d(12))
            background = UiFactory.rounded(Color.WHITE, 15f, this@MainActivity, Color.rgb(220, 227, 237))
        }
        card.addView(UiFactory.title(this,
            date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                .replaceFirstChar { it.uppercase() }, 15f))
        if (events.isEmpty()) {
            card.addView(UiFactory.muted(this, "Aucun événement ce jour-là"))
        } else {
            events.take(4).forEachIndexed { index, event ->
                card.addView(UiFactory.body(this,
                    (if (index == 0) "● " else "• ") + event.name, 13.5f).apply {
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(if (index == 0) UiFactory.BLUE else UiFactory.TEXT)
                })
            }
            if (events.size > 4) {
                card.addView(UiFactory.muted(this, "+ ${events.size - 4} autres événements"))
            }
            if (events.size > 1) {
                card.addView(UiFactory.muted(this, "Premier affiché en priorité"))
            }
        }
        PopupWindow(card, d(250), ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = d(9).toFloat()
            isClippingEnabled = true
            showAsDropDown(anchor, -d(92), d(3))
        }
    }

'''
needle='    private fun showDayDetails(date: LocalDate, events: List<EventDefinition>) {'
change(needle,popup+needle)
p.write_text(s,encoding='utf-8')
b=root/'app/build.gradle.kts'
t=b.read_text()
for old,new in [('versionCode = 51','versionCode = 52'),('versionName = "1.5.33"','versionName = "1.5.34"')]:
 assert t.count(old)==1,(old,t.count(old))
 t=t.replace(old,new,1)
assert 'applicationId = "ch.piiwii.eventb"' in t
b.write_text(t)
print('PASS popup calendar + automatic priorities 1.5.34')
