using System.IO.Compression;
using System.Security.Cryptography;
using System.Text.Json;
using System.Text.RegularExpressions;
using PiiWii.Documents.Models;

namespace PiiWii.Documents.Services;

public static class AssistantImportPackService
{
    public sealed record Result(int Imported,int Duplicates,int Errors,string BatchId,List<string> ErrorMessages);
    public static async Task<Result> ImportAsync(string zipPath,IProgress<(int done,int total,string file)>? progress=null)
    {
        var tmp=Path.Combine(Path.GetTempPath(),"piiwii-pack-"+Guid.NewGuid().ToString("N"));Directory.CreateDirectory(tmp);var errors=new List<string>();int ok=0,dups=0,err=0;
        try
        {
            using(var z=ZipFile.OpenRead(zipPath))foreach(var e in z.Entries){var full=Path.GetFullPath(Path.Combine(tmp,e.FullName.Replace('/',Path.DirectorySeparatorChar)));if(!full.StartsWith(Path.GetFullPath(tmp)+Path.DirectorySeparatorChar,StringComparison.OrdinalIgnoreCase))throw new InvalidDataException("Chemin interdit dans le paquet.");if(string.IsNullOrEmpty(e.Name)){Directory.CreateDirectory(full);continue;}Directory.CreateDirectory(Path.GetDirectoryName(full)!);e.ExtractToFile(full,true);}
            var manifest=Path.Combine(tmp,"manifest.json");if(!File.Exists(manifest))throw new InvalidDataException("manifest.json absent du paquet.");using var jd=JsonDocument.Parse(File.ReadAllText(manifest));var root=jd.RootElement;if(root.GetProperty("schema").GetString()!="piiwii-documents-import-v1")throw new InvalidDataException("Format de paquet non reconnu.");var docs=root.GetProperty("documents").EnumerateArray().ToList();var batch=DatabaseService.CreateBatch($"Paquet ChatGPT {DateTime.Now:dd.MM.yyyy HH:mm}");int done=0;
            foreach(var item in docs)
            {
                try
                {
                    var rel=item.GetProperty("file").GetString()??"";var src=Path.GetFullPath(Path.Combine(tmp,rel.Replace('/',Path.DirectorySeparatorChar)));if(!src.StartsWith(Path.GetFullPath(tmp)+Path.DirectorySeparatorChar,StringComparison.OrdinalIgnoreCase)||!File.Exists(src))throw new InvalidDataException($"Fichier absent: {rel}");progress?.Report((done,docs.Count,Path.GetFileName(src)));var sha=HashFile(src);if(item.TryGetProperty("sha256",out var sh)&&sh.ValueKind==JsonValueKind.String&&!string.IsNullOrWhiteSpace(sh.GetString())&&!string.Equals(sh.GetString(),sha,StringComparison.OrdinalIgnoreCase))throw new InvalidDataException($"SHA-256 invalide pour {rel}");if(DatabaseService.ShaExists(sha)){dups++;continue;}
                    var ocr=await OcrService.ReadAsync(src);var a=DocumentAnalyzer.Analyze(ocr.Text,Path.GetFileName(src));string? S(string n)=>item.TryGetProperty(n,out var e)&&e.ValueKind==JsonValueKind.String?e.GetString():null;var cat=Clean(S("category")??a.Category);var org=S("organization")??a.Organization;var type=S("documentType")??a.DocumentType;DateTime? dd=null;if(S("documentDate")is{}ds&&DateTime.TryParse(ds,out var dv))dd=dv.Date;dd??=a.Date;var sortDate=dd??File.GetLastWriteTime(src);var dir=Path.Combine(AppPaths.DocumentsRoot,sortDate.Year.ToString(),cat);Directory.CreateDirectory(dir);var dest=Unique(Path.Combine(dir,$"{sortDate:yyyy-MM-dd}_{Safe(org.Length>0?org:"Document")}_{Safe(type)}{Path.GetExtension(src).ToLowerInvariant()}"));File.Copy(src,dest,false);decimal? amount=a.Amount;if(item.TryGetProperty("amount",out var am)&&am.TryGetDecimal(out var av))amount=av;int conf=item.TryGetProperty("confidence",out var cf)&&cf.TryGetInt32(out var cv)?Math.Clamp(cv,0,100):Math.Max(85,a.Confidence);
                    var d=new DocumentRecord{Title=S("title")??a.Title,Category=cat,Organization=org,Description=S("description")??a.Description,OriginalName=Path.GetFileName(src),StoredPath=dest,Sha256=sha,OcrText=ocr.Text,Tags=S("tags")??a.Tags,Vehicle=S("vehicle")??a.Vehicle,DocumentType=type,Reference=S("reference")??a.Reference,Confidence=conf,AnalysisStatus="Préclassé par ChatGPT",PageCount=ocr.PageCount,BatchId=batch,Amount=amount,Currency=S("currency")??a.Currency,DocumentDate=dd,ImportedAt=DateTime.Now,Status=S("status")??a.Status,PaperOriginal="Inconnu"};var id=DatabaseService.Insert(d);DatabaseService.AuditExternal("Assistant IA","Document importé depuis paquet préparé",id,$"Lot={batch}; fichier={rel}");ok++;
                }
                catch(Exception ex){err++;errors.Add(ex.Message);}finally{done++;progress?.Report((done,docs.Count,""));}
            }
            if(ok>0)ImportService.AutoLinkBatch(batch);return new(ok,dups,err,batch,errors);
        }
        finally{try{Directory.Delete(tmp,true);}catch{}}
    }
    static string HashFile(string p){using var s=File.OpenRead(p);return Convert.ToHexString(SHA256.HashData(s)).ToLowerInvariant();}
    static string Safe(string s){foreach(var c in Path.GetInvalidFileNameChars())s=s.Replace(c,'-');s=Regex.Replace(s,@"\s+","-");var t=s.Trim('-');return t.Length<=70?t:t[..70];}
    static string Clean(string s)=>Safe(string.IsNullOrWhiteSpace(s)?"Autres":s);
    static string Unique(string p){if(!File.Exists(p))return p;var d=Path.GetDirectoryName(p)!;var n=Path.GetFileNameWithoutExtension(p);var e=Path.GetExtension(p);for(int i=2;;i++){var x=Path.Combine(d,$"{n}_{i}{e}");if(!File.Exists(x))return x;}}
}
