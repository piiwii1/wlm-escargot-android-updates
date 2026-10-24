#!/usr/bin/env python3
"""Fix annual event visibility and idempotent v7 migration."""
from pathlib import Path
import sys
root=Path(sys.argv[1]); app=root/'app/src/main/java/ch/piiwii/events'
def replace(relative, old, new, key):
    p=app/relative; s=p.read_text(encoding='utf-8'); count=s.count(old)
    assert count==1, (relative,key,count)
    p.write_text(s.replace(old,new,1),encoding='utf-8')
replace('data/EventRepository.kt','var version: Int = 6,','var version: Int = 7,','schema new')
replace('data/EventRepository.kt',
    '''        if (configuration.version >= 6) return
        val newDefaults = DefaultEvents.create().filter { it.id in DefaultEvents.newAnnualIds }''',
    '''        // One-time reconciliation of older configurations. Preserve existing events.
        if (configuration.version >= 7) return
        val newDefaults = DefaultEvents.create().filter { it.id in DefaultEvents.newAnnualIds }''',
    'schema guard')
replace('data/EventRepository.kt','''        configuration.version = 6
        saveInternal(configuration)
    }

    private fun saveInternal''','''        configuration.version = 7
        saveInternal(configuration)
    }

    private fun saveInternal''','migration persist')
replace('ui/MainActivity.kt', '''        val density = resources.displayMetrics.density
        fun d(value: Int): Int = UiFactory.dp(this, value)
        val body = UiFactory.vertical(this, 16).apply {''', '''        // Unhide the annual dates on first opening after the upgrade.
        val welcomePrefs = getSharedPreferences("widgevent_ui", Context.MODE_PRIVATE)
        if (!welcomePrefs.getBoolean("annual_events_v7_shown", false)) {
            eventFilter = "Tous"
            welcomePrefs.edit().putBoolean("annual_events_v7_shown", true).apply()
        }
        val density = resources.displayMetrics.density
        fun d(value: Int): Int = UiFactory.dp(this, value)
        val body = UiFactory.vertical(this, 16).apply {''','one-time filter reset')
replace('ui/MainActivity.kt','''        val filters = listOf("Tous", "Automatiques", "Annuels", "Ponctuels")''','''        val filters = listOf("Tous", "Nouveaux", "Automatiques", "Annuels", "Ponctuels")''','quick filter')
replace('ui/MainActivity.kt','''                    "Automatiques" -> auto
                    "Annuels" -> !auto && event.recurrenceRule == RecurrenceRule.YEARLY''','''                    "Nouveaux" -> event.id in DefaultEvents.newAnnualIds
                    "Automatiques" -> auto
                    "Annuels" -> !auto && event.recurrenceRule == RecurrenceRule.YEARLY''','quick filter logic')
replace('ui/MainActivity.kt','''        val automaticEvents = list.filter { it.id == DefaultEvents.FC_SION_ID || it.id == DefaultEvents.SWISS_MATCH_ID }
        val scheduledEvents = list.filterNot { it in automaticEvents }''','''        val annualNewEvents = list.filter { it.id in DefaultEvents.newAnnualIds }
        val automaticEvents = list.filter { it.id == DefaultEvents.FC_SION_ID || it.id == DefaultEvents.SWISS_MATCH_ID }
        val scheduledEvents = list.filterNot { it in automaticEvents || it.id in DefaultEvents.newAnnualIds }''','group partition')
replace('ui/MainActivity.kt','''        addGroup("Affichage automatique", automaticEvents)
        addGroup("Événements programmés", scheduledEvents)''','''        // Nine annual cards first; never duplicate them in the regular event list.
        addGroup("9 nouvelles dates annuelles (${annualNewEvents.size}/9)", annualNewEvents)
        addGroup("Affichage automatique", automaticEvents)
        addGroup("Événements programmés", scheduledEvents)''','group visible')
build=root/'app/build.gradle.kts';v=build.read_text(encoding='utf-8')
for old,new in [('versionCode = 59','versionCode = 60'),('versionName = "1.5.41"','versionName = "1.5.42"')]:
    assert v.count(old)==1,(old,v.count(old));v=v.replace(old,new,1)
assert 'applicationId = "ch.piiwii.eventb"' in v
build.write_text(v,encoding='utf-8')
print('PASS 1.5.42 migrated and visible annual cards')
