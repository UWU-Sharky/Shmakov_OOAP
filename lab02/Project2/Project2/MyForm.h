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

    // Класс для хранения состояния объекта
    public ref struct ShapeObject {
    public:
        int X;
        int Y;
        int SpeedX;
        int SpeedY;
        String^ ShapeType;
        String^ Color;
        float Weight;

        ShapeObject(int x, int y, int baseSpeedX, int baseSpeedY, String^ shapeType, String^ color, float weight)
            : X(x), Y(y), ShapeType(shapeType), Color(color), Weight(weight) {
            float weightFactor = 1.0f + (weight / 50.0f); // Коэффициент зависимости от веса
            SpeedX = static_cast<int>(baseSpeedX / weightFactor);
            SpeedY = static_cast<int>(baseSpeedY / weightFactor);
        }
    };

    // Основная форма
    public ref class MyForm : public Form {
    private:
        System::Collections::Generic::List<ShapeObject^>^ objects;
        System::Windows::Forms::Timer^ timer;
        String^ currentShape;

    public:
        MyForm(void) {
            InitializeComponent();
            this->DoubleBuffered = true;
            objects = gcnew System::Collections::Generic::List<ShapeObject^>();
            currentShape = "Circle_Blue";

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
            this->Text = L"Direct Draw with Weight-Based Speed";
        }

        System::Void pictureBox1_Paint(System::Object^ sender, PaintEventArgs^ e) {
            if (e->Graphics == nullptr) {
                return;
            }

            Graphics^ g = e->Graphics;
            for each (ShapeObject ^ obj in objects) {
                int x = obj->X;
                int y = obj->Y;
                int size = (int)obj->Weight;

                if (obj->ShapeType == "Circle") {
                    Brush^ brush = gcnew SolidBrush(Color::Blue);
                    g->FillEllipse(brush, x, y, size, size);
                    delete brush;
                }
                else if (obj->ShapeType == "Square") {
                    Brush^ brush = gcnew SolidBrush(Color::Red);
                    g->FillRectangle(brush, x, y, size, size);
                    delete brush;
                }
                else if (obj->ShapeType == "Triangle") {
                    Brush^ brush = gcnew SolidBrush(Color::Green);
                    array<Point>^ points = {
                        Point(x + size / 2, y),
                        Point(x, y + size),
                        Point(x + size, y + size)
                    };
                    g->FillPolygon(brush, points);
                    delete brush;
                }
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
            int baseSpeedX;
            int baseSpeedY;
            float weight;

            array<String^>^ shapeParts = currentShape->Split('_');
            String^ shapeType = shapeParts[0];
            String^ color = shapeParts[1];

            // Случайный вес для каждой фигуры
            weight = 10.0f + static_cast<float>(random->Next(0, 90));

            if (currentShape == "Circle_Blue") {
                baseSpeedX = random->Next(-10, 11);
                baseSpeedY = random->Next(-10, 11);
            }
            else if (currentShape == "Square_Red") {
                baseSpeedX = random->Next(-8, 9);
                baseSpeedY = random->Next(-8, 9);
            }
            else if (currentShape == "Triangle_Green") {
                baseSpeedX = random->Next(-12, 13);
                baseSpeedY = random->Next(-12, 13);
            }

            ShapeObject^ obj = gcnew ShapeObject(e->X, e->Y, baseSpeedX, baseSpeedY, shapeType, color, weight);
            objects->Add(obj);
        }

        System::Void MyForm_KeyDown(System::Object^ sender, System::Windows::Forms::KeyEventArgs^ e) {
            if (e->KeyCode == Keys::D1) {
                currentShape = "Circle_Blue";
            }
            else if (e->KeyCode == Keys::D2) {
                currentShape = "Square_Red";
            }
            else if (e->KeyCode == Keys::D3) {
                currentShape = "Triangle_Green";
            }
        }

        System::Void timer_Tick(System::Object^ sender, System::EventArgs^ e) {
            for each (ShapeObject ^ obj in objects) {
                obj->X += obj->SpeedX;
                obj->Y += obj->SpeedY;

                for each (ShapeObject ^ other in objects) {
                    if (obj != other) {
                        float distance = System::Math::Sqrt(
                            System::Math::Pow(obj->X - other->X, 2) +
                            System::Math::Pow(obj->Y - other->Y, 2)
                        );

                        if (distance < 30) {
                            obj->SpeedX *= -1;
                            obj->SpeedY *= -1;
                            other->SpeedX *= -1;
                            other->SpeedY *= -1;
                        }
                    }
                }

                if (obj->X > pictureBox1->Width || obj->X < 0) {
                    obj->SpeedX *= -1;
                }
                if (obj->Y > pictureBox1->Height || obj->Y < 0) {
                    obj->SpeedY *= -1;
                }
            }
            pictureBox1->Invalidate();
        }
    };
}
