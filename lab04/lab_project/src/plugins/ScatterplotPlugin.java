package plugins;

import core.DatabaseManager;
import core.IPlugin;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Плагин для визуализации данных в виде диаграммы рассеяния.
 * Позволяет пользователю выбирать колонки для осей X и Y через интерфейс.
 */
public class ScatterplotPlugin implements IPlugin {
    @Override
    public String getName() { return "Диаграмма рассеяния"; }

    @Override
    public String getDescription() { return "Визуализация корреляции между двумя выбранными переменными."; }

    @Override
    public JPanel execute(DatabaseManager dbManager) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Список всех доступных колонок из БД
        List<String> allColumns = dbManager.getColumnNames();
        String[] colArray = allColumns.toArray(new String[0]);

        // Панель управления (Выбор колонок)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Настройки осей"));

        JComboBox<String> xCombo = new JComboBox<>(colArray);
        JComboBox<String> yCombo = new JComboBox<>(colArray);
        
        // По умолчанию выбираем первую и вторую колонки, если они есть
        if (colArray.length > 1) {
            yCombo.setSelectedIndex(1);
        }

        JButton btnUpdate = new JButton("Обновить график");
        
        controlPanel.add(new JLabel("Ось X:"));
        controlPanel.add(xCombo);
        controlPanel.add(new JLabel("Ось Y:"));
        controlPanel.add(yCombo);
        controlPanel.add(btnUpdate);

        // Область для самого графика
        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setBackground(Color.WHITE);

        // Функция обновления данных и перерисовки
        Runnable updateChart = () -> {
            String selectedX = (String) xCombo.getSelectedItem();
            String selectedY = (String) yCombo.getSelectedItem();
            
            List<PointData> points = fetchData(dbManager, selectedX, selectedY);
            
            chartWrapper.removeAll();
            if (points.isEmpty()) {
                chartWrapper.add(new JLabel("Нет числовых данных в выбранных колонках", SwingConstants.CENTER));
            } else {
                chartWrapper.add(new ChartCanvas(points, selectedX, selectedY), BorderLayout.CENTER);
            }
            chartWrapper.revalidate();
            chartWrapper.repaint();
        };

        // Слушатель кнопки
        btnUpdate.addActionListener(e -> updateChart.run());

        mainPanel.add(chartWrapper, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // Первоначальный запуск отрисовки
        updateChart.run();

        return mainPanel;
    }

    /**
     * Извлечение данных из БД для конкретных выбранных колонок
     */
    private List<PointData> fetchData(DatabaseManager dbManager, String colX, String colY) {
        List<PointData> points = new ArrayList<>();
        try {
            // Запрос только нужных колонок
            String query = String.format("SELECT \"%s\", \"%s\" FROM dataset LIMIT 1000", colX, colY);
            ResultSet rs = dbManager.executeQuery(query);
            
            while (rs.next()) {
                try {
                    String valX = rs.getString(1);
                    String valY = rs.getString(2);
                    if (valX != null && valY != null) {
                        double x = Double.parseDouble(valX.replace(",", "."));
                        double y = Double.parseDouble(valY.replace(",", "."));
                        points.add(new PointData(x, y));
                    }
                } catch (NumberFormatException ignored) {
                    // Игнорируем строки, где нет чисел
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return points;
    }

    private static class PointData {
        double x, y;
        PointData(double x, double y) { this.x = x; this.y = y; }
    }

    /**
     * Внутренний класс для непосредственного рисования графики
     */
    private static class ChartCanvas extends JPanel {
        private final List<PointData> points;
        private final String xLabel, yLabel;

        ChartCanvas(List<PointData> points, String x, String y) {
            this.points = points;
            this.xLabel = x;
            this.yLabel = y;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padding = 60;
            int labelOffset = 30;
            int width = getWidth() - 2 * padding;
            int height = getHeight() - 2 * padding;

            if (points.isEmpty()) return;

            // Вычисляем границы данных
            double minX = points.stream().mapToDouble(p -> p.x).min().orElse(0);
            double maxX = points.stream().mapToDouble(p -> p.x).max().orElse(1);
            double minY = points.stream().mapToDouble(p -> p.y).min().orElse(0);
            double maxY = points.stream().mapToDouble(p -> p.y).max().orElse(1);

            // Если данные статичны, делаем небольшой отступ
            if (maxX == minX) { maxX += 1; minX -= 1; }
            if (maxY == minY) { maxY += 1; minY -= 1; }

            // Рисуем оси
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(padding, getHeight() - padding, padding, padding); // Y
            g2.drawLine(padding, getHeight() - padding, getWidth() - padding, getHeight() - padding); // X

            // Подписи осей
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(xLabel, getWidth() / 2, getHeight() - padding + labelOffset);
            
            // Поворот для вертикальной подписи Y
            Graphics2D g2y = (Graphics2D) g2.create();
            g2y.translate(padding - labelOffset, getHeight() / 2);
            g2y.rotate(-Math.PI / 2);
            g2y.drawString(yLabel, 0, 0);
            g2y.dispose();

            // Рисуем точки
            g2.setColor(new Color(50, 120, 240, 180));
            for (PointData p : points) {
                double xCoord = padding + ((p.x - minX) / (maxX - minX)) * width;
                double yCoord = (getHeight() - padding) - ((p.y - minY) / (maxY - minY)) * height;
                g2.fill(new Ellipse2D.Double(xCoord - 4, yCoord - 4, 8, 8));
            }
            
            // Рамка вокруг графика
            g2.setColor(Color.LIGHT_GRAY);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(padding, padding, width, height);
        }
    }
}