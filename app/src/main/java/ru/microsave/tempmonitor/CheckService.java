package ru.microsave.tempmonitor;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;

public class CheckService extends AppCompatActivity {
    private String MY_NUMBER_LOCAL = "+79221897658";
    private String textMessage;
    public boolean hasBeenScheduled;
    private final String LOG_TAG = "myLogs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isJobServiceOn();
    }

    public boolean isJobServiceOn() {

        JobScheduler scheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        hasBeenScheduled = false;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 777 | jobInfo.getId() == 778 ) {
                hasBeenScheduled = true;
                break;
            }
        }

        senderReport();

        return hasBeenScheduled;
    }



    private void senderReport(){

        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence timeStampChar = df.format("kk:mm dd/MM/yy", new Date());

        try {
            textMessage = "Статус службы: " + hasBeenScheduled + ", " + timeStampChar;
            new sendSMS(MY_NUMBER_LOCAL, textMessage);
        } catch (Exception e) {
            Log.d(LOG_TAG, "Сбой отправки СМС");
            e.printStackTrace();
        }

    }
}