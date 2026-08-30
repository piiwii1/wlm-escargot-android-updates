from pathlib import Path
import re

p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

s=s.replace('private LinearLayout root, content;','private LinearLayout root, content, bottomNav;')

s=re.sub(r'    private void render\(\) \{.*?\n    \}\n\n    private void header\(\)', '''    private void render() {
        mapView = null;
        applyPalette();

        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(bg);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(bg);
        screen.addView(shell, new FrameLayout.LayoutParams(-1,-1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(bg);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(8),dp(14),dp(24));
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

        screen.setOnApplyWindowInsetsListener((v,insets)->{
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            shell.setPadding(0,topInset,0,0);
            if(bottomNav!=null) bottomNav.setPadding(dp(4),dp(4),dp(4),bottomInset+dp(4));
            return insets;
        });
        screen.requestApplyInsets();
    }

    private void header()''', s, flags=re.S, count=1)

s=re.sub(r'    private void header\(\) \{.*?\n    \}\n\n    private void renderWelcome', '''    private void header() {
        LinearLayout h=new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(4),dp(4),dp(4),dp(6));

        Space left=new Space(this);
        h.addView(left,new LinearLayout.LayoutParams(dp(82),dp(50)));

        TextView title=text("MODE CONVOI",19,true,accent);
        title.setGravity(Gravity.CENTER);
        h.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));

        connectionBadge=text(prefs.hasActiveConvoy()?"●  ACTIF":"INACTIF",11,true,prefs.hasActiveConvoy()?Color.rgb(110,204,70):muted);
        connectionBadge.setGravity(Gravity.CENTER);
        connectionBadge.setPadding(dp(12),0,dp(12),0);
        connectionBadge.setBackground(roundBg(prefs.hasActiveConvoy()?Color.rgb(26,47,24):Color.rgb(42,44,46), prefs.hasActiveConvoy()?Color.rgb(71,133,48):Color.rgb(76,79,82), 18, 1));
        h.addView(connectionBadge,new LinearLayout.LayoutParams(dp(82),dp(32)));
        root.addView(h);

        View line=new View(this);
        line.setBackgroundColor(Color.rgb(41,43,45));
        root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }

    private void renderWelcome''', s, flags=re.S, count=1)

s=re.sub(r'        String\[\]\[\] statuses=\{\{.*?\}\};', '        String[][] statuses={{"🛑 Je m’arrête","stop"},{"⛽ Essence","fuel"},{"☕ Pause","pause"},{"🚻 WC","wc"},{"⚠ Problème","problem"},{"🚗 Voiture","car_problem"},{"↗ Je rejoins","joining"},{"👍 OK","ok"}};', s, flags=re.S, count=1)

s=re.sub(r'\n        LinearLayout nav=new LinearLayout\(this\);.*?Button options=ghostButton\("⋮  PARAMÈTRES DU CONVOI"\); options\.setOnClickListener\(v->convoyOptions\(\)\); content\.addView\(options\);', '', s, flags=re.S, count=1)

marker='    private void spacer(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}'
if marker not in s:
    marker='    private void spacer(int h){Space sv=new Space(this);content.addView(sv,new LinearLayout.LayoutParams(1,dp(h)));}'
if marker not in s:
    raise SystemExit('spacer marker not found')

helpers='''    private LinearLayout buildBottomNav(){
        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setBackground(roundBg(Color.rgb(14,16,18),Color.rgb(44,47,50),0,1));
        addBottomNavItem(nav,"⌂","Accueil","home");
        addBottomNavItem(nav,"⌖","Carte","map");
        addBottomNavItem(nav,"👥","Participants","participants");
        addBottomNavItem(nav,"•••","Plus","more");
        return nav;
    }

    private void refreshBottomNav(){
        if(bottomNav==null)return;
        bottomNav.removeAllViews();
        addBottomNavItem(bottomNav,"⌂","Accueil","home");
        addBottomNavItem(bottomNav,"⌖","Carte","map");
        addBottomNavItem(bottomNav,"👥","Participants","participants");
        addBottomNavItem(bottomNav,"•••","Plus","more");
    }

    private void addBottomNavItem(LinearLayout nav,String icon,String label,String page){
        boolean active=("home".equals(page) && ("home".equals(currentPage)||"welcome".equals(currentPage))) || page.equals(currentPage);
        TextView item=text(icon+"\\n"+label,11,active,active?accent:muted);
        item.setGravity(Gravity.CENTER);
        item.setLines(2);
        item.setPadding(dp(2),dp(4),dp(2),dp(2));
        item.setOnClickListener(v->navigateBottom(page));
        nav.addView(item,new LinearLayout.LayoutParams(0,dp(62),1));
    }

    private void navigateBottom(String page){
        if("home".equals(page)){
            render();
            if(prefs.hasActiveConvoy()) startPolling();
            return;
        }
        if("more".equals(page)){
            if(prefs.hasActiveConvoy()) convoyOptions(); else themeDialog();
            return;
        }
        if(!prefs.hasActiveConvoy()){
            toast("Crée ou rejoins un convoi d’abord");
            return;
        }
        if("map".equals(page)) renderMapPage();
        else if("participants".equals(page)) renderParticipantsPage();
        refreshBottomNav();
    }

'''
s=s.replace(marker,helpers+marker)

p.write_text(s)
print('bottom-nav patch applied')