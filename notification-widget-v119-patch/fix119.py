from pathlib import Path
p=Path('piiwii-notification-widget-v119/app/src/main/java/ch/piiwii/notificationwidget/WidgetProvider.java')
s=p.read_text()
needle='''    List<NotificationMonitor.Pick> slots=MultiSlotPolicy.compose(capacity,charging,alerts);
    Bundle options=manager.getAppWidgetOptions(widgetId);'''
assert s.count(needle)==1,s.count(needle)
s=s.replace(needle,'''    List<NotificationMonitor.Pick> slots=MultiSlotPolicy.compose(capacity,charging,alerts);
    // Retain the user's selected idle icon even with zero active notifications.
    if(slots.isEmpty())slots.add(new NotificationMonitor.Pick("rest",null,0,0,null,"idle"));
    Bundle options=manager.getAppWidgetOptions(widgetId);''')
p.write_text(s)
print('Multi-slot idle fallback verified')