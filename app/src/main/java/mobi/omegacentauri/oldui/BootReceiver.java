package mobi.omegacentauri.oldui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
            if (options.getBoolean("onBoot", false))
                oldui.startAnytimeUI(context);
            if (options.getBoolean("ll", false))
                oldui.startLL(context);
        }
    }
}
