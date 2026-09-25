from pathlib import Path

p=Path('euroscan-ch')

def repl(s, old, new, label):
    if old not in s:
        raise SystemExit('missing patch anchor: '+label)
    return s.replace(old,new,1)

# Version
f=p/'app/build.gradle'
s=f.read_text()
s=repl(s,'versionCode 8','versionCode 9','versionCode')
s=repl(s,"versionName '1.3.3'","versionName '1.3.4'",'versionName')
f.write_text(s)

# Results source + next jackpot
f=p/'app/src/main/java/ch/piiwii/euroscan/ResultsRepository.java'
s=f.read_text().replace('EuroScanCH/1.3.0','EuroScanCH/1.3.4')
anchor='    private DrawResult numbers(String date) throws Exception {'
insert=r'''    public long nextJackpotChf() throws Exception {
        Document d = Jsoup.connect(SWISS).userAgent(UA).timeout(15000).get();
        String text = d.text();
        Matcher m = Pattern.compile("Jackpot\\s+prochain\\s+tirage\\s*:?\\s*CHF\\s*([0-9][0-9'’  ]*)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return parseWholeMoney(m.group(1));
        int p = indexOfIgnoreCase(text, "EuroMillions");
        if (p >= 0) {
            String block = text.substring(p, Math.min(text.length(), p + 2400));
            Matcher alt = Pattern.compile("Jackpot\\s+CHF\\s*([0-9][0-9'’  ]*)", Pattern.CASE_INSENSITIVE).matcher(block);
            if (alt.find()) return parseWholeMoney(alt.group(1));
        }
        throw new IOException("Jackpot EuroMillions introuvable");
    }

    private long parseWholeMoney(String raw) throws IOException {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) throw new IOException("Montant jackpot invalide");
        try { return Long.parseLong(digits); } catch (NumberFormatException e) { throw new IOException("Montant jackpot invalide", e); }
    }

'''
s=repl(s,anchor,insert+anchor,'ResultsRepository jackpot')
f.write_text(s)

# History: keep gains, losses, pending + cumulative total
f=p/'app/src/main/java/ch/piiwii/euroscan/HistoryStore.java'
f.write_text('''package ch.piiwii.euroscan;\nimport android.content.*;import org.json.*;\npublic class HistoryStore {\n    private final SharedPreferences p; public HistoryStore(Context c){p=c.getSharedPreferences("history",Context.MODE_PRIVATE);}\n    public void add(String date,String grids,String result,double total,boolean exact,boolean won){try{JSONArray a=new JSONArray(p.getString("items","[]"));JSONObject o=new JSONObject();o.put("date",date);o.put("grids",grids);o.put("result",result);o.put("total",total);o.put("exact",exact);o.put("won",won);o.put("time",System.currentTimeMillis());JSONArray n=new JSONArray();n.put(o);for(int i=0;i<a.length()&&i<99;i++)n.put(a.get(i));p.edit().putString("items",n.toString()).apply();}catch(Exception ignored){}}\n    public JSONArray all(){try{return new JSONArray(p.getString("items","[]"));}catch(Exception e){return new JSONArray();}}\n    public double totalConfirmedGains(){double t=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)t+=o.optDouble("total",0);}return t;}\n    public int winCount(){int c=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&(o.optBoolean("won",false)||o.optDouble("total",0)>0))c++;}return c;}\n    public int lossCount(){int c=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optBoolean("exact",false)&&!o.optBoolean("won",false)&&o.optDouble("total",0)<=0)c++;}return c;}\n    public int pendingCount(){int c=0;JSONArray a=all();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&!o.optBoolean("exact",false)&&!o.optBoolean("won",false)&&o.optDouble("total",0)<=0)c++;}return c;}\n    public void clear(){p.edit().remove("items").apply();}\n}\n''')

# Main UI
f=p/'app/src/main/java/ch/piiwii/euroscan/MainActivity.java'
s=f.read_text()
s=s.replace('EUROSCAN CH  ·  v1.3.2','EUROSCAN CH  ·  v1.3.4')
s=s.replace('EuroScan CH 1.3.2','EuroScan CH 1.3.4')
s=s.replace('appIcon.setImageResource(R.mipmap.ic_launcher);','appIcon.setImageResource(R.drawable.euroscan_icon);')

