from pathlib import Path
p=Path("piiwii-notification-widget-v118/app/src/main/java/ch/piiwii/notificationwidget/WidgetProvider.java")
s=p.read_text()
needle='''        id=powerCategory(c,battery);
        else if(battery.percent>=0&&battery.percent<=15&&Categories.enabled(c,"low"))id="low";'''
replacement='''        id=powerCategory(c,battery);
        if(id==null&&battery.percent>=0&&battery.percent<=15&&Categories.enabled(c,"low"))id="low";'''
assert s.count(needle)==1,s.count(needle)
p.write_text(s.replace(needle,replacement))
print("fixed ephemeral fallback syntax")