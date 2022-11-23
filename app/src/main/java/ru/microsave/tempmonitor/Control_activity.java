package ru.microsave.tempmonitor;
/*
Класс контроллер
Сюда передаются настройки пользователя: период теста, интервалы сообщений
(период теста теперь константа, не надо ег менять пользователям)
В нем настраивается и вызывается служба шедулера

Планируется два разных задания - отдельно для нормал и аларм
а также для разных типов устройств, до API 24 и после него
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

public class Control_activity extends AppCompatActivity {
    private final String LOG_TAG = "myLogs";
    private JobScheduler mJobScheduler;
    private JobScheduler mJobSchedulerAlarm;

    private long mPeriodic;
    private long mFlexPeriodic;

    private long mPeriodicAlarm;
    private long mFlexPeriodicAlarm;

    private int multiAlarm; // Количество часов - множитель, умножаем потом на 1000*60*60
    private int multiNormal; // Количество часов - множитель, умножаем потом на 1000*60*60


    private static final int mJobId = 100500777;
    private static final int mJobAlarmId = 100500778;
    private boolean serviceONlocal;// состояние службы дежурства, запущена или нет
    private boolean isPersisted = true; // Сохранять планировщик после рестарта устройства


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        mJobSchedulerAlarm = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        Intent intent = getIntent();
        serviceONlocal = intent.getBooleanExtra("serviceIntentON", true);
        multiAlarm = intent.getIntExtra("ALARM_HOURS", 1);
        multiNormal = intent.getIntExtra("NORMAL_HOURS", 1);

        if (serviceONlocal) {
            Log.d(LOG_TAG, "--- jobPlan --- ");
            jobPlan();
        } else {
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
        // mJobScheduler.cancelAll();

        Log.d(LOG_TAG, "jobPlan() Build.VERSION.SDK_INT = " + Build.VERSION.SDK_INT);

        ComponentName componentName = new ComponentName(this, JobSchedulerService.class);
        final JobInfo jobInfo;

        ComponentName componentNameAlarm = new ComponentName(this, JobSchedulerServiceAlarm.class);
        final JobInfo jobInfoAlarm;
        // ===================================================================================
        // ======================= Планируем jobInfo ===========================================
        // Инициализация планировщика для API >= 24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            // Дает в результате первый раз 10 минут потом по 15 минут ровно
            //mPeriodic = 1000 * 60 * 25; // Решено посмотреть на разных устройствах качество срабатывания
            //mFlexPeriodic = 1000 * 60 * 5; // Решено посмотреть на разных устройствах качество срабатывания

            // Дает в результате первый раз 1 минуту 6 секунд, потом по 15 минут примерно почти ровно, например 14,55 и 16,02
            // mPeriodic = 1000 * 60 * 15; // Решено посмотреть на разных устройствах качество срабатывания
            // mFlexPeriodic = 1000 * 60 * 14; // Решено посмотреть на разных устройствах качество срабатывания

            // Это дало срабатывания 35, 36, 37 минут
            // mPeriodic = 1000 * 60 * 35; // Решено посмотреть на разных устройствах качество срабатывания
            // mFlexPeriodic = 1000 * 60 * 30; // Решено посмотреть на разных устройствах качество срабатывания

            // Это дало срабатывания 5, 65, 55, 61, 64, 60, 54, 59 минут
            mPeriodic = multiNormal * 1000 * 60 * 60; // Решено посмотреть на разных устройствах качество срабатывания
            mFlexPeriodic = multiNormal * 1000 * 60 * 55; // Решено посмотреть на разных устройствах качество срабатывания

            jobInfo = new JobInfo.Builder(mJobId, componentName)
                    .setRequiresCharging(false)// Не требовать быть на зарядке
                    .setPeriodic(mPeriodic, mFlexPeriodic)// Во втором параметре, значение flex..
                    //.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis())// Во втором параметре, значение flex..
                    .setPersisted(isPersisted)// Для восстановления после перезагрузки
                    .build();
        }

        // Инициализация планировщика для API < 24
        // Для устройств менее, чем Build.VERSION_CODES.N
        else {
            mPeriodic = 1000 * 60 * 1;
            jobInfo = new JobInfo.Builder(mJobId, componentName)
                    .setRequiresCharging(false)
                    // .setMinimumLatency(5000) - Пробовать
                    .setPeriodic(mPeriodic) // Период запусков теста должен быть меньше(?), чем переменные *_INTERVAL
                    .setPersisted(isPersisted)
                    .build();
        }
        mJobScheduler.schedule(jobInfo);
        // Код ошибки запуска планировщика
        if (mJobScheduler.schedule(jobInfo) <= 0) {
            Log.d(LOG_TAG, "onCreate: Some error, jobInfo = " + jobInfo);
        } else Log.d(LOG_TAG, "jobInfo: Все запланировалось успешно");
        // ===================================================================================
        // ===================================================================================
        // ======================= Планируем jobInfoAlarm ===========================================
        // Инициализация планировщика для API >= 24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {



            //mPeriodicAlarm = 1000 * 60 * 15; // Решено посмотреть на разных устройствах качество срабатывания
            //mFlexPeriodicAlarm = 1000 * 60 * 14; // Решено посмотреть на разных устройствах качество срабатывания


            mPeriodicAlarm = multiAlarm * 1000 * 60 * 60; // Решено посмотреть на разных устройствах качество срабатывания
            mFlexPeriodicAlarm = multiAlarm * 1000 * 60 * 59; // Решено посмотреть на разных устройствах качество срабатывания

            jobInfoAlarm = new JobInfo.Builder(mJobAlarmId, componentNameAlarm)
                    .setRequiresCharging(false)// Не требовать быть на зарядке
                    .setPeriodic(mPeriodicAlarm, mFlexPeriodicAlarm)// Во втором параметре, значение flex..
                    .setPersisted(isPersisted)// Для восстановления после перезагрузки
                    .build();
        }

        // Инициализация планировщика для API < 24
        // Для устройств менее, чем Build.VERSION_CODES.N
        else {
            mPeriodicAlarm = 1000 * 60 * 1;
            jobInfoAlarm = new JobInfo.Builder(mJobAlarmId, componentNameAlarm)
                    .setRequiresCharging(false)
                    .setPeriodic(mPeriodicAlarm)
                    .setPersisted(isPersisted)
                    .build();
        }
        mJobSchedulerAlarm.schedule(jobInfoAlarm);

        // Код ошибки запуска планировщика
        if (mJobSchedulerAlarm.schedule(jobInfo) <= 0) {
            Log.d(LOG_TAG, "onCreate: Some error, jobInfoAlarm = " + jobInfoAlarm);
        } else Log.d(LOG_TAG, "jobInfoAlarm: Все запланировалось успешно");
        // ===================================================================================
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
}