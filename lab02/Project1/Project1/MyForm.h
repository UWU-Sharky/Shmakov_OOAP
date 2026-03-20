#pragma once

#include <iostream>
#include <vcclr.h>
#include <format>
#include <string>

namespace Project1 {

    using namespace System;
    using namespace System::Windows::Forms;
    using namespace System::Drawing;
    using namespace System::Diagnostics;


    // Управляемый аналог UnsharedConcreteFlyweight
    public ref struct UnsharedConcreteFlyweight {
    public:
        int X;
        int Y;
        int SpeedX;
        int SpeedY;
        String^ Flyweight;

        UnsharedConcreteFlyweight(int x, int y, int speedX, int speedY, String^ flyweight)
            : X(x), Y(y), SpeedX(speedX), SpeedY(speedY), Flyweight(flyweight){
        }
    };

    // Управляемый интерфейс Flyweight
    public interface class IFlyweight {
        void Operation(UnsharedConcreteFlyweight^ state, Graphics^ g);
    };

    // Управляемый ConcreteFlyweight
    public ref class ConcreteFlyweight : IFlyweight {
    private:
        String^ shape;
        String^ color;
        float weight;

    public:
        ConcreteFlyweight(String^ shape, String^ color, float weight)
            : shape(shape), color(color), weight(weight) {
        }

        virtual void Operation(UnsharedConcreteFlyweight^ state, Graphics^ g) override {

            int x = state->X;
            int y = state->Y;
            int size = (int)weight;

            if (shape == "Circle") {
                Brush^ brush = gcnew SolidBrush(Color::Blue);
                g->FillEllipse(brush, x, y, size, size);
            }
            else if (shape == "Square") {
                Brush^ brush = gcnew SolidBrush(Color::Red);
                g->FillRectangle(brush, x, y, size, size);
            }
            else if (shape == "Triangle") {
                Brush^ brush = gcnew SolidBrush(Color::Green);
                array<Point>^ points = {
                    Point(x + size / 2, y),
                    Point(x, y + size),
                    Point(x + size, y + size)
                };
                g->FillPolygon(brush, points);
            }
        }
    };

    // Управляемый FlyweightFactory
    public ref class FlyweightFactory {
    private:
        System::Collections::Generic::Dictionary<String^, IFlyweight^>^ flyweights;

    public:
        FlyweightFactory() {
            flyweights = gcnew System::Collections::Generic::Dictionary<String^, IFlyweight^>();
        }

        IFlyweight^ GetFlyweight(String^ key) {
            if (flyweights->ContainsKey(key)) {
                return flyweights[key];
            }
            else {
                array<String^>^ parts = key->Split('_');
                String^ shape = parts[0];
                String^ color = parts[1];
                float weight = float::Parse(parts[2]);

                IFlyweight^ newFlyweight = gcnew ConcreteFlyweight(shape, color, weight);
                flyweights->Add(key, newFlyweight);
                return newFlyweight;
            }
        }
    };

