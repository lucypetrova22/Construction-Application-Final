import javax.swing.*;
import java.sql.Connection;

public class salespage extends JFrame {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JButton clientSalesButton;
    private JButton subconstructorSalesButton;

    private final Connection conn;

    public salespage(Connection connection) {
        this.conn = connection;

        setContentPane(mainPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Button actions
        clientSalesButton.addActionListener(e -> {
            dispose();
            new clientsSalesPage(conn);
        });

        subconstructorSalesButton.addActionListener(e -> {
            dispose();
            new subconstructorsSalesPage(conn);
        });
    }
}