from pathlib import Path
root=Path('build-piiwii-documents-0140/work')

# Copy safe overlay services.
over=Path('build-piiwii-documents-0140/overlay/Services')
services=root/'src/PiiWii.Documents/Services'
for name in ['BackupService.cs','AssistantExchangeService.cs','AssistantImportPackService.cs']:
    (services/name).write_text((over/name).read_text())

# Version bump.
for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs']:
    p=root/rel;s=p.read_text().replace('0.13.0.0','0.14.0.0').replace('0.13.0','0.14.0');p.write_text(s)

# Real backups before migration + daily backup.
p=root/'src/PiiWii.Documents/Program.cs';s=p.read_text();s=s.replace('AppPaths.Initialize();\n        DatabaseService.Initialize();','AppPaths.Initialize();\n        BackupService.CreateDatabaseBackup("avant-migration",true);\n        DatabaseService.Initialize();\n        BackupService.EnsureDailyBackup();');p.write_text(s)

# Bridge: correct advertised version, no fake classify permission, add revocation.
p=services/'BridgeService.cs';s=p.read_text().replace('version="0.5.0"','version="0.14.0"').replace('SaveToken(token,"read,search,classify"','SaveToken(token,"read,search"').replace('permissions=new[]{"read","search","classify"}','permissions=new[]{"read","search"}')
marker='    private bool ValidateCode(string code)'
if 'public int RevokeAll()' not in s:
    s=s.replace(marker,'    public int RevokeAll(){try{using var cn=new Microsoft.Data.Sqlite.SqliteConnection($"Data Source={AppPaths.DatabasePath}");cn.Open();var cmd=cn.CreateCommand();cmd.CommandText="UPDATE bridge_tokens SET revoked=1 WHERE revoked=0";var n=cmd.ExecuteNonQuery();DatabaseService.AuditExternal("Utilisateur","Accès pont révoqués",null,$"{n} session(s)");return n;}catch{return 0;}}\n'+marker)
p.write_text(s)

# Assistant version old literal may still exist elsewhere.
for rel in ['src/PiiWii.Documents/Services/BridgeService.cs']:
    p=root/rel;p.write_text(p.read_text().replace('0.13.0','0.14.0'))

