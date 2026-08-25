package ch.piiwii.waarchive;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

/** Read-only provider used to open documents extracted from the sealed WhatsApp archive. */
public class ArchiveFileProvider extends ContentProvider {
    public static final String AUTHORITY = "ch.piiwii.waarchive.files";
    private static final String DOC_DIR = "wa_docs";

    @Override public boolean onCreate() { return true; }

    private File resolve(Uri uri) throws FileNotFoundException {
        if (getContext() == null) throw new FileNotFoundException("Contexte indisponible");
        String key = uri == null ? null : uri.getLastPathSegment();
        if (key == null || key.isEmpty() || key.contains("/") || key.contains("\\")) {
            throw new FileNotFoundException("Document invalide");
        }
        File root = new File(getContext().getFilesDir(), DOC_DIR);
        File file = new File(root, key);
        try {
            String rootPath = root.getCanonicalPath() + File.separator;
            String filePath = file.getCanonicalPath();
            if (!filePath.startsWith(rootPath)) throw new FileNotFoundException("Accès refusé");
        } catch (IOException e) {
            throw new FileNotFoundException("Document inaccessible");
        }
        if (!file.exists() || !file.isFile()) throw new FileNotFoundException("Document introuvable");
        return file;
    }

    private String displayName(Uri uri, File file) {
        String requested = uri == null ? null : uri.getQueryParameter("name");
        return requested == null || requested.trim().isEmpty() ? file.getName() : requested;
    }

    @Override public String getType(Uri uri) {
        String name = uri == null ? "" : uri.getQueryParameter("name");
        if (name == null || name.isEmpty()) name = uri == null ? "" : uri.getLastPathSegment();
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        String ext = dot >= 0 ? lower.substring(dot + 1) : "";
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mime != null) return mime;
        if ("pdf".equals(ext)) return "application/pdf";
        if ("doc".equals(ext)) return "application/msword";
        if ("docx".equals(ext)) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if ("xls".equals(ext)) return "application/vnd.ms-excel";
        if ("xlsx".equals(ext)) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if ("ppt".equals(ext)) return "application/vnd.ms-powerpoint";
        if ("pptx".equals(ext)) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if ("rtf".equals(ext)) return "application/rtf";
        if ("csv".equals(ext)) return "text/csv";
        if ("vcf".equals(ext)) return "text/vcard";
        if ("ics".equals(ext)) return "text/calendar";
        if ("zip".equals(ext)) return "application/zip";
        if ("rar".equals(ext)) return "application/vnd.rar";
        if ("7z".equals(ext)) return "application/x-7z-compressed";
        return "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File file;
        try { file = resolve(uri); }
        catch (FileNotFoundException e) { return null; }
        String[] cols = projection == null || projection.length == 0
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        MatrixCursor cursor = new MatrixCursor(cols, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String col : cols) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) row.add(displayName(uri, file));
            else if (OpenableColumns.SIZE.equals(col)) row.add(file.length());
            else row.add(null);
        }
        return cursor;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (mode != null && !mode.equals("r")) throw new FileNotFoundException("Lecture seule");
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Lecture seule"); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
}
