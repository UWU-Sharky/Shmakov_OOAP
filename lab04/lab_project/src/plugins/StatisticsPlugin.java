package plugins;

import core.DatabaseManager;
import core.IPlugin;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StatisticsPlugin implements IPlugin {
    @Override
    public String getName() { return "Описательная Статистика"; }

    @Override
    public String getDescription() { return "Вычисляет базовые метрики для числовых данных."; }

    @Override
public JPanel execute(DatabaseManager dbManager) {
    JPanel mainPanel = new JPanel(new BorderLayout()); // Создаем панель заранее
    
    try {
        String[] columns = {"Параметр", "Минимум", "Максимум", "Среднее"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        List<String> colNames = dbManager.getColumnNames();
        ResultSet rs = dbManager.executeQuery("SELECT * FROM dataset");
        
        Map<String, List<Double>> dataMap = new HashMap<>();
        for (String col : colNames) dataMap.put(col, new ArrayList<>());

        while (rs.next()) {
            for (String col : colNames) {
                try {
                    String val = rs.getString(col);
                    if (val != null) dataMap.get(col).add(Double.parseDouble(val));
                } catch (Exception ignored) {}
            }
        }

        for (String col : colNames) {
            List<Double> vals = dataMap.get(col);
            if (!vals.isEmpty()) {
                double min = vals.stream().mapToDouble(v -> v).min().orElse(0);
                double max = vals.stream().mapToDouble(v -> v).max().orElse(0);
                double avg = vals.stream().mapToDouble(v -> v).average().orElse(0);
                model.addRow(new Object[]{col, min, max, String.format("%.2f", avg)});
            }
        }
        
        mainPanel.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

    } catch (SQLException e) {
        // Если произошла ошибка, просто добавляем текст ошибки на ту же панель
        mainPanel.removeAll();
        mainPanel.add(new JLabel("Ошибка БД: " + e.getMessage()), BorderLayout.CENTER);
    }

    return mainPanel; // Возвращаем именно JPanel
}
}