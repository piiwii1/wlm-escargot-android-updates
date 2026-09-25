package ch.piiwii.euroscan;
import java.util.*;
public class DrawResult {
    public String date; public List<Integer> numbers=new ArrayList<>(),stars=new ArrayList<>(); public final Map<String,Double> chfPrizes=new HashMap<>(); public boolean exactSwissPrizes;
    public int matchingNumbers(TicketGrid g){int c=0;for(int n:g.numbers)if(numbers.contains(n))c++;return c;}
    public int matchingStars(TicketGrid g){int c=0;for(int s:g.stars)if(stars.contains(s))c++;return c;}
    public Double prizeFor(int n,int s){Double v=chfPrizes.get(n+"+"+s);return v!=null?v:SwissPrizeFallback.average(n,s);}
}
