package core;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import plugins.*;

public class LabProjectApp extends JFrame {
    private final DatabaseManager dbManager = new DatabaseManager();
    private final List<IPlugin> plugins = new ArrayList<>();
    private JPanel pluginContainer;
    private CardLayout cardLayout;

    public LabProjectApp() {
        setTitle("Система анализа данных c паттерном");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Имитация динамической загрузки плагинов
        plugins.add(new StatisticsPlugin());
        plugins.add(new ScatterplotPlugin());
        plugins.add(new CorrelationPlugin());
        plugins.add(new ExportPlugin());

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Панель управления
        JPanel topPanel = new JPanel();
        JButton btnLoad = new JButton("Загрузить CSV");
        btnLoad.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    dbManager.importCSV(fc.getSelectedFile());
                    JOptionPane.showMessageDialog(this, "Данные загружены!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
                }
            }
        });
        topPanel.add(btnLoad);
        add(topPanel, BorderLayout.NORTH);

        // Список плагинов слева
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (IPlugin p : plugins) listModel.addElement(p.getName());
        JList<String> list = new JList<>(listModel);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                IPlugin selected = plugins.get(list.getSelectedIndex());
                if (dbManager.hasData()) {
                    JPanel ui = selected.execute(dbManager);
                    pluginContainer.add(ui, selected.getName());
                    cardLayout.show(pluginContainer, selected.getName());
                } else {
                    JOptionPane.showMessageDialog(this, "Сначала загрузите данные!");
                }
            }
        });

        cardLayout = new CardLayout();
        pluginContainer = new JPanel(cardLayout);
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), pluginContainer);
        split.setDividerLocation(200);
        add(split, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LabProjectApp().setVisible(true));
    }
}