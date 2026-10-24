#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1]); app=root/'app/src/main/java/ch/piiwii/events'
def change(rel,pairs):
 p=app/rel;s=p.read_text(encoding='utf-8')
 for old,new in pairs:
  assert s.count(old)==1,(rel,old[:85],s.count(old))
  s=s.replace(old,new,1)
 p.write_text(s,encoding='utf-8')
change('data/ImageStorage.kt',[
 ('''        if (file.exists() && !overwrite && (builtinRes == 0 || installedThemeMarker.exists())) return file''','''        val iconMarker = File(imageDirectory, ".emoji-" + event.id.replace(Regex("[^A-Za-z0-9_-]"), "_"))
        val iconState = event.iconKey + "|" + event.name
        if (file.exists() && !overwrite && (builtinRes == 0 || installedThemeMarker.exists()) &&
            (event.iconKey.isBlank() || (iconMarker.isFile && iconMarker.readText() == iconState)))
            return file'''),
 ('''        canvas.drawColor(Color.TRANSPARENT)
        val border = Paint''','''        canvas.drawColor(Color.TRANSPARENT)
        if (event.iconKey.isNotBlank()) {
            val palette = listOf(Color.rgb(48, 109, 230), Color.rgb(219, 81, 108),
                Color.rgb(40, 157, 139), Color.rgb(145, 92, 202), Color.rgb(225, 134, 50))
            val accent = palette[(event.id.hashCode() and Int.MAX_VALUE) % palette.size]
            val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(244, 247, 253) }
            canvas.drawRoundRect(20f, 20f, 880f, 880f, 90f, 90f, background)
            val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(115f, 115f, 785f, 685f, 75f, 75f, tile)
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent; style = Paint.Style.STROKE; strokeWidth = 12f
            }
            canvas.drawRoundRect(115f, 115f, 785f, 685f, 75f, 75f, line)
            val emoji = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; textSize = 295f; textAlign = Paint.Align.CENTER
            }
            canvas.drawText(event.iconKey, 450f, 490f, emoji)
            val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(38, 53, 76); textAlign = Paint.Align.CENTER
                textSize = 42f
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            splitLabel(event.name, 28).take(2).forEachIndexed { index, caption ->
                canvas.drawText(caption, 450f, 750f + index * 55f, title)
            }
            FileOutputStream(file).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            iconMarker.writeText(iconState)
            return file
        }
        val border = Paint''')
])
change('ui/EventEditorActivity.kt',[
 ('import android.app.DatePickerDialog','import android.app.DatePickerDialog\nimport android.app.AlertDialog'),
 ('    private lateinit var imageStatus: TextView','    private lateinit var imageStatus: TextView\n    private lateinit var iconInput: EditText'),
 ('''            custom = true
        )
    }

    private fun buildUi()''','''            custom = true,
            iconKey = "📅"
        )
    }

    private fun buildUi()'''),
 ('"Ajouter un événement" else "Modifier l’événement"','"Créer un événement personnalisé" else "Modifier l’événement"'),
 ('''        imageCard.addView(UiFactory.title(this, "Image", 18f))''','''        imageCard.addView(UiFactory.title(this, "Icône et image", 18f))
        imageCard.addView(UiFactory.muted(this,
            "Icône libre pour l’événement ; photo indépendante affichée sur le widget."))
        iconInput = edit(event.iconKey, "Emoji de l’icône")
        iconInput.setSingleLine(true)
        val iconRow = UiFactory.horizontal(this, 8)
        iconRow.addView(iconInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        iconRow.addView(UiFactory.secondaryButton(this, "Choisir").apply {
            setOnClickListener {
                val choices = listOf("📅", "🎂", "❤️", "🎧", "🐟", "🎵", "🚗",
                    "🚘", "🎭", "🚀", "🎉", "🎃", "⚽", "🎄", "❄️",
                    "🐰", "🧀", "🎸", "🎮", "🎬", "🍀", "🏆", "📷", "🎨", "🎁")
                AlertDialog.Builder(this@EventEditorActivity)
                    .setTitle("Choisir l’icône")
                    .setItems(choices.toTypedArray()) { _, index ->
                        iconInput.setText(choices[index])
                        event.iconKey = choices[index]
                        refreshPreview()
                    }.show()
            }
        })
        imageCard.addView(label("Icône personnalisable"))
        imageCard.addView(iconRow)
        imageCard.addView(UiFactory.muted(this, "Tu peux aussi saisir ton propre emoji."))
        imageCard.addView(label("Photo affichée sur le widget"))'''),
 ('''            event.name = newName
            event.enabled = enabledSwitch.isChecked''','''            event.name = newName
            event.iconKey = iconInput.text.toString().trim().take(12)
            event.enabled = enabledSwitch.isChecked'''),
 ('''        imageStorage.deleteUserImage(event)
        event.imagePath = ""''','''        // Removing a photo is committed only on Save; Cancel must preserve it.
        event.imagePath = ""'''),
 ('''            "Aucune photo personnalisée — ''','''            "Illustration par défaut : personnalise l’icône ou importe une photo. " + ''')
])
change('ui/MainActivity.kt',[
 ('''        addGroup("Événements programmés", scheduledEvents)
        body.addView(emptyMessage)''','''        addGroup("Événements programmés", scheduledEvents)
        body.addView(UiFactory.secondaryButton(this, "+ Créer une date personnalisée").apply {
            contentDescription = "Créer une date avec nom, icône et photo personnalisés"
            setOnClickListener { openEditor(null) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = d(12) })
        body.addView(emptyMessage)'''),
 ('''            val symbol = when {
                "match" in name''','''            val symbol = event?.iconKey?.takeIf { it.isNotBlank() } ?: when {
                "match" in name'''),
 ('''                    text = event.name.firstOrNull()?.uppercaseChar()?.toString() ?: "•"''','''                    text = event.iconKey.ifBlank {
                        event.name.firstOrNull()?.uppercaseChar()?.toString() ?: "•"
                    }''')
])
print('PASS icons, image fallback artwork, icon picker, personal event button')
