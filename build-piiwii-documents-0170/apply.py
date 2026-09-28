from pathlib import Path
root=Path('build-piiwii-documents-0170/work')
services=root/'src/PiiWii.Documents/Services'

# Version bump.
for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/Services/BridgeService.cs','src/PiiWii.Documents/Services/AssistantExchangeService.cs','src/PiiWii.Documents/Services/BackupService.cs','src/PiiWii.Documents/Services/AssistantUploadPackService.cs']:
    p=root/rel
    if p.exists(): p.write_text(p.read_text().replace('0.16.0.0','0.17.0.0').replace('0.16.0','0.17.0'))

# Replace outbound pack service with a session-bound v2 format.
(services/'AssistantUploadPackService.cs').write_text(r'''using System.IO.Compression;
using System.Text.Json;
using PiiWii.Documents.Models;
namespace PiiWii.Documents.Services;
public static class AssistantUploadPackService
{
    public sealed record ExportResult(string Path,int Documents,long Bytes,string ExchangeId,DateTime ExpiresAt);
    static string ExchangeRoot=>Path.Combine(AppPaths.DataRoot,"Assistant","exchanges");
    public static ExportResult Export(string? batchId,long? documentId)
    {
        List<DocumentRecord> docs;string scope;
        if(!string.IsNullOrWhiteSpace(batchId)){docs=DatabaseService.GetBatch(batchId);scope=$"batch:{batchId}";}
        else if(documentId is long id && DatabaseService.Get(id) is{} d){docs=new(){d};scope=$"document:{id}";}
        else throw new InvalidOperationException("Sélectionne un document ou ouvre un lot avant de créer un paquet pour ChatGPT.");
        if(docs.Count==0)throw new InvalidOperationException("Aucun document à exporter.");
        if(docs.Count>100)throw new InvalidOperationException("Maximum 100 documents par paquet ChatGPT.");
        Directory.CreateDirectory(ExchangeRoot);var outbox=Path.Combine(AppPaths.DataRoot,"Assistant","upload");Directory.CreateDirectory(outbox);
        var exchangeId=Guid.NewGuid().ToString("N");var expires=DateTime.UtcNow.AddHours(48);var ids=docs.Select(d=>d.Id).ToArray();
        File.WriteAllText(Path.Combine(ExchangeRoot,exchangeId+".json"),JsonSerializer.Serialize(new{schema="piiwii-documents-exchange-session-v1",exchangeId,scope,expiresAt=expires,documentIds=ids},new JsonSerializerOptions{WriteIndented=true}));
        var tmp=Path.Combine(Path.GetTempPath(),"piiwii-upload-"+Guid.NewGuid().ToString("N"));Directory.CreateDirectory(tmp);Directory.CreateDirectory(Path.Combine(tmp,"documents"));
        try
        {
            long bytes=0;var items=new List<object>();int n=0;
            foreach(var d in docs){if(!File.Exists(d.StoredPath))continue;n++;var ext=Path.GetExtension(d.StoredPath).ToLowerInvariant();var fn=$"{n:000}_{Safe(d.OriginalName.Length>0?d.OriginalName:Path.GetFileName(d.StoredPath))}";if(!Path.HasExtension(fn))fn+=ext;var rel="documents/"+fn;var dest=Path.Combine(tmp,"documents",fn);File.Copy(d.StoredPath,dest,true);bytes+=new FileInfo(dest).Length;if(bytes>500L*1024*1024)throw new InvalidOperationException("Le paquet dépasse 500 Mo.");items.Add(new{id=d.Id,file=rel,title=d.Title,documentType=d.DocumentType,category=d.Category,organization=d.Organization,reference=d.Reference,amount=d.Amount,currency=d.Currency,documentDate=d.DocumentDate,status=d.Status,vehicle=d.Vehicle,tags=d.Tags,description=d.Description,confidence=d.Confidence,analysisStatus=d.AnalysisStatus,pageCount=d.PageCount,sha256=d.Sha256});}
            var manifest=new{schema="piiwii-documents-chatgpt-upload-v2",appVersion="0.17.0",exchangeId,createdAt=DateTime.UtcNow,expiresAt=expires,scope,instructions="Analyse uniquement ces documents. Retourne un ZIP contenant result.json au schema piiwii-documents-chatgpt-return-v1, avec le meme exchangeId et uniquement les ids fournis.",documents=items};
            File.WriteAllText(Path.Combine(tmp,"manifest.json"),JsonSerializer.Serialize(manifest,new JsonSerializerOptions{WriteIndented=true}));
            var template=new{schema="piiwii-documents-chatgpt-return-v1",exchangeId,documents=docs.Select(d=>new{id=d.Id,title=d.Title,documentType=d.DocumentType,category=d.Category,organization=d.Organization,reference=d.Reference,amount=d.Amount,currency=d.Currency,documentDate=d.DocumentDate,status=d.Status,vehicle=d.Vehicle,tags=d.Tags,description=d.Description,confidence=d.Confidence}).ToArray()};
            File.WriteAllText(Path.Combine(tmp,"response-template.json"),JsonSerializer.Serialize(template,new JsonSerializerOptions{WriteIndented=true}));
            File.WriteAllText(Path.Combine(tmp,"LISEZ-MOI.txt"),"Paquet PiiWii Documents -> ChatGPT. Ne modifiez pas les fichiers originaux. Le retour attendu est un ZIP contenant result.json base sur response-template.json.");
            var path=Path.Combine(outbox,$"PiiWii-vers-ChatGPT-{DateTime.Now:yyyyMMdd-HHmmss}-{exchangeId[..8]}.zip");ZipFile.CreateFromDirectory(tmp,path,CompressionLevel.Optimal,false);DatabaseService.AuditExternal("Utilisateur","Paquet créé pour ChatGPT",null,$"{scope}; échange={exchangeId}; docs={n}");return new(path,n,bytes,exchangeId,expires);
        }catch{try{File.Delete(Path.Combine(ExchangeRoot,exchangeId+".json"));}catch{}throw;}finally{try{Directory.Delete(tmp,true);}catch{}}
    }
    static string Safe(string s){foreach(var c in Path.GetInvalidFileNameChars())s=s.Replace(c,'-');return s.Trim();}
}
''')

