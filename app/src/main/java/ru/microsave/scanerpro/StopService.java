package ru.microsave.scanerpro;

/*

Класс не нужен, только как пример остановки сервисов

 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class StopService extends Service {
    public StopService() {
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        //stop you jobservice from here
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}