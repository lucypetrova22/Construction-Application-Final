import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class subcontractordiscountspage extends JFrame {
    private final Connection conn;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JPanel panel1; // your form's panel

    public subcontractordiscountspage(Connection conn) {
        this.conn = conn;
        setTitle("Subconstructor Discounts");
        setSize(900, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // ⚡ VERY IMPORTANT: use the GUI-designed panel
        setContentPane(panel1);

        // ⚡ Assign the model to your GUI table
        tableModel = new DefaultTableModel(new Object[]{
                "Discount ID", "Company Name", "Company Address", "Industry", "Purchase Date", "Discount Value ($)", "Reason"
        }, 0);
        table.setModel(tableModel);

        setupListeners();
        loadSubconstructorDiscounts(""); // <-- pass empty string to load everything initially

        setVisible(true);
    }
    private void setupListeners() {
        addButton.addActionListener(e -> openDiscountDialog(false));
        updateButton.addActionListener(e -> openDiscountDialog(true));
        deleteButton.addActionListener(e -> deleteDiscount());
        searchButton.addActionListener(e -> loadSubconstructorDiscounts(searchField.getText().trim()));

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    openDiscountDialog(true);
                }
            }
        });
    }

    private void loadSubconstructorDiscounts(String searchQuery) {
        try {
            tableModel.setRowCount(0);

            String query = "SELECT d.DiscountID, s.CompanyName, s.CompanyAddress, s.Industry, " +
                    "s.CompanyPurchaseDate, d.DiscountValue, d.Reason " +
                    "FROM Discount d JOIN Subconstructor s ON d.SubconstructorID = s.SubconstructorID " +
                    "WHERE d.SubconstructorID IS NOT NULL";

            if (!searchQuery.isEmpty()) {
                query += " AND s.CompanyName LIKE ?";
            }

            PreparedStatement ps = conn.prepareStatement(query);

            if (!searchQuery.isEmpty()) {
                ps.setString(1, "%" + searchQuery + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("DiscountID"),
                        rs.getString("CompanyName"),
                        rs.getString("CompanyAddress"),
                        rs.getString("Industry"),
                        rs.getDate("CompanyPurchaseDate"),
                        String.format("%.2f", rs.getDouble("DiscountValue")),
                        rs.getString("Reason")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading subcontractor discounts: " + e.getMessage());
        }
    }

    private void openDiscountDialog(boolean isUpdate) {
        int discountId = -1;
        JTextField discountValueField = new JTextField();
        JTextField reasonField = new JTextField();
        JTextField companyNameField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));

        if (!isUpdate) {
            panel.add(new JLabel("Company Name:"));
            panel.add(companyNameField);
        }

        panel.add(new JLabel("Discount Value ($):"));
        panel.add(discountValueField);
        panel.add(new JLabel("Reason:"));
        panel.add(reasonField);

        if (isUpdate) {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a discount to update.");
                return;
            }
            discountId = (int) tableModel.getValueAt(row, 0);
            discountValueField.setText((String) tableModel.getValueAt(row, 5));
            reasonField.setText((String) tableModel.getValueAt(row, 6));
        }

        int result = JOptionPane.showConfirmDialog(this, panel,
                isUpdate ? "Update Discount" : "Add Discount", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            if (isUpdate) {
                updateDiscount(discountId, discountValueField.getText(), reasonField.getText());
            } else {
                addDiscountByCompanyName(companyNameField.getText().trim(), discountValueField.getText(), reasonField.getText());
            }
        }
    }

    private void addDiscountByCompanyName(String companyName, String discountValueStr, String reason) {
        try {
            int subId = fetchSubconstructorIdByName(companyName);

            if (subId == -1) {
                JOptionPane.showMessageDialog(this, "Subconstructor not found. Please check the company name.");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Discount (ClientID, SubconstructorID, DiscountValue, Reason) VALUES (NULL, ?, ?, ?)"
            );
            ps.setInt(1, subId);
            ps.setDouble(2, Double.parseDouble(discountValueStr));
            ps.setString(3, reason);
            ps.executeUpdate();

            loadSubconstructorDiscounts(searchField.getText().trim());
            JOptionPane.showMessageDialog(this, "Discount added successfully!");
        } catch (SQLException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error adding discount: " + e.getMessage());
        }
    }

    private void updateDiscount(int discountId, String discountValueStr, String reason) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Discount SET DiscountValue=?, Reason=? WHERE DiscountID=?"
            );
            ps.setDouble(1, Double.parseDouble(discountValueStr));
            ps.setString(2, reason);
            ps.setInt(3, discountId);
            ps.executeUpdate();

            loadSubconstructorDiscounts(searchField.getText().trim());
            JOptionPane.showMessageDialog(this, "Discount updated successfully!");
        } catch (SQLException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error updating discount: " + e.getMessage());
        }
    }

    private int fetchSubconstructorIdByName(String companyName) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT SubconstructorID FROM Subconstructor WHERE CompanyName = ?"
            );
            ps.setString(1, companyName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("SubconstructorID");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching subconstructor ID: " + e.getMessage());
        }
        return -1;
    }

    private void deleteDiscount() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a discount to delete.");
            return;
        }

        int discountId = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this discount?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM Discount WHERE DiscountID=?");
                ps.setInt(1, discountId);
                ps.executeUpdate();
                loadSubconstructorDiscounts(searchField.getText().trim());
                JOptionPane.showMessageDialog(this, "Discount deleted successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting discount: " + e.getMessage());
            }
        }
    }
}
