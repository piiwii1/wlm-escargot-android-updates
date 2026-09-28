from pathlib import Path
root=Path('build-piiwii-documents-0180/work')

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/Services/BridgeService.cs','src/PiiWii.Documents/Services/AssistantExchangeService.cs','src/PiiWii.Documents/Services/BackupService.cs','src/PiiWii.Documents/Services/AssistantUploadPackService.cs']:
    p=root/rel
    if p.exists(): p.write_text(p.read_text().replace('0.17.0.0','0.18.0.0').replace('0.17.0','0.18.0'))

p=root/'src/PiiWii.Documents/MainForm.cs'
s=p.read_text().replace('PiiWii Documents 0.17.0','PiiWii Documents 0.18.0').replace('v0.17.0','v0.18.0')

s=s.replace('Créer un paquet à envoyer à ChatGPT','Envoyer à ChatGPT')
s=s.replace('Importer le retour ChatGPT','Récupérer le tri de ChatGPT')
s=s.replace('Importer un paquet préparé par ChatGPT','Importer des documents préparés par ChatGPT')
s=s.replace('moreMenu.Items.Add("Importer le classement ChatGPT",null,(_,_)=>ImportAiResults());','')

for line in [
'        moreMenu.Items.Add("Pont sécurisé",null,(_,_)=>ShowBridge());\n',
'        moreMenu.Items.Add("Voir / révoquer les accès du pont",null,(_,_)=>ManageBridgeSessions());\n',
'        moreMenu.Items.Add("Révoquer les accès du pont",null,(_,_)=>RevokeBridge());\n',
'        moreMenu.Items.Add("Créer une sauvegarde maintenant",null,(_,_)=>CreateBackup());\n',
'        moreMenu.Items.Add("Ouvrir les sauvegardes",null,(_,_)=>Process.Start(new ProcessStartInfo(AppPaths.BackupsRoot){UseShellExecute=true}));\n',
'        moreMenu.Items.Add("Restaurer une sauvegarde",null,(_,_)=>RestoreBackup());\n',
'        moreMenu.Items.Add("Diagnostic complet",null,(_,_)=>RunDiagnostics());\n',
'        moreMenu.Items.Add("Tester aller-retour ChatGPT",null,(_,_)=>TestChatGptRoundTrip());\n']:
    s=s.replace(line,'')

advanced='''        var advancedMenu=new ToolStripMenuItem("Avancé");\n        advancedMenu.DropDownItems.Add("Pont local",null,(_,_)=>ShowBridge());\n        advancedMenu.DropDownItems.Add("Voir / révoquer les accès du pont",null,(_,_)=>ManageBridgeSessions());\n        advancedMenu.DropDownItems.Add("Révoquer tous les accès du pont",null,(_,_)=>RevokeBridge());\n        advancedMenu.DropDownItems.Add(new ToolStripSeparator());\n        advancedMenu.DropDownItems.Add("Créer une sauvegarde",null,(_,_)=>CreateBackup());\n        advancedMenu.DropDownItems.Add("Ouvrir les sauvegardes",null,(_,_)=>Process.Start(new ProcessStartInfo(AppPaths.BackupsRoot){UseShellExecute=true}));\n        advancedMenu.DropDownItems.Add("Restaurer une sauvegarde",null,(_,_)=>RestoreBackup());\n        advancedMenu.DropDownItems.Add(new ToolStripSeparator());\n        advancedMenu.DropDownItems.Add("Diagnostic complet",null,(_,_)=>RunDiagnostics());\n        advancedMenu.DropDownItems.Add("Tester aller-retour ChatGPT",null,(_,_)=>TestChatGptRoundTrip());\n        advancedMenu.DropDownItems.Add("Importer ancien classement JSON",null,(_,_)=>ImportAiResults());\n        moreMenu.Items.Add(advancedMenu);\n'''
anchor='        moreMenu.Items.Add("Récupérer le tri de ChatGPT",null,(_,_)=>ImportChatGptReturn());\n'
if anchor in s and 'var advancedMenu=new ToolStripMenuItem("Avancé")' not in s:
    s=s.replace(anchor,anchor+advanced)
else:
    print('WARN simple-mode anchor not found or already patched')

s=s.replace('Paquet pour ChatGPT','Envoyer à ChatGPT')
s=s.replace('Retour ChatGPT refusé','Tri ChatGPT refusé')
s=s.replace('Retour ChatGPT','Tri de ChatGPT')
p.write_text(s)
