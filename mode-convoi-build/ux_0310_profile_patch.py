from pathlib import Path
p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

s=s.replace('        prefs = new AppPrefs(this);\n', '        prefs = new AppPrefs(this);\n        ensureParticipantDefaults();\n',1)

anchor='    private void handleDeepLink(Intent i) {'
helper='''    private void ensureParticipantDefaults(){
        if(!prefs.getBool("participantDefaultsV0310",false)){
            String oldName=prefs.get("profileName","").trim();
            if("PiiWii".equalsIgnoreCase(oldName) && !prefs.hasActiveConvoy()) prefs.put("profileName","");
            String[] markerColors={"#EF4444","#3B82F6","#22C55E","#A855F7","#F97316","#14B8A6","#EC4899","#EAB308","#64748B","#06B6D4"};
            String current=prefs.get("profileVehicleMarkerColor","");
            if(current.isEmpty() || "#FFB514".equalsIgnoreCase(current)){
                int idx=Math.floorMod(java.util.UUID.randomUUID().getLeastSignificantBits(),markerColors.length);
                prefs.put("profileVehicleMarkerColor",markerColors[idx]);
            }
            prefs.putBool("participantDefaultsV0310",true);
        }
    }
    private boolean saveProfileChecked(EditText pseudo,EditText vehicle,EditText color,EditText server){
        String name=pseudo.getText().toString().trim();
        if(name.isEmpty()){
            pseudo.requestFocus();
            pseudo.setError("Choisis ton pseudo");
            toast("Choisis un pseudo avant de continuer");
            return false;
        }
        saveProfile(pseudo,vehicle,color,server);
        return true;
    }

'''
if anchor not in s: raise SystemExit('handleDeepLink anchor missing')
s=s.replace(anchor,helper+anchor,1)

s=s.replace('prefs.get("profileName","PiiWii")','prefs.get("profileName","")')

old='create.setOnClickListener(v->{saveProfile(pseudo,vehicle,color,server);createConvoy(convoyName.getText().toString());});'
new='create.setOnClickListener(v->{if(saveProfileChecked(pseudo,vehicle,color,server))createConvoy(convoyName.getText().toString());});'
if old not in s: raise SystemExit('create listener missing')
s=s.replace(old,new,1)
old='join.setOnClickListener(v->{saveProfile(pseudo,vehicle,color,server);joinConvoy(code.getText().toString());});'
new='join.setOnClickListener(v->{if(saveProfileChecked(pseudo,vehicle,color,server))joinConvoy(code.getText().toString());});'
if old not in s: raise SystemExit('join listener missing')
s=s.replace(old,new,1)
old='            saveProfile(pseudo,vehicle,color,server);\n            joinConvoy(code);'
new='            if(!saveProfileChecked(pseudo,vehicle,color,server)) return;\n            joinConvoy(code);'
if old not in s: raise SystemExit('QR profile save missing')
s=s.replace(old,new,1)

old='''        Button appearance=ghostButton("◐   APPARENCE"); appearance.setOnClickListener(v->themeDialog()); settings.addView(appearance);
        Button myVehicle=ghostButton("🚗   MON VÉHICULE");'''
new='''        Button appearance=ghostButton("◐   APPARENCE"); appearance.setOnClickListener(v->themeDialog()); settings.addView(appearance);
        Button myProfile=ghostButton("👤   MON PROFIL"); myProfile.setOnClickListener(v->profileDialog()); settings.addView(myProfile);
        Button myVehicle=ghostButton("🚗   MON VÉHICULE");'''
if old not in s: raise SystemExit('more settings anchor missing')
s=s.replace(old,new,1)

anchor='    private void vehicleAppearanceDialog(){'
profile='''    private void profileDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),dp(10));
        EditText pseudo=profileInput(box,"👤","Pseudo",prefs.get("profileName",""));
        EditText vehicle=profileInput(box,"🚗","Véhicule",prefs.get("profileVehicle","Véhicule"));
        EditText color=profileInput(box,"●","Couleur réelle du véhicule",prefs.get("profileColor",""));
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Mon profil").setView(box).setNegativeButton("Annuler",null).setPositiveButton("ENREGISTRER",null).create();
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String name=pseudo.getText().toString().trim();
            if(name.isEmpty()){pseudo.setError("Choisis ton pseudo");pseudo.requestFocus();return;}
            prefs.put("profileName",name);prefs.put("profileVehicle",vehicle.getText().toString().trim());prefs.put("profileColor",color.getText().toString().trim());
            syncProfileToServer();dlg.dismiss();
            if("participants".equals(currentPage))renderParticipantsPage();else if("map".equals(currentPage))pushMap();else render();
        }));
        dlg.show();
    }

'''
if anchor not in s: raise SystemExit('vehicleAppearanceDialog anchor missing')
s=s.replace(anchor,profile+anchor,1)
s=s.replace('toast("Véhicule mis à jour")','toast("Profil mis à jour")')
s=s.replace('Mode Convoi 0.3.9','Mode Convoi 0.3.10')
p.write_text(s)
print('0.3.10 pseudo/random-color patch applied')