old='''        nextRow.addView(countdown);\n        next.addView(nextRow);\n        root.addView(next, lp(-1, -2, 0, 0, 0, 12));'''
new='''        nextRow.addView(countdown);\n        next.addView(nextRow);\n        next.addView(gap(12));\n        TextView jackpot = txt("Jackpot estimé : chargement…", 22, GOLD, true);\n        next.addView(jackpot);\n        next.addView(gap(2));\n        next.addView(txt("Montant annoncé pour le prochain tirage", 11, MUTED, false));\n        root.addView(next, lp(-1, -2, 0, 0, 0, 12));\n        loadNextJackpot(jackpot);\n\n        LinearLayout gainSummary = box(CARD, 20, 16);\n        gainSummary.addView(txt("Mes gains", 16, TEXT, true));\n        gainSummary.addView(gap(4));\n        gainSummary.addView(txt("CHF " + money(history.totalConfirmedGains()), 30, GREEN, true));\n        gainSummary.addView(txt(history.winCount() + " gagnant" + (history.winCount()>1?"s":"") + " · " + history.lossCount() + " perdu" + (history.lossCount()>1?"s":"") + (history.pendingCount()>0?" · " + history.pendingCount() + " à valider":""), 12, MUTED, false));\n        root.addView(gainSummary, lp(-1, -2, 0, 0, 0, 12));'''
s=repl(s,old,new,'home jackpot/stats')

anchor='    private void startLiveScanner() {'
insert='''    private void loadNextJackpot(TextView out) {\n        worker.execute(() -> {\n            try {\n                long amount = repo.nextJackpotChf();\n                runOnUiThread(() -> out.setText("Jackpot estimé : CHF " + wholeMoney(amount)));\n            } catch (Exception e) {\n                runOnUiThread(() -> out.setText("Jackpot estimé : indisponible"));\n            }\n        });\n    }\n\n    private String wholeMoney(long v) {\n        String x = String.format(Locale.US, "%,d", v);\n        return x.replace(',', '\\'');\n    }\n\n    private String money(double v) {\n        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols(Locale.US);\n        DecimalFormat df = new DecimalFormat("#,##0.00", sym);\n        return df.format(v).replace(',', '\\'');\n    }\n\n'''
s=repl(s,anchor,insert+anchor,'jackpot loader')

s=repl(s,'        boolean allExact = true;\n        StringBuilder hist = new StringBuilder();','        boolean allExact = true;\n        boolean anyWin = false;\n        StringBuilder hist = new StringBuilder();','anyWin init')
s=repl(s,'            boolean euroWinning = isEuroWinningTier(n, s);\n            boolean swWinning = swm >= 3;','            boolean euroWinning = isEuroWinningTier(n, s);\n            boolean swWinning = swm >= 3;\n            if (euroWinning || swWinning) anyWin = true;','anyWin track')

old='''            c.addView(txt("Grille " + (i + 1), 17, TEXT, true));\n            c.addView(txt(g.compact(), 15, PURPLE, true));\n            c.addView(gap(8));\n            c.addView(txt("EuroMillions : " + n + " numéro" + (n > 1 ? "s" : "") + " + " + s + " étoile" + (s > 1 ? "s" : ""), 14, MUTED, false));'''
new='''            c.addView(txt("Grille " + (i + 1), 17, TEXT, true));\n            c.addView(gap(8));\n            c.addView(matchedGridRow(g, d));\n            c.addView(gap(9));\n            TextView hits = txt(matchedText(g, d), 16, (n+s)>0 ? GREEN : MUTED, true);\n            c.addView(hits);\n            c.addView(gap(5));\n            c.addView(txt("EuroMillions : " + n + " numéro" + (n > 1 ? "s" : "") + " + " + s + " étoile" + (s > 1 ? "s" : ""), 14, MUTED, false));'''
s=repl(s,old,new,'matched numbers UI')
s=repl(s,'history.add(d.date, all.toString(), hist.toString(), total, allExact);','history.add(d.date, all.toString(), hist.toString(), total, allExact, anyWin);','history signature')

