from pathlib import Path
import re

main=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
svc=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/LocationShareService.java')
s=main.read_text()

# Rôles visibles dans les cartes principales.
s=s.replace('names.addView(text(p.optString("vehicle",prefs.get("profileVehicle","Véhicule")),13,false,muted));', '''names.addView(text(p.optString("vehicle",prefs.get("profileVehicle","Véhicule")),13,false,muted));
        String role=p.optString("role","");
        if("leader".equals(role)) names.addView(text("★ Chef de convoi",11,true,Color.rgb(91,196,62)));
        else if("sweep".equals(role)) names.addView(text("◆ Voiture balai",11,true,Color.rgb(55,158,225)));''',1)

# Rôles visibles dans la liste des participants.
s=s.replace('who.addView(text(p.optString("vehicle","Véhicule"),12,false,muted)); row.addView(who,new LinearLayout.LayoutParams(0,-2,1));', '''who.addView(text(p.optString("vehicle","Véhicule"),12,false,muted));
            String role=p.optString("role","");
            if("admin".equals(role)) who.addView(text("Administrateur",11,true,accent));
            else if("leader".equals(role)) who.addView(text("Chef de convoi",11,true,Color.rgb(91,196,62)));
            else if("sweep".equals(role)) who.addView(text("Voiture balai",11,true,Color.rgb(55,158,225)));
            row.addView(who,new LinearLayout.LayoutParams(0,-2,1));''',1)

# Administration dans Participants.
s=s.replace('Button remove=adminButton("⊖  Retirer un participant",false); remove.setOnClickListener(v->manageParticipantsDialog()); content.addView(remove);', 'Button manage=adminButton("👥  Rôles et participants",false); manage.setOnClickListener(v->manageParticipantsDialog()); content.addView(manage);\n            Button stopAll=adminButton("🛑  ARRÊT GÉNÉRAL",true); stopAll.setOnClickListener(v->confirmGeneralStop()); content.addView(stopAll);',1)

# Menu de gestion du convoi.
s=s.replace('new String[]{"Renommer le convoi","Définir le point de regroupement","Gérer les participants","Définir l’apparence","Fermer le convoi"}', 'new String[]{"Renommer le convoi","Définir le point de regroupement","Gérer les participants et rôles","Arrêt général","Définir l’apparence","Fermer le convoi"}',1)
s=s.replace('else if(s.startsWith("Gérer"))manageParticipantsDialog();', 'else if(s.startsWith("Gérer"))manageParticipantsDialog();\n            else if(s.startsWith("Arrêt général"))confirmGeneralStop();',1)

# Gestion des participants : rôle ou retrait.
pat=re.compile(r'    private void manageParticipantsDialog\(\)\{.*?\n    \}\n    private void confirmRemoveParticipant',re.S)
rep='''    private void manageParticipantsDialog(){
        JSONArray ps=snapshot==null?null:snapshot.optJSONArray("participants"); if(ps==null){toast("Participants indisponibles");return;}
        ArrayList<JSONObject> targets=new ArrayList<>(); ArrayList<String> labels=new ArrayList<>(); String me=prefs.get("participantId","");
        for(int i=0;i<ps.length();i++){
            JSONObject p=ps.optJSONObject(i); if(p==null||me.equals(p.optString("id")))continue;
            targets.add(p);
            String role=p.optString("role","");
            String suffix="leader".equals(role)?" · Chef":"sweep".equals(role)?" · Balai":"";
            labels.add(p.optString("name","Participant")+" · "+p.optString("vehicle","Véhicule")+suffix);
        }
        if(targets.isEmpty()){toast("Aucun autre participant");return;}
        new AlertDialog.Builder(this).setTitle("Participants et rôles").setItems(labels.toArray(new String[0]),(d,w)->participantActionsDialog(targets.get(w))).show();
    }
    private void participantActionsDialog(JSONObject target){
        String name=target.optString("name","Participant");
        String[] actions={"★ Définir comme Chef de convoi","◆ Définir comme Voiture balai","Retirer le rôle","⊖ Retirer du convoi"};
        new AlertDialog.Builder(this).setTitle(name).setItems(actions,(d,w)->{
            if(w==0)setParticipantRole(target.optString("id"),"leader");
            else if(w==1)setParticipantRole(target.optString("id"),"sweep");
            else if(w==2)setParticipantRole(target.optString("id"),"");
            else confirmRemoveParticipant(target);
        }).show();
    }
    private void setParticipantRole(String targetId,String role){
        try{JSONObject b=authBody().put("targetParticipantId",targetId).put("role",role);runBusy("Mise à jour du rôle…",()->ConvoyApi.post(prefs.get("serverUrl",""),"/api/convoys/"+prefs.get("code","")+"/role",b,prefs.get("adminKey","")),r->pollOnce());}catch(Exception e){toast(e.getMessage());}
    }
    private void confirmRemoveParticipant'''
