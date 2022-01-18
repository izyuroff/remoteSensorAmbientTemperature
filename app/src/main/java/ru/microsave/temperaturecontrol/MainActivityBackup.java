package ru.microsave.temperaturecontrol;

class MainActivityBackup {

    /*
    package ru.microsave.temperature;

Получение температуры окружающей среды и сигнализация о слишком низком уровне
Измерения проводить каждый час - пробуждение смартфона от сна

Есть регулярное ежедневное сообщение о работоспособности устройства - FIX
Также сигнализация о работоспособности устройства - уровень батареи например

Нужен ввод номера телефона пользователем - fix
и может быть можно менять текст сообщения
Нужен ввод периодичности сообщений

Нужна проверка на наличие датчика - и никаких действий если его нет - FIX

При запуске сразу отправляется первое нормальное сообщение - сделать проверку первого запуска

 */

    /*
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

    public class MainActivity extends AppCompatActivity implements SensorEventListener {

        // Для отправки СМС
        public static String MY_NUMBER;
        String messageText;

        // Для ввода периодичности сообщений и пороговой температуры
        static long NORMAL_INTERVAL_MILLIS = 1000 * 60 * 60 * 12;
        static long ALARM_INTERVAL_MILLIS = 1000 * 60 * 60;
        static int WARNING_TEMP;
        static final int LOWEST_TEMP = 15;
        static final int HIGHEST_TEMP = 35;

        long mLastUpdated;
        long mLastMessage;

        private Sensor mSensorTemperature;
        private SensorManager mSensorManager;
        private SharedPreferences savePref;
        private boolean sensorExist; // наличие сенсора температуры

    private boolean serviceON; // состояние службы дежурства, запущена или нет
    private boolean requireCharging = true; // требуется обязательная зарядка
    private boolean isPersisted = true; // Сохранять планировщик после рестарта устройства
    JobScheduler mJobScheduler;

        private EditText number; // номер для смс
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
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            mButton = findViewById(R.id.button);
            mButton0 = findViewById(R.id.button0);
            mButton1 = findViewById(R.id.button1);
            mButton2 = findViewById(R.id.button2);

            sensorLabel = (TextView) findViewById(R.id.textView0);
            temperatureLabel = (TextView) findViewById(R.id.textView1);
            statusLabel = (TextView) findViewById(R.id.textView2);
            logLabel = (TextView) findViewById(R.id.textView3);
            numberLabel = (TextView) findViewById(R.id.textView4);

            readSharedPreferences();
            logLabel.setText("Минимальная t°C = " + WARNING_TEMP);
            numberLabel.setText("Номер: " + MY_NUMBER);

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            //mSensorManager.unregisterListener(this);


            // Проверим наличчие сенсора
            if(mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
                mSensorTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
                sensorExist = true;
                sensorLabel.setText("Датчик t°C - ОК");
            } else {
                // Если нет датчика, скажем об этом
                sensorLabel.setText("Датчика t°C - НЕТ");
                temperatureLabel.setText("?°C");
                sensorExist = false;
            }

            mSensorManager.registerListener(this, mSensorTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onSensorChanged(SensorEvent sensor) {

            if(sensorExist && mSensorTemperature.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){

                // получаю, преобразую в int и сохраняю в degrees
                int degrees = (int)sensor.values[0];

                temperatureLabel.setText(degrees + "°C");
                sendAlarm(degrees);
                // realSMS (degrees);
            }
            else msg("У вас нет датчика температуры");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }


        // Метод для кнопочки ТЕСТ ОТПРАВКИ СМС
        public void sendTestAlarm (View view){
            messageText = "Тест кнопки: " + temperatureLabel.getText().toString();
            if (sensorExist) {
                msg("Кнопка ТЕСТ: СМС отправлено ");
                new SendSMS().execute(messageText);
            }
            else msg("Кнопка ТЕСТ: У вас нет датчика температуры!");
        }

        // Метод для ПОТОКОВОЙ ОТПРАВКИ СМС ПРИ ЛЮБОМ ИЗМЕНЕНИИ ТЕМПЕРАТУРЫ
        // Зачем он нужен непонятно:) просто тестировать отправку
        public void realSMS (int degrees){
            messageText = "В доме: " + degrees + "градусов";
            new SendSMS().execute(messageText);
        }

        // Метод для ПЕРИОДИЧЕСКОЙ И АВАРИЙНОЙ ОТПРАВКИ СМС ПРИ ИЗМЕНЕНИИ ТЕМПЕРАТУРЫ
        private void sendAlarm(int degrees) {

            if (MY_NUMBER == null)
                return;

            long currentTime = System.currentTimeMillis();
            // Normal SMS
            if ((currentTime - mLastUpdated) > NORMAL_INTERVAL_MILLIS) {
                messageText = "Нормальное сообщение: " + degrees + "°C";
                mLastUpdated = currentTime;
                new SendSMS().execute(messageText);
                msg(messageText);
            }

            // Alarm SMS
            if ((degrees < WARNING_TEMP) && ((currentTime - mLastMessage) > ALARM_INTERVAL_MILLIS)) {
                messageText = "Аварийная температура: " + degrees + "°C";
                mLastMessage = currentTime;
                new SendSMS().execute(messageText);
                msg(messageText);
            }
        }


        // Метод для кнопочки БОЕВОЕ ДЕЖУРСТВО
        public void control (View view){
            statusLabel.setText("Служба не запущена!!!");

            Intent intent = new Intent(this, Control_activity.class);
            startActivity(intent);

            // jobPlan();

            msg("Служба запускается");
        }

            public void jobPlan() {
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
            WARNING_TEMP = (savePref.getInt("WARNING", 15));
            sensorExist = (savePref.getBoolean("IFSENSOR", false));
        }
        private void saveSharedPreferences() {
            savePref = getSharedPreferences("ru.microsave.temperature.Prefs", MODE_PRIVATE);
            SharedPreferences.Editor ed = savePref.edit();

            ed.putString("NUMBER", MY_NUMBER);
            ed.putInt("WARNING", WARNING_TEMP);
            ed.putBoolean("IFSENSOR", sensorExist);
            ed.apply();
            ed.commit();
        }

        // ==========================================

        // fast way to call Toast
        private void msg(String s)
        {
            Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
        }
        @Override
        protected void onResume() {
            super.onResume();
        }

        @Override
        protected void onPause() {
            super.onPause();
            mSensorManager.unregisterListener(this);
        }
        @Override
        protected void onDestroy() {
            super.onDestroy();
            mSensorManager = null;
            mSensorTemperature = null;
        }
    }


     */
}
