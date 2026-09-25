package ch.piiwii.euroscan;

import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class ResultsRepository {
    private static final String API = "https://api-euromillions.nunofcguerreiro.com";
    private static final String SWISS = "https://www.swisslos.ch/fr/euromillions/informations/r%C3%A9sultats/r%C3%A9sultats-gains.html";
    private static final String UA = "Mozilla/5.0 (Linux; Android 14) EuroScanCH/1.1.0";

    public DrawResult fetch(String date) throws Exception {
        DrawResult r = numbers(date);
        try { enrichFromSwissPage(r); } catch (Exception ignored) {}
        return r;
    }

    public DrawResult latest() throws Exception {
        DrawResult r = numbers(null);
        try { enrichFromSwissPage(r); } catch (Exception ignored) {}
        return r;
    }

    private DrawResult numbers(String date) throws Exception {
        String u = (date == null || date.trim().isEmpty()) ? API + "/results/latest" : API + "/results/date/" + URLEncoder.encode(date, "UTF-8");
        JSONObject o = new JSONObject(get(u));
        DrawResult r = new DrawResult();
        r.date = o.optString("date", date == null ? "" : date);
        JSONArray n = o.getJSONArray("numbers"), s = o.getJSONArray("stars");
        for (int i = 0; i < n.length(); i++) r.numbers.add(n.getInt(i));
        for (int i = 0; i < s.length(); i++) r.stars.add(s.getInt(i));
        Collections.sort(r.numbers); Collections.sort(r.stars);
        return r;
    }

    private void enrichFromSwissPage(DrawResult r) throws Exception {
        Document d = Jsoup.connect(SWISS).userAgent(UA).timeout(15000).get();
        String text = d.text();
        if (!matchesRequestedDraw(text, r)) return;
        extractSwissWin(text, r);
        parsePrizeTables(d, r);
    }

    private boolean matchesRequestedDraw(String text, DrawResult r) {
        int found = 0;
        int p = indexOfIgnoreCase(text, "EuroMillions");
        String head = text.substring(Math.max(0, p), Math.min(text.length(), Math.max(0, p) + 1200));
        for (int n : r.numbers) if (Pattern.compile("(?<!\\d)" + n + "(?!\\d)").matcher(head).find()) found++;
        for (int s : r.stars) if (Pattern.compile("(?<!\\d)" + s + "(?!\\d)").matcher(head).find()) found++;
        return found >= 6;
    }

    private void extractSwissWin(String text, DrawResult r) {
        int p = indexOfIgnoreCase(text, "2nd Chance");
        if (p < 0) p = indexOfIgnoreCase(text, "2. Chance");
        if (p < 0) return;
        String block = text.substring(p, Math.min(text.length(), p + 500));
        Matcher m = Pattern.compile("(?<!\\d)([1-9]|[1-4]\\d|50)(?!\\d)").matcher(block);
        while (m.find() && r.swissWinNumbers.size() < 5) {
            int v = Integer.parseInt(m.group(1));
            if (!r.swissWinNumbers.contains(v)) r.swissWinNumbers.add(v);
        }
        Collections.sort(r.swissWinNumbers);
    }

    private void parsePrizeTables(Document d, DrawResult r) {
        for (Element table : d.select("table")) {
            List<Double> vals = new ArrayList<>();
            for (Element tr : table.select("tr")) {
                Double money = lastMoney(tr.text());
                if (money != null) vals.add(money);
            }
            if (!r.exactSwissPrizes && vals.size() >= 13) {
                String[] keys = {"5+2","5+1","5+0","4+2","4+1","3+2","4+0","2+2","3+1","3+0","1+2","2+1","2+0"};
                for (int i = 0; i < 13; i++) r.chfPrizes.put(keys[i], vals.get(i));
                r.exactSwissPrizes = true;
            } else if (!r.exactSwissWin && vals.size() >= 3 && vals.size() <= 6) {
                r.swissWinPrizes.put(5, vals.get(0));
                r.swissWinPrizes.put(4, vals.get(1));
                r.swissWinPrizes.put(3, vals.get(2));
                r.exactSwissWin = true;
            }
        }
        if (!r.exactSwissPrizes || !r.exactSwissWin) parsePrizeTextFallback(d.text(), r);
    }

    private void parsePrizeTextFallback(String text, DrawResult r) {
        int first = indexOfIgnoreCase(text, "Nombre de numéros exacts");
        if (first < 0) return;
        List<Double> euro = moneySequence(text.substring(first, Math.min(text.length(), first + 2200)));
        if (!r.exactSwissPrizes && euro.size() >= 13) {
            String[] keys = {"5+2","5+1","5+0","4+2","4+1","3+2","4+0","2+2","3+1","3+0","1+2","2+1","2+0"};
            for (int i = 0; i < 13; i++) r.chfPrizes.put(keys[i], euro.get(i));
            r.exactSwissPrizes = true;
        }
        int second = indexOfIgnoreCase(text, "Nombre de numéros exacts", first + 30);
        if (second > 0) {
            List<Double> sw = moneySequence(text.substring(second, Math.min(text.length(), second + 850)));
            if (!r.exactSwissWin && sw.size() >= 3) {
                r.swissWinPrizes.put(5, sw.get(0)); r.swissWinPrizes.put(4, sw.get(1)); r.swissWinPrizes.put(3, sw.get(2)); r.exactSwissWin = true;
            }
        }
    }

    private List<Double> moneySequence(String block) {
        List<Double> vals = new ArrayList<>();
        Matcher m = Pattern.compile("(?<!\\d)(\\d{1,3}(?:['’  ]\\d{3})*(?:[.,]\\d{2}))(?!\\d)").matcher(block);
        while (m.find() && vals.size() < 20) try { vals.add(parseMoney(m.group(1))); } catch (Exception ignored) {}
        return vals;
    }

    private Double lastMoney(String row) {
        Matcher m = Pattern.compile("(\\d{1,3}(?:['’  ]\\d{3})*(?:[.,]\\d{2}))").matcher(row);
        Double v = null;
        while (m.find()) try { v = parseMoney(m.group(1)); } catch (Exception ignored) {}
        return v;
    }

    private double parseMoney(String s) {
        return Double.parseDouble(s.replace("'", "").replace("’", "").replace(" ", "").replace(" ", "").replace(',', '.'));
    }

    private int indexOfIgnoreCase(String text, String needle) { return indexOfIgnoreCase(text, needle, 0); }
    private int indexOfIgnoreCase(String text, String needle, int from) { return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT), Math.max(0, from)); }

    private String get(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setRequestProperty("User-Agent", UA);
        int code = c.getResponseCode(); InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder b = new StringBuilder();
        try (BufferedReader rr = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { String l; while ((l = rr.readLine()) != null) b.append(l); }
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
        return b.toString();
    }
}
