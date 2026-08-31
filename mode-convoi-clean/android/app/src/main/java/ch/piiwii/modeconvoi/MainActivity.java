package ch.piiwii.modeconvoi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.provider.Settings;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.*;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

public class MainActivity extends Activity {
    private static final String DEFAULT_SERVER = "https://piiwii.ch/wp-json/mode-convoi/v1";
    private AppPrefs prefs;
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout root, content;
    private FrameLayout bottomNav, screenRoot;
    private TextView talkieSpeakerBanner;
    private boolean activityForeground=false;
    private String activeTalkieSpeaker="";
    private Runnable talkieSpeakerClearRunnable;
    private ScrollView mainScroll;
    private TextView connectionBadge;
    private JSONObject snapshot;
    private WebView mapView;
    private LinearLayout snapshotArea;
    private TextView convoyCountView;
    private String currentPage = "welcome";
    private boolean busyOperation=false;
    private ConvoyPollingController pollingController;
    private ConvoySessionManager sessionManager;
    private ConvoyMapController mapController;
    private ConvoyPositionResolver positionResolver;
    private ConvoyVisualAlertController visualAlertController;
    private int bg, card, fg, muted, accent, danger, border, control, navSurface;
    private boolean darkTheme;
    private static final int REQ_LOCATION = 1001, REQ_NOTIF = 1002, REQ_AUDIO = 1003, REQ_VEHICLE_IMAGE = 2001;
    private FrameLayout vehiclePreview;
    private Dialog fullScreenMapDialog;
    private TextView talkieState;
    private View talkiePttButton;
    private boolean talkiePermissionRequestPending=false;
    private boolean talkieFingerDown=false;
    private LiveTalkieManager liveTalkie;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new AppPrefs(this);
        liveTalkie = new LiveTalkieManager(this,prefs,(label,connected,total,transmitting)->{
            boolean receiving=label!=null&&label.startsWith("🔊");
            if(talkieState!=null){
                talkieState.setText(label);
                int c=transmitting?Color.rgb(52,199,89):(receiving?Color.rgb(70,180,255):(label.startsWith("⚠")||label.startsWith("✕")?danger:Color.rgb(90,200,120)));
                talkieState.setTextColor(c);
            }
            if(talkiePttButton!=null){
                boolean compact=talkiePttButton instanceof TextView && "nav_ptt".equals(talkiePttButton.getTag());
                int radius=compact?40:20;
                if(transmitting) talkiePttButton.setBackground(roundBg(accent,Color.rgb(52,199,89),radius,5));
                else if(receiving) talkiePttButton.setBackground(roundBg(accent,Color.rgb(70,180,255),radius,5));
                else talkiePttButton.setBackground(roundBg(accent,accent,radius,0));
                if(talkiePttButton instanceof TextView){
                    TextView tv=(TextView)talkiePttButton;
                    if(compact){
                        tv.setText(transmitting?"🎙️\nLIVE":(receiving?"🔊\nRX":"🎙️\nPTT"));
                        tv.setTextColor(transmitting||receiving?Color.WHITE:Color.rgb(20,22,24));
                    }else{
                        if(transmitting) tv.setText("🎙️  ▂▄▆█\nEN DIRECT");
                        else if(receiving) tv.setText("🔊  ▂▄▆█\nRÉCEPTION AUDIO");
                        else tv.setText("🎙️\nMAINTENIR POUR PARLER");
                    }
                }
            }
            handleTalkieSpeakerState(label,receiving);
        });
        ParticipantDefaults.ensure(prefs);
        String savedServer=prefs.get("serverUrl","");
        boolean oldLocalServer=savedServer.startsWith("http://192.168.") || savedServer.startsWith("http://10.") || savedServer.startsWith("http://172.");
        // Hors session active, tous les nouveaux convois utilisent le serveur officiel.
        // Cela évite qu'un téléphone conserve silencieusement une ancienne URL et cherche le QR sur un autre serveur.
        if(!prefs.hasActiveConvoy()) prefs.put("serverUrl",DEFAULT_SERVER);
        else if(savedServer.isEmpty()) prefs.put("serverUrl",DEFAULT_SERVER);
        if(oldLocalServer && prefs.hasActiveConvoy()) { prefs.clearSession(); snapshot=null; prefs.put("serverUrl",DEFAULT_SERVER); }
        applyPalette();
        sessionManager = new ConvoySessionManager(prefs,DEFAULT_SERVER);
        mapController = new ConvoyMapController(prefs,()->snapshot);
        positionResolver = new ConvoyPositionResolver(prefs);
        visualAlertController = new ConvoyVisualAlertController(this);
        pollingController = new ConvoyPollingController(this,prefs,DEFAULT_SERVER,new ConvoyPollingController.Listener(){
            @Override public void onSnapshot(JSONObject s,boolean renamed,long synchronizedAt){
                snapshot=s;
                if(liveTalkie!=null)liveTalkie.ensureStarted();
                if(renamed&&"home".equals(currentPage))render();
                else if(mapView!=null)mapController.pushAll();
                else if("home".equals(currentPage))refreshSnapshotArea();
                else if("participants".equals(currentPage))renderParticipantsPage();
            }
            @Override public void onConnectionState(ConvoyPollingController.ConnectionState state,int failures){
                if(connectionBadge==null)return;
                if(state==ConvoyPollingController.ConnectionState.CONNECTED){
                    connectionBadge.setText("● CONNECTÉ");connectionBadge.setTextColor(Color.rgb(90,200,120));
                }else if(state==ConvoyPollingController.ConnectionState.RECONNECTING){
                    connectionBadge.setText("● RECONNEXION");connectionBadge.setTextColor(accent);
                }else{
                    connectionBadge.setText("● HORS LIGNE");connectionBadge.setTextColor(danger);
                }
            }
            @Override public void onSessionInvalidated(int statusCode){
                toast(statusCode==404?"Convoi fermé ou introuvable":"Accès au convoi retiré");
                endSession();
            }
        });
        NotificationHelper.ensureAlertChannel(this);
        handleDeepLink(getIntent());
        render();
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); handleDeepLink(intent); render(); }
    @Override protected void onResume() { super.onResume(); activityForeground=true; if(liveTalkie!=null)liveTalkie.setHostForeground(true); NotificationHelper.clearTalkieSpeaker(this); if(visualAlertController!=null)ConvoyForegroundAlertBus.register(visualAlertController); if (prefs.hasActiveConvoy()) { startPolling(); startShareServiceIfPermitted(); if(liveTalkie!=null)liveTalkie.ensureStarted(); } }
    @Override protected void onPause() { activityForeground=false; if(liveTalkie!=null)liveTalkie.setHostForeground(false); if(visualAlertController!=null)ConvoyForegroundAlertBus.unregister(visualAlertController); super.onPause(); stopPolling(); }
    @Override protected void onDestroy() { NotificationHelper.clearTalkieSpeaker(this); if(talkieSpeakerClearRunnable!=null)ui.removeCallbacks(talkieSpeakerClearRunnable); if(visualAlertController!=null){ConvoyForegroundAlertBus.unregister(visualAlertController);visualAlertController.close();} if(mapController!=null)mapController.close(); if(pollingController!=null)pollingController.close(); if(liveTalkie!=null)liveTalkie.close(); io.shutdownNow(); super.onDestroy(); }

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

    private void handleDeepLink(Intent i) {
        Uri d=i==null?null:i.getData();
        if(d!=null && "modeconvoi".equalsIgnoreCase(d.getScheme()) && "join".equalsIgnoreCase(d.getHost())) {
            String code=d.getLastPathSegment();
            if(code!=null){
                code=code.trim().toUpperCase(Locale.ROOT);
                if(code.matches("[A-Z0-9]{6}")){
                    prefs.put("pendingJoin",code);
                    if(!prefs.hasActiveConvoy())prefs.put("serverUrl",DEFAULT_SERVER);
                    else if(!code.equalsIgnoreCase(prefs.get("code","")))toast("Invitation reçue · quitte d’abord le convoi actuel pour rejoindre "+code);
                }
            }
        }
    }

    private void applyPalette() {
        String mode = prefs == null ? "system" : prefs.get("theme", "system");
        if("system".equals(mode)){
            int night=getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            darkTheme=night==Configuration.UI_MODE_NIGHT_YES;
        }else darkTheme = !mode.equals("light");
        if (darkTheme) {
            bg=Color.rgb(9,11,13); card=Color.rgb(23,26,29); control=Color.rgb(29,32,35); navSurface=Color.rgb(13,15,17);
            fg=Color.rgb(250,250,251); muted=Color.rgb(190,194,199); border=Color.rgb(58,62,66);
        } else {
            bg=Color.rgb(245,246,248); card=Color.WHITE; control=Color.rgb(249,250,251); navSurface=Color.WHITE;
            fg=Color.rgb(20,23,26); muted=Color.rgb(67,73,79); border=Color.rgb(207,212,217);
        }
        accent=Color.rgb(255,181,20); danger=Color.rgb(211,55,48);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(navSurface);
        getWindow().getDecorView().setSystemUiVisibility(darkTheme?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void render() {
        mapView = null;
        convoyCountView = null;
        talkieState = null;
        talkiePttButton = null;
        talkieSpeakerBanner = null;
        screenRoot = null;
        applyPalette();

        FrameLayout screen = new FrameLayout(this);
        screenRoot=screen;
        screen.setBackgroundColor(bg);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(bg);
        screen.addView(shell, new FrameLayout.LayoutParams(-1,-1));

        ScrollView scroll = new ScrollView(this);
        mainScroll = scroll;
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(bg);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(8),dp(16),dp(28));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        setContentView(screen);
        header();
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content,new LinearLayout.LayoutParams(-1,-2));

        if(prefs.hasActiveConvoy()) renderConvoyHome(); else renderWelcome();

        bottomNav = buildBottomNav();
        shell.addView(bottomNav,new LinearLayout.LayoutParams(-1,-2));
        installTalkieSpeakerBanner(screen);

        screen.setOnApplyWindowInsetsListener((v,insets)->{
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            shell.setPadding(0,topInset,0,0);
            if(bottomNav!=null) bottomNav.setPadding(dp(4),dp(4),dp(4),bottomInset+dp(4));
            if(talkieSpeakerBanner!=null){FrameLayout.LayoutParams blp=(FrameLayout.LayoutParams)talkieSpeakerBanner.getLayoutParams();blp.bottomMargin=bottomInset+dp(94);talkieSpeakerBanner.setLayoutParams(blp);}
            return insets;
        });
        screen.requestApplyInsets();
    }

    private void installTalkieSpeakerBanner(FrameLayout screen){
        TextView banner=text("",14,true,Color.WHITE);
        banner.setGravity(Gravity.CENTER);
        banner.setMaxLines(1);
        banner.setPadding(dp(16),0,dp(16),0);
        banner.setMaxWidth(Math.max(dp(260),getResources().getDisplayMetrics().widthPixels-dp(32)));
        banner.setBackground(roundBg(Color.rgb(24,82,116),Color.rgb(70,180,255),18,2));
        banner.setElevation(dp(12));
        banner.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,dp(44),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin=dp(94);
        screen.addView(banner,lp);
        talkieSpeakerBanner=banner;
    }

    private void handleTalkieSpeakerState(String label,boolean receiving){
        if(!receiving||label==null)return;
        String speaker=talkieSpeakerName(label);
        if(speaker.isEmpty())speaker="Un participant";
        boolean changed=!speaker.equals(activeTalkieSpeaker);
        activeTalkieSpeaker=speaker;
        if(talkieSpeakerClearRunnable!=null)ui.removeCallbacks(talkieSpeakerClearRunnable);
        if(activityForeground){
            NotificationHelper.clearTalkieSpeaker(this);
            if(talkieSpeakerBanner!=null){
                talkieSpeakerBanner.setText(label.toUpperCase(Locale.ROOT));
                talkieSpeakerBanner.setVisibility(View.VISIBLE);
            }
        }else if(changed){
            NotificationHelper.notifyTalkieSpeaker(this,speaker);
        }
        talkieSpeakerClearRunnable=()->{
            activeTalkieSpeaker="";
            if(talkieSpeakerBanner!=null)talkieSpeakerBanner.setVisibility(View.GONE);
            NotificationHelper.clearTalkieSpeaker(this);
        };
        ui.postDelayed(talkieSpeakerClearRunnable,1100);
    }

    private String talkieSpeakerName(String label){
        if(label==null||!label.startsWith("🔊"))return "";
        String value=label.substring("🔊".length()).trim();
        int talks=value.toLowerCase(Locale.ROOT).indexOf(" parle");
        if(talks>0)return value.substring(0,talks).trim();
        if(value.toLowerCase(Locale.ROOT).startsWith("réception audio"))return "Un participant";
        int meter=value.indexOf("  ");
        return (meter>0?value.substring(0,meter):value).trim();
    }

    private void header() {
        LinearLayout h=new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(4),dp(4),dp(4),dp(6));

        Space left=new Space(this);
        h.addView(left,new LinearLayout.LayoutParams(dp(104),dp(50)));

        TextView title=text("MODE CONVOI",19,true,accent);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(1);
        title.setAutoSizeTextTypeUniformWithConfiguration(15,19,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        h.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));

        connectionBadge=text(prefs.hasActiveConvoy()?"●  CONNECTÉ":"INACTIF",11,true,prefs.hasActiveConvoy()?Color.rgb(110,204,70):muted);
        connectionBadge.setGravity(Gravity.CENTER);
        connectionBadge.setMaxLines(1);
        connectionBadge.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        connectionBadge.setPadding(dp(8),0,dp(8),0);
        connectionBadge.setBackground(roundBg(prefs.hasActiveConvoy()?Color.rgb(26,47,24):Color.rgb(42,44,46), prefs.hasActiveConvoy()?Color.rgb(71,133,48):Color.rgb(76,79,82), 18, 1));
        connectionBadge.setOnClickListener(v->{ if(prefs.hasActiveConvoy()) sessionStatusDialog(); else navigateBottom("home"); });
        h.addView(connectionBadge,new LinearLayout.LayoutParams(dp(104),dp(34)));
        root.addView(h);

        View line=new View(this);
        line.setBackgroundColor(border);
        root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }

    private void renderWelcome() {
        currentPage="welcome"; sectionLabel(content,"MON PROFIL"); LinearLayout profile=cardBox();
        EditText pseudo=profileInput(profile,"👤","Prénom ou pseudo",prefs.get("profileName",""));
        EditText vehicle=profileInput(profile,"🚗","Véhicule",prefs.get("profileVehicle",""));
        EditText color=profileInput(profile,"●","Couleur",prefs.get("profileColor",""));
        EditText server=input("Serveur",prefs.get("serverUrl",DEFAULT_SERVER));
        EditText convoyName=profileInput(profile,"⚑","Nom du convoi","VW Suisse – Gênes"); Button vehicleLook=ghostButton("🚗   APPARENCE DU VÉHICULE"); vehicleLook.setOnClickListener(v->vehicleAppearanceDialog()); profile.addView(vehicleLook); content.addView(profile);
        sectionLabel(content,"REJOINDRE"); LinearLayout joinCard=cardBox(); EditText code=profileInput(joinCard,"#","Code du convoi (6 caractères)",prefs.get("pendingJoin","")); code.setAllCaps(true); content.addView(joinCard);
        Button create=button("👥   CRÉER UN CONVOI",accent,Color.rgb(17,18,19)); create.setOnClickListener(v->{if(saveProfileChecked(pseudo,vehicle,color,server))createConvoy(convoyName.getText().toString());}); content.addView(create);
        Button scan=outlinedButton("▦   SCANNER UN QR",accent); scan.setOnClickListener(v->scanJoinQr(pseudo,vehicle,color,server)); content.addView(scan);
        Button join=button("↪   REJOINDRE LE CONVOI",accent,Color.rgb(17,18,19)); join.setOnClickListener(v->{if(saveProfileChecked(pseudo,vehicle,color,server))joinConvoy(code.getText().toString());}); content.addView(join);
        Button settings=ghostButton("⚙   APPARENCE : "+prefs.get("theme","system").toUpperCase(Locale.ROOT)); settings.setOnClickListener(v->themeDialog()); content.addView(settings);
    }

    private void scanJoinQr(EditText pseudo,EditText vehicle,EditText color,EditText server){
        GmsBarcodeScannerOptions options=new GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build();
        GmsBarcodeScanner scanner=GmsBarcodeScanning.getClient(this,options);
        scanner.startScan().addOnSuccessListener(barcode->{
            String code=extractConvoyCode(barcode.getRawValue());
            if(code==null){toast("Ce QR n’est pas une invitation Mode Convoi");return;}
            if(!saveProfileChecked(pseudo,vehicle,color,server)) return;
            // Un QR Mode Convoi rejoint toujours le serveur public officiel.
            prefs.put("serverUrl",DEFAULT_SERVER);
            toast("QR Mode Convoi reconnu · "+code);
            joinConvoy(code);
        }).addOnCanceledListener(()->toast("Scan annulé")).addOnFailureListener(e->toast("Scanner QR indisponible"));
    }
    private String extractConvoyCode(String raw){
        if(raw==null)return null;
        String v=raw.trim();
        try{
            Uri u=Uri.parse(v);
            if("modeconvoi".equalsIgnoreCase(u.getScheme())&&"join".equalsIgnoreCase(u.getHost())){
                String code=u.getLastPathSegment();
                if(code==null)return null;
                code=code.trim().toUpperCase(Locale.ROOT);
                return code.matches("[A-Z0-9]{6}")?code:null;
            }
            if("drivebox".equalsIgnoreCase(u.getScheme())&&"convoy".equalsIgnoreCase(u.getHost())){
                String code=u.getLastPathSegment();
                if(code==null)return null;
                code=code.trim().toUpperCase(Locale.ROOT);
                return code.matches("[A-Z0-9]{6}")?code:null;
            }
        }catch(Exception ignored){}
        // Ne jamais transformer un texte arbitraire de six lettres (ex. CHANEL) en code de convoi.
        return null;
    }

    private void saveProfile(EditText pseudo, EditText vehicle, EditText color, EditText server) {
        prefs.put("profileName", pseudo.getText().toString().trim()); prefs.put("profileVehicle", vehicle.getText().toString().trim()); prefs.put("profileColor", color.getText().toString().trim()); prefs.put("serverUrl", server.getText().toString().trim());
    }

    private void createConvoy(String name) {
        runBusy("Création…",()->sessionManager.create(name), r->{ ensurePermissionsAndService(); render(); startPolling(); ui.postDelayed(this::showConvoyQr,250); });
    }
    private void joinConvoy(String raw) {
        try{sessionManager.normalizeJoinCode(raw);}catch(Exception e){toast("Code invalide");return;}
        runBusy("Connexion…",()->sessionManager.join(raw), r->{ ensurePermissionsAndService(); render(); startPolling(); });
    }

    private void renderConvoyHome() {
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
        GridLayout primary=new GridLayout(this);primary.setColumnCount(2);
        String[][] driving={{"🛑","Arrêt","stop"},{"⛽","Essence","fuel"},{"🚻","WC","wc"},{"⚠️","Problème","problem"}};
        for(String[] st:driving){
            View b=quickActionTile(st[0],st[1]);b.setOnClickListener(v->sendStatus(st[2]));
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(64);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));primary.addView(b,lp);
        }
        content.addView(primary,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout moreHeader=new LinearLayout(this);moreHeader.setGravity(Gravity.CENTER_VERTICAL);moreHeader.setPadding(dp(10),dp(4),dp(8),dp(4));moreHeader.setBackground(roundBg(control,border,13,1));
        LinearLayout.LayoutParams mhLp=new LinearLayout.LayoutParams(-1,dp(42));mhLp.setMargins(dp(3),dp(5),dp(3),dp(2));moreHeader.setLayoutParams(mhLp);
        TextView moreLabel=text("PLUS D’ACTIONS",11,true,muted);moreHeader.addView(moreLabel,new LinearLayout.LayoutParams(0,-1,1));
        TextView moreChevron=text("›",22,true,accent);moreChevron.setGravity(Gravity.CENTER);moreHeader.addView(moreChevron,new LinearLayout.LayoutParams(dp(38),-1));content.addView(moreHeader);
        moreHeader.setOnClickListener(v->showDrivingMoreActionsDialog());
    }
    private void showDrivingMoreActionsDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);
        final Dialog[] ref=new Dialog[1];
        String[][] extra={{"☕","Pause","pause"},{"🚗","Voiture","car_problem"},{"↗️","Je rejoins","joining"},{"👍","OK","ok"}};
        for(String[] st:extra){
            View b=quickActionTile(st[0],st[1]);
            b.setOnClickListener(v->{if(ref[0]!=null)ref[0].dismiss();sendStatus(st[2]);});
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(58);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));grid.addView(b,lp);
        }
        box.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        Button custom=outlinedButton("✎   AUTRE MESSAGE",Color.rgb(94,99,104));custom.setOnClickListener(v->{if(ref[0]!=null)ref[0].dismiss();customStatusDialog();});box.addView(custom);
        Button clear=ghostButton("ANNULER MON STATUT");clear.setOnClickListener(v->{if(ref[0]!=null)ref[0].dismiss();sendStatus("clear");});box.addView(clear);
        Button receive=ghostButton(prefs.getBool("talkieReceive",true)?"🔊  RÉCEPTION TALKIE : OUI":"🔇  RÉCEPTION TALKIE : NON");
        receive.setOnClickListener(v->{boolean on=!prefs.getBool("talkieReceive",true);prefs.putBool("talkieReceive",on);if(liveTalkie!=null)liveTalkie.setReceiveEnabled(on);receive.setText(on?"🔊  RÉCEPTION TALKIE : OUI":"🔇  RÉCEPTION TALKIE : NON");toast(on?"Réception live activée":"Réception live coupée");});box.addView(receive);
        ref[0]=styledDialog("Plus d’actions",box,"FERMER",null,null,null,false);
    }

    private String snapshotCountText(){if(snapshot==null)return "Connexion au convoi…";JSONArray ps=snapshot.optJSONArray("participants");int total=ps==null?0:ps.length();return total+" voiture"+(total>1?"s":"")+" connectée"+(total>1?"s":"");}

    private void refreshSnapshotArea() {
        if (snapshotArea == null || !"home".equals(currentPage)) return;
        if(convoyCountView!=null)convoyCountView.setText(snapshotCountText());
        snapshotArea.removeAllViews();
        if (snapshot == null) { cardTitle(snapshotArea,"Connexion au convoi…","Récupération des participants et positions."); return; }
        renderSnapshot(snapshotArea);
    }

    private void renderSnapshot(LinearLayout target) {
        JSONObject me=positionResolver.findMe(snapshot);
        ConvoyPositionResolver.Relative rel=positionResolver.resolveRelative(snapshot);
        target.addView(positionCard("DEVANT MOI",rel.ahead,Color.rgb(91,196,62),false));
        target.addView(positionCard("MOI",me,accent,true));
        target.addView(positionCard("DERRIÈRE MOI",rel.behind,Color.rgb(55,158,225),false));

        ConvoyPositionResolver.RallyInfo rallyInfo=positionResolver.rallyInfo(snapshot);
        if(rallyInfo!=null){
            JSONObject rally=rallyInfo.rally;
            LinearLayout rallyRow=new LinearLayout(this);rallyRow.setGravity(Gravity.CENTER_VERTICAL);rallyRow.setPadding(dp(9),dp(5),dp(7),dp(5));rallyRow.setBackground(roundBg(control,border,12,1));
            LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);rlp.setMargins(0,dp(2),0,dp(2));rallyRow.setLayoutParams(rlp);
            TextView pin=text("📍",19,false,accent);pin.setGravity(Gravity.CENTER);rallyRow.addView(pin,new LinearLayout.LayoutParams(dp(34),dp(38)));
            LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
            TextView rallyName=text(rally.optString("name","Point de regroupement"),13,true,fg);rallyName.setMaxLines(1);rallyName.setAutoSizeTextTypeUniformWithConfiguration(11,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);labels.addView(rallyName);
            TextView rallySub=text(rallyInfo.subtitle,10,false,muted);rallySub.setMaxLines(1);rallySub.setAutoSizeTextTypeUniformWithConfiguration(9,10,1,android.util.TypedValue.COMPLEX_UNIT_SP);labels.addView(rallySub);rallyRow.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            Button gps=smallButton("GPS",Color.TRANSPARENT,accent);gps.setBackground(roundBg(Color.TRANSPARENT,accent,10,1));gps.setOnClickListener(v->openGps(rally));rallyRow.addView(gps,new LinearLayout.LayoutParams(dp(58),dp(34)));target.addView(rallyRow);
        }
    }

    private View positionCard(String title,JSONObject p,int stripeColor,boolean own){
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


    private void renderMapPage() {
        currentPage = "map";
        refreshBottomNav();
        content.removeAllViews();
        if(root!=null) root.setPadding(dp(10),dp(4),dp(10),dp(4));
        if(mainScroll!=null){mainScroll.scrollTo(0,0);mainScroll.setVerticalScrollBarEnabled(false);mainScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);}

        LinearLayout toolbar=cardBox();
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10),dp(4),dp(8),dp(4));
        LinearLayout.LayoutParams toolbarLp=new LinearLayout.LayoutParams(-1,dp(50));
        toolbarLp.setMargins(0,dp(3),0,dp(5));
        toolbar.setLayoutParams(toolbarLp);

        TextView title=text("CARTE DU CONVOI",14,true,fg);
        toolbar.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));

        TextView gpsChip=text("● GPS",11,true,gpsStatusGood()?Color.rgb(90,200,120):danger);
        gpsChip.setGravity(Gravity.CENTER);
        gpsChip.setPadding(dp(9),0,dp(9),0);
        gpsChip.setBackground(roundBg(control,gpsStatusGood()?Color.rgb(76,130,82):danger,12,1));
        gpsChip.setOnClickListener(v->{
            if(!hasLocationPermission()) ensurePermissionsAndService();
            else{
                startShareService();
                toast("GPS relancé");
                ui.postDelayed(()->{gpsChip.setTextColor(gpsStatusGood()?Color.rgb(90,200,120):danger);pushMap();},1000);
            }
        });
        toolbar.addView(gpsChip,new LinearLayout.LayoutParams(dp(68),dp(34)));

        Button full=smallButton("⛶",Color.TRANSPARENT,accent);
        full.setTextSize(20);
        full.setBackground(roundBg(Color.TRANSPARENT,accent,10,1));
        full.setOnClickListener(v->showFullScreenMap());
        LinearLayout.LayoutParams fullLp=new LinearLayout.LayoutParams(dp(42),dp(34));
        fullLp.setMargins(dp(7),0,0,0);
        toolbar.addView(full,fullLp);
        content.addView(toolbar);

        FrameLayout mapStage=new FrameLayout(this);
        mapStage.setBackground(roundBg(card,border,16,1));
        mapStage.setClipToOutline(true);

        mapView=new WebView(this);
        mapController.attachPage(mapView,bg,()->toast("Impossible d’ouvrir la carte"));
        mapStage.addView(mapView,new FrameLayout.LayoutParams(-1,-1));

        if(snapshot!=null){
            ConvoyPositionResolver.RallyInfo rallyInfo=positionResolver.rallyInfo(snapshot);
            if(rallyInfo!=null){
                JSONObject rally=rallyInfo.rally;
                LinearLayout overlay=new LinearLayout(this);
                overlay.setOrientation(LinearLayout.HORIZONTAL);
                overlay.setGravity(Gravity.CENTER_VERTICAL);
                overlay.setPadding(dp(12),dp(7),dp(8),dp(7));
                overlay.setBackground(roundBg(darkTheme?Color.rgb(22,25,28):Color.WHITE,accent,14,1));

                LinearLayout labels=new LinearLayout(this);
                labels.setOrientation(LinearLayout.VERTICAL);
                labels.addView(text("📍  "+rally.optString("name","Point de regroupement"),13,true,fg));
                labels.addView(text(rallyInfo.subtitle,11,false,muted));
                overlay.addView(labels,new LinearLayout.LayoutParams(0,dp(48),1));

                Button open=smallButton("GPS ➤",accent,Color.rgb(17,18,19));
                open.setOnClickListener(v->openGps(rally));
                overlay.addView(open,new LinearLayout.LayoutParams(dp(76),dp(38)));

                FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(-1,dp(64),Gravity.BOTTOM);
                op.setMargins(dp(8),0,dp(8),dp(8));
                mapStage.addView(overlay,op);
            }
        }

        LinearLayout.LayoutParams mapLp=new LinearLayout.LayoutParams(-1,0,1);
        content.addView(mapStage,mapLp);
        ui.post(this::fitMapPageNoScroll);
    }

    private void fitMapPageNoScroll(){
        if(!"map".equals(currentPage)||mainScroll==null||root==null||content==null)return;
        int fixed=root.getPaddingTop()+root.getPaddingBottom();
        for(int i=0;i<root.getChildCount();i++){
            View child=root.getChildAt(i);
            if(child!=content && child.getVisibility()!=View.GONE) fixed+=child.getMeasuredHeight();
        }
        int available=Math.max(dp(320),mainScroll.getMeasuredHeight()-fixed);
        ViewGroup.LayoutParams lp=content.getLayoutParams();
        lp.height=available;
        content.setLayoutParams(lp);
        mainScroll.scrollTo(0,0);
    }

    private void restoreScrollableContent(){
        if(root!=null)root.setPadding(dp(16),dp(8),dp(16),dp(28));
        if(mainScroll!=null){mainScroll.setVerticalScrollBarEnabled(true);mainScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);mainScroll.scrollTo(0,0);}
        if(content!=null){ViewGroup.LayoutParams lp=content.getLayoutParams();lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;content.setLayoutParams(lp);}
    }
    private boolean gpsStatusGood(){ return hasLocationPermission() && prefs.getLong(LocationShareService.PREF_GPS_FIX_AT,0)>0; }
    private String gpsStatusText(){
        if(!hasLocationPermission()) return "● GPS : autorisation manquante";
        long at=prefs.getLong(LocationShareService.PREF_GPS_FIX_AT,0);
        if(at<=0) return "● GPS : en attente d’une position…";
        return "● GPS téléphone : reçu "+ageText(Math.max(0,System.currentTimeMillis()-at));
    }
    private String gpsServerStatusText(){
        String err=prefs.get(LocationShareService.PREF_GPS_ERROR,"");
        long sent=prefs.getLong(LocationShareService.PREF_GPS_SENT_AT,0);
        if(!err.isEmpty()) return "Serveur : "+err;
        if(sent>0) return "Serveur : position envoyée "+ageText(Math.max(0,System.currentTimeMillis()-sent));
        return "Serveur : aucune position envoyée pour l’instant";
    }
    private String gpsStatusCompactText(){
        if(!hasLocationPermission()) return "Localisation désactivée";
        long at=prefs.getLong(LocationShareService.PREF_GPS_FIX_AT,0);
        if(at<=0) return "GPS en attente…";
        return "GPS actif · "+ageText(Math.max(0,System.currentTimeMillis()-at));
    }
    private String gpsServerCompactText(){
        String err=prefs.get(LocationShareService.PREF_GPS_ERROR,""); if(!err.isEmpty())return err;
        long sent=prefs.getLong(LocationShareService.PREF_GPS_SENT_AT,0);
        return sent>0?"Synchronisé · "+ageText(Math.max(0,System.currentTimeMillis()-sent)):"En attente de synchronisation";
    }
    private void pushMap(){if(mapController!=null)mapController.pushPage();}

    private void renderParticipantsPage(){currentPage="participants";if(mapController!=null)mapController.detachPage();mapView=null;content.removeAllViews();LinearLayout top=pageHeader("‹","PARTICIPANTS");top.getChildAt(0).setOnClickListener(v->{render();startPolling();});content.addView(top);if(snapshot==null){cardTitle(content,"Aucun participant","Crée ou rejoins un convoi depuis Accueil pour voir les voitures ici.");return;}JSONArray ps=snapshot.optJSONArray("participants");long st=snapshot.optLong("serverTime",System.currentTimeMillis());sectionLabel(content,"PARTICIPANTS ("+(ps==null?0:ps.length())+")");LinearLayout list=cardBox();list.setPadding(dp(12),dp(4),dp(12),dp(4));for(int i=0;ps!=null&&i<ps.length();i++){JSONObject p=ps.optJSONObject(i);if(p==null)continue;long presenceAge=Math.max(0,st-p.optLong("lastSeen",0));boolean online=presenceAge<30000;JSONObject loc=p.optJSONObject("location");long posAge=loc==null?Long.MAX_VALUE:Math.max(0,st-loc.optLong("receivedAt",0));int state=online?(posAge>30000?accent:Color.rgb(91,196,62)):Color.rgb(112,116,120);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(8),0,dp(8));View icon=participantAvatar(p,40,state);row.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));LinearLayout who=new LinearLayout(this);who.setOrientation(LinearLayout.VERTICAL);who.setPadding(dp(10),0,0,0);who.addView(text(p.optString("name","Participant"),17,true,fg));who.addView(text(p.optString("vehicle","Véhicule"),12,false,muted));row.addView(who,new LinearLayout.LayoutParams(0,-2,1));String stateText=!online?"hors ligne":posAge>30000?"position ancienne":"en ligne";row.addView(text("●  "+stateText,11,false,state));list.addView(row);}content.addView(list);sectionLabel(content,"ÉVÉNEMENTS RÉCENTS");LinearLayout ev=cardBox();JSONArray events=snapshot.optJSONArray("events");int shown=0;if(events!=null)for(int i=events.length()-1;i>=0&&shown<4;i--){JSONObject e=events.optJSONObject(i);if(e==null)continue;String label=e.optString("label","");if(label.isEmpty())continue;ev.addView(text("•  "+label,13,false,fg),new LinearLayout.LayoutParams(-1,dp(42)));shown++;}if(shown==0)ev.addView(text("Aucun événement récent",13,false,muted));content.addView(ev);sectionLabel(content,"ADMINISTRATION");boolean admin=!prefs.get("adminKey","").isEmpty();if(admin){Button rename=adminButton("✎  Renommer le convoi",false);rename.setOnClickListener(v->renameConvoyDialog());content.addView(rename);Button rally=adminButton("⌖  Définir le regroupement",false);rally.setOnClickListener(v->rallyDialog());content.addView(rally);Button remove=adminButton("⊖  Retirer un participant",false);remove.setOnClickListener(v->manageParticipantsDialog());content.addView(remove);Button close=adminButton("▣  Fermer le convoi",true);close.setOnClickListener(v->confirmClose());content.addView(close);}else{Button leave=adminButton("↪  Quitter le convoi",true);leave.setOnClickListener(v->leaveConvoy());content.addView(leave);}}

    private void convoyOptions(){
        boolean admin=!prefs.get("adminKey","").isEmpty();
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        final Dialog[] ref=new Dialog[1];
        if(admin){
            box.addView(settingsRow("✎","Renommer le convoi","Modifier le nom visible par tous",v->{ref[0].dismiss();renameConvoyDialog();}));
            box.addView(settingsRow("📍","Point de regroupement","Définir le lieu de rendez-vous",v->{ref[0].dismiss();rallyDialog();}));
            box.addView(settingsRow("👥","Participants et rôles","Chef, voiture balai et exclusions",v->{ref[0].dismiss();manageParticipantsDialog();}));
            box.addView(settingsRow("🛑","Arrêt général","Envoyer une alerte immédiate",v->{ref[0].dismiss();confirmGeneralStop();}));
            box.addView(settingsRow("◐","Apparence","Changer le thème de l’application",v->{ref[0].dismiss();themeDialog();}));
        }else box.addView(settingsRow("◐","Apparence","Changer le thème de l’application",v->{ref[0].dismiss();themeDialog();}));
        ref[0]=styledDialog("Gérer le convoi",box,"FERMER",null,null,null,false);
        Button leave=destructiveButton(admin?"⏻  FERMER / QUITTER LE TRAJET":"⏻  QUITTER LE TRAJET");leave.setOnClickListener(v->{ref[0].dismiss();confirmLeaveOrClose();});box.addView(leave);
    }
    private void renameConvoyDialog(){
        EditText name=input("Nom du convoi",prefs.get("convoyName","Convoi"));
        new AlertDialog.Builder(this).setTitle("Renommer le convoi").setView(name).setNegativeButton("Annuler",null)
                .setPositiveButton("Enregistrer",(d,w)->renameConvoy(name.getText().toString())).show();
    }
    private void renameConvoy(String raw){
        String name=raw==null?"":raw.trim(); if(name.length()<2){toast("Nom trop court");return;}
        runBusy("Mise à jour…",()->sessionManager.rename(name),r->render());
    }
    private void manageParticipantsDialog(){
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
        runBusy("Mise à jour du rôle…",()->sessionManager.setParticipantRole(targetId,role),r->pollOnce());
    }
    private void confirmRemoveParticipant(JSONObject target){
        String name=target.optString("name","ce participant");
        new AlertDialog.Builder(this).setTitle("Retirer "+name+" ?").setMessage("Sa session sera invalidée et son partage de position s’arrêtera à la prochaine synchronisation.")
                .setNegativeButton("Annuler",null).setPositiveButton("Retirer",(d,w)->removeParticipant(target.optString("id"))).show();
    }
    private void removeParticipant(String targetId){
        runBusy("Suppression…",()->sessionManager.removeParticipant(targetId),r->pollOnce());
    }
    private void rallyDialog(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(20),0,dp(20),0);
        EditText n=input("Nom","Point de regroupement"); EditText desired=input("Heure souhaitée (ex. 14:30)",""); EditText lat=input("Latitude",""); EditText lon=input("Longitude","");
        l.addView(n);l.addView(desired);l.addView(lat);l.addView(lon);
        JSONObject loc=positionResolver.ownLocation(snapshot);if(loc!=null){lat.setText(String.valueOf(loc.optDouble("lat")));lon.setText(String.valueOf(loc.optDouble("lon")));}
        new AlertDialog.Builder(this).setTitle("Point de regroupement").setView(l).setPositiveButton("Partager",(d,w)->setRally(n.getText().toString(),desired.getText().toString(),lat.getText().toString(),lon.getText().toString())).setNegativeButton("Annuler",null).show();
    }
    private void setRally(String name,String desiredTime,String latS,String lonS){try{double lat=Double.parseDouble(latS.replace(',','.')),lon=Double.parseDouble(lonS.replace(',','.'));runBusy("Partage…",()->sessionManager.setRally(name,desiredTime,lat,lon),r->pollOnce());}catch(Exception e){toast("Coordonnées invalides");}}
    private void openGps(JSONObject r){try{double lat=r.optDouble("lat"),lon=r.optDouble("lon");String q=Uri.encode(r.optString("name","Point de regroupement"));Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("geo:"+lat+","+lon+"?q="+lat+","+lon+"("+q+")"));startActivity(Intent.createChooser(i,"Ouvrir dans le GPS"));}catch(Exception e){toast("Aucune application GPS disponible");}}
    private void showConvoyQr(){
        String code=prefs.get("code","").trim().toUpperCase(Locale.ROOT);
        if(!prefs.hasActiveConvoy()||!code.matches("[A-Z0-9]{6}")){toast("Aucun convoi actif à partager");return;}
        try{
            LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);box.setPadding(dp(10),dp(4),dp(10),dp(2));
            TextView intro=text("Scanne ce QR depuis Mode Convoi → Scanner un QR",12,false,muted);intro.setGravity(Gravity.CENTER);intro.setPadding(dp(4),0,dp(4),dp(10));box.addView(intro);
            FrameLayout qrFrame=new FrameLayout(this);qrFrame.setBackground(roundBg(Color.WHITE,Color.rgb(210,210,210),18,1));qrFrame.setPadding(dp(10),dp(10),dp(10),dp(10));
            QrCodeView qr=new QrCodeView(this);qr.setPayload("modeconvoi://join/"+code);qrFrame.addView(qr,new FrameLayout.LayoutParams(-1,-1));box.addView(qrFrame,new LinearLayout.LayoutParams(dp(286),dp(286)));
            TextView c=text(code,26,true,fg);c.setGravity(Gravity.CENTER);c.setLetterSpacing(.08f);box.addView(c,new LinearLayout.LayoutParams(-1,dp(54)));
            TextView serverInfo=text("Invitation Mode Convoi · serveur officiel",11,false,muted);serverInfo.setGravity(Gravity.CENTER);box.addView(serverInfo);
            Button copy=outlinedButton("#   COPIER LE CODE",accent);copy.setOnClickListener(v->copyConvoyCode());box.addView(copy);
            Button share=ghostButton("↗   PARTAGER L’INVITATION");share.setOnClickListener(v->shareConvoy());box.addView(share);
            styledDialog("QR d’invitation",box,"FERMER",null,null,null,false);
        }catch(Exception e){toast("Impossible d’afficher le QR · "+(e.getMessage()==null?"erreur inconnue":e.getMessage()));}
    }
    private void shareConvoy(){String code=prefs.get("code","");String msg="Rejoins mon convoi « "+prefs.get("convoyName","Mode Convoi")+" »\nOuvre Mode Convoi puis scanne mon QR, ou saisis le code : "+code+"\nLien : modeconvoi://join/"+code;Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,msg);startActivity(Intent.createChooser(i,"Partager le convoi"));}

    private void copyConvoyCode(){String code=prefs.get("code","");if(code.isEmpty()){toast("Aucun convoi actif");return;}ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Code Mode Convoi",code));toast("Code "+code+" copié");}

    private void sendStatus(String status){runBusy("Envoi…",()->sessionManager.sendStatus(status),r->pollOnce());}
    private void customStatusDialog(){
        EditText message=input("Message court",""); message.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        new AlertDialog.Builder(this).setTitle("Autre message").setMessage("80 caractères maximum").setView(message)
                .setNegativeButton("Annuler",null).setPositiveButton("Envoyer",(d,w)->sendCustomStatus(message.getText().toString())).show();
    }
    private void sendCustomStatus(String raw){
        String message=raw==null?"":raw.trim(); if(message.isEmpty()){toast("Message vide");return;}
        runBusy("Envoi…",()->sessionManager.sendCustomStatus(message),r->pollOnce());
    }

    private void confirmGeneralStop(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.addView(text("Tous les participants recevront immédiatement une alerte d’arrêt général.",14,false,fg));
        styledDialog("ARRÊT GÉNÉRAL ?",body,"ANNULER",null,"ENVOYER",d->{sendGeneralStop();return true;},true);
    }
    private void sendGeneralStop(){
        runBusy("Envoi de l’arrêt général…",()->sessionManager.sendGeneralStop(),r->pollOnce());
    }

    private void leaveConvoy(){leaveConvoyStyled();}
    private void confirmClose(){LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.addView(text("Tous les participants seront déconnectés et le partage GPS sera arrêté.",14,false,fg));styledDialog("Fermer le convoi ?",body,"ANNULER",null,"FERMER",d->{runBusy("Fermeture…",()->sessionManager.close(),r->endSession());return true;},true);}
    private void endSession(){stopPolling();stopService(new Intent(this,LocationShareService.class));prefs.clearSession();if(liveTalkie!=null)liveTalkie.ensureStarted();snapshot=null;render();}

    private void sessionStatusDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.addView(infoRow("●","Connecté au convoi",prefs.get("convoyName","Convoi"),Color.rgb(90,200,120)));
        box.addView(infoRow("#","Code du convoi",prefs.get("code",""),accent));
        TextView note=text("Le badge CONNECTÉ donne aussi accès à l’invitation et à la sortie du trajet.",12,false,muted);note.setPadding(dp(2),dp(10),dp(2),dp(4));box.addView(note);
        final Dialog[] ref=new Dialog[1];
        Button qr=outlinedButton("▦   AFFICHER LE QR D’INVITATION",accent);qr.setOnClickListener(v->{ref[0].dismiss();showConvoyQr();});box.addView(qr);
        Button share=ghostButton("↗   PARTAGER LE CONVOI");share.setOnClickListener(v->shareConvoy());box.addView(share);
        Button leave=destructiveButton("⏻  QUITTER LE CONVOI");leave.setOnClickListener(v->{ref[0].dismiss();confirmLeaveOrClose();});box.addView(leave);
        ref[0]=styledDialog("Connexion au trajet",box,"FERMER",null,null,null,false);
    }
    private void confirmLeaveOrClose(){
        boolean admin=!prefs.get("adminKey","").isEmpty();
        int count=0; JSONArray ps=snapshot==null?null:snapshot.optJSONArray("participants"); if(ps!=null)count=ps.length();
        if(admin){
            String msg=count>1?"Tu es l’administrateur du convoi. Pour te déconnecter proprement, il faut fermer le convoi pour les "+count+" participants.":"Tu es seul dans ce convoi. Le fermer arrêtera le partage GPS et te déconnectera immédiatement.";
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.addView(text(msg,14,false,fg));
            styledDialog("Quitter le trajet",body,"ANNULER",null,"FERMER LE CONVOI",dlg->{dlg.dismiss();confirmClose();return false;},true);
        }else leaveConvoyStyled();
    }
    private void leaveConvoyStyled(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.addView(text("Le partage GPS sera arrêté immédiatement et tu reviendras à l’écran de connexion.",14,false,fg));
        styledDialog("Quitter le convoi ?",body,"ANNULER",null,"QUITTER",dlg->{dlg.dismiss();leaveConvoyNow();return false;},true);
    }
    private void leaveConvoyNow(){
        io.execute(()->{sessionManager.leaveBestEffort();ui.post(this::endSession);});
    }

    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void ensurePermissionsAndService(){
        if(!hasLocationPermission()){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION);
            return;
        }
        startShareService();
        requestNotificationPermissionIfNeeded();
    }
    private void requestNotificationPermissionIfNeeded(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIF);
    }
    private void startShareServiceIfPermitted(){ if(hasLocationPermission()) startShareService(); }
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] grants){
        super.onRequestPermissionsResult(req,perms,grants);
        if(req==REQ_LOCATION){
            if(hasLocationPermission()){ startShareService(); requestNotificationPermissionIfNeeded(); if("map".equals(currentPage)) pushMap(); }
            else toast("La localisation est nécessaire pour apparaître dans le convoi");
        } else if(req==REQ_NOTIF){
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                toast("Notifications refusées — les alertes écran verrouillé ne pourront pas s’afficher");
            else toast("Notifications Mode Convoi activées");
        } else if(req==REQ_AUDIO){
            talkiePermissionRequestPending=false;
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){
                if(liveTalkie!=null)liveTalkie.ensureStarted();
                setTalkieIdleVisual("✓ Micro autorisé · talkie live prêt",Color.rgb(90,200,120));
                toast("Micro autorisé — maintiens le bouton pour parler en direct");
            } else {
                setTalkieIdleVisual("✕ Autorisation micro refusée",danger);
                toast("Autorise le micro dans les réglages Android pour utiliser le talkie-walkie");
            }
        }
    }
    private void startShareService(){
        if(!prefs.hasActiveConvoy() || !hasLocationPermission()) return;
        Intent s=new Intent(this,LocationShareService.class);
        try{ if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s); }
        catch(Exception e){ toast("Impossible de démarrer le partage GPS"); }
    }

    private void bindTalkieTouch(TextView ptt){
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
    private void setTalkiePressedVisual(String message,int color,boolean speaking){
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

    private void startPolling(){if(pollingController!=null)pollingController.start();}
    private void stopPolling(){if(pollingController!=null)pollingController.stop();}
    private void pollOnce(){if(pollingController!=null)pollingController.refreshNow();}

    private View participantAvatar(JSONObject p,int size,int fallbackColor){
        String image=p==null?prefs.get("profileVehicleImage",""):p.optString("vehicleImage","");
        if(image!=null&&!image.isEmpty()){
            try{
                Bitmap b=VehicleImageCache.decode(image);
                if(b!=null){
                    boolean transparentVehicle=image.startsWith("iVBOR");
                    ImageView iv=new ImageView(this);
                    iv.setScaleType(transparentVehicle?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_CROP);
                    iv.setPadding(transparentVehicle?dp(3):0,transparentVehicle?dp(3):0,transparentVehicle?dp(3):0,transparentVehicle?dp(3):0);
                    iv.setImageBitmap(b);
                    iv.setBackground(roundBg(control,participantMarkerColor(p,fallbackColor),size/2,1));
                    iv.setClipToOutline(!transparentVehicle);
                    return iv;
                }
            }catch(Exception ignored){}
        }
        String icon=p==null?prefs.get("profileVehicleIcon","🚗"):p.optString("vehicleIcon","🚗");if(icon.isEmpty()||VolkswagenIconPack.isVolkswagen(icon))icon="🚗";
        TextView v=text(icon,size>=46?24:19,false,participantMarkerColor(p,fallbackColor));v.setGravity(Gravity.CENTER);v.setBackground(roundBg(control,participantMarkerColor(p,fallbackColor),size/2,1));return v;
    }
    private int participantMarkerColor(JSONObject p,int fallback){String c=p==null?prefs.get("profileVehicleMarkerColor",""):p.optString("vehicleMarkerColor","");try{if(c!=null&&!c.isEmpty())return Color.parseColor(c);}catch(Exception ignored){}return fallback;}
    private void profileDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText pseudo=profileInput(box,"👤","Pseudo",prefs.get("profileName",""));
        EditText vehicle=profileInput(box,"🚗","Véhicule",prefs.get("profileVehicle",""));
        EditText color=profileInput(box,"●","Couleur réelle du véhicule",prefs.get("profileColor",""));
        TextView help=text("Ces informations sont visibles par les autres participants du convoi.",12,false,muted);help.setPadding(dp(4),dp(8),dp(4),0);box.addView(help);
        final Dialog[] ref=new Dialog[1];
        ref[0]=styledDialog("Mon profil",box,"ANNULER",null,"ENREGISTRER",dlg->{
            String name=pseudo.getText().toString().trim();
            if(name.isEmpty()){pseudo.setError("Choisis ton pseudo");pseudo.requestFocus();return false;}
            prefs.put("profileName",name);prefs.put("profileVehicle",vehicle.getText().toString().trim());prefs.put("profileColor",color.getText().toString().trim());
            syncProfileToServer();
            if("participants".equals(currentPage))renderParticipantsPage();else if("map".equals(currentPage))pushMap();else render();
            return true;
        },false);
    }

    private void vehicleAppearanceDialog(){
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

        LinearLayout vwHeader=new LinearLayout(this);vwHeader.setGravity(Gravity.CENTER_VERTICAL);vwHeader.setPadding(0,dp(10),0,dp(3));
        TextView vwTitle=text("ICÔNES VOLKSWAGEN",11,true,accent);vwHeader.addView(vwTitle,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView vwChevron=text("⌄",22,true,accent);vwChevron.setGravity(Gravity.CENTER);vwHeader.addView(vwChevron,new LinearLayout.LayoutParams(dp(44),dp(40)));box.addView(vwHeader);
        GridLayout vwFirst=new GridLayout(this);vwFirst.setColumnCount(5);
        GridLayout vwMore=new GridLayout(this);vwMore.setColumnCount(5);vwMore.setVisibility(View.GONE);
        VolkswagenIconPack.Item[] vwItems=VolkswagenIconPack.items();
        String selectedVehicleIcon=prefs.get("profileVehicleIcon","🚗");
        for(int i=0;i<vwItems.length;i++){
            final VolkswagenIconPack.Item item=vwItems[i];
            FrameLayout slot=new FrameLayout(this);slot.setContentDescription(item.label);
            boolean selected=item.id.equals(selectedVehicleIcon);
            slot.setBackground(roundBg(control,selected?accent:border,12,selected?2:1));
            ImageView carIcon=new ImageView(this);carIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);carIcon.setPadding(dp(4),dp(4),dp(4),dp(4));carIcon.setImageBitmap(item.bitmap());
            slot.addView(carIcon,new FrameLayout.LayoutParams(-1,-1));
            slot.setOnClickListener(v->{prefs.put("profileVehicleIcon",item.id);prefs.put("profileVehicleImage",item.base64());refreshVehiclePreview();});
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(58);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));
            (i<5?vwFirst:vwMore).addView(slot,lp);
        }
        box.addView(vwFirst,new LinearLayout.LayoutParams(-1,-2));box.addView(vwMore,new LinearLayout.LayoutParams(-1,-2));
        TextView vwHint=text("10 modèles Volkswagen · appuie sur une voiture pour l'utiliser sur la carte.",11,false,muted);vwHint.setPadding(dp(4),dp(3),dp(4),0);box.addView(vwHint);
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
    private void refreshVehiclePreview(){if(vehiclePreview==null)return;vehiclePreview.removeAllViews();View av=participantAvatar(null,72,accent);FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(72),dp(72),Gravity.CENTER);vehiclePreview.addView(av,ap);}
    private void pickVehicleImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQ_VEHICLE_IMAGE);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(requestCode!=REQ_VEHICLE_IMAGE||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        try(InputStream in=getContentResolver().openInputStream(data.getData())){Bitmap src=BitmapFactory.decodeStream(in);if(src==null){toast("Image illisible");return;}int w=src.getWidth(),h=src.getHeight(),side=Math.min(w,h);Bitmap crop=Bitmap.createBitmap(src,(w-side)/2,(h-side)/2,side,side);Bitmap small=Bitmap.createScaledBitmap(crop,96,96,true);ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,72,out);String b64=Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);if(b64.length()>24000){toast("Image trop lourde");return;}prefs.put("profileVehicleImage",b64);refreshVehiclePreview();toast("Image du véhicule enregistrée");}catch(Exception e){toast("Impossible de lire cette image");}
    }
    private void syncProfileToServer(){if(!prefs.hasActiveConvoy())return;io.execute(()->{try{sessionManager.syncProfile();ui.post(()->{toast("Profil mis à jour");pollOnce();});}catch(Exception e){ui.post(()->toast("Profil local enregistré · serveur à mettre à jour"));}});}
    private void showFullScreenMap(){
        if(fullScreenMapDialog!=null&&fullScreenMapDialog.isShowing())return;
        final Dialog d=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);fullScreenMapDialog=d;
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.rgb(10,12,14));
        WebView w=new WebView(this);mapController.attachFullScreen(w,bg,()->toast("Impossible d’ouvrir la carte"));frame.addView(w,new FrameLayout.LayoutParams(-1,-1));
        TextView close=text("✕",27,true,Color.WHITE);close.setGravity(Gravity.CENTER);close.setBackground(roundBg(Color.rgb(20,23,26),Color.rgb(90,94,98),22,1));close.setOnClickListener(v->d.dismiss());FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(46),dp(46),Gravity.LEFT|Gravity.TOP);cp.setMargins(dp(12),dp(14),0,0);frame.addView(close,cp);
        TextView title=text("CARTE DU CONVOI",12,true,accent);title.setGravity(Gravity.CENTER);title.setBackground(roundBg(Color.rgb(20,23,26),Color.rgb(90,94,98),18,1));FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(dp(160),dp(40),Gravity.TOP|Gravity.CENTER_HORIZONTAL);tp.setMargins(0,dp(17),0,0);frame.addView(title,tp);
        d.setContentView(frame);d.setOnDismissListener(x->{if(mapController!=null)mapController.detachFullScreen();fullScreenMapDialog=null;});d.show();if(d.getWindow()!=null)d.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void renderMorePage(){
        currentPage="more";
        if(mapController!=null)mapController.detachPage();mapView=null;
        content.removeAllViews();
        sectionLabel(content,"RÉGLAGES");
        LinearLayout settings=cardBox();settings.setPadding(dp(8),dp(6),dp(8),dp(6));
        settings.addView(settingsRow("◐","Apparence","Thème sombre, clair ou automatique",v->themeDialog()));
        settings.addView(settingsRow("👤","Mon profil","Pseudo, véhicule et couleur",v->profileDialog()));
        settings.addView(settingsRow("🚗","Mon véhicule","Icône, couleur du repère et photo",v->vehicleAppearanceDialog()));
        settings.addView(settingsRow("⚙","Paramètres avancés","Connexion et diagnostic du serveur",v->advancedSettingsDialog()));
        content.addView(settings);

        sectionLabel(content,"CONNEXION");
        LinearLayout diagnostic=cardBox();
        TextView diag=text("● Vérification du serveur…",13,true,muted); diagnostic.addView(diag);
        long lastSyncAt=pollingController==null?0L:pollingController.lastSuccessfulSyncAt();
        TextView sync=text(lastSyncAt>0?"Dernière synchronisation : "+ageText(Math.max(0,System.currentTimeMillis()-lastSyncAt)):"Aucune synchronisation récente",12,false,muted); sync.setPadding(0,dp(4),0,dp(6)); diagnostic.addView(sync);
        Button test=outlinedButton("↻   TESTER LA CONNEXION",accent); test.setOnClickListener(v->testServerConnectionDetailed(diag,sync)); diagnostic.addView(test);
        content.addView(diagnostic);
        testServerConnectionDetailed(diag,sync);

        if(prefs.hasActiveConvoy()){
            sectionLabel(content,"CONVOI EN COURS");
            LinearLayout convoy=cardBox();
            convoy.addView(text(prefs.get("convoyName","Convoi"),19,true,fg));
            LinearLayout codeLine=new LinearLayout(this); codeLine.setGravity(Gravity.CENTER_VERTICAL);
            codeLine.addView(text("Code : "+prefs.get("code",""),15,true,fg),new LinearLayout.LayoutParams(0,dp(44),1));
            Button copy=smallButton("COPIER",card,accent); copy.setOnClickListener(v->copyConvoyCode()); codeLine.addView(copy,new LinearLayout.LayoutParams(dp(88),dp(40)));
            convoy.addView(codeLine);
            Button qr=outlinedButton("▦   AFFICHER LE QR D’INVITATION",accent); qr.setOnClickListener(v->showConvoyQr()); convoy.addView(qr);
            Button manage=outlinedButton("⚙   GÉRER LE CONVOI",fg); manage.setOnClickListener(v->convoyOptions()); convoy.addView(manage);
            Button leave=destructiveButton("⏻   QUITTER LE TRAJET");leave.setOnClickListener(v->confirmLeaveOrClose());convoy.addView(leave);
            TextView leaveHint=text("Arrête le partage GPS et te déconnecte du trajet en cours.",11,false,muted);leaveHint.setPadding(dp(4),0,dp(4),dp(2));convoy.addView(leaveHint);
            content.addView(convoy);
        }else{
            cardTitle(content,"Aucun convoi actif","Tu peux quand même ouvrir Carte, Participants et les réglages. Pour partager des positions, crée ou rejoins un convoi depuis Accueil.");
        }

        sectionLabel(content,"À PROPOS");
        cardTitle(content,"Mode Convoi 0.3.39","Le code à 6 caractères identifie un convoi. Le QR contient exactement ce code et permet aux autres téléphones de le rejoindre sans le saisir.");
    }

    private void advancedSettingsDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.addView(infoRow("🔒","Connexion Mode Convoi","Serveur internet sécurisé",Color.rgb(90,200,120)));
        TextView help=text("Aucun réglage réseau n’est nécessaire. Utilise Réinitialiser seulement si la connexion au serveur a été modifiée ou cassée.",13,false,muted);help.setPadding(dp(4),dp(10),dp(4),dp(6));box.addView(help);
        styledDialog("Paramètres avancés",box,"FERMER",null,"RÉINITIALISER",d->{prefs.put("serverUrl",DEFAULT_SERVER);toast("Connexion Mode Convoi réinitialisée");return true;},false);
    }

    private void themeDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        String current=prefs.get("theme","system");
        View automatic=themeChoice("◐","Automatique","Suit le thème du téléphone","system",current);
        View dark=themeChoice("☾","Sombre","Fond noir et cartes foncées","dark",current);
        View light=themeChoice("☀","Clair","Fond clair et contraste renforcé","light",current);
        box.addView(automatic);box.addView(dark);box.addView(light);
        Dialog d=styledDialog("Apparence",box,"FERMER",null,null,null,false);
        automatic.setOnClickListener(v->{prefs.put("theme","system");d.dismiss();render();});
        dark.setOnClickListener(v->{prefs.put("theme","dark");d.dismiss();render();});
        light.setOnClickListener(v->{prefs.put("theme","light");d.dismiss();render();});
    }

    interface Throwing<T>{T run() throws Exception;} interface Done<T>{void accept(T t);}
    private <T> void runBusy(String label,Throwing<T> work,Done<T> done){
        if(busyOperation){toast("Une opération est déjà en cours");return;}
        busyOperation=true;toast(label);
        io.execute(()->{try{T r=work.run();ui.post(()->{busyOperation=false;done.accept(r);});}catch(Exception e){ui.post(()->{busyOperation=false;toast(humanError(e));});}});
    }

    private void testServerConnectionDetailed(TextView target,TextView sync){
        target.setText("● Test en cours…"); target.setTextColor(muted);
        io.execute(()->{
            long started=System.currentTimeMillis();
            try{
                JSONObject h=ConvoyApi.get(prefs.get("serverUrl",DEFAULT_SERVER),"/health");
                long latency=Math.max(1,System.currentTimeMillis()-started);
                boolean ok=h.optBoolean("ok",false);
                ui.post(()->{
                    target.setText(ok?"● Serveur Mode Convoi disponible":"● Serveur Mode Convoi indisponible");
                    target.setTextColor(ok?Color.rgb(90,200,120):danger);
                    sync.setText(ok?"Réponse en "+latency+" ms":"Le serveur a répondu mais n’est pas prêt");
                });
            }catch(Exception e){
                ui.post(()->{target.setText("● Connexion impossible");target.setTextColor(danger);sync.setText(humanError(e));});
            }
        });
    }

    private String humanError(Exception e){
        if(e instanceof ConvoyApi.ApiException){
            int c=((ConvoyApi.ApiException)e).statusCode;
            if(c==404)return "Serveur Mode Convoi introuvable ou plugin non activé";
            if(c==401)return "Session du convoi expirée";
            if(c==403)return "Action réservée à l’administrateur";
            if(c>=500)return "Serveur Mode Convoi temporairement indisponible";
        }
        String m=e.getMessage()==null?"":e.getMessage();
        String l=m.toLowerCase(Locale.ROOT);
        if(l.contains("unable to resolve host")||l.contains("unknownhost"))return "Pas de connexion internet";
        if(l.contains("timed out")||l.contains("timeout"))return "Le serveur Mode Convoi ne répond pas";
        if(l.contains("ssl")||l.contains("certificate"))return "Connexion sécurisée au serveur impossible";
        if(l.contains("mode convoi"))return m;
        return m.isEmpty()?"Connexion Mode Convoi impossible":m;
    }

    private void checkServerStatus(TextView title,TextView sub){
        io.execute(()->{
            try{
                JSONObject h=ConvoyApi.get(prefs.get("serverUrl",DEFAULT_SERVER),"/health");
                boolean ok=h.optBoolean("ok",false);
                ui.post(()->{title.setText(ok?"●  Connexion Mode Convoi":"●  Serveur indisponible");title.setTextColor(ok?Color.rgb(91,196,62):danger);sub.setText(ok?"Serveur en ligne — prêt à créer ou rejoindre un convoi":"Le serveur ne répond pas");});
            }catch(Exception e){
                ui.post(()->{title.setText("●  Serveur indisponible");title.setTextColor(danger);sub.setText(humanError(e));});
            }
        });
    }

    private GradientDrawable roundBg(int fill,int stroke,int radius,int strokeWidth){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(strokeWidth>0)g.setStroke(dp(strokeWidth),stroke);return g;}
    private LinearLayout cardBox(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(16),dp(14),dp(16),dp(14));b.setBackground(roundBg(card,border,16,1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(lp);return b;}
    private void cardTitle(LinearLayout parent,String title,String sub){LinearLayout b=cardBox();b.addView(text(title,18,true,fg));TextView sv=text(sub,13,false,muted);sv.setPadding(0,dp(5),0,0);b.addView(sv);parent.addView(b);}
    private void sectionLabel(LinearLayout parent,String label){TextView v=text(label,12,true,accent);v.setLetterSpacing(.05f);v.setPadding(dp(2),dp(17),0,dp(6));parent.addView(v);}
    private void compactSectionLabel(LinearLayout parent,String label){TextView v=text(label,10,true,accent);v.setLetterSpacing(.05f);v.setPadding(dp(2),dp(7),0,dp(3));parent.addView(v);}
    private LinearLayout pageHeader(String left,String title){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(0,dp(2),0,dp(3));TextView b=text(left,28,false,fg);b.setGravity(Gravity.CENTER);h.addView(b,new LinearLayout.LayoutParams(dp(46),dp(54)));TextView t=text(title,18,true,fg);t.setGravity(Gravity.CENTER);h.addView(t,new LinearLayout.LayoutParams(0,dp(54),1));TextView more=text("⋮",24,false,muted);more.setGravity(Gravity.CENTER);h.addView(more,new LinearLayout.LayoutParams(dp(46),dp(54)));return h;}
    private TextView text(String value,int sp,boolean bold,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(sp);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(false);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private EditText profileInput(LinearLayout parent,String icon,String label,String value){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(4),dp(10),dp(4));row.setBackground(roundBg(control,border,13,1));LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,dp(64));rlp.setMargins(0,dp(4),0,dp(4));row.setLayoutParams(rlp);TextView ic=text(icon,20,false,fg);ic.setGravity(Gravity.CENTER);row.addView(ic,new LinearLayout.LayoutParams(dp(42),dp(56)));LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setGravity(Gravity.CENTER_VERTICAL);labels.addView(text(label,11,true,muted));EditText e=new EditText(this);e.setText(value);e.setTextColor(fg);e.setHintTextColor(muted);e.setSingleLine(true);e.setTextSize(15);e.setPadding(0,0,0,0);e.setBackgroundColor(Color.TRANSPARENT);labels.addView(e,new LinearLayout.LayoutParams(-1,dp(31)));row.addView(labels,new LinearLayout.LayoutParams(0,dp(58),1));parent.addView(row);return e;}
    private EditText input(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted);e.setText(value);e.setTextColor(fg);e.setSingleLine(true);e.setTextSize(15);e.setPadding(dp(15),0,dp(15),0);e.setBackground(roundBg(control,border,14,1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));e.setLayoutParams(lp);return e;}
    private Button button(String label,int bgColor,int textColor){Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(textColor);b.setAllCaps(false);b.setLetterSpacing(.02f);b.setGravity(Gravity.CENTER);b.setMinHeight(0);b.setMinWidth(0);b.setPadding(dp(12),0,dp(12),0);b.setStateListAnimator(null);b.setBackgroundTintList(null);b.setBackground(roundBg(bgColor,bgColor,14,0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(lp);return b;}
    private Button outlinedButton(String label,int color){int c=(color==Color.rgb(94,99,104))?fg:color;Button b=button(label,control,c);b.setBackground(roundBg(control,c==fg?border:c,14,1));return b;}
    private Button ghostButton(String label){Button b=button(label,control,fg);b.setTextSize(12);b.setBackground(roundBg(control,border,14,1));return b;}
    private Button smallButton(String label,int bgColor,int textColor){Button b=button(label,bgColor,textColor);b.setTextSize(11);b.setMinHeight(0);return b;}
    private Button adminButton(String label,boolean destructive){Button b=button(label,control,destructive?danger:fg);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setTextSize(14);b.setBackground(roundBg(control,destructive?danger:border,12,1));return b;}
    private View quickActionTile(String icon,String label){
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
    private Button destructiveButton(String label){Button b=button(label,darkTheme?Color.rgb(58,24,24):Color.rgb(255,239,239),danger);b.setBackground(roundBg(darkTheme?Color.rgb(58,24,24):Color.rgb(255,239,239),danger,14,1));return b;}
    private View settingsRow(String icon,String title,String subtitle,View.OnClickListener click){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(6),dp(8),dp(6));
        TextView ic=text(icon,23,false,accent);ic.setGravity(Gravity.CENTER);ic.setBackground(roundBg(control,border,13,1));row.addView(ic,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(12),0,dp(4),0);labels.addView(text(title,15,true,fg));labels.addView(text(subtitle,11,false,muted));row.addView(labels,new LinearLayout.LayoutParams(0,dp(50),1));
        TextView arrow=text("›",25,false,muted);arrow.setGravity(Gravity.CENTER);row.addView(arrow,new LinearLayout.LayoutParams(dp(30),dp(50)));row.setOnClickListener(click);return row;
    }
    private View infoRow(String icon,String title,String subtitle,int iconColor){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(7),dp(8),dp(7));row.setBackground(roundBg(control,border,13,1));
        TextView ic=text(icon,20,true,iconColor);ic.setGravity(Gravity.CENTER);row.addView(ic,new LinearLayout.LayoutParams(dp(42),dp(48)));
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(8),0,0,0);labels.addView(text(title,14,true,fg));labels.addView(text(subtitle,11,false,muted));row.addView(labels,new LinearLayout.LayoutParams(0,dp(48),1));return row;
    }
    private View themeChoice(String icon,String title,String subtitle,String value,String current){
        LinearLayout row=(LinearLayout)infoRow(icon,title,subtitle,value.equals(current)?accent:fg);row.setPadding(dp(8),dp(8),dp(8),dp(8));
        if(value.equals(current)){TextView check=text("✓",18,true,accent);check.setGravity(Gravity.CENTER);row.addView(check,new LinearLayout.LayoutParams(dp(34),dp(48)));}
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(4),0,dp(4));row.setLayoutParams(lp);return row;
    }
    private interface DialogAction{boolean run(Dialog d);}
    private Dialog styledDialog(String title,View body,String negative,DialogAction negativeAction,String positive,DialogAction positiveAction,boolean dangerPositive){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(18),dp(18),dp(14));panel.setBackground(roundBg(card,border,22,1));
        TextView ttl=text(title,21,true,fg);ttl.setPadding(dp(2),0,dp(2),dp(14));panel.addView(ttl);
        panel.addView(body,new LinearLayout.LayoutParams(-1,-2));
        if((negative!=null&&!negative.isEmpty())||(positive!=null&&!positive.isEmpty())){
            LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);actions.setPadding(0,dp(14),0,0);
            if(negative!=null&&!negative.isEmpty()){Button n=smallButton(negative,control,fg);n.setBackground(roundBg(control,border,12,1));n.setOnClickListener(v->{if(negativeAction==null||negativeAction.run(d))d.dismiss();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1);lp.setMargins(0,0,dp(6),0);actions.addView(n,lp);}
            if(positive!=null&&!positive.isEmpty()){Button p=smallButton(positive,dangerPositive?(darkTheme?Color.rgb(58,24,24):Color.rgb(255,239,239)):accent,dangerPositive?danger:Color.rgb(20,22,24));p.setBackground(roundBg(dangerPositive?(darkTheme?Color.rgb(58,24,24):Color.rgb(255,239,239)):accent,dangerPositive?danger:accent,12,dangerPositive?1:0));p.setOnClickListener(v->{if(positiveAction==null||positiveAction.run(d))d.dismiss();});actions.addView(p,new LinearLayout.LayoutParams(0,dp(46),1));}
            panel.addView(actions);
        }
        d.setContentView(panel);Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams a=w.getAttributes();a.dimAmount=.65f;w.setAttributes(a);}d.show();
        w=d.getWindow();if(w!=null){DisplayMetrics dm=getResources().getDisplayMetrics();w.setLayout(Math.min(dm.widthPixels-dp(28),dp(520)),WindowManager.LayoutParams.WRAP_CONTENT);w.setGravity(Gravity.CENTER);}return d;
    }
    private FrameLayout buildBottomNav(){
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

    private void navigateBottom(String page){
        if(!"map".equals(page)) restoreScrollableContent();
        if("home".equals(page)){
            render();
            if(prefs.hasActiveConvoy()) startPolling();
            return;
        }
        if("more".equals(page)){
            renderMorePage();
            refreshBottomNav();
            return;
        }
        if("map".equals(page)) renderMapPage();
        else if("participants".equals(page)) renderParticipantsPage();
        refreshBottomNav();
    }

    private void spacer(int h){Space sv=new Space(this);content.addView(sv,new LinearLayout.LayoutParams(1,dp(h)));}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
    private String ageText(long ms){if(ms<60_000)return "il y a "+Math.max(1,ms/1000)+" s";if(ms<3_600_000)return "il y a "+ms/60_000+" min";return "il y a "+ms/3_600_000+" h";}
}
