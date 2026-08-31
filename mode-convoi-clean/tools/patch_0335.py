from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "android/app/src/main/java/ch/piiwii/modeconvoi"
main_path = java / "MainActivity.java"
api_path = java / "ConvoyApi.java"
gradle_path = root / "android/app/build.gradle"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 anchor, found {count}")
    return text.replace(old, new, 1)


def replace_between(text, start, end, replacement, label):
    s = text.find(start)
    if s < 0:
        raise SystemExit(f"{label}: start anchor missing")
    e = text.find(end, s)
    if e < 0:
        raise SystemExit(f"{label}: end anchor missing")
    if text.find(start, s + 1) >= 0:
        raise SystemExit(f"{label}: start anchor is not unique")
    return text[:s] + replacement + text[e:]

main = main_path.read_text(encoding="utf-8")
api = api_path.read_text(encoding="utf-8")
gradle = gradle_path.read_text(encoding="utf-8")

main = replace_once(
    main,
    '    private LinearLayout snapshotArea;\n',
    '    private LinearLayout snapshotArea;\n    private TextView convoyCountView;\n',
    'home count field')

main = replace_once(
    main,
    '        mapView = null;\n        applyPalette();\n',
    '        mapView = null;\n        convoyCountView = null;\n        applyPalette();\n',
    'render field reset')

main = replace_once(
    main,
    '        Space left=new Space(this);\n        h.addView(left,new LinearLayout.LayoutParams(dp(82),dp(50)));\n\n        TextView title=text("MODE CONVOI",19,true,accent);\n        title.setGravity(Gravity.CENTER);\n        h.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));\n',
    '        Space left=new Space(this);\n        h.addView(left,new LinearLayout.LayoutParams(dp(104),dp(50)));\n\n        TextView title=text("MODE CONVOI",19,true,accent);\n        title.setGravity(Gravity.CENTER);\n        title.setMaxLines(1);\n        title.setAutoSizeTextTypeUniformWithConfiguration(15,19,1,android.util.TypedValue.COMPLEX_UNIT_SP);\n        h.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));\n',
    'responsive header title')

main = replace_once(
    main,
    '        connectionBadge.setGravity(Gravity.CENTER);\n        connectionBadge.setPadding(dp(12),0,dp(12),0);\n',
    '        connectionBadge.setGravity(Gravity.CENTER);\n        connectionBadge.setMaxLines(1);\n        connectionBadge.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,android.util.TypedValue.COMPLEX_UNIT_SP);\n        connectionBadge.setPadding(dp(8),0,dp(8),0);\n',
    'responsive connection badge')

main = replace_once(
    main,
    '        h.addView(connectionBadge,new LinearLayout.LayoutParams(dp(92),dp(34)));\n',
    '        h.addView(connectionBadge,new LinearLayout.LayoutParams(dp(104),dp(34)));\n',
    'header badge width')