# New safe return-package service for existing documents only.
(services/'AssistantReturnPackService.cs').write_text(r'''using System.IO.Compression;
using System.Text.Json;
using PiiWii.Documents.Models;
namespace PiiWii.Documents.Services;
public static class AssistantReturnPackService
{
    public sealed record Preview(string ExchangeId,int Proposed,int Ignored,List<string> Changes,List<string> Errors);
    static string ExchangeRoot=>Path.Combine(AppPaths.DataRoot,"Assistant","exchanges");
    public static Preview Inspect(string zipPath)
    {
        using var z=ZipFile.OpenRead(zipPath);if(z.Entries.Count>20)throw new InvalidDataException("Paquet retour anormalement volumineux.");
        var e=z.GetEntry("result.json")??throw new InvalidDataException("result.json absent du paquet retour.");if(e.Length>5*1024*1024)throw new InvalidDataException("result.json trop volumineux.");
        using var sr=new StreamReader(e.Open());using var jd=JsonDocument.Parse(sr.ReadToEnd());var root=jd.RootElement;
        if(root.GetProperty("schema").GetString()!="piiwii-documents-chatgpt-return-v1")throw new InvalidDataException("Format de retour ChatGPT non reconnu.");
        var exchangeId=root.GetProperty("exchangeId").GetString()??"";var allowed=LoadSession(exchangeId);
        var errors=new List<string>();var changes=new List<string>();int proposed=0,ignored=0;
        if(!root.TryGetProperty("documents",out var arr)||arr.ValueKind!=JsonValueKind.Array)throw new InvalidDataException("Tableau documents absent.");
        foreach(var item in arr.EnumerateArray())
        {
            if(!item.TryGetProperty("id",out var ie)||!ie.TryGetInt64(out var id)||!allowed.Contains(id)){ignored++;continue;}var d=DatabaseService.Get(id);if(d==null){ignored++;continue;}
            var f=new List<string>();Check("title",d.Title);Check("documentType",d.DocumentType);Check("category",d.Category);Check("organization",d.Organization);Check("reference",d.Reference);Check("status",d.Status);Check("vehicle",d.Vehicle);Check("tags",d.Tags);Check("description",d.Description);if(item.TryGetProperty("amount",out var am)&&am.ValueKind==JsonValueKind.Number&&am.TryGetDecimal(out var av)&&d.Amount!=av)f.Add($"montant {d.Amount} -> {av}");if(f.Count>0){proposed++;changes.Add($"#{id} {d.Title}: {string.Join(", ",f.Take(5))}");}
            void Check(string n,string old){if(item.TryGetProperty(n,out var x)&&x.ValueKind==JsonValueKind.String){var v=x.GetString()??"";if(v.Trim()!=old.Trim())f.Add($"{n} -> {v.Trim()}");}}
        }
        return new(exchangeId,proposed,ignored,changes,errors);
    }
    public static (int updated,int ignored,List<string> errors) Apply(string zipPath)
    {
        var preview=Inspect(zipPath);BackupService.CreateDatabaseBackup("avant-retour-chatgpt",true);var errors=new List<string>();int updated=0,ignored=0;
        using var z=ZipFile.OpenRead(zipPath);using var sr=new StreamReader(z.GetEntry("result.json")!.Open());using var jd=JsonDocument.Parse(sr.ReadToEnd());var root=jd.RootElement;var allowed=LoadSession(preview.ExchangeId);
        foreach(var item in root.GetProperty("documents").EnumerateArray())try{if(!item.TryGetProperty("id",out var ie)||!ie.TryGetInt64(out var id)||!allowed.Contains(id)){ignored++;continue;}var d=DatabaseService.Get(id);if(d==null){ignored++;continue;}ApplyOne(d,item);DatabaseService.Update(d,"Assistant IA","Retour ChatGPT 0.17.0");updated++;}catch(Exception ex){errors.Add(ex.Message);}
        try{File.Delete(Path.Combine(ExchangeRoot,preview.ExchangeId+".json"));}catch{}DatabaseService.AuditExternal("Assistant IA","Retour ChatGPT appliqué",null,$"échange={preview.ExchangeId}; updated={updated}; ignored={ignored}");return(updated,ignored,errors);
    }
    static HashSet<long> LoadSession(string id){if(string.IsNullOrWhiteSpace(id)||id.Any(c=>!char.IsLetterOrDigit(c)))throw new InvalidDataException("exchangeId invalide.");var p=Path.Combine(ExchangeRoot,id+".json");if(!File.Exists(p))throw new InvalidDataException("Échange inconnu, expiré ou déjà utilisé.");using var jd=JsonDocument.Parse(File.ReadAllText(p));var r=jd.RootElement;if(r.GetProperty("expiresAt").GetDateTime().ToUniversalTime()<=DateTime.UtcNow)throw new InvalidDataException("Échange expiré.");return r.GetProperty("documentIds").EnumerateArray().Select(x=>x.GetInt64()).ToHashSet();}
    static void ApplyOne(DocumentRecord d,JsonElement i){string? S(string n)=>i.TryGetProperty(n,out var e)&&e.ValueKind==JsonValueKind.String?e.GetString():null;if(S("title")is{}a)d.Title=a.Trim();if(S("documentType")is{}b)d.DocumentType=b.Trim();if(S("category")is{}c)d.Category=c.Trim();if(S("organization")is{}e)d.Organization=e.Trim();if(S("reference")is{}f)d.Reference=f.Trim();if(S("currency")is{}g)d.Currency=g.Trim();if(S("status")is{}h)d.Status=h.Trim();if(S("vehicle")is{}v)d.Vehicle=v.Trim();if(S("tags")is{}t)d.Tags=t.Trim();if(S("description")is{}z)d.Description=z.Trim();if(i.TryGetProperty("amount",out var am)&&am.ValueKind==JsonValueKind.Number&&am.TryGetDecimal(out var av))d.Amount=av;if(S("documentDate")is{}ds&&DateTime.TryParse(ds,out var dv))d.DocumentDate=dv.Date;if(i.TryGetProperty("confidence",out var cf)&&cf.TryGetInt32(out var cv))d.Confidence=Math.Clamp(cv,0,100);d.AnalysisStatus="Classé par ChatGPT";}
}
''')

