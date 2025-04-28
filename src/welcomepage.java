import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class welcomepage extends JFrame {
    private JPanel panel1;
    private JLabel imageLabel;
    private JLabel titleLabel;
    private JButton loginButton;
    private JButton signUpButton;
    private JLabel instructionLabel;
    private final Connection connection;

    public welcomepage(Connection connection) {
        this.connection = connection;

        setTitle("Construction Database Application");
        setSize(520, 490);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(panel1); // from .form
        setLocationRelativeTo(null);

        // Load image
        ImageIcon icon = new ImageIcon("src/welcomepage_image.png");
        Image image = icon.getImage().getScaledInstance(275, 275, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(image));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Button actions
        loginButton.addActionListener(e -> {
            dispose();
            new loginpage(connection); // ✅ Pass connection
        });

        signUpButton.addActionListener(e -> {
            dispose();
            new signuppage(connection); // ✅ Pass connection
        });

        setVisible(true);
    }
}
