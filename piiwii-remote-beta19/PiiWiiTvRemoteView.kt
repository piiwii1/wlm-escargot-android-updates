package ch.piiwii.remote2.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ch.piiwii.remote2.R

object PiiWiiTvRemoteView {
    fun create(context: Context, paired: Boolean, siteUrl: String, onPair: () -> Unit, onAction: (String) -> Unit): View {
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = UiKit.dp(context, 18)
            setPadding(p, p, p, UiKit.dp(context, 28))
        }
        body.addView(UiKit.title(context, "PiiWii TV"))
        UiKit.addGap(body, 8)
        body.addView(UiKit.card(context, padding = 14) {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_piiwii_tv_custom)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(UiKit.dp(context, 54), UiKit.dp(context, 54)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(UiKit.dp(context, 12), 0, 0, 0)
                addView(TextView(context).apply { text = if (paired) "● Télécommande associée" else "● Association requise"; textSize=14f; setTypeface(typeface, Typeface.BOLD); setTextColor(if (paired) UiKit.ONLINE else UiKit.CHECKING) })
                addView(UiKit.secondary(context, siteUrl, 12f))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(smallButton(context, if (paired) "Gérer" else "Associer", onPair))
        })
        UiKit.addGap(body, 10)
        body.addView(section(context, "TÉLÉVISION"))
        UiKit.addGap(body, 6)
        body.addView(row(context,
            button(context,"⏻","TV / Direct", true){onAction("power")},
            button(context,"CH−","Chaîne −"){onAction("channel_down")},
            button(context,"CH+","Chaîne +"){onAction("channel_up")}
        ))
        UiKit.addGap(body, 6)
        body.addView(row(context,
            button(context,"VOL−","Volume −"){onAction("volume_down")},
            button(context,"🔇","Muet"){onAction("mute")},
            button(context,"VOL+","Volume +"){onAction("volume_up")}
        ))
        UiKit.addGap(body, 8)
        body.addView(section(context, "NAVIGATION")); UiKit.addGap(body, 4)
        val pad = GridLayout(context).apply { columnCount=3; rowCount=3 }
        fun nav(g:String,l:String,a:String,r:Int,c:Int){ pad.addView(button(context,g,l, a=="nav_ok"){onAction(a)}, GridLayout.LayoutParams(GridLayout.spec(r),GridLayout.spec(c)).apply{ width=0; height=UiKit.dp(context,52); setMargins(UiKit.dp(context,3),UiKit.dp(context,2),UiKit.dp(context,3),UiKit.dp(context,2)) }) }
        nav("⌂","Accueil","home",0,0); nav("▲","Haut","nav_up",0,1); nav("↩","Retour","back",0,2)
        nav("◀","Gauche","nav_left",1,0); nav("OK","OK","nav_ok",1,1); nav("▶","Droite","nav_right",1,2)
        nav("⚙","Réglages","settings",2,0); nav("▼","Bas","nav_down",2,1); nav("TV","Direct","direct",2,2)
        body.addView(pad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        UiKit.addGap(body, 8)
        body.addView(section(context, "ACCÈS RAPIDES")); UiKit.addGap(body, 4)
        body.addView(row(context,
            button(context,"▣","Guide TV"){onAction("guide")},
            button(context,"REC","Enregistrements"){onAction("recordings")},
            button(context,"♫","Radio"){onAction("radio")}
        ))
        UiKit.addGap(body, 5)
        body.addView(row(context,
            button(context,"⛶","Plein écran", true){onAction("fullscreen")},
            button(context,"▭","Petit écran"){onAction("windowed")},
            button(context,"●","Enregistrer"){onAction("record")}
        ))
        UiKit.addGap(body, 8)
        body.addView(section(context, "LECTURE")); UiKit.addGap(body, 4)
        body.addView(row(context,
            button(context,"−10","−10 s"){onAction("seek_back")},
            button(context,"▶❚❚","Lecture / pause", true){onAction("play_pause")},
            button(context,"+30","+30 s"){onAction("seek_forward")}
        ))
        return ScrollView(context).apply { isFillViewport=true; addView(body) }
    }

    private fun section(c: Context, t:String)=TextView(c).apply{ text=t; textSize=11f; letterSpacing=.08f; setTextColor(UiKit.TEXT_SECONDARY); setTypeface(typeface,Typeface.BOLD) }
    private fun row(c:Context,vararg views:View)=LinearLayout(c).apply{ orientation=LinearLayout.HORIZONTAL; views.forEach{ addView(it, LinearLayout.LayoutParams(0,UiKit.dp(c,58),1f).apply{setMargins(UiKit.dp(c,3),0,UiKit.dp(c,3),0)}) } }
    private fun button(c:Context,g:String,l:String,em:Boolean=false,click:()->Unit)=LinearLayout(c).apply{
        orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; isClickable=true; isFocusable=true; background=UiKit.interactive(c,16,if(em)UiKit.SURFACE_PRESSED else UiKit.SURFACE_ALT); setPadding(6,6,6,5)
        addView(TextView(c).apply{text=g; gravity=Gravity.CENTER; textSize=if(g.length<=3)20f else 15f; setTypeface(typeface,Typeface.BOLD); setTextColor(if(em)UiKit.ACCENT else UiKit.TEXT)})
        addView(TextView(c).apply{text=l; gravity=Gravity.CENTER; textSize=10.5f; maxLines=1; setTextColor(UiKit.TEXT_SECONDARY)})
        contentDescription=l; setOnClickListener{ performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); click() }
    }
    private fun smallButton(c:Context,t:String,click:()->Unit)=TextView(c).apply{ text=t; textSize=12f; setTypeface(typeface,Typeface.BOLD); setTextColor(UiKit.ACCENT); gravity=Gravity.CENTER; setPadding(UiKit.dp(c,12),UiKit.dp(c,10),UiKit.dp(c,12),UiKit.dp(c,10)); background=UiKit.interactive(c,12,UiKit.SURFACE_ALT); isClickable=true; setOnClickListener{click()} }
}
