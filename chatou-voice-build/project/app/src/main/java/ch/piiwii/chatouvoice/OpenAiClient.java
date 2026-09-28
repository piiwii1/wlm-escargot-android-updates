package ch.piiwii.chatouvoice;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OpenAiClient {
    public static String ask(String apiKey, String model, List<String[]> history) throws Exception {
        URL url=new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(60000); c.setDoOutput(true);
        c.setRequestProperty("Authorization","Bearer "+apiKey); c.setRequestProperty("Content-Type","application/json");
        JSONObject body=new JSONObject(); body.put("model",model);
        JSONArray msgs=new JSONArray();
        msgs.put(new JSONObject().put("role","system").put("content","Tu es Chatou, assistant vocal personnel. Réponds en français, de façon naturelle et concise, adaptée à une réponse parlée."));
        for(String[] m:history) msgs.put(new JSONObject().put("role",m[0]).put("content",m[1]));
        body.put("messages",msgs);
        try(OutputStream os=c.getOutputStream()){ os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code=c.getResponseCode(); InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        String raw=read(is); if(code<200||code>=300) throw new IOException("OpenAI HTTP "+code+": "+raw);
        JSONObject o=new JSONObject(raw); return o.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
    }
    private static String read(InputStream is)throws Exception{ BufferedReader br=new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8)); StringBuilder sb=new StringBuilder(); String l; while((l=br.readLine())!=null)sb.append(l); return sb.toString(); }
}
