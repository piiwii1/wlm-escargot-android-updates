package ch.piiwii.suchard;
import android.app.Activity;import android.graphics.Color;import android.view.*;import android.widget.*;import android.content.SharedPreferences;
final class Cards {
 static void draw(Activity a,LinearLayout parent,int i,Runnable refresh){
 Route.Stop s=Route.ALL[i];SharedPreferences prefs=a.getSharedPreferences("suchard",0);boolean done=prefs.getBoolean("done"+i,false);
 LinearLayout card=new LinearLayout(a);card.setOrientation(1);card.setTag("stop_"+i);card.setPadding(MainActivity.dp(a,13),MainActivity.dp(a,13),MainActivity.dp(a,13),MainActivity.dp(a,13));
 card.setBackground(MainActivity.bg(a,done?0xfff2ece7:Color.WHITE,16));LinearLayout.LayoutParams wrap=new LinearLayout.LayoutParams(-1,-2);wrap.bottomMargin=MainActivity.dp(a,12);parent.addView(card,wrap);
 LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head);
 TextView hour=MainActivity.txt(a,s.time,22,MainActivity.BROWN,true);head.addView(hour);
 head.addView(MainActivity.txt(a,"   "+s.emoji+"   ",18,MainActivity.PINK,false));
 TextView title=MainActivity.txt(a,s.title,17,MainActivity.DARK,true);head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
 MainActivity.gap(a,card,4);card.addView(MainActivity.txt(a,s.place,12,MainActivity.PINK,true));
 int img=photoFor(i);
 if(img!=0){
   MainActivity.gap(a,card,11);
   ImageView image=new ImageView(a);image.setImageResource(img);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
   image.setBackground(MainActivity.bg(a,0xffe0c6ad,10));image.setClipToOutline(true);
   card.addView(image,new LinearLayout.LayoutParams(-1,MainActivity.dp(a,138)));
 }
 MainActivity.gap(a,card,11);
 TextView note=MainActivity.txt(a,s.note,13,MainActivity.GRAY,false);note.setLineSpacing(MainActivity.dp(a,3),1);card.addView(note);
 MainActivity.gap(a,card,8);
 LinearLayout badge=new LinearLayout(a);badge.setGravity(Gravity.CENTER_VERTICAL);card.addView(badge);
 String type=s.destination.isEmpty()?"☀  LIBRE":s.mode.equals("walking")?"🚶  À PIED":"🚗  VOITURE";
 badge.addView(MainActivity.txt(a,type,11,MainActivity.BROWN,true));
 if(s.reminder&&prefs.getBoolean("on",true)){TextView reminder=MainActivity.txt(a,"🔔  "+String.format(java.util.Locale.FRANCE,"%02d:%02d",s.hour,s.minute),12,MainActivity.GRAY,true);reminder.setGravity(Gravity.RIGHT);badge.addView(reminder,new LinearLayout.LayoutParams(0,-2,1));}
 MainActivity.gap(a,card,12);
 LinearLayout actions=new LinearLayout(a);card.addView(actions);
 TextView go=MainActivity.btn(a,s.destination.isEmpty()?"☀  TEMPS LIBRE":"↗  Y ALLER",MainActivity.BROWN,Color.WHITE);
 LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,MainActivity.dp(a,44),1);left.rightMargin=MainActivity.dp(a,5);actions.addView(go,left);
 go.setOnClickListener(v->{if(s.destination.isEmpty())Toast.makeText(a,"Profite de la promenade libre, sans destination imposée.",Toast.LENGTH_SHORT).show();else Gps.start(a,i);});
 TextView check=MainActivity.btn(a,done?"✓  FAIT":"○  TERMINÉ",done?0xffd8e5d8:0xfff6e8e2,MainActivity.BROWN);
 LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,MainActivity.dp(a,44),1);right.leftMargin=MainActivity.dp(a,5);actions.addView(check,right);
 check.setOnClickListener(v->{prefs.edit().putBoolean("done"+i,!done).apply();refresh.run();});
 if(i==3||i==7||i==8||i==9){MainActivity.gap(a,card,8);TextView media=MainActivity.txt(a,i==3?"Image : affiche officielle du jubilé":i==7?"Photo : site Suchard 2026":i==8?"Photo : patrimoine Suchard": "Visuel du parcours Milka · site officiel",10,MainActivity.GRAY,false);card.addView(media);}
 }
 static int photoFor(int i){switch(i){case 2:return R.drawable.poster;case 3:return R.drawable.poster;case 7:return R.drawable.fresco;case 8:return R.drawable.factory;case 9:return R.drawable.milka;default:return 0;}}
}
