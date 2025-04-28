import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.sql.Connection;

public class loginpage extends JFrame {
    private JPanel panel1;
    private JLabel header;
    private JLabel subHeader;
    private JLabel emailJLabel;
    private JLabel passwordJLabel;
    private JTextField userField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton createAccountBtn;
    private JLabel profilePicLabel; // 🔁 Bind this in your GUI form
    private final Connection connection;

    public loginpage(Connection connection, String prefillEmail) {
        this.connection = connection;
        setContentPane(panel1); // UI is handled in GUI Designer
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        pack(); // 👈 Use pack instead of hardcoded size
        setVisible(true);

        userField.setText(prefillEmail); // Prefill the email field

        // 🖼️ Load profile image based on email
        File imageFile = new File(prefillEmail + ".jpg");
        if (imageFile.exists()) {
            ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
            Image scaled = icon.getImage().getScaledInstance(
                    profilePicLabel.getWidth(), profilePicLabel.getHeight(), Image.SCALE_SMOOTH);
            profilePicLabel.setIcon(new ImageIcon(scaled));
        }

        // 🔐 Login button logic
        loginButton.addActionListener((ActionEvent e) -> {
            String email = userField.getText();
            String password = new String(passwordField.getPassword());

            if (!userdatastore.userCredentials.containsKey(email)) {
                JOptionPane.showMessageDialog(this, "This email is not registered.", "Error", JOptionPane.ERROR_MESSAGE);
            } else if (!userdatastore.userCredentials.get(email).equals(password)) {
                JOptionPane.showMessageDialog(this, "Incorrect password.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                dispose();
                new dashboardpage(connection);
            }
        });

        // ➕ Create account logic
        createAccountBtn.addActionListener((ActionEvent e) -> {
            dispose();
            new signuppage(connection);
        });
    }

    // Convenience constructor without prefilled email
    public loginpage(Connection connection) {
        this(connection, "");
    }
}
