package monolith;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MonolithicApp extends JFrame {
    private Connection conn;
    private List<String> columnNames = new ArrayList<>();
    private boolean dataLoaded = false;
    private int totalRowsInDb = 0;

    private JTabbedPane tabbedPane;
    private JLabel statusLabel;
    private DefaultTableModel statTableModel;
    private JComboBox<String> scatterXCombo, scatterYCombo;
    private JPanel scatterCanvas;
    private JComboBox<String> corrXCombo, corrYCombo;
    private JSlider sampleSizeSlider;
    private JLabel sampleSizeLabel;
    private JTextArea corrOutput;

    public MonolithicApp() {
        initDatabase();
        initUI();
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite::memory:");
            System.out.println("База данных готова.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка БД: " + e.getMessage());
        }
    }

    private void initUI() {
        setTitle("Система анализа данных без паттерна");
        setSize(1000, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnLoad = new JButton("Загрузить CSV");
        statusLabel = new JLabel("Ожидание данных...");
        btnLoad.addActionListener(e -> loadCSV());
        topPanel.add(btnLoad);
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        
        // 1. Статистика
        statTableModel = new DefaultTableModel(new String[]{"Колонка", "Мин", "Макс", "Среднее"}, 0);
        tabbedPane.addTab("Статистика", new JScrollPane(new JTable(statTableModel)));

        // 2. График (Диаграмма рассеяния)
        JPanel scatterPanel = new JPanel(new BorderLayout());
        JPanel scControls = new JPanel();
        scatterXCombo = new JComboBox<>();
        scatterYCombo = new JComboBox<>();
        JButton btnDraw = new JButton("Построить график");
        btnDraw.addActionListener(e -> drawScatter());
        scControls.add(new JLabel("X:")); scControls.add(scatterXCombo);
        scControls.add(new JLabel("Y:")); scControls.add(scatterYCombo);
        scControls.add(btnDraw);
        
        scatterCanvas = new JPanel(new BorderLayout());
        scatterCanvas.setBackground(Color.WHITE);
        scatterPanel.add(scControls, BorderLayout.NORTH);
        scatterPanel.add(scatterCanvas, BorderLayout.CENTER);
        tabbedPane.addTab("График", scatterPanel);

        // 3. Анализ (Корреляция)
        JPanel corrPanel = new JPanel(new BorderLayout());
        JPanel crControls = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        corrXCombo = new JComboBox<>();
        corrYCombo = new JComboBox<>();
        
        sampleSizeSlider = new JSlider(2, 100, 100);
        sampleSizeSlider.setEnabled(false);
        sampleSizeLabel = new JLabel("Размер выборки: 100%");
        
        sampleSizeSlider.addChangeListener(e -> {
            sampleSizeLabel.setText("Размер выборки: " + sampleSizeSlider.getValue() + "%");
            if (!sampleSizeSlider.getValueIsAdjusting() && dataLoaded) {
                calculateCorrelation();
            }
        });

        JButton btnCorr = new JButton("Рассчитать сейчас");
        btnCorr.addActionListener(e -> calculateCorrelation());

        gbc.gridx = 0; gbc.gridy = 0; crControls.add(new JLabel("Переменная X:"), gbc);
        gbc.gridx = 1; crControls.add(corrXCombo, gbc);
        gbc.gridx = 2; gbc.gridy = 0; crControls.add(new JLabel("Переменная Y:"), gbc);
        gbc.gridx = 3; crControls.add(corrYCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        JLabel hintLabel = new JLabel("Использовать подмножество данных (для больших файлов):");
        hintLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        crControls.add(hintLabel, gbc);

        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 1; crControls.add(sampleSizeLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; crControls.add(sampleSizeSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4; crControls.add(btnCorr, gbc);

        corrOutput = new JTextArea();
        corrOutput.setFont(new Font("Monospaced", Font.PLAIN, 13));
        corrOutput.setEditable(false);
        corrPanel.add(crControls, BorderLayout.NORTH);
        corrPanel.add(new JScrollPane(corrOutput), BorderLayout.CENTER);
        tabbedPane.addTab("Корреляция", corrPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void loadCSV() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {
                String header = br.readLine();
                if (header == null) return;
                String[] cols = header.split(",");
                columnNames = new ArrayList<>();
                for(String s : cols) columnNames.add(s.trim());

                Statement st = conn.createStatement();
                st.execute("DROP TABLE IF EXISTS data");
                
                StringBuilder sb = new StringBuilder("CREATE TABLE data (");
                for (int i = 0; i < columnNames.size(); i++) {
                    sb.append("\"").append(columnNames.get(i)).append("\" REAL")
                      .append(i < columnNames.size() - 1 ? "," : "");
                }
                sb.append(")");
                st.execute(sb.toString());

                String placeholders = "?,".repeat(columnNames.size()).replaceAll(",$", "");
                PreparedStatement ps = conn.prepareStatement("INSERT INTO data VALUES (" + placeholders + ")");
                
                String line;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] vals = line.split(",");
                    for (int i = 0; i < columnNames.size(); i++) {
                        try {
                            if (i < vals.length) {
                                ps.setDouble(i + 1, Double.parseDouble(vals[i].trim().replace(",", ".")));
                            } else {
                                ps.setDouble(i + 1, 0.0);
                            }
                        } catch (Exception e) {
                            ps.setDouble(i + 1, 0.0);
                        }
                    }
                    ps.addBatch();
                    count++;
                }
                ps.executeBatch();
                dataLoaded = true;
                totalRowsInDb = count;
                statusLabel.setText("Успешно загружено: " + count + " строк");
                
                sampleSizeSlider.setEnabled(true);
                updateControls();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки: " + ex.getMessage());
            }
        }
    }

    private void updateControls() {
        scatterXCombo.removeAllItems(); scatterYCombo.removeAllItems();
        corrXCombo.removeAllItems(); corrYCombo.removeAllItems();
        statTableModel.setRowCount(0);
        for (String col : columnNames) {
            scatterXCombo.addItem(col); scatterYCombo.addItem(col);
            corrXCombo.addItem(col); corrYCombo.addItem(col);
            try {
                ResultSet rs = conn.createStatement().executeQuery(
                    String.format("SELECT MIN(\"%1$s\"), MAX(\"%1$s\"), AVG(\"%1$s\") FROM data", col));
                if (rs.next()) {
                    statTableModel.addRow(new Object[]{col, rs.getDouble(1), rs.getDouble(2), String.format("%.3f", rs.getDouble(3))});
                }
            } catch (Exception ignored) {}
        }
    }

    private void drawScatter() {
        if (!dataLoaded) return;
        String xCol = (String) scatterXCombo.getSelectedItem();
        String yCol = (String) scatterYCombo.getSelectedItem();
        List<Point2D.Double> points = new ArrayList<>();
        try {
            // УБРАН LIMIT: теперь загружаются ВСЕ данные для графика
            ResultSet rs = conn.createStatement().executeQuery(String.format("SELECT \"%s\", \"%s\" FROM data", xCol, yCol));
            while (rs.next()) {
                points.add(new Point2D.Double(rs.getDouble(1), rs.getDouble(2)));
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        scatterCanvas.removeAll();
        scatterCanvas.add(new ScatterPlotPanel(points, xCol, yCol));
        scatterCanvas.revalidate();
        scatterCanvas.repaint();
    }

    private void calculateCorrelation() {
        if (!dataLoaded) return;
        String xCol = (String) corrXCombo.getSelectedItem();
        String yCol = (String) corrYCombo.getSelectedItem();
        
        int limit = (int) (totalRowsInDb * (sampleSizeSlider.getValue() / 100.0));
        if (limit < 2) limit = 2;

        try {
            List<Double> X = new ArrayList<>();
            List<Double> Y = new ArrayList<>();
            
            // Если ползунок на 100%, берем данные по порядку, иначе - случайным образом
            String query;
            if (sampleSizeSlider.getValue() == 100) {
                query = String.format("SELECT \"%s\", \"%s\" FROM data", xCol, yCol);
            } else {
                query = String.format("SELECT \"%s\", \"%s\" FROM data ORDER BY RANDOM() LIMIT %d", xCol, yCol, limit);
            }

            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                X.add(rs.getDouble(1));
                Y.add(rs.getDouble(2));
            }

            double r = calculatePearson(X, Y);
            
            corrOutput.setText("ОТЧЕТ ОБ АНАЛИЗЕ КОРРЕЛЯЦИИ\n");
            corrOutput.append("==============================================\n");
            corrOutput.append("Переменные: " + xCol + " vs " + yCol + "\n");
            corrOutput.append("----------------------------------------------\n");
            corrOutput.append(String.format("ВСЕГО СТРОК В БД: %d\n", totalRowsInDb));
            corrOutput.append(String.format("ИСПОЛЬЗОВАНО:     %d строк (%d%%)\n", X.size(), sampleSizeSlider.getValue()));
            corrOutput.append("----------------------------------------------\n");
            corrOutput.append(String.format("КОЭФФИЦИЕНТ ПИРСЕНА (r): %.6f\n", r));
            
            String interpret;
            double absR = Math.abs(r);
            if (absR > 0.9) interpret = "Очень сильная корреляция";
            else if (absR > 0.7) interpret = "Сильная корреляция";
            else if (absR > 0.5) interpret = "Заметная корреляция";
            else if (absR > 0.3) interpret = "Умеренная корреляция";
            else interpret = "Слабая связь";
            
            corrOutput.append("ИНТЕРПРЕТАЦИЯ: " + interpret + "\n");
            
        } catch (Exception e) {
            corrOutput.setText("Ошибка расчетов: " + e.getMessage());
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
        double num = (n * sumXY) - (sumX * sumY);
        double den = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        return (den == 0) ? 0 : num / den;
    }

    class ScatterPlotPanel extends JPanel {
        private List<Point2D.Double> points;
        private String xLabel, yLabel;

        public ScatterPlotPanel(List<Point2D.Double> points, String xL, String yL) {
            this.points = points;
            this.xLabel = xL;
            this.yLabel = yL;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int margin = 60;
            int w = getWidth() - 2 * margin;
            int h = getHeight() - 2 * margin;

            if (points == null || points.isEmpty()) return;

            double minX = points.stream().mapToDouble(p -> p.x).min().orElse(0);
            double maxX = points.stream().mapToDouble(p -> p.x).max().orElse(1);
            double minY = points.stream().mapToDouble(p -> p.y).min().orElse(0);
            double maxY = points.stream().mapToDouble(p -> p.y).max().orElse(1);

            g2.setColor(Color.BLACK);
            g2.draw(new Line2D.Double(margin, getHeight() - margin, getWidth() - margin, getHeight() - margin));
            g2.draw(new Line2D.Double(margin, margin, margin, getHeight() - margin));

            g2.drawString(xLabel, getWidth() / 2, getHeight() - 20);
            g2.rotate(-Math.PI / 2);
            g2.drawString(yLabel, -getHeight() / 2, 20);
            g2.rotate(Math.PI / 2);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString(String.format("%.1f", minX), margin, getHeight() - margin + 15);
            g2.drawString(String.format("%.1f", maxX), getWidth() - margin - 20, getHeight() - margin + 15);
            g2.drawString(String.format("%.1f", minY), margin - 35, getHeight() - margin);
            g2.drawString(String.format("%.1f", maxY), margin - 35, margin + 10);

            g2.setColor(new Color(70, 130, 180, 150));
            for (Point2D.Double p : points) {
                double px = margin + ((p.x - minX) / (maxX - minX == 0 ? 1 : maxX - minX)) * w;
                double py = (getHeight() - margin) - ((p.y - minY) / (maxY - minY == 0 ? 1 : maxY - minY)) * h;
                g2.fill(new Ellipse2D.Double(px - 3, py - 3, 6, 6));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MonolithicApp().setVisible(true));
    }
}