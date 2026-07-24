package mobi.omegacentauri.oldui;

// TODO: close store

import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class BootStartFix extends android.accessibilityservice.AccessibilityService {
    private static BootStartFix instance = null;

    public static boolean supportedLanguage() {
        return true; // Locale.getDefault().getLanguage().equals("en");
    }

    public static BootStartFix getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v("OldUI", "bsf onCreate");
    }
    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v("OldUI", "bsf onConnected");
        instance = this;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        Log.v("OldUI", "bsf onUnbind");
        return super.onUnbind(intent);
    }

    public static void globalAction(int ga) {
        if (instance != null) {
            Log.v("OldUI", "global action "+ga);
            instance.performGlobalAction(ga);
        }
    }

}
