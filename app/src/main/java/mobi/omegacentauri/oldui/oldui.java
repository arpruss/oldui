package mobi.omegacentauri.oldui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startAnytimeUI(this);
        finish();
    }
}
