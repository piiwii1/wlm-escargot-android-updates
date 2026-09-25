from pathlib import Path
p=Path('euroscan-ch')
# version
f=p/'app/build.gradle'; s=f.read_text(); s=s.replace('versionCode 12','versionCode 13').replace("versionName '1.3.7'","versionName '1.3.8'"); f.write_text(s)
# labels/version text
f=p/'app/src/main/java/ch/piiwii/euroscan/MainActivity.java'; s=f.read_text().replace('1.3.7','1.3.8'); f.write_text(s)
# robust jackpot extraction
f=p/'app/src/main/java/ch/piiwii/euroscan/ResultsRepository.java'; s=f.read_text()
start=s.index('    public long nextJackpotChf() throws Exception {')
end=s.index('    private DrawResult numbers(String date) throws Exception {', start)
new=r'''    public long nextJackpotChf() throws Exception {
        Document d = Jsoup.connect(SWISS).userAgent(UA).timeout(15000).get();
        String text = d.text();
        long amount = extractJackpotAmount(text, "Jackpot prochain tirage");
        if (amount > 0) return amount;
        int p = indexOfIgnoreCase(text, "EuroMillions");
        if (p >= 0) {
            String block = text.substring(p, Math.min(text.length(), p + 2600));
            amount = extractJackpotAmount(block, "Jackpot");
            if (amount > 0) return amount;
        }
        throw new IOException("Jackpot EuroMillions introuvable");
    }

    private long extractJackpotAmount(String text, String label) {
        int pos = indexOfIgnoreCase(text, label);
        if (pos < 0) return -1;
        String block = text.substring(pos, Math.min(text.length(), pos + 180));
        Matcher m = Pattern.compile("CHF\\s*([0-9]{1,3}(?:['’  ]?[0-9]{3}){2,3}|[0-9]{7,12})(?![0-9])", Pattern.CASE_INSENSITIVE).matcher(block);
        while (m.find()) {
            try {
                long v = parseWholeMoney(m.group(1));
                if (v >= 1_000_000L && v <= 1_000_000_000L) return v;
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private long parseWholeMoney(String raw) throws IOException {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) throw new IOException("Montant jackpot invalide");
        try { return Long.parseLong(digits); } catch (NumberFormatException e) { throw new IOException("Montant jackpot invalide", e); }
    }

'''
s=s[:start]+new+s[end:]
f.write_text(s)
print('EuroScan 1.3.8 jackpot parser patched')
