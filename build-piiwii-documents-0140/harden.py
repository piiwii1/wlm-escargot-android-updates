from pathlib import Path
root=Path('build-piiwii-documents-0140/work')
services=root/'src/PiiWii.Documents/Services'

# Replace bridge with scoped, permission-aware implementation.
(services/'BridgeService.cs').write_text((Path('build-piiwii-documents-0140/overlay/Services')/'BridgeService.cs').read_text())

# Schema migration for bridge scope fields.
p=services/'DatabaseService.cs';s=p.read_text()
needle='        EnsureColumn(c,"documents","batch_id","TEXT NOT NULL DEFAULT \'\'");\n'
if needle in s and 'scope_type' not in s:
    s=s.replace(needle,needle+'        EnsureColumn(c,"bridge_tokens","scope_type","TEXT NOT NULL DEFAULT \'all\'");\n        EnsureColumn(c,"bridge_tokens","scope_value","TEXT NOT NULL DEFAULT \'\'");\n')
p.write_text(s)

# Backup before assistant result application.
p=services/'AssistantExchangeService.cs';s=p.read_text()
s=s.replace('    public static (int updated,int ignored,List<string> errors) ApplyResults(string path)\n    {\n        var errors=', '    public static (int updated,int ignored,List<string> errors) ApplyResults(string path)\n    {\n        BackupService.CreateDatabaseBackup("avant-application-chatgpt",true);\n        var errors=')
p.write_text(s)

# Backup before ChatGPT import pack.
p=services/'AssistantImportPackService.cs';s=p.read_text()
s=s.replace('    {\n        var tmp=Path.Combine(Path.GetTempPath(),"piiwii-pack-"', '    {\n        BackupService.CreateDatabaseBackup("avant-paquet-chatgpt",true);\n        var tmp=Path.Combine(Path.GetTempPath(),"piiwii-pack-"')
p.write_text(s)

# UI: backup before normal imports + bridge pairing scoped to current batch/document.
p=root/'src/PiiWii.Documents/MainForm.cs';s=p.read_text()
s=s.replace('async Task RunImport(IEnumerable<string> files){busy=true;progressBar.Visible=true;', 'async Task RunImport(IEnumerable<string> files){try{BackupService.CreateDatabaseBackup("avant-import",true);}catch{} busy=true;progressBar.Visible=true;')
old='void ShowBridge(){var b=BridgeService.Instance;var code=b.GeneratePairCode();MessageBox.Show($"Pont sécurisé local : {(b.Running?"ACTIF":"INACTIF")}\\nAdresse : http://127.0.0.1:{b.Port}/api/v1/\\n\\nCode d\'appairage : {code}\\nValable 5 minutes.\\n\\nLe pont n\'accepte aucune commande système et n\'expose que PiiWii Documents.","Pont sécurisé",MessageBoxButtons.OK,MessageBoxIcon.Information);}'
new='void ShowBridge(){var b=BridgeService.Instance;string scopeType="all",scopeValue="",scopeLabel="toute l\'archive";var batch=!string.IsNullOrWhiteSpace(lastBatchId)?lastBatchId:(selectedId is long sid?DatabaseService.Get(sid)?.BatchId:"");if(!string.IsNullOrWhiteSpace(batch)){scopeType="batch";scopeValue=batch;scopeLabel=$"lot {batch}";}else if(selectedId is long did){scopeType="document";scopeValue=did.ToString();scopeLabel=$"document #{did}";}var code=b.GeneratePairCode(scopeType,scopeValue);MessageBox.Show($"Pont sécurisé local : {(b.Running?"ACTIF":"INACTIF")}\\nAdresse : http://127.0.0.1:{b.Port}/api/v1/\\nAccès actifs : {b.ActiveTokenCount()}\\n\\nCode d\'appairage : {code}\\nValable 5 minutes.\\nPortée : {scopeLabel}\\nPermissions : lecture + recherche uniquement.\\n\\nAucune commande système, aucune écriture via le pont.","Pont sécurisé",MessageBoxButtons.OK,MessageBoxIcon.Information);}'
if old in s:s=s.replace(old,new)
else: print('WARN ShowBridge signature not found')
p.write_text(s)
