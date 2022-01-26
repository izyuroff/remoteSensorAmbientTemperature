package ru.microsave.tempmonitor;
// Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
/*

    MainActivity
    ControlActivity
    JobSchedulerService
    JobTask
    SendSMS

 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

class JobTask extends AsyncTask <JobParameters, Void, JobParameters> implements SensorEventListener {
    private String MY_NUMBER_LOCAL;
    private int WARNING_TEMP_LOCAL;
    private Boolean mALARM_TYPE;
    private static int DEGREES_LOCAL; // Похоже только static работает

    private static Sensor mJobSensorTemperature;
    private static SensorManager mJobSensorManager;
    private static int myJobTask = 0;

    private final String LOG_TAG = "myLogs";
    private final JobService jobService;
    private String textMessage;


    public JobTask(JobService jobService, String s, int i, Boolean b, Boolean ifSensor) {

        MY_NUMBER_LOCAL = s;
        WARNING_TEMP_LOCAL = i;
        mALARM_TYPE = b;

        mJobSensorManager = (SensorManager) jobService.getSystemService(Context.SENSOR_SERVICE);
        mJobSensorTemperature = mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mJobSensorManager.registerListener(this, mJobSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);


        this.jobService = jobService;

        if (!ifSensor) {
            getCpuTemp();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(mJobSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            // получаю, преобразую в int и сохраняю в degrees
            DEGREES_LOCAL = (int)sensorEvent.values[0];
           // Log.d(LOG_TAG, "DEGREES = " + DEGREES);

        }
        else
            Log.d(LOG_TAG, "Нет сенсора, будет температура CPU");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {
            ++myJobTask;
            Log.d(LOG_TAG, "myJobTask = " + myJobTask);
            myMessage(DEGREES_LOCAL);
        return jobParameters[0];
    }
    @Override
    protected void onPostExecute(JobParameters jobParameters) {
       // mJobSensorManager.unregisterListener(this);
        jobService.jobFinished(jobParameters, true);

    }

    private void myMessage(int degrees){
        long currentTime = System.currentTimeMillis();
        String timestamp = DateFormat.getDateTimeInstance().format(new Date(currentTime));

        if (degrees != 0 && degrees < WARNING_TEMP_LOCAL && mALARM_TYPE) {
            textMessage = timestamp +  "--- Низкая ---: " + degrees;
            try {
            //    Временно ничего не отправляем!
            //    SmsManager.getDefault()
            //            .sendTextMessage(MY_NUMBER_LOCAL, null, textMessage, null, null);
                Log.d(LOG_TAG, textMessage);
            } catch (Exception e) {
                Log.d(LOG_TAG, "failed to send message");
                e.printStackTrace();
            }
        }

        if (degrees != 0 && !mALARM_TYPE) {
            textMessage = timestamp +  "--- Нормальная ---: " + degrees;
            try {
                //    Временно ничего не отправляем!
                // SmsManager.getDefault()
                //        .sendTextMessage(MY_NUMBER_LOCAL, null, textMessage, null, null);
                Log.d(LOG_TAG, textMessage);
            } catch (Exception e) {
                Log.d(LOG_TAG, "failed to send message");
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
