package ru.microsave.tempmonitor;
/*
Шедулер для периодического сообщения об уровне темпертатуры
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class JobSchedulerService extends JobService implements SensorEventListener {
    private float tempSensor;
    private float tempBattery;
    private final String LOG_TAG = "myLogs";
    private boolean ifSensor;
    private boolean ifFlexTime;

    private String myApp;
    private String myNumber;
    private long myNormalInterval;

    private long mCurrentTime;
    private long mLastInfo;

    private int TASK_NUMBER;
    private static Sensor mJobSensorTemperature;
    private static SensorManager mJobSensorManager;


    public JobSchedulerService() {
    }

    @Override
    public void onCreate() {
        // Log.d(LOG_TAG, "JobSchedulerService onCreate");
        readSharedPreferences();
        this.mJobSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mJobSensorTemperature = mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobSensorManager.registerListener(this, mJobSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        myApp = getString(R.string.app_name);

        Log.d(LOG_TAG, "JobSchedulerService: onCreate");

//        Notification.Builder builder = new Notification.Builder(this)
//                .setSmallIcon(R.drawable.ic_android_black_24dp);
//        Notification notification;
//        if (Build.VERSION.SDK_INT < 16)
//            notification = builder.getNotification();
//        else
//            notification = builder.build();
//        startForeground(100500777, notification);


//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "M_CH_ID");
//
//        notificationBuilder.setAutoCancel(true)
//                .setDefaults(Notification.DEFAULT_ALL)
//                .setWhen(System.currentTimeMillis())
//                .setSmallIcon(R.drawable.ic_android_black_24dp)
//                .setTicker("Hearty365")
//                .setPriority(Notification.PRIORITY_MAX) // this is deprecated in API 26 but you can still use for below 26. check below update for 26 API
//                .setContentTitle("Default notification")
//                .setContentText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
//                .setContentInfo("Info");
//
//        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(1, notificationBuilder.build());
//
//
        //=============================


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_MAX);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_android_black_24dp)
                .setTicker("Hearty365")
                //     .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("Default notification")
                .setContentText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
                .setContentInfo("Info");

        notificationManager.notify(/*notification id*/1, notificationBuilder.build());
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        tempSensor = sensorEvent.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG,"JobSchedulerService, Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters param) {
        readSharedPreferences();
        batteryTemperature();
        mCurrentTime = System.currentTimeMillis();
        //    Log.d(LOG_TAG, "mCurrentTime 1: " + mCurrentTime);
        // Log.d(LOG_TAG, "myNormalInterval 1: " + mCurrentTime + " - " + mLastInfo + " = " +  (mCurrentTime - mLastInfo)/1000/60 + " ? " + myNormalInterval/1000/60);
        //Log.d(LOG_TAG, "mLastInfo 1: " + mLastInfo);
        //Log.d(LOG_TAG, "myNormalInterval 1: " + myNormalInterval);
        // Log.d(LOG_TAG, "myNormalInterval 1: " + mCurrentTime + " - " + mLastInfo + " = " + (mCurrentTime - mLastInfo) + " ? " + myNormalInterval * 1000 * 60 * 60 );

        // не использую нигде
        // String timestamp = DateFormat.getDateTimeInstance().format(new Date(mCurrentTime));

        // При старте всегда равно нулю, можно добавить поправку, в размере интервала, иначе первый тест пропускается
        // Не надо поправку, некорректно отсчитывается
        if (mLastInfo == 0) {
            mLastInfo = mCurrentTime;
            // Log.d(LOG_TAG, "mLastInfo 2: " + mLastInfo);
            saveSharedPreferences();
        }

        // =======================================================================================
        // Это блок для регулярных периодических сообщений
        // Если FlexTime то время не проверяем!
        if (ifFlexTime) {
            if ((mCurrentTime - mLastInfo) > (myNormalInterval * 1000L * 60L * 15L)) {
                Log.d(LOG_TAG, "1. Info (mCurrentTime - mLastInfo) = " + ((mCurrentTime - mLastInfo)/1000L/60L));
                mLastInfo = mCurrentTime - (1000L * 60L * 5L); // Новый таймштамп

                if (ifSensor) {
                    // Log.d(LOG_TAG, "new: JobInfoSensor");
                    // TODO: 12.11.2022 КОСТЫЛЬ - ИНОГДА СЕНСОР ОТДАТ НОЛЬ НЕПОНЯТНО ПОЧЕМУ
                    //    if (tempSensor == 0) tempSensor = tempBattery;
                    ++TASK_NUMBER;
                    saveSharedPreferences();
                    new JobInfoSensor(this, myNumber, tempSensor, tempBattery, TASK_NUMBER, myApp).execute(param);
                } else {
                    // Log.d(LOG_TAG, "new: JobInfoBattery");
                    ++TASK_NUMBER;
                    saveSharedPreferences();
                    new JobInfoBattery(this, myNumber, tempBattery, TASK_NUMBER, myApp).execute(param);
                }
            }
        } else {
            // Проверка времени для старых устройств (в миллисекундах!)
            if ((mCurrentTime - mLastInfo)  > myNormalInterval * 1000L * 60L * 60L) {
                Log.d(LOG_TAG, "2. Info (mCurrentTime - mLastInfo) = " + ((mCurrentTime - mLastInfo)/1000L/60L));

                mLastInfo = mCurrentTime - (1000L * 60L * 5L); // Новый таймштамп

                if (ifSensor) {
                    // Log.d(LOG_TAG, "new: JobInfoSensor");
                    // Log.d(LOG_TAG, "new: JobInfoSensor");
                    // TODO: 12.11.2022 КОСТЫЛЬ - ИНОГДА СЕНСОР ОТДАТ НОЛЬ НЕПОНЯТНО ПОЧЕМУ
                    //    if (tempSensor == 0) tempSensor = tempBattery;

                    ++TASK_NUMBER;
                    saveSharedPreferences();
                    new JobInfoSensor(this, myNumber, tempSensor, tempBattery, TASK_NUMBER, myApp).execute(param);
                } else {
                    // Log.d(LOG_TAG, "new: JobInfoBattery");
                    ++TASK_NUMBER;
                    saveSharedPreferences();
                    new JobInfoBattery(this, myNumber, tempBattery, TASK_NUMBER, myApp).execute(param);
                }
            }
        }

        saveSharedPreferences();
        // =======================================================================================
        // job not really finished here but we assume success & prevent backoff procedures, wakelocking, etc.
       // jobFinished(param, true);

        // false - не требуется ручной вызов jobFinished, true - будет вызван вручную
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
         Log.d(LOG_TAG, "--- onStopJob --- return true --- СЕРВИС НОРМАЛ ОСТАНОВЛЕН!!!!!!!!!");
        // Log.d(LOG_TAG, "onStopJob() called with: params = [" + params + "]");

        // true - говорит о том что служба может повторяться (будет перезапущена)
        return true;
    }

    @Override
    public void onDestroy() {
        //    stopService(new Intent(this, JobSchedulerService.class));
        try {
            if (mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
                mJobSensorManager.unregisterListener(this);
                //    Log.d(LOG_TAG, "mJobSensorManager.unregisterListener");
            }

        } catch (Exception e) {
            Log.d(LOG_TAG, "mJobSensorManager.unregisterListener = null");
            e.printStackTrace();
        }
    }

    private void readSharedPreferences() {
        SharedPreferences saveJobPref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        myNumber = (saveJobPref.getString("NUMBER", "+7123456789"));
        myNormalInterval = (saveJobPref.getLong("NORMAL_INTERVAL", 6));
        ifSensor = (saveJobPref.getBoolean("IFSENSOR", true));
        ifFlexTime = (saveJobPref.getBoolean("USE_FLEX_TIME", true));
        mLastInfo = (saveJobPref.getLong("LAST_INFO", 0));
        TASK_NUMBER = (saveJobPref.getInt("TASK_NUMBER", 0));
        // Log.d(LOG_TAG, "readSharedPreferences: OK");
    }


    private void saveSharedPreferences() {
        SharedPreferences savePref;
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        SharedPreferences.Editor ed = savePref.edit();

        ed.putLong("LAST_INFO", mLastInfo);
        ed.putInt("TASK_NUMBER", TASK_NUMBER);
        ed.apply();
    }

    public float batteryTemperature() {
        Log.d(LOG_TAG, "JobSchedulerService.batteryTemperature working");
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        tempBattery = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10; // Почему разделил на 10??? Да почему то выдача идет в 10 раз больше
        return tempBattery;
    }
}