# Add a round-trip self-test that does not touch real records.
(services/'RoundTripSelfTestService.cs').write_text(r'''using System.IO.Compression;
using System.Text.Json;
namespace PiiWii.Documents.Services;
public static class RoundTripSelfTestService
{
    public sealed record Result(bool Ok,string Detail);
    public static Result Run()
    {
        var tmp=Path.Combine(Path.GetTempPath(),"piiwii-roundtrip-test-"+Guid.NewGuid().ToString("N"));Directory.CreateDirectory(tmp);
        try
        {
            var id=Guid.NewGuid().ToString("N");var exchanges=Path.Combine(AppPaths.DataRoot,"Assistant","exchanges");Directory.CreateDirectory(exchanges);var session=Path.Combine(exchanges,id+".json");File.WriteAllText(session,JsonSerializer.Serialize(new{schema="piiwii-documents-exchange-session-v1",exchangeId=id,scope="selftest",expiresAt=DateTime.UtcNow.AddMinutes(5),documentIds=Array.Empty<long>()}));
            var result=Path.Combine(tmp,"result.json");File.WriteAllText(result,JsonSerializer.Serialize(new{schema="piiwii-documents-chatgpt-return-v1",exchangeId=id,documents=Array.Empty<object>()}));var zip=Path.Combine(tmp,"return.zip");using(var z=ZipFile.Open(zip,ZipArchiveMode.Create))z.CreateEntryFromFile(result,"result.json");var p=AssistantReturnPackService.Inspect(zip);try{File.Delete(session);}catch{}return new(p.ExchangeId==id,"Paquet retour, session temporaire et validation : OK");
        }catch(Exception ex){return new(false,ex.Message);}finally{try{Directory.Delete(tmp,true);}catch{}}
    }
}
''')

