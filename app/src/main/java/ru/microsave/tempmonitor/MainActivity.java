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
Измерение температуры происходит каждые 15 минут, менее нельзя из-за особенностей класса JobScheduler
Аварийное сообщение при низком уровне температуры, отправка тревожных СМС каждый час
Регулярное сообщение о работоспособности устройства - отправка нормальных СМС раз в 12 часов



---= Добавить в будущем =---
Также сигнализация о работоспособности устройства - уровень батареи например
и может быть можно менять текст сообщения


Нужен ввод для настройки периодичности сообщений FIX

Нужно можно вводить несколько номеров для СМС

Снимать температуру также с батареи FIX
Не обновляется автоматически, это плохо FIX

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
import android.os.SystemClock;
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
    public boolean messageRead; // сообщение прочитано при запуске, больше не выводить
    private Sensor mSensorTemperature;
    private SensorManager mSensorManager;
    private SharedPreferences savePref;

    private Button mButton0,mButton1,mButton2,mButton3,mButton4,mButton5;
    private TextView sensorLabel,temperatureLabel,batteryLabel,statusLabel,dataLabel,numberLabel;

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

        mButton0 = findViewById(R.id.button0);
        mButton1 = findViewById(R.id.button1);
        mButton2 = findViewById(R.id.button2);
        mButton3 = findViewById(R.id.button3);
        mButton4 = findViewById(R.id.button4);
        mButton5 = findViewById(R.id.button5);

        sensorLabel = findViewById(R.id.textView);
        temperatureLabel = findViewById(R.id.textView1);
        statusLabel = findViewById(R.id.textView2);
        dataLabel = findViewById(R.id.textView3);
        numberLabel = findViewById(R.id.textView4);
        batteryLabel = findViewById(R.id.textView5);

        // Прежде всего получим сенсор менеджер
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Тут проверка наличия сенсора и чтение сохраненных настроек
        Log.d(LOG_TAG, "1 MainActivity.sensorExist = " + sensorExist);
        readSharedPreferences();
        Log.d(LOG_TAG, "2 MainActivity sensorExist = " + sensorExist);
        updateScreen();

        checkSensor();

        Log.d(LOG_TAG, "4 MainActivity sensorExist = " + sensorExist);

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
    }

    // Описание Runnable-объекта
    private Runnable timeUpdaterRunnable = new Runnable() {
        public void run() {
            // вычисляем время
            final long start = mTime;
            long millis = SystemClock.uptimeMillis() - start;
            int second = (int) (millis / 1000);
            int min = second / 60;
            second = second % 60;
            // выводим время
            batteryTemp();
            // batteryLabel.setText("" + min + ":" + String.format("%02d", second));
            // повторяем через каждые 200 миллисекунд
            mHandler.postDelayed(this, 3000);
        }
    };

    private void updateScreen() {
        //String period = String.valueOf((int)mainPeriodic/1000/60);
        String pAlarm = String.valueOf((int)ALARM_INTERVAL/1000/60);
        String pNormal = String.valueOf((int)NORMAL_INTERVAL/1000/60);
    // mainPeriodic сделал константой  на 15 минут
    //    dataLabel.setText("t°: " + WARNING_TEMP +  ", " + "Тест: " + period +  ", " + "Тревога: " + pAlarm + ", " + "Норма: " + pNormal);
        dataLabel.setText("Минимальная темп.: " + WARNING_TEMP + "°C" + "\nЧастота тревоги: " + pAlarm + " минут" + "\nЧастота норм смс: " + pNormal + " минут");
        numberLabel.setText("Номер для СМС: " + MY_NUMBER);
        invertButton(serviseON);
    }


    @Override
    public void onSensorChanged(SensorEvent sensor) {
    // В этом методе не нужно ничего лишнего! Если есть сенсор, то здесь большая цикличная работа
        mDEGREES = (int)sensor.values[0];
        temperatureLabel.setText(mDEGREES + "°C");

        // Поначал наделал тут проверок, ничего не нужно оказалось
/*        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH && mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {

            // ### Вот эта проверка больше нигде не встречается:
            if(sensorExist && mSensorTemperature.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
                // получаю, преобразую в int и сохраняю в degrees
                mDEGREES = (int)sensor.values[0];
                temperatureLabel.setText(mDEGREES + "°C");
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

        statusLabel.setText("Служба остановлена.");
        Log.d(LOG_TAG, "6 MainActivity sensorExist = " + sensorExist);
        if (sensorExist)temperatureLabel.setText(mDEGREES + "°C");
        Log.d(LOG_TAG, "MainActivity sensorExist = " + sensorExist);
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
        msg("Служба запущена успешно!");
        serviseON = true;
        invertButton(serviseON);
        saveSharedPreferences();
        statusLabel.setText("Служба выполняется");

        // Может быть надо раскомментировать?
        // mSensorManager.unregisterListener(this);
        Intent intent = new Intent(this, Control_activity.class);

        // Запретить оптимизировать батарею
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //    msg("Служба запускается для API > 22 ");
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
        //    msg("Служба запускается для API < 22");
            intent.putExtra("serviceIntentON", serviseON);
            intent.putExtra("schedulerPeriodic", mainPeriodic);
            startActivity(intent);
        }
    }

    private void saveSharedPreferences() {
        // String period = String.valueOf((int)mainPeriodic/1000/60);
        String pAlarm = String.valueOf((int)ALARM_INTERVAL/1000/60);
        String pNormal = String.valueOf((int)NORMAL_INTERVAL/1000/60);

        // dataLabel.setText("t°: " + WARNING_TEMP +  ", " + "Тест: " + period +  ", " + "Тревога: " + pAlarm + ", " + "Норма: " + pNormal);
        dataLabel.setText("Минимальная темп.: " + WARNING_TEMP + "°C" + "\nЧастота тревоги: " + pAlarm + " минут" + "\nЧастота норм смс: " + pNormal + " минут");

        savePref = getSharedPreferences("ru.microsave.tempmonitor.Prefs", MODE_PRIVATE);
        SharedPreferences.Editor ed = savePref.edit();

        ed.putString("NUMBER", MY_NUMBER);
        ed.putInt("WARNING", WARNING_TEMP);

        // ed.putLong("PERIOD_INTERVAL", mainPeriodic);
        ed.putLong("NORMAL_INTERVAL", NORMAL_INTERVAL);
        ed.putLong("ALARM_INTERVAL", ALARM_INTERVAL);

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
        MY_NUMBER = (savePref.getString("NUMBER", "+7123456789"));
        WARNING_TEMP = (savePref.getInt("WARNING", 15));

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
            mButton0.setEnabled(false);
            mButton1.setEnabled(true);
            mButton2.setEnabled(false);
            mButton3.setEnabled(false);
            mButton4.setEnabled(false);
            mButton5.setEnabled(false);
        }
        // Если НЕ было запусков или была остановка
        else {
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
                    Toast.makeText(getApplicationContext(),"Выход без изменений!",Toast.LENGTH_LONG).show();
                    return;
                }

                int minute = (Integer.parseInt(value));
               // mainPeriodic = Long.valueOf(minute * 60 * 1000);
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
        readSharedPreferences();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Частота тревоги в минутах");

        alert.setMessage("Тревожные СМС отправляются,\nесли температура ниже, чем: " + WARNING_TEMP + " °С" +
                "\n\nНастроено: " + ALARM_INTERVAL/60/1000 + " минут" +
                "\n\nВажно: Интервал меньше, чем 15 минут установить нельзя!");

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
                    Toast.makeText(getApplicationContext(),"Выход без изменений!",Toast.LENGTH_LONG).show();
                    return;
                }

                int minute= (Integer.parseInt(value));
                // Проверка значения
                if (minute < 15){
                    Toast.makeText(getApplicationContext(),"Нельзя устанавливать менее 15 минут!",Toast.LENGTH_LONG).show();
                    inputAlarma(null);
                }

                else {
                    ALARM_INTERVAL = Long.valueOf(minute * 60 * 1000);
                    Log.d(LOG_TAG, "--- ALARM_INTERVAL ---" + ALARM_INTERVAL/1000/60);
                    saveSharedPreferences();
                }
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
        readSharedPreferences();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Частота СМС в минутах");
        alert.setMessage("СМС только для информации,\nот температуры не зависят!" +
                "\n\n Настроено: " + NORMAL_INTERVAL/60/1000 + " минут" +
                "\n\nВажно: Интервал меньше, чем 15 минут установить нельзя!");

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
                    Toast.makeText(getApplicationContext(),"Выход без изменений!",Toast.LENGTH_LONG).show();
                    return;
                }

                int minute= (Integer.parseInt(value));
                // Проверка значения
                if (minute < 15){
                    Toast.makeText(getApplicationContext(),"Нельзя устанавливать менее 15 минут!",Toast.LENGTH_LONG).show();
                    inputNormal(null);
                }
                else {
                    NORMAL_INTERVAL = Long.valueOf(minute * 60 * 1000);
                    Log.d(LOG_TAG, "--- NORMAL_INTERVAL ---" + NORMAL_INTERVAL/1000/60);
                    saveSharedPreferences();
                }
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

        alert.setTitle("Номер для СМС");
        alert.setMessage("Код страны и \"+\" обязателен!\nпример: +7987654321");

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
                    Toast.makeText(getApplicationContext(),"Выход без изменений!",Toast.LENGTH_LONG).show();
                    return;
                }

                // TODO: 12.04.2022 Хорошо бы добавить проверку введенного номера на правильность
                MY_NUMBER = value;
                numberLabel.setText("Сохранен номер: " + value);
                saveSharedPreferences();
                msg("Сохранен номер: " + value);
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

        alert.setTitle("Минимальная температура");
        alert.setMessage("Это для тревожных СМС" + "\n\nВажно:\nНельзя установить 0 и ниже!");

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
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    public void messageBattery (View view) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            Log.d(LOG_TAG, "33 MainActivity sensorExist = " + sensorExist);
            if (!sensorExist) {
            alert.setTitle("Термометр не обнаружен!");
            alert.setMessage("Измерения производятся на аккумуляторе. Это дает погрешность при включенном экране." +
                    "\n\nОставьте телефон в покое примерно на один час." +
                    "\nДатчик начнет выдавать значения, близкие к реальной температуре воздуха.");
            }
            else {
                alert.setTitle("Термометр обнаружен!");
                alert.setMessage(
                        "Имеется встроенный датчик температуры окружающего воздуха" +
                        "\n\nВсе измерения производятся именно на нем"+
                        "\n\nПоказания батареи выводятся только для информации!");
            }
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    messageRead = true;
                    saveSharedPreferences();
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
        if (!messageRead) messageBattery (null);
        saveSharedPreferences();
    }




    public void batteryTemp ()
    {
        Intent intent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int  temp   = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)) /10;

        String message = String.valueOf(temp) + Character.toString ((char) 176) + "C";
        batteryLabel.setText(message);
    }
}