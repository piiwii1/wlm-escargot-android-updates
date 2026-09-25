package ch.piiwii.euroscan;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScannerActivity extends ComponentActivity {
    private static final int REQ_CAMERA = 4401;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean analysing = new AtomicBoolean(false);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    private PreviewView previewView;
    private ScannerOverlay overlay;
    private TextView status;
    private TextView detail;
    private FrameLayout topBar;
    private LinearLayout bottomCard;

    private String lastSignature = "";
    private String lastGoodText = "";
    private int stableFrames = 0;
    private boolean completed = false;
    private long lastAnalysisAt = 0L;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        buildUi();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
        else requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(previewView, new FrameLayout.LayoutParams(-1, -1));

        overlay = new ScannerOverlay();
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        topBar = new FrameLayout(this);
        topBar.setPadding(dp(14), dp(8), dp(14), dp(8));
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(-1, dp(56), Gravity.TOP);
        topLp.setMargins(dp(10), dp(10), dp(10), 0);
        root.addView(topBar, topLp);

        TextView title = label("Scanner automatique", 17, Color.WHITE, true);
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_VERTICAL | Gravity.START);
        topBar.addView(title, titleLp);

        TextView close = label("✕", 24, Color.WHITE, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(round(Color.argb(120, 0, 0, 0), 18));
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER_VERTICAL | Gravity.END);
        topBar.addView(close, closeLp);
        close.setOnClickListener(v -> { setResult(Activity.RESULT_CANCELED); finish(); });

        bottomCard = new LinearLayout(this);
        bottomCard.setOrientation(LinearLayout.VERTICAL);
        bottomCard.setGravity(Gravity.CENTER);
        bottomCard.setPadding(dp(18), dp(15), dp(18), dp(15));
        bottomCard.setBackground(round(Color.argb(225, 22, 16, 39), 20));
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        bottomLp.setMargins(dp(16), 0, dp(16), dp(18));
        root.addView(bottomCard, bottomLp);

        status = label("Place le ticket dans le cadre", 18, Color.WHITE, true);
        status.setGravity(Gravity.CENTER);
        detail = label("Tu n'as rien à appuyer : dès que la lecture est nette et stable, le scan se termine tout seul.", 13, Color.rgb(220, 216, 232), false);
        detail.setGravity(Gravity.CENTER);
        bottomCard.addView(status);
        bottomCard.addView(space(5));
        bottomCard.addView(detail);

        setContentView(root);
        applySafeInsets(root, topLp, bottomLp);
    }

    private void applySafeInsets(View root, FrameLayout.LayoutParams topLp, FrameLayout.LayoutParams bottomLp) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars());
                top = i.top; bottom = i.bottom;
            } else {
                top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom();
            }
            topLp.topMargin = top + dp(10);
            bottomLp.bottomMargin = bottom + dp(18);
            topBar.setLayoutParams(topLp);
            bottomCard.setLayoutParams(bottomLp);
            return insets;
        });
        root.requestApplyInsets();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startCamera();
        else {
            status.setText("Caméra refusée");
            detail.setText("Autorise la caméra pour utiliser le scanner automatique.");
            main.postDelayed(this::finish, 1300);
        }
    }

    private void startCamera() {
        status.setText("Recherche du ticket…");
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyseFrame);
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception e) {
                status.setText("Caméra indisponible");
                detail.setText("Utilise l'import d'une photo depuis l'accueil.");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyseFrame(ImageProxy proxy) {
        if (completed) { proxy.close(); return; }
        long now = SystemClock.elapsedRealtime();
        if (now - lastAnalysisAt < 420 || !analysing.compareAndSet(false, true)) { proxy.close(); return; }
        lastAnalysisAt = now;
        if (proxy.getImage() == null) { analysing.set(false); proxy.close(); return; }

        InputImage image = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());
        recognizer.process(image)
                .addOnSuccessListener(result -> evaluate(result.getText()))
                .addOnFailureListener(e -> main.post(() -> {
                    status.setText("Recherche du ticket…");
                    detail.setText("Approche un peu le ticket et évite les reflets.");
                }))
                .addOnCompleteListener(task -> {
                    analysing.set(false);
                    proxy.close();
                });
    }

    private void evaluate(String text) {
        if (completed || text == null) return;
        String upper = text.toUpperCase(Locale.ROOT);
        boolean ticketWord = upper.contains("EUROMILLION") || upper.contains("EURO MILLION") || upper.contains("SWISS WIN") || upper.contains("LOTERIE ROMANDE") || upper.contains("LOTERIE");
        TicketParser.ParsedTicket parsed = TicketParser.parse(text);
        boolean gridFound = !parsed.grids.isEmpty();
        boolean enoughText = text.length() > 45;

        if (!ticketWord || !gridFound || !enoughText) {
            stableFrames = 0;
            lastSignature = "";
            main.post(() -> {
                status.setText(ticketWord ? "Ticket détecté — lecture…" : "Recherche du ticket…");
                detail.setText(gridFound ? "Garde le ticket immobile dans le cadre." : "Cadre tout le reçu, avec les numéros bien nets.");
                overlay.setReady(false);
            });
            return;
        }

        StringBuilder sig = new StringBuilder(parsed.date == null ? "?" : parsed.date);
        int limit = Math.min(parsed.grids.size(), 12);
        for (int i = 0; i < limit; i++) sig.append('|').append(parsed.grids.get(i).compact());
        String signature = sig.toString();

        if (signature.equals(lastSignature)) stableFrames++; else {
            lastSignature = signature;
            stableFrames = 1;
        }
        lastGoodText = text;

        main.post(() -> {
            overlay.setReady(true);
            if (stableFrames == 1) {
                status.setText("Ticket lu — ne bouge plus");
                detail.setText(parsed.grids.size() + " grille" + (parsed.grids.size() > 1 ? "s" : "") + " détectée" + (parsed.grids.size() > 1 ? "s" : "") + ". Je confirme la lecture…");
            } else {
                status.setText("Lecture stable ✓");
                detail.setText("Le ticket est suffisamment lisible.");
            }
        });

        if (stableFrames >= 3) finishScan();
    }

    private void finishScan() {
        if (completed) return;
        completed = true;
        main.post(() -> {
            status.setText("Ticket scanné ✓");
            detail.setText("Ouverture du contrôle…");
            overlay.setComplete();
            main.postDelayed(() -> {
                Intent data = new Intent();
                data.putExtra("ocr_text", lastGoodText);
                setResult(Activity.RESULT_OK, data);
                finish();
            }, 350);
        });
    }

    @Override
    protected void onDestroy() {
        recognizer.close();
        cameraExecutor.shutdownNow();
        super.onDestroy();
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }

    private View space(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); return v; }
    private android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private class ScannerOverlay extends View {
        private final Paint dim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float phase = 0f;
        private int frameColor = Color.WHITE;
        private final ValueAnimator animator;

        ScannerOverlay() {
            super(ScannerActivity.this);
            dim.setColor(Color.argb(105, 0, 0, 0));
            frame.setStyle(Paint.Style.STROKE); frame.setStrokeWidth(dp(3)); frame.setColor(frameColor);
            line.setStrokeWidth(dp(2)); line.setColor(Color.rgb(246, 165, 0));
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1500); animator.setRepeatCount(ValueAnimator.INFINITE); animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); invalidate(); });
            animator.start();
        }

        void setReady(boolean ready) { frameColor = ready ? Color.rgb(56, 211, 137) : Color.WHITE; invalidate(); }
        void setComplete() { frameColor = Color.rgb(56, 211, 137); animator.cancel(); phase = .5f; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float left = getWidth() * .075f, right = getWidth() * .925f;
            float top = getHeight() * .18f, bottom = getHeight() * .74f;
            RectF r = new RectF(left, top, right, bottom);
            c.drawRect(0, 0, getWidth(), top, dim);
            c.drawRect(0, bottom, getWidth(), getHeight(), dim);
            c.drawRect(0, top, left, bottom, dim);
            c.drawRect(right, top, getWidth(), bottom, dim);
            frame.setColor(frameColor);
            c.drawRoundRect(r, dp(18), dp(18), frame);
            float y = top + dp(16) + phase * Math.max(1, (bottom - top - dp(32)));
            c.drawLine(left + dp(16), y, right - dp(16), y, line);
        }
    }
}
