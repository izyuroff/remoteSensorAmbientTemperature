package ru.microsave.tempmonitor;
/*
Этот класс для выполнения работы в отдельном потоке и вызывается из JobSchedulerService
    Здесь производится измерение температуры сенсора и отправка СМС сообщения по стандартному интервалу
 */

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;

import java.util.Date;

class JobInfoSensor extends AsyncTask <JobParameters, Void, JobParameters> {

    private String MY_NUMBER_LOCAL;

    private int DEGREES_LOCAL; // Похоже только static работает
    private int DEGREES_LOCAL_BAT;

    private static int myJobTaskNorm;

    private final String LOG_TAG = "myLogs";
    private final JobService jobServiceInfoSens;
    private String textMessage;
    private String mAppname;


    public JobInfoSensor(JobService jobService, String num, float tempSensor,  float tempBat, int count, String appname) {

        MY_NUMBER_LOCAL = num;
        DEGREES_LOCAL = (int)tempSensor;
        DEGREES_LOCAL_BAT = (int)tempBat;
        myJobTaskNorm = count;
        mAppname = appname;

        this.jobServiceInfoSens = jobService;
    }

    @Override
    protected JobParameters doInBackground(JobParameters... jobParameters) {

                myMessage(DEGREES_LOCAL);

        return jobParameters[0];
    }
    @Override
    protected void onPostExecute(JobParameters jobParameters) {
    //    Log.d(LOG_TAG, "jobFinished(jobParameters, true)");
    //    jobServiceInfoSens.jobFinished(jobParameters, false); //###
    }

    private void myMessage(int degrees){

        // Один вариант таймштампа
       // long currentTime = System.currentTimeMillis();
       // SimpleDateFormat formatter = new SimpleDateFormat("dd/mm/yyyy");
       // String timeStampString = formatter.format(new Date(Long.parseLong(String.valueOf(currentTime))));

        // Второй вариант оформления метки времени
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence timeStampChar = df.format("kk:mm dd/MM/yy", new Date());
        String timeStampString = (String) timeStampChar;
        
                // Третий вариант, он был раньше, без форматирования
        // long currentTime = System.currentTimeMillis();
        // String timestamp = DateFormat.getDateTimeInstance().format(new Date(currentTime));

        // Log.d(LOG_TAG, "myJobTask = " + myJobTask);
        textMessage = "ДАТЧИК: " + degrees + Character.toString ((char) 176) + "C"  + ", БАТАРЕЯ: " + DEGREES_LOCAL_BAT + Character.toString ((char) 176) + "C" + ", " + timeStampChar + ". " + mAppname+ ", #" + myJobTaskNorm;
            // Отправляем созданный номер задачи и текст в класс для отправки СМС
            new sendSMS(MY_NUMBER_LOCAL, textMessage);
    }
}
