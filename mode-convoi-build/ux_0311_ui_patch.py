from pathlib import Path
import re
p=Path('mode-convoi-build/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
s=p.read_text()

def rep(old,new,label):
    global s
    if old not in s: raise SystemExit('missing '+label)
    s=s.replace(old,new,1)

rep('private int bg, card, fg, muted, accent, danger;', 'private int bg, card, fg, muted, accent, danger, border, control, navSurface;\n    private boolean darkTheme;', 'fields')

s,n=re.subn(r'''    private void applyPalette\(\) \{.*?\n    \}\n\n    private void render\(\) \{''','''    private void applyPalette() {
        String mode = prefs == null ? "dark" : prefs.get("theme", "dark");
        darkTheme = !mode.equals("light");
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

    private void render() {''',s,flags=re.S)
if n!=1: raise SystemExit('palette replace '+str(n))

rep('root.setPadding(dp(14),dp(8),dp(14),dp(24));','root.setPadding(dp(16),dp(8),dp(16),dp(28));','root padding')
rep('line.setBackgroundColor(Color.rgb(41,43,45));','line.setBackgroundColor(border);','divider')

old='''        LinearLayout codeRow=new LinearLayout(this); codeRow.setGravity(Gravity.CENTER_VERTICAL); TextView codeV=text("CODE  "+code,13,true,muted); codeRow.addView(codeV,new LinearLayout.LayoutParams(0,dp(44),1));
        Button qr=smallButton("MON QR",card,fg);qr.setOnClickListener(v->showConvoyQr());codeRow.addView(qr,new LinearLayout.LayoutParams(dp(82),dp(40)));
        Button share=smallButton("PARTAGER",card,fg);share.setOnClickListener(v->shareConvoy());LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-2,dp(40));slp.setMargins(dp(6),0,0,0);codeRow.addView(share,slp);content.addView(codeRow);'''
new='''        LinearLayout codeRow=new LinearLayout(this); codeRow.setGravity(Gravity.CENTER_VERTICAL); codeRow.setPadding(dp(12),dp(5),dp(7),dp(5)); codeRow.setBackground(roundBg(control,border,15,1));
        LinearLayout.LayoutParams codeLp=new LinearLayout.LayoutParams(-1,dp(54)); codeLp.setMargins(0,dp(5),0,dp(8)); codeRow.setLayoutParams(codeLp);
        TextView codeV=text("CODE   "+code,14,true,fg); codeV.setLetterSpacing(.05f); codeRow.addView(codeV,new LinearLayout.LayoutParams(0,dp(44),1));
        Button qr=smallButton("QR",Color.TRANSPARENT,accent); qr.setBackground(roundBg(Color.TRANSPARENT,accent,11,1)); qr.setOnClickListener(v->showConvoyQr()); codeRow.addView(qr,new LinearLayout.LayoutParams(dp(64),dp(38)));
        Button share=smallButton("PARTAGER",Color.TRANSPARENT,accent); share.setBackground(roundBg(Color.TRANSPARENT,accent,11,1)); share.setOnClickListener(v->shareConvoy()); LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(dp(96),dp(38)); slp.setMargins(dp(6),0,0,0); codeRow.addView(share,slp); content.addView(codeRow);'''
rep(old,new,'code row')

s,n=re.subn(r'''\n        LinearLayout nav=new LinearLayout\(this\);Button map=outlinedButton\("⌖  CARTE",accent\);.*?Button options=ghostButton\("⋮  PARAMÈTRES DU CONVOI"\);options\.setOnClickListener\(v->convoyOptions\(\)\);content\.addView\(options\);''','',s,flags=re.S)
if n!=1: raise SystemExit('home duplicates '+str(n))

rep('outer.setBackground(roundBg(card,own?accent:Color.rgb(52,55,58),14,own?2:1));','outer.setBackground(roundBg(card,own?accent:border,16,own?2:1));','position card')
rep('cardTitle(content,"Mode Convoi 0.3.10"','cardTitle(content,"Mode Convoi 0.3.11"','about version')

start=s.index('    private LinearLayout cardBox(){')
end=s.index('    private LinearLayout buildBottomNav(){',start)
helpers='''    private LinearLayout cardBox(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(16),dp(14),dp(16),dp(14));b.setBackground(roundBg(card,border,16,1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(lp);return b;}
    private void cardTitle(LinearLayout parent,String title,String sub){LinearLayout b=cardBox();b.addView(text(title,18,true,fg));TextView sv=text(sub,13,false,muted);sv.setPadding(0,dp(5),0,0);b.addView(sv);parent.addView(b);}
    private void sectionLabel(LinearLayout parent,String label){TextView v=text(label,12,true,accent);v.setLetterSpacing(.05f);v.setPadding(dp(2),dp(17),0,dp(6));parent.addView(v);}
    private LinearLayout pageHeader(String left,String title){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(0,dp(2),0,dp(3));TextView b=text(left,28,false,fg);b.setGravity(Gravity.CENTER);h.addView(b,new LinearLayout.LayoutParams(dp(46),dp(54)));TextView t=text(title,18,true,fg);t.setGravity(Gravity.CENTER);h.addView(t,new LinearLayout.LayoutParams(0,dp(54),1));TextView more=text("⋮",24,false,muted);more.setGravity(Gravity.CENTER);h.addView(more,new LinearLayout.LayoutParams(dp(46),dp(54)));return h;}
    private TextView text(String value,int sp,boolean bold,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(sp);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(false);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private EditText profileInput(LinearLayout parent,String icon,String label,String value){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(9),dp(4),dp(9),dp(4));TextView ic=text(icon,17,false,muted);ic.setGravity(Gravity.CENTER);row.addView(ic,new LinearLayout.LayoutParams(dp(36),dp(54)));LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setGravity(Gravity.CENTER_VERTICAL);labels.addView(text(label,11,true,muted));EditText e=new EditText(this);e.setText(value);e.setTextColor(fg);e.setHintTextColor(muted);e.setSingleLine(true);e.setTextSize(15);e.setPadding(0,0,0,0);e.setBackgroundColor(Color.TRANSPARENT);labels.addView(e,new LinearLayout.LayoutParams(-1,dp(31)));row.addView(labels,new LinearLayout.LayoutParams(0,dp(58),1));parent.addView(row);return e;}
    private EditText input(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted);e.setText(value);e.setTextColor(fg);e.setSingleLine(true);e.setTextSize(15);e.setPadding(dp(15),0,dp(15),0);e.setBackground(roundBg(control,border,14,1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));e.setLayoutParams(lp);return e;}
    private Button button(String label,int bgColor,int textColor){Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(textColor);b.setAllCaps(false);b.setLetterSpacing(.02f);b.setGravity(Gravity.CENTER);b.setMinHeight(0);b.setMinWidth(0);b.setPadding(dp(12),0,dp(12),0);b.setStateListAnimator(null);b.setBackgroundTintList(null);b.setBackground(roundBg(bgColor,bgColor,14,0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(lp);return b;}
    private Button outlinedButton(String label,int color){int c=(color==Color.rgb(94,99,104))?fg:color;Button b=button(label,control,c);b.setBackground(roundBg(control,c==fg?border:c,14,1));return b;}
    private Button ghostButton(String label){Button b=button(label,control,fg);b.setTextSize(12);b.setBackground(roundBg(control,border,14,1));return b;}
    private Button smallButton(String label,int bgColor,int textColor){Button b=button(label,bgColor,textColor);b.setTextSize(11);b.setMinHeight(0);return b;}
    private Button quickButton(String label){Button b=button(label,control,fg);b.setTextSize(11);b.setGravity(Gravity.CENTER);b.setPadding(dp(4),dp(6),dp(4),dp(6));b.setBackground(roundBg(control,border,14,1));return b;}
    private Button adminButton(String label,boolean destructive){Button b=button(label,control,destructive?danger:fg);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setTextSize(14);b.setBackground(roundBg(control,destructive?danger:border,12,1));return b;}
'''
s=s[:start]+helpers+s[end:]
rep('nav.setBackground(roundBg(Color.rgb(14,16,18),Color.rgb(44,47,50),0,1));','nav.setBackground(roundBg(navSurface,border,0,1));','bottom nav bg')
rep('item.setPadding(dp(2),dp(4),dp(2),dp(2));','item.setPadding(dp(2),dp(6),dp(2),dp(4));','bottom nav padding')
rep('nav.addView(item,new LinearLayout.LayoutParams(0,dp(62),1));','nav.addView(item,new LinearLayout.LayoutParams(0,dp(64),1));','bottom nav height')

p.write_text(s)
print('Mode Convoi 0.3.11 UI patch applied')
