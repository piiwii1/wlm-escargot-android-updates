from pathlib import Path
root=Path('build-piiwii-documents-090/work')

def rw(rel,pairs):
    p=root/rel; s=p.read_text()
    for a,b in pairs:
        if a not in s: print('WARN missing',rel,a[:100])
        s=s.replace(a,b)
    p.write_text(s)

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/Services/BridgeService.cs']:
    rw(rel,[('0.8.0','0.9.0')])
ax=root/'src/PiiWii.Documents/Services/AssistantExchangeService.cs'
if ax.exists(): ax.write_text(ax.read_text().replace('0.8.0','0.9.0'))

p=root/'src/PiiWii.Documents/MainForm.cs'; s=p.read_text()
s=s.replace('PiiWii Documents 0.8.0','PiiWii Documents 0.9.0').replace('v0.8.0','v0.9.0')

s=s.replace('readonly Label activeView=new(){AutoSize=true,Font=new("Segoe UI Semibold",10),ForeColor=Color.FromArgb(27,111,238),Text="Vue : Tous les documents",TextAlign=ContentAlignment.MiddleRight};', 'readonly Label activeView=new(){AutoSize=true,Font=new("Segoe UI Semibold",10),ForeColor=Color.FromArgb(27,111,238),Text="Vue : Tous les documents",TextAlign=ContentAlignment.MiddleRight};\n    readonly Dictionary<string,Button> smartButtons=new(StringComparer.OrdinalIgnoreCase);')

s=s.replace('nav.Controls.Add(Nav("⚠   À vérifier",()=>Filter("À vérifier","À vérifier")));', 'nav.Controls.Add(SmartNav("review","⚠   À vérifier","À vérifier",()=>Filter("À vérifier","À vérifier")));')
s=s.replace('nav.Controls.Add(Nav("▣   À payer",()=>Filter("À payer","À payer")));', 'nav.Controls.Add(SmartNav("topay","▣   À payer","À payer",()=>Filter("À payer","À payer")));')

s=s.replace('categoryPanel.Controls.Add(CategoryHeader("ORGANISMES"));', 'categoryPanel.Controls.Add(CategoryHeader("DOSSIERS INTELLIGENTS"));')
s=s.replace('categoryPanel.Controls.Add(Category("Allianz",()=>Filter("Allianz","Organisme : Allianz"),true));', 'categoryPanel.Controls.Add(SmartCategory("Allianz","Allianz",()=>Filter("Allianz","Dossier : Allianz"),true));')
s=s.replace('categoryPanel.Controls.Add(Category("Helvetia",()=>Filter("Helvetia","Organisme : Helvetia")));', 'categoryPanel.Controls.Add(SmartCategory("Helvetia","Helvetia",()=>Filter("Helvetia","Dossier : Helvetia")));')
s=s.replace('categoryPanel.Controls.Add(Category("Swisscom",()=>Filter("Swisscom","Organisme : Swisscom")));', 'categoryPanel.Controls.Add(SmartCategory("Swisscom","Swisscom",()=>Filter("Swisscom","Dossier : Swisscom")));')
s=s.replace('categoryPanel.Controls.Add(Category("État du Valais",()=>Filter("État du Valais","Organisme : État du Valais")));', 'categoryPanel.Controls.Add(SmartCategory("État du Valais","État du Valais",()=>Filter("État du Valais","Dossier : État du Valais")));\n        categoryPanel.Controls.Add(SmartCategory("Amendes","Amendes",()=>Filter("Amendes","Dossier : Amendes"),true));\n        categoryPanel.Controls.Add(SmartCategory("Recours","Recours",()=>Filter("Recours","Dossier : Recours"),true));')

marker='Button Category(string text,Action a,bool strong=false){'
if marker in s:
    helper='''Button SmartNav(string key,string label,string query,Action a){var b=Nav(label,a);smartButtons[key]=b;b.Tag=(label,query);return b;}\n    Button SmartCategory(string key,string label,Action a,bool strong=false){var b=Category(label,a,strong);smartButtons[key]=b;b.Tag=(label,label);return b;}\n    int CountQuery(string q){try{return DatabaseService.Search(q,false).Count;}catch{return 0;}}\n    void RefreshSmartCounts(){Dictionary<string,int>? ds=null;try{ds=DatabaseService.Dashboard();}catch{}foreach(var kv in smartButtons){if(kv.Value.Tag is ValueTuple<string,string> t){var count=kv.Key=="review"&&ds!=null?ds["review"]:kv.Key=="topay"&&ds!=null?ds["topay"]:CountQuery(t.Item2);kv.Value.Text=$"{t.Item1}   ({count})";}}}\n    '''
    s=s.replace(marker,helper+marker)

