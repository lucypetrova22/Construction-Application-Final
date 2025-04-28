import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.Connection;

public class signuppage extends JFrame {
  private JPanel panel1;
  private JLabel firstNameLabel;
   private JLabel lastNameLabel;
    private JLabel emailLabel;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JPasswordField passwordField1;
    private JPasswordField passwordField2;
    private JButton uploadPhotoBtn;
    private JButton signUpBtn;
    private JLabel imagePreviewLabel;
    private File selectedImageFile;
    private JLabel passwordLabel;
    private JLabel repeatPasswordLabel;
    private final Connection connection;

    public signuppage(Connection connection) {
        this.connection = connection;

        setTitle("Sign Up");
        setContentPane(panel1); // panel1 is the root from the .form
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        // Upload photo logic
        uploadPhotoBtn.addActionListener(e -> uploadImage());

        // Sign up logic
        signUpBtn.addActionListener(e -> {
            String password = new String(passwordField1.getPassword());
            String repeatPassword = new String(passwordField2.getPassword());

            if (!password.equals(repeatPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!isValidPassword(password)) {
                JOptionPane.showMessageDialog(this,
                        "Password must be at least 8 characters and contain at least 2 special characters.",
                        "Invalid Password", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty()
                    || emailField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill out all fields.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Simulated data save
            userdatastore.userCredentials.put(emailField.getText(), password);
            JOptionPane.showMessageDialog(this, "User registered successfully!");
            dispose();
            new loginpage(connection, emailField.getText());
        });
    }

    private void uploadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose an Image");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()) {
                ImageIcon icon = new ImageIcon(selectedFile.getAbsolutePath());

                // Define fixed size for the preview
                int previewWidth = 150;
                int previewHeight = 150;

                // Scale the image nicely
                Image scaled = icon.getImage().getScaledInstance(previewWidth, previewHeight, Image.SCALE_SMOOTH);

                // Set the image to label
                imagePreviewLabel.setIcon(new ImageIcon(scaled));

                // Set preferred size for the label (important!)
                imagePreviewLabel.setPreferredSize(new Dimension(previewWidth, previewHeight));

                imagePreviewLabel.repaint();
            } else {
                JOptionPane.showMessageDialog(this, "File not found.");
            }
        }
    }



    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;

        for (char ch : password.toCharArray()) {
            if ("!@#$%^&*()_+=-{}[]:;'\"<>,.?/~\\|".indexOf(ch) != -1) {
                return true; // Found at least one special character
            }
        }
        return false; // No special character found
    }
}
