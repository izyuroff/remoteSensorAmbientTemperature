package ru.microsave.tempmonitor;

/*
Полезняшки которые возможно надо использовать
 */
class Garbage {


    // =============================================================================================
    // =============================================================================================

/*    public void jobPlan() {
        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(1,new ComponentName(getPackageName(),JobSchedulerService.class.getName()));
        builder.setPeriodic(10000);
        builder.setPersisted(isPersisted);
       // builder.setRequiresCharging(requireCharging); // неясно, только для старта видимо
       // builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // то же самое

      //  JobInfo jobInfo = builder.build();
      //  mJobScheduler.schedule(jobInfo);

        if (mJobScheduler.schedule(builder.build()) <= 0) {
            Log.d(LOG_TAG, "onCreate: Some error while scheduling the job");
        }
    }*/
    // =============================================================================================
    // Метод для ПЕРИОДИЧЕСКОЙ И АВАРИЙНОЙ ОТПРАВКИ СМС ПРИ ИЗМЕНЕНИИ ТЕМПЕРАТУРЫ
 /*   private void sendAlarm(int degrees) {
        if (MY_NUMBER == null)
            return;
        long currentTime = System.currentTimeMillis();
        // Normal SMS
        if ((currentTime - mLastUpdated) > NORMAL_INTERVAL_MILLIS) {
            messageText = "Нормальное сообщение: " + degrees + "°C";
            mLastUpdated = currentTime;
            // new SendSMS().execute(messageText);
            msg(messageText);
        }

        // Alarm SMS
        if ((degrees < WARNING_TEMP) && ((currentTime - mLastMessage) > ALARM_INTERVAL_MILLIS)) {
            messageText = "Аварийная температура: " + degrees + "°C";
            mLastMessage = currentTime;
           // new SendSMS().execute(messageText);
            msg(messageText);
        }
    }*/
    // =============================================================================================
    /*public void inputNumber(View view) {
        // Пока метод не использован - надо отладить проверку ввода и сохранение в настройках
        // до этого номер только лишь мой в виде константы
        msg("вызов метода: inputNumber");
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Ввод номера для СМС");
        alert.setMessage("в формате +7123456789");

// Set an EditText view to get user input
        final EditText input = new EditText(this);

        input.requestFocus();
        //getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        input.setInputType(InputType.TYPE_CLASS_PHONE);  //установит клавиатуру для ввода номера телефона
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                numberText = value;
                numberLabel.setText("Номер для СМС: " + value);
                msg("Введен номер: " + value);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }*/
    // =============================================================================================
    // Проверить наличие сети
    // Пока метод нигде не использован
/*    public boolean isOnline(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(netInfo !=null && netInfo.isConnectedOrConnecting()){
            return true;
        }else {
            return false;
        }
    }*/

    // =============================================================================================
    // Проверка разрешений на отправку SMS
    // Пока не буду использовать
    // Читать тут: https://coderlessons.com/tutorials/mobilnaia-razrabotka/uchitsia-android/android-otpravka-sms
    // Пока не проверяю наличие разрешений
/*        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }*/

/*    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //   SmsManager smsManager = SmsManager.getDefault();
                    //    smsManager.sendTextMessage(phoneNo, null, message, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }*/







}
