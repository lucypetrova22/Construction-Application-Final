import javax.swing.*;
import java.sql.Connection;

public class dashboardpage extends JFrame {
    private JPanel panel1;
    private JButton subcontractorsButton;
    private JButton clientsButton;
    private JButton salesButton;
    private JButton productsButton;
    private JButton revenueButton;
    private JButton discountsButton;
    private final Connection connection;

    public dashboardpage(Connection connection) {
        this.connection = connection;

        setTitle("Construction DB - Dashboard");
        setSize(800, 500);
        setContentPane(panel1);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        clientsButton.addActionListener(e -> new clientspage(connection));
        subcontractorsButton.addActionListener(e -> new subconstructorspage(connection));
        productsButton.addActionListener(e -> new productspage(connection));
        salesButton.addActionListener(e -> new salespage(connection));
        revenueButton.addActionListener(e -> new revenuepage(connection));
        discountsButton.addActionListener(e -> new discountspage(connection));

        setVisible(true);
    }
}
