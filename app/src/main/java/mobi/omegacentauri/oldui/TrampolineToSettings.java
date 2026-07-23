package mobi.omegacentauri.oldui;

import static mobi.omegacentauri.oldui.oldui.SETTINGS;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class TrampolineToSettings extends Activity {
    boolean launched = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!launched) {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + "com.oculus.store"));
            i.setPackage(SETTINGS);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            launched = true;
        }
        else {
            finishAndRemoveTask();
        }
    }
}
