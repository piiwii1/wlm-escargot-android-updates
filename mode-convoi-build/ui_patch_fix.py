from pathlib import Path
import re
p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()
fixed='String[][] statuses={{"🛑 Arrêt","stop"},{"⛽ Essence","fuel"},{"☕ Pause","pause"},{"🚻 WC","wc"},{"⚠ Problème","problem"},{"🚗 Voiture","car_problem"},{"↗ Rejoins","joining"},{"👍 Tout va bien","ok"}};'
s,n=re.subn(r'String\[\]\[\] statuses=\{\{.*?\}\};',fixed,s,flags=re.S,count=1)
if n!=1:
    raise SystemExit('statuses block not found exactly once')
p.write_text(s)
