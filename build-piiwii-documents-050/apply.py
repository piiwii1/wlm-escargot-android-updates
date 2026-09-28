from pathlib import Path
root=Path('build-piiwii-documents-050/work')

def rw(rel, pairs):
    p=root/rel; s=p.read_text()
    for a,b in pairs:
        if a not in s: print('WARN missing', rel, a[:60])
        s=s.replace(a,b)
    p.write_text(s)

rw('src/PiiWii.Documents/PiiWii.Documents.csproj',[
('<Version>0.4.0</Version>','<Version>0.5.0</Version>'),('<FileVersion>0.4.0.0</FileVersion>','<FileVersion>0.5.0.0</FileVersion>'),('<AssemblyVersion>0.4.0.0</AssemblyVersion>','<AssemblyVersion>0.5.0.0</AssemblyVersion>')])
rw('src/PiiWii.Documents.Installer/PiiWii.Documents.Installer.csproj',[('PiiWii-Documents-Setup-0.4.0','PiiWii-Documents-Setup-0.5.0'),('<Version>0.4.0</Version>','<Version>0.5.0</Version>')])
rw('src/PiiWii.Documents.Installer/Program.cs',[('0.4.0','0.5.0')])
rw('src/PiiWii.Documents/Services/BridgeService.cs',[('version="0.4.0"','version="0.5.0"')])
rw('src/PiiWii.Documents/Services/AssistantExchangeService.cs',[('appVersion="0.4.0"','appVersion="0.5.0"')])

