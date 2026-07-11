package mobi.omegacentauri.oldui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

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
    }

    public static Intent getLLIntent(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent i;
        i = packageManager.getLaunchIntentForPackage("com.threethan.launcher");
        if (i != null)
            return i;
        i = packageManager.getLaunchIntentForPackage("com.threethan.launcher.metastore");
        Log.v("ARP", "i "+(i!=null));
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
    }

    @Override
    public void onStart() {
        super.onStart();
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
    }

    public void go(View view) {
        startAnytimeUI(this);
    }
}
