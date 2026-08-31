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


def replace_region(text, start_marker, end_marker, replacement, label):
    start = text.find(start_marker)
    if start < 0:
        raise SystemExit(f"{label}: start marker missing")
    end = text.find(end_marker, start)
    if end < 0:
        raise SystemExit(f"{label}: end marker missing")
    if text.find(start_marker, start + 1) >= 0:
        raise SystemExit(f"{label}: duplicate start marker")
    return text[:start] + replacement + text[end:]

main = main_path.read_text()
main = replace_once(main,
    "    private LinearLayout root, content, bottomNav;\n",
    "    private LinearLayout root, content;\n    private FrameLayout bottomNav;\n",
    "bottom nav field")
main = replace_once(main,
    "        convoyCountView = null;\n        applyPalette();\n",
    "        convoyCountView = null;\n        talkieState = null;\n        talkiePttButton = null;\n        applyPalette();\n",
    "render reset")

# Compact callback visuals for the floating nav PTT.
main = replace_once(main,
'''            if(talkiePttButton!=null){
                if(transmitting) talkiePttButton.setBackground(roundBg(accent,Color.rgb(52,199,89),20,5));
                else if(receiving) talkiePttButton.setBackground(roundBg(accent,Color.rgb(70,180,255),20,5));
                else talkiePttButton.setBackground(roundBg(accent,accent,20,0));
                if(talkiePttButton instanceof TextView){
                    TextView tv=(TextView)talkiePttButton;
                    if(transmitting) tv.setText("🎙️  ▂▄▆█\\nEN DIRECT");
                    else if(receiving) tv.setText("🔊  ▂▄▆█\\nRÉCEPTION AUDIO");
                    else tv.setText("🎙️\\nMAINTENIR POUR PARLER");
                }
            }
''',
'''            if(talkiePttButton!=null){
                boolean compact=talkiePttButton instanceof TextView && "nav_ptt".equals(talkiePttButton.getTag());
                int radius=compact?40:20;
                if(transmitting) talkiePttButton.setBackground(roundBg(accent,Color.rgb(52,199,89),radius,5));
                else if(receiving) talkiePttButton.setBackground(roundBg(accent,Color.rgb(70,180,255),radius,5));
                else talkiePttButton.setBackground(roundBg(accent,accent,radius,0));
                if(talkiePttButton instanceof TextView){
                    TextView tv=(TextView)talkiePttButton;
                    if(compact){
                        tv.setText(transmitting?"🎙️\\nLIVE":(receiving?"🔊\\nRX":"🎙️\\nPTT"));
                        tv.setTextColor(transmitting||receiving?Color.WHITE:Color.rgb(20,22,24));
                    }else{
                        if(transmitting) tv.setText("🎙️  ▂▄▆█\\nEN DIRECT");
                        else if(receiving) tv.setText("🔊  ▂▄▆█\\nRÉCEPTION AUDIO");
                        else tv.setText("🎙️\\nMAINTENIR POUR PARLER");
                    }
                }
            }
''',
    "talkie callback")

