package ru.microsave.temperature;
/*
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
Нужен ввод для настройки периодичности сообщений







 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
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

    // Для отправки СМС
    public String MY_NUMBER;
    public int WARNING_TEMP;

    private final long ALARM_INTERVAL     = 1000 * 60 * 60 * 2;
    private final long NORMAL_INTERVAL    = 1000 * 60 * 60 * 3;
    private final long mainPeriodic       = 1000 * 60 * 60;
/*
    private long NORMAL_INTERVAL = 1000 * 60 * 60 * 24;
    private long ALARM_INTERVAL = 1000 * 60 * 60 * 2;
    private long mainPeriodic = 1000 * 60 * 60;
*/

    public int mDEGREES;
    public boolean serviseON; // состояние службы боевого дежурства, запущена или нет

    private Sensor mSensorTemperature;
    private SensorManager mSensorManager;
    private SharedPreferences savePref;
    private boolean sensorExist; // наличие сенсора температуры


    private Button mButton,mButton0,mButton1,mButton2;
    private TextView sensorLabel;
    private TextView temperatureLabel;
    private TextView statusLabel;
    private TextView logLabel;
    private TextView numberLabel;


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

        sensorLabel = (TextView) findViewById(R.id.textView0);
        temperatureLabel = (TextView) findViewById(R.id.textView1);
        statusLabel = (TextView) findViewById(R.id.textView2);
        logLabel = (TextView) findViewById(R.id.textView3);
        numberLabel = (TextView) findViewById(R.id.textView4);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        readSharedPreferences();

        String period = String.valueOf((int)mainPeriodic/1000/60/60);
        String pAlarm = String.valueOf((int)ALARM_INTERVAL/1000/60/60);
        String pNormal = String.valueOf((int)NORMAL_INTERVAL/1000/60/60);

        logLabel.setText("t°C = " + WARNING_TEMP +  "," + "Период: " + period +  "," + "Интервалы: " + pAlarm + ", " + pNormal);
        numberLabel.setText("Номер: " + MY_NUMBER);
        invertButton(serviseON);

        // Проверим наличчие сенсора

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH && mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            mSensorTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            sensorExist = true;
            sensorLabel.setText("Датчик t°C - ОК");
        } else {
            // Если нет датчика, скажем об этом
            sensorLabel.setText("Датчика t°C - НЕТ");
            temperatureLabel.setText("?°C");
            sensorExist = false;
            mButton1.setEnabled(false);
            mButton2.setEnabled(false);
        }

        mSensorManager.registerListener(this, mSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(LOG_TAG, "--- onCreate MainActivity ---");
    }

    @Override
    public void onSensorChanged(SensorEvent sensor) {

        if(sensorExist && mSensorTemperature.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){

        // получаю, преобразую в int и сохраняю в degrees
           mDEGREES = (int)sensor.values[0];

          temperatureLabel.setText(mDEGREES + "°C");
       // sendAlarm(degrees);
       // realSMS (degrees);
        }
        else msg("У вас нет датчика температуры");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    // Метод для кнопочки ОСТАНОВИТЬ СЛУЖБУ
    public void stopSheduler (View view){


        stopService(new Intent(this, JobSchedulerService.class));

        serviseON = false;
        invertButton(serviseON);

        statusLabel.setText("Служба остановлена.");
        temperatureLabel.setText(mDEGREES + "°C");
        saveSharedPreferences();
    //    Intent intent = new Intent(this, Control_activity.class);
    //    intent.putExtra("serviceON", false);
    //    startActivity(intent);
        Log.d(LOG_TAG, "--- stopSheduler MainActivity --- serviceON = " + serviseON);
       // finish();
    }

    // Метод для кнопочки БОЕВОЕ ДЕЖУРСТВО
    public void control (View view){

        if (!sensorExist) {
            msg("У вас нет датчика температуры");
            return;
        }

        statusLabel.setText("Служба запущена!!!");
        temperatureLabel.setText(mDEGREES + "°C");
        serviseON = true;
        invertButton(serviseON);
        saveSharedPreferences();
    //    mSensorManager.unregisterListener(this);
        Intent intent = new Intent(this, Control_activity.class);

        // Запретить оптимизировать батарею
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            msg("Служба запускается для API > 22 ");
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.putExtra("serviceIntentON", true);
                intent.putExtra("schedulerPeriodic", mainPeriodic);
                startActivity(intent);
            }
        }
        else {
            intent.putExtra("serviceIntentON", true);
            intent.putExtra("schedulerPeriodic", mainPeriodic);
            startActivity(intent);
            msg("Служба запускается для API < 22");
        }

    }

// ==========================================
    public void inputNumber(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Ведите номер пожалуйста");
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

        alert.setTitle("Ведите минимальную температуру");
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
                logLabel.setText("Минимальная t°C = " + WARNING_TEMP);
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
    private void readSharedPreferences(){
        savePref = getSharedPreferences("ru.microsave.temperature.Prefs", MODE_PRIVATE);

        MY_NUMBER = (savePref.getString("NUMBER", "+7123456789"));
        WARNING_TEMP = (savePref.getInt("WARNING", 16));
        sensorExist = (savePref.getBoolean("IFSENSOR", false));
        serviseON = (savePref.getBoolean("SERVICEON", false));
        invertButton(serviseON);
        if (serviseON)
            statusLabel.setText("Служба была запущена!");
        else
            statusLabel.setText("Служба не была запущена.");
    }
    private void saveSharedPreferences() {
        savePref = getSharedPreferences("ru.microsave.temperature.Prefs", MODE_PRIVATE);
        SharedPreferences.Editor ed = savePref.edit();

        ed.putString("NUMBER", MY_NUMBER);
        ed.putInt("WARNING", WARNING_TEMP);

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
            mButton1.setEnabled(true);
            mButton2.setEnabled(false);
        }
        // Если НЕ было запусков или была остановка
        else {
            mButton1.setEnabled(false);
            mButton2.setEnabled(true);
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
      //  mSensorManager.unregisterListener(this);

    }
    @Override
    protected void onDestroy() {
        saveSharedPreferences();
      //  mSensorManager = null;
      //  mSensorTemperature = null;
        super.onDestroy();
    }
}