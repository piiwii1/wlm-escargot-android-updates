using Microsoft.Data.Sqlite;
using System.IO.Compression;
using System.Text.Json;

namespace PiiWii.Documents.Services;

public static class BackupService
{
    public static string CreateDatabaseBackup(string reason="manuel", bool silent=false)
    {
        Directory.CreateDirectory(AppPaths.BackupsRoot);
        if(!File.Exists(AppPaths.DatabasePath)) return "";
        var stamp=DateTime.Now.ToString("yyyyMMdd-HHmmss");
        var safeReason=string.Concat(reason.Select(c=>char.IsLetterOrDigit(c)||c is '-' or '_'?c:'-')).Trim('-');
        if(string.IsNullOrWhiteSpace(safeReason)) safeReason="backup";
        var zip=Path.Combine(AppPaths.BackupsRoot,$"PiiWii-Documents-{stamp}-{safeReason}.zip");
        var tmp=Path.Combine(Path.GetTempPath(),$"piiwii-backup-{Guid.NewGuid():N}");
        Directory.CreateDirectory(tmp);
        try
        {
            var dbCopy=Path.Combine(tmp,"piiwii-documents.db");
            using(var src=new SqliteConnection($"Data Source={AppPaths.DatabasePath};Mode=ReadOnly"))
            using(var dst=new SqliteConnection($"Data Source={dbCopy}"))
            { src.Open(); dst.Open(); src.BackupDatabase(dst); }
            var manifest=new {schema="piiwii-documents-backup-v1",appVersion="0.14.0",createdAt=DateTime.UtcNow,reason,dataRoot=AppPaths.DataRoot,databaseFile="piiwii-documents.db"};
            File.WriteAllText(Path.Combine(tmp,"manifest.json"),JsonSerializer.Serialize(manifest,new JsonSerializerOptions{WriteIndented=true}));
            ZipFile.CreateFromDirectory(tmp,zip,CompressionLevel.Optimal,false);
            DatabaseService.AuditExternal("Système","Sauvegarde créée",null,$"{reason}; {Path.GetFileName(zip)}");
            Rotate(10); return zip;
        }
        catch when(silent){return "";}
        finally{try{Directory.Delete(tmp,true);}catch{}}
    }

    public static void EnsureDailyBackup()
    {
        if(!File.Exists(AppPaths.DatabasePath)) return;
        Directory.CreateDirectory(AppPaths.BackupsRoot);
        var prefix=$"PiiWii-Documents-{DateTime.Now:yyyyMMdd}-";
        if(Directory.EnumerateFiles(AppPaths.BackupsRoot,prefix+"*.zip").Any()) return;
        CreateDatabaseBackup("quotidienne",true);
    }

    static void Rotate(int keep)
    {
        var files=Directory.EnumerateFiles(AppPaths.BackupsRoot,"PiiWii-Documents-*.zip").Select(x=>new FileInfo(x)).OrderByDescending(x=>x.CreationTimeUtc).ToList();
        foreach(var f in files.Skip(keep)) try{f.Delete();}catch{}
    }
}
