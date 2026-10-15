package ch.piiwii.suchard;
import android.app.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.view.*;import android.widget.*;
public final class PosterActivity extends Activity{
 @Override protected void onCreate(android.os.Bundle b){super.onCreate(b);getWindow().setStatusBarColor(MainActivity.BROWN);getWindow().setNavigationBarColor(MainActivity.CREAM);if(android.os.Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
 ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(0xfffaf5f1);LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(MainActivity.dp(this,14),MainActivity.dp(this,16),MainActivity.dp(this,14),MainActivity.dp(this,22));s.addView(page);setContentView(s);
 if(android.os.Build.VERSION.SDK_INT>=30){s.setClipToPadding(false);s.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets top=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.displayCutout());android.graphics.Insets bottom=insets.getInsets(WindowInsets.Type.navigationBars());v.setPadding(0,top.top,0,bottom.bottom);return insets;});s.requestApplyInsets();}
 TextView back=MainActivity.btn(this,"←  RETOUR AU PROGRAMME",MainActivity.BROWN,Color.WHITE);page.addView(back);back.setOnClickListener(v->finish());MainActivity.gap(this,page,12);
 page.addView(MainActivity.txt(this,"Affiche officielle · 200 ans Suchard",20,MainActivity.BROWN,true));MainActivity.gap(this,page,8);
 ImageView image=new ImageView(this);image.setImageResource(R.drawable.poster);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);page.addView(image,new LinearLayout.LayoutParams(-1,-2));
 MainActivity.gap(this,page,12);page.addView(MainActivity.txt(this,"Affiche du jubilé publiée sur suchard2026.ch. Cette image est incluse dans l'application pour une consultation hors ligne.",12,MainActivity.GRAY,false));
 MainActivity.gap(this,page,11);TextView official=MainActivity.btn(this,"↗  VOIR LE SITE OFFICIEL",0xffd15183,Color.WHITE);page.addView(official);
 official.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.suchard2026.ch/")));}catch(Exception ex){Toast.makeText(this,"Impossible d'ouvrir le site",Toast.LENGTH_SHORT).show();}});
 }
}
