package ru.microsave.tempmonitor;
/*
Класс отправки СМС в фоновом режиме, через AsyncTask
 */

import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;

//import static ru.microsave.tempmonitor.MainActivity.MY_NUMBER;

class SendBackgroundSMS extends AsyncTask<String, Void, Void> {
    private String NUMBER_SMS;
    private String TEXT_SMS;
    private final String LOG_TAG = "myLogs";


    SendBackgroundSMS(String num, String message){
        NUMBER_SMS = num;
        TEXT_SMS = message;
    }

    @Override
    protected Void doInBackground(String... mess) {

        try {
            SmsManager.getDefault()
                    .sendTextMessage(NUMBER_SMS, null,TEXT_SMS, null, null);
            Log.d(LOG_TAG, mess[0]);
        } catch (Exception e) {
            Log.d(LOG_TAG, "class SendBackgroundSMS failed to send message");
            e.printStackTrace();
        }
        return null;
    }
}
