package ru.microsave.tempmonitor;
/*
Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
    Здесь производится измерение температуры сенсора и отправка СМС сообщения по стандартному интервалу
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.Date;

class JobInfoSensor extends AsyncTask <JobParameters, Void, JobParameters> {

    private String MY_NUMBER_LOCAL;

    private int DEGREES_LOCAL; // Похоже только static работает

    private static int myJobTask;

    private final String LOG_TAG = "myLogs";
    private final JobService jobService;
    private String textMessage;


    public JobInfoSensor(JobService jobService, String num, float tempSensor, int count) {

        MY_NUMBER_LOCAL = num;
        myJobTask = count;
        DEGREES_LOCAL = (int)tempSensor;
        this.jobService = jobService;
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {

                myMessage(DEGREES_LOCAL);

        return jobParameters[0];
    }
    @Override
    protected void onPostExecute(JobParameters jobParameters) {
            jobService.jobFinished(jobParameters, true);
    }

    private void myMessage(int degrees){

        // Один вариант таймштампа
       // long currentTime = System.currentTimeMillis();
       // SimpleDateFormat formatter = new SimpleDateFormat("dd/mm/yyyy");
       // String timeStampString = formatter.format(new Date(Long.parseLong(String.valueOf(currentTime))));

        // Второй вариант оформления метки времени
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence timesTampChar = df.format("dd-MM-yyyy kk:mm", new Date());

        // Третий вариант, он был раньше, без форматирования
        // long currentTime = System.currentTimeMillis();
        // String timestamp = DateFormat.getDateTimeInstance().format(new Date(currentTime));

        // Log.d(LOG_TAG, "myJobTask = " + myJobTask);
            textMessage = "sms#" + myJobTask + ", " + timesTampChar + ", " + " ИНФО:" + degrees + Character.toString ((char) 176) + "C";

            try {
                 SmsManager.getDefault()
                        .sendTextMessage(MY_NUMBER_LOCAL, null, textMessage, null, null);
                Log.d(LOG_TAG, textMessage);
            } catch (Exception e) {
                Log.d(LOG_TAG, "Failed to send Info Sensor message: " + textMessage);
                e.printStackTrace();
            }
    }
}
