package ch.piiwii.waarchive;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** WA Archive 0.4.5: vrais médias + correction largeur des bulles. */
public class MainActivityV045 extends MainActivityV043 {
    private static final String DB_NAME = "archive.db";
    private static final String BACKUP_DB = "archive_before_045.db";
    private static final String PREFS = "archive_prefs";
    private static final String MEDIA_DIR = "wa_media";
    private static final String MEDIA_TMP = "wa_media_tmp";

    private static final int TEXT = Color.rgb(17,27,33);
    private static final int META = Color.rgb(102,119,129);
    private static final int GREEN = Color.rgb(0,128,105);
    private static final int SENT = Color.rgb(217,253,211);
    private static final int RECEIVED = Color.WHITE;

    private static final Pattern FILE_REF = Pattern.compile(
            "(?i)([^<>\\r\\n]*?\\.(?:jpg|jpeg|png|webp|gif|heic|opus|ogg|m4a|mp3|aac|amr|wav))");

    private SQLiteDatabase renderDb;
    private Cursor renderCursor;
    private ArchiveAdapter renderAdapter;
    private MediaPlayer player;
    private PlayIcon currentPlayIcon;
    private String currentAudio;
    private boolean extracting;

    private final LruCache<String, Bitmap> bitmaps = new LruCache<String, Bitmap>(16 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };

    private int dp(float n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float sp, int color) {
        TextView v = new TextView(this);
        v.setText(value == null ? "" : value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        return v;
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable bubbleBg(boolean sent) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(sent ? SENT : RECEIVED);
        float r = dp(8), s = dp(2.5f);
        if (sent) d.setCornerRadii(new float[]{r,r,s,s,r,r,r,r});
        else d.setCornerRadii(new float[]{s,s,r,r,r,r,r,r});
        return d;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        installRenderer();
        recoverMediaIfPossible();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == 4301 || requestCode == 1001) && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            tryPersist(uri, data.getFlags());
            importWithMedia(uri, displayName(uri));
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void tryPersist(Uri uri, int flags) {
        try {
            int read = flags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri,
                    read == 0 ? Intent.FLAG_GRANT_READ_URI_PERMISSION : read);
        } catch (Exception ignored) {}
    }

