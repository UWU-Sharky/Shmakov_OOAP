using System;
using System.Diagnostics;
using System.Threading;

namespace project
{
    class Task
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


        public Task()
        {
            this.level = levelDurations[levelDurations.Keys.ElementAt(new Random().Next(levelDurations.Count))];
        }

        public Task(string level)
        {
            this.level = levelDurations[level];
        }

    }

    class Queue
    {
        public List<Task> tasks;

        public Singleton bridge;
        public int Count
        {
            get
            {
                return tasks.Count;
            }
        }

        public bool IsEmpty
        {
            get { return tasks.Count == 0; }
        }

        public Queue()
        {
            this.tasks = new List<Task>();

        }

        public Queue(int capacity)
        {
            this.tasks = new List<Task>(capacity);
            for (int i = 0; i < capacity; i++)
            {
                this.tasks.Add(new Task());
            }
        }

        public Queue(int capacity, string level)
        {
            this.tasks = new List<Task>(capacity);
            for (int i = 0; i < capacity; i++)
            {
                this.tasks.Add(new Task(level));
            }
        }

        public void Add(Task task)
        {
            this.tasks.Add(task);
        }

        public Task Delete()
        {
            if (this.tasks.Count == 0)
            {
                throw new InvalidOperationException("Queue is empty");
            }

            Task task = this.tasks[0];
            this.tasks.RemoveAt(0);
            return task;
        }

        public void ApplyWorker(Worker worker)
        {
            this.bridge = Singleton.getInstance(worker);
        }

        public void Clear()
        {
            this.tasks.Clear();
        }


    }

    class Singleton
    {
        private static Singleton instance;
        public Worker worker { get; private set; }
        protected Singleton(Worker worker)
        {
            this.worker = worker;
        }

        public static Singleton getInstance(Worker worker)
        {
            if (instance == null)
            {
                instance = new Singleton(worker);
            }
            return instance;
        }
    }

    class Worker
    {
        private static int _idCounter = 0;
        public int Id { get; private set; }

        public bool IsWorking = false;
        public Worker()
        {
            _idCounter++;
            this.Id = _idCounter; // Как будто бы можно что-то придумать про многопоточность
        }
        public void Working(Queue queue)
        {
            int c = 1;
            this.IsWorking = true;
            Console.WriteLine($"\nОбработчик_{this.Id} Загрузка:");
            while (queue.Count > 0)
            {
                Task task = queue.Delete();

                Console.WriteLine($"\n\t Задача_{c} в процессе: ");

                for (int i = 0; i <= task.Level; i++)
                {
                    Client.DrawProgress(i, task.Level);
                    Thread.Sleep(1000);
                }
                c++;
            }
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
            Worker worker = new();
            Queue queue = new(10);
            queue.ApplyWorker(worker);
            queue.bridge.worker.Working(queue);
        }
    }
}
