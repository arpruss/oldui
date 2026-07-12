package mobi.omegacentauri.oldui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class oldui extends Activity {

    private SharedPreferences options;
    static final String SETTINGS = "com.android.settings";

    public static void startAnytimeUI(Context context) {
        String pkg = "com.oculus.systemux";
        String cls = "com.oculus.panelapp.anytimeui.AnytimeUIActivity";
        Intent i = new Intent();
        i.setComponent(new ComponentName(pkg, cls));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        AccessibilityService as = AccessibilityService.getInstance();
        if (as != null) {
            as.startTime = 0;
            as.state = 0;
            settingsForShell(context);
/*            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    settingsForShell(context);
                }
            }, 250); */
        }
    }

    private static void settingsForShell(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

        // Attach the package name to the intent so Android knows which app to show
        Uri uri = Uri.parse("package:" + "com.oculus.vrshell");
        intent.setData(uri);
        intent.setPackage(SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        context.startActivity(intent);
    }

    public static void startStore(Context context) {
        String pkg = "com.oculus.store";
        String cls = "com.oculus.store.StoreActivity";
        Intent i = new Intent();
        i.setComponent(new ComponentName(pkg, cls));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    public static Intent getLLIntent(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent i;
        i = packageManager.getLaunchIntentForPackage("com.threethan.launcher");
        if (i != null)
            return i;
        i = packageManager.getLaunchIntentForPackage("com.threethan.launcher.metastore");
        return i;
    }

    public static void startLL(Context context) {
        Intent i = getLLIntent(context);
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        options = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.main);
        IntentFilter filter = new IntentFilter("mobi.omegacentauri.oldui.modeChange");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateMode();
            }
        }, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        CheckBox cb = findViewById(R.id.checkBox);
        cb.setChecked(options.getBoolean("onBoot", false));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                options.edit().putBoolean("onBoot", b).apply();
            }
        });
        cb = findViewById(R.id.checkBoxLL);
        if (null != getLLIntent(this)) {
            cb.setVisibility(View.VISIBLE);
            cb.setChecked(options.getBoolean("ll", false));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    options.edit().putBoolean("ll", b).apply();
                }
            });
        }
        else {
            cb.setVisibility(View.GONE);
        }
        updateMode();
    }

    private void updateMode() {
        TextView tv = (TextView)findViewById(R.id.accssibilityMode);
        if (!AccessibilityService.supportedLanguage()) {
            tv.setVisibility(View.GONE);
            return;
        }

        if (AccessibilityService.getInstance() == null) {
            tv.setText("Current mode: launch. To switch to kill mode, you need to activate "+
                    "OldUI's accessibility service. Click on 'Launch Android Settings', then 'Open', "+
                    "then scroll to Accessibility, and activate OldUI's accessibility service.");
        }
        else {
            tv.setText("Current mode: kill. To switch to launch mode, you need to deactivate "+
                    "OldUI's accessibility service. Click on 'Launch Android Settings', then 'Open', "+
                    "then scroll to Accessibility, and deactivate OldUI's accessibility service.");
        }
    }

    public void go(View view) {
        startAnytimeUI(this);
    }

    public void settings(View view) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + SETTINGS));
        i.setPackage(SETTINGS);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(i);
    }
}