    private String displayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                String n = c.getString(0);
                if (n != null && !n.trim().isEmpty()) return n;
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return "Export WhatsApp";
    }

    private void importWithMedia(final Uri uri, final String name) {
        if (extracting) return;
        extracting = true;
        Toast.makeText(this, "Analyse des messages puis extraction des photos et vocaux…", Toast.LENGTH_LONG).show();

        new Thread(new Runnable() {
            @Override public void run() {
                File backup = getDatabasePath(BACKUP_DB);
                boolean hadOld = false;
                try {
                    closeBaseDb();
                    closeRenderer();
                    File db = getDatabasePath(DB_NAME);
                    if (backup.exists()) backup.delete();
                    if (db.exists() && db.length() > 0) {
                        copyFile(db, backup);
                        hadOld = true;
                    }

                    long parsed = invokeParser(uri);
                    long actual = countMessages();
                    if (parsed <= 0 || actual <= 0) throw new IOException("Aucun message n’a été importé.");

                    MediaStats media = extractMedia(uri);
                    invokeOwnerSelection();
                    rememberUri(uri, name);
                    if (backup.exists()) backup.delete();

                    final long count = actual;
                    final MediaStats stats = media;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            extracting = false;
                            Toast.makeText(MainActivityV045.this,
                                    count + " messages • " + stats.images + " images • " + stats.audio + " vocaux",
                                    Toast.LENGTH_LONG).show();
                            recreate();
                        }
                    });
                } catch (final Exception e) {
                    try {
                        deleteDatabase(DB_NAME);
                        if (hadOld && backup.exists()) copyFile(backup, getDatabasePath(DB_NAME));
                    } catch (Exception ignored) {}
                    if (backup.exists()) backup.delete();
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            extracting = false;
                            new AlertDialog.Builder(MainActivityV045.this)
                                    .setTitle("Import impossible")
                                    .setMessage(message(e))
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override public void onClick(DialogInterface dialog, int which) { recreate(); }
                                    }).show();
                        }
                    });
                }
            }
        }).start();
    }

    private long invokeParser(Uri uri) throws Exception {
        try {
            Method m = MainActivityV042.class.getDeclaredMethod("robustImport", Uri.class);
            m.setAccessible(true);
            Object r = m.invoke(this, uri);
            return r instanceof Number ? ((Number)r).longValue() : 0;
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            if (c instanceof Exception) throw (Exception)c;
            throw new IOException(c == null ? "Erreur d’analyse." : c.toString());
        }
    }

    private void invokeOwnerSelection() {
        try {
            Method m = MainActivityV042.class.getDeclaredMethod("selectOwnerAutomatically");
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception ignored) {}
    }

    private String message(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }

    private void rememberUri(Uri uri, String name) {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String u = uri.toString();
        String old0 = p.getString("recent_uri_0", "");
        String old0n = p.getString("recent_name_0", "");
        String old1 = p.getString("recent_uri_1", "");
        String old1n = p.getString("recent_name_1", "");
        p.edit()
                .putString("recent_uri_2", old1).putString("recent_name_2", old1n)
                .putString("recent_uri_1", old0).putString("recent_name_1", old0n)
                .putString("recent_uri_0", u).putString("recent_name_0", name)
                .putString("last_archive_uri", u).putInt("last_pos", -1).apply();
    }

    private long countMessages() {
        File f = getDatabasePath(DB_NAME);
        if (!f.exists() || f.length() == 0) return 0;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("SELECT COUNT(*) FROM messages", null);
            return c.moveToFirst() ? c.getLong(0) : 0;
        } catch (Exception e) {
            return 0;
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
    }

    private void closeBaseDb() {
        try {
            Field c = MainActivity.class.getDeclaredField("messageCursor");
            c.setAccessible(true);
            Object value = c.get(this);
            if (value instanceof Cursor) ((Cursor)value).close();
            c.set(this, null);
        } catch (Exception ignored) {}
        try {
            Field d = MainActivity.class.getDeclaredField("db");
            d.setAccessible(true);
            Object value = d.get(this);
            if (value instanceof SQLiteDatabase && ((SQLiteDatabase)value).isOpen()) ((SQLiteDatabase)value).close();
            d.set(this, null);
        } catch (Exception ignored) {}
    }

    private void copyFile(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[128 * 1024];
        try {
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf,0,n);
        } finally {
            try { in.close(); } catch (Exception ignored) {}
            try { out.close(); } catch (Exception ignored) {}
        }
    }

    private void recoverMediaIfPossible() {
        if (countMessages() <= 0 || mediaCount() > 0 || extracting) return;
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString("last_archive_uri",
                getSharedPreferences(PREFS, MODE_PRIVATE).getString("recent_uri_0", ""));
        if (raw == null || raw.isEmpty()) return;
        final Uri uri = Uri.parse(raw);
        extracting = true;
        Toast.makeText(this, "Récupération des photos et vocaux…", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final MediaStats stats = extractMedia(uri);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            extracting = false;
                            Toast.makeText(MainActivityV045.this,
                                    stats.images + " images et " + stats.audio + " vocaux récupérés",
                                    Toast.LENGTH_LONG).show();
                            installRenderer();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            extracting = false;
                            Toast.makeText(MainActivityV045.this,
                                    "Réimporte le même ZIP une fois pour activer les médias.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private int mediaCount() {
        File dir = new File(getFilesDir(), MEDIA_DIR);
        File[] f = dir.listFiles();
        return f == null ? 0 : f.length;
    }

    private MediaStats extractMedia(Uri uri) throws Exception {
        InputStream probe = getContentResolver().openInputStream(uri);
        if (probe == null) throw new IOException("Archive inaccessible.");
        BufferedInputStream check = new BufferedInputStream(probe, 4096);
        int a = check.read(), b = check.read();
        check.close();
        if (a != 'P' || b != 'K') return new MediaStats();

        File tmp = new File(getFilesDir(), MEDIA_TMP);
        deleteTree(tmp);
        if (!tmp.mkdirs() && !tmp.isDirectory()) throw new IOException("Impossible de créer le cache média.");

        MediaStats stats = new MediaStats();
        InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) throw new IOException("ZIP inaccessible.");
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw, 128 * 1024));
        byte[] buf = new byte[128 * 1024];
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String base = new File(entry.getName()).getName();
                Kind kind = kind(base);
                if (kind == Kind.NONE) continue;
                File outFile = new File(tmp, key(base));
                FileOutputStream out = new FileOutputStream(outFile);
                long size = 0;
                try {
                    int n;
                    while ((n = zip.read(buf)) != -1) {
                        size += n;
                        if (size > 250L * 1024L * 1024L) throw new IOException("Média trop volumineux : " + base);
                        out.write(buf,0,n);
                    }
                } finally { out.close(); }
                if (kind == Kind.IMAGE) stats.images++; else stats.audio++;
            }
        } finally { try { zip.close(); } catch (Exception ignored) {} }

        File live = new File(getFilesDir(), MEDIA_DIR);
        deleteTree(live);
        if (!tmp.renameTo(live)) {
            if (!live.mkdirs() && !live.isDirectory()) throw new IOException("Impossible d’activer les médias.");
            File[] files = tmp.listFiles();
            if (files != null) for (File f : files) copyFile(f, new File(live,f.getName()));
            deleteTree(tmp);
        }
        bitmaps.evictAll();
        return stats;
    }

    private void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteTree(c);
        }
        try { f.delete(); } catch (Exception ignored) {}
    }

    private enum Kind { NONE, IMAGE, AUDIO }

    private Kind kind(String name) {
        String s = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png") || s.endsWith(".webp") || s.endsWith(".gif") || s.endsWith(".heic")) return Kind.IMAGE;
        if (s.endsWith(".opus") || s.endsWith(".ogg") || s.endsWith(".m4a") || s.endsWith(".mp3") || s.endsWith(".aac") || s.endsWith(".amr") || s.endsWith(".wav")) return Kind.AUDIO;
        return Kind.NONE;
    }

    private String attachment(String body) {
        if (body == null) return null;
        Matcher m = FILE_REF.matcher(body.replace("\u200e","").replace("\u200f",""));
        String found = null;
        while (m.find()) found = m.group(1).trim();
        if (found == null) return null;
        return new File(found).getName().trim();
    }

    private String key(String base) {
        String lower = base == null ? "" : base.toLowerCase(Locale.ROOT).trim();
        String safe = lower.replaceAll("[^a-z0-9._-]", "_");
        if (safe.length() > 90) safe = safe.substring(safe.length()-90);
        return Integer.toHexString(lower.hashCode()) + "_" + safe;
    }

    private File mediaFile(String body) {
        String name = attachment(body);
        if (name == null) return null;
        File f = new File(new File(getFilesDir(),MEDIA_DIR), key(name));
        return f.exists() && f.length() > 0 ? f : null;
    }

    private boolean mine(String sender) {
        String owner = getSharedPreferences(PREFS,MODE_PRIVATE).getString("owner","");
        return sender != null && owner != null && !owner.isEmpty() && sender.equals(owner);
    }

    private void installRenderer() {
        closeRenderer();
        try {
            Field f = MainActivity.class.getDeclaredField("messageList");
            f.setAccessible(true);
            Object value = f.get(this);
            if (!(value instanceof ListView)) return;
            File dbFile = getDatabasePath(DB_NAME);
            if (!dbFile.exists() || dbFile.length() == 0) return;
            renderDb = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
            renderCursor = renderDb.rawQuery("SELECT id AS _id,mdate,mtime,sender,body,system,show_date FROM messages ORDER BY id",null);
            renderAdapter = new ArchiveAdapter(renderCursor);
            ListView list = (ListView)value;
            list.setAdapter(renderAdapter);
            int saved = getSharedPreferences(PREFS,MODE_PRIVATE).getInt("last_pos",-1);
            int target = saved >= 0 && saved < renderAdapter.getCount() ? saved : Math.max(0,renderAdapter.getCount()-1);
            list.setSelection(target);
        } catch (Exception ignored) {}
    }

    private void closeRenderer() {
        if (renderCursor != null) { try { renderCursor.close(); } catch (Exception ignored) {} renderCursor=null; }
        if (renderDb != null) { try { renderDb.close(); } catch (Exception ignored) {} renderDb=null; }
    }

    private View rowView(String date,String time,String sender,String body,boolean system,boolean showDate) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);

        if (showDate) {
            LinearLayout dr = new LinearLayout(this);
            dr.setGravity(Gravity.CENTER);
            TextView chip = text(date,11.3f,Color.rgb(84,101,111));
            chip.setPadding(dp(9),dp(5),dp(9),dp(5));
            chip.setBackground(bg(Color.rgb(248,252,253),8));
            dr.addView(chip);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            dl.topMargin=dp(4); dl.bottomMargin=dp(6);
            outer.addView(dr,dl);
        }

        if (system) {
            LinearLayout sr = new LinearLayout(this);
            sr.setGravity(Gravity.CENTER);
            TextView s = text(body,11.5f,Color.rgb(84,101,111));
            s.setGravity(Gravity.CENTER);
            s.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels*0.86f));
            s.setPadding(dp(9),dp(6),dp(9),dp(6));
            s.setBackground(bg(Color.rgb(255,244,199),8));
            sr.addView(s);
            outer.addView(sr,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
            return outer;
        }

        boolean sent = mine(sender);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(sent ? Gravity.END : Gravity.START);
        row.setPadding(dp(7),dp(1),dp(7),dp(1));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(9),dp(6),dp(8),dp(5));
        bubble.setBackground(bubbleBg(sent));

        String ref = attachment(body);
        Kind type = kind(ref);
        if (type == Kind.IMAGE) bubble.addView(imageView(body,time,sent));
        else if (type == Kind.AUDIO) bubble.addView(audioView(body,time,sent));
        else {
            TextView msg = text(body,15.2f,TEXT);
            msg.setLineSpacing(0,1.03f);
            int max = (int)(getResources().getDisplayMetrics().widthPixels*0.72f);
            int width = textWidth(msg,body,max);
            bubble.addView(msg,new LinearLayout.LayoutParams(width,ViewGroup.LayoutParams.WRAP_CONTENT));
            addMeta(bubble,time,sent);
        }

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.leftMargin = sent ? dp(42) : 0;
        bp.rightMargin = sent ? 0 : dp(42);
        row.addView(bubble,bp);
        outer.addView(row,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        return outer;
    }

    private int textWidth(TextView view,String body,int max) {
        if (body == null || body.isEmpty()) return dp(30);
        float widest = 0;
        String[] lines = body.split("\\n",-1);
        for (String line : lines) widest = Math.max(widest,view.getPaint().measureText(line));
        int natural = (int)Math.ceil(widest)+dp(8);
        if (body.length() > 24 || natural > max) return max;
        return Math.max(dp(34),Math.min(max,natural));
    }

    private void addMeta(LinearLayout box,String time,boolean sent) {
        TextView meta = text((time == null ? "" : time)+(sent ? "  ✓✓" : ""),10.3f,META);
        meta.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity=Gravity.END; lp.leftMargin=dp(22); lp.topMargin=dp(1);
        box.addView(meta,lp);
    }

    private View imageView(String body,String time,boolean sent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final File file = mediaFile(body);
        if (file == null) {
            TextView t = text("Image non extraite\n"+shortName(body),12.5f,META);
            t.setGravity(Gravity.CENTER); t.setBackground(bg(Color.rgb(201,225,221),7));
            box.addView(t,new LinearLayout.LayoutParams(dp(248),dp(145)));
        } else {
            final Bitmap bitmap = loadBitmap(file);
            if (bitmap == null) {
                TextView t = text("Image illisible\n"+shortName(body),12.5f,META);
                t.setGravity(Gravity.CENTER); t.setBackground(bg(Color.rgb(201,225,221),7));
                box.addView(t,new LinearLayout.LayoutParams(dp(248),dp(145)));
            } else {
                ImageView image = new ImageView(this);
                image.setImageBitmap(bitmap);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setAdjustViewBounds(true);
                int w=dp(248);
                int h=Math.round(w*(bitmap.getHeight()/(float)Math.max(1,bitmap.getWidth())));
                h=Math.max(dp(110),Math.min(dp(350),h));
                box.addView(image,new LinearLayout.LayoutParams(w,h));
                image.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        ImageView full = new ImageView(MainActivityV045.this);
                        full.setImageBitmap(bitmap); full.setAdjustViewBounds(true); full.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        new AlertDialog.Builder(MainActivityV045.this).setView(full).setPositiveButton("Fermer",null).show();
                    }
                });
            }
        }
        addMeta(box,time,sent);
        return box;
    }

    private Bitmap loadBitmap(File file) {
        String k=file.getAbsolutePath();
        Bitmap cached=bitmaps.get(k);
        if (cached!=null && !cached.isRecycled()) return cached;
        BitmapFactory.Options bound=new BitmapFactory.Options(); bound.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(k,bound);
        int sample=1;
        while (bound.outWidth/sample>1400 || bound.outHeight/sample>1400) sample*=2;
        BitmapFactory.Options opt=new BitmapFactory.Options(); opt.inSampleSize=Math.max(1,sample);
        Bitmap b=BitmapFactory.decodeFile(k,opt);
        if (b!=null) bitmaps.put(k,b);
        return b;
    }

    private View audioView(String body,String time,boolean sent) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        final File file=mediaFile(body);
        LinearLayout line=new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL);
        final PlayIcon icon=new PlayIcon(); icon.setBackground(bg(GREEN,22));
        line.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
        Wave wave=new Wave();
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(dp(180),dp(34)); wp.leftMargin=dp(8);
        line.addView(wave,wp);
        box.addView(line,new LinearLayout.LayoutParams(dp(232),ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView detail=text(file==null ? "Audio non extrait" : duration(file),10.3f,META);
        LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); dl.leftMargin=dp(50);
        box.addView(detail,dl); addMeta(box,time,sent);
        View.OnClickListener click=new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (file==null) {
                    Toast.makeText(MainActivityV045.this,"Audio introuvable : réimporte le ZIP une fois.",Toast.LENGTH_LONG).show();
                } else toggle(file,icon);
            }
        };
        icon.setOnClickListener(click); line.setOnClickListener(click);
        return box;
    }

    private String duration(File f) {
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            String raw=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long sec=raw==null ? 0 : Long.parseLong(raw)/1000L;
            return String.format(Locale.ROOT,"%d:%02d",sec/60,sec%60);
        } catch (Exception e) { return shortName(f.getName()); }
        finally { try { r.release(); } catch (Exception ignored) {} }
    }

    private void toggle(File f,PlayIcon icon) {
        try {
            String path=f.getAbsolutePath();
            if (player!=null && path.equals(currentAudio)) {
                if (player.isPlaying()) { player.pause(); icon.playing=false; icon.invalidate(); }
                else { player.start(); icon.playing=true; icon.invalidate(); }
                return;
            }
            releasePlayer();
            player=new MediaPlayer(); player.setDataSource(path); player.prepare();
            currentAudio=path; currentPlayIcon=icon;
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    if (currentPlayIcon!=null) { currentPlayIcon.playing=false; currentPlayIcon.invalidate(); }
                    try { mp.seekTo(0); } catch (Exception ignored) {}
                }
            });
            player.start(); icon.playing=true; icon.invalidate();
        } catch (Exception e) {
            releasePlayer();
            Toast.makeText(this,"Impossible de lire ce vocal : "+message(e),Toast.LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (currentPlayIcon!=null) { currentPlayIcon.playing=false; currentPlayIcon.invalidate(); }
        if (player!=null) { try { player.release(); } catch (Exception ignored) {} }
        player=null; currentPlayIcon=null; currentAudio=null;
    }

    private String shortName(String body) {
        String n=attachment(body);
        if (n==null) n=body==null ? "Média" : body.trim();
        return n.length()>48 ? n.substring(0,45)+"…" : n;
    }

    private class ArchiveAdapter extends CursorAdapter {
        ArchiveAdapter(Cursor c) { super(MainActivityV045.this,c,0); }
        @Override public View newView(android.content.Context context,Cursor cursor,ViewGroup parent) { return new LinearLayout(MainActivityV045.this); }
        @Override public void bindView(View view,android.content.Context context,Cursor c) {
            LinearLayout h=(LinearLayout)view; h.removeAllViews(); h.setOrientation(LinearLayout.VERTICAL);
            String date=c.getString(c.getColumnIndexOrThrow("mdate"));
            String time=c.getString(c.getColumnIndexOrThrow("mtime"));
            String sender=c.isNull(c.getColumnIndexOrThrow("sender")) ? null : c.getString(c.getColumnIndexOrThrow("sender"));
            String body=c.getString(c.getColumnIndexOrThrow("body"));
            boolean system=c.getInt(c.getColumnIndexOrThrow("system"))!=0;
            boolean show=c.getInt(c.getColumnIndexOrThrow("show_date"))!=0;
            h.addView(rowView(date,time,sender,body,system,show),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private class PlayIcon extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); boolean playing;
        PlayIcon() { super(MainActivityV045.this); p.setColor(Color.WHITE); p.setStyle(Paint.Style.FILL); }
        @Override protected void onDraw(Canvas c) {
            float x=getWidth()/2f,y=getHeight()/2f;
            if (playing) {
                c.drawRect(x-dp(6),y-dp(8),x-dp(2),y+dp(8),p); c.drawRect(x+dp(2),y-dp(8),x+dp(6),y+dp(8),p);
            } else {
                Path path=new Path(); path.moveTo(x-dp(5),y-dp(9)); path.lineTo(x+dp(8),y); path.lineTo(x-dp(5),y+dp(9)); path.close(); c.drawPath(path,p);
            }
        }
    }

    private class Wave extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final int[] bars={4,8,12,7,15,10,6,13,17,8,5,12,9,16,11,7,14,18,10,5,12,7,15,9,6,13,8,17,11,7,14,9};
        Wave() { super(MainActivityV045.this); p.setColor(Color.rgb(111,132,142)); p.setStrokeWidth(dp(1.6f)); p.setStrokeCap(Paint.Cap.ROUND); }
        @Override protected void onDraw(Canvas c) {
            float gap=getWidth()/(float)bars.length,mid=getHeight()/2f;
            for(int i=0;i<bars.length;i++){ float h=dp(bars[i]*0.48f),x=gap*i+gap/2f; c.drawLine(x,mid-h/2,x,mid+h/2,p); }
        }
    }

    private static class MediaStats { int images; int audio; }

    @Override protected void onDestroy() {
        releasePlayer(); closeRenderer(); bitmaps.evictAll(); super.onDestroy();
    }
}
