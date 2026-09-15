package ch.piiwii.rabais;

import android.text.Editable;
import android.text.TextWatcher;

public class SimpleTextWatcher implements TextWatcher {
    private final Runnable action;

    public SimpleTextWatcher(Runnable action) {
        this.action = action;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { action.run(); }
    @Override public void afterTextChanged(Editable s) {}
}
