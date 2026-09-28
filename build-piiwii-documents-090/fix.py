from pathlib import Path
p=Path('build-piiwii-documents-090/work/src/PiiWii.Documents/MainForm.cs')
s=p.read_text()
s=s.replace('detailMeta.Text=$"🗂  Dossier documentaire\\r\\n"+', 'detailMeta.Text=$"🗂  Dossier documentaire\\r\\n')
p.write_text(s)
