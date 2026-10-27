from pathlib import Path
from PIL import Image, ImageOps
import base64, io, re, sys

root=Path(sys.argv[1])
blob=Path(sys.argv[2]).read_text()
atlas=Image.open(io.BytesIO(base64.b64decode(''.join(blob.split())))).convert('RGBA')
assert atlas.size==(2172,724),atlas.size
rows=[
['music','music_spotify','music_equalizer','music_vinyl','music_playing','music_pause','music_play','music_stop','music_shuffle','music_repeat','music_previous','music_next'],
['output_phone','output_phone_muted','output_earbuds_wired','output_earbuds_usb','output_earbuds_airpods','output_earbuds_galaxy','output_headphones','output_headphones_gaming','output_portable_speaker','output_speaker','output_speaker_sony','output_speaker_other'],
['output_car','output_car_stereo','output_helmet','output_tv','output_cast','output_console','output_computer','output_bluetooth','output_usb','output_aux','output_hifi','output_mixer']]
aliases={'music_pulse':'music','music_wave':'music_equalizer','music_orbit':'music_playing',
'output_earbuds':'output_earbuds_airpods','output_headphones_wired':'output_headphones_gaming',
'output_unknown':'output_mixer','phone':'output_phone','missed':'output_phone_muted'}
labels=[
['Note musicale','Spotify','Égaliseur','Vinyle','Lecture en cours','Pause','Lecture','Arrêt','Aléatoire','Répétition','Précédent','Suivant'],
['Haut-parleur du téléphone','Téléphone silencieux','Écouteurs filaires','Écouteurs USB-C','AirPods','Galaxy Buds','Casque Bluetooth','Casque gaming','Enceinte JBL','Enceinte Bose','Enceinte Sony','Autre enceinte Bluetooth'],
['Voiture','Autoradio','Casque moto','Téléviseur','Chromecast / Cast','Console','Ordinateur','Bluetooth','USB','Prise Jack / AUX','Hi-Fi','Mixeur audio']]
folder=root/'app/src/main/res/drawable-nodpi'
folder.mkdir(exist_ok=True)
for ri,row in enumerate(rows):
    for ci,key in enumerate(row):
        x0=round(ci*atlas.width/12)+8;x1=round((ci+1)*atlas.width/12)-8
        y0=[12,235,471][ri];y1=[174,399,634][ri]
        crop=atlas.crop((x0,y0,x1,y1))
        bbox=crop.getchannel('A').point(lambda a:255 if a>35 else 0).getbbox()
        if bbox:crop=crop.crop(bbox)
        image=Image.new('RGBA',(320,320),(0,0,0,0))
        fit=ImageOps.contain(crop,(296,296),Image.Resampling.LANCZOS)
        image.alpha_composite(fit,((320-fit.width)//2,(320-fit.height)//2))
        image.save(folder/('pic_'+key+'.png'),compress_level=6)
for alias,name in aliases.items():
    (folder/('pic_'+alias+'.png')).write_bytes((folder/('pic_'+name+'.png')).read_bytes())
allkeys={key for row in rows for key in row}|set(aliases)
draw=root/'app/src/main/res/drawable'
for key in allkeys:
    p=draw/('pic_'+key+'.xml')
    if p.exists():p.unlink()
java=root/'app/src/main/java/ch/piiwii/notificationwidget'
gf=java/'GraphicIcons.java';s=gf.read_text()
known=set(m.group(1) for m in re.finditer(r'case "([a-z0-9_]+)":\s*return R\.drawable\.pic_\w+;',s))
extra=''.join('      case "'+k+'":return R.drawable.pic_'+k+';\n' for k in sorted(allkeys-known))
assert s.count('      default: return 0;')==1
s=s.replace('      default: return 0;',extra+'      default: return 0;',1)
assert 'if(!colorful(key))d.setTint(tint);' in s
s=s.replace('if(!colorful(key))d.setTint(tint);',
'''if(!colorful(key)&&!key.startsWith("music")&&!key.startsWith("output")
           &&!"phone".equals(key)&&!"missed".equals(key))d.setTint(tint);''',1)
gf.write_text(s)
main=java/'MainActivity.java';m=main.read_text()
a=m.index('  private static final String[][] MUSIC_GRAPHICS={')
b=m.index('  private void openAudioIconEditor(',a)
entries='\n'.join('    '+','.join('{"'+key+'","'+labels[ri][i]+'"}' for i,key in enumerate(row))+(',' if ri<2 else '') for ri,row in enumerate(rows))
m=m[:a]+'  private static final String[][] MUSIC_GRAPHICS={\n'+entries+'\n  };\n'+m[b:]
main.write_text(m)
build=root/'app/build.gradle';s=build.read_text()
old="versionCode 39; versionName '1.0.38'"
assert s.count(old)==1
build.write_text(s.replace(old,"versionCode 40; versionName '1.0.39'"))
assert len(list(folder.glob('pic_*.png')))==44
assert all(not (draw/('pic_'+k+'.xml')).exists() for k in allkeys)
assert all((folder/('pic_'+k+'.png')).is_file() for k in allkeys)
assert 'music_unified_icon' in (java/'IconRenderer.java').read_text()
print('PASS: 36 PHOTO ICONS + 8 LEGACY ALIASES, 36 GALLERY OPTIONS, v1.0.39')
