from pathlib import Path
root=Path('build-piiwii-documents-0130/work')

def rw(rel,pairs):
    p=root/rel; s=p.read_text()
    for a,b in pairs:
        if a not in s: print('WARN missing',rel,a[:120])
        s=s.replace(a,b)
    p.write_text(s)

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/Services/BridgeService.cs']:
    rw(rel,[('0.12.0','0.13.0')])
ax=root/'src/PiiWii.Documents/Services/AssistantExchangeService.cs'
if ax.exists(): ax.write_text(ax.read_text().replace('0.12.0','0.13.0'))

p=root/'src/PiiWii.Documents/MainForm.cs'; s=p.read_text()
s=s.replace('PiiWii Documents 0.12.0','PiiWii Documents 0.13.0').replace('v0.12.0','v0.13.0')

# Dashboard identity: less generic, closer to a finished product.
s=s.replace('Text="Bienvenue !"','Text="Mes documents"')
s=s.replace('Text="Vos documents, simplement."','Text="Tout retrouver, sans chercher dans les cartons."')

# Search feels like the centre of the product, not a raw textbox.
s=s.replace('PlaceholderText="Rechercher : Allianz, 607, Polo, amende 2025..."','PlaceholderText="Rechercher un document, organisme, montant, véhicule, amende, recours..."')
s=s.replace('search.BorderStyle=BorderStyle.FixedSingle;','search.BorderStyle=BorderStyle.FixedSingle;search.BackColor=Color.White;search.ForeColor=Color.FromArgb(15,23,42);')

# Calm premium background and clearer separation of the content zones.
s=s.replace('BackColor=Color.FromArgb(244,247,251)','BackColor=Color.FromArgb(242,246,250)')
s=s.replace('split.BackColor=Color.FromArgb(225,229,236)','split.BackColor=Color.FromArgb(224,231,239)')
s=s.replace('right=new TableLayoutPanel{Dock=DockStyle.Fill,RowCount=2,BackColor=Color.White}','right=new TableLayoutPanel{Dock=DockStyle.Fill,RowCount=2,BackColor=Color.FromArgb(255,255,255),Padding=new Padding(10)}')

# Cards: larger, softer, no default button chrome, stronger title hierarchy.
s=s.replace('Height=88,Dock=DockStyle.Fill','Height=94,Dock=DockStyle.Fill')
s=s.replace('host.FlatAppearance.BorderColor=Color.FromArgb(207,218,232);host.FlatAppearance.BorderSize=1;', 'host.FlatAppearance.BorderColor=Color.FromArgb(211,220,232);host.FlatAppearance.BorderSize=1;')
s=s.replace('host.FlatAppearance.MouseOverBackColor=Color.FromArgb(247,250,255);','host.FlatAppearance.MouseOverBackColor=Color.FromArgb(245,249,255);')
s=s.replace('host.FlatAppearance.MouseDownBackColor=Color.FromArgb(239,246,255);','host.FlatAppearance.MouseDownBackColor=Color.FromArgb(235,244,255);')

# Navigation: more like a modern sidebar, with hover feedback and more breathing room.
s=s.replace('Height=38,FlatStyle=FlatStyle.Flat','Height=40,FlatStyle=FlatStyle.Flat')
s=s.replace('b.FlatAppearance.BorderSize=0;b.Click+=(_,_)=>a();return b;', 'b.FlatAppearance.BorderSize=0;b.FlatAppearance.MouseOverBackColor=Color.FromArgb(241,246,253);b.FlatAppearance.MouseDownBackColor=Color.FromArgb(229,240,255);b.Cursor=Cursors.Hand;b.Click+=(_,_)=>a();return b;')

# Category rows: feel like folders rather than plain buttons.
s=s.replace('Height=34,FlatStyle=FlatStyle.Flat','Height=36,FlatStyle=FlatStyle.Flat')
s=s.replace('b.FlatAppearance.BorderSize=0;b.Click+=(_,_)=>a();return b;}', 'b.FlatAppearance.BorderSize=0;b.FlatAppearance.MouseOverBackColor=Color.FromArgb(239,246,255);b.FlatAppearance.MouseDownBackColor=Color.FromArgb(219,234,254);b.Cursor=Cursors.Hand;b.Click+=(_,_)=>a();return b;}',1)

# Grid polish: zebra rows, bigger headers and cleaner selection.
s=s.replace('grid.ColumnHeadersHeight=42;','grid.ColumnHeadersHeight=44;')
s=s.replace('grid.RowTemplate.Height=46;','grid.RowTemplate.Height=48;')
s=s.replace('grid.GridColor=Color.FromArgb(235,238,243);','grid.GridColor=Color.FromArgb(232,237,244);grid.AlternatingRowsDefaultCellStyle.BackColor=Color.FromArgb(249,251,253);')
s=s.replace('grid.CellBorderStyle=DataGridViewCellBorderStyle.SingleHorizontal;','grid.CellBorderStyle=DataGridViewCellBorderStyle.SingleHorizontal;grid.BackgroundColor=Color.White;')

# Right-side document card hierarchy.
s=s.replace('detailTitle=new(){Text="Aperçu du document"','detailTitle=new(){Text="Aperçu du document"')
s=s.replace('Font=new("Segoe UI Semibold",12)','Font=new("Segoe UI Semibold",13)')
s=s.replace('Text="⚙ Actions avancées"','Text="⚙  Actions avancées"')

# Better no-selection copy.
s=s.replace('Cliquez sur un document dans la liste pour l’afficher ici.','Choisissez un document dans la liste : son aperçu et ses informations s’afficheront ici.')

p.write_text(s)
(root/'README.md').write_text('# PiiWii Documents 0.13.0\n\nPasse de finition visuelle : accueil plus identifiable, navigation plus moderne, cartes et liste plus premium, panneau document plus propre.\n')
(root/'CHANGELOG.md').write_text('''# Changelog\n\n## 0.13.0\n- Accueil renommé et wording simplifié pour un usage personnel quotidien.\n- Recherche mise davantage au centre du flux utilisateur.\n- Navigation avec survol et retour visuel plus modernes.\n- Catégories/dossiers plus agréables à parcourir.\n- Cartes du tableau de bord plus grandes et plus réactives.\n- Liste documentaire avec lignes alternées, entêtes plus nets et sélection plus lisible.\n- Panneau aperçu/détails mieux hiérarchisé.\n- Aucun changement destructif sur les documents, l’OCR, la base ou les associations.\n- Mise à jour compatible avec les versions précédentes.\n''')
