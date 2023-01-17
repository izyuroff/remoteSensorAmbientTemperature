package ru.microsave.tempmonitor;
/*
Класс отправки СМС в фоновом режиме, через AsyncTask
 */

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;

//import static ru.microsave.tempmonitor.MainActivity.MY_NUMBER;

class SendBackgroundSMS extends AsyncTask<Void, Void, Boolean> {
    private String NUMBER_SMS;
    private String TEXT_SMS;
    private static final SmsManager smsManager = SmsManager.getDefault();
    private static final int SEND_SMS_REQUEST_CODE = 0;
    private Context context;

    SendBackgroundSMS(Context context,String num, String message) {
        this.context = context;
        NUMBER_SMS = num;
        TEXT_SMS = message;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            smsManager.sendTextMessage(
                    NUMBER_SMS,
                    null,
                    TEXT_SMS,
                    PendingIntent.getBroadcast(context, SEND_SMS_REQUEST_CODE, new Intent("SMS_SENT"), 0),
                    PendingIntent.getBroadcast(context, SEND_SMS_REQUEST_CODE, new Intent("SMS_DELIVERED"), 0)
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (success) {
            Log.d("SendBackgroundSMS", "Successfully sent message to " + NUMBER_SMS);
        } else {
            Log.d("SendBackgroundSMS", "Failed to send message to " + NUMBER_SMS);
        }
    }
}