new_home = r'''    private void renderConvoyHome() {
        currentPage="home";
        if(root!=null)root.setPadding(dp(12),dp(4),dp(12),dp(12));
        String code=prefs.get("code","");

        // Compact convoy header: useful driving data stays visible, sharing tools stay folded.
        LinearLayout hero=cardBox();
        hero.setPadding(dp(14),dp(9),dp(14),dp(10));
        LinearLayout.LayoutParams heroLp=(LinearLayout.LayoutParams)hero.getLayoutParams();
        heroLp.setMargins(0,dp(4),0,dp(3));hero.setLayoutParams(heroLp);

        LinearLayout heroTop=new LinearLayout(this);heroTop.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heroLabels=new LinearLayout(this);heroLabels.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow=text("CONVOI EN COURS",10,true,accent);eyebrow.setLetterSpacing(.06f);heroLabels.addView(eyebrow);
        TextView name=text(prefs.get("convoyName","Convoi"),20,true,fg);name.setMaxLines(1);name.setAutoSizeTextTypeUniformWithConfiguration(15,20,1,android.util.TypedValue.COMPLEX_UNIT_SP);heroLabels.addView(name);
        convoyCountView=text(snapshotCountText(),11,true,accent);convoyCountView.setMaxLines(1);heroLabels.addView(convoyCountView);
        heroTop.addView(heroLabels,new LinearLayout.LayoutParams(0,-2,1));
        TextView live=text("● LIVE",9,true,Color.rgb(101,211,117));live.setGravity(Gravity.CENTER);live.setPadding(dp(8),0,dp(8),0);live.setBackground(roundBg(darkTheme?Color.rgb(23,50,29):Color.rgb(233,249,236),Color.rgb(77,150,88),12,1));heroTop.addView(live,new LinearLayout.LayoutParams(-2,dp(26)));
        boolean detailsOpen=prefs.getBool("homeConvoyDetailsExpanded",false);
        TextView detailsChevron=text(detailsOpen?"⌃":"⌄",22,true,accent);detailsChevron.setGravity(Gravity.CENTER);heroTop.addView(detailsChevron,new LinearLayout.LayoutParams(dp(38),dp(42)));
        hero.addView(heroTop);

        LinearLayout codePanel=new LinearLayout(this);codePanel.setOrientation(LinearLayout.VERTICAL);codePanel.setPadding(dp(10),dp(8),dp(10),dp(9));codePanel.setBackground(roundBg(control,border,13,1));
        LinearLayout.LayoutParams cpLp=new LinearLayout.LayoutParams(-1,-2);cpLp.setMargins(0,dp(8),0,0);codePanel.setLayoutParams(cpLp);
        TextView codeV=text("CODE   "+code,13,true,fg);codeV.setLetterSpacing(.05f);codeV.setGravity(Gravity.CENTER);codeV.setMaxLines(1);codePanel.addView(codeV,new LinearLayout.LayoutParams(-1,dp(28)));
        LinearLayout shareRow=new LinearLayout(this);shareRow.setGravity(Gravity.CENTER_VERTICAL);shareRow.setPadding(0,dp(4),0,0);
        Button qr=smallButton("▦  QR",Color.TRANSPARENT,accent);qr.setBackground(roundBg(Color.TRANSPARENT,accent,11,1));qr.setOnClickListener(v->showConvoyQr());LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(0,dp(38),1);qlp.setMargins(0,0,dp(4),0);shareRow.addView(qr,qlp);
        Button share=smallButton("↗  PARTAGER",Color.TRANSPARENT,accent);share.setBackground(roundBg(Color.TRANSPARENT,accent,11,1));share.setOnClickListener(v->shareConvoy());LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(0,dp(38),1);slp.setMargins(dp(4),0,0,0);shareRow.addView(share,slp);codePanel.addView(shareRow);
        codePanel.setVisibility(detailsOpen?View.VISIBLE:View.GONE);hero.addView(codePanel);
        heroTop.setOnClickListener(v->{boolean open=codePanel.getVisibility()==View.VISIBLE;codePanel.setVisibility(open?View.GONE:View.VISIBLE);detailsChevron.setText(open?"⌄":"⌃");prefs.putBool("homeConvoyDetailsExpanded",!open);});
        content.addView(hero);

        compactSectionLabel(content,"POSITION DU CONVOI");
        snapshotArea=new LinearLayout(this);snapshotArea.setOrientation(LinearLayout.VERTICAL);content.addView(snapshotArea,new LinearLayout.LayoutParams(-1,-2));refreshSnapshotArea();

        compactSectionLabel(content,"ACTIONS CONDUITE");
        GridLayout primary=new GridLayout(this);primary.setColumnCount(4);
        String[][] driving={{"🛑","Arrêt","stop"},{"⛽","Essence","fuel"},{"🚻","WC","wc"},{"⚠️","Problème","problem"}};
        for(String[] st:driving){
            View b=quickActionTile(st[0],st[1]);b.setOnClickListener(v->sendStatus(st[2]));
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(66);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));primary.addView(b,lp);
        }
        content.addView(primary,new LinearLayout.LayoutParams(-1,-2));

        boolean moreOpen=prefs.getBool("homeMoreActionsExpanded",false);
        LinearLayout moreHeader=new LinearLayout(this);moreHeader.setGravity(Gravity.CENTER_VERTICAL);moreHeader.setPadding(dp(10),dp(4),dp(8),dp(4));moreHeader.setBackground(roundBg(control,border,13,1));
        LinearLayout.LayoutParams mhLp=new LinearLayout.LayoutParams(-1,dp(42));mhLp.setMargins(dp(3),dp(5),dp(3),dp(2));moreHeader.setLayoutParams(mhLp);
        TextView moreLabel=text("PLUS D’ACTIONS",11,true,muted);moreHeader.addView(moreLabel,new LinearLayout.LayoutParams(0,-1,1));
        TextView moreChevron=text(moreOpen?"⌃":"⌄",20,true,accent);moreChevron.setGravity(Gravity.CENTER);moreHeader.addView(moreChevron,new LinearLayout.LayoutParams(dp(38),-1));content.addView(moreHeader);

        LinearLayout secondary=new LinearLayout(this);secondary.setOrientation(LinearLayout.VERTICAL);secondary.setVisibility(moreOpen?View.VISIBLE:View.GONE);
        GridLayout moreGrid=new GridLayout(this);moreGrid.setColumnCount(2);
        String[][] extra={{"☕","Pause","pause"},{"🚗","Voiture","car_problem"},{"↗️","Je rejoins","joining"},{"👍","OK","ok"}};
        for(String[] st:extra){View b=quickActionTile(st[0],st[1]);b.setOnClickListener(v->sendStatus(st[2]));GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(62);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));moreGrid.addView(b,lp);}secondary.addView(moreGrid,new LinearLayout.LayoutParams(-1,-2));
        Button custom=outlinedButton("✎   AUTRE MESSAGE",Color.rgb(94,99,104));custom.setOnClickListener(v->customStatusDialog());secondary.addView(custom);
        Button clear=ghostButton("ANNULER MON STATUT");clear.setOnClickListener(v->sendStatus("clear"));secondary.addView(clear);
        Button receive=ghostButton(prefs.getBool("talkieReceive",true)?"🔊  RÉCEPTION TALKIE : OUI":"🔇  RÉCEPTION TALKIE : NON");
        receive.setOnClickListener(v->{boolean on=!prefs.getBool("talkieReceive",true);prefs.putBool("talkieReceive",on);if(liveTalkie!=null)liveTalkie.setReceiveEnabled(on);receive.setText(on?"🔊  RÉCEPTION TALKIE : OUI":"🔇  RÉCEPTION TALKIE : NON");toast(on?"Réception live activée":"Réception live coupée");});secondary.addView(receive);
        content.addView(secondary);
        moreHeader.setOnClickListener(v->{boolean open=secondary.getVisibility()==View.VISIBLE;secondary.setVisibility(open?View.GONE:View.VISIBLE);moreChevron.setText(open?"⌄":"⌃");prefs.putBool("homeMoreActionsExpanded",!open);});
    }
'''
main = replace_region(main,
    "    private void renderConvoyHome() {",
    "    private String snapshotCountText()",
    new_home,
    "home method")