    // Основная форма
    public ref class MyForm : public Form {
    private:
        FlyweightFactory^ factory;
        System::Collections::Generic::List<UnsharedConcreteFlyweight^>^ objects;
        System::Windows::Forms::Timer^ timer;
        String^ currentShape;

    public:
        MyForm(void) {
            InitializeComponent();
            this->DoubleBuffered = true;
            factory = gcnew FlyweightFactory();
            objects = gcnew System::Collections::Generic::List<UnsharedConcreteFlyweight^>();
            currentShape ="Circle_Blue";

            timer = gcnew System::Windows::Forms::Timer();
            timer->Interval = 16; 
            timer->Tick += gcnew System::EventHandler(this, &MyForm::timer_Tick);
            timer->Start();
        }

    protected:
        ~MyForm() {
            if (components) delete components;
        }

    private:
        System::Windows::Forms::PictureBox^ pictureBox1;
        System::ComponentModel::Container^ components;

        void InitializeComponent(void) {
            this->pictureBox1 = gcnew PictureBox();
            this->pictureBox1->Dock = DockStyle::Fill;
            this->pictureBox1->BackColor = Color::White;
            this->pictureBox1->Paint += gcnew PaintEventHandler(this, &MyForm::pictureBox1_Paint);
            this->pictureBox1->MouseDown += gcnew MouseEventHandler(this, &MyForm::pictureBox1_MouseDown);
  

            this->KeyDown += gcnew System::Windows::Forms::KeyEventHandler(this, &MyForm::MyForm_KeyDown);

            this->ClientSize = System::Drawing::Size(584, 561);
            this->Controls->Add(this->pictureBox1);
            this->Text = L"Flyweight Draw";
        }

        System::Void pictureBox1_Paint(System::Object^ sender, PaintEventArgs^ e) 
        {
            if (e->Graphics == nullptr) {
                return; 
            }

            Graphics^ g = e->Graphics;
            for each(UnsharedConcreteFlyweight ^ obj in objects) {
                IFlyweight^ fw = factory->GetFlyweight(obj->Flyweight);
                fw->Operation(obj, g);
            }
            long long bytesUsed = Process::GetCurrentProcess()->WorkingSet64;
            float memoryMB = bytesUsed / (1024.0f * 1024.0f);

            String^ memoryText = String::Format("Memory: {0:F2} MB", memoryMB);

            System::Drawing::Font^ drawFont = gcnew System::Drawing::Font("Arial", 10, FontStyle::Bold);
            System::Drawing::SolidBrush^ drawBrush = gcnew System::Drawing::SolidBrush(Color::Red); 

            g->DrawString(memoryText, drawFont, drawBrush, 10.0f, 10.0f);

            delete drawFont;
            delete drawBrush;
        }


        System::Void pictureBox1_MouseDown(System::Object^ sender, MouseEventArgs^ e) {
            Random^ random = gcnew Random();
            int speedX;
            int speedY;
            if (currentShape == "Circle_Blue")
            {
                speedX = random->Next(-5, 6);
                speedY = random->Next(-5, 6);
            }
            else if (currentShape == "Square_Red")
            {
                speedX = random->Next(-3, 8);
                speedY = random->Next(-3, 8);
            }
            else if (currentShape == "Triangle_Green")
            {
                speedX = random->Next(-8, 3);
                speedY = random->Next(-8, 3);
            }
            String^ result = currentShape + "_50";
            IFlyweight^ fw = factory->GetFlyweight(result);
            UnsharedConcreteFlyweight^ obj = gcnew UnsharedConcreteFlyweight(e->X, e->Y, speedX, speedY, result);

            objects->Add(obj);

        }


        System::Void MyForm_KeyDown(System::Object^ sender, System::Windows::Forms::KeyEventArgs^ e) {

            if (e->KeyCode == Keys::D1)
            {
                currentShape = "Circle_Blue";
            }
            else if (e->KeyCode == Keys::D2)
            {
                currentShape = "Square_Red";
            }
            else if (e->KeyCode == Keys::D3)
            {
                currentShape = "Triangle_Green";
            }
            
        }


        System::Void timer_Tick(System::Object^ sender, System::EventArgs^ e) {
            // Обновляем координаты всех объектов
            for each(UnsharedConcreteFlyweight ^ obj in objects) {
                obj->X += obj->SpeedX; 
                obj->Y += obj->SpeedY; 

                // Проверяем столкновения с другими объектами
                for each(UnsharedConcreteFlyweight ^ other in objects) {
                    if (obj != other) {

                        // Расстояние между объектами
                        float distance = System::Math::Sqrt(
                            System::Math::Pow(obj->X - other->X, 2) +
                            System::Math::Pow(obj->Y - other->Y, 2)
                        );

                        // Если расстояние меньше некоторого порога, меняем направление
                        if (distance < 30) {
                            obj->SpeedX *= -1; 
                            obj->SpeedY *= -1; 
                            other->SpeedX *= -1; 
                            other->SpeedY *= -1; 
                        }
                    }
                }

                // Если объект вышел за границы, возвращаем его
                if (obj->X > pictureBox1->Width || obj->X < 0) {
                    obj->SpeedX *= -1; 
                }
                if (obj->Y > pictureBox1->Height || obj->Y < 0) {
                    obj->SpeedY *= -1; 
                }
            }
            pictureBox1->Invalidate(); // Перерисовать
        }
    };
}
