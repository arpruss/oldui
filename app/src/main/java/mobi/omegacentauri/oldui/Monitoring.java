package mobi.omegacentauri.oldui;

import static mobi.omegacentauri.oldui.oldui.startAnytimeUI;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.omegacentauri.oldui.oldui;

public class Monitoring extends Service {
    private static final String CHANNEL_ID = "AppMonitorChannel";
    private boolean isMonitoring = false;
    private String lastPackageName = "";
    private NotificationChannel mChannel;
    private SharedPreferences options;
    private int savedVolume = 8;
    private boolean launchLL;
    private boolean muteHome;
    private boolean onBootOldUI;
    private boolean bootFix;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        options = PreferenceManager.getDefaultSharedPreferences(this);
        savedVolume = options.getInt("savedVolume", 8);
    }

    private void setMedia(boolean state) {
        int volumeLevel;

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (audioManager != null) {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (!state) {
                if (currentVolume==0)
                    return;
                savedVolume = currentVolume;
                options.edit().putInt("savedVolume", savedVolume).apply();
                Log.v("MuteHome", "Saving "+savedVolume);
                volumeLevel = 0;
            }
            else {
                if (currentVolume != 0)
                    return;
                Log.v("MuteHome", "Restoring "+savedVolume);
                volumeLevel = savedVolume;
            }

            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC, // The Media stream
                    volumeLevel,               // The volume index
                    0                          // Flags (e.g., AudioManager.FLAG_SHOW_UI)
            );
        }
    }

    @SuppressLint({"ForegroundServiceType", "WrongConstant"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String channelId = "oldui_channel";
        Notification.Builder nb;
        Log.v("OldUI", "onStartCommand");

        muteHome = options.getBoolean(oldui.OPTION_MUTE_HOME, false) && PackageManager.PERMISSION_GRANTED == checkSelfPermission(android.Manifest.permission.READ_LOGS);
        if (intent.getBooleanExtra("boot", false)) {
            bootFix = options.getBoolean(oldui.OPTION_BOOT_FIX, false) && BootStartFix.getInstance() != null;
            onBootOldUI = options.getBoolean(oldui.OPTION_ONBOOT_OLDUI, false);
            launchLL = options.getBoolean(oldui.OPTION_ONBOOT_LL, false);
        }
        else {
            bootFix = false;
            onBootOldUI = false;
            launchLL = false;
        }

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nb = new Notification.Builder(this, channelId);
            mChannel = new NotificationChannel(channelId, "Mute Home", NotificationManager.IMPORTANCE_LOW);
            // Configure the notification channel.
            mChannel.setDescription("Mute Home monitoring");
            mChannel.enableLights(false);
            mChannel.setVibrationPattern(null);
            mNotificationManager.createNotificationChannel(mChannel);
            nb.setChannelId(channelId);
        }
        else {
            nb = new Notification.Builder(this);
        }
        nb.setOngoing(true);
        Intent activityIntent = new Intent(this, oldui.class);
        nb.setContentIntent(PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE));
        nb.setContentText("Monitoring app starts/stops");
        nb.setSmallIcon(R.drawable.updown);
        nb.setContentTitle("OldUI");
        Notification notification = nb.build();
        if (notification == null) {
            Log.e("OldUI", "null notification");
            // don't know what to do or how it can happen
        }
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(startId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);//0x40000000
        }
        else {
            startForeground(startId, notification);
        }

        if (!isMonitoring && muteHome) {
            isMonitoring = true;
            startLogcatThread();
        }

        otherStuff();

        return START_STICKY;
    }

    PowerManager.WakeLock wakeLock = null;
    private static final int RUN_NONE = 0;
    private static final int RUN_LL = 1;
    private static final int RUN_OLD_UI = 2;
    private static final int RUN_BOOT_FIX_1 = 3;
    private static final int RUN_BOOT_FIX_2 = 4;
    private static final int RUN_BOOT_FIX_3 = 5;
    private static final int RUN_END = -1;
    TimerTask runner;

    static class DelayedAction {
        int action;
        int delay;

        public DelayedAction(int action, int delay) {
            this.action = action;
            this.delay = delay;
        }
    };

    LinkedList<DelayedAction> actions;

    private void otherStuff() {
        Log.v("OldUI", "otherStuff");

        actions = new LinkedList<>();
        if (launchLL) {
            actions.add(new DelayedAction(RUN_LL, 100));
        }
        if (onBootOldUI) {
            actions.add(new DelayedAction(RUN_OLD_UI, 500));
        }
        if (bootFix) {
            actions.add(new DelayedAction(RUN_BOOT_FIX_1, 1000));
            actions.add(new DelayedAction(RUN_BOOT_FIX_2, 1500));
            actions.add(new DelayedAction(RUN_BOOT_FIX_3, 1000));
        }
        actions.add(new DelayedAction(RUN_END, 500));
        Log.v("OldUI", "actions "+actions.size());
        if (!actions.isEmpty()) {
            ActionTask runner = new ActionTask(this);
            new Timer().schedule(runner, actions.getFirst().delay);
        }
    }

    private void startLogcatThread() {
        new Thread(() -> {
            try {
                // Pre-compile regex for performance
                // Matches "Displayed com.package.name"
                Pattern homePattern = Pattern.compile("wm_on_(start|stop)_called: .*com\\.oculus\\.vrshell\\.HomeActivity");

                // Clear logcat buffer first to avoid old logs triggering events
                Runtime.getRuntime().exec("logcat -c");

                // Read the ActivityTaskManager logs
                Process process = Runtime.getRuntime().exec("logcat -b events");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = null;
                Log.v("OldUI", "monitoring "+isMonitoring);
                while (isMonitoring && (line = reader.readLine()) != null) {
                    Matcher matcher = homePattern.matcher(line);
                    if (muteHome && matcher.find()) {
                        String packageName = matcher.group(1);
                        Log.v("OldUI", "matched home toggele "+packageName);
                        handleHomeDetected(packageName.equals("stop"));
                    }
                }
            } catch (Exception e) {
                Log.e("OldUI", "Error reading logcat", e);
            }
        }).start();
    }

    private void handleHomeDetected(boolean stop) {
        Log.v("OldUI", "value "+stop);
        setMedia(stop);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.v("OldUI", "monitoring destroyed");
        isMonitoring = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class ActionTask extends TimerTask {
        Context context;

        public ActionTask(Context c) {
            context = c;
        }
        @Override
        public void run() {
            try {
                int a = actions.removeFirst().action;
                switch (a) {
                    case RUN_LL:
                        Log.v("OldUI", "starting LL");
                        oldui.startLL(context);
                        break;
                    case RUN_OLD_UI:
                        Log.v("OldUI", "starting old UI");
                        startAnytimeUI(context);
                        break;
                    case RUN_BOOT_FIX_1:
                        Log.v("OldUI", "boot fix 1");
                        BootStartFix.globalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
                        break;
                    case RUN_BOOT_FIX_2:
                        Log.v("OldUI", "boot fix 2");
                        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                        wakeLock = powerManager.newWakeLock(
                                PowerManager.FULL_WAKE_LOCK |
                                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                        PowerManager.ON_AFTER_RELEASE,
                                "MyApp:WakeUpTag"
                        );

                        wakeLock.acquire(5000);
                        break;
                    case RUN_BOOT_FIX_3:
                        Log.v("OldUI", "boot fix 3");
                        if (wakeLock != null) {
                            wakeLock.release();
                            wakeLock = null;
                        }
                        break;
                    case RUN_END:
                        if (!muteHome) {
                            Log.v("OldUI", "closing service");
                            stopSelf();
                        }
                    default:
                        break;
                }

            } catch (NoSuchElementException e) {
            }

            if (!actions.isEmpty()) {
                ActionTask runner = new ActionTask(Monitoring.this);
                new Timer().schedule(runner, actions.getFirst().delay);
            }
        }

    }


}