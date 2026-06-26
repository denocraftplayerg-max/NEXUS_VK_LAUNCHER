package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public final class CustomSkinStore {

    private static final String PREFS = "custom_skin_store";
    private static final String KEY_SKIN_PATH = "skin_path";
    private static final String KEY_SKIN_MODEL = "skin_model";

    private final SharedPreferences prefs;

    public CustomSkinStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Nullable public String getSkinPath() { return prefs.getString(KEY_SKIN_PATH, null); }
    @NonNull  public String getSkinModel() { return prefs.getString(KEY_SKIN_MODEL, "classic"); }
    public boolean hasSkin() { String p = getSkinPath(); return p != null && new File(p).exists(); }

    public void setSkin(@NonNull String path, @NonNull String model) {
        prefs.edit().putString(KEY_SKIN_PATH, path).putString(KEY_SKIN_MODEL, model).apply();
    }

    public void clearSkin() {
        String p = getSkinPath();
        if (p != null) new File(p).delete();
        prefs.edit().remove(KEY_SKIN_PATH).remove(KEY_SKIN_MODEL).apply();
    }
}