home_method = '''    private void renderConvoyHome() {
        currentPage="home";
        String code=prefs.get("code","");

        LinearLayout hero=cardBox();
        hero.setPadding(dp(16),dp(14),dp(16),dp(16));
        LinearLayout heroTop=new LinearLayout(this);heroTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow=text("CONVOI EN COURS",11,true,accent);eyebrow.setLetterSpacing(.06f);heroTop.addView(eyebrow,new LinearLayout.LayoutParams(0,dp(28),1));
        TextView live=text("●  LIVE",10,true,Color.rgb(101,211,117));live.setGravity(Gravity.CENTER);live.setPadding(dp(10),0,dp(10),0);live.setBackground(roundBg(darkTheme?Color.rgb(23,50,29):Color.rgb(233,249,236),Color.rgb(77,150,88),14,1));heroTop.addView(live,new LinearLayout.LayoutParams(-2,dp(28)));hero.addView(heroTop);

        TextView name=text(prefs.get("convoyName","Convoi"),23,true,fg);name.setMaxLines(2);name.setPadding(0,dp(3),0,0);hero.addView(name);
        convoyCountView=text(snapshotCountText(),13,true,accent);convoyCountView.setMaxLines(2);convoyCountView.setPadding(0,dp(5),0,dp(10));hero.addView(convoyCountView);

        LinearLayout codePanel=new LinearLayout(this);codePanel.setOrientation(LinearLayout.VERTICAL);codePanel.setPadding(dp(12),dp(9),dp(12),dp(10));codePanel.setBackground(roundBg(control,border,14,1));
        TextView codeV=text("CODE DU CONVOI   "+code,15,true,fg);codeV.setLetterSpacing(.045f);codeV.setGravity(Gravity.CENTER);codeV.setMaxLines(1);codeV.setAutoSizeTextTypeUniformWithConfiguration(12,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);codePanel.addView(codeV,new LinearLayout.LayoutParams(-1,dp(34)));
        LinearLayout shareRow=new LinearLayout(this);shareRow.setGravity(Gravity.CENTER_VERTICAL);shareRow.setPadding(0,dp(5),0,0);
        Button qr=smallButton("▦  QR",Color.TRANSPARENT,accent);qr.setBackground(roundBg(Color.TRANSPARENT,accent,11,1));qr.setOnClickListener(v->showConvoyQr());LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(0,dp(40),1);qlp.setMargins(0,0,dp(5),0);shareRow.addView(qr,qlp);
        Button share=smallButton("↗  PARTAGER",Color.TRANSPARENT,accent);share.setBackground(roundBg(Color.TRANSPARENT,accent,11,1));share.setOnClickListener(v->shareConvoy());LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(0,dp(40),1);slp.setMargins(dp(5),0,0,0);shareRow.addView(share,slp);codePanel.addView(shareRow);
        hero.addView(codePanel);content.addView(hero);

        sectionLabel(content,"POSITION DU CONVOI");
        snapshotArea=new LinearLayout(this);snapshotArea.setOrientation(LinearLayout.VERTICAL);content.addView(snapshotArea,new LinearLayout.LayoutParams(-1,-2));refreshSnapshotArea();

        addTalkieWalkieSection();

        sectionLabel(content,"ACTIONS RAPIDES");
        TextView actionHint=text("Envoie immédiatement ton état aux autres voitures.",11,false,muted);actionHint.setPadding(dp(2),0,dp(2),dp(4));content.addView(actionHint);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);
        String[][] statuses={{"🛑","Je m’arrête","stop"},{"⛽","Essence","fuel"},{"☕","Pause","pause"},{"🚻","WC","wc"},{"⚠️","Problème","problem"},{"🚗","Voiture","car_problem"},{"↗️","Je rejoins","joining"},{"👍","OK","ok"}};
        for(String[] st:statuses){
            View b=quickActionTile(st[0],st[1]);b.setOnClickListener(v->sendStatus(st[2]));
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(72);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(b,lp);
        }
        content.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        Button custom=outlinedButton("✎   AUTRE MESSAGE",Color.rgb(94,99,104));custom.setOnClickListener(v->customStatusDialog());content.addView(custom);
        Button clear=ghostButton("ANNULER MON STATUT");clear.setOnClickListener(v->sendStatus("clear"));content.addView(clear);
    }
'''
main = replace_between(
    main,
    '    private void renderConvoyHome() {',
    '    private String snapshotCountText()',
    home_method,
    'home layout')

main = replace_once(
    main,
    '    private void refreshSnapshotArea() {\n        if (snapshotArea == null || !"home".equals(currentPage)) return;\n        snapshotArea.removeAllViews();\n',
    '    private void refreshSnapshotArea() {\n        if (snapshotArea == null || !"home".equals(currentPage)) return;\n        if(convoyCountView!=null)convoyCountView.setText(snapshotCountText());\n        snapshotArea.removeAllViews();\n',
    'live convoy count refresh')

