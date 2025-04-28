import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.InputStream;
import java.sql.*;

public class subconstructorspage extends JFrame {
    private JPanel panel1;
    private JTable subcontractorTable;
    private JTextField searchField;
    private JComboBox<String> topComboBox;
    private JComboBox<String> sortComboBox;
    private JButton addSubconstructorButton;
    private JButton viewSubconstructorButton;
    private JButton updateSubconstructorButton;
    private JButton removeSubconstructorButton;
    private JButton showTopButton;

    private DefaultTableModel tableModel;
    private final Connection conn;

    public subconstructorspage(Connection connection) {
        this.conn = connection;
        setTitle("Subconstructors Management");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(panel1); // important: GUI panel

        setupTable();
        setupListeners();
        loadSubcontractors();

        setVisible(true);
    }

    private void setupTable() {
        tableModel = new DefaultTableModel(new Object[]{
                "Subconstructor ID", "Company Name", "Company Address", "Industry", "Purchase Date"
        }, 0);
        subcontractorTable.setModel(tableModel);
    }

    private void setupListeners() {
        searchField.addActionListener(e -> filterSubcontractorsByName(searchField.getText()));
        sortComboBox.addActionListener(e -> filterSubcontractorsByName(searchField.getText()));
        addSubconstructorButton.addActionListener(e -> addSubcontractor());
        updateSubconstructorButton.addActionListener(e -> updateSubcontractor());
        removeSubconstructorButton.addActionListener(e -> removeSubcontractor());
        viewSubconstructorButton.addActionListener(e -> viewSubcontractorProfile());
        showTopButton.addActionListener(e -> showTopSubconstructors());
    }

    private void loadSubcontractors() {
        filterSubcontractorsByName("");
    }

    private void filterSubcontractorsByName(String keyword) {
        try {
            tableModel.setRowCount(0);
            String sortOption = (String) sortComboBox.getSelectedItem();
            String orderBy = switch (sortOption) {
                case "Name A-Z" -> "CompanyName ASC";
                case "Name Z-A" -> "CompanyName DESC";
                case "Latest Purchase" -> "CompanyPurchaseDate DESC";
                case "Oldest Purchase" -> "CompanyPurchaseDate ASC";
                default -> "CompanyName ASC";
            };

            String query = "SELECT SubconstructorID, CompanyName, CompanyAddress, Industry, CompanyPurchaseDate " +
                    "FROM Subconstructor " +
                    "WHERE CompanyName LIKE ? OR CompanyAddress LIKE ? OR Industry LIKE ? " +
                    "ORDER BY " + orderBy;

            PreparedStatement ps = conn.prepareStatement(query);
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("SubconstructorID"),
                        rs.getString("CompanyName"),
                        rs.getString("CompanyAddress"),
                        rs.getString("Industry"),
                        rs.getDate("CompanyPurchaseDate")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading subcontractors: " + e.getMessage());
        }
    }

    private void addSubcontractor() {
        JTextField name = new JTextField();
        JTextField address = new JTextField();
        JTextField industry = new JTextField();
        JTextField purchaseDate = new JTextField("YYYY-MM-DD");

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Company Name:")); panel.add(name);
        panel.add(new JLabel("Company Address:")); panel.add(address);
        panel.add(new JLabel("Industry:")); panel.add(industry);
        panel.add(new JLabel("Purchase Date:")); panel.add(purchaseDate);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Subconstructor", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Subconstructor (CompanyName, CompanyAddress, Industry, CompanyPurchaseDate) VALUES (?, ?, ?, ?)"
                );
                ps.setString(1, name.getText());
                ps.setString(2, address.getText());
                ps.setString(3, industry.getText());
                ps.setDate(4, Date.valueOf(purchaseDate.getText()));
                ps.executeUpdate();
                loadSubcontractors();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding subcontractor: " + e.getMessage());
            }
        }
    }

    private void updateSubcontractor() {
        int row = subcontractorTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a subcontractor to update.");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        JTextField name = new JTextField((String) tableModel.getValueAt(row, 1));
        JTextField address = new JTextField((String) tableModel.getValueAt(row, 2));
        JTextField industry = new JTextField((String) tableModel.getValueAt(row, 3));
        JTextField purchaseDate = new JTextField(tableModel.getValueAt(row, 4).toString());

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Company Name:")); panel.add(name);
        panel.add(new JLabel("Company Address:")); panel.add(address);
        panel.add(new JLabel("Industry:")); panel.add(industry);
        panel.add(new JLabel("Purchase Date:")); panel.add(purchaseDate);

        int result = JOptionPane.showConfirmDialog(this, panel, "Update Subconstructor", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Subconstructor SET CompanyName=?, CompanyAddress=?, Industry=?, CompanyPurchaseDate=? WHERE SubconstructorID=?"
                );
                ps.setString(1, name.getText());
                ps.setString(2, address.getText());
                ps.setString(3, industry.getText());
                ps.setDate(4, Date.valueOf(purchaseDate.getText()));
                ps.setInt(5, id);
                ps.executeUpdate();
                loadSubcontractors();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error updating subcontractor: " + e.getMessage());
            }
        }
    }

    private void removeSubcontractor() {
        int row = subcontractorTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a subcontractor to remove.");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this subcontractor?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM Subconstructor WHERE SubconstructorID = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
                loadSubcontractors();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error removing subcontractor: " + e.getMessage());
            }
        }
    }

    private void viewSubcontractorProfile() {
        int row = subcontractorTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a subcontractor to view.");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT CompanyName, CompanyImage FROM Subconstructor WHERE SubconstructorID = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String name = rs.getString("CompanyName");
                InputStream imgStream = rs.getBinaryStream("CompanyImage");

                JPanel profilePanel = new JPanel(new BorderLayout(10, 10));
                profilePanel.add(new JLabel("Company: " + name, JLabel.CENTER), BorderLayout.NORTH);

                if (imgStream != null) {
                    Image image = ImageIO.read(imgStream);
                    ImageIcon icon = new ImageIcon(image.getScaledInstance(200, 200, Image.SCALE_SMOOTH));
                    JLabel imgLabel = new JLabel(icon);
                    profilePanel.add(imgLabel, BorderLayout.CENTER);
                } else {
                    profilePanel.add(new JLabel("No image available."), BorderLayout.CENTER);
                }

                JOptionPane.showMessageDialog(this, profilePanel, "Subconstructor Profile", JOptionPane.PLAIN_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading subcontractor profile: " + e.getMessage());
        }
    }

    private void showTopSubconstructors() {
        int limit = switch ((String) topComboBox.getSelectedItem()) {
            case "Top 5" -> 5;
            case "Top 10" -> 10;
            default -> 3;
        };

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT sc.CompanyName, SUM(s.ProductPrice * s.QuantitySold) AS TotalSales " +
                            "FROM Subconstructor sc JOIN Sales s ON sc.SubconstructorID = s.SubconstructorID " +
                            "GROUP BY sc.SubconstructorID, sc.CompanyName " +
                            "ORDER BY TotalSales DESC LIMIT ?"
            );
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("Top Subconstructors by Sales:\n");
            while (rs.next()) {
                sb.append(rs.getString("CompanyName")).append(" - $")
                        .append(String.format("%.2f", rs.getDouble("TotalSales"))).append("\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString(), "Top Subconstructors", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading top subcontractors: " + e.getMessage());
        }
    }
}
