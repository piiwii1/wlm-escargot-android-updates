package ch.piiwii.euroscan;
import java.util.*;
public class TicketGrid {
    public final List<Integer> numbers, stars;
    public TicketGrid(List<Integer> numbers,List<Integer> stars){this.numbers=new ArrayList<>(numbers);this.stars=new ArrayList<>(stars);Collections.sort(this.numbers);Collections.sort(this.stars);}
    public String compact(){return join(numbers)+"  ★ "+join(stars);}
    private static String join(List<Integer> v){StringBuilder b=new StringBuilder();for(int i=0;i<v.size();i++){if(i>0)b.append(' ');b.append(String.format(Locale.US,"%02d",v.get(i)));}return b.toString();}
}
