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
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class oldui extends Activity {

    public static final String OPTION_ONBOOT_LL = "ll";
    public static final String OPTION_BOOT_FIX = "bootfix";
    private SharedPreferences options;
    static final String SETTINGS = "com.android.settings";
    static final String OPTION_ONBOOT_OLDUI = "onBootOldUI";
    static final String OPTION_MUTE_HOME = "muteHome";

    public static void startAnytimeUI(Context context) {
        String pkg = "com.oculus.systemux";
        String cls = "com.oculus.panelapp.anytimeui.AnytimeUIActivity";
        Intent i = new Intent();
        i.setComponent(new ComponentName(pkg, cls));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
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
            Log.v("OldUI", "running "+i.toString());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
        else {
            Log.v("OldUI", "cannot find LL");
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
        cb.setChecked(options.getBoolean(OPTION_ONBOOT_OLDUI, false));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                options.edit().putBoolean(OPTION_ONBOOT_OLDUI, b).apply();
            }
        });
/*        cb = findViewById(R.id.closeStore);
        cb.setChecked(options.getBoolean("closeStore", false));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                options.edit().putBoolean("closeStore", b).apply();
            }
        }); */
        cb = findViewById(R.id.checkBoxLL);
        if (null != getLLIntent(this)) {
            cb.setVisibility(View.VISIBLE);
            cb.setChecked(options.getBoolean(OPTION_ONBOOT_LL, false)&&Settings.canDrawOverlays(oldui.this));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    options.edit().putBoolean(OPTION_ONBOOT_LL, b).apply();
                    if (b && !Settings.canDrawOverlays(oldui.this)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 0);
                    }
                }
            });
        }
        else {
            cb.setVisibility(View.GONE);
        }
        cb = findViewById(R.id.muteHome);
        cb.setChecked(options.getBoolean(OPTION_MUTE_HOME, false));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                options.edit().putBoolean(OPTION_MUTE_HOME, b).apply();
            }
        });
        cb = findViewById(R.id.bootFix);
        cb.setChecked(options.getBoolean(OPTION_BOOT_FIX, false));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                options.edit().putBoolean(OPTION_BOOT_FIX, b).apply();
            }
        });

        updateMode();
    }

    private void updateMode() {
        TextView tv = (TextView)findViewById(R.id.bootFixAccessibility);
        if (BootStartFix.getInstance() == null) {
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
        }

        TextView mw = (TextView)findViewById(R.id.muteWork);
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(android.Manifest.permission.READ_LOGS)) {
            mw.setVisibility(View.VISIBLE);
        }
        else {
            mw.setVisibility(View.GONE);
        }

        if (options.getBoolean(OPTION_MUTE_HOME, false )) {
            activateMonitoring(this, false);
        }
        else {
            deactivateMonitoring(this);
        }
    }

    public void go(View view) {
        startAnytimeUI(this);
    }

    public void settings(View view) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + SETTINGS));
        i.setPackage(SETTINGS);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(i);
    }

    public static void activateMonitoring(Context context, boolean boot) {
        Intent serviceIntent = new Intent(context, Monitoring.class);
        context.stopService(serviceIntent);
        serviceIntent.putExtra("boot", boot);
        context.startForegroundService(serviceIntent);
        Log.v("OldUI", "activate monitoring");
    }

    static private void deactivateMonitoring(Context context) {
        Intent serviceIntent = new Intent(context, Monitoring.class);
        context.stopService(serviceIntent);
        Log.v("OldUI", "deactivate monitoring");
    }
}
