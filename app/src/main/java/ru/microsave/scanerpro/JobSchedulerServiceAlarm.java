package ru.microsave.scanerpro;
/**
 * Шедулер для периодического контроля аварийной темпертатуры
 *
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import androidx.work.Configuration;

public class JobSchedulerServiceAlarm  extends JobService implements SensorEventListener {
    private float tempSensor;
    private float tempBattery;
    private final String LOG_TAG = "myLogs";
    private boolean ifSensor;
    private boolean ifFlexTime;

    private String myApp;
    private String myNumber;
    private int myWarningTemperature;

    private long myAlarmInterval;

    private long mCurrentTime;
    private long mLastAlarm;

    private int TASK_NUMBER;
    private static Sensor mJobAlarmSensorTemperature;
    private static SensorManager mJobAlarmSensorManager;

    public JobSchedulerServiceAlarm() {
        Configuration.Builder builder = new Configuration.Builder();
        builder.setJobSchedulerJobIdRange(2001, 3001);
    }

    @Override
    public void onCreate() {
        // Log.d(LOG_TAG, "JobSchedulerServiceAlarm onCreate");
        readSharedPreferences();
        mJobAlarmSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mJobAlarmSensorTemperature = mJobAlarmSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobAlarmSensorManager.registerListener(this, mJobAlarmSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        myApp = getString(R.string.app_name);

        Log.d(LOG_TAG, "JobSchedulerServiceAlarm: onCreate");

//        Notification.Builder builder = new Notification.Builder(this)
//                .setSmallIcon(R.drawable.ic_android_black_24dp);
//        Notification notification;
//        if (Build.VERSION.SDK_INT < 16)
//            notification = builder.getNotification();
//        else
//            notification = builder.build();
//        startForeground(100500778, notification);
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
        Log.d(LOG_TAG,"JobSchedulerServiceAlarm, Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters param) {
        readSharedPreferences();
        batteryTemperature();
        mCurrentTime = System.currentTimeMillis();

        // При старте всегда равно нулю (обнуляется по кнопке Stop)
        if (mLastAlarm == 0) {
            mLastAlarm = mCurrentTime;
            saveSharedPreferences();
        }

        // =======================================================================================
        if ((mCurrentTime - mLastAlarm) > myAlarmInterval * 1000L * 60L * 60L) {
            Log.d(LOG_TAG, "Alarm check: interval passed = " + ((mCurrentTime - mLastAlarm) / 1000L / 60L) + " мин.");
            mLastAlarm = mCurrentTime - (1000L * 3L); // Новый таймштамп

            // Лог для диагностики
            Log.d(LOG_TAG, "Alarm check values: tempSensor=" + tempSensor + ", tempBattery=" + tempBattery + ", warning=" + myWarningTemperature);

            // Для сенсора: игнорируем дефолтный ноль
            if (ifSensor && tempSensor > 0 && tempSensor <= myWarningTemperature) {
                ++TASK_NUMBER;
                saveSharedPreferences();
                new JobAlarmSensor(this, myNumber, tempSensor, tempBattery, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
            }

            // Фолбэк: если сенсор есть, но значение 0 (не обновился), проверим батарею
            else if (ifSensor && tempSensor <= 0) {
                Log.d(LOG_TAG, "Сенсор вернул 0, fallback на батарею");
                if (tempBattery <= myWarningTemperature) {
                    ++TASK_NUMBER;
                    saveSharedPreferences();
                    new JobAlarmBattery(this, myNumber, tempBattery, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
                }
            }

            // Если сенсора нет — работаем только по батарее
            else if (!ifSensor && tempBattery <= myWarningTemperature) {
                ++TASK_NUMBER;
                saveSharedPreferences();
                new JobAlarmBattery(this, myNumber, tempBattery, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
            }
        }

        saveSharedPreferences();
        // =======================================================================================
        // job not really finished here but we assume success & prevent backoff procedures, wakelocking, etc.
        // jobFinished(param, true);
        return false;
    }


    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(LOG_TAG, "--- onStopJob --- return true --- ALARM СЕРВИС ОСТАНОВЛЕН!!!!!!!!!");
        return true;
    }

    @Override
    public void onDestroy() {
        //    stopService(new Intent(this, JobSchedulerServiceAlarm.class));
        try {
            if (mJobAlarmSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
                mJobAlarmSensorManager.unregisterListener(this);
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
        myWarningTemperature = (saveJobPref.getInt("WARNING", 5));

        myAlarmInterval = (saveJobPref.getLong("ALARM_INTERVAL", 1));
        ifSensor = (saveJobPref.getBoolean("IFSENSOR", true));
        ifFlexTime = (saveJobPref.getBoolean("USE_FLEX_TIME", true));
        mLastAlarm = (saveJobPref.getLong("LAST_ALARM", 0));
        TASK_NUMBER = (saveJobPref.getInt("TASK_NUMBER", 0));
        // Log.d(LOG_TAG, "readSharedPreferences: OK");
    }


    private void saveSharedPreferences() {
        SharedPreferences savePref;
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        SharedPreferences.Editor ed = savePref.edit();

        ed.putLong("LAST_ALARM", mLastAlarm);
        ed.putInt("TASK_NUMBER", TASK_NUMBER);
        ed.apply();
    }

    public float batteryTemperature() {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        tempBattery = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10; // Почему разделил на 10??? Да почему то выдача идет в 10 раз больше
        return tempBattery;
    }
}