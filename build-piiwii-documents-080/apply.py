from pathlib import Path
root=Path('build-piiwii-documents-080/work')

def rw(rel,pairs):
    p=root/rel
    s=p.read_text()
    for a,b in pairs:
        if a not in s:
            print('WARN missing',rel,a[:80])
        s=s.replace(a,b)
    p.write_text(s)

rw('src/PiiWii.Documents/PiiWii.Documents.csproj',[('0.7.0','0.8.0')])
rw('src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj',[('0.7.0','0.8.0')])
rw('src/PiiWii.Documents.Installer/Program.cs',[('0.7.0','0.8.0')])
rw('src/PiiWii.Documents/Services/BridgeService.cs',[('0.7.0','0.8.0')])
ax=root/'src/PiiWii.Documents/Services/AssistantExchangeService.cs'
if ax.exists(): ax.write_text(ax.read_text().replace('0.7.0','0.8.0'))

p=root/'src/PiiWii.Documents/MainForm.cs'
s=p.read_text()
s=s.replace('PiiWii Documents 0.7.0','PiiWii Documents 0.8.0').replace('v0.7.0','v0.8.0')

s=s.replace('readonly Label detailMeta=new(){AutoSize=true,Padding=new(12,4,8,8),ForeColor=Color.DimGray};', 'readonly Label detailMeta=new(){AutoSize=true,Padding=new(12,4,8,8),ForeColor=Color.DimGray};\n    readonly Label activeView=new(){AutoSize=true,Font=new("Segoe UI Semibold",10),ForeColor=Color.FromArgb(27,111,238),Text="Vue : Tous les documents",TextAlign=ContentAlignment.MiddleRight};')

s=s.replace('main.Controls.Add(new Label{Text="Bienvenue !",Dock=DockStyle.Fill,Font=new("Segoe UI Semibold",21),ForeColor=Color.FromArgb(24,32,48),TextAlign=ContentAlignment.MiddleLeft},0,0);', 'var header=new TableLayoutPanel{Dock=DockStyle.Fill,ColumnCount=2}; header.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,100)); header.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize)); header.Controls.Add(new Label{Text="Bienvenue !",Dock=DockStyle.Fill,Font=new("Segoe UI Semibold",21),ForeColor=Color.FromArgb(24,32,48),TextAlign=ContentAlignment.MiddleLeft},0,0); header.Controls.Add(activeView,1,0); main.Controls.Add(header,0,0);')

s=s.replace('nav.Controls.Add(Nav("⌂   Accueil",()=>{trashMode=false;search.Text="";RefreshList();}));', 'nav.Controls.Add(Nav("⌂   Accueil",()=>{trashMode=false;activeView.Text="Vue : Accueil";search.Text="";RefreshList();}));')
s=s.replace('nav.Controls.Add(Nav("▤   Tous les documents",()=>{trashMode=false;search.Text="";RefreshList();}));', 'nav.Controls.Add(Nav("▤   Tous les documents",()=>{trashMode=false;activeView.Text="Vue : Tous les documents";search.Text="";RefreshList();}));')
s=s.replace('nav.Controls.Add(Nav("⚠   À vérifier",()=>Filter("À vérifier")));', 'nav.Controls.Add(Nav("⚠   À vérifier",()=>Filter("À vérifier","À vérifier")));')
s=s.replace('nav.Controls.Add(Nav("▣   À payer",()=>Filter("À payer")));', 'nav.Controls.Add(Nav("▣   À payer",()=>Filter("À payer","À payer")));')
s=s.replace('Filter(lastBatchId);', 'Filter(lastBatchId,"Dernier import");')

s=s.replace('categoryPanel.Controls.Add(Category("Autres",()=>Filter("Autres")));\n        nav.Controls.Add(categoryPanel);', 'categoryPanel.Controls.Add(Category("Autres",()=>Filter("Autres","Autres")));\n        categoryPanel.Controls.Add(CategoryHeader("ORGANISMES"));\n        categoryPanel.Controls.Add(Category("Allianz",()=>Filter("Allianz","Organisme : Allianz"),true));\n        categoryPanel.Controls.Add(Category("Helvetia",()=>Filter("Helvetia","Organisme : Helvetia")));\n        categoryPanel.Controls.Add(Category("Swisscom",()=>Filter("Swisscom","Organisme : Swisscom")));\n        categoryPanel.Controls.Add(Category("État du Valais",()=>Filter("État du Valais","Organisme : État du Valais")));\n        nav.Controls.Add(categoryPanel);')

