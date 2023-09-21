package ru.microsave.tempmonitor;
/*
Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
    Здесь производится подготовка и передача СМС сообщения для отправки, если температура ниже аварийного порога
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;

import java.util.Date;

class JobAlarmSensor extends AsyncTask<JobParameters, Void, JobParameters> {

    private String MY_NUMBER_LOCAL;
    private int WARNING_TEMP_LOCAL;
    private int DEGREES_LOCAL; // Похоже только static работает
    private int DEGREES_LOCAL_BAT; // Похоже только static работает

//    private static Sensor mJobSensorTemperatureAlarm;
//    private static SensorManager mJobSensorManagerAlarm;
    private static int myJobTaskAlarm;

    private final String LOG_TAG = "myLogs";
    private final JobService jobServiceAlarmSens;
    private String textMessage;
    private String mAppname;

    public JobAlarmSensor(JobService jobService, String num, float tempSensor, float tempBat, int count, int war, String appname) {

        MY_NUMBER_LOCAL = num;
        DEGREES_LOCAL = (int) tempSensor;
        DEGREES_LOCAL_BAT = (int) tempBat;
        myJobTaskAlarm = count;
        WARNING_TEMP_LOCAL = war;
        mAppname = appname;


        this.jobServiceAlarmSens = jobService;
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {

        //Log.d(LOG_TAG, "EXIST A SENSOR, DEGREES_LOCAL = " + DEGREES_LOCAL);

        myMessage(DEGREES_LOCAL);

        return jobParameters[0];
    }

    @Override
    protected void onPostExecute(JobParameters jobParameters) {
        //     Log.d(LOG_TAG, "jobFinished(jobParameters, true)");
        //     jobServiceAlarmSens.jobFinished(jobParameters, false);
    }

    private void myMessage(int degrees) {

        //if (degrees == 0) return;
        // TODO: 17.04.2022 Это временный костыль. А мало ли реально будет 0 градусов. Надо понять почему первый замер равен нулю.

        // Второй вариант оформления метки времени
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence timeStampChar = df.format("kk:mm dd/MM/yy", new Date());


        textMessage = "ТРЕВОГА: " + degrees + Character.toString((char) 176) + "C" + ", БАТАРЕЯ: " + DEGREES_LOCAL_BAT + Character.toString((char) 176) + "C" + ", " + timeStampChar + ". " + mAppname + ", #" + myJobTaskAlarm;
        // Отправляем созданный номер задачи и текст в класс для отправки СМС
        new SendSMS(MY_NUMBER_LOCAL, textMessage);
    }
}
