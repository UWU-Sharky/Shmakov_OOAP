package core;

import javax.swing.JPanel;


public interface IPlugin {
    String getName();
    String getDescription();
    JPanel execute(DatabaseManager dbManager);
}