start=s.index('    private void history() {')
end=s.index('    private String drawLine(DrawResult d)',start)
history=r'''    private void history() {
        page("Historique", "Gains, pertes et contrôles à valider enregistrés sur ce téléphone.");
        JSONArray a = history.all();

        LinearLayout summary = box(CARD, 20, 16);
        summary.addView(txt("Total de mes gains confirmés", 14, MUTED, true));
        summary.addView(gap(4));
        summary.addView(txt("CHF " + money(history.totalConfirmedGains()), 32, GREEN, true));
        summary.addView(gap(5));
        summary.addView(txt(history.winCount() + " gagnant" + (history.winCount()>1?"s":"") + " · " + history.lossCount() + " perdu" + (history.lossCount()>1?"s":"") + (history.pendingCount()>0?" · " + history.pendingCount() + " à valider":""), 12, MUTED, false));
        root.addView(summary, lp(-1, -2, 0, 0, 0, 14));

        if (a.length() == 0) root.addView(txt("Aucun ticket contrôlé pour l'instant.", 15, MUTED, false));
        for (int i = 0; i < a.length(); i++) try {
            JSONObject o = a.getJSONObject(i);
            double amount = o.optDouble("total",0);
            boolean won = o.optBoolean("won", amount > 0);
            boolean ex = o.optBoolean("exact", false);
            String status = won ? (ex ? "GAGNÉ" : "GAIN À VALIDER") : (ex ? "PERDU" : "À VALIDER");
            int statusColor = won ? GREEN : (ex ? RED : ORANGE);
            LinearLayout c = card();
            LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(txt(o.optString("date"), 17, TEXT, true), new LinearLayout.LayoutParams(0,-2,1));
            TextView badge=txt(status,11,statusColor,true); badge.setPadding(dp(9),dp(5),dp(9),dp(5)); badge.setBackground(stroke(statusColor,CARD_2,14)); top.addView(badge);
            c.addView(top);
            c.addView(gap(7));
            c.addView(txt(o.optString("grids"), 12, MUTED, false));
            c.addView(gap(5));
            c.addView(txt(o.optString("result"), 12, MUTED, false));
            c.addView(gap(8));
            c.addView(txt((won?"Gain confirmé : ":"Total confirmé : ") + "CHF " + money(amount) + (ex ? "" : " · contrôle partiel"), 16, won ? GREEN : (ex ? MUTED : ORANGE), true));
            root.addView(c, lp(-1, -2, 0, 0, 0, 10));
        } catch (Exception ignored) {}

        Button clear = secondary("Effacer l'historique");
        clear.setTextColor(RED);
        Button h = btn("Accueil", GOLD, Color.rgb(18, 23, 32));
        root.addView(clear, lp(-1, dp(50), 0, 12, 0, 10));
        root.addView(h, lp(-1, dp(50), 0, 0, 0, 0));
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Effacer l'historique ?").setNegativeButton("Annuler", null).setPositiveButton("Effacer", (d, w) -> { history.clear(); history(); }).show());
        h.setOnClickListener(v -> home());
    }

    private LinearLayout matchedGridRow(TicketGrid g, DrawResult d) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        for (int i=0;i<g.numbers.size();i++) { int n=g.numbers.get(i); row.addView(matchChip(n,d.numbers.contains(n),false),new LinearLayout.LayoutParams(0,dp(42),1)); if(i<g.numbers.size()-1)row.addView(gapH(5)); }
        row.addView(gapH(8));
        for (int i=0;i<g.stars.size();i++) { int n=g.stars.get(i); row.addView(matchChip(n,d.stars.contains(n),true),new LinearLayout.LayoutParams(dp(42),dp(42))); if(i<g.stars.size()-1)row.addView(gapH(5)); }
        return row;
    }

    private TextView matchChip(int n, boolean matched, boolean star) {
        int bg = matched ? (star ? GOLD : GREEN) : CARD_2;
        int fg = matched ? NAVY : MUTED;
        TextView v=txt(String.format(Locale.US,"%02d",n),15,fg,true); v.setGravity(Gravity.CENTER); v.setBackground(round(bg,15)); return v;
    }

    private String matchedText(TicketGrid g, DrawResult d) {
        List<String> nums=new ArrayList<>(), stars=new ArrayList<>();
        for(int n:g.numbers) if(d.numbers.contains(n)) nums.add(String.format(Locale.US,"%02d",n));
        for(int n:g.stars) if(d.stars.contains(n)) stars.add(String.format(Locale.US,"%02d",n));
        if(nums.isEmpty()&&stars.isEmpty()) return "Aucun numéro juste";
        StringBuilder b=new StringBuilder("✓ BONS : ");
        if(!nums.isEmpty()) b.append(android.text.TextUtils.join(" · ",nums));
        if(!stars.isEmpty()){ if(!nums.isEmpty())b.append("   "); b.append("★ ").append(android.text.TextUtils.join(" · ",stars)); }
        return b.toString();
    }

'''
s=s[:start]+history+s[end:]
f.write_text(s)
print('EuroScan CH 1.3.4 patched')
