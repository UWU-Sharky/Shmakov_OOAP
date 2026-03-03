using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Diagnostics;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Xml.Schema;

namespace WindowsFormsApp1
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();

            queueBox.DataSource = DataStorage.GlobalQueues;
            queueBox.DisplayMember = "ID";

            // Подписываемся на смену выбранной очереди
            queueBox.SelectedIndexChanged += QueueBox_SelectedIndexChanged;

            // Инициализируем начальное состояние
            UpdateGridSource();
        }

        private void QueueBox_SelectedIndexChanged(object sender, EventArgs e)
        {
            UpdateGridSource();
        }

        private void UpdateGridSource()
        {
            if (queueBox.SelectedItem is Queue selectedQueue)
            {
                // Перепривязываем источник данных к списку задач выбранной очереди
                dataGridTask.DataSource = null;
                dataGridTask.DataSource = selectedQueue.CustomTasks;

            }
            else
            {
                dataGridTask.DataSource = null;
            }
        }
        private void edNewQueue_Click(object sender, EventArgs e)
        {
            DataStorage.GlobalQueues.Add(new Queue());
        }

        private void edDeleteQueue_Click(object sender, EventArgs e)
        {
            if (DataStorage.GlobalQueues.Count > 0)
            {
                Queue queue = (Queue)queueBox.SelectedItem;
                queue.Clear();
                DataStorage.GlobalQueues.RemoveAt(DataStorage.GlobalQueues.IndexOf(queue));
            }
        }

        private void edNewTask_Click(object sender, EventArgs e)
        {
            if (edLevels.SelectedIndex != -1 && queueBox.SelectedItem is Queue selectedQueue)
            {
                int tasks = (int)CountTasks.Value;
                string level = (string)edLevels.SelectedItem;
                for (int i = 0; i < tasks; i++)
                {
                    selectedQueue.Add(level);
                }
                dataGridTask.DataSource = null;
                dataGridTask.DataSource = selectedQueue.CustomTasks;
            }

            if (DataStorage.GlobalQueues.Count == 0 && edLevels.SelectedIndex != -1)
            {
                selectedQueue = new Queue();

                int tasks = (int)CountTasks.Value;
                string level = (string)edLevels.SelectedItem;
                for (int i = 0; i < tasks; i++)
                {
                    selectedQueue.Add(level);
                }

                DataStorage.GlobalQueues.Add(selectedQueue);

                dataGridTask.DataSource = null;
                dataGridTask.DataSource = selectedQueue.CustomTasks;
            }
        }

        private void btnDeleteTask_Click(object sender, EventArgs e)
        {
            if (dataGridTask.CurrentRow != null && dataGridTask.CurrentRow.DataBoundItem is CustomTask task)
            {
                if (queueBox.SelectedItem is Queue selectedQueue)
                {
                    selectedQueue.CustomTasks.Remove(task);
                }
            }
        }

        private void edStart_Click(object sender, EventArgs e)
        {
            ProgressForm progressForm = new ProgressForm((int)edWorkerCount.Value, DataStorage.GlobalQueues);
            progressForm.ShowDialog();
        }
    }

    public class DataStorage
    {
        public static BindingList<Queue> GlobalQueues = new BindingList<Queue>();
    }

    public class CustomTask
    {
        private int level = 0;
        public int Level
        {
            get
            {
                return this.level;
            }
            set
            {
                this.level = value;
            }
        }

        public static readonly Dictionary<string, int> levelDurations = new Dictionary<string, int>
        {
            {"Easy", 2},
            {"Medium", 3},
            {"Hard", 4}
        };


        public CustomTask()
        {
            this.level = levelDurations[levelDurations.Keys.ElementAt(new Random().Next(levelDurations.Count))];
        }

        public CustomTask(string level)
        {
            this.level = levelDurations[level];
        }

    }

    public class Queue
    {
        private readonly object _lock = new object();

        private static int _idCounter = 0;

        private static readonly object _queuesLock = new object();

        public BindingList<CustomTask> CustomTasks;

        private static Singleton bridge = Singleton.Instance;
        public int Id { get; private set; }
        public int Count
        {
            get
            {
                lock (_lock)
                {
                    return CustomTasks.Count;
                }
            }
        }

        public bool IsEmpty
        {
            get
            {
                lock (_lock)
                {
                    return CustomTasks.Count == 0;
                }
            }
        }

        public Queue()
        {
            lock (_queuesLock)
            {
                this.CustomTasks = new BindingList<CustomTask>();
                _idCounter++;
                this.Id = _idCounter;
            }

        }

        public Queue(int capacity)
        {
            lock (_queuesLock)
            {
                this.CustomTasks = new BindingList<CustomTask>();
                _idCounter++;
                this.Id = _idCounter;
                for (int i = 0; i < capacity; i++)
                {
                    this.CustomTasks.Add(new CustomTask());
                }
            }
        }

        public Queue(int capacity, string level)
        {
            lock (_queuesLock)
            {
                this.CustomTasks = new BindingList<CustomTask>();
                _idCounter++;
                this.Id = _idCounter;
                for (int i = 0; i < capacity; i++)
                {
                    this.CustomTasks.Add(new CustomTask(level));
                }
            }
        }

        public void Add(string level)
        {
            lock (_lock)
            {
                this.CustomTasks.Add(new CustomTask(level));
            }
        }

        public CustomTask Delete()
        {
            lock (_lock)
            {
                if (this.CustomTasks.Count == 0)
                {
                    throw new InvalidOperationException("Queue is empty");
                }

                CustomTask CustomTask = this.CustomTasks[0];
                this.CustomTasks.RemoveAt(0);
                return CustomTask;
            }
        }

        public void ApplyWorker(IProgress<int> progress)
        {
            Worker worker = bridge.FindWorker(this);
            worker.Working(this, progress);
        }

        public void Clear()
        {
            lock (_lock)
            {
                this.CustomTasks.Clear();
            }
        }


    }

    public sealed class Singleton
    {
        private static readonly object _lock = new object();
        // Lazy<T> гарантирует потокобезопасную ленивую инициализацию
        private static readonly Lazy<Singleton> _instance =
            new Lazy<Singleton>(() => new Singleton(), isThreadSafe: true);

        public static Singleton Instance
        {
            get
            {
                return _instance.Value;
            }
        }

        private Singleton() { }

        public Worker FindWorker(Queue queue)
        {
            while (true)
            {
                lock (_lock)
                {
                    foreach (Worker worker in Worker.Workers)
                    {
                        if (!worker.IsWorking)
                        {
                            worker.IsWorking = true;
                            return worker;
                        }
                    }
                }
                Thread.Sleep(100);
            }
        }
    }

    public class Worker
    {
        public int Id { get; set; }
        public bool IsWorking { get; set; } = false;
        public static List<Worker> Workers = new List<Worker>();

        public void Working(Queue queue, IProgress<int> progress)
        {
            int total = queue.Count;
            if (total == 0) { IsWorking = false; return; }

            while (queue.Count > 0)
            {
                CustomTask task = queue.Delete();
                Thread.Sleep(task.Level * 1000);

                int percent = (int)((1.0 - (double)queue.Count / total) * 100);
                progress?.Report(percent);
            }

            IsWorking = false;
        }
    }

    public partial class ProgressForm : Form
    {
        private List<ProgressBar> pbList = new List<ProgressBar>();
        private BindingList<Queue> _queues;

        public ProgressForm(int k, BindingList<Queue> GlobalQueues)
        {
            InitializeComponent();
            _queues = GlobalQueues;

            // Создаем воркеров, если их еще нет
            if (Worker.Workers.Count == 0)
            {
                for (int i = 0; i < k; i++)
                    Worker.Workers.Add(new Worker { Id = i + 1 });
            }

            // Динамически создаем контролы
            for (int i = 0; i < _queues.Count; i++)
            {
                Label lbl = new Label { Text = $"Queue #{_queues[i].Id}", Top = 20 + (i * 35), Left = 10, Width = 80 };
                ProgressBar pb = new ProgressBar { Top = 20 + (i * 35), Left = 100, Width = 200 };
                pbList.Add(pb);
                this.Controls.Add(lbl);
                this.Controls.Add(pb);
            }

            // Запуск асинхронно ПОСЛЕ отрисовки формы
            this.Shown += async (s, e) => await StartProcessing();
        }

        private async Task StartProcessing()
        {
            List<Task> tasks = new List<Task>();

            for (int i = 0; i < _queues.Count; i++)
            {
                var queue = _queues[i];
                var pb = pbList[i];

                // Progress<T> сам делает Invoke, обращаясь к UI-потоку
                var reporter = new Progress<int>(val => pb.Value = val);

                // Запускаем обработку каждой очереди в отдельном потоке
                tasks.Add(Task.Run(() => queue.ApplyWorker(reporter)));
            }

            await Task.WhenAll(tasks);
            MessageBox.Show($"{Worker.Workers.Count}");
        }
    }
}