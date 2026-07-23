package mobi.omegacentauri.oldui;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
    private boolean launchLL = false;
    private boolean muteHome = false;
    private boolean launchOldUI = false;

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

        launchLL = options.getBoolean(oldui.OPTION_ONBOOT_LL, false) && intent.getBooleanExtra("boot", false);
        muteHome = options.getBoolean(oldui.OPTION_MUTE_HOME, false);
        launchOldUI = options.getBoolean(oldui.OPTION_ONBOOT_OLDUI, false) && AccessibilityService.getInstance() == null;

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
            Log.e("mutehome", "null notification");
            // don't know what to do or how it can happen
        }
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(startId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);//0x40000000
        }
        else {
            startForeground(startId, notification);
        }

        if (!isMonitoring) {
            isMonitoring = true;
            startLogcatThread();
        }

        return START_STICKY;
    }

    private void startLogcatThread() {
        new Thread(() -> {
            try {
                // Pre-compile regex for performance
                // Matches "Displayed com.package.name"
                Pattern homePattern = Pattern.compile("wm_on_(start|stop)_called: .*com\\.oculus\\.vrshell\\.HomeActivity");
                Pattern storePattern = Pattern.compile("wm_on_start_called: .*com\\.oculus\\.store");

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
                    if (launchLL || launchOldUI) {
                        matcher = storePattern.matcher(line);
                        if (matcher.find()) {
                            Log.v("OldUI", "matched store launch");
                            if (launchLL) {
                                launchLL = false;
                                oldui.startLL(this);
                            }
                            if (launchOldUI) {
                                launchOldUI = false;
                                oldui.startAnytimeUI(this);
                            }
                            if (!muteHome) {
                                Log.v("OldUI", "no need for more monitoring");
                                isMonitoring = false;
                                stopSelf();
                            }
                        }
                    }
                }
                Log.v("OldUI", "monitoring done "+(line==null)+" "+isMonitoring);
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
}