import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ProfileImageUploader extends JFrame {
    private final Connection conn;
    private final String nameOrId;
    private final String entityType;

    public ProfileImageUploader(Connection conn, String nameOrId, String entityType) {
        this.conn = conn;
        this.nameOrId = nameOrId;
        this.entityType = entityType;

        setTitle("Upload Profile Image - " + entityType);
        setSize(400, 200);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        setupUI();
        setVisible(true);
    }

    private void setupUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel label = new JLabel("Upload image for " + entityType + ": " + nameOrId, JLabel.CENTER);
        panel.add(label, BorderLayout.NORTH);

        JButton uploadButton = new JButton("Choose Image and Upload");
        panel.add(uploadButton, BorderLayout.CENTER);

        uploadButton.addActionListener(e -> uploadImage());

        add(panel);
    }

    private void uploadImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File imageFile = fileChooser.getSelectedFile();

            try (FileInputStream fis = new FileInputStream(imageFile)) {
                String sql;
                PreparedStatement ps;

                if (entityType.equalsIgnoreCase("Client")) {
                    sql = "UPDATE Clients SET ProfilePicture = ? WHERE ClientFullName = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setBinaryStream(1, fis, (int) imageFile.length());
                    ps.setString(2, nameOrId.trim());

                } else if (entityType.equalsIgnoreCase("Subconstructor")) {
                    sql = "UPDATE Subconstructor SET CompanyImage = ? WHERE CompanyName = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setBinaryStream(1, fis, (int) imageFile.length());
                    ps.setString(2, nameOrId.trim());

                } else if (entityType.equalsIgnoreCase("Product")) {
                    sql = "UPDATE Products SET ProductImage = ? WHERE ProductID = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setBinaryStream(1, fis, (int) imageFile.length());
                    ps.setInt(2, Integer.parseInt(nameOrId.trim())); // ProductID is an integer

                } else {
                    JOptionPane.showMessageDialog(this, "Unsupported entity type.");
                    return;
                }

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Image uploaded successfully!");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, entityType + " not found!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error uploading image: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Connection conn = dbcon.getConnection(); // your database connection method

        String[] options = {"Client", "Subconstructor", "Product"};
        int choice = JOptionPane.showOptionDialog(null,
                "What do you want to upload an image for?",
                "Select Type",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == -1) return; // User canceled

        String entityType = options[choice];
        String nameOrId;

        if (entityType.equals("Product")) {
            nameOrId = JOptionPane.showInputDialog("Enter Product ID:");
            if (nameOrId == null || nameOrId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "You must enter a Product ID.");
                return;
            }
            try {
                Integer.parseInt(nameOrId.trim()); // validate it's a number
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid Product ID.");
                return;
            }
        } else {
            nameOrId = JOptionPane.showInputDialog("Enter " + (entityType.equals("Client") ? "Client Full Name" : "Subconstructor Company Name") + ":");
            if (nameOrId == null || nameOrId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "You must enter a name.");
                return;
            }
        }

        new ProfileImageUploader(conn, nameOrId.trim(), entityType);
    }
}
