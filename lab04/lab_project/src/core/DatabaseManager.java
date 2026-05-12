package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private Connection conn;
    private List<String> columnNames = new ArrayList<>();
    private boolean dataLoaded = false;
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    public DatabaseManager() {
        try {
            // Используем БД в памяти для удобства (удаляется после закрытия)
            conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при подключении к базе данных", e);
        }
    }

    public boolean hasData() { return dataLoaded; }
    public List<String> getColumnNames() { return columnNames; }

    public int importCSV(File file) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) throw new Exception("Файл пуст");

            String[] headers = headerLine.split(",");
            columnNames = Arrays.asList(headers);

            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE IF EXISTS dataset");
            
            StringBuilder createTable = new StringBuilder("CREATE TABLE dataset (");
            for (int i = 0; i < headers.length; i++) {
                createTable.append("\"").append(headers[i].trim()).append("\" TEXT");
                if (i < headers.length - 1) createTable.append(", ");
            }
            createTable.append(")");
            stmt.execute(createTable.toString());

            String insertSql = "INSERT INTO dataset VALUES (" + "?,".repeat(headers.length).replaceAll(",$", "") + ")";
            PreparedStatement pstmt = conn.prepareStatement(insertSql);

            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (int i = 0; i < headers.length; i++) {
                    pstmt.setString(i + 1, i < values.length ? values[i].trim() : "");
                }
                pstmt.addBatch();
                count++;
            }
            pstmt.executeBatch();
            dataLoaded = true;
            return count;
        }
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        return conn.createStatement().executeQuery(sql);
    }
}