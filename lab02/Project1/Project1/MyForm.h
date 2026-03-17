#pragma once

#include <vcclr.h>

namespace Project1 {

    using namespace System;
    using namespace System::Windows::Forms;
    using namespace System::Drawing;

    // Управляемый аналог UnsharedConcreteFlyweight
    public ref struct UnsharedConcreteFlyweight {
    public:
        int X;
        int Y;
        int SpeedX;
        int SpeedY;

        UnsharedConcreteFlyweight(int x, int y, int speedX, int speedY)
            : X(x), Y(y), SpeedX(speedX), SpeedY(speedY) {
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
            Brush^ brush = gcnew SolidBrush(Color::Blue);

            int x = state->X;
            int y = state->Y;
            int size = (int)weight;

            if (shape == "Circle") {
                g->FillEllipse(brush, x, y, size, size);
            }
            else if (shape == "Square") {
                g->FillRectangle(brush, x, y, size, size);
            }
            else if (shape == "Triangle") {
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

    public:
        MyForm(void) {
            InitializeComponent();
            this->DoubleBuffered = true;
            factory = gcnew FlyweightFactory();
            objects = gcnew System::Collections::Generic::List<UnsharedConcreteFlyweight^>();

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

            this->ClientSize = System::Drawing::Size(584, 561);
            this->Controls->Add(this->pictureBox1);
            this->Text = L"Flyweight Draw";
        }

        System::Void pictureBox1_Paint(System::Object^ sender, PaintEventArgs^ e) {
            for each(UnsharedConcreteFlyweight ^ obj in objects) {
                IFlyweight^ fw = factory->GetFlyweight("Circle_Blue_50");
                fw->Operation(obj, e->Graphics);
            }
        }

        System::Void pictureBox1_MouseDown(System::Object^ sender, MouseEventArgs^ e) {
            // Генерация случайных скоростей по X и Y
            Random^ random = gcnew Random();
            int speedX = random->Next(-5, 6);
            int speedY = random->Next(-5, 6);

            // Добавляем объект с случайной скоростью
            objects->Add(gcnew UnsharedConcreteFlyweight(e->X, e->Y, speedX, speedY));
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
