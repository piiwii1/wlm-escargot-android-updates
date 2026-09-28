from pathlib import Path
root=Path('build-piiwii-documents-0100/work')

def rw(rel,pairs):
    p=root/rel; s=p.read_text()
    for a,b in pairs:
        if a not in s: print('WARN missing',rel,a[:100])
        s=s.replace(a,b)
    p.write_text(s)

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/Services/BridgeService.cs']:
    rw(rel,[('0.9.0','0.10.0')])
ax=root/'src/PiiWii.Documents/Services/AssistantExchangeService.cs'
if ax.exists(): ax.write_text(ax.read_text().replace('0.9.0','0.10.0'))

p=root/'src/PiiWii.Documents/MainForm.cs'; s=p.read_text()
s=s.replace('PiiWii Documents 0.9.0','PiiWii Documents 0.10.0').replace('v0.9.0','v0.10.0')

# More breathing room and a slightly wider navigation column.
s=s.replace('root.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,245));','root.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,260));')
s=s.replace('Padding=new Padding(24,14,20,14)','Padding=new Padding(26,16,24,18)')
s=s.replace('new RowStyle(SizeType.Absolute,92)','new RowStyle(SizeType.Absolute,104)')

# Make categories feel like a normal part of navigation instead of a hidden technical menu.
s=s.replace('Nav("•••  Plus / catégories",ToggleCategories)','Nav("▦   Catégories et dossiers",ToggleCategories)')
s=s.replace('Visible=true','Visible=true',1)

# Preview should never appear as a black rectangle when empty.
s=s.replace('readonly WebView2 preview=new(){Dock=DockStyle.Fill};','readonly WebView2 preview=new(){Dock=DockStyle.Fill,DefaultBackgroundColor=Color.White};')
s=s.replace('async Task InitPreview(){try{await preview.EnsureCoreWebView2Async();preview.CoreWebView2.Settings.AreDevToolsEnabled=false;}catch{}}', 'async Task InitPreview(){try{await preview.EnsureCoreWebView2Async();preview.CoreWebView2.Settings.AreDevToolsEnabled=false;ShowPreviewPlaceholder();}catch{}}\n    void ShowPreviewPlaceholder(){try{if(preview.CoreWebView2!=null)preview.NavigateToString("<html><body style=\'margin:0;background:#f8fafc;font-family:Segoe UI;color:#64748b;display:flex;align-items:center;justify-content:center;height:100vh\'><div style=\'text-align:center\'><div style=\'font-size:42px;margin-bottom:12px\'>📄</div><div style=\'font-size:16px;font-weight:600;color:#334155\'>Sélectionnez un document</div><div style=\'font-size:13px;margin-top:6px\'>L’aperçu apparaîtra ici.</div></div></body></html>");}catch{}}')

# Give cards a stronger visual identity.
s=s.replace('BackColor=Color.White,ForeColor=Color.FromArgb(30,43,62)', 'BackColor=Color.FromArgb(255,255,255),ForeColor=Color.FromArgb(30,43,62)')
s=s.replace('host.FlatAppearance.BorderColor=Color.FromArgb(224,228,235);host.FlatAppearance.BorderSize=1;', 'host.FlatAppearance.BorderColor=Color.FromArgb(214,222,232);host.FlatAppearance.BorderSize=1;host.Cursor=Cursors.Hand;')

# Stronger hierarchy on the document list.
s=s.replace('grid.ColumnHeadersHeight=38;','grid.ColumnHeadersHeight=42;')
s=s.replace('grid.RowTemplate.Height=42;','grid.RowTemplate.Height=46;')

# Friendlier empty-state counters and wording.
s=s.replace('clic sur une catégorie ou un organisme pour filtrer','choisissez une catégorie, un dossier ou importez vos premiers documents')
s=s.replace('dossiers intelligents à gauche','catégories et dossiers à gauche')

# Right panel: more useful placeholder text before selection.
s=s.replace('readonly Label detailTitle=new(){AutoSize=false,Dock=DockStyle.Top,Height=34', 'readonly Label detailTitle=new(){Text="Aucun document sélectionné",AutoSize=false,Dock=DockStyle.Top,Height=34')
s=s.replace('readonly Label detailCategory=new(){AutoSize=true', 'readonly Label detailCategory=new(){Text="Importez un document ou sélectionnez-en un dans la liste.",AutoSize=true')

p.write_text(s)
(root/'README.md').write_text('# PiiWii Documents 0.10.0\n\nPasse visuelle basée sur la capture réelle 0.9.0 : accueil plus dense, cartes plus nettes, navigation mieux structurée et aperçu vide remplacé par un vrai état d’accueil.\n')
(root/'CHANGELOG.md').write_text('''# Changelog\n\n## 0.10.0\n- Refonte visuelle directement basée sur la capture réelle de la 0.9.0.\n- Suppression du rectangle noir quand aucun document n’est sélectionné.\n- Nouvel état d’aperçu : “Sélectionnez un document”.\n- Navigation légèrement élargie et catégories/dossiers plus visibles.\n- Cartes d’accueil plus lisibles et plus faciles à cliquer.\n- Tableau central plus confortable (entêtes et lignes plus hautes).\n- Panneau de droite plus compréhensible à vide.\n- Aucun changement destructif sur les documents, OCR ou base existante.\n- Mise à jour compatible avec les données 0.1 à 0.9.\n''')
