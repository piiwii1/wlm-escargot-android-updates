package ch.piiwii.rabais;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText priceInput;
    private TextView discountLabel;
    private TextView currentDiscount;
    private TextView resultValue;
    private TextView savingValue;
    private Button customButton;
    private SharedPreferences prefs;
    private int discount;
    private boolean customSelected;

    private final int[] discountButtonIds = {R.id.b10, R.id.b20, R.id.b30, R.id.b40, R.id.b50};
    private final int[] discountValues = {10, 20, 30, 40, 50};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applySystemBarInsets();

        prefs = getSharedPreferences("rabais", MODE_PRIVATE);
        discount = prefs.getInt("last_discount", 30);
        customSelected = !isPreset(discount);

        priceInput = findViewById(R.id.priceInput);
        discountLabel = findViewById(R.id.discountLabel);
        currentDiscount = findViewById(R.id.currentDiscount);
        resultValue = findViewById(R.id.resultValue);
        savingValue = findViewById(R.id.savingValue);
        customButton = findViewById(R.id.customButton);

        bind(R.id.b10, 10);
        bind(R.id.b20, 20);
        bind(R.id.b30, 30);
        bind(R.id.b40, 40);
        bind(R.id.b50, 50);

        customButton.setOnClickListener(v -> customDiscount());
        findViewById(R.id.clearButton).setOnClickListener(v -> {
            priceInput.setText("");
            priceInput.requestFocus();
        });

        priceInput.addTextChangedListener(new SimpleTextWatcher(this::update));
        update();
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.rootScroll);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
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

    private void bind(int id, int percent) {
        findViewById(id).setOnClickListener(v -> {
            discount = percent;
            customSelected = false;
            prefs.edit().putInt("last_discount", percent).apply();
            update();
        });
    }

    private boolean isPreset(int value) {
        return value == 10 || value == 20 || value == 30 || value == 40 || value == 50;
    }

    private void customDiscount() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(discount));
        input.setSelectAllOnFocus(true);
        int sidePadding = dp(22);
        input.setPadding(sidePadding, dp(8), sidePadding, dp(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rabais personnalisé")
                .setMessage("Entre un pourcentage entre 0 et 100.")
                .setView(input)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Utiliser", null)
                .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int value = Integer.parseInt(input.getText().toString().trim());
                if (value < 0 || value > 100) throw new NumberFormatException();
                discount = value;
                customSelected = !isPreset(value);
                prefs.edit().putInt("last_discount", discount).apply();
                update();
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(this, "Entre un pourcentage entre 0 et 100.", Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private void update() {
        discountLabel.setText("Rabais appliqué : -" + discount + " %");
        currentDiscount.setText("-" + discount + " % sélectionné");
        refreshButtons();

        try {
            String raw = priceInput.getText().toString().trim()
                    .replace("'", "")
                    .replace("’", "")
                    .replace(" ", "")
                    .replace(',', '.');
            if (raw.isEmpty()) throw new NumberFormatException();

            BigDecimal price = new BigDecimal(raw);
            if (price.signum() < 0) throw new NumberFormatException();

            BigDecimal saving = price
                    .multiply(BigDecimal.valueOf(discount))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal finalPrice = price.subtract(saving).setScale(2, RoundingMode.HALF_UP);

            resultValue.setText(chf(finalPrice));
            savingValue.setText("Tu économises " + chf(saving));
        } catch (Exception e) {
            resultValue.setText("CHF 0.00");
            savingValue.setText("Tu économises CHF 0.00");
        }
    }

    private void refreshButtons() {
        for (int i = 0; i < discountButtonIds.length; i++) {
            Button button = findViewById(discountButtonIds[i]);
            boolean selected = !customSelected && discount == discountValues[i];
            button.setBackgroundResource(selected ? R.drawable.bg_discount_selected : R.drawable.bg_discount_button);
            button.setTextColor(selected ? Color.WHITE : Color.rgb(22, 25, 30));
        }

        if (customSelected) {
            customButton.setText("-" + discount + " % · personnalisé");
            customButton.setBackgroundResource(R.drawable.bg_discount_selected);
            customButton.setTextColor(Color.WHITE);
        } else {
            customButton.setText("Autre rabais…");
            customButton.setBackgroundResource(R.drawable.bg_custom);
            customButton.setTextColor(Color.rgb(61, 67, 75));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String chf(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("de", "CH"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(true);
        return "CHF " + format.format(value);
    }
}
