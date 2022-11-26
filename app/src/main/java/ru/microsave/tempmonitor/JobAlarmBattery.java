package ru.microsave.tempmonitor;
/*
Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
    Здесь производится измерение температуры батареи и отправка СМС сообщения, если температура ниже аварийного порога
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;

import java.util.Date;

class JobAlarmBattery extends AsyncTask<JobParameters, Void, JobParameters> {
    private float mTempBattery;

    private String MY_NUMBER_LOCAL;
    private int WARNING_TEMP_LOCAL;
    private static int DEGREES_LOCAL; // Похоже только static работает

    private static int myJobTaskAlarm;

    private final String LOG_TAG = "myLogs";
    private final JobService jobServiceAlarmBatt;
    private String textMessage;
    private String mAppname;

    public JobAlarmBattery(JobService jobService, String num, float tempBat, int count, int war, String appname) {

        MY_NUMBER_LOCAL = num;
        mTempBattery = tempBat;
        myJobTaskAlarm = count;
        WARNING_TEMP_LOCAL = war;
        mAppname = appname;

        this.jobServiceAlarmBatt = jobService;
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
        //    Log.d(LOG_TAG, "jobFinished(jobParameters, true)");
            jobServiceAlarmBatt.jobFinished(jobParameters, true);
    }

    private void myMessage(int degrees) {

        // Второй вариант оформления метки времени
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence timeStampChar = df.format("kk:mm dd/MM/yy", new Date());

        //textMessage = "ТРЕВОГА: " + degrees + Character.toString ((char) 176) + "C" + ", " + timeStampChar + ", СМС#" + myJobTaskAlarm +  ". " + mAppname;
        textMessage = "ТРЕВОГА: " + degrees + Character.toString((char) 176) + "C" + ", " + timeStampChar + ". " + mAppname + ", #" + myJobTaskAlarm;
        if (degrees < WARNING_TEMP_LOCAL) {
            // Отправляем созданный номер задачи и текст в класс для отправки СМС
            //new sendSMS(MY_NUMBER_LOCAL, textMessage);
            new sendSMS(MY_NUMBER_LOCAL, textMessage);
        }
    }
}
