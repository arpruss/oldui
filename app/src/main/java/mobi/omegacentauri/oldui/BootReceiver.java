package mobi.omegacentauri.oldui;

import static mobi.omegacentauri.oldui.oldui.startAnytimeUI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
        if (options.getBoolean("onBoot", false)) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    startAnytimeUI(context);
                }
            }, 500);
        }
        if (options.getBoolean("ll", false)) {
            AccessibilityService as = AccessibilityService.getInstance();
            if (as == null) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.v("OldUI", "starting LL");
                        oldui.startLL(context);
                    }
                }, 1000);
            }
        }
    }
}
