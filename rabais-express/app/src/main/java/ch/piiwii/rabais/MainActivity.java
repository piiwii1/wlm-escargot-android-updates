package ch.piiwii.rabais;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView priceValue;
    private TextView resultValue;
    private TextView savingValue;
    private TextView discountValue;
    private TextView moreToggle;
    private Button recentButton1;
    private Button recentButton2;
    private LinearLayout morePanel;
    private SharedPreferences prefs;

    private final StringBuilder priceBuffer = new StringBuilder();
    private int discount;
    private boolean customSelected;
    private int recent1;
    private int recent2;
    private boolean extrasOpen = false;

    private final int[] discountButtonIds = {R.id.b10, R.id.b20, R.id.b30, R.id.b40, R.id.b50};
    private final int[] discountValues = {10, 20, 30, 40, 50};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main);

        applySystemBarInsets();

        prefs = getSharedPreferences("rabais", MODE_PRIVATE);
        discount = clampDiscount(prefs.getInt("last_discount", 30));
        customSelected = prefs.getBoolean("custom_selected", false);
        loadRecentDiscounts();

        priceValue = findViewById(R.id.priceValue);
        resultValue = findViewById(R.id.resultValue);
        savingValue = findViewById(R.id.savingValue);
        discountValue = findViewById(R.id.discountValue);
        moreToggle = findViewById(R.id.moreToggle);
        recentButton1 = findViewById(R.id.recentButton1);
        recentButton2 = findViewById(R.id.recentButton2);
        morePanel = findViewById(R.id.morePanel);

        recentButton1.setOnClickListener(v -> applyPreset(recent1));
        recentButton2.setOnClickListener(v -> applyPreset(recent2));

        bindDiscount(R.id.b10, 10);
        bindDiscount(R.id.b20, 20);
        bindDiscount(R.id.b30, 30);
        bindDiscount(R.id.b40, 40);
        bindDiscount(R.id.b50, 50);

        moreToggle.setOnClickListener(v -> toggleExtras());

        bindNumber(R.id.key0, "0");
        bindNumber(R.id.key1, "1");
        bindNumber(R.id.key2, "2");
        bindNumber(R.id.key3, "3");
        bindNumber(R.id.key4, "4");
        bindNumber(R.id.key5, "5");
        bindNumber(R.id.key6, "6");
        bindNumber(R.id.key7, "7");
        bindNumber(R.id.key8, "8");
        bindNumber(R.id.key9, "9");

        findViewById(R.id.keyComma).setOnClickListener(v -> appendDecimal());
        findViewById(R.id.keyClear).setOnClickListener(v -> clearPrice());
        findViewById(R.id.backspaceButton).setOnClickListener(v -> backspace());

        findViewById(R.id.minus5).setOnClickListener(v -> adjustDiscount(-5));
        findViewById(R.id.minus1).setOnClickListener(v -> adjustDiscount(-1));
        findViewById(R.id.plus1).setOnClickListener(v -> adjustDiscount(1));
        findViewById(R.id.plus5).setOnClickListener(v -> adjustDiscount(5));

        updateAll();
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.rootScroll);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void loadRecentDiscounts() {
        if (!prefs.contains("recent_discount_1")) {
            if (!customSelected && isPreset(discount)) {
                recent1 = discount;
                recent2 = discount == 40 ? 30 : 40;
            } else {
                recent1 = 30;
                recent2 = 40;
            }
            persistRecents();
        } else {
            recent1 = normalizeRecent(prefs.getInt("recent_discount_1", 30), 30);
            recent2 = normalizeRecent(prefs.getInt("recent_discount_2", 40), 40);
            if (recent1 == recent2) recent2 = recent1 == 40 ? 30 : 40;
        }
    }

    private int normalizeRecent(int value, int fallback) {
        return isPreset(value) ? value : fallback;
    }

    private boolean isPreset(int value) {
        return value == 10 || value == 20 || value == 30 || value == 40 || value == 50;
    }

    private void bindDiscount(int id, int percent) {
        findViewById(id).setOnClickListener(v -> applyPreset(percent));
    }

    private void applyPreset(int percent) {
        discount = percent;
        customSelected = false;
        rememberPreset(percent);
        saveDiscount();
        updateAll();
    }

    private void rememberPreset(int percent) {
        if (percent == recent1) return;
        if (percent == recent2) {
            int previousLatest = recent1;
            recent1 = percent;
            recent2 = previousLatest;
        } else {
            recent2 = recent1;
            recent1 = percent;
        }
        persistRecents();
    }

    private void persistRecents() {
        prefs.edit()
                .putInt("recent_discount_1", recent1)
                .putInt("recent_discount_2", recent2)
                .apply();
    }

    private void toggleExtras() {
        extrasOpen = !extrasOpen;
        morePanel.setVisibility(extrasOpen ? View.VISIBLE : View.GONE);
        moreToggle.setText(extrasOpen ? "Masquer les autres rabais  ▴" : "Autres rabais  ▾");
    }

    private void bindNumber(int id, String token) {
        findViewById(id).setOnClickListener(v -> appendDigit(token));
    }

    private void appendDigit(String digit) {
        int dot = priceBuffer.indexOf(".");
        if (dot >= 0 && priceBuffer.length() - dot - 1 >= 2) return;
        if (priceBuffer.length() >= 10) return;

        if (priceBuffer.length() == 1 && priceBuffer.charAt(0) == '0' && dot < 0) {
            if (!"0".equals(digit)) {
                priceBuffer.setLength(0);
                priceBuffer.append(digit);
            }
        } else {
            priceBuffer.append(digit);
        }
        updateAll();
    }

    private void appendDecimal() {
        if (priceBuffer.indexOf(".") >= 0) return;
        if (priceBuffer.length() == 0) priceBuffer.append('0');
        priceBuffer.append('.');
        updateAll();
    }

    private void backspace() {
        if (priceBuffer.length() > 0) {
            priceBuffer.deleteCharAt(priceBuffer.length() - 1);
            updateAll();
        }
    }

    private void clearPrice() {
        priceBuffer.setLength(0);
        updateAll();
    }

    private void adjustDiscount(int delta) {
        discount = clampDiscount(discount + delta);
        customSelected = true;
        saveDiscount();
        updateAll();
    }

    private int clampDiscount(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private void saveDiscount() {
        prefs.edit()
                .putInt("last_discount", discount)
                .putBoolean("custom_selected", customSelected)
                .apply();
    }

    private void updateAll() {
        updatePriceDisplay();
        updateDiscountDisplay();
        updateResult();
    }

    private void updatePriceDisplay() {
        if (priceBuffer.length() == 0) {
            priceValue.setText("CHF 0.00");
            return;
        }
        String typed = priceBuffer.toString().replace('.', ',');
        priceValue.setText("CHF " + typed);
    }

    private void updateDiscountDisplay() {
        discountValue.setText("-" + discount + " %");

        recentButton1.setText("-" + recent1 + " %");
        recentButton2.setText("-" + recent2 + " %");
        styleRecentButton(recentButton1, recent1);
        styleRecentButton(recentButton2, recent2);

        for (int i = 0; i < discountButtonIds.length; i++) {
            Button button = findViewById(discountButtonIds[i]);
            int value = discountValues[i];
            boolean isRecent = value == recent1 || value == recent2;
            button.setVisibility(isRecent ? View.GONE : View.VISIBLE);
            boolean selected = !customSelected && discount == value;
            button.setBackgroundResource(selected ? R.drawable.bg_discount_selected : R.drawable.bg_discount_button);
            button.setTextColor(selected ? Color.WHITE : Color.rgb(22, 25, 30));
        }
    }

    private void styleRecentButton(Button button, int value) {
        boolean selected = !customSelected && discount == value;
        button.setBackgroundResource(selected ? R.drawable.bg_discount_selected : R.drawable.bg_discount_button);
        button.setTextColor(selected ? Color.WHITE : Color.rgb(22, 25, 30));
    }

    private void updateResult() {
        BigDecimal price = parsePrice();
        if (price == null) {
            resultValue.setText("CHF 0.00");
            savingValue.setText("Économie  CHF 0.00");
            return;
        }

        BigDecimal saving = price
                .multiply(BigDecimal.valueOf(discount))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalPrice = price.subtract(saving).setScale(2, RoundingMode.HALF_UP);

        resultValue.setText(chf(finalPrice));
        savingValue.setText("Économie  " + chf(saving));
    }

    private BigDecimal parsePrice() {
        try {
            if (priceBuffer.length() == 0) return null;
            String raw = priceBuffer.toString();
            if (raw.endsWith(".")) raw = raw.substring(0, raw.length() - 1);
            if (raw.isEmpty()) return null;
            BigDecimal price = new BigDecimal(raw);
            if (price.signum() < 0) return null;
            return price;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String chf(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("de", "CH"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(true);
        return "CHF " + format.format(value);
    }
}
