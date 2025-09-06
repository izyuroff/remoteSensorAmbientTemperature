package ru.microsave.scanerpro;
/*
 Вся информация в read.me
 */

import android.Manifest;
//import android.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import ru.rustore.sdk.billingclient.RuStoreBillingClient;
import ru.rustore.sdk.billingclient.RuStoreBillingClientFactory;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int PERMISSION_REQUEST_CODE = 1;

    // Для обновления температуры батареи в UI
    private final Handler mHandler = new Handler();

    // Для отправки СМС implements View.OnClickListener
    public String MY_NUMBER;
    public int WARNING_TEMP;

    // TODO: 06.10.2023 Это же теперь final? На будущее что ли оставил?? Надо убрать! 
    private long ALARM_INTERVAL; //     аварийный интервал всегда в часах
    private long NORMAL_INTERVAL; //    НАСТРОЙКА КОЛИЧЕСТВА ЧАСОВ

    public int mDEGREES;
    public boolean serviseON; // состояние службы боевого дежурства, запущена пользователем или нет,
    public boolean sensorExist; // наличие сенсора температуры
    public boolean messageRead; // сообщение прочитано при первом запуске, больше не выводить
    
    /*
    useFlexTime Влияет на настройку шедулера, для API >= 24 используется два параметра: 
    .setPeriodic(mPeriodic, mFlexPeriodic)
     */
    public boolean useFlexTime; // Эта переменная проверяется в JobSchedulerService и JobSchedulerServiceAlarm
    
    private Sensor mSensorTemperature;
    private SensorManager mSensorManager;
    private SharedPreferences savePref;

    private Button mButton0, mButton1, mButton2, mButton3, mButton4, mButton5;

    private TextView sensorLabel, temperatureLabel, batteryLabel, statusLabel, numberLabel, tvMinimum, tvAlarm, tvStandart, tvCounter, tvTimer, tvTitleTimer, tvStatus;

    private final String LOG_TAG = "myLogs";
    private int mTASK_NUMBER; //Счетчик отправленных сообщений

    // Подсчет времени работы сервиса
    private long mStartTime; // При запуске службы
    private long mStopTime; // При остановке службы
    private long mTimeNow; // Текущее время в методе countTime()
    // mCountedTime = (mCurrentTime - mStartTime);
    private long mCountedTime; // Подсчитанное время работы текущего сеанса работы
    private long mLongTime; // Подсчитанное время работы прошлого запуска
    private long mLastAlarm;
    private long mLastInfo;
    private int mBatteryTemp;
    // private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);*/

        mButton0 = findViewById(R.id.button0);
        mButton1 = findViewById(R.id.button1);
        mButton2 = findViewById(R.id.button2);
        mButton3 = findViewById(R.id.button3);

        mButton4 = findViewById(R.id.button4);
        mButton4.setVisibility(View.GONE);

        mButton5 = findViewById(R.id.button5);

        sensorLabel = findViewById(R.id.textView);
        temperatureLabel = findViewById(R.id.textView1);
        statusLabel = findViewById(R.id.textView2);
        numberLabel = findViewById(R.id.textView4);
        batteryLabel = findViewById(R.id.textView5);
        tvMinimum = findViewById(R.id.textView6);

        tvAlarm = findViewById(R.id.textView8);
        tvAlarm.setVisibility(View.GONE);

        tvStandart = findViewById(R.id.textView9);
        tvCounter = findViewById(R.id.textView11);
        tvTitleTimer = findViewById(R.id.textView3);
        tvTimer = findViewById(R.id.textView7);
        tvStatus = findViewById(R.id.textViewStatus);

        // Прежде всего получим сенсор менеджер
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Тут проверка наличия сенсора и чтение сохраненных настроек
        // Log.d(LOG_TAG, "1 MainActivity sensorExist = " + sensorExist);
        readSharedPreferences();
        Log.d(LOG_TAG, "mTASK_NUMBER AFTER READ = " + mTASK_NUMBER);
        // Log.d(LOG_TAG, "2 MainActivity sensorExist = " + sensorExist);
        updateScreen();

        checkSensor();

        Log.d(LOG_TAG, "MainActivity sensorExist = " + sensorExist);

        // Только после проверки регистрируем листенер
        if (sensorExist) {
            mSensorManager.registerListener(this, mSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        }



        // Запрос пермишна (закомментил if после минимального sdk = 23
   //     if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_DENIED) {

                Log.d("permission", "permission denied to SEND_SMS - requesting it");
                String[] permissions = {Manifest.permission.SEND_SMS};

                requestPermissions(permissions, PERMISSION_REQUEST_CODE);

            }
   //     }



        // Типичный листенер заменил на лямбду
        mButton2.setOnClickListener(view -> MainActivity.this.startSheduler());



        // TODO: 22.09.2023
        // Далее для RUSTORE поддержка встроенных покупок
        final Context context  = getApplicationContext();
        final String consoleApplicationId = "2063500489";
        final String deeplinkScheme = "yourappscheme";


        // Опциональные параметры - пока не надо
    //    final BillingClientThemeProvider themeProvider = null;
    //    final boolean debugLogs = false;
    //    final ExternalPaymentLoggerFactory externalPaymentLoggerFactory = null;



        RuStoreBillingClient billingClient = RuStoreBillingClientFactory.INSTANCE.create(
                context,
                consoleApplicationId,
                deeplinkScheme
                // Опциональные параметры
               // themeProvider
                // debugLogs,
                //externalPaymentLoggerFactory
        );











        // TODO: 02.11.2022  Блокировка экрана, чтоб не выключался (для отладки, потом убрать)
