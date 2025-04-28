import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class clientdiscountspage extends JFrame {
    private final Connection conn;
    private JPanel panel1;
    private JTable table;
    private JTextField searchField;
    private JButton searchButton;
    private JButton addDiscount;
    private JButton updateButton;
    private JButton deleteButton;

    private DefaultTableModel tableModel;

    public clientdiscountspage(Connection conn) {
        this.conn = conn;
        setTitle("Client Discounts");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(panel1); // GUI manages layout

        setupTable();
        setupListeners();
        loadClientDiscounts("");

        setVisible(true);
    }

    private void setupTable() {
        // ✅ Create only the model manually and attach it to the JTable from GUI
        tableModel = new DefaultTableModel(new Object[]{
                "Discount ID", "Client Name", "Contact Info", "Address", "Purchase Date", "Discount Value ($)", "Reason"
        }, 0);
        table.setModel(tableModel);
    }

    private void setupListeners() {
        addDiscount.addActionListener(e -> openDiscountDialog(false));
        updateButton.addActionListener(e -> openDiscountDialog(true));
        deleteButton.addActionListener(e -> deleteDiscount());
        searchButton.addActionListener(e -> loadClientDiscounts(searchField.getText().trim()));

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    openDiscountDialog(true);
                }
            }
        });
    }

    private void loadClientDiscounts(String searchQuery) {
        try {
            tableModel.setRowCount(0);

            String query = "SELECT d.DiscountID, c.ClientFullName, c.ClientContactInfo, c.ClientAddress, " +
                    "c.ClientPurchaseDate, d.DiscountValue, d.Reason " +
                    "FROM Discount d " +
                    "JOIN Clients c ON d.ClientID = c.ClientID " +
                    "WHERE d.ClientID IS NOT NULL ";

            boolean hasSearch = searchQuery != null && !searchQuery.isEmpty();
            if (hasSearch) {
                query += "AND c.ClientFullName LIKE ? ";
            }
            query += "ORDER BY c.ClientFullName ASC";

            PreparedStatement ps = conn.prepareStatement(query);
            if (hasSearch) {
                ps.setString(1, "%" + searchQuery + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("DiscountID"),
                        rs.getString("ClientFullName"),
                        rs.getString("ClientContactInfo"),
                        rs.getString("ClientAddress"),
                        rs.getDate("ClientPurchaseDate"),
                        String.format("%.2f", rs.getDouble("DiscountValue")),
                        rs.getString("Reason")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading client discounts: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDiscountDialog(boolean isUpdate) {
        int discountId = -1;
        JTextField discountValueField = new JTextField();
        JTextField reasonField = new JTextField();
        JTextField clientNameField = new JTextField();

        JPanel panel = new JPanel(new java.awt.GridLayout(0, 1));

        if (!isUpdate) {
            panel.add(new JLabel("Client Full Name:"));
            panel.add(clientNameField);
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
            discountValueField.setText(tableModel.getValueAt(row, 5).toString());
            reasonField.setText((String) tableModel.getValueAt(row, 6));
        }

        int result = JOptionPane.showConfirmDialog(this, panel,
                isUpdate ? "Update Discount" : "Add Discount", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            if (isUpdate) {
                updateDiscount(discountId, discountValueField.getText(), reasonField.getText());
            } else {
                addDiscountByClientName(clientNameField.getText().trim(), discountValueField.getText(), reasonField.getText());
            }
        }
    }

    private void addDiscountByClientName(String clientName, String discountValueStr, String reason) {
        try {
            int clientId = fetchClientIdByName(clientName);

            if (clientId == -1) {
                JOptionPane.showMessageDialog(this, "Client not found. Please check the full name.");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Discount (ClientID, DiscountValue, Reason) VALUES (?, ?, ?)"
            );
            ps.setInt(1, clientId);
            ps.setDouble(2, Double.parseDouble(discountValueStr));
            ps.setString(3, reason);
            ps.executeUpdate();

            loadClientDiscounts("");
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

            loadClientDiscounts("");
            JOptionPane.showMessageDialog(this, "Discount updated successfully!");

        } catch (SQLException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error updating discount: " + e.getMessage());
        }
    }

    private int fetchClientIdByName(String clientName) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT ClientID FROM Clients WHERE ClientFullName = ?"
            );
            ps.setString(1, clientName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("ClientID");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching client ID: " + e.getMessage());
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
                PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Discount WHERE DiscountID = ?"
                );
                ps.setInt(1, discountId);
                ps.executeUpdate();

                loadClientDiscounts("");
                JOptionPane.showMessageDialog(this, "Discount deleted successfully!");

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting discount: " + e.getMessage());
            }
        }
    }
}
