package ch.piiwii.modeconvoi;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public final class VolkswagenIconPack {
    private static final int CELL=192;
    private static final int COLS=5;
    private static Bitmap spriteCache;
    private static Bitmap spriteMoreCache;
    private static final Bitmap[] bitmapCache=new Bitmap[20];

    public static final class Item {
        public final String id;
        public final String label;
        private final int index;
        Item(String id,String label,int index){this.id=id;this.label=label;this.index=index;}
        public Bitmap bitmap(Resources resources){return bitmapAt(resources,index);}
    }

    private static final Item[] ITEMS={
        new Item("vw:beetle","Coccinelle",0),
        new Item("vw:t1","Combi T1",1),
        new Item("vw:golf1","Golf 1 GTI",2),
        new Item("vw:golf2","Golf 2 GTI",3),
        new Item("vw:golf3","Golf 3",4),
        new Item("vw:golf4","Golf 4",5),
        new Item("vw:polo6n2","Polo 6N2 GTI",6),
        new Item("vw:passatb5","Passat B5 Variant",7),
        new Item("vw:jetta2","Jetta 2",8),
        new Item("vw:newbeetle","New Beetle",9),
        new Item("vw:karmannghia","Karmann Ghia",10),
        new Item("vw:type3fastback","Type 3 Fastback",11),
        new Item("vw:thing181","Type 181",12),
        new Item("vw:t2","Combi T2",13),
        new Item("vw:t3","Combi T3",14),
        new Item("vw:scirocco1","Scirocco 1",15),
        new Item("vw:scirocco2","Scirocco 2",16),
        new Item("vw:corrado","Corrado VR6",17),
        new Item("vw:lupogti","Lupo GTI",18),
        new Item("vw:polog40","Polo G40",19)
    };

    private VolkswagenIconPack(){}
    public static Item[] items(){return ITEMS.clone();}
    public static Item find(String id){if(id==null)return null;for(Item item:ITEMS)if(item.id.equals(id))return item;return null;}
    public static boolean isVolkswagen(String id){return find(id)!=null;}
    public static int indexOf(String id){Item item=find(id);return item==null?-1:item.index;}
    public static Bitmap bitmapFor(Resources resources,String id){Item item=find(id);return item==null?null:bitmapAt(resources,item.index);}

    private static synchronized Bitmap bitmapAt(Resources resources,int index){
        if(index<0||index>=bitmapCache.length||resources==null)return null;
        if(bitmapCache[index]!=null&&!bitmapCache[index].isRecycled())return bitmapCache[index];
        Bitmap sprite; int localIndex;
        if(index<10){
            if(spriteCache==null||spriteCache.isRecycled())spriteCache=BitmapFactory.decodeResource(resources,R.drawable.vw_sprite_192);
            sprite=spriteCache;localIndex=index;
        }else{
            if(spriteMoreCache==null||spriteMoreCache.isRecycled())spriteMoreCache=BitmapFactory.decodeResource(resources,R.drawable.vw_sprite_more_192);
            sprite=spriteMoreCache;localIndex=index-10;
        }
        if(sprite==null)return null;
        int x=(localIndex%COLS)*CELL,y=(localIndex/COLS)*CELL;
        if(x+CELL>sprite.getWidth()||y+CELL>sprite.getHeight())return null;
        bitmapCache[index]=Bitmap.createBitmap(sprite,x,y,CELL,CELL);
        return bitmapCache[index];
    }
}
