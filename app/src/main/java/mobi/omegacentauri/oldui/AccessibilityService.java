package mobi.omegacentauri.oldui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    private static AccessibilityService instance = null;
    public static int state = -1;
    static final int WAITING_FOR_STORAGE = 1;
    static final int WAITING_FOR_DELETE = 2;
    static final int DISABLED = -1;
    public long startTime = 0;
    public long lastRestart = -100000;
    public boolean launchLL = false;
    private SharedPreferences options;
    private boolean useEnglish;

    public static AccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        options = PreferenceManager.getDefaultSharedPreferences(this);
        if (! supportedLanguage()) {
            instance = null;
            state = -1;
        }
        else {
            instance = this;
        }
        Intent intent = new Intent("mobi.omegacentauri.oldui.modeChange");
        sendBroadcast(intent);
        useEnglish = Locale.getDefault().getLanguage().equals("en");
        Log.v("OldUI", "english "+useEnglish);
    }

    public static boolean supportedLanguage() {
        return true; // Locale.getDefault().getLanguage().equals("en");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        state = 0;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        Intent i = new Intent("mobi.omegacentauri.oldui.modeChange");
        sendBroadcast(i);
        return super.onUnbind(intent);
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (state < 0 || instance == null) {
//            disableSelf();
            return;
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (startTime != 0 && System.currentTimeMillis() > startTime + 8000) {
                state = -1;
                return;
            }
            String appName = event.getPackageName().toString();
            if (appName.equals("com.android.settings")) {
                String text = event.getText().toString();
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode == null) return;
                Log.v("OldUI", "app:"+appName+" "+rootNode.getPackageName()+ " "+text+(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)+ " "+state );
                if (! rootNode.getPackageName().equals("com.android.settings")) {
                    if (lastRestart + 1000 < System.currentTimeMillis()) {
                        state = 0;
                        startTime = 0;
                        lastRestart = System.currentTimeMillis();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.parse("package:" + "com.oculus.vrshell");
                        intent.setData(uri);
                        intent.setPackage("com.android.settings");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        Log.v("OldUI", "restarting settings");
                    }
                    return;
                }

                AccessibilityNodeInfo n = findNode(rootNode, "Meta Horizon Shell");
                if (n != null) {
                    Log.v("OldUI", "app info");

                    if (useEnglish) {
                        AccessibilityNodeInfo s = findNode(rootNode, "Storage & cache");
                        if (s == null)
                            s = findNode(rootNode, "Storage and cache");

                        if (s != null && click(s)) {
                            Log.v("OldUI", "storage click");
                            startTime = System.currentTimeMillis();
                            state = WAITING_FOR_STORAGE;
                        } else {
                            Log.v("OldUI", "cannot find Storage & cache");
                        }
                    }
                    else {
                        List<AccessibilityNodeInfo> clickables = new ArrayList<>();
                        getClickables(clickables, rootNode);
                        Log.v("OldUI", "clickable count: "+clickables.size());
                        if (clickables.size() == 7) {
                            Log.v("OldUI", "storage click");
                            startTime = System.currentTimeMillis();
                            state = WAITING_FOR_STORAGE;
                            clickables.get(5).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            findNode(rootNode, "ajfj");
                        }
                    }
                }
                else if (state == WAITING_FOR_STORAGE &&
                        event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                        (!useEnglish || text.equals("[Storage]"))
                ) {
                    Log.v("OldUI", "storage check");
                    if (useEnglish) {
                        AccessibilityNodeInfo c = findNode(rootNode, "CLEAR STORAGE");
                        if (c != null && click(c)) {
                             Log.v("OldUI", "clear click");
                            c.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            state = WAITING_FOR_DELETE;
                        } else {
                            Log.v("OldUI", "cannot find CLEAR STORAGE");
                        }
                    }
                    else {
                        List<AccessibilityNodeInfo> clickables = new ArrayList<>();
                        getClickables(clickables, rootNode);
                        Log.v("OldUI", "clickable count: "+clickables.size());
                        if (clickables.size() == 4) {
                            Log.v("OldUI", "clear click");
                            startTime = System.currentTimeMillis();
                            state = WAITING_FOR_DELETE;
                            clickables.get(2).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
                else if (state == WAITING_FOR_DELETE &&
                        event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                        (!useEnglish || text.startsWith("[Delete app data?")) ) {
                    Log.v("OldUI", "delete check");

                    if (useEnglish) {
                        AccessibilityNodeInfo d = findNode(rootNode, "DELETE");
                        if (d != null && click(d)) {
                            Log.v("OldUI", "delete click");
                            state = -1;
                            runLLIfNeeded();
                        } else {
                            Log.v("OldUI", "cannot find DELETE");
                        }
                    }
                    else {
                        List<AccessibilityNodeInfo> clickables = new ArrayList<>();
                        getClickables(clickables, rootNode);
                        Log.v("OldUI", "cickables size "+clickables.size());
                        if (clickables.size() == 2) {
                            Log.v("OldUI", "delete click");
                            clickables.get(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            state = -1;
                            runLLIfNeeded();
                        }
                    }
                }
                else {
                    Log.v("OldUI", "doesn't match");
                }

                rootNode.recycle();
            }
        }
    }

    private void runLLIfNeeded() {
        if (options.getBoolean("ll", false)) {
            Log.v("OldUI", "need to start LL");
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.v("OldUI", "starting LL");
                    PackageManager packageManager = getPackageManager();
                    Intent i;
                    i = packageManager.getLaunchIntentForPackage("com.threethan.launcher");
                    if (i == null)
                        i = packageManager.getLaunchIntentForPackage("com.threethan.launcher.metastore");
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            }, 3000);
        }
    }

    private void getClickables(List<AccessibilityNodeInfo> list, AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.isClickable())
            list.add(node);

        for (int i = 0; i < node.getChildCount(); i++) {
            getClickables(list, node.getChild(i));
        }
    }

    private boolean click(AccessibilityNodeInfo s) {
        AccessibilityNodeInfo clickableNode = s;

        // 3. Walk up the view tree until we find the layout that is actually clickable
        while (clickableNode != null && !clickableNode.isClickable()) {
            clickableNode = clickableNode.getParent();
        }

        // 4. Perform the click
        if (clickableNode != null) {
            clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }

        return false;
    }

    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo node, String toFind) {
        if (node == null) return null;

        String text = node.getText() != null ? node.getText().toString() : "";
        if (node.isClickable())
            Log.v("OldUI", "***");
        Log.v("OldUI", "::"+text);

        // Only log nodes that actually contain some text or description
        if (text.equals(toFind)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo n = findNode(node.getChild(i), toFind);
            if (null != n)
                return n;
        }

        return null;
    }
    @Override
    public void onInterrupt() {

    }
}
