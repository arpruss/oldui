package mobi.omegacentauri.oldui;

import static mobi.omegacentauri.oldui.oldui.startAnytimeUI;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v("OldUI", "OldUI on boot");

        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
        boolean bootFix = options.getBoolean(oldui.OPTION_BOOT_FIX, false);
        if (bootFix && BootStartFix.getInstance() == null)
            bootFix = false;
        boolean onBootOldUI = options.getBoolean(oldui.OPTION_ONBOOT_OLDUI, false);
        boolean launchLL = options.getBoolean(oldui.OPTION_ONBOOT_LL, false);
        boolean muteHome = options.getBoolean(oldui.OPTION_MUTE_HOME, false);

        if (bootFix || launchLL || onBootOldUI || (muteHome && PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(android.Manifest.permission.READ_LOGS))) {
            Log.v("OldUI", "activating app launch monitoring");

            oldui.activateMonitoring(context, true);
        }
    }
}


