package ru.microsave.tempmonitor;


import android.app.Activity;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;

class SendHandlerSMS extends Activity {
    private Handler mHandler = new Handler();
    private String NUMBER_SMS;
    private String TEXT_SMS;
    private final String LOG_TAG = "myLogs";

    SendHandlerSMS(String num, String message){
        NUMBER_SMS = num;
        TEXT_SMS = message;
    }




    // Описание Runnable-объекта
    private Runnable timeUpdaterRunnable = new Runnable() {
        public void run() {
            try {
                SmsManager.getDefault()
                        .sendTextMessage(NUMBER_SMS, null,TEXT_SMS, null, null);
            } catch (Exception e) {
                Log.d(LOG_TAG, "class SendBackgroundSMS failed to send message");
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
