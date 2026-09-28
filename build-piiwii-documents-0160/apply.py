from pathlib import Path
root=Path('build-piiwii-documents-0160/work')
services=root/'src/PiiWii.Documents/Services'

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/Services/BridgeService.cs','src/PiiWii.Documents/Services/AssistantExchangeService.cs','src/PiiWii.Documents/Services/BackupService.cs']:
    p=root/rel
    if p.exists(): p.write_text(p.read_text().replace('0.15.0.0','0.16.0.0').replace('0.15.0','0.16.0'))

(services/'AssistantUploadPackService.cs').write_text(r'''using System.IO.Compression;
using System.Text.Json;
using PiiWii.Documents.Models;
namespace PiiWii.Documents.Services;
public static class AssistantUploadPackService
{
    public sealed record ExportResult(string Path,int Documents,long Bytes);
    public static ExportResult Export(string? batchId,long? documentId)
    {
        List<DocumentRecord> docs;string scope;
        if(!string.IsNullOrWhiteSpace(batchId)){docs=DatabaseService.GetBatch(batchId);scope=$"batch:{batchId}";}
        else if(documentId is long id && DatabaseService.Get(id) is{} d){docs=new(){d};scope=$"document:{id}";}
        else throw new InvalidOperationException("Sélectionne un document ou ouvre un lot avant de créer un paquet pour ChatGPT.");
        if(docs.Count==0)throw new InvalidOperationException("Aucun document à exporter.");
        if(docs.Count>100)throw new InvalidOperationException("Maximum 100 documents par paquet ChatGPT.");
        var outbox=Path.Combine(AppPaths.DataRoot,"Assistant","upload");Directory.CreateDirectory(outbox);
        var tmp=Path.Combine(Path.GetTempPath(),"piiwii-upload-"+Guid.NewGuid().ToString("N"));Directory.CreateDirectory(tmp);Directory.CreateDirectory(Path.Combine(tmp,"documents"));
        try
        {
            long bytes=0;var items=new List<object>();int n=0;
            foreach(var d in docs){if(!File.Exists(d.StoredPath))continue;n++;var ext=Path.GetExtension(d.StoredPath).ToLowerInvariant();var fn=$"{n:000}_{Safe(d.OriginalName.Length>0?d.OriginalName:Path.GetFileName(d.StoredPath))}";if(!Path.HasExtension(fn))fn+=ext;var rel="documents/"+fn;var dest=Path.Combine(tmp,"documents",fn);File.Copy(d.StoredPath,dest,true);bytes+=new FileInfo(dest).Length;if(bytes>500L*1024*1024)throw new InvalidOperationException("Le paquet dépasse 500 Mo.");items.Add(new{d.Id,file=rel,d.Title,d.DocumentType,d.Category,d.Organization,d.Reference,d.Amount,d.Currency,d.DocumentDate,d.Status,d.Vehicle,d.Tags,d.Description,d.Confidence,d.AnalysisStatus,d.PageCount,d.Sha256});}
            var manifest=new{schema="piiwii-documents-chatgpt-upload-v1",appVersion="0.16.0",createdAt=DateTime.UtcNow,scope,instructions="Analyse les fichiers et retourne un PiiWii Import Pack (schema piiwii-documents-import-v1) ou un résultat de classement compatible.",documents=items};
            File.WriteAllText(Path.Combine(tmp,"manifest.json"),JsonSerializer.Serialize(manifest,new JsonSerializerOptions{WriteIndented=true}));
            var path=Path.Combine(outbox,$"PiiWii-vers-ChatGPT-{DateTime.Now:yyyyMMdd-HHmmss}.zip");ZipFile.CreateFromDirectory(tmp,path,CompressionLevel.Optimal,false);DatabaseService.AuditExternal("Utilisateur","Paquet créé pour ChatGPT",null,$"{scope}; docs={n}");return new(path,n,bytes);
        }finally{try{Directory.Delete(tmp,true);}catch{}}
    }
    static string Safe(string s){foreach(var c in Path.GetInvalidFileNameChars())s=s.Replace(c,'-');return s.Trim();}
}
''')

(services/'DiagnosticsService.cs').write_text(r'''using Microsoft.Data.Sqlite;
namespace PiiWii.Documents.Services;
public static class DiagnosticsService
{
    public sealed record Check(string Name,bool Ok,string Detail);
    public static List<Check> Run()
    {
        var r=new List<Check>();
        try{using var cn=new SqliteConnection($"Data Source={AppPaths.DatabasePath};Mode=ReadOnly");cn.Open();using var c=cn.CreateCommand();c.CommandText="PRAGMA integrity_check;";var v=c.ExecuteScalar()?.ToString()??"";r.Add(new("Base SQLite",string.Equals(v,"ok",StringComparison.OrdinalIgnoreCase),v));}catch(Exception ex){r.Add(new("Base SQLite",false,ex.Message));}
        try{Directory.CreateDirectory(AppPaths.DocumentsRoot);var p=Path.Combine(AppPaths.DocumentsRoot,".piiwii-write-test");File.WriteAllText(p,"ok");File.Delete(p);r.Add(new("Dossier documents",true,AppPaths.DocumentsRoot));}catch(Exception ex){r.Add(new("Dossier documents",false,ex.Message));}
        try{Directory.CreateDirectory(AppPaths.BackupsRoot);var p=BackupService.CreateDatabaseBackup("diagnostic",true);r.Add(new("Sauvegarde",!string.IsNullOrWhiteSpace(p),string.IsNullOrWhiteSpace(p)?"Échec":"OK"));}catch(Exception ex){r.Add(new("Sauvegarde",false,ex.Message));}
        try{var b=BridgeService.Instance;r.Add(new("Pont local",b.Running,$"127.0.0.1:{b.Port} — sessions actives: {b.ActiveTokenCount()}"));}catch(Exception ex){r.Add(new("Pont local",false,ex.Message));}
        try{var dir=Path.Combine(AppPaths.DataRoot,"Assistant");Directory.CreateDirectory(dir);r.Add(new("Échanges ChatGPT",true,dir));}catch(Exception ex){r.Add(new("Échanges ChatGPT",false,ex.Message));}
        return r;
    }
}
''')

