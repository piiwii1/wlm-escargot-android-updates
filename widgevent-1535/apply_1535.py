#!/usr/bin/env python3
from pathlib import Path
import sys
r=Path(sys.argv[1])
p=r/'app/src/main/java/ch/piiwii/events/ui/MainActivity.kt'
s=p.read_text(encoding='utf-8')
def block(start,end,update):
 global s
 assert s.count(start)==1,(start,s.count(start))
 a=s.index(start);b=s.index(end,a)
 s=s[:a]+update+s[b:]
def replace(old,new):
 global s
 assert s.count(old)==1,(old[:80],s.count(old))
 s=s.replace(old,new,1)
block('            listed.forEach { event ->\n','        }\n        // Vue chronologique intelligente','''            listed.forEachIndexed { index, event ->
                val card = UiFactory.horizontal(this, 12).apply {
                    setPadding(d(12), d(12), d(13), d(12))
                    background = UiFactory.rounded(Color.WHITE, 19f, this@MainActivity, Color.rgb(230, 235, 244))
                    minimumHeight = d(91)
                    isClickable = true
                    isFocusable = true
                    contentDescription = "¤{event.name} : ouvrir les détails"
                    setOnClickListener { openEditor(event.id) }
                    setOnLongClickListener { showDayDetails(selected, listed); true }
                }
                card.addView(calendarEventArtwork(event, 66), LinearLayout.LayoutParams(d(66), d(66)))
                val details = UiFactory.vertical(this, 0).apply { setPadding(0, 0, 0, 0) }
                details.addView(UiFactory.title(this, event.name, 16.5f).apply {
                    setPadding(0, 0, 0, d(3))
                    maxLines = 2
                })
                details.addView(UiFactory.muted(this, calendarEventTime(event, selected, engine, zone)).apply {
                    setPadding(0, 0, 0, d(2)); maxLines = 2
                })
                if (listed.size > 1 && index == 0) {
                    details.addView(UiFactory.body(this, "● Affiché en priorité", 11.5f).apply {
                        setTextColor(UiFactory.BLUE)
                    })
                }
                card.addView(details, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                body.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = d(9) })
            }
''')
block('        upcoming.forEach { entry ->\n','        return page(body)\n','''        upcoming.forEach { entry ->
            val card = UiFactory.vertical(this, 0).apply {
                background = UiFactory.rounded(Color.WHITE, 20f, this@MainActivity, Color.rgb(230, 235, 244))
                setPadding(d(13), d(13), d(13), d(12))
                isClickable = true
                isFocusable = true
                contentDescription = "¤{entry.event.name}, ¤{entry.countdown}, ouvrir l'événement"
                setOnClickListener { openEditor(entry.event.id) }
            }
            val header = UiFactory.horizontal(this, 12)
            header.addView(calendarEventArtwork(entry.event, 74), LinearLayout.LayoutParams(d(74), d(74)))
            val headerText = UiFactory.vertical(this, 0).apply { setPadding(0, 0, 0, 0) }
            headerText.addView(UiFactory.title(this, entry.event.name, 17f).apply {
                setPadding(0, 0, 0, d(5)); maxLines = 2
            })
            headerText.addView(UiFactory.chip(this, entry.countdown, entry.activeNow))
            header.addView(headerText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(header)
            card.addView(UiFactory.body(this, entry.periodLabel, 13f).apply {
                setPadding(0, d(9), 0, d(2))
            })
            card.addView(UiFactory.muted(this, "Durée : ¤{entry.durationLabel}").apply {
                setPadding(0, 0, 0, d(9))
            })
            val shown = config.events.firstOrNull { it.name == entry.visibleImage }
            val afterwards = config.events.firstOrNull { it.name == entry.imageAfter }
            card.addView(calendarImageSummary("IMAGE AFFICHÉE", entry.visibleImage, shown))
            card.addView(calendarImageSummary("ENSUITE", entry.imageAfter, afterwards),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = d(5) })
            if (entry.overlaps.isNotEmpty()) {
                card.addView(UiFactory.muted(this,
                    "Superposition automatique : ¤{entry.overlaps.joinToString()}").apply {
                    setPadding(0, d(9), 0, 0)
                    maxLines = 3
                })
            }
            body.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = d(11) })
        }
''')
replace('''        return if (interval.start.isBefore(from)) {
            "Déjà actif à 00:00"
        } else {
            "Actif à partir de " + interval.start.format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH))
        }''','''        return when {
            interval.start.isBefore(from) -> "Déjà actif depuis minuit"
            interval.start.toLocalTime() == java.time.LocalTime.MIDNIGHT -> "Actif dès minuit"
            else -> "Actif à partir de " + interval.start.format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH))
        }''')
