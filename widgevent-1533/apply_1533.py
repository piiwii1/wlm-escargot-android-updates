#!/usr/bin/env python3
from pathlib import Path
import sys
r=Path(sys.argv[1]); app=r/'app/src/main/java/ch/piiwii/events'
def edit(name, replacements):
 p=app/name
 s=p.read_text(encoding='utf-8')
 for old,new in replacements:
  n=s.count(old)
  if n!=1: raise RuntimeError(f'{name}: expected one occurrence, got {n}: {old[:90]}')
  s=s.replace(old,new,1)
 p.write_text(s,encoding='utf-8')
 print('PATCHED',name)
P='engine/EventPriorityResolver.kt'
(app/P).write_text("""package ch.piiwii.events.engine

import ch.piiwii.events.data.DefaultEvents
import ch.piiwii.events.model.EventDefinition
import ch.piiwii.events.model.EventInterval
import ch.piiwii.events.model.RecurrenceRule
import java.time.Duration
import java.util.Locale

/** Automatic precedence shared by the widget and calendar. No saved data is changed. */
object EventPriorityResolver {
    private val seasonKeys = setOf("winter", "spring", "summer", "autumn", "fall",
        "season_winter", "season_spring", "season_summer", "season_autumn")
    private val seasonNames = setOf("hiver", "printemps", "ete", "été", "automne",
        "winter", "spring", "summer", "autumn", "fall")

    fun tier(event: EventDefinition): Int = when {
        event.id == DefaultEvents.FC_SION_ID || event.id == DefaultEvents.SWISS_MATCH_ID -> 0
        event.id == DefaultEvents.NEUTRAL_ID || event.alwaysActive ||
            event.recurrenceRule == RecurrenceRule.ALWAYS -> 3
        seasonKeys.contains(event.predefinedKey.lowercase(Locale.ROOT)) ||
            seasonKeys.contains(event.id.lowercase(Locale.ROOT)) ||
            seasonNames.contains(event.name.trim().lowercase(Locale.ROOT)) -> 2
        else -> 1
    }

    fun rank(intervals: Collection<EventInterval>): List<EventInterval> =
        intervals.sortedWith(compareBy<EventInterval> { tier(it.event) }
            .thenBy { Duration.between(it.start.toInstant(), it.endExclusive.toInstant()).toMillis().coerceAtLeast(0) }
            .thenBy { it.event.priority }
            .thenBy { it.event.name.lowercase(Locale.ROOT) }
            .thenBy { it.event.id })

    fun sort(events: Collection<EventDefinition>): List<EventDefinition> =
        events.sortedWith(compareBy<EventDefinition> { tier(it) }
            .thenBy { it.priority }.thenBy { it.name.lowercase(Locale.ROOT) }.thenBy { it.id })

    fun choose(events: Collection<EventDefinition>): EventDefinition? = sort(events).firstOrNull()
}
""")
edit('data/EventRepository.kt', [
 ('        configuration.events.forEach { it.updatedAt = System.currentTimeMillis() }','        // Only the edited event updates its timestamp.'),
 ('        if (configFile.exists()) configFile.delete()\n','        // Replace config by rename; do not delete the old config first.\n')
])
edit('data/ImageStorage.kt', [
 ('}-v1")','}-v2")')
])
edit('engine/DateEventEngine.kt', [
 ('''        val activeIntervals = intervals.filter { !now.isBefore(it.start) && now.isBefore(it.endExclusive) }
        val activeEvents = EventPriorityResolver.sort(activeIntervals.map { it.event }.distinctBy { it.id })''',
 '''        val activeIntervals = EventPriorityResolver.rank(
            intervals.filter { !now.isBefore(it.start) && now.isBefore(it.endExclusive) }
        )
        val activeEvents = activeIntervals.map { it.event }.distinctBy { it.id }'''),
 ('        val chosen = testEvent ?: EventPriorityResolver.choose(activeEvents) ?: neutral','        val chosen = testEvent ?: activeEvents.firstOrNull() ?: neutral'),
 ('''        return EventPriorityResolver.sort(
            intervals.filter { it.start.isBefore(dayEnd) && it.endExclusive.isAfter(dayStart) }
                .map { it.event }
                .distinctBy { it.id }
        )''','''        return EventPriorityResolver.rank(
            intervals.filter { it.start.isBefore(dayEnd) && it.endExclusive.isAfter(dayStart) }
        ).map { it.event }.distinctBy { it.id }'''),
 ('a la priorité ${chosen.priority}, devant','s’affiche automatiquement devant')
])
edit('engine/IntelligentCalendarForecast.kt', [
 ('val conflicts: List<String>','val overlaps: List<String>'),
 ('{ it.event.priority }, { it.start.toInstant() }','{ EventPriorityResolver.tier(it.event) }, { it.start.toInstant() }'),
 ('compareBy<EventDefinition> { it.priority }.thenBy { it.name }','compareBy<EventDefinition> { EventPriorityResolver.tier(it) }.thenBy { it.name }'),
 ('conflicts = otherEvents','overlaps = otherEvents')
])
edit('ui/MainActivity.kt', [
 ('''            text = "${upcoming.count { it.conflicts.isNotEmpty() }} conflit(s)"''', '''            text = "Priorités auto"'''),
 ('''            setTextColor(if (upcoming.any { it.conflicts.isNotEmpty() })
                Color.rgb(172, 84, 20) else UiFactory.MUTED)''','''            setTextColor(UiFactory.MUTED)'''),
 ('if (entry.conflicts.isNotEmpty())','if (entry.overlaps.isNotEmpty())'),
 ('"⚠ Chevauchement : ${entry.conflicts.joinToString()} · priorité appliquée"','"Superposition gérée automatiquement avec : ${entry.overlaps.joinToString()}"'),
 ('setTextColor(Color.rgb(165, 83, 22))','setTextColor(UiFactory.MUTED)'),
 ('''        body.addView(UiFactory.settingsIconTile(this, ch.piiwii.events.R.drawable.settings_priority,
            "Priorités", "Choisir quelle image l’emporte", Color.rgb(147, 104, 229)).apply {
            setOnClickListener { openSettings(SettingsSection.PRIORITIES) }
        })''','''        // Automatic priority ordering: no manual adjustment needed.'''),
 ('WidgetRenderer.updateAll(this@MainActivity, "fc_sion_toggle")','WidgetRenderer.setTestEvent(this@MainActivity, null)'),
 ('WidgetRenderer.updateAll(this@MainActivity, "swiss_toggle")','WidgetRenderer.setTestEvent(this@MainActivity, null)')
])
edit('ui/EventEditorActivity.kt', [
 ('''        priorityInput = edit(event.priority.toString(), "100").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        info.addView(label("Priorité (plus petit = plus prioritaire)")); info.addView(priorityInput)''','''        // Priority is derived automatically from event kind and interval length.'''),
 ('            val newPriority = priorityInput.text.toString().toInt()\n',''),
 ('            event.priority = newPriority\n','')
])
p=r/'app/build.gradle.kts';s=p.read_text()
for old,new in [('versionCode = 45','versionCode = 51'),('versionName = "1.5.27"','versionName = "1.5.33"')]:
 assert s.count(old)==1,(old,s.count(old))
 s=s.replace(old,new,1)
p.write_text(s)
assert 'applicationId = "ch.piiwii.eventb"' in s
print('PASS: WidgEvent 1.5.33 automatic priorities; same application ID')
