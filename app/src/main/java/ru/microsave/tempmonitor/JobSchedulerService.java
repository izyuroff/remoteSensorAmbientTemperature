package ru.microsave.tempmonitor;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

public class JobSchedulerService extends JobService {
    private float tempBattery;
    private final String LOG_TAG = "myLogs";
    private boolean ifSensor;

    private String myNumber;
    private long myAlarmInterval;
    private long myNormalInterval;
    private long mCurrentTime;

    private static long mLastAlarm;
    private static long mLastNormal;
    private static int TASK_NUMBER = 0;

    public JobSchedulerService() {

    }

    @Override
    public boolean onStartJob(JobParameters param) {
        Log.d(LOG_TAG, "Запуск шедулера OK: onStartJob");
        readSharedPreferences();

        mCurrentTime = System.currentTimeMillis();
        String timestamp = DateFormat.getDateTimeInstance().format(new Date(mCurrentTime));

        // При старте равно нулю, можно добавить поправку, в размере интервала, иначе первый тест пропускается
        if (mLastAlarm == 0 && mLastNormal == 0 ){
            mLastAlarm = mCurrentTime;
            mLastNormal = mCurrentTime;
        }

        if (mCurrentTime - mLastAlarm > myAlarmInterval){
            ++TASK_NUMBER;
            Log.d(LOG_TAG, "myAlarmInterval: " + mCurrentTime + " - " + mLastAlarm + " = " +  (mCurrentTime - mLastAlarm) + " ? " + myAlarmInterval);
            mLastAlarm = mCurrentTime - 5000;
                if (ifSensor) {
                    Log.d(LOG_TAG, "new: JobAlarmSensor");
                    new JobAlarmSensor(this, myNumber,TASK_NUMBER).execute(param);
                }
                else {
                batteryTemperature ();
                    Log.d(LOG_TAG, "new: JobAlarmBattery");
                    new JobAlarmBattery(this, myNumber,tempBattery,TASK_NUMBER).execute(param);
                }
        }

        if (mCurrentTime - mLastNormal > myNormalInterval){
            ++TASK_NUMBER;
            Log.d(LOG_TAG, "myNormalInterval: " + mCurrentTime + " - " + mLastNormal + " = " +  (mCurrentTime - mLastNormal) + " ? " + myNormalInterval);
            mLastNormal = mCurrentTime - 10000;
                if (ifSensor) {
                    Log.d(LOG_TAG, "new: JobInfoSensor");
                    new JobInfoSensor(this, myNumber,TASK_NUMBER).execute(param);
                }
                else {
                batteryTemperature ();
                    Log.d(LOG_TAG, "new: JobInfoBattery");
                    new JobInfoBattery(this, myNumber,tempBattery,TASK_NUMBER).execute(param);
                }
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(LOG_TAG, "--- onStopJob --- return false");
        return false;
    }

    @Override
    public void onDestroy() {
    //    stopService(new Intent(this, JobSchedulerService.class));
    }
    @Override
    public void onCreate() {
        //readSharedPreferences();
    }

    private void readSharedPreferences(){
        SharedPreferences saveJobPref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        myNumber = (saveJobPref.getString("NUMBER", "+7123456789"));
        myAlarmInterval = (saveJobPref.getLong("ALARM_INTERVAL", 1000 * 60 * 60 * 1));
        myNormalInterval = (saveJobPref.getLong("NORMAL_INTERVAL", 1000 * 60 * 60 * 12));
        ifSensor = (saveJobPref.getBoolean("IFSENSOR", true));
        Log.d(LOG_TAG, "readSharedPreferences: OK");
    }

    public float batteryTemperature () {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        tempBattery   = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)) / 10; // Почему разделил на 10??? Да почему то выдача идет в 10 раз больше
        return tempBattery;
    }

}