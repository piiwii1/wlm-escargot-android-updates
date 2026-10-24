#!/usr/bin/env python3
from pathlib import Path
import sys
r=Path(sys.argv[1]); app=r/'app/src/main/java/ch/piiwii/events'
source=Path('widgevent-1537/BackupManager.kt')
assert source.is_file()
original=(app/'data/BackupManager.kt')
assert original.exists()
original.write_bytes(source.read_bytes())
main=app/'ui/MainActivity.kt'
s=main.read_text(encoding='utf-8')
changes=[
 ('"Exportez un fichier ZIP avec vos événements, vos réglages et vos images personnalisées."',
  '"Les photos importées restent dans WidgEvent même si elles sont supprimées de la galerie. Exportez vos événements, paramètres et photos dans un seul ZIP à conserver sur un autre appareil ou dans le cloud."'),
 ('"Choisissez une archive précédemment exportée. Vérifiez son contenu avant de remplacer la configuration actuelle."',
  '"Sur votre nouveau téléphone, choisissez le ZIP pour récupérer les événements ET leurs photos. Le contenu est vérifié avant toute modification."'),
 ('"Exporter une sauvegarde"', '"Sauvegarder tout (photos incluses)"'),
 ('"Importer une sauvegarde"', '"Restaurer une sauvegarde ZIP"'),
 ('"live-event-widget-sauvegarde.zip"', '"WidgEvent-sauvegarde-complete.zip"'),
 ('"¤{preview.eventCount} événements et ¤{preview.imageCount} images seront importés. La configuration actuelle sera remplacée."',
  '"¤{preview.eventCount} événements et ¤{preview.imageCount} images intégrées seront restaurés. Vos événements actuels seront remplacés après validation."'),
 ('"Sauvegarde exportée"', '"Sauvegarde complète créée : conservez le ZIP ailleurs que sur ce téléphone"')
]
for a,b in changes:
 a=a.replace('¤','$');b=b.replace('¤','$')
 assert s.count(a)==1,(a,s.count(a))
 s=s.replace(a,b,1)
main.write_text(s,encoding='utf-8')
build=r/'app/build.gradle.kts'
v=build.read_text()
for a,b in [('versionCode = 54','versionCode = 55'),('versionName = "1.5.36"','versionName = "1.5.37"')]:
 assert v.count(a)==1,(a,v.count(a))
 v=v.replace(a,b,1)
assert 'applicationId = "ch.piiwii.eventb"' in v
build.write_text(v)
print("PASS WidgEvent 1.5.37 full offline photos backup")
