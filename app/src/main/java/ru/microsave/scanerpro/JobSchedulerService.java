package ru.microsave.scanerpro;
/*
Шедулер для периодического сообщения об уровне темпертатуры
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

import androidx.work.Configuration;

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
    private Sensor mJobSensorTemperature;
    private SensorManager mJobSensorManager;


    public JobSchedulerService() {
        Configuration.Builder builder = new Configuration.Builder();
        builder.setJobSchedulerJobIdRange(1000, 2000);
    }

    @Override
    public void onCreate() {
        // Log.d(LOG_TAG, "JobSchedulerService onCreate");
        readSharedPreferences();
        mJobSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mJobSensorTemperature = mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobSensorManager.registerListener(this, mJobSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        myApp = getString(R.string.app_name);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float newValue = sensorEvent.values[0];

        // Игнорируем ложные нули и отрицательные значения
        if (newValue > 0) {
            tempSensor = newValue;
            Log.d(LOG_TAG, "JobSchedulerService: tempSensor updated = " + tempSensor);
        } else {
            Log.w(LOG_TAG, "JobSchedulerService: sensor returned " + newValue + ", keeping old value = " + tempSensor);
        }
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
        // Если FlexTime то время не проверяем! (ПРОВЕРЯЕМ)
        if (ifFlexTime) {
            if ((mCurrentTime - mLastInfo) > (myNormalInterval * 1000L * 60L * 60L)) {
                Log.d(LOG_TAG, "1. Info (mCurrentTime - mLastInfo) = " + ((mCurrentTime - mLastInfo)/1000L/60L));
                mLastInfo = mCurrentTime - (1000L * 3L); // Новый таймштамп

                if (ifSensor) {
                    // Log.d(LOG_TAG, "new: JobInfoSensor");
                    // TODO: 12.11.2022 КОСТЫЛЬ - ИНОГДА СЕНСОР ОТДАЁТ НОЛЬ НЕПОНЯТНО ПОЧЕМУ
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

                mLastInfo = mCurrentTime - (1000L * 3L); // Новый таймштамп
                if (ifSensor) {
                    // TODO (12.11.2022): раньше здесь стоял костыль, потому что сенсор иногда отдавал 0
                    // Теперь нули фильтруются в onSensorChanged(), а в случае 0 выполняется fallback на батарею
                    if (tempSensor > 0) {
                        ++TASK_NUMBER;
                        saveSharedPreferences();
                        new JobInfoSensor(this, myNumber, tempSensor, tempBattery, TASK_NUMBER, myApp).execute(param);
                    } else {
                        Log.d(LOG_TAG, "JobSchedulerService: tempSensor=0, fallback на батарею");
                        ++TASK_NUMBER;
                        saveSharedPreferences();
                        new JobInfoBattery(this, myNumber, tempBattery, TASK_NUMBER, myApp).execute(param);
                    }
                } else {
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
            if (mJobSensorManager != null &&
                    mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
                mJobSensorManager.unregisterListener(this);
                Log.d(LOG_TAG, "mJobSensorManager.unregisterListener");
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "mJobSensorManager.unregisterListener = null");
            e.printStackTrace();
        }

        // Сброс значения сенсора, чтобы при новом запуске не использовать старые данные
        tempSensor = 0f;
        Log.d(LOG_TAG, "JobSchedulerService: tempSensor reset to 0");

        super.onDestroy();
    }


    private void readSharedPreferences() {
        SharedPreferences saveJobPref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        myNumber = (saveJobPref.getString("NUMBER", "+7123456789"));
        myNormalInterval = (saveJobPref.getLong("NORMAL_INTERVAL", 12));
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
        if (intent != null) {
            // EXTRA_TEMPERATURE возвращается в десятых долях °C → делим на 10
            tempBattery = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10; // Почему разделил на 10??? Да почему то выдача идет в 10 раз больше

        } else {
            Log.w(LOG_TAG, "batteryTemperature: intent = null, returning last known value");
            // tempBattery уже хранит предыдущее значение, его и возвращаем
        }

        return tempBattery;
    }
}