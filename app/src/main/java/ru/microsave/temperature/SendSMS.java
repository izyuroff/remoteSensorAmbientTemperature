package ru.microsave.temperature;

import android.os.AsyncTask;
import android.util.Log;

//import static ru.microsave.temperature.MainActivity.MY_NUMBER;

class  SendSMS extends AsyncTask<String, Void, Void> {
    private final String LOG_TAG = "myLogs";

    @Override
    protected Void doInBackground(String... mess) {

        try {
     //       SmsManager.getDefault()
     //               .sendTextMessage(MY_NUMBER, null, mess[0], null, null);
            Log.d(LOG_TAG, mess[0]);
        } catch (Exception e) {
            Log.d(LOG_TAG, "class SendSMS failed to send message");
            e.printStackTrace();
        }
        return null;
    }
}
