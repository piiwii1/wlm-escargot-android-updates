using Microsoft.Data.Sqlite;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using PiiWii.Documents.Models;

namespace PiiWii.Documents.Services;

public sealed class BridgeService
{
    public static BridgeService Instance { get; }=new();
    private HttpListener? _listener; private CancellationTokenSource? _cts;
    public int Port { get; }=47831;
    public bool Running => _listener?.IsListening==true;
    private string PairFile => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"PiiWii Documents","bridge.key");
    private sealed record PairRequest(string Code,DateTime ExpiresAt,string ScopeType,string ScopeValue);
    private sealed record TokenInfo(string Permissions,string ScopeType,string ScopeValue,DateTime ExpiresAt);

    public void Start(){if(!AppPaths.BridgeEnabled||Running)return;try{_listener=new HttpListener();_listener.Prefixes.Add($"http://127.0.0.1:{Port}/");_listener.Start();_cts=new();_ = Task.Run(()=>Loop(_cts.Token));}catch{Stop();}}
    public void Stop(){try{_cts?.Cancel();_listener?.Stop();_listener?.Close();}catch{} _listener=null;}

    public string GeneratePairCode(string scopeType="all",string scopeValue="")
    {
        if(scopeType is not ("all" or "batch" or "document"))throw new ArgumentException("Portée invalide.");
        var code=RandomNumberGenerator.GetInt32(100000,1000000).ToString();
        File.WriteAllText(PairFile,JsonSerializer.Serialize(new PairRequest(code,DateTime.UtcNow.AddMinutes(5),scopeType,scopeValue??"")));
        return code;
    }

    public int ActiveTokenCount(){try{using var cn=new SqliteConnection($"Data Source={AppPaths.DatabasePath}");cn.Open();var cmd=cn.CreateCommand();cmd.CommandText="SELECT COUNT(*) FROM bridge_tokens WHERE revoked=0 AND expires_at>$now";cmd.Parameters.AddWithValue("$now",DateTime.UtcNow.ToString("O"));return Convert.ToInt32(cmd.ExecuteScalar()??0);}catch{return 0;}}
    public int RevokeAll(){try{using var cn=new SqliteConnection($"Data Source={AppPaths.DatabasePath}");cn.Open();var cmd=cn.CreateCommand();cmd.CommandText="UPDATE bridge_tokens SET revoked=1 WHERE revoked=0";var n=cmd.ExecuteNonQuery();DatabaseService.AuditExternal("Utilisateur","Accès pont révoqués",null,$"{n} session(s)");return n;}catch{return 0;}}

    private async Task Loop(CancellationToken ct){while(!ct.IsCancellationRequested&&_listener?.IsListening==true){HttpListenerContext ctx;try{ctx=await _listener.GetContextAsync();}catch{break;}_=Task.Run(()=>Handle(ctx),ct);}}
    private async Task Handle(HttpListenerContext c)
    {
        try
        {
            c.Response.Headers["X-Content-Type-Options"]="nosniff";c.Response.Headers["Cache-Control"]="no-store";
            var path=c.Request.Url?.AbsolutePath??"/";
            if(path=="/api/v1/health"){await Json(c,new{ok=true,app="PiiWii Documents",version="0.14.0",scope="local-only",write=false});return;}
            if(path=="/api/v1/pair"&&c.Request.HttpMethod=="POST")
            {
                using var sr=new StreamReader(c.Request.InputStream,c.Request.ContentEncoding);var body=await sr.ReadToEndAsync();var req=JsonSerializer.Deserialize<Dictionary<string,string>>(body)??new();
                if(!req.TryGetValue("code",out var code)||!ValidateCode(code,out var pair)){await Error(c,401,"Code invalide ou expiré");return;}
                var token=Convert.ToHexString(RandomNumberGenerator.GetBytes(32)).ToLowerInvariant();await SaveToken(token,"read,search",pair.ScopeType,pair.ScopeValue,DateTime.UtcNow.AddHours(1));try{File.Delete(PairFile);}catch{}
                await Json(c,new{token,expiresInSeconds=3600,permissions=new[]{"read","search"},scope=new{type=pair.ScopeType,value=pair.ScopeValue}});return;
            }
            var tok=c.Request.Headers["Authorization"]?.Replace("Bearer ","",StringComparison.OrdinalIgnoreCase);if(string.IsNullOrWhiteSpace(tok)||GetToken(tok) is not TokenInfo info){await Error(c,401,"Non autorisé");return;}
            if(path=="/api/v1/documents"&&c.Request.HttpMethod=="GET")
            {
                if(!HasPermission(info,"search")){await Error(c,403,"Permission search requise");return;}
                var q=c.Request.QueryString["q"]??"";var docs=ScopedSearch(info,q).Select(d=>new{d.Id,d.Title,d.DocumentType,d.Category,d.Organization,d.Amount,d.Currency,d.DocumentDate,d.Status,d.Tags,d.Vehicle,d.Reference,d.Confidence,d.AnalysisStatus});DatabaseService.AuditExternal("Bridge","Recherche",null,$"Scope={info.ScopeType}:{info.ScopeValue}; q={q}");await Json(c,docs);return;
            }
            if(path.StartsWith("/api/v1/documents/",StringComparison.Ordinal)&&c.Request.HttpMethod=="GET")
            {
                if(!HasPermission(info,"read")){await Error(c,403,"Permission read requise");return;}
                var tail=path[18..];if(long.TryParse(tail,out var id)){var d=DatabaseService.Get(id);if(d==null||!Allowed(info,d)){await Error(c,404,"Introuvable");return;}DatabaseService.AuditExternal("Bridge","Lecture métadonnées",id,$"Scope={info.ScopeType}:{info.ScopeValue}; {d.Title}");await Json(c,new{d.Id,d.Title,d.DocumentType,d.Category,d.Organization,d.Description,d.OriginalName,d.Sha256,d.OcrText,d.Tags,d.Vehicle,d.Reference,d.Confidence,d.AnalysisStatus,d.PageCount,d.Amount,d.Currency,d.DocumentDate,d.ImportedAt,d.Status,d.PaperOriginal});return;}
            }
            await Error(c,404,"Route inconnue");
        }catch(Exception ex){await Error(c,500,ex.Message);}finally{try{c.Response.Close();}catch{}}
    }

    private IEnumerable<DocumentRecord> ScopedSearch(TokenInfo info,string q)
    {
        IEnumerable<DocumentRecord> docs=info.ScopeType switch{"batch"=>DatabaseService.GetBatch(info.ScopeValue),"document"=>long.TryParse(info.ScopeValue,out var id)&&DatabaseService.Get(id) is{} d?new[]{d}:Array.Empty<DocumentRecord>(),_=>DatabaseService.Search(q)};
        if(info.ScopeType=="all"||string.IsNullOrWhiteSpace(q))return docs;
        q=q.Trim();return docs.Where(d=>($"{d.Title} {d.DocumentType} {d.Category} {d.Organization} {d.Description} {d.OcrText} {d.Tags} {d.Vehicle} {d.Reference}").Contains(q,StringComparison.OrdinalIgnoreCase));
    }
    private static bool Allowed(TokenInfo info,DocumentRecord d)=>info.ScopeType switch{"all"=>true,"batch"=>string.Equals(d.BatchId,info.ScopeValue,StringComparison.Ordinal),"document"=>long.TryParse(info.ScopeValue,out var id)&&d.Id==id,_=>false};
    private static bool HasPermission(TokenInfo info,string p)=>info.Permissions.Split(',',StringSplitOptions.RemoveEmptyEntries|StringSplitOptions.TrimEntries).Contains(p,StringComparer.OrdinalIgnoreCase);
    private bool ValidateCode(string code,out PairRequest pair){pair=new PairRequest("",DateTime.MinValue,"all","");try{if(!File.Exists(PairFile))return false;pair=JsonSerializer.Deserialize<PairRequest>(File.ReadAllText(PairFile))!;return pair!=null&&CryptographicOperations.FixedTimeEquals(Encoding.UTF8.GetBytes(pair.Code),Encoding.UTF8.GetBytes(code))&&pair.ExpiresAt.ToUniversalTime()>DateTime.UtcNow;}catch{return false;}}
    private async Task SaveToken(string token,string perms,string scopeType,string scopeValue,DateTime exp){using var cn=new SqliteConnection($"Data Source={AppPaths.DatabasePath}");cn.Open();var cmd=cn.CreateCommand();cmd.CommandText="INSERT INTO bridge_tokens(token_hash,permissions,scope_type,scope_value,expires_at,created_at,revoked) VALUES($h,$p,$st,$sv,$e,$c,0)";cmd.Parameters.AddWithValue("$h",Hash(token));cmd.Parameters.AddWithValue("$p",perms);cmd.Parameters.AddWithValue("$st",scopeType);cmd.Parameters.AddWithValue("$sv",scopeValue);cmd.Parameters.AddWithValue("$e",exp.ToString("O"));cmd.Parameters.AddWithValue("$c",DateTime.UtcNow.ToString("O"));await cmd.ExecuteNonQueryAsync();}
    private TokenInfo? GetToken(string token){try{using var cn=new SqliteConnection($"Data Source={AppPaths.DatabasePath}");cn.Open();var cmd=cn.CreateCommand();cmd.CommandText="SELECT permissions,scope_type,scope_value,expires_at,revoked FROM bridge_tokens WHERE token_hash=$h LIMIT 1";cmd.Parameters.AddWithValue("$h",Hash(token));using var r=cmd.ExecuteReader();if(!r.Read()||r.GetInt32(4)!=0)return null;var exp=DateTime.Parse(r.GetString(3)).ToUniversalTime();if(exp<=DateTime.UtcNow)return null;return new TokenInfo(r.GetString(0),r[1]?.ToString()??"all",r[2]?.ToString()??"",exp);}catch{return null;}}
    private static string Hash(string s)=>Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(s))).ToLowerInvariant();
    private static async Task Json(HttpListenerContext c,object o){var b=Encoding.UTF8.GetBytes(JsonSerializer.Serialize(o));c.Response.ContentType="application/json; charset=utf-8";c.Response.ContentLength64=b.Length;await c.Response.OutputStream.WriteAsync(b);}
    private static async Task Error(HttpListenerContext c,int code,string msg){c.Response.StatusCode=code;await Json(c,new{error=msg});}
}
