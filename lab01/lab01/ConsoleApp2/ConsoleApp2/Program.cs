using System;
using System.Diagnostics;
using System.Threading;

namespace project
{
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
            {"Easy", 3 },
            {"Meadle", 4},
            {"Hard", 5}
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

        private static readonly object _queuesLock = new object();

        public List<CustomTask> CustomTasks;

        private static Singleton bridge = Singleton.Instance;

        public static List<Queue> _queues { get; } = new List<Queue>();


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
                this.CustomTasks = new List<CustomTask>();
                _queues.Add(this);
            }

        }

        public Queue(int capacity)
        {
            lock (_queuesLock)
            {
                this.CustomTasks = new List<CustomTask>(capacity);
                for (int i = 0; i < capacity; i++)
                {
                    this.CustomTasks.Add(new CustomTask());
                    _queues.Add(this);
                }
            }
        }

        public Queue(int capacity, string level)
        {
            lock (_queuesLock)
            {
                this.CustomTasks = new List<CustomTask>(capacity);
                for (int i = 0; i < capacity; i++)
                {
                    this.CustomTasks.Add(new CustomTask(level));
                    _queues.Add(this);
                }
            }
        }

        public void Add(CustomTask CustomTask)
        {
            lock (_lock)
            {
                this.CustomTasks.Add(CustomTask);
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

        public void ApplyWorker()
        {
            Worker worker = bridge.FindWorker(this);
            worker.Working(this);
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
        private static int _idCounter = 0;

        public static List<Worker> Workers { get; } = new List<Worker>();

        public int Id { get; private set; }

        public bool IsWorking = false;

        public Worker()
        {
            _idCounter++;
            this.Id = _idCounter;
            Workers.Add(this);
        }

        public void Working(Queue queue)
        {
            int c = 1;
            Console.WriteLine($"\nОбработчик_{this.Id} Загрузка:");
            while (queue.Count > 0)
            {
                CustomTask CustomTask = queue.Delete();

                Console.WriteLine($"\n\t Задача_{c} в процессе: ");

                for (int i = 0; i <= CustomTask.Level; i++)
                {
                    Client.DrawProgress(i, CustomTask.Level);
                    Thread.Sleep(1000);
                }
                c++;
            }
            this.IsWorking = false;
        }
    }

    class Client
    {
        public static void DrawProgress(int progress, int total)
        {
            int percent = (int)(((double)progress / total) * 100);

            int filled = (int)(((double)progress / total) * 30);

            string bar = new string('#', filled) + new string('-', 30 - filled);

            Console.Write($"\r[{bar}] {percent}%");
        }
    }


    class Program
    {
        static void Main()
        {
            Console.WriteLine("Введите кол-во очередей: ");
            int N = Convert.ToInt32(Console.ReadLine());
            Console.WriteLine("Введите кол-во worker-ов: ");
            int k = Convert.ToInt32(Console.ReadLine());

            for(int i = 0; i < N; i++)
            {
                Worker myWorker = new();
            }

            for (int i = 0; i < k; i++)
            {
                Queue myQueue = new(10);
            }

            // Запускаем обработку очередей в потоках и сохраняем задачи
            List<System.Threading.Tasks.Task> tasks = new List<System.Threading.Tasks.Task>();
            foreach (Queue queue in Queue._queues)
            {
                var task = System.Threading.Tasks.Task.Run(() =>
                {
                    queue.ApplyWorker();
                });
                tasks.Add(task);
            }

            // Ожидаем завершения всех задач
            System.Threading.Tasks.Task.WhenAll(tasks).Wait();

            Console.WriteLine("Завершено");
        }
    }
}
