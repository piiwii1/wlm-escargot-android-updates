package ch.piiwii.suchard;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
final class Cards {
 static void draw(Activity a,LinearLayout parent,int i,Runnable refresh){
  Route.Stop s=Route.ALL[i];SharedPreferences pref=a.getSharedPreferences("suchard",0);
  boolean done=pref.getBoolean("done"+i,false);
  LinearLayout card=new LinearLayout(a);card.setOrientation(1);card.setTag("stop_"+i);
  card.setPadding(MainActivity.dp(a,13),MainActivity.dp(a,13),MainActivity.dp(a,13),MainActivity.dp(a,13));
  card.setBackground(MainActivity.bg(a,done?0xffede9e5:Color.WHITE,15));
  LinearLayout.LayoutParams wrap=new LinearLayout.LayoutParams(-1,-2);
  wrap.topMargin=MainActivity.dp(a,10);parent.addView(card,wrap);
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);card.addView(row);
  LinearLayout details=new LinearLayout(a);details.setOrientation(1);
  row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
  details.addView(MainActivity.txt(a,s.time+"  ·  "+s.emoji,14,MainActivity.PINK,true));
  MainActivity.gap(a,details,3);
  TextView title=MainActivity.txt(a,s.title,16,MainActivity.DARK,true);title.setMaxLines(2);details.addView(title);
  MainActivity.gap(a,details,2);
  TextView address=MainActivity.txt(a,s.place,12,MainActivity.GRAY,false);address.setMaxLines(2);details.addView(address);
  int pic=photoFor(i);
  if(pic!=0){
   ImageView preview=new ImageView(a);preview.setImageResource(pic);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
   preview.setBackground(MainActivity.bg(a,0xffead8cf,10));preview.setClipToOutline(true);
   LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(MainActivity.dp(a,76),MainActivity.dp(a,73));
   pp.leftMargin=MainActivity.dp(a,8);row.addView(preview,pp);
   preview.setOnClickListener(v->{if(i==2||i==3)a.startActivity(new android.content.Intent(a,PosterActivity.class));});
  }
  MainActivity.gap(a,card,9);
  TextView note=MainActivity.txt(a,s.note,12,MainActivity.GRAY,false);
  note.setMaxLines(3);note.setLineSpacing(MainActivity.dp(a,1),1);
  note.setContentDescription("Détails de "+s.title+". Toucher pour déplier ou replier.");
  note.setOnClickListener(v->note.setMaxLines(note.getMaxLines()==3?100:3));
  card.addView(note);
  MainActivity.gap(a,card,11);
  LinearLayout actions=new LinearLayout(a);actions.setGravity(Gravity.CENTER_VERTICAL);card.addView(actions);
  TextView directions=MainActivity.btn(a,s.destination.isEmpty()?"☀ LIBRE":s.mode.equals("walking")?"↗ GPS PIÉTON":"↗ GPS VOITURE",MainActivity.BROWN,Color.WHITE);
  directions.setTextSize(12);
  LinearLayout.LayoutParams goParams=new LinearLayout.LayoutParams(0,MainActivity.dp(a,42),1);goParams.rightMargin=MainActivity.dp(a,6);
  actions.addView(directions,goParams);
  directions.setOnClickListener(v->{if(s.destination.isEmpty())Toast.makeText(a,"Promenade libre : aucun trajet imposé",Toast.LENGTH_SHORT).show();else Gps.start(a,i);});
  TextView completed=MainActivity.btn(a,done?"✓ FAIT":"○ TERMINÉ",0xfff2e6e2,MainActivity.BROWN);
  completed.setTextSize(12);actions.addView(completed,new LinearLayout.LayoutParams(0,MainActivity.dp(a,42),1));
  completed.setOnClickListener(v->{Notify.markDone(a,i,!done);refresh.run();});
 }
 static int photoFor(int i){
  switch(i){
   case 2:case 3:return R.drawable.poster;
   case 7:return R.drawable.fresco;
   case 8:return R.drawable.factory;
   case 9:return R.drawable.milka;
   default:return 0;
  }
 }
}
