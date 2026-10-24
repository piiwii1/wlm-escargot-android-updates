#!/usr/bin/env python3
"""WidgEvent 1.5.38: show the actual upcoming widget image in Aujourd'hui."""
from pathlib import Path
import sys
root=Path(sys.argv[1])
main=root/'app/src/main/java/ch/piiwii/events/ui/MainActivity.kt'
s=main.read_text(encoding='utf-8')
old='''        val dateBubble = UiFactory.title(this, state.nextChange?.dayOfMonth?.toString() ?: "—", 19f).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(225, 121, 37))
            background = UiFactory.rounded(Color.rgb(255, 239, 226), 50f, this@MainActivity)
            setPadding(0, 0, 0, 0)
        }
        nextRow.addView(dateBubble, LinearLayout.LayoutParams(UiFactory.dp(this, 51), UiFactory.dp(this, 51)))
'''
new='''        // Preview the image chosen by the real engine just AFTER the next boundary.
        // This also covers the end of a match, when a season or festival takes over.
        val upcomingImageEvent = state.nextChange?.let { boundary ->
            runCatching {
                DateEventEngine().resolve(config, boundary.plusSeconds(1)).selectedEvent
            }.getOrNull()
        }
        val upcomingThumbnail = calendarEventArtwork(upcomingImageEvent, 51, fitArtwork = true)
        nextRow.addView(upcomingThumbnail, LinearLayout.LayoutParams(
            UiFactory.dp(this, 51), UiFactory.dp(this, 51)))
'''
assert s.count(old)==1,('dateBubble',s.count(old));s=s.replace(old,new,1)
old='''        nextInfo.addView(UiFactory.title(this, state.nextEventToStart?.name ?: "Prochaine transition", 18f))'''
new='''        nextInfo.addView(UiFactory.title(this, upcomingImageEvent?.name
            ?: state.nextEventToStart?.name ?: "Aucun changement prévu", 18f))'''
assert s.count(old)==1,('label',s.count(old));s=s.replace(old,new,1)
old='''    private fun calendarEventArtwork(event: EventDefinition?, sizeDp: Int): View {'''
new='''    private fun calendarEventArtwork(event: EventDefinition?, sizeDp: Int,
                                     fitArtwork: Boolean = false): View {'''
assert s.count(old)==1,('artwork-function',s.count(old));s=s.replace(old,new,1)
old='''                scaleType = if (photo != null) ImageView.ScaleType.CENTER_CROP
                    else ImageView.ScaleType.FIT_CENTER'''
new='''                scaleType = if (photo != null && !fitArtwork) ImageView.ScaleType.CENTER_CROP
                    else ImageView.ScaleType.FIT_CENTER'''
assert s.count(old)==1,('scaleType',s.count(old));s=s.replace(old,new,1)
main.write_text(s,encoding='utf-8')
build=root/'app/build.gradle.kts';v=build.read_text(encoding='utf-8')
for a,b in [('versionCode = 55','versionCode = 56'),('versionName = "1.5.37"','versionName = "1.5.38"')]:
 assert v.count(a)==1,(a,v.count(a));v=v.replace(a,b,1)
assert 'applicationId = "ch.piiwii.eventb"' in v
build.write_text(v,encoding='utf-8')
print('PASS WidgEvent 1.5.38 next-change actual image, fallback artwork, same package')