snapshot_block = '''    private void renderSnapshot(LinearLayout target) {
        JSONObject me=positionResolver.findMe(snapshot);
        ConvoyPositionResolver.Relative rel=positionResolver.resolveRelative(snapshot);
        target.addView(positionCard("DEVANT MOI",rel.ahead,Color.rgb(91,196,62),false));
        target.addView(positionCard("MOI",me,accent,true));
        target.addView(positionCard("DERRIÈRE MOI",rel.behind,Color.rgb(55,158,225),false));

        JSONObject myStatus=me==null?null:me.optJSONObject("activeStatus");
        if(myStatus!=null){
            TextView chip=text("●  "+myStatus.optString("label","Statut actif"),12,true,Color.rgb(132,218,84));
            chip.setGravity(Gravity.CENTER);chip.setMaxLines(2);chip.setPadding(dp(12),dp(8),dp(12),dp(8));
            chip.setBackground(roundBg(darkTheme?Color.rgb(26,48,23):Color.rgb(235,249,232),Color.rgb(62,103,47),16,1));
            LinearLayout holder=new LinearLayout(this);holder.setGravity(Gravity.CENTER);holder.setPadding(0,dp(2),0,dp(2));holder.addView(chip,new LinearLayout.LayoutParams(-1,-2));target.addView(holder);
        }

        ConvoyPositionResolver.RallyInfo rallyInfo=positionResolver.rallyInfo(snapshot);
        if(rallyInfo!=null){
            JSONObject rally=rallyInfo.rally;
            LinearLayout box=cardBox();box.setPadding(dp(14),dp(12),dp(12),dp(12));
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
            labels.addView(text("📍  POINT DE REGROUPEMENT",11,true,accent));
            TextView rallyName=text(rally.optString("name","Point de regroupement"),17,true,fg);rallyName.setMaxLines(2);rallyName.setPadding(0,dp(3),0,0);labels.addView(rallyName);
            TextView rallySub=text(rallyInfo.subtitle,12,false,muted);rallySub.setMaxLines(2);rallySub.setPadding(0,dp(3),0,0);labels.addView(rallySub);
            row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            Button gps=smallButton("GPS ➤",Color.TRANSPARENT,accent);gps.setBackground(roundBg(Color.TRANSPARENT,accent,11,1));gps.setOnClickListener(v->openGps(rally));LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(dp(72),dp(38));glp.setMargins(dp(8),0,0,0);row.addView(gps,glp);box.addView(row);target.addView(box);
        }
    }

    private View positionCard(String title,JSONObject p,int stripeColor,boolean own){
        LinearLayout outer=cardBox();outer.setBackground(roundBg(card,own?accent:border,16,own?2:1));
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text(title,11,true,stripeColor),new LinearLayout.LayoutParams(0,dp(30),1));
        if(own){TextView active=text("ACTIF",10,true,Color.rgb(131,220,74));active.setGravity(Gravity.CENTER);active.setPadding(dp(9),0,dp(9),0);active.setBackground(roundBg(darkTheme?Color.rgb(24,48,20):Color.rgb(235,249,232),Color.rgb(92,159,55),12,1));titleRow.addView(active,new LinearLayout.LayoutParams(-2,dp(27)));}
        outer.addView(titleRow);
        if(p==null){TextView missing=text("Position indisponible",15,true,muted);missing.setPadding(0,dp(4),0,dp(3));outer.addView(missing);return outer;}

        LinearLayout info=new LinearLayout(this);info.setGravity(Gravity.CENTER_VERTICAL);
        View carIcon=participantAvatar(p,48,stripeColor);info.addView(carIcon,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(12),0,dp(2),0);
        TextView person=text(p.optString("name",prefs.get("profileName","Moi")),18,true,fg);person.setMaxLines(2);names.addView(person);
        TextView vehicle=text(p.optString("vehicle",prefs.get("profileVehicle","Véhicule")),12,false,muted);vehicle.setMaxLines(2);names.addView(vehicle);
        String relative=own?"":p.optString("_relative","");
        if(!relative.isEmpty()){TextView distance=text(relative.replace(" devant","").replace(" derrière",""),12,true,stripeColor);distance.setPadding(0,dp(2),0,0);names.addView(distance);}
        String role=p.optString("role","");
        if("leader".equals(role)) names.addView(text("★ Chef de convoi",11,true,Color.rgb(91,196,62)));
        else if("sweep".equals(role)) names.addView(text("◆ Voiture balai",11,true,Color.rgb(55,158,225)));
        info.addView(names,new LinearLayout.LayoutParams(0,-2,1));outer.addView(info);
        JSONObject st=p.optJSONObject("activeStatus");
        if(st!=null){TextView state=text("⚑  "+st.optString("label",""),12,true,accent);state.setMaxLines(2);state.setPadding(0,dp(7),0,0);outer.addView(state);}
        return outer;
    }


'''
main = replace_between(
    main,
    '    private void renderSnapshot(LinearLayout target) {',
    '    private void renderMapPage() {',
    snapshot_block,
    'home position cards')

main = replace_once(
    main,
    '        TextView ptt=text("🎙️\\nMAINTENIR POUR PARLER",17,true,Color.rgb(20,22,24));\n        ptt.setGravity(Gravity.CENTER);ptt.setPadding(dp(12),dp(10),dp(12),dp(10));ptt.setBackground(roundBg(accent,accent,20,0));\n',
    '        TextView ptt=text("🎙️\\nMAINTENIR POUR PARLER",17,true,Color.rgb(20,22,24));\n        ptt.setGravity(Gravity.CENTER);ptt.setMaxLines(3);ptt.setPadding(dp(12),dp(10),dp(12),dp(10));ptt.setBackground(roundBg(accent,accent,20,0));\n',
    'talkie button text wrapping')

