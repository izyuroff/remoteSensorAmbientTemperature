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
    private boolean serviseJobON;
    private boolean ifSensor;
    private boolean alarmType;

    private String myNumber;
    private int myWarning;
    private long myAlarmInterval;
    private long myNormalInterval;
    private long mCurrentTime;

    private static long mLastAlarm;
    private static long mLastNormal;
    public static int TASK_NUMBER = 0;

    public JobSchedulerService() {

    }

    @Override
    public boolean onStartJob(JobParameters param) {
        readSharedPreferences();
        ++TASK_NUMBER;
        mCurrentTime = System.currentTimeMillis();
        String timestamp = DateFormat.getDateTimeInstance().format(new Date(mCurrentTime));

        // При старте равно нулю, можно добавить поправку, в размере интервала, иначе первый тест пропускается
        if (mLastAlarm == 0 && mLastNormal == 0 ){
            mLastAlarm = mCurrentTime - mLastAlarm;
            mLastNormal = mCurrentTime - mLastNormal;
        }

        Log.d(LOG_TAG, "myAlarmInterval: " + mCurrentTime + " - " + mLastAlarm + " = " +  (mCurrentTime - mLastAlarm) + " ? " + myAlarmInterval);
        Log.d(LOG_TAG, "myNormalInterval: " + mCurrentTime + " - " + mLastNormal + " = " +  (mCurrentTime - mLastNormal) + " ? " + myNormalInterval);

        if (mCurrentTime - mLastAlarm > myAlarmInterval){
            mLastAlarm = mCurrentTime;
            alarmType = true;
                if (ifSensor) {
                    new JobAlarmSensor(this, myNumber,TASK_NUMBER).execute(param);
                }
                else {
                batteryTemperature ();
                new JobAlarmBattery(this, myNumber,tempBattery,TASK_NUMBER).execute(param);
                }
        }

        if (mCurrentTime - mLastNormal > myNormalInterval){
            mLastNormal = mCurrentTime;
            alarmType = true;
                if (ifSensor) {
                new JobInfoSensor(this, myNumber,TASK_NUMBER).execute(param);
                }
                else {
                batteryTemperature ();
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
        readSharedPreferences();
    }

    private void readSharedPreferences(){
        SharedPreferences saveJobPref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        serviseJobON = (saveJobPref.getBoolean("SERVICEON", false));
        myNumber = (saveJobPref.getString("NUMBER", "+7123456789"));
        myWarning = (saveJobPref.getInt("WARNING", 16));

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