from pathlib import Path
import re

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

pat=re.compile(r'    private void shareConvoy\(\)\{.*?\n\s*private void copyConvoyCode\(\)', re.S)
share='''    private void shareConvoy(){String code=prefs.get("code","");String msg="Rejoins mon convoi « "+prefs.get("convoyName","Mode Convoi")+" »\\nOuvre Mode Convoi puis scanne mon QR, ou saisis le code : "+code+"\\nLien : modeconvoi://join/"+code;Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,msg);startActivity(Intent.createChooser(i,"Partager le convoi"));}\n\n    private void copyConvoyCode()'''
s,n=pat.subn(lambda m: share,s,count=1)
if n!=1:
    raise SystemExit('0.3.6 share fix target not found')

p.write_text(s)
print('0.3.6 share string fix applied')
