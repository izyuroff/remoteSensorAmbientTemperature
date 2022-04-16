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

class JobInfoSensor extends AsyncTask <JobParameters, Void, JobParameters> implements SensorEventListener {

    private String MY_NUMBER_LOCAL;

    private static int DEGREES_LOCAL; // Похоже только static работает

    private static Sensor mJobSensorTemperature;
    private static SensorManager mJobSensorManager;
    private static int myJobTask;

    private final String LOG_TAG = "myLogs";
    private final JobService jobService;
    private String textMessage;


    public JobInfoSensor (JobService jobService, String num, int count) {

        MY_NUMBER_LOCAL = num;
        myJobTask = count;

        mJobSensorManager = (SensorManager) jobService.getSystemService(Context.SENSOR_SERVICE);
        mJobSensorTemperature = mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobSensorManager.registerListener(this, mJobSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);

        this.jobService = jobService;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        DEGREES_LOCAL = (int)sensorEvent.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {

                Log.d(LOG_TAG, "EXIST A SENSOR, DEGREES_LOCAL = " + DEGREES_LOCAL);
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
                Log.d(LOG_TAG, "Failed to send InfoSensor message" + textMessage);
                e.printStackTrace();
            }
    }
}