s,n=pat.subn(rep,s,count=1)
if n!=1: raise SystemExit('manageParticipants patch failed')

# Heure souhaitée sur le point de regroupement.
pat=re.compile(r'    private void rallyDialog\(\)\{.*?\n    \}\n    private void setRally\(String name,String latS,String lonS\)\{.*?\n',re.S)
rep='''    private void rallyDialog(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(20),0,dp(20),0);
        EditText n=input("Nom","Point de regroupement"); EditText desired=input("Heure souhaitée (ex. 14:30)",""); EditText lat=input("Latitude",""); EditText lon=input("Longitude","");
        l.addView(n);l.addView(desired);l.addView(lat);l.addView(lon);
        JSONObject me=findMe();JSONObject loc=me==null?null:me.optJSONObject("location");if(loc!=null){lat.setText(String.valueOf(loc.optDouble("lat")));lon.setText(String.valueOf(loc.optDouble("lon")));}
        new AlertDialog.Builder(this).setTitle("Point de regroupement").setView(l).setPositiveButton("Partager",(d,w)->setRally(n.getText().toString(),desired.getText().toString(),lat.getText().toString(),lon.getText().toString())).setNegativeButton("Annuler",null).show();
    }
    private void setRally(String name,String desiredTime,String latS,String lonS){try{double lat=Double.parseDouble(latS.replace(',','.')),lon=Double.parseDouble(lonS.replace(',','.')); JSONObject b=authBody().put("name",name).put("desiredTime",desiredTime.trim()).put("lat",lat).put("lon",lon);runBusy("Partage…",()->ConvoyApi.post(prefs.get("serverUrl",""),"/api/convoys/"+prefs.get("code","")+"/rally",b,prefs.get("adminKey","")),r->pollOnce());}catch(Exception e){toast("Coordonnées invalides");}}
'''
s,n=pat.subn(rep,s,count=1)
if n!=1: raise SystemExit('rally patch failed')

s=s.replace('box.addView(text(rally.optString("name","Point de regroupement"),18,true,fg));', 'box.addView(text(rally.optString("name","Point de regroupement"),18,true,fg));\n            String desired=rally.optString("desiredTime",""); if(!desired.isEmpty()) box.addView(text("Heure souhaitée : "+desired,12,true,accent));',1)
s=s.replace('box.addView(text(rally.optString("name","Point de regroupement"),20,true,fg));', 'box.addView(text(rally.optString("name","Point de regroupement"),20,true,fg)); String desired=rally.optString("desiredTime",""); if(!desired.isEmpty()) box.addView(text("Heure souhaitée : "+desired,12,true,accent));',1)

# Arrêt général.
mark='    private void leaveConvoy(){'
methods='''    private void confirmGeneralStop(){
        new AlertDialog.Builder(this).setTitle("ARRÊT GÉNÉRAL ?").setMessage("Tous les participants recevront immédiatement une alerte d’arrêt général.")
                .setNegativeButton("Annuler",null).setPositiveButton("ENVOYER",(d,w)->sendGeneralStop()).show();
    }
    private void sendGeneralStop(){
        try{JSONObject b=authBody();runBusy("Envoi de l’arrêt général…",()->ConvoyApi.post(prefs.get("serverUrl",""),"/api/convoys/"+prefs.get("code","")+"/general-stop",b,prefs.get("adminKey","")),r->pollOnce());}catch(Exception e){toast(e.getMessage());}
    }

'''
if mark not in s: raise SystemExit('leave marker missing')
s=s.replace(mark,methods+mark,1)

# Notifications pour les nouvelles catégories d’événements.
s=s.replace('e.optString("type").equals("close")||e.optString("type").equals("status-clear-auto")', 'e.optString("type").equals("close")||e.optString("type").equals("role")||e.optString("type").equals("general-stop")||e.optString("type").equals("status-clear-auto")',1)
s=s.replace('if(type.equals("rally"))return "⚑";return "•";', 'if(type.equals("rally"))return "⚑";if(type.equals("general-stop"))return "🛑";if(type.equals("role"))return "★";return "•";',1)

# Version affichée dans Plus.
s=s.replace('Mode Convoi 0.3.2','Mode Convoi 0.3.3')

main.write_text(s)

ss=svc.read_text()
ss=ss.replace('type.equals("close") || type.equals("status-clear-auto")', 'type.equals("close") || type.equals("role") || type.equals("general-stop") || type.equals("status-clear-auto")',1)
svc.write_text(ss)
print('0.3.3 UX patch applied')