p=root/'src/PiiWii.Documents/MainForm.cs'; s=p.read_text()
s=s.replace('    long? selectedId; bool trashMode=false; bool busy=false; string lastBatchId="";','    long? selectedId; bool trashMode=false; bool busy=false; string lastBatchId=""; SplitContainer? detailsSplit; readonly ContextMenuStrip moreMenu=new();')
s=s.replace('Text="PiiWii Documents 0.4.0";','Text="PiiWii Documents 0.5.0";')
start=s.index('    void BuildUi()')
end=s.index('    Control BuildEditor()')
new_build=r'''    void BuildUi()
    {
        BackColor=Color.FromArgb(247,248,250);
        var root=new TableLayoutPanel{Dock=DockStyle.Fill,ColumnCount=2,RowCount=1};
        root.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,190));root.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,100));Controls.Add(root);

        var nav=new FlowLayoutPanel{Dock=DockStyle.Fill,FlowDirection=FlowDirection.TopDown,WrapContents=false,Padding=new(12,18,12,12),BackColor=Color.White};root.Controls.Add(nav,0,0);
        nav.Controls.Add(new Label{Text="PiiWii\nDocuments",AutoSize=true,Font=new("Segoe UI Semibold",16),Padding=new(8,0,0,18)});
        nav.Controls.Add(Nav("🏠  Accueil",()=>{trashMode=false;search.Text="";RefreshList();}));
        nav.Controls.Add(Nav("📄  Tous les documents",()=>{trashMode=false;search.Text="";RefreshList();}));
        nav.Controls.Add(Nav("⚠  À vérifier",()=>{trashMode=false;search.Text="À vérifier";RefreshList();}));
        nav.Controls.Add(Nav("💳  À payer",()=>{trashMode=false;search.Text="À payer";RefreshList();}));
        nav.Controls.Add(Nav("🕘  Dernier import",()=>{if(string.IsNullOrEmpty(lastBatchId)){MessageBox.Show("Aucun import récent pendant cette session.");return;}trashMode=false;search.Text=lastBatchId;RefreshList();}));
        nav.Controls.Add(new Label{Text="",Width=160,Height=20});
        nav.Controls.Add(Nav("🗑  Corbeille",()=>{trashMode=true;search.Text="";RefreshList();}));
        nav.Controls.Add(new Label{Text="",Width=160,Height=90});
        nav.Controls.Add(new Label{Text="v0.5.0",AutoSize=true,ForeColor=Color.Silver,Padding=new(8,0,0,0)});

        var main=new TableLayoutPanel{Dock=DockStyle.Fill,RowCount=5,Padding=new Padding(20,16,20,16)};
        main.RowStyles.Add(new RowStyle(SizeType.Absolute,48));main.RowStyles.Add(new RowStyle(SizeType.Absolute,62));main.RowStyles.Add(new RowStyle(SizeType.Absolute,40));main.RowStyles.Add(new RowStyle(SizeType.Absolute,8));main.RowStyles.Add(new RowStyle(SizeType.Percent,100));root.Controls.Add(main,1,0);
        main.Controls.Add(new Label{Text="Mes documents",Dock=DockStyle.Fill,Font=new("Segoe UI Semibold",20),TextAlign=ContentAlignment.MiddleLeft},0,0);

        var top=new TableLayoutPanel{Dock=DockStyle.Fill,ColumnCount=4};top.ColumnStyles.Add(new ColumnStyle(SizeType.Percent,100));top.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,145));top.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,145));top.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute,100));
        search.Margin=new Padding(0,10,10,10);search.Font=new("Segoe UI",11);top.Controls.Add(search,0,0);
        top.Controls.Add(Btn("＋ Importer",async()=>await Import()),1,0);top.Controls.Add(Btn("Importer dossier",async()=>await ImportFolder()),2,0);top.Controls.Add(Btn("Plus  ⋯",ShowMoreMenu),3,0);main.Controls.Add(top,0,1);
        search.TextChanged+=(_,_)=>RefreshList();

        main.Controls.Add(stats,0,2);main.Controls.Add(progressBar,0,3);
        var split=new SplitContainer{Dock=DockStyle.Fill,SplitterDistance=760,BackColor=Color.White,BorderStyle=BorderStyle.FixedSingle};main.Controls.Add(split,0,4);
        BuildGrid();grid.RowTemplate.Height=40;grid.RowHeadersVisible=false;split.Panel1.Controls.Add(grid);
        detailsSplit=new SplitContainer{Dock=DockStyle.Fill,Orientation=Orientation.Horizontal,SplitterDistance=500};split.Panel2.Controls.Add(detailsSplit);detailsSplit.Panel1.Controls.Add(preview);detailsSplit.Panel2.Controls.Add(BuildEditor());detailsSplit.Panel2Collapsed=true;
        grid.SelectionChanged+=(_,_)=>LoadSelected();grid.CellDoubleClick+=(_,_)=>OpenFile();

        moreMenu.Items.Add("Modifier les informations",null,(_,_)=>ToggleEditor());
        moreMenu.Items.Add("Ouvrir le fichier",null,(_,_)=>OpenFile());
        moreMenu.Items.Add("Réanalyser ce document",null,async(_,_)=>await Reanalyze());
        moreMenu.Items.Add(new ToolStripSeparator());
        moreMenu.Items.Add("Contrôler le dernier lot",null,(_,_)=>ReviewBatch());
        moreMenu.Items.Add("Classer le lot avec ChatGPT",null,(_,_)=>ClassifyWithChatGpt());
        moreMenu.Items.Add("Importer un résultat IA",null,(_,_)=>ImportAiResults());
        moreMenu.Items.Add(new ToolStripSeparator());
        moreMenu.Items.Add("Pont sécurisé",null,(_,_)=>ShowBridge());
        moreMenu.Items.Add("Dossier de données",null,(_,_)=>ChooseDataRoot());
        moreMenu.Items.Add("Supprimer / restaurer",null,(_,_)=>ToggleDelete());
    }
    void ShowMoreMenu(){moreMenu.Show(Cursor.Position);}
    void ToggleEditor(){if(selectedId is null){MessageBox.Show("Sélectionne d'abord un document.");return;}if(detailsSplit!=null)detailsSplit.Panel2Collapsed=!detailsSplit.Panel2Collapsed;}
'''
s=s[:start]+new_build+s[end:]
s=s.replace('stats.Text=$"{s["total"]} documents   •   {s["topay"]} à payer   •   {s["review"]} à vérifier   •   {s["paper"]} originaux conservés   •   Données : {AppPaths.DataRoot}";', 'stats.Text=$"{s["total"]} documents   •   {s["review"]} à vérifier   •   {s["topay"]} à payer   •   {s["paper"]} originaux conservés";')
s=s.replace('var msg=$"Importés : {r.Imported}\\nDoublons ignorés : {r.Duplicates}\\nÀ vérifier : {r.NeedsReview}\\nPaires facture/paiement associées : {r.LinkedPairs}\\nErreurs : {r.Errors}";', 'var msg=$"{r.Imported} document(s) importé(s)\\n{r.Duplicates} doublon(s) ignoré(s)\\n{r.LinkedPairs} paiement(s) associé(s)\\n{r.NeedsReview} document(s) à vérifier"; if(r.Errors>0)msg+=$"\\n{r.Errors} erreur(s)";')
s=s.replace('MessageBox.Show(msg,"Import intelligent + OCR",MessageBoxButtons.OK,r.Errors>0?MessageBoxIcon.Warning:MessageBoxIcon.Information);','MessageBox.Show(msg,"Import terminé",MessageBoxButtons.OK,r.Errors>0?MessageBoxIcon.Warning:MessageBoxIcon.Information);')
p.write_text(s)

(root/'README.md').write_text('# PiiWii Documents 0.5.0\n\nVersion ergonomie : écran principal simplifié. Recherche, import, liste et aperçu sont prioritaires. Les métadonnées et outils avancés sont rangés dans Plus.\n')
(root/'CHANGELOG.md').write_text('''# Changelog\n\n## 0.5.0\n- Interface principale fortement simplifiée.\n- 8 boutons permanents remplacés par Importer, Importer dossier et Plus.\n- Catégories retirées de la barre latérale pour réduire le bruit visuel.\n- Éditeur de métadonnées fermé par défaut.\n- Outils IA, pont sécurisé et administration regroupés dans Plus.\n- Double-clic sur un document pour l'ouvrir.\n- Résumé d'import raccourci.\n- Données et base 0.1 à 0.4 conservées.\n''')
