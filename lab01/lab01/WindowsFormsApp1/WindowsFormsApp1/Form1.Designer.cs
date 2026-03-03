namespace WindowsFormsApp1
{
    partial class Form1
    {
        /// <summary>
        /// Обязательная переменная конструктора.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Освободить все используемые ресурсы.
        /// </summary>
        /// <param name="disposing">истинно, если управляемый ресурс должен быть удален; иначе ложно.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Код, автоматически созданный конструктором форм Windows

        /// <summary>
        /// Требуемый метод для поддержки конструктора — не изменяйте 
        /// содержимое этого метода с помощью редактора кода.
        /// </summary>
        private void InitializeComponent()
        {
            this.queueBox = new System.Windows.Forms.ComboBox();
            this.dataGridTask = new System.Windows.Forms.DataGridView();
            this.edLevels = new System.Windows.Forms.ComboBox();
            this.edNewTask = new System.Windows.Forms.Button();
            this.CountTasks = new System.Windows.Forms.NumericUpDown();
            this.edStart = new System.Windows.Forms.Button();
            this.edNewQueue = new System.Windows.Forms.Button();
            this.label1 = new System.Windows.Forms.Label();
            this.edDeleteQueue = new System.Windows.Forms.Button();
            this.btnDeleteTask = new System.Windows.Forms.Button();
            this.label2 = new System.Windows.Forms.Label();
            this.edWorkerCount = new System.Windows.Forms.NumericUpDown();
            ((System.ComponentModel.ISupportInitialize)(this.dataGridTask)).BeginInit();
            ((System.ComponentModel.ISupportInitialize)(this.CountTasks)).BeginInit();
            ((System.ComponentModel.ISupportInitialize)(this.edWorkerCount)).BeginInit();
            this.SuspendLayout();
            // 
            // queueBox
            // 
            this.queueBox.FormattingEnabled = true;
            this.queueBox.Location = new System.Drawing.Point(150, 30);
            this.queueBox.Name = "queueBox";
            this.queueBox.Size = new System.Drawing.Size(120, 21);
            this.queueBox.TabIndex = 0;
            // 
            // dataGridTask
            // 
            this.dataGridTask.AutoSizeColumnsMode = System.Windows.Forms.DataGridViewAutoSizeColumnsMode.Fill;
            this.dataGridTask.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            this.dataGridTask.Location = new System.Drawing.Point(30, 57);
            this.dataGridTask.Name = "dataGridTask";
            this.dataGridTask.Size = new System.Drawing.Size(240, 280);
            this.dataGridTask.TabIndex = 1;
            // 
            // edLevels
            // 
            this.edLevels.FormattingEnabled = true;
            this.edLevels.Items.AddRange(new object[] {
            "Easy",
            "Medium",
            "Hard",
            "Random"});
            this.edLevels.Location = new System.Drawing.Point(314, 84);
            this.edLevels.Name = "edLevels";
            this.edLevels.Size = new System.Drawing.Size(121, 21);
            this.edLevels.TabIndex = 2;
            // 
            // edNewTask
            // 
            this.edNewTask.BackColor = System.Drawing.Color.Moccasin;
            this.edNewTask.Location = new System.Drawing.Point(314, 111);
            this.edNewTask.Name = "edNewTask";
            this.edNewTask.Size = new System.Drawing.Size(121, 23);
            this.edNewTask.TabIndex = 3;
            this.edNewTask.Text = "Добавить задачу";
            this.edNewTask.UseVisualStyleBackColor = false;
            this.edNewTask.Click += new System.EventHandler(this.edNewTask_Click);
            // 
            // CountTasks
            // 
            this.CountTasks.Location = new System.Drawing.Point(441, 84);
            this.CountTasks.Minimum = new decimal(new int[] {
            1,
            0,
            0,
            0});
            this.CountTasks.Name = "CountTasks";
            this.CountTasks.Size = new System.Drawing.Size(120, 20);
            this.CountTasks.TabIndex = 4;
            this.CountTasks.Value = new decimal(new int[] {
            1,
            0,
            0,
            0});
            // 
            // edStart
            // 
            this.edStart.BackColor = System.Drawing.Color.PaleGreen;
            this.edStart.Location = new System.Drawing.Point(314, 140);
            this.edStart.Name = "edStart";
            this.edStart.Size = new System.Drawing.Size(247, 23);
            this.edStart.TabIndex = 5;
            this.edStart.Text = "Запуск";
            this.edStart.UseVisualStyleBackColor = false;
            this.edStart.Click += new System.EventHandler(this.edStart_Click);
            // 
            // edNewQueue
            // 
            this.edNewQueue.BackColor = System.Drawing.Color.LightBlue;
            this.edNewQueue.Location = new System.Drawing.Point(314, 55);
            this.edNewQueue.Name = "edNewQueue";
            this.edNewQueue.Size = new System.Drawing.Size(121, 23);
            this.edNewQueue.TabIndex = 6;
            this.edNewQueue.Text = "Новая очередь";
            this.edNewQueue.UseVisualStyleBackColor = false;
            this.edNewQueue.Click += new System.EventHandler(this.edNewQueue_Click);
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(80, 33);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(64, 13);
            this.label1.TabIndex = 8;
            this.label1.Text = "Очередь №";
            // 
            // edDeleteQueue
            // 
            this.edDeleteQueue.BackColor = System.Drawing.Color.Salmon;
            this.edDeleteQueue.Location = new System.Drawing.Point(441, 55);
            this.edDeleteQueue.Name = "edDeleteQueue";
            this.edDeleteQueue.Size = new System.Drawing.Size(120, 23);
            this.edDeleteQueue.TabIndex = 7;
            this.edDeleteQueue.Text = "Удалить очередь";
            this.edDeleteQueue.UseVisualStyleBackColor = false;
            this.edDeleteQueue.Click += new System.EventHandler(this.edDeleteQueue_Click);
            // 
            // btnDeleteTask
            // 
            this.btnDeleteTask.BackColor = System.Drawing.Color.Violet;
            this.btnDeleteTask.Location = new System.Drawing.Point(441, 111);
            this.btnDeleteTask.Name = "btnDeleteTask";
            this.btnDeleteTask.Size = new System.Drawing.Size(120, 23);
            this.btnDeleteTask.TabIndex = 9;
            this.btnDeleteTask.Text = "Удалить задачу";
            this.btnDeleteTask.UseVisualStyleBackColor = false;
            this.btnDeleteTask.Click += new System.EventHandler(this.btnDeleteTask_Click);
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Location = new System.Drawing.Point(311, 33);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(125, 13);
            this.label2.TabIndex = 11;
            this.label2.Text = "Количество Worker-ов: ";
            // 
            // edWorkerCount
            // 
            this.edWorkerCount.Location = new System.Drawing.Point(441, 31);
            this.edWorkerCount.Minimum = new decimal(new int[] {
            1,
            0,
            0,
            0});
            this.edWorkerCount.Name = "edWorkerCount";
            this.edWorkerCount.Size = new System.Drawing.Size(120, 20);
            this.edWorkerCount.TabIndex = 12;
            this.edWorkerCount.Value = new decimal(new int[] {
            1,
            0,
            0,
            0});
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(587, 461);
            this.Controls.Add(this.edWorkerCount);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.btnDeleteTask);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.edDeleteQueue);
            this.Controls.Add(this.edNewQueue);
            this.Controls.Add(this.edStart);
            this.Controls.Add(this.CountTasks);
            this.Controls.Add(this.edNewTask);
            this.Controls.Add(this.edLevels);
            this.Controls.Add(this.dataGridTask);
            this.Controls.Add(this.queueBox);
            this.Name = "Form1";
            this.Text = "OOAP#1";
            ((System.ComponentModel.ISupportInitialize)(this.dataGridTask)).EndInit();
            ((System.ComponentModel.ISupportInitialize)(this.CountTasks)).EndInit();
            ((System.ComponentModel.ISupportInitialize)(this.edWorkerCount)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.ComboBox queueBox;
        private System.Windows.Forms.DataGridView dataGridTask;
        private System.Windows.Forms.ComboBox edLevels;
        private System.Windows.Forms.Button edNewTask;
        private System.Windows.Forms.NumericUpDown CountTasks;
        private System.Windows.Forms.Button edStart;
        private System.Windows.Forms.Button edNewQueue;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.Button edDeleteQueue;
        private System.Windows.Forms.Button btnDeleteTask;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.NumericUpDown edWorkerCount;
    }
}

