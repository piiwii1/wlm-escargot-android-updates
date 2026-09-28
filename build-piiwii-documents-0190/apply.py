from pathlib import Path
root=Path('build-piiwii-documents-0190/work')

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/MainForm.cs','src/PiiWii.Documents/Services/BridgeService.cs','src/PiiWii.Documents/Services/AssistantExchangeService.cs','src/PiiWii.Documents/Services/BackupService.cs','src/PiiWii.Documents/Services/AssistantUploadPackService.cs']:
    p=root/rel
    if p.exists(): p.write_text(p.read_text().replace('0.18.0.0','0.19.0.0').replace('0.18.0','0.19.0'))

svc=root/'src/PiiWii.Documents/Services/StartupReturnService.cs'
svc.write_text(r'''namespace PiiWii.Documents.Services;

public static class StartupReturnService
{
    public static bool IsSupported(string path)=>File.Exists(path)&&string.Equals(Path.GetExtension(path),".piiwii-docs",StringComparison.OrdinalIgnoreCase);
    public static bool Handle(string path)
    {
        if(!IsSupported(path))return false;
        try
        {
            var p=AssistantReturnPackService.Inspect(path);
            var sample=p.Changes.Count==0?"Aucune modification détectée.":string.Join("\n",p.Changes.Take(10));
            if(MessageBox.Show($"Tri reçu de ChatGPT\n\nModifications proposées : {p.Proposed}\nIgnorées : {p.Ignored}\n\n{sample}\n\nAppliquer ce tri ?","PiiWii Documents",MessageBoxButtons.YesNo,MessageBoxIcon.Question)!=DialogResult.Yes)return true;
            var r=AssistantReturnPackService.Apply(path);
            MessageBox.Show($"Tri appliqué.\n\nDocuments mis à jour : {r.updated}\nIgnorés : {r.ignored}\nErreurs : {r.errors.Count}","PiiWii Documents",MessageBoxButtons.OK,r.errors.Count==0?MessageBoxIcon.Information:MessageBoxIcon.Warning);
            return true;
        }
        catch(Exception ex){MessageBox.Show(ex.Message,"Retour ChatGPT refusé",MessageBoxButtons.OK,MessageBoxIcon.Error);return true;}
    }
}
''')

p=root/'src/PiiWii.Documents/Program.cs';s=p.read_text()
s=s.replace('static void Main()','static void Main(string[] args)')
needle='        Application.Run(new MainForm());'
repl='        if(args.Length>0 && StartupReturnService.IsSupported(args[0])) StartupReturnService.Handle(args[0]);\n        Application.Run(new MainForm());'
if needle not in s: raise SystemExit('Program anchor missing')
p.write_text(s.replace(needle,repl))

p=root/'src/PiiWii.Documents.Installer/Program.cs';s=p.read_text()
needle='        k.SetValue("NoModify",1,RegistryValueKind.DWord); k.SetValue("NoRepair",1,RegistryValueKind.DWord);'
repl='''        k.SetValue("NoModify",1,RegistryValueKind.DWord); k.SetValue("NoRepair",1,RegistryValueKind.DWord);
        var exe=Path.Combine(dir,"PiiWii.Documents.exe");
        using(var ext=Registry.LocalMachine.CreateSubKey(@"Software\\Classes\\.piiwii-docs")) ext.SetValue("","PiiWiiDocuments.ChatGPTReturn");
        using(var cls=Registry.LocalMachine.CreateSubKey(@"Software\\Classes\\PiiWiiDocuments.ChatGPTReturn")){cls.SetValue("","Retour ChatGPT PiiWii Documents");using(var ico=cls.CreateSubKey("DefaultIcon"))ico.SetValue("",exe+",0");using(var cmd=cls.CreateSubKey(@"shell\\open\\command"))cmd.SetValue("","\\\""+exe+"\\\" \\\"%1\\\"");}'''
if needle not in s: raise SystemExit('Installer install anchor missing')
s=s.replace(needle,repl)
needle='        try { Registry.LocalMachine.DeleteSubKeyTree(@"Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\PiiWiiDocuments",false); } catch { }'
repl=needle+'\n        try { Registry.LocalMachine.DeleteSubKeyTree(@"Software\\Classes\\PiiWiiDocuments.ChatGPTReturn",false); } catch { }\n        try { Registry.LocalMachine.DeleteSubKeyTree(@"Software\\Classes\\.piiwii-docs",false); } catch { }'
s=s.replace(needle,repl)
p.write_text(s)
