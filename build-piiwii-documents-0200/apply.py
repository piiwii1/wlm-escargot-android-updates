from pathlib import Path
root=Path('build-piiwii-documents-0200/work')

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/MainForm.cs','src/PiiWii.Documents/Services/BridgeService.cs','src/PiiWii.Documents/Services/AssistantExchangeService.cs','src/PiiWii.Documents/Services/BackupService.cs','src/PiiWii.Documents/Services/AssistantUploadPackService.cs']:
    p=root/rel
    if p.exists(): p.write_text(p.read_text().replace('0.19.0.0','0.20.0.0').replace('0.19.0','0.20.0'))

src=Path('build-piiwii-documents-0200/templates/DocumentSyncService.cs')
dst=root/'src/PiiWii.Documents/Services/DocumentSyncService.cs'
dst.write_text(src.read_text())

p=root/'src/PiiWii.Documents/MainForm.cs';s=p.read_text()
field='    readonly Dictionary<string,Button> smartButtons=new(StringComparer.OrdinalIgnoreCase);'
if field not in s: raise SystemExit('MainForm field anchor missing')
s=s.replace(field,field+'\n    readonly Label syncStatus=new(){Text="ChatGPT : connexion…",AutoSize=true,ForeColor=Color.FromArgb(110,118,132),Padding=new(8,4,0,0),Font=new("Segoe UI",8.5f)};')
ctor='        BuildUi(); _=InitPreview(); RefreshList();'
if ctor not in s: raise SystemExit('MainForm ctor anchor missing')
s=s.replace(ctor,'        BuildUi(); _=InitPreview(); RefreshList();\n        DocumentSyncService.Instance.StatusChanged+=SyncStatusChanged;\n        Shown+=(_,_)=>DocumentSyncService.Instance.Start();\n        FormClosed+=(_,_)=>DocumentSyncService.Instance.StatusChanged-=SyncStatusChanged;')
ver='        nav.Controls.Add(new Label{Text="v0.20.0",AutoSize=true,ForeColor=Color.Silver,Padding=new(8,14,0,0)});'
if ver not in s: raise SystemExit('Version label anchor missing')
s=s.replace(ver,ver+'\n        nav.Controls.Add(syncStatus);')
anchor='    async Task InitPreview()'
extra='    void SyncStatusChanged(string text){if(IsDisposed)return;try{BeginInvoke(()=>{syncStatus.Text="ChatGPT : "+text;syncStatus.ForeColor=text=="connecté"?Color.FromArgb(22,130,80):Color.FromArgb(150,100,30);});}catch{}}\n'
if anchor not in s: raise SystemExit('InitPreview anchor missing')
s=s.replace(anchor,extra+anchor)
p.write_text(s)
