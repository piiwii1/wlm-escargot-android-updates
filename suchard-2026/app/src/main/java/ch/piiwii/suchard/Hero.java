package ch.piiwii.suchard;
import android.app.Activity;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;
final class Hero {
 static void draw(Activity a,LinearLayout page,int next){
   LinearLayout top=new LinearLayout(a);top.setOrientation(1);top.setBackgroundColor(0xff412820);top.setPadding(MainActivity.dp(a,20),MainActivity.dp(a,20),MainActivity.dp(a,20),MainActivity.dp(a,22));page.addView(top);
   TextView eyebrow=MainActivity.txt(a,"PIIWII  •  SORTIE CHOCOLATÉE",11,0xffeabb94,true);top.addView(eyebrow);
   MainActivity.gap(a,top,10);
   TextView title=MainActivity.txt(a,"SUCHARD 200 ANS",28,0xffffffff,true);top.addView(title);
   top.addView(MainActivity.txt(a,"SAMEDI 17 OCTOBRE 2026  ·  NEUCHÂTEL",12,0xfff4cfba,false));
   MainActivity.gap(a,top,15);
   ImageView photo=new ImageView(a);photo.setImageResource(R.drawable.hero_history);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);photo.setBackgroundColor(0xff754e3e);
   top.addView(photo,new LinearLayout.LayoutParams(-1,MainActivity.dp(a,178)));photo.setOnClickListener(v->openPoster(a));
   MainActivity.gap(a,top,12);
   LinearLayout action=new LinearLayout(a);action.setOrientation(0);top.addView(action);
   TextView poster=MainActivity.btn(a,"▧  VOIR L'AFFICHE",0xfff9dfd6,0xff512c27);
   action.addView(poster,new LinearLayout.LayoutParams(0,MainActivity.dp(a,44),1));poster.setOnClickListener(v->openPoster(a));
   TextView site=MainActivity.btn(a,"↗ SITE OFFICIEL",0xff85584d,0xffffffff);LinearLayout.LayoutParams r=new LinearLayout.LayoutParams(0,MainActivity.dp(a,44),1);r.leftMargin=MainActivity.dp(a,9);action.addView(site,r);
   site.setOnClickListener(v->{try{a.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.suchard2026.ch/")));}catch(Exception e){Toast.makeText(a,"Site officiel indisponible",Toast.LENGTH_SHORT).show();}});
   LinearLayout summary=new LinearLayout(a);summary.setOrientation(1);summary.setPadding(MainActivity.dp(a,16),MainActivity.dp(a,16),MainActivity.dp(a,16),MainActivity.dp(a,16));summary.setBackground(MainActivity.bg(a,0xffffffff,17));
   LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(MainActivity.dp(a,15),MainActivity.dp(a,14),MainActivity.dp(a,15),0);page.addView(summary,sp);
   int doneCount=0;for(int i=0;i<Route.ALL.length;i++)if(a.getSharedPreferences("suchard",0).getBoolean("done"+i,false))doneCount++;
   LinearLayout stats=new LinearLayout(a);stats.setGravity(Gravity.CENTER_VERTICAL);summary.addView(stats);
   stats.addView(MainActivity.txt(a,"TON AVANCEMENT",12,MainActivity.BROWN,true));
   TextView count=MainActivity.txt(a,doneCount+" / "+Route.ALL.length+" étapes",13,MainActivity.GRAY,true);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);count.setGravity(Gravity.RIGHT);stats.addView(count,cp);
   MainActivity.gap(a,summary,8);
   ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(Route.ALL.length);progress.setProgress(doneCount);
   progress.setProgressTintList(android.content.res.ColorStateList.valueOf(MainActivity.PINK));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffeee3de));summary.addView(progress,new LinearLayout.LayoutParams(-1,MainActivity.dp(a,8)));
   MainActivity.gap(a,summary,14);
   if(next>=0){Route.Stop st=Route.ALL[next];
     summary.addView(MainActivity.txt(a,"PROCHAINE ÉTAPE  ·  "+st.time,12,MainActivity.PINK,true));MainActivity.gap(a,summary,4);
     summary.addView(MainActivity.txt(a,st.emoji+"  "+st.title,19,MainActivity.DARK,true));
     summary.addView(MainActivity.txt(a,st.place,13,MainActivity.GRAY,false));MainActivity.gap(a,summary,10);
     if(!st.destination.isEmpty()){TextView go=MainActivity.btn(a,"↗  Y ALLER MAINTENANT",MainActivity.BROWN,0xffffffff);summary.addView(go);go.setOnClickListener(v->Gps.start(a,next));}
   }else{summary.addView(MainActivity.txt(a,"🎉 Journée terminée, profite des souvenirs !",17,MainActivity.BROWN,true));}
 }
 static void openPoster(Activity a){a.startActivity(new Intent(a,PosterActivity.class));}
}
