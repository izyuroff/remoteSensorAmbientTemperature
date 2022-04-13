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

class JobTask extends AsyncTask <JobParameters, Void, JobParameters> implements SensorEventListener {
    private float mTempBattery;
    private String MY_NUMBER_LOCAL;
    private int WARNING_TEMP_LOCAL;
    private Boolean mALARM_TYPE;
    private Boolean ifSensor;
    private static int DEGREES_LOCAL; // Похоже только static работает

    private static Sensor mJobSensorTemperature;
    private static SensorManager mJobSensorManager;
    private static int myJobTask = 0;

    private final String LOG_TAG = "myLogs";
    private final JobService jobService;
    private String textMessage;


    public JobTask(JobService jobService, String num, int war, Boolean al, Boolean sen, float tempBat) {

        MY_NUMBER_LOCAL = num;
        WARNING_TEMP_LOCAL = war;
        mALARM_TYPE = al;
        ifSensor = sen;
        mTempBattery = tempBat;



        if (ifSensor) {
            mJobSensorManager = (SensorManager) jobService.getSystemService(Context.SENSOR_SERVICE);
            mJobSensorTemperature = mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            mJobSensorManager.registerListener(this, mJobSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else
        Log.d(LOG_TAG, "Нет сенсора, будет температура mTempBattery = " + mTempBattery);

        this.jobService = jobService;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            // получаю, преобразую в int и сохраняю в degrees
            DEGREES_LOCAL = (int)sensorEvent.values[0];
           // Log.d(LOG_TAG, "DEGREES = " + DEGREES);

        }
        else
            Log.d(LOG_TAG, "Нет сенсора, будет температура CPU, UPD:но это условие никогда не выполнится :)");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {
            Log.d(LOG_TAG, "mTempBattery = " + mTempBattery);

            if (ifSensor) {
                Log.d(LOG_TAG, "EXIST A SENSOR, DEGREES_LOCAL = " + DEGREES_LOCAL);
                myMessage(DEGREES_LOCAL);
            }
            else {
                // Если нет сенсора, получаем и отправляем температуру батареи
                DEGREES_LOCAL = (int) mTempBattery;
                Log.d(LOG_TAG, "NO A SENSOR, DEGREES_LOCAL = " + DEGREES_LOCAL);
                myMessage(DEGREES_LOCAL);
            }

        return jobParameters[0];
    }
    @Override
    protected void onPostExecute(JobParameters jobParameters) {
        try {
            mJobSensorManager.unregisterListener(this);
            Log.d(LOG_TAG, "mJobSensorManager.unregisterListener");
        } catch (Exception e) {
            Log.d(LOG_TAG, "mJobSensorManager.unregisterListener = null");
            e.printStackTrace();
        }
        //    jobService.jobFinished(jobParameters, true);

    }

    private void myMessage(int degrees){
        long currentTime = System.currentTimeMillis();

       // SimpleDateFormat SDFormat = new SimpleDateFormat("MM/dd/yyyy");
       // DateFormat DFormat = DateFormat.getDateTimeInstance();
       // String timestamp = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(currentTime));

       // String timestamp = DFormat.format(new Date(currentTime));
        String timestamp = DateFormat.getDateTimeInstance().format(new Date(currentTime));

        if (degrees != 0 && degrees < WARNING_TEMP_LOCAL && mALARM_TYPE) {
            ++myJobTask;
            Log.d(LOG_TAG, "myJobTask = " + myJobTask);
            textMessage = "#" + myJobTask + " " + timestamp +  " ТРЕВОГА: " + degrees + Character.toString ((char) 176) + "C";
            try {
                SmsManager.getDefault()
                        .sendTextMessage(MY_NUMBER_LOCAL, null, textMessage, null, null);
                Log.d(LOG_TAG, textMessage);
            } catch (Exception e) {
                Log.d(LOG_TAG, "failed to send message: " + textMessage);
                e.printStackTrace();
            }
        }

        if (degrees != 0 && !mALARM_TYPE) {
            ++myJobTask;
            Log.d(LOG_TAG, "myJobTask = " + myJobTask);
            textMessage = "#" + myJobTask + " " + timestamp +  " ИНФО: " + degrees + Character.toString ((char) 176) + "C";

          //  tempBattery = String.valueOf(temp) + Character.toString ((char) 176) + "C";

            try {
                 SmsManager.getDefault()
                        .sendTextMessage(MY_NUMBER_LOCAL, null, textMessage, null, null);
                Log.d(LOG_TAG, textMessage);
            } catch (Exception e) {
                Log.d(LOG_TAG, "failed to send message" + textMessage);
                e.printStackTrace();
            }
        }
    }
    public float getCpuTemp() {
        Process process;
        try {
            process = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            float temp = Float.parseFloat(line) / 1000.0f;

            DEGREES_LOCAL = (int)temp;
            Log.d(LOG_TAG, "DEGREES_LOCAL = " + DEGREES_LOCAL);
            return DEGREES_LOCAL;

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "Exception e = ");
            return 0.0f;
        }
    }

}
