package ru.microsave.tempmonitor;
/*
 После запуска первое измерение на сенсоре равно 0:
 #1 17 Апр 2022 г. 13:08:14 ТРЕВОГА: 0°C


 Добавить таймер - отметку времени сколько служба работает, и постить это в смс
 И надо бы и температуру батареи всегда отправлять в СМС


Vivo Y81, наблюдал - после удаления приложения и установки заново - все настройки сохранялись,
очевидно в оболочке Funtouch от Vivo как то сохраняется этот файл,
таким образом хорошо бы добавить кнопку сброса настроек в дефолтные начальные для подобных случаев

Temperature monitor with alarm SMS

RemoteTemperatureGSM
Temperature alarm SMS
Temperature remote monitoring and alarm SMS

Alarm SMS at low temperature of the heating system


Автономная система контроля температуры для смартфона с ОС Андроид
При изменении температуры за заданные пределы отпраляется сообщение SMS на заданный номер

Можно использовать:
Для удаленного контроля температуры, системы отопления
Возможно в качестве пожарной сигнализации


v.1.1
Получение температуры окружающей среды и сигнализация о слишком низком уровне

---= Уже реализовано =---
Проверка на наличие датчика - и (никаких действий если его нет) измерение на аккумуляторе

Ввод номера телефона пользователем
Ввод уровня низкой температуры,

Пробуждение смартфона от сна, реализовано настройкой класса JobScheduler
Измерение температуры происходит каждые 15 минут, менее нельзя из-за особенностей класса JobScheduler
Аварийное сообщение при низком уровне температуры, отправка тревожных СМС каждый час
Регулярное сообщение о работоспособности устройства - отправка нормальных СМС раз в 12 часов



---= Добавить в будущем =---
Также сигнализация о работоспособности устройства - уровень батареи например
и может быть можно менять текст сообщения

Нужно выводить таймер продолжительности работы службы

Нужно добавить политику конфиденциальности

Нужно добавить меню бутерброд или выезжающее сбоку

Нужен ввод для настройки периодичности сообщений FIX

Нужно можно вводить несколько номеров телефонов для СМС

Снимать температуру также с батареи FIX
Не обновляется автоматически, это плохо FIX

Последовательность выполнения
    MainActivity - UI (листенер и измерение батареи только для вывода на экран)
    ControlActivity - Запуск шедулера
    JobSchedulerService - собственно сервис, в нем фильтр условий и запуск разных задач
    JobTask (4 штуки) - непосредственно измерения и отправка СМС


 */

