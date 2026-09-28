from pathlib import Path
root=Path('build-piiwii-documents-0110/work')

def rw(rel,pairs):
    p=root/rel; s=p.read_text()
    for a,b in pairs:
        if a not in s: print('WARN missing',rel,a[:100])
        s=s.replace(a,b)
    p.write_text(s)

for rel in ['src/PiiWii.Documents/PiiWii.Documents.csproj','src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj','src/PiiWii.Documents.Installer/Program.cs','src/PiiWii.Documents/Services/BridgeService.cs']:
    rw(rel,[('0.10.0','0.11.0')])
ax=root/'src/PiiWii.Documents/Services/AssistantExchangeService.cs'
if ax.exists(): ax.write_text(ax.read_text().replace('0.10.0','0.11.0'))

p=root/'src/PiiWii.Documents/MainForm.cs'; s=p.read_text()
s=s.replace('PiiWii Documents 0.10.0','PiiWii Documents 0.11.0').replace('v0.10.0','v0.11.0')

# Wider navigation and roomier dashboard.
s=s.replace('root.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,260));','root.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,280));')
s=s.replace('Padding=new Padding(26,16,24,18)','Padding=new Padding(28,18,26,20)')
s=s.replace('new RowStyle(SizeType.Absolute,104)','new RowStyle(SizeType.Absolute,116)')

# Categories should feel like a permanent navigation area, not a hidden tool.
s=s.replace('Nav("▦   Catégories et dossiers",ToggleCategories)','Nav("▾   Catégories & dossiers",ToggleCategories)')
s=s.replace('categoryPanel=new FlowLayoutPanel{Width=205,AutoSize=true','categoryPanel=new FlowLayoutPanel{Width=225,AutoSize=true')
s=s.replace('Width=190,Height=30','Width=215,Height=32')

# Give more space to the preview/details panel.
s=s.replace('SplitterDistance=735','SplitterDistance=680')
s=s.replace('new RowStyle(SizeType.Percent,62)','new RowStyle(SizeType.Percent,58)')
s=s.replace('new RowStyle(SizeType.Percent,38)','new RowStyle(SizeType.Percent,42)')

# Make the top cards more substantial and readable.
s=s.replace('Height=76,Dock=DockStyle.Fill','Height=88,Dock=DockStyle.Fill')
s=s.replace('Font=new("Segoe UI Semibold",10)','Font=new("Segoe UI Semibold",10.5f)')

# Friendlier labels around empty/document states.
s=s.replace('Text="Aucun document sélectionné"','Text="Vos documents"')
s=s.replace('Text="Importez un document ou sélectionnez-en un dans la liste."','Text="Sélectionnez un document pour afficher son aperçu et ses informations."')
s=s.replace('L’aperçu apparaîtra ici.','Cliquez sur un document dans la liste pour l’afficher ici.')

# Stronger empty-state instruction in dashboard line.
s=s.replace('catégories et dossiers à gauche','commencez par Importer, ou ouvrez une catégorie à gauche')

p.write_text(s)
(root/'README.md').write_text('# PiiWii Documents 0.11.0\n\nNouvelle passe ergonomique basée sur la capture réelle : navigation plus lisible, catégories plus présentes, panneau document plus large et tableau de bord moins vide.\n')
(root/'CHANGELOG.md').write_text('''# Changelog\n\n## 0.11.0\n- Menu gauche élargi et catégories/dossiers plus faciles à parcourir.\n- Zone catégories agrandie pour éviter les libellés tassés.\n- Panneau de droite élargi : aperçu et fiche document plus confortables.\n- Répartition aperçu/détails revue pour mieux exploiter la hauteur.\n- Cartes d’accueil plus hautes et plus lisibles.\n- États vides reformulés pour guider l’utilisateur au lieu d’afficher du vide.\n- Aucun changement destructif sur les documents, l’OCR ou la base.\n- Mise à jour compatible avec les versions précédentes.\n''')
