package ru.microsave.scanerpro.temp;
/*

Черновик, пробовал смс отправлять в Handler

 */

import android.app.Activity;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;

class SendHandlerSMS extends Activity {
    private final Handler mHandler = new Handler();
    private final String NUMBER_SMS;
    private final String TEXT_SMS;
    private final String LOG_TAG = "myLogs";

    SendHandlerSMS(String num, String message){
        NUMBER_SMS = num;
        TEXT_SMS = message;
    }

    // Описание Runnable-объекта
    private final Runnable timeUpdaterRunnable = new Runnable() {
        public void run() {
            try {
                SmsManager.getDefault()
                        .sendTextMessage(NUMBER_SMS, null,TEXT_SMS, null, null);
                Log.d(LOG_TAG, "class SendHandlerSMS well to send message");
            } catch (Exception e) {
                Log.d(LOG_TAG, "class SendHandlerSMS failed to send message");
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Добавляем Runnable-объект
        mHandler.postDelayed(timeUpdaterRunnable, 5000);
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Удаляем Runnable-объект для прекращения задачи
        mHandler.removeCallbacks(timeUpdaterRunnable);
    }
}