replace('    /** Résumé horaire issu des périodes résolues, sans horaires inventés. */','''    /** Prefer existing imported photos; otherwise use bundled artwork or a suitable pictogram. */
    private fun calendarEventArtwork(event: EventDefinition?, sizeDp: Int): View {
        val d = { n: Int -> UiFactory.dp(this, n) }
        val frame = FrameLayout(this).apply {
            background = UiFactory.rounded(Color.rgb(240, 245, 252), 13f, this@MainActivity)
            clipToOutline = true
        }
        val eventImage = event?.let { evt ->
            listOf(evt.imagePath, evt.originalImagePath).firstOrNull { path ->
                path.isNotBlank() && File(path).isFile
            }
        }
        val photo = eventImage?.let { imageStorage.loadScaled(it, d(sizeDp * 2), d(sizeDp * 2)) }
        val iconRes = event?.let { ThemeIconCatalog.iconFor(it) } ?: 0
        if (photo != null || iconRes != 0) {
            val img = ImageView(this).apply {
                scaleType = if (photo != null) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                if (photo != null) setImageBitmap(photo) else setImageResource(iconRes)
                contentDescription = event?.name ?: "Illustration"
                if (photo == null) setPadding(d(3), d(3), d(3), d(3))
            }
            frame.addView(img, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            val name = event?.name.orEmpty().lowercase(Locale.FRENCH)
            val symbol = when {
                "match" in name || "sion" in name || "suisse" in name -> "⚽"
                "hiver" in name || "noël" in name -> "❄️"
                "printemps" in name || "pâques" in name -> "🌷"
                "été" in name || "soleil" in name -> "☀️"
                "automne" in name -> "🍂"
                "carnaval" in name -> "🎭"
                "concert" in name || "festival" in name || "musique" in name -> "🎵"
                "fête" in name || "anniversaire" in name -> "🎉"
                else -> "📅"
            }
            frame.addView(TextView(this).apply {
                text = symbol
                textSize = if (sizeDp > 40) 28f else 17f
                gravity = Gravity.CENTER
                contentDescription = event?.name ?: "Événement"
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))
        }
        return frame
    }

    private fun calendarImageSummary(label: String, imageName: String, event: EventDefinition?): View {
        val d = { n: Int -> UiFactory.dp(this, n) }
        return UiFactory.horizontal(this, 9).apply {
            setPadding(d(8), d(5), d(8), d(5))
            background = UiFactory.rounded(Color.rgb(246, 249, 254), 11f, this@MainActivity)
            addView(calendarEventArtwork(event, 32), LinearLayout.LayoutParams(d(32), d(32)))
            val textGroup = UiFactory.vertical(this@MainActivity, 0).apply { setPadding(0, 0, 0, 0) }
            textGroup.addView(UiFactory.body(this@MainActivity, label, 10.5f).apply {
                setTextColor(UiFactory.MUTED); setPadding(0, 0, 0, 0)
            })
            textGroup.addView(UiFactory.body(this@MainActivity, imageName, 13.5f).apply {
                maxLines = 2
                setPadding(0, 0, 0, 0)
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
            addView(textGroup, LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    /** Résumé horaire issu des périodes résolues, sans horaires inventés. */''')
s=s.replace('¤','$')
p.write_text(s,encoding='utf-8')
b=r/'app/build.gradle.kts';v=b.read_text()
for old,new in [('versionCode = 52','versionCode = 53'),('versionName = "1.5.34"','versionName = "1.5.35"')]:
 assert v.count(old)==1,(old,v.count(old));v=v.replace(old,new,1)
b.write_text(v)
assert 'applicationId = "ch.piiwii.eventb"' in v
print('PASS 1.5.35 calendar artwork and visual cards')
