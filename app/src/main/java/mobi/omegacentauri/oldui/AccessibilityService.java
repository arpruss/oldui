package mobi.omegacentauri.oldui;

// TODO: close store

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    private static AccessibilityService instance = null;
    public static int state = -1;
    static final int WAITING_FOR_SHELL_SETTINGS = 0;
    static final int WAITING_FOR_STORAGE = 1;
    static final int WAITING_FOR_DELETE = 2;
    static final int WAITING_FOR_STORE = 3;
    static final int WAITING_FOR_STORE_SETTINGS = 4;
    static final int WAITING_FOR_STOP_CONFIRM = 5;
    static final int WAITING_FOR_STORE_SETTINGS2 = 6;
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
        state = -1;
        options = PreferenceManager.getDefaultSharedPreferences(this);
        if (! supportedLanguage()) {
            instance = null;
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
        state = -1;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        Intent i = new Intent("mobi.omegacentauri.oldui.modeChange");
        sendBroadcast(i);
        return super.onUnbind(intent);
    }

    private void processStore(AccessibilityEvent event) {
        if (!useEnglish)
            return; // TODO: work without English
        if (state == WAITING_FOR_STORE) {
            List< AccessibilityWindowInfo> ww = getWindows();
            Log.v("OldUI", "checking windows "+ww.size());

            for (AccessibilityWindowInfo w : ww) {
                AccessibilityNodeInfo r = w.getRoot();
                if (r != null) {
                    String pkg = r.getPackageName().toString();
                    Log.v("OldUI", pkg);
                    if (pkg.equals("com.oculus.store")) {
                        Log.v("OldUI", "found store");
                        Intent intent = new Intent(this, TrampolineToSettings.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        state = WAITING_FOR_STORE_SETTINGS;
                        r.recycle();
                        return;
                    }
                    r.recycle();
                }
            }
            return;
        }
        String appName = event.getPackageName().toString();
        if (!appName.equals("com.android.settings"))
            return;
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        AccessibilityNodeInfo n = findNode(rootNode, "Store");
        String text = event.getText().toString();
        Log.v("OldUI", "store close: "+text);
        if (n != null) {
            Log.v("OldUI", "app info");

            AccessibilityNodeInfo s = findNode(rootNode, "Force stop");

            if (state == WAITING_FOR_STORE_SETTINGS) {
                if (s != null && click(s)) {
                    Log.v("OldUI", "stop click");
                    startTime = System.currentTimeMillis();
                    state = WAITING_FOR_STOP_CONFIRM;
                } else {
                    Log.v("OldUI", "cannot find Force stop");
                }
            }
            else if (state == WAITING_FOR_STORE_SETTINGS2) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.v("OldUI", "closing ");
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    }
                }, 250);
                state = -1;
            }
        }
        else if (state == WAITING_FOR_STOP_CONFIRM &&
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                (!useEnglish || text.startsWith("[Force")) ) {
            Log.v("OldUI", "force check");

            AccessibilityNodeInfo d = findNode(rootNode, "OK");
            if (d != null && click(d)) {
                Log.v("OldUI", "ok click");
                state = WAITING_FOR_STORE_SETTINGS2;
            }
        }
        else {
            Log.v("OldUI", "doesn't match");
        }

        rootNode.recycle();


    }

    private void dumpWindows() {
        int i=0;
        int cCount = 0;
        for (AccessibilityWindowInfo w : getWindows()) {
            AccessibilityNodeInfo r = w.getRoot();
            if (r != null) {
                String pkg = r.getPackageName().toString();
                if (true || pkg.equals("com.android.settings")) {
  //                  Log.v("OldUI", w.getTitle().toString() + " -> " + pkg+" "+i);
                    i++;
                    List<AccessibilityNodeInfo> c = new ArrayList<AccessibilityNodeInfo>();
                    getClickables(c, r);
                    cCount += c.size();
                    for (AccessibilityNodeInfo a : c) {
                        Rect rr = new Rect();
                        a.getBoundsInScreen(rr);
//                        Log.v("OldUI", rr.toString());
                    }
                }
            }
        }
        Log.v("OldUI", "clickables "+cCount);

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.v("OldUI", event.getPackageName().toString());
//        dumpWindows();
//        if (true) return;
        if (state < 0 || instance == null) {
//            disableSelf();
            return;
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
/*            if (state >= WAITING_FOR_STORE) {
                processStore(event);
                return;
            } */
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
                    rootNode.recycle();
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
                            runLLIfNeeded();
/*                            if (options.getBoolean("closeStore", false)) {
                                Log.v("OldUI", "close store wait");
                                state = WAITING_FOR_STORE;
                            }
                            else */
                                state = -1;
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

    private boolean closeStore() {
        List< AccessibilityWindowInfo> ww = getWindows();
        Log.v("OldUI", "windows "+ww.size());

        Rect storeBounds = new Rect();
        AccessibilityNodeInfo store = null;
        AccessibilityNodeInfo ux = null;

        for (AccessibilityWindowInfo w : ww) {
            Log.v("OldUI", w.getTitle().toString());
            AccessibilityNodeInfo r = w.getRoot();
            if (r != null) {
                String pkg = r.getPackageName().toString();
                Log.v("OldUI", pkg);
                if (pkg.equals("com.oculus.store")) {
                    r.getBoundsInScreen(storeBounds);
                    store = r;
                    if (ux != null)
                        break;
                }
                else if (pkg.equals("com.oculus.systemux")) {
                    ux = r;
                    if (store != null)
                        break;
                }
            }
        }

        if (store != null && ux != null) {
            List<AccessibilityNodeInfo> clickables = new ArrayList<>();
            getClickables(clickables, ux);
            AccessibilityNodeInfo rightmost = null;
            int rightMostX = -storeBounds.left-100;
            Rect b = new Rect();
            for (AccessibilityNodeInfo c : clickables) {
                c.getBoundsInScreen(b);
                if (b.top <= storeBounds.bottom && (b.left+b.right)/2 >= storeBounds.left &&
                        (b.left+b.right)/2 < storeBounds.right) {
                    if (b.left > rightMostX) {
                        rightMostX = b.left;
                        rightmost = c;
                    }
                }
            }
            if (rightmost != null) {
                Log.v("OldUI", "closing store");
                rightmost.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        return false;
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

//        Log.v("OldUI", "node:"+(node.getText() == null?"":node.getText().toString())+" "+node.isClickable());
        if (node.isClickable()) {
            list.add(node);
        }

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
//        if (node.isClickable())
//            Log.v("OldUI", "***");
//        Log.v("OldUI", "::"+text);

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
