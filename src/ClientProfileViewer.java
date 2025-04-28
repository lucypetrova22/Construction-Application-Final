import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClientProfileViewer extends JFrame {
    private final Connection conn;
    private final String clientName;
    private JPanel panel1;
    private JTextArea infoArea;
    private JLabel profilePictureLabel;

    public ClientProfileViewer(Connection conn, String clientName) {
        this.conn = conn;
        this.clientName = clientName;

        setTitle("Client Profile - " + clientName);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setContentPane(panel1);
        loadClientProfile();
        setVisible(true);
    }

    private void loadClientProfile() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT ClientContactInfo, ClientAddress, ClientPurchaseDate, ProfilePicture " +
                            "FROM Clients WHERE ClientFullName = ?"
            );
            ps.setString(1, clientName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String contact = rs.getString("ClientContactInfo");
                String address = rs.getString("ClientAddress");
                String purchaseDate = rs.getDate("ClientPurchaseDate").toString();
                InputStream imageStream = rs.getBinaryStream("ProfilePicture");

                // Set text info into infoArea
                infoArea.setText(
                        "Name: " + clientName + "\n" +
                                "Contact: " + contact + "\n" +
                                "Address: " + address + "\n" +
                                "Purchase Date: " + purchaseDate
                );

                // Set profile picture if available
                if (imageStream != null) {
                    ImageIcon imageIcon = new ImageIcon(ImageIO.read(imageStream));
                    Image scaledImage = imageIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    profilePictureLabel.setIcon(new ImageIcon(scaledImage));
                } else {
                    profilePictureLabel.setText("No profile picture available.");
                    profilePictureLabel.setHorizontalAlignment(SwingConstants.CENTER);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Client not found.");
                dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading client profile: " + e.getMessage());
        }
    }
}
