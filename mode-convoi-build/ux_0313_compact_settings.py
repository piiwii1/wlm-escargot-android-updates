from pathlib import Path
import re

p = Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s = p.read_text()

vehicle = r'''    private void vehicleAppearanceDialog(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),dp(14));scroll.addView(box);
        vehiclePreview=new FrameLayout(this);vehiclePreview.setPadding(dp(8),dp(8),dp(8),dp(8));box.addView(vehiclePreview,new LinearLayout.LayoutParams(-1,dp(92)));refreshVehiclePreview();

        // Icônes génériques : une seule ligne visible, deux lignes supplémentaires à déplier.
        LinearLayout iconHeader=new LinearLayout(this);iconHeader.setGravity(Gravity.CENTER_VERTICAL);iconHeader.setPadding(0,dp(6),0,dp(3));
        TextView il=text("CHOISIS UNE ICÔNE",11,true,accent);iconHeader.addView(il,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView iconChevron=text("⌄",22,true,accent);iconChevron.setGravity(Gravity.CENTER);iconHeader.addView(iconChevron,new LinearLayout.LayoutParams(dp(44),dp(40)));box.addView(iconHeader);
        String[] all={"🚗","🚙","🏎️","🚐","🛻","🚕","🚓","🚘","🚖","🚚","🚌","🚜","🏁","⚡","★"};
        GridLayout iconsFirst=new GridLayout(this);iconsFirst.setColumnCount(5);
        GridLayout iconsMore=new GridLayout(this);iconsMore.setColumnCount(5);iconsMore.setVisibility(View.GONE);
        for(int i=0;i<all.length;i++){
            final String ic=all[i];TextView b=text(ic,25,false,fg);b.setGravity(Gravity.CENTER);
            boolean selected=ic.equals(prefs.get("profileVehicleIcon","🚗")) && prefs.get("profileVehicleImage","").isEmpty();
            b.setBackground(roundBg(control,selected?accent:border,12,selected?2:1));
            b.setOnClickListener(v->{prefs.put("profileVehicleIcon",ic);prefs.remove("profileVehicleImage");refreshVehiclePreview();});
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(56);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));
            (i<5?iconsFirst:iconsMore).addView(b,lp);
        }
        box.addView(iconsFirst,new LinearLayout.LayoutParams(-1,-2));box.addView(iconsMore,new LinearLayout.LayoutParams(-1,-2));
        iconHeader.setOnClickListener(v->{boolean open=iconsMore.getVisibility()==View.VISIBLE;iconsMore.setVisibility(open?View.GONE:View.VISIBLE);iconChevron.setText(open?"⌄":"⌃");});

        // Emplacements réservés au futur pack d'icônes Volkswagen.
        LinearLayout vwHeader=new LinearLayout(this);vwHeader.setGravity(Gravity.CENTER_VERTICAL);vwHeader.setPadding(0,dp(10),0,dp(3));
        TextView vwTitle=text("ICÔNES VOLKSWAGEN",11,true,accent);vwHeader.addView(vwTitle,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView vwChevron=text("⌄",22,true,accent);vwChevron.setGravity(Gravity.CENTER);vwHeader.addView(vwChevron,new LinearLayout.LayoutParams(dp(44),dp(40)));box.addView(vwHeader);
        GridLayout vwFirst=new GridLayout(this);vwFirst.setColumnCount(5);
        GridLayout vwMore=new GridLayout(this);vwMore.setColumnCount(5);vwMore.setVisibility(View.GONE);
        for(int i=0;i<15;i++){
            TextView slot=text("",18,false,muted);slot.setGravity(Gravity.CENTER);slot.setBackground(roundBg(control,border,12,1));slot.setAlpha(.55f);
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(54);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));
            (i<5?vwFirst:vwMore).addView(slot,lp);
        }
        box.addView(vwFirst,new LinearLayout.LayoutParams(-1,-2));box.addView(vwMore,new LinearLayout.LayoutParams(-1,-2));
        TextView vwHint=text("Emplacements réservés au futur pack Volkswagen.",11,false,muted);vwHint.setPadding(dp(4),dp(3),dp(4),0);box.addView(vwHint);
        vwHeader.setOnClickListener(v->{boolean open=vwMore.getVisibility()==View.VISIBLE;vwMore.setVisibility(open?View.GONE:View.VISIBLE);vwChevron.setText(open?"⌄":"⌃");});

        TextView cl=text("COULEUR DU REPÈRE",11,true,accent);cl.setPadding(0,dp(14),0,dp(6));box.addView(cl);
        GridLayout colors=new GridLayout(this);colors.setColumnCount(4);String[] cs={"#FFB514","#EF4444","#3B82F6","#22C55E","#A855F7","#F97316","#E5E7EB","#111827"};
        for(String c:cs){
            boolean selected=c.equalsIgnoreCase(prefs.get("profileVehicleMarkerColor","#FFB514"));
            TextView dot=text("●",38,false,Color.parseColor(c));dot.setGravity(Gravity.CENTER);dot.setBackground(roundBg(control,selected?accent:border,14,selected?2:1));
            dot.setOnClickListener(v->{prefs.put("profileVehicleMarkerColor",c);refreshVehiclePreview();});
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(60);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(4),dp(4),dp(4),dp(4));colors.addView(dot,lp);
        }
        box.addView(colors,new LinearLayout.LayoutParams(-1,-2));
        Button photo=outlinedButton("▣   CHOISIR MA PROPRE IMAGE",accent);photo.setOnClickListener(v->pickVehicleImage());box.addView(photo);
        if(!prefs.get("profileVehicleImage","").isEmpty()){Button remove=ghostButton("RETIRER L’IMAGE PERSONNELLE");remove.setOnClickListener(v->{prefs.remove("profileVehicleImage");refreshVehiclePreview();});box.addView(remove);}
        TextView hint=text("Choisis une icône, une couleur de repère ou une photo. L’aperçu est mis à jour immédiatement.",12,false,muted);hint.setPadding(0,dp(8),0,0);box.addView(hint);
        Dialog dlg=styledDialog("Mon véhicule",scroll,"FERMER",d->{vehiclePreview=null;return true;},"ENREGISTRER",d->{syncProfileToServer();vehiclePreview=null;if("participants".equals(currentPage))renderParticipantsPage();else if("map".equals(currentPage))pushMap();else if("home".equals(currentPage))refreshSnapshotArea();return true;},false);
        dlg.setOnDismissListener(d->vehiclePreview=null);
    }
'''

