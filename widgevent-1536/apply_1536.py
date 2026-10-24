#!/usr/bin/env python3
"""WidgEvent 1.5.36: compact icon-title-J-x/date-duration cards."""
from pathlib import Path
import sys
root=Path(sys.argv[1]); p=root/'app/src/main/java/ch/piiwii/events/ui/MainActivity.kt'
s=p.read_text(encoding='utf-8')
def replace_once(old,new):
 global s
 assert s.count(old)==1, (old[:90],s.count(old))
 s=s.replace(old,new,1)
def block(start,end,new):
 global s
 assert s.count(start)==1, (start,s.count(start))
 a=s.index(start); b=s.index(end,a)
 s=s[:a]+new+s[b:]
block('            listed.forEachIndexed { index, event ->\n',
'        }\n        // Vue chronologique intelligente','''            listed.forEach { event ->
                val card = UiFactory.horizontal(this, 0).apply {
                    setPadding(d(11), d(10), d(12), d(10))
                    background = UiFactory.rounded(Color.WHITE, 18f, this@MainActivity,
                        Color.rgb(230, 235, 244))
                    minimumHeight = d(75)
                    isClickable = true
                    isFocusable = true
                    contentDescription = "§{event.name} : ouvrir les détails"
                    setOnClickListener { openEditor(event.id) }
                    setOnLongClickListener { showDayDetails(selected, listed); true }
                }
                card.addView(calendarEventArtwork(event, 54), LinearLayout.LayoutParams(d(54), d(54)))
                val details = UiFactory.vertical(this, 0)
                val heading = UiFactory.horizontal(this, 0)
                heading.addView(UiFactory.title(this, event.name, 16f).apply {
                    maxLines = 2
                    setPadding(0, 0, 0, 0)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val counter = calendarDayCountdown(selected, zone)
                heading.addView(UiFactory.chip(this, counter, counter == "Aujourd’hui"),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = d(8) })
                details.addView(heading)
                val period = calendarDayPeriod(event, selected, engine, zone)
                details.addView(UiFactory.muted(this, period.first).apply {
                    setPadding(0, d(4), 0, 0); maxLines = 2
                })
                if (period.second.isNotBlank()) details.addView(UiFactory.muted(this,
                    "Durée : §{period.second}").apply { setPadding(0, d(2), 0, 0) })
                card.addView(details, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = d(10) })
                body.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = d(9) })
            }
''')
replace_once('"Dates, durée, image prévue et chevauchements · événements activés uniquement"',
             '"Dates et durée · événements activés uniquement"')
block('        upcoming.forEach { entry ->\n','        return page(body)\n','''        upcoming.forEach { entry ->
            val card = UiFactory.horizontal(this, 0).apply {
                setPadding(d(11), d(10), d(12), d(10))
                background = UiFactory.rounded(Color.WHITE, 18f, this@MainActivity,
                    Color.rgb(230, 235, 244))
                minimumHeight = d(77)
                isClickable = true
                isFocusable = true
                contentDescription = "§{entry.event.name}, §{entry.countdown}, ouvrir l'événement"
                setOnClickListener { openEditor(entry.event.id) }
            }
            card.addView(calendarEventArtwork(entry.event, 54),
                LinearLayout.LayoutParams(d(54), d(54)))
            val details = UiFactory.vertical(this, 0)
            val heading = UiFactory.horizontal(this, 0)
            heading.addView(UiFactory.title(this, entry.event.name, 16f).apply {
                setPadding(0, 0, 0, 0); maxLines = 2
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val countdown = if (entry.countdown == "Demain") "J-1" else entry.countdown.replace('−', '-')
            heading.addView(UiFactory.chip(this, countdown, entry.activeNow),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = d(8) })
            details.addView(heading)
            details.addView(UiFactory.muted(this, entry.periodLabel).apply {
                setPadding(0, d(4), 0, 0); maxLines = 2
            })
            details.addView(UiFactory.muted(this, "Durée : §{entry.durationLabel}").apply {
                setPadding(0, d(2), 0, 0)
            })
            card.addView(details, LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = d(10) })
            body.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = d(9) })
        }
''')
needle='    /** Résumé horaire issu des périodes résolues, sans horaires inventés. */'
helper='''    /** Calendar day countdown in the upper-right corner. */
    private fun calendarDayCountdown(date: LocalDate, zone: java.time.ZoneId): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(zone), date)
        return when {
            days > 0 -> "J-$days"
            days == 0L -> "Aujourd’hui"
            else -> "Passé"
        }
    }

    /** Date and duration from the real occurrence (no guessed event duration). */
    private fun calendarDayPeriod(event: EventDefinition, date: LocalDate,
        engine: DateEventEngine, zone: java.time.ZoneId): Pair<String, String> {
        val start = date.atStartOfDay(zone)
        val end = start.plusDays(1)
        val occurrence = engine.intervalsForEvent(event, zone, start.minusYears(1), end.plusYears(1))
            .filter { it.start.isBefore(end) && it.endExclusive.isAfter(start) }
            .minByOrNull { it.start.toInstant() }
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRENCH)
        if (occurrence == null) return date.format(DateTimeFormatter.ofPattern(
            "d MMM yyyy", Locale.FRENCH)) to ""
        val dateLabel = if (occurrence.start.toLocalDate() ==
            occurrence.endExclusive.minusNanos(1).toLocalDate()) {
            occurrence.start.format(formatter)
        } else {
            "§{occurrence.start.format(formatter)} → §{occurrence.endExclusive.minusMinutes(1).format(formatter)}"
        }
        val minutes = java.time.Duration.between(occurrence.start, occurrence.endExclusive)
            .toMinutes().coerceAtLeast(1)
        val durationLabel = when {
            minutes >= 1440L && minutes % 1440L == 0L -> "§{minutes / 1440L} j"
            minutes >= 1440L -> "§{minutes / 1440L} j §{minutes % 1440L / 60L} h"
            minutes >= 60L -> "§{minutes / 60L} h §{minutes % 60L} min"
            else -> "$minutes min"
        }
        return dateLabel to durationLabel
    }

'''
replace_once(needle,helper+needle)
s=s.replace('§','$')
p.write_text(s,encoding='utf-8')
b=root/'app/build.gradle.kts'; v=b.read_text()
for old,new in [('versionCode = 53','versionCode = 54'),('versionName = "1.5.35"','versionName = "1.5.36"')]:
 assert v.count(old)==1,(old,v.count(old));v=v.replace(old,new,1)
assert 'applicationId = "ch.piiwii.eventb"' in v
b.write_text(v)
print('PASS 1.5.36: image | 10dp | title + countdown | date | duration')
