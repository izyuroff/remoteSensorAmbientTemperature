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

    private static final int mJobId = 777;
    private boolean serviceONlocal;// состояние службы дежурства, запущена или нет
    private boolean isPersisted = true; // Сохранять планировщик после рестарта устройства

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        Intent intent = getIntent();
        serviceONlocal = intent.getBooleanExtra("serviceIntentON",true);

        mPeriodic = intent.getLongExtra("schedulerPeriodic",1000 * 60 * 15); // по умолчанию 15 минут
        //Log.d(LOG_TAG, "--- onCreate ControlActivity serviceON = " + serviceONlocal);

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
        //Log.d(LOG_TAG, "stopPlanNow serviceONlocal = " + serviceONlocal);
        serviceONlocal = false;

        List<JobInfo> allPendingJobs = mJobScheduler.getAllPendingJobs();
        for (JobInfo info : allPendingJobs) {
            int id = info.getId();
        //    Log.d(LOG_TAG, "Cancel all scheduled jobs with id = " + id);
            mJobScheduler.cancel(id);
        }

        mJobScheduler.cancelAll();
        // Log.d(LOG_TAG, "Cancel all scheduled jobs");
        finish(); // Возвращаемся в MainActivity
    }

    public void jobPlan() {
        // Прибить неприбитое?

        try {
            mJobScheduler.cancelAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }

        Log.d(LOG_TAG, "jobPlan() Build.VERSION.SDK_INT = " + Build.VERSION.SDK_INT);

        ComponentName componentName = new ComponentName(this, JobSchedulerService.class);
        final JobInfo jobInfo;

        // Инициализация планировщика два блока для разных устройств
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobInfo = new JobInfo.Builder(mJobId, componentName)
                        .setRequiresCharging(false)// Не требовать быть на зарядке
                        .setPeriodic(mPeriodic, 1000 * 60 * 30)// Во втором параметре, значение для обязательного выполнения
                        .setPersisted(isPersisted)// Для восстановления после перезагрузки
                        .build();
            }

        // Для устройств менее, чем Build.VERSION_CODES.N
        else {
                jobInfo = new JobInfo.Builder(mJobId, componentName)
                        .setRequiresCharging(false)
                        .setPeriodic(mPeriodic) // Период запусков теста должен быть меньше(?), чем переменные *_INTERVAL
                        .setPersisted(isPersisted)
                        .build();
            }
        mJobScheduler.schedule(jobInfo);

        // Код ошибки запуска планировщика
        if (mJobScheduler.schedule(jobInfo) <= 0) {
            Log.d(LOG_TAG, "onCreate: Some error, jobInfo = " + jobInfo);
        }
        else Log.d(LOG_TAG, "Job scheduled successfully Все запланировалось успешно");

        // ========================== Прочие параметры
        // builder.setOverrideDeadline(60*1000)
        // builder.setMinimumLatency(3*1000)
        // builder.setMinimumLatency(16*60*1000)
        // builder.setRequiresCharging(requireCharging);
        // builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        finish(); // Возвращаемся в MainActivity
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

    protected void onAlarmManager() {

    }
}