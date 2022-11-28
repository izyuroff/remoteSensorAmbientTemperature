package ru.microsave.tempmonitor;
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
import android.util.Log;

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
    }

    @Override
    public void onCreate() {
        // Log.d(LOG_TAG, "JobSchedulerServiceAlarm onCreate");
        readSharedPreferences();
        this.mJobAlarmSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mJobAlarmSensorTemperature = mJobAlarmSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobAlarmSensorManager.registerListener(this, mJobAlarmSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
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
        // Log.d(LOG_TAG, "mCurrentTime 1: " + mCurrentTime);
        // Log.d(LOG_TAG, "myAlarmInterval 1: " + mCurrentTime + " - " + mLastAlarm + " = " +  (mCurrentTime - mLastAlarm)/1000/60 + " ? " + myAlarmInterval/1000/60);
        // Log.d(LOG_TAG, "mLastAlarm 1: " + mLastAlarm);
        // Log.d(LOG_TAG, "myAlarmInterval 1: " + myAlarmInterval);
        // Log.d(LOG_TAG, "myAlarmInterval 1: " + mCurrentTime + " - " + mLastAlarm + " = " + (mCurrentTime - mLastAlarm) + " ? " + myAlarmInterval * 1000 * 60 * 60);

        // При старте всегда равно нулю (обнуляется по кнопке Stop)
        if (mLastAlarm == 0) {
            mLastAlarm = mCurrentTime;
            // Log.d(LOG_TAG, "mLastAlarm 2: " + mLastAlarm);
            saveSharedPreferences();
        }

            // =======================================================================================
            if (ifFlexTime) {
                // Если FlexTime то время не проверяем! (НЕТ, ТЕПЕРЬ ПРОВЕРЯЕМ)
                if ((mCurrentTime - mLastAlarm) > myAlarmInterval * 1000L * 60L * 60L) {
                    mLastAlarm = mCurrentTime - (1000L * 45L); // Новый таймштамп

                    // Для сенсора и проверка температуры
                    if (ifSensor && tempSensor < myWarningTemperature) {
                        ++TASK_NUMBER;
                        saveSharedPreferences();
                        new JobAlarmSensor(this, myNumber, tempSensor, tempBattery, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
                    }
                    // Для батареи и проверка температуры
                    if (!ifSensor && tempBattery < myWarningTemperature) {
                        ++TASK_NUMBER;
                        saveSharedPreferences();
                        new JobAlarmBattery(this, myNumber, tempBattery, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
                    }
                }
            }
                else {
                        // Проверка времени для старых устройств (в миллисекундах!)
                        if ((mCurrentTime - mLastAlarm) > myAlarmInterval * 1000L * 60L * 60L) {
                            mLastAlarm = mCurrentTime - (1000L * 45L); // Новый таймштамп, сразу же после сработки
                            // Log.d(LOG_TAG, "new: JobInfoSensor");
                            // TODO: 12.11.2022 КОСТЫЛЬ - ИНОГДА СЕНСОР ОТДАТ НОЛЬ НЕПОНЯТНО ПОЧЕМУ
                            //    if (tempSensor == 0) tempSensor = tempBattery;

                            // Для сенсора и проверка температуры
                            if (ifSensor && tempSensor < myWarningTemperature) {
                                ++TASK_NUMBER;
                                saveSharedPreferences();
                                new JobAlarmSensor(this, myNumber, tempSensor, tempBattery, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
                            }
                            // Для батареи и проверка температуры
                            if (!ifSensor && tempBattery < myWarningTemperature) {
                                ++TASK_NUMBER;
                                saveSharedPreferences();
                                new JobAlarmBattery(this, myNumber, tempBattery, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
                            }
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
        myAlarmInterval = (saveJobPref.getInt("ALARM_INTERVAL", 1));
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