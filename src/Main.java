import javax.swing.*;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Connection connection = dbcon.getConnection();
            if (connection != null) {
                new welcomepage(connection);
            } else {
                JOptionPane.showMessageDialog(null, "Database connection failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}