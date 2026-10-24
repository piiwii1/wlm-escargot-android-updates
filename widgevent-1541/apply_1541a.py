#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1]); app=root/'app/src/main/java/ch/piiwii/events'
def change(rel,pairs):
 p=app/rel;s=p.read_text(encoding='utf-8')
 for old,new in pairs:
  assert s.count(old)==1,(rel,old[:90],s.count(old))
  s=s.replace(old,new,1)
 p.write_text(s,encoding='utf-8')

change('model/EventDefinition.kt',[
 ('    var imageSourceName: String = "",','    var imageSourceName: String = "",\n    var iconKey: String = "",'),
 ('        put("imageSourceName", imageSourceName)','        put("imageSourceName", imageSourceName)\n        put("iconKey", iconKey)'),
 ('                imageSourceName = json.optString("imageSourceName", ""),','                imageSourceName = json.optString("imageSourceName", ""),\n                iconKey = json.optString("iconKey", ""),')
])
change('data/DefaultEvents.kt',[
 ('    const val WORLD_CUP_ID = "world-cup"','''    const val WORLD_CUP_ID = "world-cup"
    const val VALENTINE_ID = "saint-valentin"
    const val DJ_DAY_ID = "journee-mondiale-dj"
    const val BIRTHDAY_ID = "anniversaire-personnel-23-mars"
    const val APRIL_FOOLS_ID = "poisson-avril"
    const val MUSIC_DAY_ID = "fete-musique"
    const val VW_CLUB_ID = "anniversaire-club-vw-suisse"
    const val BTTF_ID = "back-to-the-future-day"
    const val V_VENDETTA_ID = "v-for-vendetta-day"
    const val DOCTOR_WHO_ID = "doctor-who-day"
    val newAnnualIds = setOf(VALENTINE_ID, DJ_DAY_ID, BIRTHDAY_ID,
        APRIL_FOOLS_ID, MUSIC_DAY_ID, VW_CLUB_ID, BTTF_ID, V_VENDETTA_ID, DOCTOR_WHO_ID)'''),
 ('        )\n    }\n\n    fun defaultPriority','''        ).apply { addAll(newAnnualEvents()) }
    }

    private fun newAnnualEvents(): List<EventDefinition> {
        fun annual(id: String, title: String, month: Int, day: Int, icon: String): EventDefinition =
            EventDefinition(id = id, name = title, enabled = true, priority = 420,
                recurrenceRule = RecurrenceRule.YEARLY,
                periods = mutableListOf(EventPeriod(label = "%02d.%02d".format(day, month),
                    startMonth = month, startDay = day, endMonth = month, endDay = day,
                    startMinute = 0, endMinute = 1439)),
                iconKey = icon, defaultFileName = "event_" + id + ".png",
                predefinedKey = id, custom = false)
        return listOf(
            annual(VALENTINE_ID, "Saint-Valentin", 2, 14, "❤️"),
            annual(DJ_DAY_ID, "Journée mondiale du DJ", 3, 9, "🎧"),
            annual(BIRTHDAY_ID, "Mon anniversaire", 3, 23, "🎂"),
            annual(APRIL_FOOLS_ID, "Poisson d’avril", 4, 1, "🐟"),
            annual(MUSIC_DAY_ID, "Fête de la musique", 6, 21, "🎵"),
            annual(VW_CLUB_ID, "Anniversaire du Club VW Suisse", 8, 15, "🚗"),
            annual(BTTF_ID, "Back to the Future Day", 10, 21, "🚘"),
            annual(V_VENDETTA_ID, "V for Vendetta", 11, 5, "🎭"),
            annual(DOCTOR_WHO_ID, "Doctor Who Day", 11, 23, "🚀")
        )
    }

    fun defaultPriority''')
])
change('data/EventRepository.kt',[
 ('        var version: Int = 5,','        var version: Int = 6,'),
 ('.also { config -> ensureDefaults(config) }','.also { config -> ensureDefaults(config); migrateAnnualEvents(config) }'),
 ('        ensureDefaults(configuration)\n        // Only the edited event updates its timestamp.','        ensureDefaults(configuration)\n        migrateAnnualEvents(configuration)\n        // Only the edited event updates its timestamp.'),
 ('    private fun saveInternal(configuration: Configuration) {','''    /** Add new events once only, never overwrite user changes or photos. */
    private fun migrateAnnualEvents(configuration: Configuration) {
        if (configuration.version >= 6) return
        fun normalized(value: String): String = java.text.Normalizer
            .normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ").trim()
        val ids = configuration.events.map { it.id }.toMutableSet()
        val names = configuration.events.map { normalized(it.name) }.toMutableSet()
        for (fresh in DefaultEvents.create().filter { it.id in DefaultEvents.newAnnualIds }) {
            if (ids.add(fresh.id) && names.add(normalized(fresh.name))) {
                configuration.events.add(fresh)
            }
        }
        configuration.version = 6
        saveInternal(configuration)
    }

    private fun saveInternal(configuration: Configuration) {''')
])
b=root/'app/build.gradle.kts';v=b.read_text(encoding='utf-8')
for old,new in [('versionCode = 58','versionCode = 59'),('versionName = "1.5.40"','versionName = "1.5.41"')]:
 assert v.count(old)==1,(old,v.count(old));v=v.replace(old,new,1)
assert 'applicationId = "ch.piiwii.eventb"' in v
b.write_text(v,encoding='utf-8')
print('PASS nine annual events, JSON icon support, non-destructive v6 migration')
