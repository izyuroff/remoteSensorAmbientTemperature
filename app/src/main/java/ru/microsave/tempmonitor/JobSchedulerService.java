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

    private String myApp;
    private String myNumber;
    private int myWarningTemperature;
    private long myAlarmInterval;
    private long myNormalInterval;
    private long mCurrentTime;

    private static long mLastAlarm;
    private static long mLastInfo;
    private static int TASK_NUMBER;
    private static Sensor mJobSensorTemperature;
    private static SensorManager mJobSensorManager;

    //private int numlog = 0;

    public JobSchedulerService() {
        //readSharedPreferences();
    }

    @Override
    public void onCreate() {
       // numlog++;
        Log.d(LOG_TAG, "JobSchedulerService onCreate");
        //readSharedPreferences();
        this.mJobSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mJobSensorTemperature = mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobSensorManager.registerListener(this, mJobSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        myApp = getString(R.string.app_name);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        tempSensor = sensorEvent.values[0];
      //  Log.d(LOG_TAG, "sensorEvent: OK");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onStartJob(JobParameters param) {
    //    Log.d(LOG_TAG, "--- onStartJob --- return true ---  СЕРВИС ЗАПУЩЕН!!!!!!!!!");
        //numlog++;
        //Log.d(LOG_TAG, "JobSchedulerService onStartJob: " + numlog);
         readSharedPreferences();
        // TODO: 06.06.2022 Очень интересно, почему надо вызывать onCreate
         onCreate(); // Избыточно поди (Вот почему то нет!  Если закомментить - вообще перестает все работать!)
        mCurrentTime = System.currentTimeMillis();
    //    Log.d(LOG_TAG, "mCurrentTime: " + mCurrentTime);
        Log.d(LOG_TAG, "mLastAlarm 1: " + mLastAlarm);
        Log.d(LOG_TAG, "mLastInfo 1: " + mLastInfo);


        String timestamp = DateFormat.getDateTimeInstance().format(new Date(mCurrentTime));

        // При старте равно нулю, можно добавить поправку, в размере интервала, иначе первый тест пропускается
        if (mLastAlarm == 0 && mLastInfo == 0 ){
            //mLastAlarm = mCurrentTime - myAlarmInterval;
            //mLastInfo = mCurrentTime - myNormalInterval;
            mLastAlarm = mCurrentTime;
            mLastInfo = mCurrentTime;
            Log.d(LOG_TAG, "mLastAlarm 2: " + mLastAlarm);
            Log.d(LOG_TAG, "mLastInfo 2: " + mLastInfo);
            saveSharedPreferences();
        }

        // Вычисление периода тревоги
        if (mCurrentTime - mLastAlarm > myAlarmInterval){

            Log.d(LOG_TAG, "myAlarmInterval: " + mCurrentTime + " - " + mLastAlarm + " = " +  (mCurrentTime - mLastAlarm) + " ? " + myAlarmInterval);
            mLastAlarm = mCurrentTime; // Новый таймштамп и поправка секунд 10 для корректировки непредвиденных задержек следующего запуска
            Log.d(LOG_TAG, "mLastAlarm 3: " + mLastAlarm);

            saveSharedPreferences();
            batteryTemperature ();

            // Для сенсора
            if(ifSensor && tempSensor < myWarningTemperature){
                ++TASK_NUMBER;
                saveSharedPreferences();
                new JobAlarmSensor(this, myNumber, tempSensor, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
            }

            // Для батареи
            if (!ifSensor && tempBattery < myWarningTemperature) {
                ++TASK_NUMBER;
                saveSharedPreferences();
                new JobAlarmBattery(this, myNumber, tempBattery, TASK_NUMBER, myWarningTemperature, myApp).execute(param);
            }
        }

        // Вычисление периода регулярной информации
        if (mCurrentTime - mLastInfo > myNormalInterval){
            Log.d(LOG_TAG, "myNormalInterval: " + mCurrentTime + " - " + mLastInfo + " = " +  (mCurrentTime - mLastInfo) + " ? " + myNormalInterval);
            mLastInfo = mCurrentTime; // Новый таймштамп и поправка секунд 10 для корректировки непредвиденных задержек следующего запуска
            Log.d(LOG_TAG, "mLastInfo 3: " + mLastInfo);

            ++TASK_NUMBER;
            saveSharedPreferences();

                if (ifSensor) {
                //    Log.d(LOG_TAG, "new: JobInfoSensor");
                    new JobInfoSensor(this, myNumber, tempSensor, TASK_NUMBER, myApp).execute(param);
                }
                else {
                batteryTemperature ();
                //    Log.d(LOG_TAG, "new: JobInfoBattery");
                    new JobInfoBattery(this, myNumber, tempBattery, TASK_NUMBER, myApp).execute(param);
                }
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(LOG_TAG, "--- onStopJob --- return true --- СЕРВИС ОСТАНОВЛЕН!!!!!!!!!");
        return true;

    }

    @Override
    public void onDestroy() {
    //    stopService(new Intent(this, JobSchedulerService.class));
        try {
            if (mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
                mJobSensorManager.unregisterListener(this);
            //    Log.d(LOG_TAG, "mJobSensorManager.unregisterListener");
            }

        } catch (Exception e) {
            Log.d(LOG_TAG, "mJobSensorManager.unregisterListener = null");
            e.printStackTrace();
        }
    }

    private void readSharedPreferences(){
        SharedPreferences saveJobPref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        myNumber = (saveJobPref.getString("NUMBER", "+7123456789"));
        myWarningTemperature = (saveJobPref.getInt("WARNING", 5));
        myAlarmInterval = (saveJobPref.getLong("ALARM_INTERVAL", 1000 * 60 * 60 * 1));
        myNormalInterval = (saveJobPref.getLong("NORMAL_INTERVAL", 1000 * 60 * 60 * 6));
        ifSensor = (saveJobPref.getBoolean("IFSENSOR", true));
        mLastAlarm = (saveJobPref.getLong("LAST_ALARM", 0));
        mLastInfo = (saveJobPref.getLong("LAST_INFO", 0));
        TASK_NUMBER = (saveJobPref.getInt("TASK_NUMBER", 0));
        // Log.d(LOG_TAG, "readSharedPreferences: OK");
    }


    private void saveSharedPreferences() {
        SharedPreferences savePref;
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        SharedPreferences.Editor ed = savePref.edit();

        ed.putLong("LAST_ALARM", mLastAlarm);
        ed.putLong("LAST_INFO", mLastInfo);
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