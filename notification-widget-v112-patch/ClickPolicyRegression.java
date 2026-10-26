package ch.piiwii.notificationwidget;
public final class ClickPolicyRegression {
  static int n=0;
  static void check(String actual,String expected){n++;if(!expected.equals(actual))throw new AssertionError("expected "+expected+", got "+actual);}
  public static void main(String[] args){
    check(ClickPolicy.choose(true,true),"notification");
    check(ClickPolicy.choose(true,false),"notification");
    check(ClickPolicy.choose(false,true),"application");
    check(ClickPolicy.choose(false,false),"settings");
    check(ClickPolicy.afterCancelled(true),"application");
    check(ClickPolicy.afterCancelled(false),"settings");
    System.out.println("Widget click regression: "+n+"/"+n+" passed");
  }
}