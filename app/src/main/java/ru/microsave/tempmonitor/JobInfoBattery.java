package ru.microsave.tempmonitor;
/*
Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
    Здесь производится измерение температуры и отправка СМС сообщения

    MainActivity
    ControlActivity
    JobSchedulerService
    JobTask

 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

class JobInfoBattery extends AsyncTask <JobParameters, Void, JobParameters> {
    private float mTempBattery;
    private String MY_NUMBER_LOCAL;

    private static int DEGREES_LOCAL; // Похоже только static работает

    private static Sensor mJobSensorTemperature;
    private static SensorManager mJobSensorManager;
    private static int myJobTask;

    private final String LOG_TAG = "myLogs";
    private final JobService jobService;
    private String textMessage;


    public JobInfoBattery(JobService jobService, String num, float tempBat, int count) {

        MY_NUMBER_LOCAL = num;
        myJobTask = count;
        mTempBattery = tempBat;

        this.jobService = jobService;
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {
            Log.d(LOG_TAG, "mTempBattery = " + mTempBattery);

                // получаем и отправляем температуру батареи
                DEGREES_LOCAL = (int) mTempBattery;
                Log.d(LOG_TAG, "NO A SENSOR, DEGREES_LOCAL = " + DEGREES_LOCAL);
                myMessage(DEGREES_LOCAL);

        return jobParameters[0];
    }

    @Override
    protected void onPostExecute(JobParameters jobParameters) {
            jobService.jobFinished(jobParameters, true);
    }

    private void myMessage(int degrees){
        long currentTime = System.currentTimeMillis();
        String timestamp = DateFormat.getDateTimeInstance().format(new Date(currentTime));

            Log.d(LOG_TAG, "myJobTask = " + myJobTask);
            textMessage = "#" + myJobTask + " " + timestamp +  " ИНФО: " + degrees + Character.toString ((char) 176) + "C";
            try {
                SmsManager.getDefault()
                        .sendTextMessage(MY_NUMBER_LOCAL, null, textMessage, null, null);
                Log.d(LOG_TAG, textMessage);
            } catch (Exception e) {
                Log.d(LOG_TAG, "Failed to send InfoBattery message: " + textMessage);
                e.printStackTrace();
            }
    }
}
