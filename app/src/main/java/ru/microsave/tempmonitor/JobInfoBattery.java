package ru.microsave.tempmonitor;
/*
Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
    Здесь производится измерение температуры батареи и отправка СМС сообщения по стандартному интервалу
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;

import java.util.Date;

class JobInfoBattery extends AsyncTask <JobParameters, Void, JobParameters> {
    private float mTempBattery;
    private String MY_NUMBER_LOCAL;

    private static int DEGREES_LOCAL; // Похоже только static работает

    private static Sensor mJobSensorTemperature;
    private static SensorManager mJobSensorManager;
    private static int myJobTaskNorm;

    private final String LOG_TAG = "myLogs";
    private final JobService jobService;
    private String textMessage;
    private String mAppname;

    public JobInfoBattery(JobService jobService, String num, float tempBat, int count, String appname) {

        MY_NUMBER_LOCAL = num;
        mTempBattery = tempBat;
        myJobTaskNorm = count;
        mAppname = appname;


        this.jobService = jobService;
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {
          //  Log.d(LOG_TAG, "mTempBattery = " + mTempBattery);

                // получаем и отправляем температуру батареи
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

        // Второй вариант оформления метки времени
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence timeStampChar = df.format("kk:mm, dd/MM/yyyy", new Date());
        String timeStampString = (String) timeStampChar;

        textMessage = degrees + Character.toString ((char) 176) + "C" + ", " + timeStampString + ", СМС#" + myJobTaskNorm +  ". " + mAppname;

        //textMessage = "НОРМА: " + degrees + Character.toString ((char) 176) + "C" + ", " + timeStampString + ", #" + myJobTaskNorm;
        // Отправляем созданный номер задачи и текст в класс для отправки СМС
        //new sendSMS(MY_NUMBER_LOCAL, textMessage);
        new sendSMS(MY_NUMBER_LOCAL, textMessage);
    }
}
