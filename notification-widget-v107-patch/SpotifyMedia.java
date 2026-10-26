package ch.piiwii.notificationwidget;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.Locale;

/**
 * Uses Android's active media sessions (available to an enabled notification
 * listener). Unlike ordinary notification text, playback state changes on
 * play/pause/track changes without requiring a new notification.
 */
final class SpotifyMedia {
  private static MediaSessionManager manager;
  private static MediaController controller;
  private static MediaController.Callback playbackCallback;
  private static MediaSessionManager.OnActiveSessionsChangedListener sessionsListener;
  private static NotificationMonitor listener;
  private static final Handler main=new Handler(Looper.getMainLooper());
  private static boolean wasPlaying=false;
  private static boolean allowed=false;

  private SpotifyMedia(){}

  static synchronized void start(NotificationMonitor service){
    stop();
    listener=service;
    manager=(MediaSessionManager)service.getSystemService(Context.MEDIA_SESSION_SERVICE);
    if(manager==null)return;
    ComponentName name=new ComponentName(service,NotificationMonitor.class);
    try{
      sessionsListener=sessions->{
        synchronized(SpotifyMedia.class){
          select(sessions);
        }
        WidgetProvider.refresh(service);
      };
      manager.addOnActiveSessionsChangedListener(sessionsListener,name,main);
      allowed=true;
      select(manager.getActiveSessions(name));
    }catch(SecurityException ex){
      allowed=false;
      clearController();
    }catch(RuntimeException ex){
      allowed=false;
      clearController();
    }
  }

  static synchronized void stop(){
    if(manager!=null&&sessionsListener!=null){
      try{manager.removeOnActiveSessionsChangedListener(sessionsListener);}
      catch(RuntimeException ignored){}
    }
    clearController();
    manager=null;
    sessionsListener=null;
    listener=null;
    allowed=false;
    wasPlaying=false;
  }

  private static void clearController(){
    if(controller!=null&&playbackCallback!=null){
      try{controller.unregisterCallback(playbackCallback);}
      catch(RuntimeException ignored){}
    }
    controller=null;
    playbackCallback=null;
  }

  private static boolean isSpotify(MediaController c){
    return c!=null&&c.getPackageName()!=null&&
      c.getPackageName().toLowerCase(Locale.ROOT).contains("spotify");
  }

  private static boolean isPlaying(MediaController c){
    if(c==null)return false;
    try{
      PlaybackState state=c.getPlaybackState();
      return state!=null&&(state.getState()==PlaybackState.STATE_PLAYING
        ||state.getState()==PlaybackState.STATE_BUFFERING);
    }catch(RuntimeException ignored){return false;}
  }

  private static void select(List<MediaController> sessions){
    MediaController chosen=null;
    if(sessions!=null)for(MediaController item:sessions){
      if(isSpotify(item)){
        chosen=item;
        if(isPlaying(item))break;
      }
    }
    if(controller!=chosen){
      clearController();
      controller=chosen;
      if(controller!=null){
        playbackCallback=new MediaController.Callback(){
          @Override public void onPlaybackStateChanged(PlaybackState state){
            onPlaybackChange();
          }
          @Override public void onSessionDestroyed(){
            onPlaybackChange();
          }
        };
        try{controller.registerCallback(playbackCallback,main);}
        catch(RuntimeException ignored){}
      }
    }
    updatePlaying();
  }

  private static void onPlaybackChange(){
    synchronized(SpotifyMedia.class){updatePlaying();}
    if(listener!=null)WidgetProvider.refresh(listener);
  }

  private static void updatePlaying(){
    boolean now=isPlaying(controller);
    if(now&&!wasPlaying&&listener!=null){
      EphemeralEvents.onMediaStarted(listener);
    }
    wasPlaying=now;
  }

  static synchronized NotificationMonitor.Pick playingPick(Context c){
    if(!Categories.enabled(c,"spotify"))return null;
    if(controller==null || !isPlaying(controller))return null;
    PendingIntent intent=null;
    try{intent=controller.getSessionActivity();}catch(RuntimeException ignored){}
    return new NotificationMonitor.Pick("spotify",controller.getPackageName(),
      System.currentTimeMillis(),Categories.priority(c,"spotify"),intent);
  }

  static synchronized boolean hasSession(){return controller!=null;}
  static synchronized String description(Context c){
    if(!Categories.enabled(c,"spotify"))return "Désactivé : coche Spotify dans Notifications.";
    if(NotificationMonitor.instance==null)return "Accès aux notifications absent ou service déconnecté.";
    if(!allowed)return "Accès aux sessions multimédias indisponible. Vérifie l'autorisation Android.";
    if(controller==null)return "Aucune session Spotify locale trouvée. Lance un morceau sur ce téléphone.";
    if(isPlaying(controller))return "Spotify détecté : lecture active. L'icône musicale est disponible.";
    return "Spotify détecté, mais la lecture est en pause ou arrêtée.";
  }
}
