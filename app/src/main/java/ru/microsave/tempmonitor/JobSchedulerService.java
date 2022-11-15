package ru.microsave.tempmonitor;
/*

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
import android.util.Log;

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
        Log.d(LOG_TAG, "JobSchedulerService onCreate");
        readSharedPreferences();
        this.mJobSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mJobSensorTemperature = mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobSensorManager.registerListener(this, mJobSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        myApp = getString(R.string.app_name);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        tempSensor = sensorEvent.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public boolean onStartJob(JobParameters param) {
        readSharedPreferences();
        batteryTemperature();
        mCurrentTime = System.currentTimeMillis();
        //    Log.d(LOG_TAG, "mCurrentTime 1: " + mCurrentTime);
        // Log.d(LOG_TAG, "myNormalInterval 1: " + mCurrentTime + " - " + mLastInfo + " = " +  (mCurrentTime - mLastInfo)/1000/60 + " ? " + myNormalInterval/1000/60);
        Log.d(LOG_TAG, "mLastInfo 1: " + mLastInfo);
        Log.d(LOG_TAG, "myNormalInterval 1: " + myNormalInterval);
        Log.d(LOG_TAG, "myNormalInterval 1: " + mCurrentTime + " - " + mLastInfo + " = " + (mCurrentTime - mLastInfo) / 1000 / 60 / 60 + " ? " + myNormalInterval * 1000 * 60 * 60);

        // не использую нигде
        // String timestamp = DateFormat.getDateTimeInstance().format(new Date(mCurrentTime));

        // При старте всегда равно нулю, можно добавить поправку, в размере интервала, иначе первый тест пропускается
        // Не надо поправку, некорректно отсчитывается
        if (mLastInfo == 0) {
            mLastInfo = mCurrentTime;
            Log.d(LOG_TAG, "mLastInfo 2: " + mLastInfo);
            saveSharedPreferences();
        }

        // =======================================================================================
        // Это блок для регулярных периодических сообщений
        // Если FlexTime то время не проверяем!
        if (ifFlexTime) {
            ++TASK_NUMBER;
            if (ifSensor) {
                Log.d(LOG_TAG, "new: JobInfoSensor");

                // TODO: 12.11.2022 КОСТЫЛЬ - ИНОГДА СЕНСОР ОТДАТ НОЛЬ НЕПОНЯТНО ПОЧЕМУ
                if (tempSensor == 0) tempSensor = tempBattery;

                new JobInfoSensor(this, myNumber, tempSensor, tempBattery, TASK_NUMBER, myApp).execute(param);
            } else {
                Log.d(LOG_TAG, "new: JobInfoBattery");
                new JobInfoBattery(this, myNumber, tempBattery, TASK_NUMBER, myApp).execute(param);
            }

        } else {
            // Проверка времени для старых устройств (в минутах!)
            if ((mCurrentTime - mLastInfo)  / 1000 / 60 > myNormalInterval * 1000 * 60) {
                ++TASK_NUMBER;

                Log.d(LOG_TAG, "myNormalInterval 3: " + mCurrentTime + " - " + mLastInfo + " = " + (mCurrentTime - mLastInfo) / 1000 / 60 / 60 + " ? " + myNormalInterval * 1000 * 60 * 60);

                if (ifSensor) {
                    Log.d(LOG_TAG, "new: JobInfoSensor");

                    // TODO: 12.11.2022 КОСТЫЛЬ - ИНОГДА СЕНСОР ОТДАТ НОЛЬ НЕПОНЯТНО ПОЧЕМУ
                    if (tempSensor == 0) tempSensor = tempBattery;

                    new JobInfoSensor(this, myNumber, tempSensor, tempBattery, TASK_NUMBER, myApp).execute(param);
                } else {
                    Log.d(LOG_TAG, "new: JobInfoBattery");
                    new JobInfoBattery(this, myNumber, tempBattery, TASK_NUMBER, myApp).execute(param);
                }

                mLastInfo = mCurrentTime; // Новый таймштамп
            }
        }

        saveSharedPreferences();
        // =======================================================================================
        // job not really finished here but we assume success & prevent backoff procedures, wakelocking, etc.
        jobFinished(param, true);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(LOG_TAG, "--- onStopJob --- return true --- СЕРВИС НОРМАЛ ОСТАНОВЛЕН!!!!!!!!!");
        Log.d(LOG_TAG, "onStopJob() called with: params = [" + params + "]");
        return false;
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
        myNormalInterval = (saveJobPref.getInt("NORMAL_INTERVAL", 6));
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
        ed.commit();
    }

    public float batteryTemperature() {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        tempBattery = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10; // Почему разделил на 10??? Да почему то выдача идет в 10 раз больше
        return tempBattery;
    }
}