p=root/'src/PiiWii.Documents/MainForm.cs';s=p.read_text().replace('PiiWii Documents 0.16.0','PiiWii Documents 0.17.0').replace('v0.16.0','v0.17.0')
s=s.replace('moreMenu.Items.Add("Créer un paquet à envoyer à ChatGPT",null,(_,_)=>ExportForChatGpt());','moreMenu.Items.Add("Créer un paquet à envoyer à ChatGPT",null,(_,_)=>ExportForChatGpt());\n        moreMenu.Items.Add("Importer le retour ChatGPT",null,(_,_)=>ImportChatGptReturn());')
s=s.replace('moreMenu.Items.Add("Diagnostic complet",null,(_,_)=>RunDiagnostics());','moreMenu.Items.Add("Diagnostic complet",null,(_,_)=>RunDiagnostics());\n        moreMenu.Items.Add("Tester aller-retour ChatGPT",null,(_,_)=>TestChatGptRoundTrip());')
s=s.replace('MessageBox.Show($"Paquet créé : {r.Documents} document(s)\\n{r.Bytes/1024d/1024d:0.0} Mo\\n\\n{r.Path}\\n\\nOuvrir le dossier ?","Paquet pour ChatGPT"','MessageBox.Show($"Paquet créé : {r.Documents} document(s)\\n{r.Bytes/1024d/1024d:0.0} Mo\\nÉchange : {r.ExchangeId[..8]}\\nExpire : {r.ExpiresAt.ToLocalTime():dd.MM.yyyy HH:mm}\\n\\n{r.Path}\\n\\nOuvrir le dossier ?","Paquet pour ChatGPT"')
marker='    void RunDiagnostics(){var checks=DiagnosticsService.Run();'
extra=r'''    void ImportChatGptReturn(){using var o=new OpenFileDialog{Filter="Retour PiiWii ChatGPT|*.zip|Tous les fichiers|*.*",Title="Importer le retour ChatGPT"};if(o.ShowDialog()!=DialogResult.OK)return;try{var p=AssistantReturnPackService.Inspect(o.FileName);var sample=p.Changes.Count==0?"Aucune modification détectée.":string.Join("\n",p.Changes.Take(10));if(MessageBox.Show($"Échange : {p.ExchangeId[..8]}\nModifications proposées : {p.Proposed}\nIgnorées : {p.Ignored}\n\n{sample}\n\nAppliquer ce retour ?","Retour ChatGPT",MessageBoxButtons.YesNo,MessageBoxIcon.Question)!=DialogResult.Yes)return;var r=AssistantReturnPackService.Apply(o.FileName);MessageBox.Show($"Documents mis à jour : {r.updated}\nIgnorés : {r.ignored}\nErreurs : {r.errors.Count}","Retour ChatGPT",MessageBoxButtons.OK,r.errors.Count==0?MessageBoxIcon.Information:MessageBoxIcon.Warning);RefreshList();LoadSelected();}catch(Exception ex){MessageBox.Show(ex.Message,"Retour ChatGPT refusé",MessageBoxButtons.OK,MessageBoxIcon.Error);}}
    void TestChatGptRoundTrip(){var r=RoundTripSelfTestService.Run();MessageBox.Show(r.Detail,r.Ok?"Test aller-retour : OK":"Test aller-retour : échec",MessageBoxButtons.OK,r.Ok?MessageBoxIcon.Information:MessageBoxIcon.Error);}
'''
if 'void ImportChatGptReturn()' not in s:s=s.replace(marker,extra+marker)
p.write_text(s)
