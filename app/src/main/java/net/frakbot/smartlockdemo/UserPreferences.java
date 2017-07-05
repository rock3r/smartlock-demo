package net.frakbot.smartlockdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class UserPreferences {

    private static final String KEY_USE_SMART_LOCK = "use_smart_lock";

    private final SharedPreferences preferences;

    public static UserPreferences with(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new UserPreferences(preferences);
    }

    private UserPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    void setUserRefusedSmartLock() {
        preferences.edit()
                .putBoolean(KEY_USE_SMART_LOCK, false)
                .apply();
    }

    void resetUseSmartLock() {
        preferences.edit()
                .remove(KEY_USE_SMART_LOCK)
                .apply();
    }

    boolean useSmartLock() {
        return preferences.getBoolean(KEY_USE_SMART_LOCK, true);
    }
}
