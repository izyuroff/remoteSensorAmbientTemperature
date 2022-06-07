# remoteSensorAmbientTemperature v.1.1 Branches 1.1
 
 * <ul>
 *     <li><p>When running as a pre-O service, the act of enqueueing work will generally start
 *     the service immediately, regardless of whether the device is dozing or in other
 *     conditions.  When running as a Job, it will be subject to standard JobScheduler
 *     policies for a Job with a {@link android.app.job.JobInfo.Builder#setOverrideDeadline(long)}
 *     of 0: the job will not run while the device is dozing, it may get delayed more than
 *     a service if the device is under strong memory pressure with lots of demand to run
 *     jobs.</p></li>
 *     <li><p>When running as a pre-O service, the normal service execution semantics apply:
 *     the service can run indefinitely, though the longer it runs the more likely the system
 *     will be to outright kill its process, and under memory pressure one should expect
 *     the process to be killed even of recently started services.  When running as a Job,
 *     the typical {@link android.app.job.JobService} execution time limit will apply, after
 *     which the job will be stopped (cleanly, not by killing the process) and rescheduled
 *     to continue its execution later.  Job are generally not killed when the system is
 *     under memory pressure, since the number of concurrent jobs is adjusted based on the
 *     memory state of the device.</p></li>
 * </ul>
 
Получение температуры окружающей среды и сигнализация о слишком низком уровне отправкой СМС на заданный номер

---= Уже реализовано =---

Проверка на наличие датчика - и никаких действий если его нет

Ввод номера телефона пользователем

Ввод уровня низкой температуры

Ввод интервалов для настройки периодичности сообщений

Пробуждение смартфона от сна, реализовано настройкой класса JobScheduler

Аварийное сообщение при низком уровне температуры, отправка СМС каждый час

Сообщение о работоспособности устройства - отправка СМС периодически при любой температуре

---= Добавить в будущем =---

Также сигнализация о работоспособности устройства - уровень батареи например

возможность менять текст сообщения

добавить ввод данных для контроля высокой температуры

переработать интерфейс