new_position = r'''    private View positionCard(String title,JSONObject p,int stripeColor,boolean own){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setPadding(dp(10),dp(7),dp(10),dp(7));outer.setBackground(roundBg(card,own?accent:border,14,own?2:1));
        LinearLayout.LayoutParams outerLp=new LinearLayout.LayoutParams(-1,-2);outerLp.setMargins(0,dp(2),0,dp(2));outer.setLayoutParams(outerLp);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        if(p==null){
            LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.addView(text(title,10,true,stripeColor));labels.addView(text("Position indisponible",14,true,muted));row.addView(labels,new LinearLayout.LayoutParams(0,dp(48),1));outer.addView(row);return outer;
        }
        View carIcon=participantAvatar(p,42,stripeColor);row.addView(carIcon,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(10),0,dp(4),0);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView where=text(title,10,true,stripeColor);top.addView(where,new LinearLayout.LayoutParams(0,dp(18),1));
        if(own){TextView active=text("ACTIF",9,true,Color.rgb(131,220,74));active.setGravity(Gravity.CENTER);active.setPadding(dp(7),0,dp(7),0);active.setBackground(roundBg(darkTheme?Color.rgb(24,48,20):Color.rgb(235,249,232),Color.rgb(92,159,55),10,1));top.addView(active,new LinearLayout.LayoutParams(-2,dp(20)));}labels.addView(top);
        TextView person=text(p.optString("name",prefs.get("profileName","Moi")),16,true,fg);person.setMaxLines(1);person.setAutoSizeTextTypeUniformWithConfiguration(13,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);labels.addView(person);
        String meta=p.optString("vehicle",prefs.get("profileVehicle","Véhicule"));String role=p.optString("role","");if("leader".equals(role))meta+=" · Chef";else if("sweep".equals(role))meta+=" · Balai";TextView vehicle=text(meta,11,false,muted);vehicle.setMaxLines(1);vehicle.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,android.util.TypedValue.COMPLEX_UNIT_SP);labels.addView(vehicle);
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        String relative=own?"":p.optString("_relative","");if(!relative.isEmpty()){TextView distance=text(relative.replace(" devant","").replace(" derrière",""),12,true,stripeColor);distance.setGravity(Gravity.CENTER);distance.setMaxLines(2);row.addView(distance,new LinearLayout.LayoutParams(dp(64),dp(42)));}
        outer.addView(row);
        JSONObject st=p.optJSONObject("activeStatus");if(st!=null){TextView state=text("⚑  "+st.optString("label",""),10,true,accent);state.setMaxLines(1);state.setAutoSizeTextTypeUniformWithConfiguration(9,10,1,android.util.TypedValue.COMPLEX_UNIT_SP);state.setPadding(dp(52),dp(3),0,0);outer.addView(state);}
        return outer;
    }


'''
main = replace_region(main,
    "    private View positionCard(String title,JSONObject p,int stripeColor,boolean own){",
    "    private void renderMapPage()",
    new_position,
    "position card")

