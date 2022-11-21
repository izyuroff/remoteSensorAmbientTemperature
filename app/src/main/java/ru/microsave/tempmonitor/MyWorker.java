package ru.microsave.tempmonitor;
/*

Заготовка, ничего еще не делал тут

 */


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MyWorker extends Worker {
    private final String LOG_TAG = "myLogs";
    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @NonNull
    @Override
    public Worker.Result doWork() {
        Log.d(LOG_TAG, "doWork: start");

    //    new CheckService();

        Log.d(LOG_TAG, "doWork: end");

        return Worker.Result.success();
    }


  //  OneTimeWorkRequest myWorkRequest = new OneTimeWorkRequest.Builder(MyWorker.class).build();

}