main = replace_once(
    main,
    '        talkieState=text(initial,14,true,initial.startsWith("⚠")?danger:Color.rgb(90,200,120));talkieState.setGravity(Gravity.CENTER);card.addView(talkieState,new LinearLayout.LayoutParams(-1,dp(42)));\n',
    '        talkieState=text(initial,14,true,initial.startsWith("⚠")?danger:Color.rgb(90,200,120));talkieState.setGravity(Gravity.CENTER);talkieState.setMaxLines(2);talkieState.setPadding(dp(4),dp(7),dp(4),dp(7));card.addView(talkieState,new LinearLayout.LayoutParams(-1,-2));\n',
    'talkie state wrapping')

old_tile = '''    private View quickActionTile(String icon,String label){
        LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(4),dp(7),dp(4),dp(6));tile.setBackground(roundBg(control,border,15,1));
        TextView iv=text(icon,27,false,accent);iv.setGravity(Gravity.CENTER);tile.addView(iv,new LinearLayout.LayoutParams(-1,dp(47)));
        TextView tv=text(label,11,true,fg);tv.setGravity(Gravity.CENTER);tv.setMaxLines(1);tile.addView(tv,new LinearLayout.LayoutParams(-1,dp(27)));
        return tile;
    }
'''
new_tile = '''    private View quickActionTile(String icon,String label){
        boolean urgent=label.toLowerCase(Locale.ROOT).contains("arrêt")||label.toLowerCase(Locale.ROOT).contains("problème")||label.toLowerCase(Locale.ROOT).contains("voiture");
        int tileFill=urgent?(darkTheme?Color.rgb(52,27,27):Color.rgb(255,242,242)):control;
        int tileStroke=urgent?danger:border;
        int iconColor=urgent?danger:accent;
        LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.HORIZONTAL);tile.setGravity(Gravity.CENTER_VERTICAL);tile.setPadding(dp(11),dp(7),dp(10),dp(7));tile.setBackground(roundBg(tileFill,tileStroke,15,urgent?2:1));
        TextView iv=text(icon,25,false,iconColor);iv.setGravity(Gravity.CENTER);tile.addView(iv,new LinearLayout.LayoutParams(dp(42),-1));
        TextView tv=text(label,13,true,fg);tv.setGravity(Gravity.CENTER_VERTICAL);tv.setMaxLines(2);tv.setAutoSizeTextTypeUniformWithConfiguration(11,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);tv.setPadding(dp(5),0,0,0);tile.addView(tv,new LinearLayout.LayoutParams(0,-1,1));
        return tile;
    }
'''
main = replace_once(main, old_tile, new_tile, 'quick action tiles')

main = replace_once(
    main,
    '        TextView tv=text(label,11,active,active?accent:muted);tv.setGravity(Gravity.CENTER);item.addView(tv,new LinearLayout.LayoutParams(-1,dp(24)));\n',
    '        TextView tv=text(label,11,active,active?accent:muted);tv.setGravity(Gravity.CENTER);tv.setMaxLines(1);tv.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,android.util.TypedValue.COMPLEX_UNIT_SP);item.addView(tv,new LinearLayout.LayoutParams(-1,dp(24)));\n',
    'bottom nav label autosize')

main = replace_once(main, 'Mode Convoi 0.3.34', 'Mode Convoi 0.3.35', 'about version')
api = replace_once(api, 'ModeConvoi-Android/0.3.34', 'ModeConvoi-Android/0.3.35', 'user agent version')
gradle = replace_once(gradle, 'versionCode 37', 'versionCode 38', 'version code')
gradle = replace_once(gradle, "versionName '0.3.34'", "versionName '0.3.35'", 'version name')

for forbidden in ['versionName \'0.3.34\'', 'ModeConvoi-Android/0.3.34', 'Mode Convoi 0.3.34']:
    if forbidden in main or forbidden in api or forbidden in gradle:
        raise SystemExit(f"old version marker remains: {forbidden}")

if 'grid.setColumnCount(2)' not in main:
    raise SystemExit('home quick action grid was not migrated to two columns')
if 'convoyCountView.setText(snapshotCountText())' not in main:
    raise SystemExit('live convoy counter refresh is missing')
if 'setAutoSizeTextTypeUniformWithConfiguration' not in main:
    raise SystemExit('responsive text safeguards are missing')

main_path.write_text(main, encoding="utf-8")
api_path.write_text(api, encoding="utf-8")
gradle_path.write_text(gradle, encoding="utf-8")
print('Mode Convoi 0.3.35 ergonomics migration applied successfully')
