package plugins;

import core.IPlugin;
import core.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Модуль (Плагин) для экспорта сводного отчета по данным в текстовый файл.
 */
public class ExportPlugin implements IPlugin {
    @Override
    public String getName() { return "Экспорт отчета"; }

    @Override
    public String getDescription() { return "Создание текстового файла с результатами анализа всех данных."; }

    @Override
    public JPanel execute(DatabaseManager dbManager) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        JButton exportBtn = new JButton("Сгенерировать и сохранить отчет (.txt)");
        exportBtn.setPreferredSize(new Dimension(300, 50));
        exportBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel infoLabel = new JLabel("Модуль создаст файл с описанием структуры и базовой статистикой данных.");
        infoLabel.setForeground(Color.GRAY);

        exportBtn.addActionListener(e -> performExport(dbManager));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(exportBtn, gbc);

        gbc.gridy = 1;
        panel.add(infoLabel, gbc);

        return panel;
    }

    private void performExport(DatabaseManager dbManager) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите место для сохранения отчета");
        fileChooser.setSelectedFile(new File("report_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".txt"));

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(generateReportContent(dbManager));
                JOptionPane.showMessageDialog(null, "Отчет успешно сохранен в:\n" + file.getAbsolutePath());
            } catch (IOException | SQLException ex) {
                JOptionPane.showMessageDialog(null, "Ошибка при сохранении: " + ex.getMessage());
            }
        }
    }

    private String generateReportContent(DatabaseManager dbManager) throws SQLException {
        StringBuilder sb = new StringBuilder();
        List<String> columns = dbManager.getColumnNames();
        
        sb.append("ОТЧЕТ ПО АНАЛИЗУ ДАННЫХ\n");
        sb.append("Дата генерации: ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())).append("\n");
        sb.append("==========================================\n\n");

        sb.append("1. СТРУКТУРА ДАННЫХ\n");
        sb.append("Всего колонок: ").append(columns.size()).append("\n");
        sb.append("Список полей: ").append(String.join(", ", columns)).append("\n\n");

        sb.append("2. СТАТИСТИЧЕСКАЯ СВОДКА\n");
        sb.append(String.format("%-20s | %-10s | %-10s | %-10s\n", "Колонка", "Мин.", "Макс.", "Среднее"));
        sb.append("-".repeat(60)).append("\n");

        // Базовая логика сбора статистики для отчета
        ResultSet rs = dbManager.executeQuery("SELECT * FROM dataset");
        // Примечание: Для больших файлов здесь лучше использовать агрегатные функции SQL (MIN, MAX, AVG)
        // Но для простоты модуля используем проход по результатам
        
        for (String col : columns) {
            try {
                // Пытаемся получить статистику через SQL для каждой колонки
                ResultSet statsRs = dbManager.executeQuery(
                    String.format("SELECT MIN(\"%s\"), MAX(\"%s\"), AVG(\"%s\") FROM dataset", col, col, col)
                );
                if (statsRs.next()) {
                    double min = statsRs.getDouble(1);
                    double max = statsRs.getDouble(2);
                    double avg = statsRs.getDouble(3);
                    sb.append(String.format("%-20s | %-10.2f | %-10.2f | %-10.2f\n", col, min, max, avg));
                }
            } catch (SQLException e) {
                sb.append(String.format("%-20s | %-35s\n", col, "[Нечисловые данные]"));
            }
        }

        sb.append("\n==========================================\n");
        sb.append("Конец отчета.\n");
        
        return sb.toString();
    }
}