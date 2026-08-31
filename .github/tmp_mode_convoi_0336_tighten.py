from pathlib import Path

path=Path('mode-convoi-clean/android/app/src/main/java/ch/piiwii/modeconvoi/MainActivity.java')
text=path.read_text()

start=text.find('        JSONObject myStatus=me==null?null:me.optJSONObject("activeStatus");')
end=text.find('        ConvoyPositionResolver.RallyInfo rallyInfo=positionResolver.rallyInfo(snapshot);',start)
if start<0 or end<0:
    raise SystemExit('status duplicate anchors missing')
text=text[:start]+text[end:]

start=text.find('        ConvoyPositionResolver.RallyInfo rallyInfo=positionResolver.rallyInfo(snapshot);')
end=text.find('    private View positionCard(String title,JSONObject p,int stripeColor,boolean own){',start)
if start<0 or end<0:
    raise SystemExit('rally anchors missing')
replacement=r'''        ConvoyPositionResolver.RallyInfo rallyInfo=positionResolver.rallyInfo(snapshot);
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

'''
text=text[:start]+replacement+text[end:]

if 'JSONObject myStatus=me==null' in text:
    raise SystemExit('duplicate my status block still present')
if 'rallyRow=new LinearLayout' not in text:
    raise SystemExit('compact rally row missing')
path.write_text(text)
print('Mode Convoi 0.3.36 final driving layout tighten applied')