# Main UI: safe import pack, preview-before-apply, backup actions and bridge revoke.
p=root/'src/PiiWii.Documents/MainForm.cs';s=p.read_text().replace('PiiWii Documents 0.13.0','PiiWii Documents 0.14.0').replace('v0.13.0','v0.14.0')
s=s.replace('moreMenu.Items.Add("Importer le classement ChatGPT",null,(_,_)=>ImportAiResults());','moreMenu.Items.Add("Importer le classement ChatGPT",null,(_,_)=>ImportAiResults());\n        moreMenu.Items.Add("Importer un paquet préparé par ChatGPT",null,async(_,_)=>await ImportAssistantPack());')
s=s.replace('moreMenu.Items.Add("Pont sécurisé",null,(_,_)=>ShowBridge());','moreMenu.Items.Add("Pont sécurisé",null,(_,_)=>ShowBridge());\n        moreMenu.Items.Add("Révoquer les accès du pont",null,(_,_)=>RevokeBridge());\n        moreMenu.Items.Add(new ToolStripSeparator());\n        moreMenu.Items.Add("Créer une sauvegarde maintenant",null,(_,_)=>CreateBackup());\n        moreMenu.Items.Add("Ouvrir les sauvegardes",null,(_,_)=>Process.Start(new ProcessStartInfo(AppPaths.BackupsRoot){UseShellExecute=true}));')
old='void ImportAiResults(){using var o=new OpenFileDialog{Filter="Résultats assistant JSON|*.json|Tous les fichiers|*.*",Title="Importer le classement renvoyé par l\'assistant"};if(o.ShowDialog()!=DialogResult.OK)return;try{var r=AssistantExchangeService.ImportResults(o.FileName);MessageBox.Show($"Mises à jour appliquées : {r.updated}\\nIgnorées : {r.ignored}\\nErreurs : {r.errors.Count}","Résultats assistant",MessageBoxButtons.OK,r.errors.Count>0?MessageBoxIcon.Warning:MessageBoxIcon.Information);RefreshList();LoadSelected();}catch(Exception ex){MessageBox.Show(ex.Message,"Résultats assistant",MessageBoxButtons.OK,MessageBoxIcon.Error);}}'
new='void ImportAiResults(){using var o=new OpenFileDialog{Filter="Résultats assistant JSON|*.json|Tous les fichiers|*.*",Title="Importer le classement renvoyé par l\'assistant"};if(o.ShowDialog()!=DialogResult.OK)return;try{var p=AssistantExchangeService.PreviewResults(o.FileName);var sample=string.Join("\\n",p.Changes.Take(8));if(MessageBox.Show($"Modifications proposées : {p.Proposed}\\nIgnorées : {p.Ignored}\\n\\n{sample}\\n\\nAppliquer ces modifications ?","Vérifier le classement ChatGPT",MessageBoxButtons.YesNo,MessageBoxIcon.Question)!=DialogResult.Yes)return;var r=AssistantExchangeService.ApplyResults(o.FileName);MessageBox.Show($"Mises à jour appliquées : {r.updated}\\nIgnorées : {r.ignored}\\nErreurs : {r.errors.Count}","Résultats assistant",MessageBoxButtons.OK,r.errors.Count>0?MessageBoxIcon.Warning:MessageBoxIcon.Information);RefreshList();LoadSelected();}catch(Exception ex){MessageBox.Show(ex.Message,"Résultats assistant",MessageBoxButtons.OK,MessageBoxIcon.Error);}}'
s=s.replace(old,new)
show='void ShowBridge(){var b=BridgeService.Instance;var code=b.GeneratePairCode();MessageBox.Show($"Pont sécurisé local : {(b.Running?"ACTIF":"INACTIF")}\\nAdresse : http://127.0.0.1:{b.Port}/api/v1/\\n\\nCode d\'appairage : {code}\\nValable 5 minutes.\\n\\nLe pont n\'accepte aucune commande système et n\'expose que PiiWii Documents.","Pont sécurisé",MessageBoxButtons.OK,MessageBoxIcon.Information);}'
extra='    async Task ImportAssistantPack(){using var o=new OpenFileDialog{Filter="Paquet PiiWii préparé par ChatGPT|*.zip|Tous les fichiers|*.*",Title="Importer un paquet préparé par ChatGPT"};if(o.ShowDialog()!=DialogResult.OK)return;busy=true;progressBar.Visible=true;var pr=new Progress<(int done,int total,string file)>(x=>{progressBar.Maximum=Math.Max(1,x.total);progressBar.Value=Math.Min(x.done,progressBar.Maximum);progressLabel.Text=$"Import ChatGPT {x.done}/{x.total} {x.file}";});try{var r=await AssistantImportPackService.ImportAsync(o.FileName,pr);lastBatchId=r.BatchId;MessageBox.Show($"Documents importés : {r.Imported}\\nDoublons ignorés : {r.Duplicates}\\nErreurs : {r.Errors}","Paquet ChatGPT",MessageBoxButtons.OK,r.Errors>0?MessageBoxIcon.Warning:MessageBoxIcon.Information);search.Text=r.BatchId;trashMode=false;RefreshList();}catch(Exception ex){MessageBox.Show(ex.Message,"Paquet ChatGPT",MessageBoxButtons.OK,MessageBoxIcon.Error);}finally{busy=false;progressBar.Visible=false;progressLabel.Text="";}}\n    void RevokeBridge(){var n=BridgeService.Instance.RevokeAll();MessageBox.Show($"{n} session(s) révoquée(s).","Pont sécurisé",MessageBoxButtons.OK,MessageBoxIcon.Information);}\n    void CreateBackup(){try{var p=BackupService.CreateDatabaseBackup("manuel");MessageBox.Show(string.IsNullOrWhiteSpace(p)?"Aucune base à sauvegarder.":$"Sauvegarde créée :\\n{p}","Sauvegarde",MessageBoxButtons.OK,MessageBoxIcon.Information);}catch(Exception ex){MessageBox.Show(ex.Message,"Sauvegarde",MessageBoxButtons.OK,MessageBoxIcon.Error);}}\n'
if extra.strip() not in s:s=s.replace(show,extra+show)
p.write_text(s)