s=s.replace('stats.Text=$"{ds["total"]} document(s)  •  {ds["review"]} à vérifier  •  {ds["topay"]} à payer  •  clic sur une catégorie ou un organisme pour filtrer";', 'stats.Text=$"{ds["total"]} document(s)  •  {ds["review"]} à vérifier  •  {ds["topay"]} à payer  •  dossiers intelligents à gauche";RefreshSmartCounts();')

s=s.replace('grid.ContextMenuStrip=moreMenu;', 'grid.ContextMenuStrip=moreMenu;grid.CellPainting+=GridCellPainting;')
insert='''\n    void GridCellPainting(object? sender,DataGridViewCellPaintingEventArgs e){\n        if(e.RowIndex<0||e.ColumnIndex!=5)return;\n        e.PaintBackground(e.CellBounds,true);\n        var text=Convert.ToString(e.FormattedValue)??"";\n        if(string.IsNullOrWhiteSpace(text)){e.Handled=true;return;}\n        var low=text.ToLowerInvariant();\n        Color bg,fg;\n        if(low.Contains("payé")||low=="clos"){bg=Color.FromArgb(221,246,231);fg=Color.FromArgb(22,101,52);}\n        else if(low.Contains("à payer")){bg=Color.FromArgb(255,226,230);fg=Color.FromArgb(170,35,52);}\n        else if(low.Contains("recours")||low.Contains("contest")||low.Contains("attente")){bg=Color.FromArgb(237,230,255);fg=Color.FromArgb(92,56,160);}\n        else if(low.Contains("vérifier")){bg=Color.FromArgb(255,242,204);fg=Color.FromArgb(133,86,0);}\n        else {bg=Color.FromArgb(232,238,247);fg=Color.FromArgb(60,72,92);}\n        var r=new Rectangle(e.CellBounds.X+8,e.CellBounds.Y+8,Math.Max(30,e.CellBounds.Width-16),Math.Max(20,e.CellBounds.Height-16));\n        using var path=new System.Drawing.Drawing2D.GraphicsPath();var radius=10;path.AddArc(r.X,r.Y,radius*2,radius*2,180,90);path.AddArc(r.Right-radius*2,r.Y,radius*2,radius*2,270,90);path.AddArc(r.Right-radius*2,r.Bottom-radius*2,radius*2,radius*2,0,90);path.AddArc(r.X,r.Bottom-radius*2,radius*2,radius*2,90,90);path.CloseFigure();\n        using var br=new SolidBrush(bg);e.Graphics.FillPath(br,path);TextRenderer.DrawText(e.Graphics,text,new Font("Segoe UI Semibold",8.5f),r,fg,TextFormatFlags.HorizontalCenter|TextFormatFlags.VerticalCenter|TextFormatFlags.EndEllipsis);e.Handled=true;\n    }\n'''
pos=s.find('    async Task Import()')
if pos!=-1: s=s[:pos]+insert+s[pos:]

s=s.replace('detailCategory.Text=$"Catégorie :', 'detailCategory.Text=$"📁  Catégorie :')
s=s.replace('detailOrg.Text=$"Organisme :', 'detailOrg.Text=$"🏢  Organisme :')
s=s.replace('detailType.Text=$"Type :', 'detailType.Text=$"📄  Type :')
s=s.replace('detailVehicle.Text=$"Véhicule :', 'detailVehicle.Text=$"🚗  Véhicule :')
s=s.replace('detailStatus.Text=$"Statut :', 'detailStatus.Text=$"●  Statut :')
s=s.replace('detailMeta.Text=$"', 'detailMeta.Text=$"🗂  Dossier documentaire\\r\\n"+')

p.write_text(s)
(root/'README.md').write_text('# PiiWii Documents 0.9.0\n\nFinition ergonomique : compteurs dynamiques, dossiers intelligents, badges d’état colorés et fiche documentaire plus lisible.\n')
(root/'CHANGELOG.md').write_text('''# Changelog\n\n## 0.9.0\n- Compteurs dynamiques sur À vérifier, À payer et les dossiers intelligents.\n- Dossiers intelligents Allianz, Helvetia, Swisscom, État du Valais, Amendes et Recours.\n- Badges colorés dans la colonne État pour Payé, À payer, À vérifier, recours/contestation/attente et Clos.\n- Fiche de droite reformulée comme un dossier documentaire avec repères visuels.\n- Navigation plus immédiate, sans ajouter d’écran complexe.\n- Import, OCR, classement, pont sécurisé et données existantes conservés.\n- Mise à jour compatible avec les données 0.1 à 0.8.\n''')
