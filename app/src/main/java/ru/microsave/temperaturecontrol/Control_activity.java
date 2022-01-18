package ru.microsave.temperaturecontrol;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
//  import static ru.microsave.temperature.MainActivity.serviceON;

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
        mPeriodic = intent.getLongExtra("schedulerPeriodic",1000 * 15);
        Log.d(LOG_TAG, "--- onCreate ControlActivity serviceON = " + serviceONlocal);

        if (serviceONlocal)
            jobPlan();
        else
            stopPlanNow();

    }

    public void stopPlan(View v) {
        mJobScheduler.cancelAll();
        serviceONlocal = false;
        Log.d(LOG_TAG, "--- stopPlan ControlActivity serviceON = " + serviceONlocal);
    //    Intent intent = new Intent(this, MainActivity.class);
    //    intent.putExtra("serviceON", false);
    //    startActivity(intent);
      //  finish();
    }

    public void stopPlanNow() {
        serviceONlocal = false;
        Log.d(LOG_TAG, "Cancel all scheduled jobs");
        JobScheduler scheduler = (JobScheduler) getSystemService(
                Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> allPendingJobs = scheduler.getAllPendingJobs();
        for (JobInfo info : allPendingJobs) {
            int id = info.getId();
            scheduler.cancel(id);
        }
        scheduler.cancelAll();

        if (mJobScheduler==null) {
            finish();
        //    Intent intent = new Intent(this, MainActivity.class);
        //    intent.putExtra("STOPSTOPSTOP", false);
        //    startActivity(intent);
        }
        else Log.d(LOG_TAG, "--- А НИЧЕ И Н БЫЛО ЗАПУЩЕНО = " + serviceONlocal);
        finish();
    }

    public void jobPlan() {

        // ==========================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            ComponentName componentName = new ComponentName(this, JobSchedulerService.class);
            final JobInfo jobInfo = new JobInfo.Builder(mJobId, componentName)
                    .setRequiresCharging(false)
                    .setPeriodic(mPeriodic, mPeriodic)
                    .build();
            JobScheduler jobScheduler = (JobScheduler) getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(jobInfo);

            if (mJobScheduler.schedule(jobInfo) <= 0) {
                Log.d(LOG_TAG, "onCreate: Some error, jobInfo = " + jobInfo);
            }
            /*jobInfo = new JobInfo.Builder(mJobId++, new ComponentName(this, JobSchedulerService.class.getName()))
                    .setPeriodic(1*60*1000,30*1000)
                    .build();*/
           // Log.d(LOG_TAG, "VERSION_CODES > N setPeriodic = 1 and 0,5 min, +mJobId = " + mJobId);
            Log.d(LOG_TAG, "onJob ControlActivity jobInfo = " + jobInfo);
        } else
        {
            ComponentName componentName = new ComponentName(this, JobSchedulerService.class);
            final JobInfo jobInfo = new JobInfo.Builder(mJobId, componentName)
                    .setRequiresCharging(false)
                    .setPeriodic(mPeriodic) // Период запусков должен быть больше, чем переменные *_INTERVAL
                    .build();
            JobScheduler jobScheduler = (JobScheduler) getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(jobInfo);

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