import android.Manifest;
import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private final long mainPeriodic = 1000 * 60 * 15;

    // Для обновления температуры батареи в UI
    private Handler mHandler = new Handler();
    private long mTime = 0L;

    // Для отправки СМС implements View.OnClickListener
    public String MY_NUMBER;
    public int WARNING_TEMP;
    private long ALARM_INTERVAL; //     = 1000 * 60 * 60 * 2;
    private long NORMAL_INTERVAL; //     = 1000 * 60 * 60 * 3;
    public int mDEGREES;
    public boolean serviseON; // состояние службы боевого дежурства, запущена или нет
    public boolean sensorExist; // наличие сенсора температуры
    public boolean messageRead; // сообщение прочитано при первом запуске, больше не выводить
    private Sensor mSensorTemperature;
    private SensorManager mSensorManager;
    private SharedPreferences savePref;

    private Button mButton0,mButton1,mButton2,mButton3,mButton4,mButton5;

    private TextView sensorLabel,temperatureLabel,batteryLabel,statusLabel,numberLabel,tvMinimum,tvAlarm,tvStandart,tvCounter,tvTimer, tvTitleTimer;

    private final String LOG_TAG = "myLogs";
    private int mTASK_NUMBER; //Счетчик отправленных сообщений

    // Подсчет времени работы сервиса
    private long mStartTime; // При запуске службы
    private long mStopTime; // При остановке службы
    private long mTimeNow; // Текущее время в методе countTime()
    // mCountedTime = (mCurrentTime - mStartTime);
    private long mCountedTime; // Подсчитанное время работы текущего сеанса работы
    private long mLongTime; // Подсчитанное время работы прошлого запуска


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
        mButton5 = findViewById(R.id.button5);

        sensorLabel = findViewById(R.id.textView);
        temperatureLabel = findViewById(R.id.textView1);
        statusLabel = findViewById(R.id.textView2);
        numberLabel = findViewById(R.id.textView4);
        batteryLabel = findViewById(R.id.textView5);
        tvMinimum = findViewById(R.id.textView6);
        tvAlarm = findViewById(R.id.textView8);
        tvStandart = findViewById(R.id.textView9);
        tvCounter = findViewById(R.id.textView11);
        tvTitleTimer = findViewById(R.id.textView3);
        tvTimer = findViewById(R.id.textView7);

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
        if (sensorExist)
        mSensorManager.registerListener(this, mSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);

        // Запрос пермишна
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_DENIED) {

                Log.d("permission", "permission denied to SEND_SMS - requesting it");
                String[] permissions = {Manifest.permission.SEND_SMS};

                requestPermissions(permissions, PERMISSION_REQUEST_CODE);

            }
        }

        mButton2.setOnClickListener(view -> startSheduler());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id){

            case R.id.action_info:
                messageBattery();
                return true;

            case R.id.action_test:
                testSMS();
                return true;

            case R.id.action_count:
                reset_counter();
                return true;

            case R.id.action_inc_count:
                inc_counter();
                return true;

            case R.id.action_settings :
                return true;

            case R.id.open_privacy:
                return true;

            case R.id.open_about:
                return true;
        }
        //headerView.setText(item.getTitle());
        return super.onOptionsItemSelected(item);
    }

    // Описание Runnable-объекта
    // Тут у нас цикличный опрос температуры батареи
    private Runnable timeUpdaterRunnable = new Runnable() {
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
            // повторяем через каждые 3000 миллисекунд
            mHandler.postDelayed(this, 3000);
        }
    };

    private void updateScreen() {
        //String period = String.valueOf((int)mainPeriodic/1000/60);
        String pAlarm = String.valueOf((int)ALARM_INTERVAL/1000/60);
        String pNormal = String.valueOf((int)NORMAL_INTERVAL/1000/60);
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
        mDEGREES = (int)sensor.values[0];
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
    public void stopSheduler (View view){

    //    stopService(new Intent(this, JobSchedulerService.class));
        serviseON = false;
        invertButton(serviseON);

        mStopTime = System.currentTimeMillis();
        mLongTime = mStopTime - mStartTime;

        statusLabel.setText("Служба остановлена.");
        // Log.d(LOG_TAG, "6 MainActivity sensorExist = " + sensorExist);

        if (sensorExist)temperatureLabel.setText(mDEGREES + getString(R.string.symbol_degrees));
        // Log.d(LOG_TAG, "MainActivity sensorExist = " + sensorExist);

        saveSharedPreferences();
        Log.d(LOG_TAG, "--- stopSheduler MainActivity --- serviceON = " + serviseON);

        // Может быть надо раскомментировать?
        // mSensorManager.unregisterListener(this);
        Intent intent = new Intent(this, Control_activity.class);
        intent.putExtra("serviceIntentON", serviseON);
        startActivity(intent);
    }

    // Метод для кнопочки БОЕВОЕ ДЕЖУРСТВО, то есть СТАРТ
    public void startSheduler (){
        msg("Служба запущена успешно!");
        serviseON = true;
        invertButton(serviseON);
        mStartTime = System.currentTimeMillis();
        saveSharedPreferences();
        statusLabel.setText("Служба выполняется");

        // Может быть надо раскомментировать?
        // mSensorManager.unregisterListener(this);
        Intent intent = new Intent(this, Control_activity.class);

        // Запретить оптимизировать батарею
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            msg("Служба запускается для API > 22 ");
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.putExtra("serviceIntentON", serviseON);
                intent.putExtra("schedulerPeriodic", mainPeriodic);
                startActivity(intent);
            }
        }
        else {
            msg("Служба запускается для API < 22");
            intent.putExtra("serviceIntentON", serviseON);
            intent.putExtra("schedulerPeriodic", mainPeriodic);
            startActivity(intent);
        }
    }

    private void saveSharedPreferences() {
        // String period = String.valueOf((int)mainPeriodic/1000/60);
        String pAlarm = String.valueOf((int)ALARM_INTERVAL/1000/60);
        String pNormal = String.valueOf((int)NORMAL_INTERVAL/1000/60);

        tvMinimum.setText(WARNING_TEMP + getString(R.string.symbol_degrees));
        tvAlarm.setText("" + pAlarm);
        tvStandart.setText("" + pNormal);
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);

        SharedPreferences.Editor ed = savePref.edit();
        ed.putInt("TASK_NUMBER", mTASK_NUMBER);
        ed.putString("NUMBER", MY_NUMBER);
        ed.putInt("WARNING", WARNING_TEMP);

        // ed.putLong("PERIOD_INTERVAL", mainPeriodic);
        ed.putLong("NORMAL_INTERVAL", NORMAL_INTERVAL);
        ed.putLong("ALARM_INTERVAL", ALARM_INTERVAL);
        ed.putLong("START_TIME", mStartTime);
        ed.putLong("STOP_TIME", mStopTime);
        ed.putLong("LONG_TIME", mLongTime);

        ed.putBoolean("IFSENSOR", sensorExist);
        ed.putBoolean("MESSAGEREAD", messageRead);
        ed.putBoolean("SERVICEON", serviseON);

        ed.apply();
        ed.commit();
    }
    private void readSharedPreferences(){
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        // mainPeriodic = (savePref.getLong("PERIOD_INTERVAL", 1000 * 60 * 15));
        ALARM_INTERVAL = (savePref.getLong("ALARM_INTERVAL", 1000 * 60 * 60 * 1));
        NORMAL_INTERVAL = (savePref.getLong("NORMAL_INTERVAL", 1000 * 60 * 60 * 12));
        mStartTime = (savePref.getLong("START_TIME", 0));
        mStopTime = (savePref.getLong("STOP_TIME", 0));
        mLongTime = (savePref.getLong("LONG_TIME", 0));

        MY_NUMBER = (savePref.getString("NUMBER", "+7123456789"));
        WARNING_TEMP = (savePref.getInt("WARNING", 15));
        mTASK_NUMBER = (savePref.getInt("TASK_NUMBER", 0));

        sensorExist = (savePref.getBoolean("IFSENSOR", false));
        messageRead = (savePref.getBoolean("MESSAGEREAD", false));
        serviseON = (savePref.getBoolean("SERVICEON", false));
        invertButton(serviseON);
        if (serviseON)
            statusLabel.setText("Служба выполняется!");
        else
            statusLabel.setText("Служба остановлена.");
    }
    // ==========================================

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

        // Меняем кнопочки
    private void invertButton(Boolean b)
    {
        // если serviceON
        if (b) {
            mButton1.setBackgroundResource(R.drawable.red_button);
            mButton2.setBackgroundResource(R.drawable.gray_button);

            mButton0.setEnabled(false);
            mButton1.setEnabled(true);
            mButton2.setEnabled(false);
            mButton3.setEnabled(false);
            mButton4.setEnabled(false);
            mButton5.setEnabled(false);

        }
        // Если НЕ было запусков или была остановка
        else {
            mButton1.setBackgroundResource(R.drawable.gray_button);
            mButton2.setBackgroundResource(R.drawable.red_button);
            mButton0.setEnabled(true);
            mButton1.setEnabled(false);
            mButton2.setEnabled(true);
            mButton3.setEnabled(true);
            mButton4.setEnabled(true);
            mButton5.setEnabled(true);
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

    public void inputAlarma(View view) {
        readSharedPreferences();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.inputAlarmTitle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        alert.setMessage(getString(R.string.inputAlarmMessage1) + WARNING_TEMP + (getString(R.string.symbol_degrees) +
                getString(R.string.inputAlarmMessage2) + " " + ALARM_INTERVAL/60/1000 + " " + getString(R.string.inputAlarmMessage3)));

        else
        alert.setMessage(getString(R.string.inputAlarmMessage1) + WARNING_TEMP + (getString(R.string.symbol_degrees) +
                getString(R.string.inputAlarmMessage2) + " " + ALARM_INTERVAL/60/1000));

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
                    Toast.makeText(getApplicationContext(),"Выход без изменений!",Toast.LENGTH_LONG).show();
                    return;
                }

                int minute= (Integer.parseInt(value));
                // Проверка значения
                if (minute < 15 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || minute < 1){
                    Toast.makeText(getApplicationContext(),"Нельзя устанавливать менее 15 минут или 0!",Toast.LENGTH_LONG).show();
                    inputAlarma(null);
                }

                else {
                    ALARM_INTERVAL = Long.valueOf(minute * 60 * 1000);
                    Log.d(LOG_TAG, "--- ALARM_INTERVAL ---" + ALARM_INTERVAL/1000/60);
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

    public void inputNormal(View view) {
        readSharedPreferences();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.inputNormalTitle);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        alert.setMessage(getString(R.string.inputNormalMessage1) + " " + NORMAL_INTERVAL/60/1000 + " " + getString(R.string.inputNormalMessage2));
        else
        alert.setMessage(getString(R.string.inputNormalMessage1) + " " + NORMAL_INTERVAL/60/1000);

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
                    Toast.makeText(getApplicationContext(),"Выход без изменений!",Toast.LENGTH_LONG).show();
                    return;
                }

                int minute= (Integer.parseInt(value));
                // Проверка значения
                if (minute < 15 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || minute < 1){
                    Toast.makeText(getApplicationContext(),"Нельзя устанавливать менее 15 минут или 0!",Toast.LENGTH_LONG).show();
                    inputNormal(null);
                }
                else {
                    NORMAL_INTERVAL = Long.valueOf(minute * 60 * 1000);
                    Log.d(LOG_TAG, "--- NORMAL_INTERVAL ---" + NORMAL_INTERVAL/1000/60);
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

    // ==========================================
    public void inputNumber(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.inputNumberTitle);
        alert.setMessage(getString(R.string.inputNumberMessage) + " " +  MY_NUMBER);
        // TODO: 12.04.2022 Что то тут не так, проверка нужна на правильность ввода номера

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
       // input.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
        input.requestFocus();
        //getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        input.setInputType(InputType.TYPE_CLASS_PHONE);  //установит клавиатуру для ввода номера телефона
        alert.setView(input);

        alert.setPositiveButton(R.string.buttonOK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());
                
                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    Toast.makeText(getApplicationContext(),"Выход без изменений!",Toast.LENGTH_LONG).show();
                    return;
                }


                // TODO: 12.04.2022 Хорошо бы добавить проверку введенного номера на правильность
                MY_NUMBER = value;
                numberLabel.setText("Сохранен номер:" + " " + value);
                saveSharedPreferences();
                msg("Сохранен номер:" + " " + value);
            }
        });
        alert.setNegativeButton(R.string.buttonCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }
    // ==========================================
    public void inputWarning(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.inputWarningTitle);
        alert.setMessage(getString(R.string.inputWarningMessage) +  " " + WARNING_TEMP +  " " + (getString(R.string.symbol_degrees)));

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        //input.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
        input.requestFocus();
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  //установит клавиатуру для ввода номера телефона
        alert.setView(input);

        alert.setPositiveButton(R.string.buttonOK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    Toast.makeText(getApplicationContext(),"Выход без изменений!",Toast.LENGTH_LONG).show();
                    return;
                }

                // Проверка значения, запрещено устанавливать ноль и ниже
                int minimalTemp = (Integer.parseInt(value));
                if (minimalTemp < 1){
                    Toast.makeText(getApplicationContext(),"Нельзя устанавливать 0!",Toast.LENGTH_LONG).show();
                    inputWarning(null);
                }

            else {
                    WARNING_TEMP = Integer.parseInt(value);
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

    public void messageBattery () {

        // Сообщение при первом запуске было прочитано
        if (!messageRead) {
            messageRead = true;
            saveSharedPreferences();
        }

        // Собственно диалоговое окно с информацией
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            // Log.d(LOG_TAG, "33 MainActivity sensorExist = " + sensorExist);
            if (!sensorExist) {
            alert.setTitle(R.string.infoBatteryTitle);
            alert.setMessage(R.string.infoBatteryMessage);
            }
            else {
                alert.setTitle(R.string.infoSensorTitle);
                alert.setMessage(R.string.infoSensorMessage);
            }
            alert.setPositiveButton(R.string.buttonOK, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });

            alert.show();
    }

    private void checkSensor(){
        // Проверим наличчие сенсора

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH && mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            sensorLabel.setText("ТЕРМОМЕТР");
            sensorExist = true;
            mSensorTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            //    mSensorTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT);

        } else {
            // Если нет датчика, скажем об этом

            sensorExist = false;
            temperatureLabel.setText("-----");
        }
        if (!messageRead) {
            messageBattery ();
        }
        saveSharedPreferences();
    }

    public void batteryTemp ()
    {
        // Также каждые три секунды проверяем изменение счетчика СМС в сохраненном файле
        readCounter();
        //countTime();
        Intent intent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int  temp   = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)) /10;

        // Строка для вывод значения температуры батареи
        String message = String.valueOf(temp) + Character.toString ((char) 176) + "C";
        batteryLabel.setText(message);
    }


    private void inc_counter() {
        mTASK_NUMBER++;
        tvCounter.setText("" + mTASK_NUMBER);
        saveSharedPreferences();
    }

    private void readCounter(){
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        mTASK_NUMBER = (savePref.getInt("TASK_NUMBER", 0));
        tvCounter.setText("# " + mTASK_NUMBER);
        countTime();
    }

    private void reset_counter() {
        mTASK_NUMBER = 0; // Сбросить счетчик сообщений можно через меню
        saveSharedPreferences();
    }
    private void countTime(){
        if (serviseON) {

            tvTitleTimer.setText("Текущая сессия:");
            mTimeNow = System.currentTimeMillis();
        //    Log.d(LOG_TAG, "Текущая сессия mTimeNow: " + mTimeNow);


            savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
            mStartTime = (savePref.getLong("START_TIME", 0));
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

            tvTimer.setText( days +" дн, " + hours + " час, " + minutes + " мин");
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
        }
        else {
            tvTitleTimer.setText("Прошлая сессия:");

            long days = TimeUnit.MILLISECONDS.toDays(mLongTime);
            long hours = TimeUnit.MILLISECONDS.toHours(mLongTime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(mLongTime);

            hours = hours % 24;
            minutes = minutes % 60;

            tvTimer.setText( days +" ДН, " + hours + " ЧАС, " + minutes + "МИН");
            //long mLongTime = (24 * 60 * 60 * 1000) * 365;

        //    Log.d(LOG_TAG, " mLongTime:" + mLongTime);

        //    Log.d(LOG_TAG, "ЛЕТ " + String.valueOf(mLongTime / (31 * 24 * 60 * 60 * 1000) % 12));
        //    Log.d(LOG_TAG, "МЕСЯЦЕВ " + String.valueOf(mLongTime / (1000 * 60 * 60 * 24 * 30) % 12));


        //    Log.d(LOG_TAG, "ДНЕЙ " + String.valueOf(mLongTime / (24 * 60 * 60 * 1000)));
        //    Log.d(LOG_TAG, "ЧАСОВ " + String.valueOf(mLongTime / (60 * 60 * 1000) % 24));
        //    Log.d(LOG_TAG, "МИНУТ " + String.valueOf(mLongTime / (60 * 1000) % 60));
        }
    }

    private void testSMS(){

        mTASK_NUMBER++;

        String text = mDEGREES + Character.toString ((char) 176) + "C" + ", #" + mTASK_NUMBER+ ". " + (getString(R.string.app_name));

        new sendSMS (MY_NUMBER, text);
       // new SendHandlerSMS(MY_NUMBER, "Тест, отправлено " + mTASK_NUMBER);
        saveSharedPreferences();
        Log.d(LOG_TAG, text);
    }

}