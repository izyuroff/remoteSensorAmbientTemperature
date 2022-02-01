package ru.microsave.tempmonitor;
/*
tempmonitor
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
Проверка на наличие датчика - и никаких действий если его нет

Ввод номера телефона пользователем
Ввод уровня низкой температуры,

Пробуждение смартфона от сна, реализовано настройкой класса JobScheduler
Аварийное сообщение при низком уровне температуры, отправка СМС каждый час
Регулярное сообщение о работоспособности устройства - отправка СМС раз в 12 часов



---= Добавить в будущем =---
Также сигнализация о работоспособности устройства - уровень батареи например
и может быть можно менять текст сообщения
Нужен ввод для настройки периодичности сообщений FIX

Нужно можно вводить несколько номеров для СМС
Снимать температуру также с батареи FIX

 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Для отправки СМС implements View.OnClickListener
    public String MY_NUMBER;
    public int WARNING_TEMP;

    private long ALARM_INTERVAL; //     = 1000 * 60 * 60 * 2;
    private long NORMAL_INTERVAL; //     = 1000 * 60 * 60 * 3;
    private long mainPeriodic; //        = 1000 * 60 * 60;
/*
    private long NORMAL_INTERVAL = 1000 * 60 * 60 * 24;
    private long ALARM_INTERVAL = 1000 * 60 * 60 * 2;
    private long mainPeriodic = 1000 * 60 * 60;
*/

    public int mDEGREES;
    public boolean serviseON; // состояние службы боевого дежурства, запущена или нет
    public boolean sensorExist; // наличие сенсора температуры
    private Sensor mSensorTemperature;
    private SensorManager mSensorManager;
    private SharedPreferences savePref;



    private Button mButton,mButton0,mButton1,mButton2, mButton3,mButton4,mButton5;
    private TextView sensorLabel,temperatureLabel,batteryLabel,statusLabel,logLabel,numberLabel;


    private final String LOG_TAG = "myLogs";
    // private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);*/

        mButton = findViewById(R.id.button);
        mButton0 = findViewById(R.id.button0);
        mButton1 = findViewById(R.id.button1);
        mButton2 = findViewById(R.id.button2);
        mButton3 = findViewById(R.id.button3);
        mButton4 = findViewById(R.id.button4);
        mButton5 = findViewById(R.id.button5);

        sensorLabel = findViewById(R.id.textView);
        temperatureLabel = findViewById(R.id.textView1);
        statusLabel = findViewById(R.id.textView2);
        logLabel = findViewById(R.id.textView3);
        numberLabel = findViewById(R.id.textView4);
        batteryLabel = findViewById(R.id.textView5);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        readSharedPreferences();
        updateScreen();
        checkSensor();

        mSensorManager.registerListener(this, mSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(LOG_TAG, "--- onCreate.MainActivity.sensorExist---" + sensorExist);
    }

    private void updateScreen() {
        String period = String.valueOf((int)mainPeriodic/1000/60);
        String pAlarm = String.valueOf((int)ALARM_INTERVAL/1000/60);
        String pNormal = String.valueOf((int)NORMAL_INTERVAL/1000/60);

        logLabel.setText("t°: " + WARNING_TEMP +  ", " + "Тест: " + period +  ", " + "Тревога: " + pAlarm + ", " + "Норма: " + pNormal);
        numberLabel.setText("Номер: " + MY_NUMBER);
        invertButton(serviseON);
    }

    @Override
    public void onSensorChanged(SensorEvent sensor) {
       // Log.d(LOG_TAG, "--- onSensorChanged.sensorExist ---: " + sensorExist);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH && mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {

            if(sensorExist && mSensorTemperature.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
                //if(mSensorTemperature.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){

                // получаю, преобразую в int и сохраняю в degrees
                mDEGREES = (int)sensor.values[0];

                temperatureLabel.setText(mDEGREES + "°C");
                // sendAlarm(degrees);
                // realSMS (degrees);
            }
            // msg("Датчика температуры нет, измеряем t°C CPU");
            else temperatureLabel.setText("Проблема, нет датчика t");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    // Метод для кнопочки ОСТАНОВИТЬ СЛУЖБУ
    public void stopSheduler (View view){

    //    stopService(new Intent(this, JobSchedulerService.class));
        serviseON = false;
        invertButton(serviseON);

        statusLabel.setText("Служба остановлена.");
        temperatureLabel.setText(mDEGREES + "°C");
        saveSharedPreferences();
        Log.d(LOG_TAG, "--- stopSheduler MainActivity --- serviceON = " + serviseON);

        // Может быть надо раскомментировать?
        // mSensorManager.unregisterListener(this);

        Intent intent = new Intent(this, Control_activity.class);
        intent.putExtra("serviceIntentON", serviseON);
        startActivity(intent);
    }

    // Метод для кнопочки БОЕВОЕ ДЕЖУРСТВО
    public void control (View view){

        if (!sensorExist) {
            msg("У вас нет датчика температуры");
            // return;
            temperatureLabel.setText("? t°C");
        }
        else
        {
            temperatureLabel.setText(mDEGREES + "°C");
        }

        serviseON = true;
        invertButton(serviseON);
        saveSharedPreferences();
        statusLabel.setText("Служба запущена!!!");

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

    private void readSharedPreferences(){
        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);

        mainPeriodic = (savePref.getLong("PERIOD_INTERVAL", 1000 * 60 * 15));
        ALARM_INTERVAL = (savePref.getLong("ALARM_INTERVAL", 1000 * 60 * 60 * 1));
        NORMAL_INTERVAL = (savePref.getLong("NORMAL_INTERVAL", 1000 * 60 * 60 * 12));
/*

        mainPeriodic = (savePref.getLong("PERIOD_INTERVAL", 1000 * 60 * 60 * 1));
        ALARM_INTERVAL = (savePref.getLong("ALARM_INTERVAL", 1000 * 60 * 60 * 1));
        NORMAL_INTERVAL = (savePref.getLong("NORMAL_INTERVAL", 1000 * 60 * 60 * 12));
*/

        MY_NUMBER = (savePref.getString("NUMBER", "+7123456789"));
        WARNING_TEMP = (savePref.getInt("WARNING", 15));
        sensorExist = (savePref.getBoolean("IFSENSOR", false));
        serviseON = (savePref.getBoolean("SERVICEON", false));
        invertButton(serviseON);
        if (serviseON)
            statusLabel.setText("Служба была запущена!");
        else
            statusLabel.setText("Служба не была запущена.");
    }
    private void saveSharedPreferences() {
        String period = String.valueOf((int)mainPeriodic/1000/60);
        String pAlarm = String.valueOf((int)ALARM_INTERVAL/1000/60);
        String pNormal = String.valueOf((int)NORMAL_INTERVAL/1000/60);

        logLabel.setText("t°: " + WARNING_TEMP +  ", " + "Тест: " + period +  ", " + "Тревога: " + pAlarm + ", " + "Норма: " + pNormal);


        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        SharedPreferences.Editor ed = savePref.edit();

        ed.putString("NUMBER", MY_NUMBER);
        ed.putInt("WARNING", WARNING_TEMP);

        ed.putLong("PERIOD_INTERVAL", mainPeriodic);
        ed.putLong("NORMAL_INTERVAL", NORMAL_INTERVAL);
        ed.putLong("ALARM_INTERVAL", ALARM_INTERVAL);

        ed.putBoolean("IFSENSOR", sensorExist);
        ed.putBoolean("SERVICEON", serviseON);

        ed.apply();
        ed.commit();
    }
    // ==========================================

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
    }

        // Меняем кнопочки
    private void invertButton(Boolean b)
    {
        // если serviceON
        if (b) {
            mButton.setEnabled(false);
            mButton0.setEnabled(false);
            mButton1.setEnabled(true);
            mButton2.setEnabled(false);
            mButton3.setEnabled(false);
            mButton4.setEnabled(false);
            mButton5.setEnabled(false);
        }
        // Если НЕ было запусков или была остановка
        else {
            mButton.setEnabled(true);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSharedPreferences();

        // Почему то закомментировал, не помню, были проблемы вроде бы
        // Но в то же время надо бы выполнять
        // Да, точно, при уходе в другую активити
       // mSensorManager.unregisterListener(this);

    }
    @Override
    protected void onDestroy() {
        saveSharedPreferences();
      //  mSensorManager = null;
      //  mSensorTemperature = null;
        super.onDestroy();
    }

    public void inputPeriod(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Период проверки в минутах");
            alert.setMessage("Важно: не менее 15 минут!");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);

        input.requestFocus();
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  //установит клавиатуру для ввода номера телефона
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    return;
                }

                int minute = (Integer.parseInt(value));
                mainPeriodic = Long.valueOf(minute * 60 * 1000);
                saveSharedPreferences();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    public void inputAlarma(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Ведите интервал тревоги, в минутах");
        alert.setMessage("по умолчанию 60 минут");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);

        input.requestFocus();
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  //установит клавиатуру для ввода номера телефона
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    return;
                }

                int minute= (Integer.parseInt(value));
                ALARM_INTERVAL = Long.valueOf(minute * 60 * 1000);
                Log.d(LOG_TAG, "--- ALARM_INTERVAL ---" + ALARM_INTERVAL);

                saveSharedPreferences();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    public void inputNormal(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Введите интервал норма, в минутах ");
        alert.setMessage("12 часов = 720 минут");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);

        input.requestFocus();
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  //установит клавиатуру для ввода номера телефона
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    return;
                }

                int minute= (Integer.parseInt(value));
                NORMAL_INTERVAL = Long.valueOf(minute * 60 * 1000);
                Log.d(LOG_TAG, "--- NORMAL_INTERVAL ---" + NORMAL_INTERVAL);

                saveSharedPreferences();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    // ==========================================
    public void inputNumber(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Введите номер пожалуйста");
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
                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    return;
                }
                MY_NUMBER = value;
                numberLabel.setText("Сохранен номер: " + value);
                saveSharedPreferences();
                msg("Введен номер: " + value);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }
    // ==========================================
    public void inputWarning(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Введите минимальную температуру");
        alert.setMessage("пожалуйста");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);

        input.requestFocus();

        input.setInputType(InputType.TYPE_CLASS_NUMBER);  //установит клавиатуру для ввода номера телефона
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());

                // Проверяем поля на пустоту
                if (TextUtils.isEmpty(input.getText().toString())) {
                    return;
                }

                WARNING_TEMP = Integer.parseInt(value);
                saveSharedPreferences();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
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
            sensorLabel.setText("НЕТ ТЕРМОМЕТРА");
            batteryTemp(null);
            //  temperatureLabel.setText("?°C");
            // getCpuTemp();
            sensorExist = false;
            // mButton1.setEnabled(false);
            // mButton2.setEnabled(false);

        }

    }

    public void batteryTemp (View v)
    {
        Intent intent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float  temp   = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)) / 10;

        String message = String.valueOf(temp) + Character.toString ((char) 176) + "C";
        batteryLabel.setText(message);
    }
}