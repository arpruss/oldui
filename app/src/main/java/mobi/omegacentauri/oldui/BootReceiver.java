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

import java.util.Timer;
import java.util.TimerTask;

public class BootReceiver extends BroadcastReceiver {
    private boolean onBootOldUI;
    private boolean launchLL;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v("OldUI", "OldUI on boot");
        /*
        The logic here is a bit complicated. If we don't do the boot fix, then we launch LL
        and old UI with a small delay from here. If we do the boot fix, then we launch LL and old UI after the store
        gets launched if we can monitor for the store launch (have READ_LOGS permission), and
        otherwise we launch them a bit after the boot fix.
         */

        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
        boolean bootFix = options.getBoolean(oldui.OPTION_BOOT_FIX, false);
        if (bootFix && BootStartFix.getInstance() == null)
            bootFix = false;
        onBootOldUI = options.getBoolean(oldui.OPTION_ONBOOT_OLDUI, false);
        launchLL = options.getBoolean(oldui.OPTION_ONBOOT_LL, false);
        boolean muteHome = options.getBoolean(oldui.OPTION_MUTE_HOME, false);

        if ((muteHome || bootFix && (launchLL || onBootOldUI)) && PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(android.Manifest.permission.READ_LOGS)) {
            Log.v("OldUI", "activating app launch monitoring");

            boolean doLaunches = bootFix && (launchLL || onBootOldUI);

            if (bootFix) {
                launchLL = false;
                onBootOldUI = false;
            }

            // we will launch LL and old UI after the store
            oldui.activateMonitoring(context, doLaunches);
        }

        if (bootFix) {
            Log.v("OldUI", "double tap power");
            BootStartFix.globalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                            PowerManager.FULL_WAKE_LOCK |
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                    PowerManager.ON_AFTER_RELEASE,
                            "MyApp:WakeUpTag"
                    );

                    wakeLock.acquire(5000);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            wakeLock.release();
                            launches(context);
                        }
                    }, 1000);
                }
            }, 1500);
        }
        else {
            launches(context);
        }
    }

    private void launches(Context context) {
        if (launchLL) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.v("OldUI", "starting LL");
                    oldui.startLL(context);
                }
            }, 1000);
        }
        if (onBootOldUI) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.v("OldUI", "starting old ui");
                    oldui.startAnytimeUI(context);
                }
            }, 1500);
        }
    }
}