/*        PowerManager pm=(PowerManager) this.getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag")
        PowerManager.WakeLock wl= pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MYTAG");
//Осуществляем блокировку
        wl.acquire();*/
    }

    // Объект для контроля за внезапными остановками основной службы.
   // PeriodicWorkRequest myWorkRequest = new PeriodicWorkRequest.Builder(MyWorker.class, 60, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)

    // Для теста настроил на 20 минут
/*
    PeriodicWorkRequest myWorkRequest = new PeriodicWorkRequest.Builder(MyWorker.class, 20, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)
            //   PeriodicWorkRequest myWorkRequest = new PeriodicWorkRequest.Builder(MyWorker.class, PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS , TimeUnit.MINUTES, MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MINUTES)
            .addTag("checkService")
            //.setInitialDelay(15, TimeUnit.MINUTES) //Начальная задержка
            .build();
*/



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            case R.id.action_info:
                messageBattery();
                return true;
/*
            case R.id.action_test:
                testSMS();
                return true;
*/


            case R.id.action_count:
                reset_counter();
                return true;

/*
            case R.id.action_inc_count:
                inc_counter();
                return true;
*/

/*
            case R.id.action_test_Job:
                checkJobService();
                return true;
*/

/*
            case R.id.action_settings:
                return true;

            case R.id.open_privacy:
                return true;

            case R.id.open_about:
                return true;
*/
        }
        //headerView.setText(item.getTitle());
        return super.onOptionsItemSelected(item);
    }

    // Описание Runnable-объекта
    // Тут у нас цикличный опрос температуры батареи
    private final Runnable timeUpdaterRunnable = new Runnable() {
        public void run() {
            // вычисляем время (Кажется этот код из какого то примера по Handler:))))
/*            final long start = mTime;
            long millis = SystemClock.uptimeMillis() - start;
            int second = (int) (millis / 1000);
            int min = second / 60;
            second = second % 60;
            // выводим время
*/
            batteryTemp();
            // batteryLabel.setText("" + min + ":" + String.format("%02d", second));
            // повторяем через каждые 1000 миллисекунд
            mHandler.postDelayed(this, 2000);
        }
    };

    private void updateScreen() {
        //String period = String.valueOf((int)mainPeriodic/1000/60);
        String pAlarm = String.valueOf((int) ALARM_INTERVAL);
        String pNormal = String.valueOf((int) NORMAL_INTERVAL);
        // mainPeriodic сделал константой  на 15 минут
        //    dataLabel.setText("t°: " + WARNING_TEMP +  ", " + "Тест: " + period +  ", " + "Тревога: " + pAlarm + ", " + "Норма: " + pNormal);
        tvMinimum.setText(WARNING_TEMP + getString(R.string.symbol_degrees));
        tvAlarm.setText("" + pAlarm);
        tvStandart.setText("" + pNormal);
        numberLabel.setText(MY_NUMBER);
        invertButton(serviseON);
    }

    @Override
    public void onSensorChanged(SensorEvent sensor) {
        // В этом методе не нужно ничего лишнего! Если есть сенсор, то здесь большая цикличная работа
        // Метод кстати не связан с отправкой СМС, это только для вывода температуры на экран, когда приложение активно,
        // для отправки СМС работает другой аналогичный метод в классе JobTask
        mDEGREES = (int) sensor.values[0];
        temperatureLabel.setText(mDEGREES + getString(R.string.symbol_degrees));

        // Поначал наделал тут проверок, ничего не нужно оказалось
/*        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH && mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {

            // ### Вот эта проверка больше нигде не встречается:
            if(sensorExist && mSensorTemperature.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
                // получаю, преобразую в int и сохраняю в degrees
                mDEGREES = (int)sensor.values[0];
                temperatureLabel.setText(mDEGREES + getString(R.string.symbol_degrees));
            }
            // Кстати, else никогда не выполняется, если нет сенсора то и в этот метод не попадем!
            else temperatureLabel.setText("-----");
        }*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    // Метод для кнопочки ОСТАНОВИТЬ СЛУЖБУ
    public void stopSheduler(View view) {
    //    stopWorking();
        //    stopService(new Intent(this, JobSchedulerService.class));
        serviseON = false;
        invertButton(serviseON);
        reset_timer();
        mStopTime = System.currentTimeMillis();
        mLongTime = mStopTime - mStartTime;

        statusLabel.setText(R.string.setServiceStop);
        // Log.d(LOG_TAG, "6 MainActivity sensorExist = " + sensorExist);

        // Это зачем тут было не помню
        if (sensorExist) temperatureLabel.setText(mDEGREES + getString(R.string.symbol_degrees));
        // Log.d(LOG_TAG, "MainActivity sensorExist = " + sensorExist);

        Log.d(LOG_TAG, "--- stopSheduler MainActivity --- serviceON = " + serviseON);
        saveSharedPreferences();

        // Может быть надо раскомментировать?
        // mSensorManager.unregisterListener(this);

        Intent intent = new Intent(this, Control_activity.class);
        intent.putExtra("serviceIntentON", serviseON);
        startActivity(intent);
    }

    // Метод для кнопочки БОЕВОЕ ДЕЖУРСТВО, то есть СТАРТ
    private void saveSharedPreferences() {
        String pAlarm = String.valueOf((int) ALARM_INTERVAL);
        String pNormal = String.valueOf((int) NORMAL_INTERVAL);

        tvMinimum.setText(WARNING_TEMP + getString(R.string.symbol_degrees));
        tvAlarm.setText("" + pAlarm);
        tvStandart.setText("" + pNormal);
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);

        SharedPreferences.Editor ed = savePref.edit();
        ed.putInt("TASK_NUMBER", mTASK_NUMBER);
        ed.putLong("LAST_ALARM", mLastAlarm);
        ed.putLong("LAST_INFO", mLastInfo);

        ed.putString("NUMBER", MY_NUMBER);
        ed.putInt("WARNING", WARNING_TEMP);

        // ed.putLong("PERIOD_INTERVAL", mainPeriodic);
        ed.putLong("NORMAL_INTERVAL", NORMAL_INTERVAL);
        ed.putLong("ALARM_INTERVAL", ALARM_INTERVAL);
        ed.putLong("START_TIME", mStartTime);
        ed.putLong("STOP_TIME", mStopTime);
        ed.putLong("LONG_TIME", mLongTime);

        ed.putBoolean("IFSENSOR", sensorExist);
        ed.putBoolean("USE_FLEX_TIME", useFlexTime);
        ed.putBoolean("MESSAGEREAD", messageRead);
        ed.putBoolean("SERVICEON", serviseON);

        ed.apply();
    }

    public void startSheduler() {
    //    startWorking();
        msgShort(getString(R.string.msgServiceON));
        serviseON = true;
        invertButton(serviseON);
        mStartTime = System.currentTimeMillis();
        saveSharedPreferences();
        statusLabel.setText(R.string.setServiceStart);

        // Может быть надо раскомментировать?
        // mSensorManager.unregisterListener(this);
        Intent intent = new Intent(this, Control_activity.class);
        //    msg("Служба запускается для API = " + Build.VERSION.SDK_INT);


        // Проверить условие Запретить оптимизировать батарею
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        // TODO: 22.09.2023 Если публиковать в гугл маркете - удалить пермишн!
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                msg(getString(R.string.msgBatteryIgnore));
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            }

            intent.setData(Uri.parse("package:" + packageName));


        intent.putExtra("serviceIntentON", serviseON);
        intent.putExtra("ALARM_HOURS", ALARM_INTERVAL);
        intent.putExtra("NORMAL_HOURS", NORMAL_INTERVAL);
        startActivity(intent);

    }

    private void readSharedPreferences() {
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        // mainPeriodic = (savePref.getLong("PERIOD_INTERVAL", 1000 * 60 * 15));
        ALARM_INTERVAL = (savePref.getLong("ALARM_INTERVAL", 1));
        NORMAL_INTERVAL = (savePref.getLong("NORMAL_INTERVAL", 12));
        mStartTime = (savePref.getLong("START_TIME", 0));
        mStopTime = (savePref.getLong("STOP_TIME", 0));
        mLongTime = (savePref.getLong("LONG_TIME", 0));

        MY_NUMBER = (savePref.getString("NUMBER", "+7123456789"));
        WARNING_TEMP = (savePref.getInt("WARNING", 10));
        mTASK_NUMBER = (savePref.getInt("TASK_NUMBER", 0));
        mLastAlarm = (savePref.getLong("LAST_ALARM", 0L));
        mLastInfo = (savePref.getLong("LAST_INFO", 0L));
        sensorExist = (savePref.getBoolean("IFSENSOR", false));
        messageRead = (savePref.getBoolean("MESSAGEREAD", false));
        useFlexTime = (savePref.getBoolean("USE_FLEX_TIME", false));
        serviseON = (savePref.getBoolean("SERVICEON", false));
        invertButton(serviseON);
        if (serviseON)
            statusLabel.setText(R.string.setServiceStart);
        else
            statusLabel.setText(R.string.setServiceStop);
    }


    private void resetSharedPreferences() {
        // Планировал возможность сброса к дефолтным настройкам, пока решил
        // опцией в манифесте android:allowBackup="false"
        SharedPreferences.Editor ed = savePref.edit();
        ed.putInt("TASK_NUMBER", 0);
        ed.putLong("LAST_ALARM", 0);
        ed.putLong("LAST_INFO", 0);
        ed.putString("NUMBER", "+7123456789");
        ed.putInt("WARNING", 5);
        ed.putInt("NORMAL_INTERVAL", 6);
        ed.putInt("ALARM_INTERVAL", 1);
        ed.putLong("START_TIME", 0);
        ed.putLong("STOP_TIME", 0);
        ed.putLong("LONG_TIME", 0);
        ed.putBoolean("IFSENSOR", false);
        ed.putBoolean("USE_FLEX_TIME", false);
        ed.putBoolean("MESSAGEREAD", false);
        ed.putBoolean("SERVICEON", false);
        ed.apply();
        //updateScreen();
    }

    // ==========================================

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    // fast way to call Toast
    private void msgShort(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    // Меняем кнопочки
    private void invertButton(Boolean b) {
        // если serviceON
        if (b) {
            mButton1.setBackgroundResource(R.drawable.red_button);
            mButton2.setBackgroundResource(R.drawable.gray_button);

            mButton0.setEnabled(false);
            mButton1.setEnabled(true);
            mButton2.setEnabled(false);
            mButton3.setEnabled(false);
            //mButton4.setEnabled(false);
            mButton5.setEnabled(false);
            tvStandart.setEnabled(false);
            tvMinimum.setEnabled(false);
            numberLabel.setEnabled(false);

            mButton2.setVisibility(View.INVISIBLE);
            mButton1.setVisibility(View.VISIBLE);


        }
        // Если НЕ было запусков или была остановка
        else {
            mButton1.setBackgroundResource(R.drawable.gray_button);
            mButton2.setBackgroundResource(R.drawable.red_button);
            mButton0.setEnabled(true);
            mButton1.setEnabled(false);
            mButton2.setEnabled(true);
            mButton3.setEnabled(true);
            //mButton4.setEnabled(true);
            mButton5.setEnabled(true);
            tvStandart.setEnabled(true);
            tvMinimum.setEnabled(true);
            numberLabel.setEnabled(true);

            mButton2.setVisibility(View.VISIBLE);
            mButton1.setVisibility(View.INVISIBLE);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        readSharedPreferences();

        // Добавляем Runnable-объект
        mHandler.postDelayed(timeUpdaterRunnable, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSharedPreferences();

        // Почему то закомментировал, не помню, были проблемы вроде бы
        // Но в то же время надо бы выполнять
        // Да, точно, при уходе в другую активити (что точно? проблемы или нет?)
        // mSensorManager.unregisterListener(this);

        // Удаляем Runnable-объект для прекращения задачи
        mHandler.removeCallbacks(timeUpdaterRunnable);
    }

    @Override
    protected void onDestroy() {
        saveSharedPreferences();
        //  mSensorManager = null;
        //  mSensorTemperature = null;
        super.onDestroy();
    }
/*
    // Этот метод настраивал периоды тревоги - потом я решил что каждый час достаточно
    public void inputAlarma(View view) {
        readSharedPreferences();
        AlertDialog.Builder alert = new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert);

        alert.setTitle(R.string.inputAlarmTitle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            alert.setMessage(getString(R.string.inputAlarmMessage1) + WARNING_TEMP + (getString(R.string.symbol_degrees) +
                    getString(R.string.inputAlarmMessage2) + " " + ALARM_INTERVAL+ " " + getString(R.string.inputAlarmMessage3)));

        else
            alert.setMessage(getString(R.string.inputAlarmMessage1) + WARNING_TEMP + (getString(R.string.symbol_degrees) +
                    getString(R.string.inputAlarmMessage2) + " " + ALARM_INTERVAL));

        // Set an EditText view to get user input
        final EditText input = new EditText(this);

        // input.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
        input.requestFocus();
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  //установит клавиатуру для ввода номера телефона
        alert.setView(input);


        alert.setPositiveButton(R.string.buttonOK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    msg("Выход без изменений!");
                    return;
                }

                int hours = (Integer.parseInt(value));
                // Проверка значения
                if (hours < 1) {
                    msg("Нельзя устанавливать менее 1 часа!");
                    inputAlarma(null);
                } else {
                    ALARM_INTERVAL = Long.valueOf(hours);
                    Log.d(LOG_TAG, "--- ALARM_INTERVAL ---" + ALARM_INTERVAL);
                    saveSharedPreferences();
                }
            }
        });

        alert.setNegativeButton(R.string.buttonCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }
*/

    public void inputNormal(View view) {
        readSharedPreferences();
        AlertDialog.Builder alert = new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert);

        alert.setTitle(R.string.inputNormalTitle);
        alert.setMessage(getString(R.string.inputNormalMessage1) + " " + NORMAL_INTERVAL+ " " + getString(R.string.inputNormalMessage2));

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setHint(R.string.hintNormal);


        // input.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
        input.requestFocus();
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  //установит клавиатуру для ввода номера телефона

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);


        alert.setView(input);

        alert.setPositiveButton(R.string.buttonOK, (dialog, whichButton) -> {
            String value = String.valueOf(input.getText());

            // Проверяем поля на пустоту
            if (TextUtils.isEmpty(input.getText().toString())) {
                msg(getString(R.string.msgCancel));
                return;
            }

            int hours = (Integer.parseInt(value));
            // Проверка значения
            if (hours < 1) {
                msg(getString(R.string.msgMinHours));
                inputNormal(null);
            } else {
                NORMAL_INTERVAL = Long.valueOf(hours);
                Log.d(LOG_TAG, "--- NORMAL_INTERVAL ---" + NORMAL_INTERVAL);
                saveSharedPreferences();
            }


          //  InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            dialog.cancel();
        });
        alert.setNegativeButton(R.string.buttonCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
               // InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                dialog.cancel();
            }
        });
        alert.show();
    }

    // ==========================================
    public void inputNumber(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert);
        alert.setTitle(R.string.inputNumberTitle);
        alert.setMessage(getString(R.string.inputNumberMessage) + " " + MY_NUMBER + "\n\n");
        // TODO: 12.04.2022 Что то тут не так, проверка нужна на правильность ввода номера

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setHint(R.string.prefix);
        // input.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
        input.requestFocus();

        //getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);


        input.setInputType(InputType.TYPE_CLASS_PHONE);  //установит клавиатуру для ввода номера телефона
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        alert.setView(input);

        alert.setPositiveButton(R.string.buttonOK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    msg(getString(R.string.msgCancel));
                    return;
                }


                // TODO: 12.04.2022 Хорошо бы добавить проверку введенного номера на правильность
                MY_NUMBER = value;
                numberLabel.setText(value);
                saveSharedPreferences();
                msg(getString(R.string.setSaveNumber) + " " + value);

                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                dialog.cancel();

            }
        });
        alert.setNegativeButton(R.string.buttonCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                dialog.cancel();
            }
        });
        alert.show();
    }

    // ==========================================
    public void inputWarning(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert);
        alert.setTitle(R.string.inputWarningTitle);
        alert.setMessage(getString(R.string.inputWarningMessage) + " " + WARNING_TEMP + " " + (getString(R.string.symbol_degrees)));

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setHint(R.string.hintWarning);

        //input.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
        input.requestFocus();
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  //установит клавиатуру для ввода номера телефона
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        alert.setView(input);

        alert.setPositiveButton(R.string.buttonOK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    msg(getString(R.string.msgCancel));
                    return;
                }

                // Проверка значения, запрещено устанавливать ноль и ниже
                int minimalTemp = (Integer.parseInt(value));
                if (minimalTemp < 1) {
                    msg(getString(R.string.msgNotZero));
                    inputWarning(null);
                } else {
                    WARNING_TEMP = Integer.parseInt(value);
                    saveSharedPreferences();
                }
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                dialog.cancel();
            }
        });
        alert.setNegativeButton(R.string.buttonCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                dialog.cancel();
            }
        });
        alert.show();
    }

    public void messageBattery() {

        // Сообщение при первом запуске было прочитано
        if (!messageRead) {
            messageRead = true;
            saveSharedPreferences();
        }

        // Собственно диалоговое окно с информацией
        AlertDialog.Builder alert = new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert);
        // Log.d(LOG_TAG, "33 MainActivity sensorExist = " + sensorExist);
        if (!sensorExist) {
            alert.setTitle(R.string.infoBatteryTitle);
            alert.setMessage(R.string.infoBatteryMessage);
        } else {
            alert.setTitle(R.string.infoSensorTitle);
            alert.setMessage(R.string.infoSensorMessage);
        }
        alert.setPositiveButton(R.string.buttonOK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }



    // =============================================================
    // Остальной код (сенсоры, сервисы, SharedPreferences и т.д.)
    // --- оставил без изменений ---
    // =============================================================


    private void checkSensor() {
        // Проверим наличчие сенсора

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            sensorLabel.setText(R.string.setThermometer);
            sensorExist = true;
            mSensorTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            //    mSensorTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT);

        } else {
            // Если нет датчика, скажем об этом

            sensorExist = false;
            temperatureLabel.setText(R.string.tvLine);
        }

        if (!messageRead) {
            // Если не читали, значит первый раз в приложении
            messageBattery();
        }

        // Инициализация планировщика для разных API , если >= 24 то true
        // Эта переменная проверяется в JobSchedulerService и JobSchedulerServiceAlarm
        useFlexTime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
            saveSharedPreferences();
    }
    // вызывается из меню для проверки работы службы
    public boolean checkJobService() {
        // https://overcoder.net/q/1601745/%D0%BA%D0%B0%D0%BA-%D0%BF%D1%80%D0%BE%D0%B2%D0%B5%D1%80%D0%B8%D1%82%D1%8C-%D1%80%D0%B0%D0%B1%D0%BE%D1%82%D0%B0%D0%B5%D1%82-jobservice-%D0%B8%D0%BB%D0%B8-%D0%BD%D0%B5%D1%82-%D0%B2-android
        // https://clck.ru/32YZ7j
        JobScheduler scheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 1077 | jobInfo.getId() == 2078 ) {
                hasBeenScheduled = true;
                break;
            }
        }
        if (hasBeenScheduled)
            msg(getString(R.string.msgServiceWorking));
        else
            msg(getString(R.string.msgServiceNotWorking));

        return hasBeenScheduled;
    }

    public void checkService(){
        // автоматическая проверка службы, фактически работает или нет
        // Автоматическая проверка статуса, асинхронно
        JobScheduler s = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean serviseCheck = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // небольшая оптимизация, чтобы не перебирать все задачи
            // Это только для андроида 7 и выше
            if (s.getPendingJob(1077) != null | s.getPendingJob(2078) != null)
                serviseCheck = true;
        }

        else {
            // Если ниже, то просто перебираем все запущенные задачи
            for (JobInfo jobInfo1 : s.getAllPendingJobs()) {
                if (jobInfo1.getId() == 1077 | jobInfo1.getId() == 2078) {
                    serviseCheck = true;
                    break;
                }
            }
        }


        if (serviseCheck) {
            tvStatus.setText(R.string.setServiceWorking);
            if (!serviseON)
                tvStatus.setText(R.string.setServiceNotStopped);
        }
        else {
            tvStatus.setText(R.string.setServiceNotWorking);
            if (serviseON) {
                tvStatus.setText(R.string.setServiceFailure);
                stopSheduler(null);
            }
        }
    }

    public void batteryTemp() {
        // Также каждые три секунды проверяем изменение счетчика СМС в сохраненном файле
        readCounter(); // нижеперечисленные методы вложил в другие методы по цепочке, иначе они не всегда срабатывают
        //countTime(); // Сперва подсчет времени
        // checkService(); // После всего проверка состояния службы

        Intent intent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        try {
            mBatteryTemp = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;
        } catch (Exception e) {
            Log.d(LOG_TAG, "batteryTemp error, mBatteryTemp = " + mBatteryTemp);
            throw new RuntimeException(e);
        }

        // Строка для вывод значения температуры батареи
        String message = String.valueOf(mBatteryTemp) + (char) 176 + "C";
        batteryLabel.setText(message);
    }


    private void inc_counter() {
        mTASK_NUMBER++;
        tvCounter.setText("" + mTASK_NUMBER);
        saveSharedPreferences();
    }

    private void readCounter() {
    //    savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
    //    mTASK_NUMBER = (savePref.getInt("TASK_NUMBER", 0));

        readSharedPreferences();
        tvCounter.setText("# " + mTASK_NUMBER);
        countTime();
    }

    private void reset_counter() {
        mTASK_NUMBER = 0; // Сбросить счетчик сообщений можно через меню
        // reset_timer();
    //    resetSharedPreferences();
        saveSharedPreferences();
    }

    private void reset_timer() {
        mLastAlarm = 0;
        mLastInfo = 0;
    }

    private void countTime() {
        // Подсчет и вывод на экран времени работы приложения
        if (serviseON) {

            tvTitleTimer.setText(R.string.setTimerSession);
            mTimeNow = System.currentTimeMillis();
            //    Log.d(LOG_TAG, "Текущая сессия mTimeNow: " + mTimeNow);

        // Решил, что избыточно запрашивать
        //    savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        //    mStartTime = (savePref.getLong("START_TIME", 0));
            //    Log.d(LOG_TAG, "Текущая сессия mStartTime: " + mStartTime);

            mCountedTime = mTimeNow - mStartTime;
            //    Log.d(LOG_TAG, "Текущая сессия mTimeNow - mStartTime: " + mCountedTime);


            //String workedTime = DateFormat.getDateTimeInstance().format(new Date(mCountedTime));

            // long weeks = mCountedTime / 604800;
            //long days = (mCountedTime % 604800) / 86400;

            long days = TimeUnit.MILLISECONDS.toDays(mCountedTime);
            long hours = TimeUnit.MILLISECONDS.toHours(mCountedTime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(mCountedTime);

            days = days % 365;
            hours = hours % 24;
            minutes = minutes % 60;

            //    long days = mCountedTime / (365 * 24 * 60 * 60 * 1000) % 86400000;
            //    long hours = mCountedTime / (60 * 60 * 1000) % 24;
            //    long minutes = (mCountedTime / (60 * 1000) % 60);


            //  long hours = ((mCountedTime % 604800) % 86400) / 3600;
            //  long minutes = (((mCountedTime % 604800) % 86400) % 3600) / 60;
            //  long seconds = (((mCountedTime % 604800) % 86400) % 3600) % 60;

            tvTimer.setText(days + " " + getString(R.string.tvTimerDays) + " " + hours + " " + getString(R.string.tvTimerHours) + " " + minutes + " " + getString(R.string.tvTimerMins));

            //tvTimer.setText(mCountedTime + "");
            //    Log.d(LOG_TAG, days +" дней, " + hours + " часов, " + minutes + " минут");

/*            if (mCountedTime/1000/60 < 60) {
                tvTimer.setText(mCountedTime / 1000 / 60 + " мин.");

                if (mCountedTime/1000/60 < 1440) {
                    tvTimer.setText(mCountedTime / 1000 / 60 + " часы и минуты");
                }
            }
            else
                tvTimer.setText((mCountedTime/1000/60 ) + " дней часов минут");
                Log.d(LOG_TAG, "mCountedTime = " + mCountedTime/1000/60);*/
        } else {
            tvTitleTimer.setText(R.string.setTimerLastSession);
            long days = TimeUnit.MILLISECONDS.toDays(mLongTime);
            long hours = TimeUnit.MILLISECONDS.toHours(mLongTime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(mLongTime);

            hours = hours % 24;
            minutes = minutes % 60;
            tvTimer.setText(days + " " + getString(R.string.tvTimerDays) + " " + hours + " " + getString(R.string.tvTimerHours) + " " + minutes + " " + getString(R.string.tvTimerMins));

            //    Log.d(LOG_TAG, "ЛЕТ " + String.valueOf(mLongTime / (31 * 24 * 60 * 60 * 1000) % 12));
            //    Log.d(LOG_TAG, "МЕСЯЦЕВ " + String.valueOf(mLongTime / (1000 * 60 * 60 * 24 * 30) % 12));
            //    Log.d(LOG_TAG, "ДНЕЙ " + String.valueOf(mLongTime / (24 * 60 * 60 * 1000)));
            //    Log.d(LOG_TAG, "ЧАСОВ " + String.valueOf(mLongTime / (60 * 60 * 1000) % 24));
            //    Log.d(LOG_TAG, "МИНУТ " + String.valueOf(mLongTime / (60 * 1000) % 60));
        }

        checkService(); // После всего проверка службы

    }

    private void testSMS() {
        // Просто проверка отправки СМС прямо из меню
        readSharedPreferences();
        ++mTASK_NUMBER;
        String text;

        if (sensorExist) {
            text = mDEGREES + Character.toString((char) 176) + "C" + ", #" + mTASK_NUMBER + ". " + (getString(R.string.app_name));
        }
        else {
            //batteryTemp();
            text = mBatteryTemp + Character.toString((char) 176) + "C" + ", #" + mTASK_NUMBER + ". " + (getString(R.string.app_name));
        }

        new SendSMS(MY_NUMBER, text);
        // new SendHandlerSMS(MY_NUMBER, "Тест, отправлено " + mTASK_NUMBER);
        saveSharedPreferences();
        Log.d(LOG_TAG, text);

    }

    // При старте службы с кнопки создается отдельный класс, выполняется регулярно для проверки
    // работоспособности jobSheduler
    private void startWorking() {
    // Идея такая - иногда JobSheduler убивается системой, а WorkManager будет проверять это и
    // возобновлять работу, если только служба не была остановлена пользователем

        Log.d(LOG_TAG, "startWorking: start button");

        // Прописана политика - при имеющемся задании не запускать новое а сохранять старое

//        WorkManager.getInstance().enqueueUniquePeriodicWork(
//                "checkService",
//                ExistingPeriodicWorkPolicy.KEEP,
//                myWorkRequest);

    }

    public void stopWorking() {
        Log.d(LOG_TAG, "stopWorking: stop button");
      //  WorkManager.getInstance().cancelWorkById(myWorkRequest.getId());

        //    WorkManager.getInstance().cancelWorkById(myWorkRequest.getId());
        //    WorkManager.getInstance().cancelAllWorkByTag("sms1");
        //    WorkManager.getInstance().cancelAllWork();
    }
}