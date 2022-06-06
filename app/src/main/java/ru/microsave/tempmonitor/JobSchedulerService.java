package ru.microsave.tempmonitor;
/*

TASK_NUMBER был не всегда стабилен, иногда обнулялся ни  того ни с сего


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

import java.text.DateFormat;
import java.util.Date;

public class JobSchedulerService extends JobService implements SensorEventListener {
    private float tempSensor;
    private float tempBattery;
    private final String LOG_TAG = "myLogs";
    private boolean ifSensor;

    private String myNumber;
    private int myWarningTemperature;
    private long myAlarmInterval;
    private long myNormalInterval;
    private long mCurrentTime;

    private static long mLastAlarm;
    private static long mLastNormal;
    private static int TASK_NUMBER;
    private static Sensor mJobSensorTemperature;
    private static SensorManager mJobSensorManager;

    public JobSchedulerService() {
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "JobSchedulerService onCreate: OK");
        readSharedPreferences();
        this.mJobSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mJobSensorTemperature = mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobSensorManager.registerListener(this, mJobSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        tempSensor = sensorEvent.values[0];
        Log.d(LOG_TAG, "sensorEvent: OK");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onStartJob(JobParameters param) {
        Log.d(LOG_TAG, "JobSchedulerService onStartJob: OK");
        readSharedPreferences();
        // TODO: 06.06.2022 Очень интересно, почему надо вызывать onCreate 
         onCreate(); // Избыточно поди (Вот почему то нет!  Если закомментить - вообще перестает все работать!)
        mCurrentTime = System.currentTimeMillis();
        String timestamp = DateFormat.getDateTimeInstance().format(new Date(mCurrentTime));

        // При старте равно нулю, можно добавить поправку, в размере интервала, иначе первый тест пропускается
        if (mLastAlarm == 0 && mLastNormal == 0 ){
            mLastAlarm = mCurrentTime;
            mLastNormal = mCurrentTime;
            TASK_NUMBER = 0;
        }

        // Вычисление периода тревоги
        if (mCurrentTime - mLastAlarm > myAlarmInterval){

        //    Log.d(LOG_TAG, "myAlarmInterval: " + mCurrentTime + " - " + mLastAlarm + " = " +  (mCurrentTime - mLastAlarm) + " ? " + myAlarmInterval);
            mLastAlarm = mCurrentTime; // Новый таймштамп и поправка секунд 10 для корректировки непредвиденных задержек следующего запуска

            if(tempSensor < myWarningTemperature){

                ++TASK_NUMBER;
                saveSharedPreferences();

                if (ifSensor) {
                    // Log.d(LOG_TAG, "new: JobAlarmSensor");
                    new JobAlarmSensor(this, myNumber, tempSensor, TASK_NUMBER, myWarningTemperature).execute(param);
                }
                else {
                    batteryTemperature ();
                    // Log.d(LOG_TAG, "new: JobAlarmBattery");
                    new JobAlarmBattery(this, myNumber, tempBattery, TASK_NUMBER, myWarningTemperature).execute(param);
                }

            }


        }

        // Вычисление периода информации
        if (mCurrentTime - mLastNormal > myNormalInterval){
            ++TASK_NUMBER;
            saveSharedPreferences();
       //     Log.d(LOG_TAG, "myNormalInterval: " + mCurrentTime + " - " + mLastNormal + " = " +  (mCurrentTime - mLastNormal) + " ? " + myNormalInterval);
            mLastNormal = mCurrentTime; // Новый таймштамп и поправка секунд 10 для корректировки непредвиденных задержек следующего запуска
                if (ifSensor) {
                  //  Log.d(LOG_TAG, "new: JobInfoSensor");
                    new JobInfoSensor(this, myNumber, tempSensor, TASK_NUMBER).execute(param);
                }
                else {
                batteryTemperature ();
                 //   Log.d(LOG_TAG, "new: JobInfoBattery");
                    new JobInfoBattery(this, myNumber, tempBattery, TASK_NUMBER).execute(param);
                }
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
       // Log.d(LOG_TAG, "--- onStopJob --- return false");
        return false;
    }

    @Override
    public void onDestroy() {
    //    stopService(new Intent(this, JobSchedulerService.class));
        try {
            if (mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
                mJobSensorManager.unregisterListener(this);
             //   Log.d(LOG_TAG, "mJobSensorManager.unregisterListener");
            }

        } catch (Exception e) {
           // Log.d(LOG_TAG, "mJobSensorManager.unregisterListener = null");
            e.printStackTrace();
        }
    }

    private void readSharedPreferences(){
        SharedPreferences saveJobPref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        myNumber = (saveJobPref.getString("NUMBER", "+7123456789"));
        myWarningTemperature = (saveJobPref.getInt("WARNING", 15));
        myAlarmInterval = (saveJobPref.getLong("ALARM_INTERVAL", 1000 * 60 * 60 * 1));
        myNormalInterval = (saveJobPref.getLong("NORMAL_INTERVAL", 1000 * 60 * 60 * 12));
        ifSensor = (saveJobPref.getBoolean("IFSENSOR", true));
        TASK_NUMBER = (saveJobPref.getInt("TASK_NUMBER", 0));
      //  Log.d(LOG_TAG, "readSharedPreferences: OK");
    }


    private void saveSharedPreferences() {
        SharedPreferences savePref;
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        SharedPreferences.Editor ed = savePref.edit();

        ed.putInt("TASK_NUMBER", TASK_NUMBER);
        ed.apply();
        ed.commit();
    }

    public float batteryTemperature () {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        tempBattery   = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)) / 10; // Почему разделил на 10??? Да почему то выдача идет в 10 раз больше
        return tempBattery;
    }

}