s, n = re.subn(r'    private void vehicleAppearanceDialog\(\)\{.*?(?=    private void refreshVehiclePreview\(\))', vehicle, s, flags=re.S)
if n != 1:
    raise SystemExit(f'vehicleAppearanceDialog replacement count={n}')

# Supprime le titre PLUS en haut de la page Réglages.
s, n = re.subn(r'        TextView title=text\("PLUS",24,true,fg\); title\.setPadding\(dp\(4\),dp\(16\),0,dp\(8\)\); content\.addView\(title\);\n\n', '', s)
if n != 1:
    raise SystemExit(f'PLUS title removal count={n}')

# Menu inférieur : Plus -> Réglages avec engrenage.
s = s.replace('addBottomNavItem(nav,"•••","Plus","more");', 'addBottomNavItem(nav,"⚙","Réglages","more");')
s = s.replace('addBottomNavItem(bottomNav,"•••","Plus","more");', 'addBottomNavItem(bottomNav,"⚙","Réglages","more");')
if '"•••","Plus","more"' in s or 'TextView title=text("PLUS"' in s:
    raise SystemExit('legacy Plus UI still present')
if s.count('"⚙","Réglages","more"') != 2:
    raise SystemExit('settings nav replacement incomplete')

p.write_text(s)
print('Mode Convoi 0.3.13 compact vehicle/settings patch applied')
