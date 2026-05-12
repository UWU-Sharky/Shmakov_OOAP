package plugins;

import core.DatabaseManager;
import core.IPlugin;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class CorrelationPlugin implements IPlugin {
    private JSlider sampleSizeSlider;
    private JLabel sampleSizeLabel;
    private JComboBox<String> xCombo, yCombo;
    private JTextArea outputArea;
    private DatabaseManager currentDb;

    @Override
    public String getName() {
        return "Корреляция (Выборка)";
    }

    @Override
    public String getDescription() {
        return "Расчет корреляции Пирсона";
    }

    @Override
    public JPanel execute(DatabaseManager dbManager) {
        this.currentDb = dbManager;
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        List<String> cols = dbManager.getColumnNames();
        xCombo = new JComboBox<>(cols.toArray(new String[0]));
        yCombo = new JComboBox<>(cols.toArray(new String[0]));
        
        sampleSizeSlider = new JSlider(2, 100, 100);
        sampleSizeLabel = new JLabel("Размер выборки: 100%");
        
        sampleSizeSlider.addChangeListener(e -> {
            sampleSizeLabel.setText("Размер выборки: " + sampleSizeSlider.getValue() + "%");
            if (!sampleSizeSlider.getValueIsAdjusting()) {
                calculate();
            }
        });

        JButton btnCalc = new JButton("Пересчитать вручную");
        btnCalc.addActionListener(e -> calculate());

        gbc.gridx = 0; gbc.gridy = 0; controlPanel.add(new JLabel("Переменная X:"), gbc);
        gbc.gridx = 1; controlPanel.add(xCombo, gbc);
        gbc.gridx = 2; controlPanel.add(new JLabel("Переменная Y:"), gbc);
        gbc.gridx = 3; controlPanel.add(yCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; controlPanel.add(sampleSizeLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; controlPanel.add(sampleSizeSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; controlPanel.add(btnCalc, gbc);

        outputArea = new JTextArea(12, 45);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setEditable(false);
        outputArea.setBorder(BorderFactory.createTitledBorder("Результаты анализа"));

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        if (dbManager.hasData()) {
            calculate();
        }

        return mainPanel;
    }

    private void calculate() {
        if (currentDb == null || !currentDb.hasData()) {
            outputArea.setText("Данные не загружены в систему.");
            return;
        }

        String colX = (String) xCombo.getSelectedItem();
        String colY = (String) yCombo.getSelectedItem();
        int percent = sampleSizeSlider.getValue();

        try {
            ResultSet countRs = currentDb.executeQuery("SELECT COUNT(*) FROM dataset");
            int total = 0;
            if (countRs.next()) {
                total = countRs.getInt(1);
            }
            
            int limit = (int) (total * (percent / 100.0));
            if (limit < 2) limit = 2;

            List<Double> X = new ArrayList<>();
            List<Double> Y = new ArrayList<>();

            // Используем кавычки для имен колонок, чтобы избежать проблем с пробелами или спецсимволами
            String query;
            if (percent == 100) {
                query = String.format("SELECT \"%s\", \"%s\" FROM dataset", colX, colY);
            } else {
                query = String.format("SELECT \"%s\", \"%s\" FROM dataset ORDER BY RANDOM() LIMIT %d", colX, colY, limit);
            }

            ResultSet rs = currentDb.executeQuery(query);
            while (rs.next()) {
                try {
                    // Безопасное получение данных по именам колонок, а не по индексам
                    String valXStr = rs.getString(1); // Первая колонка из SELECT
                    String valYStr = rs.getString(2); // Вторая колонка из SELECT
                    
                    if (valXStr != null && valYStr != null) {
                        double x = Double.parseDouble(valXStr.replace(",", "."));
                        double y = Double.parseDouble(valYStr.replace(",", "."));
                        X.add(x);
                        Y.add(y);
                    }
                } catch (Exception ignored) {
                    // Игнорируем строки с ошибками парсинга (например, заголовки или текст в ячейках)
                }
            }

            double r = calculatePearson(X, Y);
            
            outputArea.setText("ОТЧЕТ О КОРРЕЛЯЦИИ\n");
            outputArea.append("========================================\n");
            outputArea.append(String.format("Анализ: %s VS %s\n", colX, colY));
            outputArea.append(String.format("Общее число строк в БД: %d\n", total));
            outputArea.append(String.format("Использовано в расчете: %d\n", X.size()));
            outputArea.append(String.format("Выбранный процент:      %d%%\n", percent));
            outputArea.append("----------------------------------------\n");
            outputArea.append(String.format("Коэффициент r-Пирсона:  %.6f\n", r));
            outputArea.append(String.format("Тип связи:             %s\n", interpret(r)));
            outputArea.append("========================================\n");

        } catch (Exception e) {
            outputArea.setText("Ошибка при выполнении расчета:\n" + e.getMessage());
        }
    }

    private double calculatePearson(List<Double> X, List<Double> Y) {
        int n = X.size();
        if (n < 2) return 0;
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        
        for (int i = 0; i < n; i++) {
            double xi = X.get(i);
            double yi = Y.get(i);
            sumX += xi;
            sumY += yi;
            sumXY += xi * yi;
            sumX2 += xi * xi;
            sumY2 += yi * yi;
        }
        
        double numerator = (n * sumXY) - (sumX * sumY);
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        
        return (denominator == 0) ? 0 : numerator / denominator;
    }

    private String interpret(double r) {
        double absR = Math.abs(r);
        if (absR >= 0.9) return "Очень сильная";
        if (absR >= 0.7) return "Сильная";
        if (absR >= 0.5) return "Заметная";
        if (absR >= 0.3) return "Умеренная";
        if (absR > 0)    return "Слабая";
        return "Отсутствует";
    }
}