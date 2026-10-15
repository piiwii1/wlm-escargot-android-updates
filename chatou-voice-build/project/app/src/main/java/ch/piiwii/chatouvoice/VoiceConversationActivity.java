package ch.piiwii.chatouvoice;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceConversationActivity extends Activity implements RecognitionListener, TextToSpeech.OnInitListener {
    private SpeechRecognizer sr; private Intent rec; private TextToSpeech tts; private TextView state, transcript; private boolean ttsReady=false, listening=false; private final ExecutorService io=Executors.newSingleThreadExecutor(); private final List<String[]> history=new ArrayList<>();
    @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(ui()); tts=new TextToSpeech(this,this); setupRecognizer(); new Handler(Looper.getMainLooper()).postDelayed(this::listen,500); }
    private LinearLayout ui(){ LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(40,52,40,30);r.setGravity(Gravity.CENTER_HORIZONTAL);r.setBackgroundColor(Color.rgb(245,247,250)); TextView title=new TextView(this);title.setText("Chatou");title.setTextSize(32); state=new TextView(this);state.setText("Initialisation…");state.setTextSize(20);state.setPadding(0,35,0,25); transcript=new TextView(this);transcript.setText("Parle naturellement. Dis « au revoir » pour fermer la conversation.");transcript.setTextSize(16);transcript.setPadding(20,20,20,20);transcript.setBackgroundColor(Color.WHITE); Button again=new Button(this);again.setText("🎙 Parler");again.setOnClickListener(v->listen()); Button close=new Button(this);close.setText("Fermer");close.setOnClickListener(v->finish());r.addView(title);r.addView(state);r.addView(transcript,new LinearLayout.LayoutParams(-1,0,1));r.addView(again);r.addView(close);return r; }
    private void setupRecognizer(){ if(!SpeechRecognizer.isRecognitionAvailable(this)){ state.setText("Reconnaissance vocale indisponible");return;} sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(this);rec=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);rec.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);rec.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault().toLanguageTag());rec.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true); }
    private void listen(){ if(sr==null||listening)return; try{ if(tts!=null)tts.stop(); sr.startListening(rec);listening=true;state.setText("🎙 Je t'écoute…");}catch(Exception e){state.setText("Micro occupé");} }
    private void send(String text){ if(text==null||text.trim().isEmpty()){listen();return;} String low=text.toLowerCase(Locale.ROOT); transcript.setText("Toi : "+text); if(low.matches(".*\\b(au revoir|stop chatou|ferme chatou)\\b.*")){ speakThenClose("À bientôt.");return;} String key=SecurePrefs.getApiKey(this); if(key.isEmpty()){ state.setText("Clé API manquante"); Toast.makeText(this,"Configure la clé API dans les paramètres",Toast.LENGTH_LONG).show();return;} history.add(new String[]{"user",text}); while(history.size()>12)history.remove(0); state.setText("Chatou réfléchit…"); String model=getSharedPreferences("chatou",MODE_PRIVATE).getString("model","gpt-5.6-luna"); io.submit(()->{ try{String ans=OpenAiClient.ask(key,model,history);history.add(new String[]{"assistant",ans});runOnUiThread(()->respond(ans));}catch(Exception e){runOnUiThread(()->{state.setText("Erreur de connexion");transcript.setText("Erreur : "+e.getMessage());});} }); }
    private void respond(String ans){ transcript.setText(transcript.getText()+"\n\nChatou : "+ans);state.setText("🔊 Chatou répond…"); if(getSharedPreferences("chatou",MODE_PRIVATE).getBoolean("speak",true)&&ttsReady){ Bundle p=new Bundle(); tts.speak(ans,TextToSpeech.QUEUE_FLUSH,p,"reply"); } else new Handler(Looper.getMainLooper()).postDelayed(this::listen,500); }
    private void speakThenClose(String s){ if(ttsReady){tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"close");} else finish(); }
    @Override public void onInit(int status){ if(status==TextToSpeech.SUCCESS){ttsReady=true;tts.setLanguage(Locale.FRENCH);tts.setSpeechRate(1.0f);tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onError(String id){runOnUiThread(()->listen());}public void onDone(String id){runOnUiThread(()->{if("close".equals(id))finish();else listen();});}}); } }
    @Override public void onResults(Bundle r){listening=false;ArrayList<String>x=r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);send(x==null||x.isEmpty()?"":x.get(0));}
    @Override public void onPartialResults(Bundle r){ArrayList<String>x=r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(x!=null&&!x.isEmpty())state.setText("🎙 "+x.get(0));}
    @Override public void onError(int e){listening=false;state.setText("Je réécoute…");new Handler(Looper.getMainLooper()).postDelayed(this::listen,600);}
    @Override protected void onDestroy(){if(sr!=null){sr.cancel();sr.destroy();}if(tts!=null){tts.stop();tts.shutdown();}io.shutdownNow();super.onDestroy();}
    @Override public void onReadyForSpeech(Bundle b){}@Override public void onBeginningOfSpeech(){}@Override public void onRmsChanged(float f){}@Override public void onBufferReceived(byte[] b){}@Override public void onEndOfSpeech(){listening=false;}@Override public void onEvent(int i,Bundle b){}
}