# Remove the large talkie card and reuse its press/release logic for the floating nav control.
new_talkie = r'''    private void bindTalkieTouch(TextView ptt){
        if(liveTalkie!=null)liveTalkie.ensureStarted();
        ptt.setClickable(true);ptt.setFocusable(true);ptt.setSoundEffectsEnabled(true);
        ptt.setOnTouchListener((v,e)->{
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                talkieFingerDown=true;v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if(!prefs.hasActiveConvoy()){setTalkiePressedVisual("✕ Aucun convoi actif",danger,false);return true;}
                if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                    talkiePermissionRequestPending=true;setTalkiePressedVisual("● Autorisation micro…",accent,false);
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return true;
                }
                if(liveTalkie!=null){
                    liveTalkie.ensureStarted();
                    boolean ok=liveTalkie.setTransmitting(true);
                    if(ok)ptt.setText("🎙️\nLIVE");
                    setTalkiePressedVisual(ok?(liveTalkie.connectedPeerCount()>0?"● EN DIRECT":"● EN DIRECT · connexion…"):"✕ Micro live indisponible",ok?Color.WHITE:danger,ok);
                }
                return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                talkieFingerDown=false;
                if(liveTalkie!=null)liveTalkie.setTransmitting(false);
                if(!talkiePermissionRequestPending)setTalkieIdleVisual(liveTalkie==null?"◌ Live…":liveTalkie.stateLabel(),Color.rgb(90,200,120));
                if("nav_ptt".equals(ptt.getTag()))ptt.setText("🎙️\nPTT");
                v.performClick();return true;
            }
            return true;
        });
        ptt.setOnClickListener(v->{});
    }
'''
main = replace_region(main,
    "    private void addTalkieWalkieSection(){",
    "    private void setTalkiePressedVisual",
    new_talkie,
    "talkie section")

