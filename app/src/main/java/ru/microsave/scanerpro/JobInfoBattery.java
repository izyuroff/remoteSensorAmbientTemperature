package ru.microsave.scanerpro;
/*
Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
    Здесь производится подготовка и передача СМС сообщения для отправки, с заданным интервалом
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.text.format.DateFormat;

import java.util.Date;

class JobInfoBattery extends AsyncTask <JobParameters, Void, JobParameters> {
    private final float mTempBattery;
    private final String MY_NUMBER_LOCAL;

    private static int DEGREES_LOCAL; // Похоже только static работает

//    private static Sensor mJobSensorTemperature;
//    private static SensorManager mJobSensorManager;
    private static int myJobTaskNorm;

    private final String LOG_TAG = "myLogs";
    private final JobService jobServiceInfoBatt;
    private String textMessage;
    private final String mAppname;

    public JobInfoBattery(JobService jobService, String num, float tempBat, int count, String appname) {

        MY_NUMBER_LOCAL = num;
        mTempBattery = tempBat;
        myJobTaskNorm = count;
        mAppname = appname;


        this.jobServiceInfoBatt = jobService;
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {
           // Log.d(LOG_TAG, "mTempBattery = " + mTempBattery);

                // получаем и отправляем температуру батареи
                DEGREES_LOCAL = (int) mTempBattery;
              //  Log.d(LOG_TAG, "NO A SENSOR, DEGREES_LOCAL = " + DEGREES_LOCAL);
                myMessage(DEGREES_LOCAL);

        return jobParameters[0];
    }

    @Override
    protected void onPostExecute(JobParameters jobParameters) {
    //        Log.d(LOG_TAG, "jobFinished(jobParameters, true)");
    //    jobServiceInfoBatt.jobFinished(jobParameters, false);
    }

    private void myMessage(int degrees){

        // Второй вариант оформления метки времени
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence timeStampChar = DateFormat.format("kk:mm dd/MM/yy", new Date());
        String timeStampString = (String) timeStampChar;

        textMessage = "БАТАРЕЯ: " + degrees + (char) 176 + "C" + ", " + timeStampString + ". " + mAppname + ", #" + myJobTaskNorm;
        // Отправляем созданный номер задачи и текст в класс для отправки СМС
        new SendSMS(MY_NUMBER_LOCAL, textMessage);
    }
}
