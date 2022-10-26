package ru.microsave.tempmonitor;

import android.telephony.SmsManager;
import android.util.Log;

class sendSMS {
    private String NUMBER_SMS;
    private String TEXT_SMS;
    private final String LOG_TAG = "myLogs";

    public sendSMS(String num, String message) {

        NUMBER_SMS = num;
        TEXT_SMS = message;

        try {
            SmsManager.getDefault()
                    .sendTextMessage(NUMBER_SMS, null, TEXT_SMS, null, null);
            Log.d(LOG_TAG, "new class sendSMS: " + TEXT_SMS);
        } catch (Exception e) {
            Log.d(LOG_TAG, "Failed to send Info Sensor message: " + TEXT_SMS);
            e.printStackTrace();
        }
    }




}
