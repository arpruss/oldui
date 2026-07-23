package mobi.omegacentauri.oldui;

import static mobi.omegacentauri.oldui.oldui.startAnytimeUI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v("OldUI", "OldUI on boot");
        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
        boolean onBootOldUI = options.getBoolean(oldui.OPTION_ONBOOT_OLDUI, false);
        boolean launchLL = options.getBoolean(oldui.OPTION_ONBOOT_LL, false);
        boolean muteHome = options.getBoolean(oldui.OPTION_MUTE_HOME, false) && AccessibilityService.getInstance() == null;
        AccessibilityService as = AccessibilityService.getInstance();
        if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(android.Manifest.permission.READ_LOGS)) {
            if (((onBootOldUI && as == null) || launchLL || muteHome)) {
                Log.v("OldUI", "activating monitoring");
                oldui.activateMonitoring(context, true);
            }
        }
        else {
            if (launchLL) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.v("OldUI", "starting LL");
                        oldui.startLL(context);
                    }
                }, 1000);
            }
            if (onBootOldUI && as == null) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.v("OldUI", "starting old ui");
                        oldui.startAnytimeUI(context);
                    }
                }, 1500);
            }
        }
/*        else if (options.getBoolean("closeStore", false)) {
            AccessibilityService as = AccessibilityService.getInstance();
            if (as != null) {
                as.state = AccessibilityService.WAITING_FOR_STORE;
            }
        } */
    }
}
