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

    private String myNumber;
    private int myWarning;
    private long myAlarmInterval;
    private long myNormalInterval;

    private static long mLastAlarm;
    private static long mLastNormal;

    public JobSchedulerService() {

    }

    @Override
    public boolean onStartJob(JobParameters param) {
       // Toast.makeText(getApplicationContext(), "Job Started", Toast.LENGTH_SHORT).show();
        batteryTemperature ();

        long currentTime = System.currentTimeMillis();
        String timestamp = DateFormat.getDateTimeInstance().format(new Date(currentTime));
        // При старте равно нулю, можно добавить поправку, в размере интервала, иначе первый тест пропускается
        if (mLastAlarm == 0 && mLastNormal == 0 ){
            mLastAlarm = currentTime - 5000;
            mLastNormal = currentTime - 5000;
        }

//        Log.d(LOG_TAG, "--- onStartJob ---");
//        Log.d(LOG_TAG, "currentTime - mLastAlarm > myAlarmInterval: " + currentTime + " - " + mLastAlarm + " = " +  (currentTime - mLastAlarm) + " > " + myAlarmInterval);
//        Log.d(LOG_TAG, "currentTime - mLastNormal > myNormalInterval: " + currentTime + " - " + mLastNormal + " = " +  (currentTime - mLastNormal) + " > " + myNormalInterval);
        Log.d(LOG_TAG, "myAlarmInterval: " + currentTime + " - " + mLastAlarm + " = " +  (currentTime - mLastAlarm) + " ? " + myAlarmInterval);
        Log.d(LOG_TAG, "myNormalInterval: " + currentTime + " - " + mLastNormal + " = " +  (currentTime - mLastNormal) + " ? " + myNormalInterval);

        // true если тревога
        boolean alarmType;
        if (serviseJobON && (currentTime - mLastAlarm > myAlarmInterval)) {
            mLastAlarm = currentTime;
            alarmType = true;
            new JobTask(this, myNumber, myWarning, alarmType,ifSensor,tempBattery).execute(param);
        }

        if (serviseJobON && (currentTime - mLastNormal > myNormalInterval)) {
            mLastNormal = currentTime;
            alarmType = false;
            new JobTask(this, myNumber, myWarning, alarmType,ifSensor,tempBattery).execute(param);
        }



        // jobFinished(param, false);

/*        // Normal SMS
        if ((currentTime - mLastUpdated) > NORMAL_INTERVAL) {
            mLastUpdated = currentTime;
            String textMessage = "Все системы работают нормально";
           // Log.d(LOG_TAG,"currentTime - mJobLastUpdated: " + String.valueOf(currentTime - mLastUpdated));
            // Log.d(LOG_TAG,"NORMAL_INTERVAL: " + String.valueOf(NORMAL_INTERVAL));
           // Log.d(LOG_TAG, textMessage);
            new SendSMS().execute(textMessage);
        }*/
        // Alarm SMS
/*       if ((currentTime - mLastMessage) > ALARM_INTERVAL) {
            mLastMessage = currentTime;
            //Log.d(LOG_TAG,"currentTime - mLastMessage: " + String.valueOf(currentTime - mLastMessage));
            new JobTask(this).execute(param);
        }*/
      //  new JobTask(this).execute(param);
        return false;
    }
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(LOG_TAG, "--- onStopJob --- return true");
    //    Log.d(LOG_TAG, "mLastAlarm = " + mLastAlarm);
    //    Log.d(LOG_TAG, "mLastNormal = " + mLastNormal);
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
    }
    public float batteryTemperature ()
    {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        tempBattery   = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)) / 10; // Почему разделил на 10??? Да почему то выдача идет в 10 раз больше

        return tempBattery;
    }
}