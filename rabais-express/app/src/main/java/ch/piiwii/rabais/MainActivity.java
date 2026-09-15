package ch.piiwii.rabais;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
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
    private TextView resultValue;
    private TextView savingValue;
    private SharedPreferences prefs;
    private int discount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("rabais", MODE_PRIVATE);
        discount = prefs.getInt("last_discount", 30);

        priceInput = findViewById(R.id.priceInput);
        discountLabel = findViewById(R.id.discountLabel);
        resultValue = findViewById(R.id.resultValue);
        savingValue = findViewById(R.id.savingValue);

        bind(R.id.b10, 10);
        bind(R.id.b20, 20);
        bind(R.id.b30, 30);
        bind(R.id.b40, 40);
        bind(R.id.b50, 50);
        findViewById(R.id.customButton).setOnClickListener(v -> customDiscount());
        priceInput.addTextChangedListener(new SimpleTextWatcher(this::update));
        update();
    }

    private void bind(int id, int percent) {
        findViewById(id).setOnClickListener(v -> {
            discount = percent;
            prefs.edit().putInt("last_discount", percent).apply();
            update();
        });
    }

    private void customDiscount() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(discount));
        input.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rabais personnalisé (%)")
                .setView(input)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Utiliser", null)
                .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                double value = Double.parseDouble(input.getText().toString().trim().replace(',', '.'));
                if (value < 0 || value > 100) throw new NumberFormatException();
                discount = (int) Math.round(value);
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
        discountLabel.setText("Avec -" + discount + " %");
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

    private String chf(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("de", "CH"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(true);
        return "CHF " + format.format(value);
    }
}
