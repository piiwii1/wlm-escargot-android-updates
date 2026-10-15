package ch.piiwii.suchard;
import android.app.Activity;import android.graphics.Color;import android.view.*;import android.widget.*;import android.content.SharedPreferences;
final class Cards {
 static void draw(Activity a,LinearLayout list,int index,Runnable redraw){
 Route.Stop s=Route.ALL[index];SharedPreferences prefs=a.getSharedPreferences("suchard",0);
 boolean done=prefs.getBoolean("done"+index,false);
 LinearLayout card=new LinearLayout(a);card.setOrientation(1);card.setPadding(MainActivity.dp(a,14),MainActivity.dp(a,15),MainActivity.dp(a,14),MainActivity.dp(a,14));
 card.setBackground(MainActivity.bg(a,done?0xffeeebe7:Color.WHITE,16));LinearLayout.LayoutParams outer=new LinearLayout.LayoutParams(-1,-2);outer.bottomMargin=MainActivity.dp(a,11);list.addView(card,outer);
 LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);card.addView(header);
 header.addView(MainActivity.txt(a,s.time,22,MainActivity.BROWN,true));
 header.addView(MainActivity.txt(a,"  "+s.emoji+"  ",18,MainActivity.DARK,false));
 header.addView(MainActivity.txt(a,s.title,17,MainActivity.DARK,true),new LinearLayout.LayoutParams(0,-2,1));
 MainActivity.gap(a,card,7);card.addView(MainActivity.txt(a,s.place,13,MainActivity.PINK,true));MainActivity.gap(a,card,6);
 TextView note=MainActivity.txt(a,s.note,13,MainActivity.GRAY,false);note.setLineSpacing(MainActivity.dp(a,3),1);card.addView(note);
 if(s.reminder&&prefs.getBoolean("on",true)){MainActivity.gap(a,card,7);card.addView(MainActivity.txt(a,"🔔 Rappel à "+String.format(java.util.Locale.FRANCE,"%02d:%02d",s.hour,s.minute),12,MainActivity.BROWN,true));}
 MainActivity.gap(a,card,12);LinearLayout actions=new LinearLayout(a);card.addView(actions);
 TextView gps=MainActivity.btn(a,s.destination.isEmpty()?"☀ À VOTRE RYTHME":"↗ Y ALLER",MainActivity.BROWN,Color.WHITE);
 LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,MainActivity.dp(a,44),1);left.rightMargin=MainActivity.dp(a,5);actions.addView(gps,left);
 gps.setOnClickListener(v->{if(s.destination.isEmpty())Toast.makeText(a,"Balade libre, aucune destination imposée.",Toast.LENGTH_SHORT).show();else Gps.start(a,index);});
 TextView check=MainActivity.btn(a,done?"✓ FAIT":"○ TERMINÉ",0xfff4ebe7,MainActivity.BROWN);
 LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,MainActivity.dp(a,44),1);right.leftMargin=MainActivity.dp(a,5);actions.addView(check,right);
 check.setOnClickListener(v->{prefs.edit().putBoolean("done"+index,!done).apply();redraw.run();});
 }
}
