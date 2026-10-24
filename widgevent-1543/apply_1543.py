#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1]); app=root/'app/src/main/java/ch/piiwii/events'
def patch(relative, old, new, label):
    p=app/relative; s=p.read_text(encoding='utf-8')
    assert s.count(old)==1,(relative,label,s.count(old))
    p.write_text(s.replace(old,new,1),encoding='utf-8')
patch('ui/MainActivity.kt', '''            val repeat = if (event.recurrenceRule == RecurrenceRule.YEARLY) " · Annuel" else ""
            if (period.startDate.isNotBlank()) {
                val first = formatStoredDate(period.startDate)
                val last = if (period.endDate.isNotBlank() && period.endDate != period.startDate)
                    " – ${formatStoredDate(period.endDate)}" else ""
                return first + last + repeat
            }
            return if (event.recurrenceRule == RecurrenceRule.YEARLY && period.startMonth in 1..12) {
                val startLabel = "%02d.%02d".format(Locale.ROOT, period.startDay, period.startMonth)
                val endLabel = "%02d.%02d".format(Locale.ROOT, period.endDay, period.endMonth)
                startLabel + (if (startLabel != endLabel) " – $endLabel" else "") + repeat
            } else periodSummary(event).take(80)''', '''            // Annual dates come from month/day, never from the unused 2026-01-01 default.
            if (event.recurrenceRule == RecurrenceRule.YEARLY && period.startMonth in 1..12) {
                val first = formatAnnualDate(period.startMonth, period.startDay)
                val last = formatAnnualDate(period.endMonth, period.endDay)
                return first + (if (first != last) " – $last" else "") + " · Chaque année"
            }
            if (period.startDate.isNotBlank() && (event.recurrenceRule == RecurrenceRule.ONCE ||
                event.recurrenceRule == RecurrenceRule.MULTIPLE_DATES)) {
                val first = formatStoredDate(period.startDate)
                val last = if (period.endDate.isNotBlank() && period.endDate != period.startDate)
                    " – ${formatStoredDate(period.endDate)}" else ""
                return first + last
            }
            return periodSummary(event).take(80)''', 'date display')
patch('ui/MainActivity.kt', '''        val list = repository.load().events.filter { it.id != DefaultEvents.NEUTRAL_ID }.sortedBy { it.priority }
        val annualNewEvents = list.filter { it.id in DefaultEvents.newAnnualIds }
        val automaticEvents = list.filter { it.id == DefaultEvents.FC_SION_ID || it.id == DefaultEvents.SWISS_MATCH_ID }
        val scheduledEvents = list.filterNot { it in automaticEvents || it.id in DefaultEvents.newAnnualIds }''', '''        // Merge all fixed-date events (including custom and the nine new ones).
        // Sort by the actual next occurrence; undated entries go after dated ones.
        val list = repository.load().events.filter { it.id != DefaultEvents.NEUTRAL_ID }
        val automaticEvents = list.filter {
            it.id == DefaultEvents.FC_SION_ID || it.id == DefaultEvents.SWISS_MATCH_ID
        }
        val datedEvents = list.filterNot { it in automaticEvents }
        val zone = repository.resolvedZoneId()
        val now = ZonedDateTime.now(zone)
        val engine = DateEventEngine()
        val nextOccurrenceById = datedEvents.associate { event ->
            val next = engine.intervalsForEvent(event.copy(enabled = true), zone,
                now.minusDays(2), now.plusYears(2))
                .filter { it.endExclusive.isAfter(now) }
                .minByOrNull { it.start.toInstant() }
            event.id to (next?.start?.toInstant() ?: java.time.Instant.MAX)
        }
        val scheduledEvents = datedEvents.sortedWith(
            compareBy<EventDefinition> { nextOccurrenceById[it.id] ?: java.time.Instant.MAX }
                .thenBy { it.name.lowercase(Locale.FRENCH) }
        )''', 'chronological sort')
patch('ui/MainActivity.kt', '''        // Nine annual cards first; never duplicate them in the regular event list.
        addGroup("9 nouvelles dates annuelles (${annualNewEvents.size}/9)", annualNewEvents)
        addGroup("Affichage automatique", automaticEvents)
        addGroup("Événements programmés", scheduledEvents)''', '''        addGroup("Événements par date", scheduledEvents)
        addGroup("Matchs automatiques (sans date fixe)", automaticEvents)''', 'remove new subgroup')
patch('data/DefaultEvents.kt', '''                    startMonth = month, startDay = day, endMonth = month, endDay = day,
                    startMinute = 0, endMinute = 1439)),''', '''                    startDate = "", endDate = "",
                    startMonth = month, startDay = day, endMonth = month, endDay = day,
                    startMinute = 0, endMinute = 1439)),''', 'new annual defaults')
patch('data/EventRepository.kt','var version: Int = 7,','var version: Int = 8,','schema 8')
patch('data/EventRepository.kt', '''                }.also { config -> ensureDefaults(config); migrateAnnualEvents(config) }''', '''                }.also { config ->
                    ensureDefaults(config)
                    migrateAnnualEvents(config)
                    cleanAnnualDatePlaceholders(config)
                }''', 'migration on load')
patch('data/EventRepository.kt','''        ensureDefaults(configuration)
        migrateAnnualEvents(configuration)
        // Only the edited event updates its timestamp.''','''        ensureDefaults(configuration)
        migrateAnnualEvents(configuration)
        cleanAnnualDatePlaceholders(configuration)
        // Only the edited event updates its timestamp.''','migration on save')
patch('data/EventRepository.kt', '''    private fun saveInternal(configuration: Configuration) {''', '''    /** One-time safe cleanup: the historical Jan 1 string is unused for annual dates.
     * Preserve actual month/day, the user's own dates, images and deleted events.
     * Do not rerun the earlier event-addition migration after schema v7.
     */
    private fun cleanAnnualDatePlaceholders(configuration: Configuration) {
        if (configuration.version >= 8) return
        configuration.events.filter { it.id in DefaultEvents.newAnnualIds &&
            it.recurrenceRule == ch.piiwii.events.model.RecurrenceRule.YEARLY
        }.forEach { event ->
            event.periods.forEach { period ->
                if (period.startDate == "2026-01-01") period.startDate = ""
                if (period.endDate == "2026-01-01") period.endDate = ""
            }
        }
        configuration.version = 8
        saveInternal(configuration)
    }

    private fun saveInternal(configuration: Configuration) {''', 'safe cleanup')
b=root/'app/build.gradle.kts'; v=b.read_text(encoding='utf-8')
for old,new in [('versionCode = 60','versionCode = 61'),('versionName = "1.5.42"','versionName = "1.5.43"')]:
    assert v.count(old)==1,(old,v.count(old));v=v.replace(old,new,1)
assert 'applicationId = "ch.piiwii.eventb"' in v
b.write_text(v,encoding='utf-8')
expected=[('VALENTINE_ID',2,14),('DJ_DAY_ID',3,9),('BIRTHDAY_ID',3,23),('APRIL_FOOLS_ID',4,1),('MUSIC_DAY_ID',6,21),('VW_CLUB_ID',8,15),('BTTF_ID',10,21),('V_VENDETTA_ID',11,5),('DOCTOR_WHO_ID',11,23)]
txt=(app/'data/DefaultEvents.kt').read_text()
for sym,m,d in expected:
    assert any(t.strip().startswith('annual('+sym+', ') and (', '+str(m)+', '+str(d)+', ') in t for t in txt.splitlines()),(sym,m,d)
print('PASS: nine correct annual dates; chronological sorting; safe v8 migration')
