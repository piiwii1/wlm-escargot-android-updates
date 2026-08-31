package ch.piiwii.modeconvoi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

public final class VolkswagenIconPack {
    public static final class Item {
        public final String id;
        public final String label;
        private final String base64;
        Item(String id,String label,String base64){this.id=id;this.label=label;this.base64=base64;}
        public Bitmap bitmap(){
            try{byte[] raw=Base64.decode(base64,Base64.DEFAULT);return BitmapFactory.decodeByteArray(raw,0,raw.length);}catch(Throwable ignored){return null;}
        }
        public String base64(){return base64;}
    }

    private static final Item[] ITEMS={
        new Item("vw:beetle","Coccinelle",VwIcon01.DATA),
        new Item("vw:t1","Combi T1",VwIcon02.DATA),
        new Item("vw:golf1","Golf 1 GTI",VwIcon03.DATA),
        new Item("vw:golf2","Golf 2 GTI",VwIcon04.DATA),
        new Item("vw:golf3","Golf 3",VwIcon05.DATA),
        new Item("vw:golf4","Golf 4",VwIcon06.DATA),
        new Item("vw:polo6n2","Polo 6N2 GTI",VwIcon07.DATA),
        new Item("vw:passatb5","Passat B5 Variant",VwIcon08.DATA),
        new Item("vw:jetta2","Jetta 2",VwIcon09.DATA),
        new Item("vw:newbeetle","New Beetle",VwIcon10.DATA)
    };

    private VolkswagenIconPack(){}
    public static Item[] items(){return ITEMS.clone();}
    public static Item find(String id){if(id==null)return null;for(Item item:ITEMS)if(item.id.equals(id))return item;return null;}
    public static boolean isVolkswagen(String id){return find(id)!=null;}
    public static Bitmap bitmapFor(String id){Item item=find(id);return item==null?null:item.bitmap();}
    public static String base64For(String id){Item item=find(id);return item==null?"":item.base64();}
}
