import javax.swing.*;
import java.sql.Connection;

public class discountspage extends JFrame {
    private final Connection conn;
    private JPanel panel1;
    private JLabel titleLabel;
    private JButton clientDiscountsButton;
    private JButton subcontractorDiscountsButton;

    public discountspage(Connection conn) {
        this.conn = conn;

        setTitle("Discounts");
        setSize(400, 200);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(panel1); // Use GUI designer panel

        setupListeners();

        setVisible(true);
    }

    private void setupListeners() {
        clientDiscountsButton.addActionListener(e -> {
            new clientdiscountspage(conn);
            dispose();
        });

        subcontractorDiscountsButton.addActionListener(e -> {
            new subcontractordiscountspage(conn);
            dispose();
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here if needed
    }
}