main = replace_region(main,
    "    private void setTalkiePressedVisual(String message,int color,boolean speaking){",
    "    private void startPolling()",
r'''    private void setTalkiePressedVisual(String message,int color,boolean speaking){
        if(talkiePttButton!=null){
            boolean active=prefs.hasActiveConvoy();
            boolean compact=talkiePttButton instanceof TextView && "nav_ptt".equals(talkiePttButton.getTag());
            int outline=speaking?Color.rgb(52,199,89):(active?accent:danger);
            int stroke=speaking||!active?4:2;
            talkiePttButton.setBackground(roundBg(accent,outline,compact?40:20,stroke));
            if(compact && talkiePttButton instanceof TextView){((TextView)talkiePttButton).setTextColor(speaking?Color.WHITE:Color.rgb(20,22,24));}
        }
        if(talkieState!=null){talkieState.setText(message);talkieState.setTextColor(color);}
    }
    private void setTalkieIdleVisual(String message,int color){
        if(talkiePttButton!=null){boolean compact=talkiePttButton instanceof TextView && "nav_ptt".equals(talkiePttButton.getTag());talkiePttButton.setBackground(roundBg(accent,accent,compact?40:20,0));if(compact&&talkiePttButton instanceof TextView){((TextView)talkiePttButton).setText("🎙️\nPTT");((TextView)talkiePttButton).setTextColor(Color.rgb(20,22,24));}}
        if(talkieState!=null){talkieState.setText(message);talkieState.setTextColor(color);}
    }

''',
    "talkie visual methods")

# Compact action tiles for the one-row driving controls.
main = replace_region(main,
    "    private View quickActionTile(String icon,String label){",
    "    private Button destructiveButton",
r'''    private View quickActionTile(String icon,String label){
        String low=label.toLowerCase(Locale.ROOT);
        boolean urgent=low.contains("arrêt")||low.contains("problème")||low.contains("voiture");
        int tileFill=urgent?(darkTheme?Color.rgb(52,27,27):Color.rgb(255,242,242)):control;
        int tileStroke=urgent?danger:border;
        int iconColor=urgent?danger:accent;
        LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(3),dp(5),dp(3),dp(4));tile.setBackground(roundBg(tileFill,tileStroke,14,urgent?2:1));
        TextView iv=text(icon,22,false,iconColor);iv.setGravity(Gravity.CENTER);tile.addView(iv,new LinearLayout.LayoutParams(-1,dp(31)));
        TextView tv=text(label,11,true,fg);tv.setGravity(Gravity.CENTER);tv.setMaxLines(2);tv.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,android.util.TypedValue.COMPLEX_UNIT_SP);tile.addView(tv,new LinearLayout.LayoutParams(-1,dp(26)));
        return tile;
    }
''',
    "quick action tile")

# Add a tighter section label used by the driving home.
main = replace_once(main,
    "    private void sectionLabel(LinearLayout parent,String label){TextView v=text(label,12,true,accent);v.setLetterSpacing(.05f);v.setPadding(dp(2),dp(17),0,dp(6));parent.addView(v);}\n",
    "    private void sectionLabel(LinearLayout parent,String label){TextView v=text(label,12,true,accent);v.setLetterSpacing(.05f);v.setPadding(dp(2),dp(17),0,dp(6));parent.addView(v);}\n    private void compactSectionLabel(LinearLayout parent,String label){TextView v=text(label,10,true,accent);v.setLetterSpacing(.05f);v.setPadding(dp(2),dp(7),0,dp(3));parent.addView(v);}\n",
    "compact section helper")

