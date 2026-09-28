from pathlib import Path
root=Path('build-piiwii-documents-070/work')

def rw(rel,pairs):
    p=root/rel
    s=p.read_text()
    for a,b in pairs:
        s=s.replace(a,b)
    p.write_text(s)

rw('src/PiiWii.Documents/PiiWii.Documents.csproj',[('0.6.0','0.7.0')])
rw('src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj',[('0.6.0','0.7.0')])
rw('src/PiiWii.Documents.Installer/Program.cs',[('0.6.0','0.7.0')])
rw('src/PiiWii.Documents/Services/BridgeService.cs',[('0.6.0','0.7.0')])
ax=root/'src/PiiWii.Documents/Services/AssistantExchangeService.cs'
if ax.exists():
    ax.write_text(ax.read_text().replace('0.6.0','0.7.0'))

b64=''.join((Path('build-piiwii-documents-070/mainform')/f'{i:02d}').read_text() for i in range(5))
import base64
(root/'src/PiiWii.Documents/MainForm.cs').write_bytes(base64.b64decode(b64))
(root/'README.md').write_text('# PiiWii Documents 0.7.0\n\nRefonte visuelle basée sur la maquette validée : catégories cliquables, liste centrale, aperçu et détails à droite, actions avancées discrètes.\n')
(root/'CHANGELOG.md').write_text('''# Changelog\n\n## 0.7.0\n- Refonte complète de l’écran principal selon la maquette validée.\n- Navigation latérale avec catégories cliquables.\n- Sous-catégories Assurances : véhicule, ménage, maladie.\n- Catégories Amendes, Recours, Télécom, Impôts, Santé, Administration et Autres.\n- Liste centrale avec colonne Catégorie.\n- Aperçu du document et résumé lisible toujours visibles à droite.\n- Éditeur complet ouvert uniquement via Modifier.\n- Actions OCR, lots, ChatGPT et administration rangées dans Actions avancées / Plus.\n- Import, OCR, base, recherche, associations et documents existants conservés.\n''')
