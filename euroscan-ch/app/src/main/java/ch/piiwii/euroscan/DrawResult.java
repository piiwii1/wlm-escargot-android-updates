package ch.piiwii.euroscan;

import java.util.*;

public class DrawResult {
    public String date;
    public final List<Integer> numbers = new ArrayList<>();
    public final List<Integer> stars = new ArrayList<>();
    public final List<Integer> swissWinNumbers = new ArrayList<>();
    public final Map<String, Double> chfPrizes = new LinkedHashMap<>();
    public final Map<Integer, Double> swissWinPrizes = new LinkedHashMap<>();
    public boolean exactSwissPrizes;
    public boolean exactSwissWin;

    public int matchingNumbers(TicketGrid g) {
        int c = 0;
        for (int n : g.numbers) if (numbers.contains(n)) c++;
        return c;
    }

    public int matchingStars(TicketGrid g) {
        int c = 0;
        for (int s : g.stars) if (stars.contains(s)) c++;
        return c;
    }

    public int matchingSwissWin(TicketGrid g) {
        int c = 0;
        for (int n : g.numbers) if (swissWinNumbers.contains(n)) c++;
        return c;
    }

    public Double euroPrizeFor(int n, int s) {
        return chfPrizes.get(n + "+" + s);
    }

    public Double swissWinPrizeFor(int n) {
        return swissWinPrizes.get(n);
    }
}