# Make every category call set a friendly active-view label.
repls={
'Filter("Assurances")':'Filter("Assurances","Assurances")',
'Filter("Assurance véhicule")':'Filter("Assurance véhicule","Assurances › Véhicule")',
'Filter("Assurance ménage")':'Filter("Assurance ménage","Assurances › Ménage")',
'Filter("Assurance maladie")':'Filter("Assurance maladie","Assurances › Maladie")',
'Filter("Véhicules")':'Filter("Véhicules","Véhicules")',
'Filter("Service des automobiles")':'Filter("Service des automobiles","Service des automobiles")',
'Filter("Amendes")':'Filter("Amendes","Amendes")',
'Filter("Recours")':'Filter("Recours","Recours")',
'Filter("Télécom")':'Filter("Télécom","Télécom")',
'Filter("Impôts")':'Filter("Impôts","Impôts")',
'Filter("Santé")':'Filter("Santé","Santé")',
'Filter("Administration")':'Filter("Administration","Administration")',
}
for a,b in repls.items(): s=s.replace(a,b)

s=s.replace('void Filter(string value){trashMode=false;search.Text=value;RefreshList();}', 'void Filter(string value,string? label=null){trashMode=false;activeView.Text="Vue : "+(label??value);search.Text=value;RefreshList();}')

s=s.replace('status.Items.AddRange(new[]{"Information","À payer","Payé","Annulé"});', 'status.Items.AddRange(new[]{"Information","À payer","Payé","Annulé","Contesté","Recours envoyé","En attente de réponse","En recours","Clos"});')

old='var i=grid.Rows.Add(d.Title,catText,d.Organization,d.DocumentDate?.ToString("dd.MM.yyyy")??"—",amountText,d.Status);grid.Rows[i].Tag=d.Id;if(d.AnalysisStatus=="À vérifier"||d.AnalysisStatus=="Erreur OCR")grid.Rows[i].DefaultCellStyle.BackColor=Color.FromArgb(255,249,231);'
new='var i=grid.Rows.Add(d.Title,catText,d.Organization,d.DocumentDate?.ToString("dd.MM.yyyy")??"—",amountText,d.Status);grid.Rows[i].Tag=d.Id;var st=(d.Status??"").ToLowerInvariant();if(st.Contains("recours")||st.Contains("contest")||st.Contains("attente"))grid.Rows[i].DefaultCellStyle.BackColor=Color.FromArgb(244,240,255);else if(st.Contains("à payer"))grid.Rows[i].DefaultCellStyle.BackColor=Color.FromArgb(255,241,243);else if(st.Contains("payé"))grid.Rows[i].DefaultCellStyle.BackColor=Color.FromArgb(241,252,246);else if(d.AnalysisStatus=="À vérifier"||d.AnalysisStatus=="Erreur OCR")grid.Rows[i].DefaultCellStyle.BackColor=Color.FromArgb(255,249,231);'
s=s.replace(old,new)

s=s.replace('stats.Text=$"{ds["total"]} document(s) • {ds["review"]} à vérifier • {ds["topay"]} à payer";', 'stats.Text=$"{ds["total"]} document(s)  •  {ds["review"]} à vérifier  •  {ds["topay"]} à payer  •  clic sur une catégorie ou un organisme pour filtrer";')

(root/'src/PiiWii.Documents/MainForm.cs').write_text(s)
(root/'README.md').write_text('# PiiWii Documents 0.8.0\n\nNavigation métier améliorée : vue active visible, catégories et organismes cliquables, statuts spécifiques aux amendes et recours, couleurs de lecture plus claires.\n')
(root/'CHANGELOG.md').write_text('''# Changelog\n\n## 0.8.0\n- Vue active affichée clairement en haut de l’écran.\n- Raccourcis organismes dans le volet catégories : Allianz, Helvetia, Swisscom, État du Valais.\n- Clic sur un organisme = tous ses documents au même endroit.\n- Sous-catégories assurances conservées et mieux identifiées.\n- Statuts ajoutés pour les amendes et recours : Contesté, Recours envoyé, En attente de réponse, En recours, Clos.\n- Couleurs légères dans la liste selon l’état : payé, à payer, recours/contestation, à vérifier.\n- Moteur d’import, OCR, recherche, associations facture/paiement et archives existantes inchangés.\n- Mise à jour compatible avec les données 0.1 à 0.7.\n''')
