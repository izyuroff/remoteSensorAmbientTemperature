package ru.microsave.tempmonitor;
/*
Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
    Здесь производится измерение температуры батареи и отправка СМС сообщения, если температура ниже аварийного порога
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

class JobAlarmBattery extends AsyncTask <JobParameters, Void, JobParameters> {
    private float mTempBattery;

    private String MY_NUMBER_LOCAL;
    private int WARNING_TEMP_LOCAL;
    private int DEGREES_LOCAL; // Похоже только static работает

    private static int myJobTask;

    private final String LOG_TAG = "myLogs";
    private final JobService jobService;
    private String textMessage;

    public JobAlarmBattery(JobService jobService, String num, float tempBat, int count, int war) {

        MY_NUMBER_LOCAL = num;
        WARNING_TEMP_LOCAL = war;
        myJobTask = count;
        mTempBattery = tempBat;

        this.jobService = jobService;
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {
          //  Log.d(LOG_TAG, "mTempBattery = " + mTempBattery);

                DEGREES_LOCAL = (int) mTempBattery;
              //  Log.d(LOG_TAG, "NO A SENSOR, DEGREES_LOCAL = " + DEGREES_LOCAL);
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

          //  Log.d(LOG_TAG, "myJobTask = " + myJobTask);
            textMessage = "#" + myJobTask + " " + timestamp +  " ТРЕВОГА: " + degrees + Character.toString ((char) 176) + "C";
          //  Log.d(LOG_TAG, "1 Подготовлено: " + textMessage);

        if (degrees < WARNING_TEMP_LOCAL){

            try {
                SmsManager.getDefault()
                        .sendTextMessage(MY_NUMBER_LOCAL, null, textMessage, null, null);
                        Log.d(LOG_TAG, textMessage);
            } catch (Exception e) {
                        Log.d(LOG_TAG, "Failed to send AlarmBattery message: " + textMessage);
                        e.printStackTrace();
            }

        }


    }
}