new_nav = r'''    private FrameLayout buildBottomNav(){
        FrameLayout nav=new FrameLayout(this);nav.setClipChildren(false);nav.setClipToPadding(false);populateBottomNav(nav);return nav;
    }

    private void populateBottomNav(FrameLayout nav){
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER);bar.setPadding(dp(3),0,dp(3),0);bar.setBackground(roundBg(navSurface,border,20,1));
        FrameLayout.LayoutParams barLp=new FrameLayout.LayoutParams(-1,dp(68),Gravity.BOTTOM);barLp.setMargins(dp(6),dp(18),dp(6),0);nav.addView(bar,barLp);
        if(prefs.hasActiveConvoy()){
            addBottomNavItem(bar,"🏠","Accueil","home");
            addBottomNavItem(bar,"🗺","Carte","map");
            Space gap=new Space(this);bar.addView(gap,new LinearLayout.LayoutParams(0,dp(68),1.15f));
            addBottomNavItem(bar,"👥","Participants","participants");
            addBottomNavItem(bar,"⚙","Réglage","more");

            TextView ptt=text("🎙️\nPTT",12,true,Color.rgb(20,22,24));ptt.setTag("nav_ptt");ptt.setGravity(Gravity.CENTER);ptt.setMaxLines(2);ptt.setPadding(dp(4),dp(4),dp(4),dp(4));ptt.setBackground(roundBg(accent,accent,40,0));ptt.setElevation(dp(10));bindTalkieTouch(ptt);talkiePttButton=ptt;
            FrameLayout.LayoutParams pttLp=new FrameLayout.LayoutParams(dp(74),dp(74),Gravity.TOP|Gravity.CENTER_HORIZONTAL);pttLp.setMargins(0,0,0,0);nav.addView(ptt,pttLp);
        }else{
            addBottomNavItem(bar,"🏠","Accueil","home");addBottomNavItem(bar,"🗺","Carte","map");addBottomNavItem(bar,"👥","Participants","participants");addBottomNavItem(bar,"⚙","Réglage","more");
        }
    }

    private void refreshBottomNav(){
        if(bottomNav==null)return;bottomNav.removeAllViews();talkiePttButton=null;populateBottomNav(bottomNav);
    }

    private void addBottomNavItem(LinearLayout nav,String icon,String label,String page){
        boolean active=("home".equals(page) && ("home".equals(currentPage)||"welcome".equals(currentPage))) || page.equals(currentPage);
        LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setPadding(dp(2),dp(5),dp(2),dp(3));
        TextView iv=text(icon,21,active,active?accent:muted);iv.setGravity(Gravity.CENTER);item.addView(iv,new LinearLayout.LayoutParams(-1,dp(31)));
        TextView tv=text(label,10,active,active?accent:muted);tv.setGravity(Gravity.CENTER);tv.setMaxLines(1);tv.setAutoSizeTextTypeUniformWithConfiguration(8,10,1,android.util.TypedValue.COMPLEX_UNIT_SP);item.addView(tv,new LinearLayout.LayoutParams(-1,dp(22)));
        if(active)item.setBackground(roundBg(darkTheme?Color.rgb(28,29,25):Color.rgb(255,248,228),Color.TRANSPARENT,13,0));
        item.setOnClickListener(v->navigateBottom(page));nav.addView(item,new LinearLayout.LayoutParams(0,dp(62),1));
    }

'''
main = replace_region(main,
    "    private LinearLayout buildBottomNav(){",
    "    private void navigateBottom(String page)",
    new_nav,
    "bottom nav")

# Version labels.
main = replace_once(main, 'Mode Convoi 0.3.35', 'Mode Convoi 0.3.36', 'about version')

# Validation of the final activity shape.
required = [
    'private FrameLayout bottomNav;',
    'homeConvoyDetailsExpanded',
    'homeMoreActionsExpanded',
    'String[][] driving={{"🛑","Arrêt","stop"},{"⛽","Essence","fuel"},{"🚻","WC","wc"},{"⚠️","Problème","problem"}};',
    'ptt.setTag("nav_ptt")',
    'private void bindTalkieTouch(TextView ptt)',
    'private void compactSectionLabel',
    'Mode Convoi 0.3.36'
]
for token in required:
    if token not in main:
        raise SystemExit(f"post-condition missing: {token}")
if 'addTalkieWalkieSection();' in main:
    raise SystemExit('legacy home talkie section call still present')

main_path.write_text(main)

api = api_path.read_text()
api = replace_once(api, 'ModeConvoi-Android/0.3.35', 'ModeConvoi-Android/0.3.36', 'user agent')
api_path.write_text(api)

gradle = gradle_path.read_text()
gradle = replace_once(gradle, "        versionCode 38\n        versionName '0.3.35'", "        versionCode 39\n        versionName '0.3.36'", 'gradle version')
gradle_path.write_text(gradle)

print('Mode Convoi 0.3.36 driving-home migration applied')
