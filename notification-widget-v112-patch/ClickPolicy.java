package ch.piiwii.notificationwidget;
final class ClickPolicy {
  static final String NOTIFICATION="notification",APPLICATION="application",SETTINGS="settings";
  static String choose(boolean hasIntent,boolean hasPackage){
    if(hasIntent)return NOTIFICATION;
    return hasPackage?APPLICATION:SETTINGS;
  }
  static String afterCancelled(boolean hasPackage){return hasPackage?APPLICATION:SETTINGS;}
}