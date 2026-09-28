from pathlib import Path
root=Path('build-piiwii-documents-0210/work')

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/MainForm.cs','src/PiiWii.Documents/Services/BridgeService.cs','src/PiiWii.Documents/Services/AssistantExchangeService.cs','src/PiiWii.Documents/Services/BackupService.cs','src/PiiWii.Documents/Services/AssistantUploadPackService.cs','src/PiiWii.Documents/Services/DocumentSyncService.cs']:
    p=root/rel
    if p.exists(): p.write_text(p.read_text().replace('0.20.0.0','0.21.0.0').replace('0.20.0','0.21.0'))

p=root/'src/PiiWii.Documents/Services/DocumentSyncService.cs'; s=p.read_text()
anchor='    public event Action<string>? StatusChanged;\n'
extra='''    public event Action<string>? StatusChanged;\n    public string DeviceId => (config??=LoadOrCreate()).DeviceId;\n    public string DeviceLabel => (config??=LoadOrCreate()).Label;\n    public async Task<(bool ok,string message)> DiagnosticAsync()\n    {\n        try\n        {\n            using var r=await http.GetAsync(BaseUrl+"/health");\n            var body=await r.Content.ReadAsStringAsync();\n            if(!r.IsSuccessStatusCode)return(false,$"Relais HTTP {(int)r.StatusCode}");\n            return(true,string.IsNullOrWhiteSpace(body)?"Relais accessible":"Relais accessible et répond");\n        }\n        catch(Exception ex){return(false,"Relais indisponible : "+ex.Message);}\n    }\n'''
if anchor not in s: raise SystemExit('sync event anchor missing')
s=s.replace(anchor,extra,1)
p.write_text(s)

p=root/'src/PiiWii.Documents/MainForm.cs'; s=p.read_text()
anchor='        advancedMenu.DropDownItems.Add("Tester aller-retour ChatGPT",null,(_,_)=>TestChatGptRoundTrip());'
repl=anchor+'\n        advancedMenu.DropDownItems.Add("État du pilotage ChatGPT (45 %)",null,async(_,_)=>await ShowPilotageStatus());'
if anchor not in s: raise SystemExit('advanced menu anchor missing')
s=s.replace(anchor,repl,1)
anchor='    void SyncStatusChanged(string text){if(IsDisposed)return;try{BeginInvoke(()=>{syncStatus.Text="ChatGPT : "+text;syncStatus.ForeColor=text=="connecté"?Color.FromArgb(22,130,80):Color.FromArgb(150,100,30);});}catch{}}\n'
extra='''    void SyncStatusChanged(string text){if(IsDisposed)return;try{BeginInvoke(()=>{syncStatus.Text="ChatGPT : "+text;syncStatus.ForeColor=text=="connecté"?Color.FromArgb(22,130,80):Color.FromArgb(150,100,30);});}catch{}}\n    async Task ShowPilotageStatus()\n    {\n        var d=await DocumentSyncService.Instance.DiagnosticAsync();\n        var state=d.ok?"SERVEUR ACCESSIBLE":"SERVEUR NON BRANCHÉ";\n        var msg=$"Pilotage ChatGPT : 45 %\\n\\nCôté PC : PRÊT\\nRelais piiwii.ch : {state}\\nConnexion active : {(DocumentSyncService.Instance.Connected?"oui":"non")}\\nAppareil : {DocumentSyncService.Instance.DeviceLabel}\\nID : {DocumentSyncService.Instance.DeviceId}\\n\\n{d.message}\\n\\nLe programme accepte uniquement : état, recherche, lecture OCR et correction des métadonnées. Aucune commande Windows.";\n        MessageBox.Show(msg,"Pilotage ChatGPT",MessageBoxButtons.OK,d.ok?MessageBoxIcon.Information:MessageBoxIcon.Warning);\n    }\n'''
if anchor not in s: raise SystemExit('sync method anchor missing')
s=s.replace(anchor,extra,1)
p.write_text(s)
