from pathlib import Path
root=Path('build-piiwii-documents-0120/work')

def rw(rel,pairs):
    p=root/rel; s=p.read_text()
    for a,b in pairs:
        if a not in s: print('WARN missing',rel,a[:100])
        s=s.replace(a,b)
    p.write_text(s)

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/Services/BridgeService.cs']:
    rw(rel,[('0.11.0','0.12.0')])
ax=root/'src/PiiWii.Documents/Services/AssistantExchangeService.cs'
if ax.exists(): ax.write_text(ax.read_text().replace('0.11.0','0.12.0'))

p=root/'src/PiiWii.Documents/MainForm.cs'; s=p.read_text()
s=s.replace('PiiWii Documents 0.11.0','PiiWii Documents 0.12.0').replace('v0.11.0','v0.12.0')

# Premium light palette and calmer surfaces.
s=s.replace('BackColor=Color.FromArgb(246,248,251)','BackColor=Color.FromArgb(244,247,251)')
s=s.replace('BackColor=Color.White','BackColor=Color.FromArgb(255,255,255)')
s=s.replace('Color.FromArgb(214,222,232)','Color.FromArgb(207,218,232)')
s=s.replace('Color.FromArgb(224,228,235)','Color.FromArgb(214,222,232)')

# Give the navigation more hierarchy and softer category rows.
s=s.replace('Font=new("Segoe UI UI Semibold",8)','Font=new("Segoe UI Semibold",8.5f)')
s=s.replace('Height=32,FlatStyle=FlatStyle.Flat','Height=34,FlatStyle=FlatStyle.Flat')
s=s.replace('ForeColor=Color.FromArgb(52,61,76)','ForeColor=Color.FromArgb(43,54,72)')

# Make cards look less like plain WinForms buttons.
s=s.replace('host.FlatAppearance.BorderSize=1;host.Cursor=Cursors.Hand;','host.FlatAppearance.BorderSize=1;host.FlatAppearance.MouseOverBackColor=Color.FromArgb(247,250,255);host.FlatAppearance.MouseDownBackColor=Color.FromArgb(239,246,255);host.Cursor=Cursors.Hand;')
s=s.replace('Padding=new Padding(14,4,8,4)','Padding=new Padding(16,6,10,6)')

# Stronger list header and selected row hierarchy.
s=s.replace('grid.ColumnHeadersDefaultCellStyle.BackColor=Color.FromArgb(248,249,251);','grid.ColumnHeadersDefaultCellStyle.BackColor=Color.FromArgb(241,245,249);')
s=s.replace('grid.DefaultCellStyle.SelectionBackColor=Color.FromArgb(222,235,253);','grid.DefaultCellStyle.SelectionBackColor=Color.FromArgb(219,234,254);')
s=s.replace('grid.DefaultCellStyle.SelectionForeColor=Color.FromArgb(30,44,64);','grid.DefaultCellStyle.SelectionForeColor=Color.FromArgb(15,23,42);')

# Slightly roomier document pane and less cramped preview card.
s=s.replace('SplitterDistance=680','SplitterDistance=650')
s=s.replace('new RowStyle(SizeType.Percent,58)','new RowStyle(SizeType.Percent,60)')
s=s.replace('new RowStyle(SizeType.Percent,42)','new RowStyle(SizeType.Percent,40)')

# Upgrade wording and labels for a more polished product feel.
s=s.replace('Vos documents','Aperçu du document')
s=s.replace('Sélectionnez un document pour afficher son aperçu et ses informations.','Sélectionnez un document dans la liste pour afficher son aperçu, son classement et son statut.')
s=s.replace('▾   Catégories & dossiers','▾   Dossiers & catégories')
s=s.replace('CATÉGORIES','VOS DOSSIERS')

# Small visual cues for common folders.
s=s.replace('Category("Assurances",','Category("🛡  Assurances",')
s=s.replace('Category("Véhicules",','Category("🚗  Véhicules",')
s=s.replace('Category("Service des automobiles",','Category("🏛  Service des automobiles",')
s=s.replace('Category("Amendes",','Category("⚠  Amendes",')
s=s.replace('Category("Recours",','Category("↩  Recours",')
s=s.replace('Category("Télécom",','Category("📱  Télécom",')
s=s.replace('Category("Impôts",','Category("💰  Impôts",')
s=s.replace('Category("Santé",','Category("✚  Santé",')
s=s.replace('Category("Administration",','Category("📁  Administration",')
s=s.replace('Category("Autres",','Category("🗂  Autres",')

p.write_text(s)
(root/'README.md').write_text('# PiiWii Documents 0.12.0\n\nPasse visuelle premium : navigation plus claire, dossiers illustrés, cartes plus modernes, meilleure hiérarchie des listes et panneau document plus élégant.\n')
(root/'CHANGELOG.md').write_text('''# Changelog\n\n## 0.12.0\n- Nouvelle passe visuelle premium sans modification du moteur documentaire.\n- Palette plus douce et cohérente.\n- Dossiers/catégories mieux différenciés avec repères visuels.\n- Cartes d’accueil plus réactives au survol et à la sélection.\n- Tableau central plus lisible avec entête et sélection retravaillés.\n- Panneau document légèrement agrandi et mieux hiérarchisé.\n- Libellés simplifiés pour un usage quotidien plus intuitif.\n- Import, OCR, recherche, associations et base existante inchangés.\n- Mise à jour compatible avec les versions précédentes.\n''')
