from pathlib import Path
import re

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

# Welcome: keep server value internally but do not show the technical URL in normal use.
s=s.replace('EditText server=profileInput(profile,"🌐","Serveur",prefs.get("serverUrl","http://192.168.1.25:8787"));\n        EditText convoyName=', 'EditText server=input("Serveur",prefs.get("serverUrl","http://192.168.1.25:8787"));\n        EditText convoyName=')
s=s.replace('EditText code=profileInput(joinCard,"#","Code du convoi",prefs.get("pendingJoin",""));', 'EditText code=profileInput(joinCard,"#","Code du convoi (6 caractères)",prefs.get("pendingJoin",""));')

# Make QR generation obvious after convoy creation.
old='r->{ ensurePermissionsAndService(); render(); startPolling(); });'
new='r->{ ensurePermissionsAndService(); render(); startPolling(); ui.postDelayed(this::showConvoyQr,250); });'
if old in s:
    s=s.replace(old,new,1)

# Rename the home QR action so it is explicit.
s=s.replace('Button qr=smallButton("QR",card,fg);', 'Button qr=smallButton("MON QR",card,fg);')
s=s.replace('codeRow.addView(qr,new LinearLayout.LayoutParams(dp(58),dp(40)));', 'codeRow.addView(qr,new LinearLayout.LayoutParams(dp(82),dp(40)));')

# Bottom navigation: all tabs open, even without an active convoy. Plus is a real page.
old_nav='''        if("more".equals(page)){
            if(prefs.hasActiveConvoy()) convoyOptions(); else themeDialog();
            return;
        }
        if(!prefs.hasActiveConvoy()){
            toast("Crée ou rejoins un convoi d’abord");
            return;
        }
        if("map".equals(page)) renderMapPage();
        else if("participants".equals(page)) renderParticipantsPage();
        refreshBottomNav();'''
new_nav='''        if("more".equals(page)){
            renderMorePage();
            refreshBottomNav();
            return;
        }
        if("map".equals(page)) renderMapPage();
        else if("participants".equals(page)) renderParticipantsPage();
        refreshBottomNav();'''
if old_nav not in s:
    raise SystemExit('bottom navigation block not found')
s=s.replace(old_nav,new_nav,1)

# Add a proper More/settings page before themeDialog.
marker='    private void themeDialog(){'
if marker not in s:
    raise SystemExit('themeDialog marker not found')
more='''    private void renderMorePage(){
        currentPage="more";
        mapView=null;
        content.removeAllViews();
        TextView title=text("PLUS",24,true,fg); title.setPadding(dp(4),dp(16),0,dp(8)); content.addView(title);

        sectionLabel(content,"RÉGLAGES");
        LinearLayout settings=cardBox();
        Button appearance=ghostButton("◐   APPARENCE"); appearance.setOnClickListener(v->themeDialog()); settings.addView(appearance);
        Button advanced=ghostButton("⚙   PARAMÈTRES AVANCÉS"); advanced.setOnClickListener(v->advancedSettingsDialog()); settings.addView(advanced);
        content.addView(settings);

        if(prefs.hasActiveConvoy()){
            sectionLabel(content,"CONVOI EN COURS");
            LinearLayout convoy=cardBox();
            convoy.addView(text(prefs.get("convoyName","Convoi"),19,true,fg));
            convoy.addView(text("Code : "+prefs.get("code",""),13,false,muted));
            Button qr=outlinedButton("▦   AFFICHER LE QR D’INVITATION",accent); qr.setOnClickListener(v->showConvoyQr()); convoy.addView(qr);
            Button manage=outlinedButton("⚙   GÉRER LE CONVOI",Color.rgb(94,99,104)); manage.setOnClickListener(v->convoyOptions()); convoy.addView(manage);
            content.addView(convoy);
        }else{
            cardTitle(content,"Aucun convoi actif","Tu peux quand même ouvrir Carte, Participants et les réglages. Pour partager des positions, crée ou rejoins un convoi depuis Accueil.");
        }

        sectionLabel(content,"À PROPOS");
        cardTitle(content,"Mode Convoi 0.3.2","Le code à 6 caractères identifie un convoi. Le QR contient exactement ce code et permet aux autres téléphones de le rejoindre sans le saisir.");
    }

    private void advancedSettingsDialog(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(6),dp(20),0);
        TextView help=text("Réglage technique du serveur de synchronisation. Tu n’as normalement pas besoin d’y toucher.",12,false,muted); help.setPadding(0,0,0,dp(8)); box.addView(help);
        EditText server=input("Adresse du serveur",prefs.get("serverUrl","http://192.168.1.25:8787")); box.addView(server);
        new AlertDialog.Builder(this).setTitle("Paramètres avancés").setView(box).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{prefs.put("serverUrl",server.getText().toString().trim());toast("Serveur enregistré");}).show();
    }

'''
s=s.replace(marker,more+marker,1)

# Empty-state language on participants should be understandable without an active convoy.
s=s.replace('cardTitle(content,"Aucune donnée","Synchronisation du convoi en cours.");return;', 'cardTitle(content,"Aucun participant","Crée ou rejoins un convoi depuis Accueil pour voir les voitures ici.");return;')

p.write_text(s)
print('0.3.2 UX patch applied')
