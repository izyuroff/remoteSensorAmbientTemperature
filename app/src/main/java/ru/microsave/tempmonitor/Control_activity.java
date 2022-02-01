package ru.microsave.tempmonitor;
/*
Класс контроллер
Сюда передаются настройки пользователя: период теста, интервалы сообщений
В нем настраивается и вызывается служба шедулера

 */
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
//  import static ru.microsave.tempmonitor.MainActivity.serviceON;

public class Control_activity extends AppCompatActivity {
    private final String LOG_TAG = "myLogs";
    private JobScheduler mJobScheduler;
    //JobInfo jobInfo;
    private long mPeriodic;

    private static int mJobId = 777;
    private boolean serviceONlocal;
    //private boolean isPersisted = true; // Сохранять планировщик после рестарта устройства
    //private boolean serviceON; // состояние службы дежурства, запущена или нет
    //private boolean requireCharging = true; // требуется обязательная зарядка


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        Intent intent = getIntent();
        serviceONlocal = intent.getBooleanExtra("serviceIntentON",true);
        mPeriodic = intent.getLongExtra("schedulerPeriodic",1000 * 60 * 15); // по умолчанию 15 минут
        Log.d(LOG_TAG, "--- onCreate ControlActivity serviceON = " + serviceONlocal);

        if (serviceONlocal){
            Log.d(LOG_TAG, "--- jobPlan --- ");
            jobPlan();
        }
        else{
            Log.d(LOG_TAG, "--- stopPlanNow --- ");
            stopPlanNow();
        }
    }

    public void stopPlanNow() {
        serviceONlocal = false;

        List<JobInfo> allPendingJobs = mJobScheduler.getAllPendingJobs();
        for (JobInfo info : allPendingJobs) {
            int id = info.getId();
            Log.d(LOG_TAG, "Cancel all scheduled jobs with id = " + id);
            mJobScheduler.cancel(id);
        }

        mJobScheduler.cancelAll();
        Log.d(LOG_TAG, "Cancel all scheduled jobs");
        finish();
    }

    public void jobPlan() {

        // ==========================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ComponentName componentName = new ComponentName(this, JobSchedulerService.class);

            int i = JobInfo.BACKOFF_POLICY_EXPONENTIAL;

            final JobInfo jobInfo = new JobInfo.Builder(mJobId, componentName)
                    //.setPeriodic(1000*60,1000*30)
                    .setPeriodic(mPeriodic, 10 * 60 *1000)
                    //.setOverrideDeadline(60*1000)
                    //.setMinimumLatency(3*1000)
                    //.setMinimumLatency(16*60*1000)
                    .build();
            Log.d(LOG_TAG, "JobInfo.BACKOFF_POLICY_EXPONENTIAL = " + i);
            mJobScheduler.schedule(jobInfo);

            int ret = mJobScheduler.schedule(jobInfo);
            if (ret == JobScheduler.RESULT_SUCCESS) {
                Log.d(LOG_TAG, "Job scheduled successfully ret = " + ret);
            } else {
                Log.d(LOG_TAG, "Job scheduling failed");
            }

            if (mJobScheduler.schedule(jobInfo) <= 0) {
                Log.d(LOG_TAG, "onCreate: Some error, jobInfo = " + jobInfo);
            }
            Log.d(LOG_TAG, "onJob ControlActivity jobInfo = " + jobInfo);
        } else
        {
            ComponentName componentName = new ComponentName(this, JobSchedulerService.class);
            final JobInfo jobInfo = new JobInfo.Builder(mJobId, componentName)
                    .setRequiresCharging(false)
                    .setPeriodic(mPeriodic) // Период запусков должен быть больше(?), чем переменные *_INTERVAL
                    .build();

            mJobScheduler.schedule(jobInfo);

            if (mJobScheduler.schedule(jobInfo) <= 0) {
                Log.d(LOG_TAG, "onCreate: Some error, jobInfo = " + jobInfo);
            }
           // Log.d(LOG_TAG, "VERSION_CODES < N setPeriodic = 15 min, +mJobId = " + mJobId);
            Log.d(LOG_TAG, "onJob ControlActivity mPeriodic = " + mPeriodic);
        }

        // ==========================
       // JobInfo.Builder builder = new JobInfo.Builder(JOB_ID,new ComponentName(getPackageName(),JobSchedulerService.class.getName()));
      //  builder.setPeriodic(10000);
      //  builder.setPersisted(isPersisted);
        // builder.setRequiresCharging(requireCharging); // неясно, только для старта видимо
        // builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // то же самое

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}