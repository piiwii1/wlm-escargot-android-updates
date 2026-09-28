using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text.Json;
using PiiWii.Documents.Models;

namespace PiiWii.Documents.Services;

public sealed class DocumentSyncService : IDisposable
{
    public static DocumentSyncService Instance { get; } = new();
    const string BaseUrl="https://piiwii.ch/wp-json/piiwii-documents/v1";
    readonly HttpClient http=new(){Timeout=TimeSpan.FromSeconds(20)};
    readonly CancellationTokenSource cts=new();
    Task? loop;
    SyncConfig? config;
    public bool Connected { get; private set; }
    public event Action<string>? StatusChanged;

    sealed record SyncConfig(string DeviceId,string Secret,string Label);
    sealed class PollResponse { public List<SyncRequest> Commands { get; set; } = new(); }
    sealed class SyncRequest { public string Id { get; set; }=""; public string Action { get; set; }=""; public JsonElement Args { get; set; } }

    DocumentSyncService(){}

    public void Start(){if(loop!=null)return;config=LoadOrCreate();loop=Task.Run(()=>Loop(cts.Token));}

    async Task Loop(CancellationToken ct)
    {
        while(!ct.IsCancellationRequested)
        {
            try
            {
                config??=LoadOrCreate();
                await Register(ct);
                var response=await http.PostAsJsonAsync(BaseUrl+"/device/poll",new{device_id=config.DeviceId,secret=config.Secret},ct);
                response.EnsureSuccessStatusCode();
                var poll=await response.Content.ReadFromJsonAsync<PollResponse>(cancellationToken:ct)??new();
                Connected=true;StatusChanged?.Invoke("connecté");
                foreach(var request in poll.Commands.Take(10))await Process(request,ct);
            }
            catch(OperationCanceledException) when(ct.IsCancellationRequested){break;}
            catch{Connected=false;StatusChanged?.Invoke("hors ligne");}
            try{await Task.Delay(TimeSpan.FromSeconds(5),ct);}catch(OperationCanceledException){break;}
        }
    }

    async Task Register(CancellationToken ct)
    {
        if(config==null)return;
        var r=await http.PostAsJsonAsync(BaseUrl+"/device/register",new{device_id=config.DeviceId,secret=config.Secret,label=config.Label,version="0.20.0"},ct);
        r.EnsureSuccessStatusCode();
    }

    async Task Process(SyncRequest request,CancellationToken ct)
    {
        object result;bool ok=true;string? error=null;
        try{result=Execute(request);}catch(Exception ex){ok=false;error=ex.Message;result=new{};}
        try
        {
            if(config==null)return;
            await http.PostAsJsonAsync(BaseUrl+"/device/result",new{device_id=config.DeviceId,secret=config.Secret,command_id=request.Id,ok,error,result},ct);
        }
        catch{}
    }

    object Execute(SyncRequest request)=>request.Action switch
    {
        "health"=>Health(),
        "search"=>Search(request.Args),
        "get_document"=>GetDocument(request.Args),
        "update_document"=>UpdateDocument(request.Args),
        _=>throw new InvalidOperationException("Action documentaire non autorisée.")
    };

    object Health(){var d=DatabaseService.Dashboard();return new{version="0.20.0",total=d["total"],review=d["review"],toPay=d["topay"]};}

    object Search(JsonElement args)
    {
        var q=args.TryGetProperty("query",out var e)&&e.ValueKind==JsonValueKind.String?e.GetString()??"":"";
        var limit=args.TryGetProperty("limit",out var l)&&l.TryGetInt32(out var n)?Math.Clamp(n,1,50):30;
        return DatabaseService.Search(q).Take(limit).Select(d=>new{d.Id,d.Title,d.Category,d.Organization,d.DocumentType,d.Reference,d.Amount,d.Currency,documentDate=d.DocumentDate?.ToString("yyyy-MM-dd"),d.Status,d.Vehicle,d.Tags,d.Confidence,d.AnalysisStatus,d.PageCount}).ToArray();
    }

    object GetDocument(JsonElement args)
    {
        if(!args.TryGetProperty("id",out var e)||!e.TryGetInt64(out var id))throw new InvalidOperationException("ID document manquant.");
        var d=DatabaseService.Get(id)??throw new InvalidOperationException("Document introuvable.");
        return new{d.Id,d.Title,d.Category,d.Organization,d.Description,d.DocumentType,d.Reference,d.Amount,d.Currency,documentDate=d.DocumentDate?.ToString("yyyy-MM-dd"),d.Status,d.Vehicle,d.Tags,d.Confidence,d.AnalysisStatus,d.PageCount,d.OcrText,relations=DatabaseService.GetLinks(id).Select(x=>new{x.id,x.title,x.type,x.confidence}).ToArray()};
    }

    object UpdateDocument(JsonElement args)
    {
        if(!args.TryGetProperty("id",out var e)||!e.TryGetInt64(out var id))throw new InvalidOperationException("ID document manquant.");
        var d=DatabaseService.Get(id)??throw new InvalidOperationException("Document introuvable.");
        BackupService.CreateDatabaseBackup("avant-sync-chatgpt",true);
        Set("title",v=>d.Title=v);Set("category",v=>d.Category=v);Set("organization",v=>d.Organization=v);Set("description",v=>d.Description=v);Set("documentType",v=>d.DocumentType=v);Set("reference",v=>d.Reference=v);Set("currency",v=>d.Currency=v);Set("status",v=>d.Status=v);Set("vehicle",v=>d.Vehicle=v);Set("tags",v=>d.Tags=v);
        if(args.TryGetProperty("amount",out var am)){if(am.ValueKind==JsonValueKind.Null)d.Amount=null;else if(am.TryGetDecimal(out var av))d.Amount=av;}
        if(args.TryGetProperty("documentDate",out var de)&&de.ValueKind==JsonValueKind.String){var ds=de.GetString();d.DocumentDate=DateTime.TryParse(ds,out var dt)?dt.Date:null;}
        if(args.TryGetProperty("confidence",out var cf)&&cf.TryGetInt32(out var cv))d.Confidence=Math.Clamp(cv,0,100);
        d.AnalysisStatus="Classé par ChatGPT (sync)";
        DatabaseService.Update(d,"ChatGPT sync","Correction métadonnées");
        return new{updated=true,id=d.Id,d.Title,d.Category,d.Organization,d.DocumentType,d.Reference,d.Amount,d.Status};
        void Set(string name,Action<string> apply){if(args.TryGetProperty(name,out var x)&&x.ValueKind==JsonValueKind.String)apply((x.GetString()??"").Trim());}
    }

    SyncConfig LoadOrCreate()
    {
        var dir=Path.Combine(AppPaths.DataRoot,"Assistant");Directory.CreateDirectory(dir);var path=Path.Combine(dir,"relay.json");
        try{if(File.Exists(path)){var c=JsonSerializer.Deserialize<SyncConfig>(File.ReadAllText(path));if(c!=null&&!string.IsNullOrWhiteSpace(c.DeviceId)&&!string.IsNullOrWhiteSpace(c.Secret))return c;}}catch{}
        var cfg=new SyncConfig(Guid.NewGuid().ToString("N"),Convert.ToHexString(RandomNumberGenerator.GetBytes(32)).ToLowerInvariant(),Environment.MachineName);
        File.WriteAllText(path,JsonSerializer.Serialize(cfg,new JsonSerializerOptions{WriteIndented=true}));return cfg;
    }

    public void Dispose(){cts.Cancel();http.Dispose();cts.Dispose();}
}
