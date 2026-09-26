package ch.piiwii.chatouvoice;

import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.*;
import java.util.*;

public class WakeWordService extends Service implements RecognitionListener {
  private SpeechRecognizer sr; private Intent ri; private boolean busy;
  private final Handler h=new Handler(Looper.getMainLooper());
  @Override public void onCreate(){super.onCreate(); channel(); startForeground(1001,note()); if(SpeechRecognizer.isRecognitionAvailable(this)){sr=SpeechRecognizer.createSpeechRecognizer(this); sr.setRecognitionListener(this); ri=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); ri.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); ri.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);}}
  @Override public int onStartCommand(Intent i,int f,int id){again(300); return START_STICKY;}
  @Override public IBinder onBind(Intent i){return null;}
  @Override public void onDestroy(){if(sr!=null){sr.cancel();sr.destroy();}super.onDestroy();}
  private void again(long d){h.postDelayed(()->{if(sr==null||busy||checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;try{sr.startListening(ri);busy=true;}catch(Exception e){busy=false;again(1000);}},d);}
  private boolean hit(Bundle b){ArrayList<String>x=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(x==null)return false;String w=getSharedPreferences("chatou",MODE_PRIVATE).getString("wake_word","ok chatou").toLowerCase(Locale.ROOT);for(String s:x)if(s.toLowerCase(Locale.ROOT).contains(w))return true;return false;}
  private void open(){busy=false;try{sr.cancel();}catch(Exception ignored){}try{startActivity(new Intent(this,VoiceConversationActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception ignored){}again(45000);}
  private void channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("chatou_wake","Chatou Voice",NotificationManager.IMPORTANCE_LOW));}
  private Notification note(){PendingIntent p=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,"chatou_wake").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Chatou Voice actif").setContentText("Écoute du mot de réveil").setOngoing(true).setContentIntent(p).build();}
  @Override public void onResults(Bundle b){busy=false;if(hit(b))open();else again(300);}
  @Override public void onPartialResults(Bundle b){if(hit(b))open();}
  @Override public void onError(int e){busy=false;again(600);}
  @Override public void onEndOfSpeech(){busy=false;again(300);}
  @Override public void onReadyForSpeech(Bundle b){} public void onBeginningOfSpeech(){} public void onRmsChanged(float f){} public void onBufferReceived(byte[] b){} public void onEvent(int e,Bundle b){}
}