p=root/'src/PiiWii.Documents/MainForm.cs';s=p.read_text().replace('PiiWii Documents 0.15.0','PiiWii Documents 0.16.0').replace('v0.15.0','v0.16.0')
s=s.replace('moreMenu.Items.Add("Importer un paquet préparé par ChatGPT",null,async(_,_)=>await ImportAssistantPack());','moreMenu.Items.Add("Importer un paquet préparé par ChatGPT",null,async(_,_)=>await ImportAssistantPack());\n        moreMenu.Items.Add("Créer un paquet à envoyer à ChatGPT",null,(_,_)=>ExportForChatGpt());')
s=s.replace('moreMenu.Items.Add("Voir les accès du pont",null,(_,_)=>ShowBridgeSessions());','moreMenu.Items.Add("Voir / révoquer les accès du pont",null,(_,_)=>ManageBridgeSessions());')
s=s.replace('moreMenu.Items.Add("Restaurer une sauvegarde",null,(_,_)=>RestoreBackup());','moreMenu.Items.Add("Restaurer une sauvegarde",null,(_,_)=>RestoreBackup());\n        moreMenu.Items.Add(new ToolStripSeparator());\n        moreMenu.Items.Add("Diagnostic complet",null,(_,_)=>RunDiagnostics());')
start=s.find('    void ShowBridgeSessions(){')
if start>=0:
    end=s.find('\n    void RestoreBackup()',start)
    if end>start:s=s[:start]+s[end:]
marker='    void RevokeBridge(){var n=BridgeService.Instance.RevokeAll();MessageBox.Show($"{n} session(s) révoquée(s).","Pont sécurisé",MessageBoxButtons.OK,MessageBoxIcon.Information);}'
extra=r'''    void ExportForChatGpt(){try{var r=AssistantUploadPackService.Export(lastBatchId,selectedId);if(MessageBox.Show($"Paquet créé : {r.Documents} document(s)\n{r.Bytes/1024d/1024d:0.0} Mo\n\n{r.Path}\n\nOuvrir le dossier ?","Paquet pour ChatGPT",MessageBoxButtons.YesNo,MessageBoxIcon.Information)==DialogResult.Yes)Process.Start(new ProcessStartInfo(Path.GetDirectoryName(r.Path)!){UseShellExecute=true});}catch(Exception ex){MessageBox.Show(ex.Message,"Paquet pour ChatGPT",MessageBoxButtons.OK,MessageBoxIcon.Error);}}
    void RunDiagnostics(){var checks=DiagnosticsService.Run();var txt=string.Join("\n",checks.Select(x=>$"{(x.Ok?"[OK]":"[ERREUR]")} {x.Name} — {x.Detail}"));var ok=checks.All(x=>x.Ok);MessageBox.Show(txt,ok?"Diagnostic : tout est sain":"Diagnostic : contrôle nécessaire",MessageBoxButtons.OK,ok?MessageBoxIcon.Information:MessageBoxIcon.Warning);}
    void ManageBridgeSessions(){var form=new Form{Text="Accès du pont ChatGPT",Width=760,Height=430,StartPosition=FormStartPosition.CenterParent,MinimizeBox=false,MaximizeBox=false};var list=new ListBox{Dock=DockStyle.Fill,Font=new Font("Consolas",9)};var buttons=new FlowLayoutPanel{Dock=DockStyle.Bottom,Height=48,FlowDirection=FlowDirection.RightToLeft,Padding=new Padding(8)};var revoke=new Button{Text="Révoquer la session",Width=150,Height=30};var all=new Button{Text="Tout révoquer",Width=120,Height=30};var close=new Button{Text="Fermer",Width=90,Height=30};buttons.Controls.AddRange(new Control[]{close,all,revoke});form.Controls.Add(list);form.Controls.Add(buttons);void Reload(){list.Items.Clear();foreach(var x in BridgeService.Instance.ListSessions())list.Items.Add(new SessionItem(x,$"#{x.Id,-4} {(x.Revoked?"RÉVOQUÉ":x.ExpiresAt<=DateTime.Now?"EXPIRÉ":"ACTIF"),-8} {x.ScopeType}:{x.ScopeValue}  exp {x.ExpiresAt:dd.MM HH:mm}"));}Reload();close.Click+=(_,_)=>form.Close();revoke.Click+=(_,_)=>{if(list.SelectedItem is SessionItem i&&BridgeService.Instance.RevokeSession(i.Info.Id))Reload();};all.Click+=(_,_)=>{BridgeService.Instance.RevokeAll();Reload();};form.ShowDialog(this);}
    sealed class SessionItem{public BridgeService.SessionInfo Info{get;}readonly string text;public SessionItem(BridgeService.SessionInfo i,string t){Info=i;text=t;}public override string ToString()=>text;}
'''
if 'void ExportForChatGpt()' not in s:s=s.replace(marker,extra+marker)
p.write_text(s)
