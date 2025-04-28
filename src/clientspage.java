import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class clientspage extends JFrame {
    private JPanel panel1;
    private JPanel mainPanel;
    private JTable clientTable;
    private JTextField searchField;
    private JButton addButton;
    private JButton updateButton;
    private JButton removeButton;
    private JButton topClientsButton;
    private JButton viewClientButton;
    private JComboBox<String> topClientDropdown;
    private JComboBox<String> sortComboBox;

    private DefaultTableModel tableModel;
    private final Connection conn;

    public clientspage(Connection connection) {
        this.conn = connection;

        setTitle("Clients Management");
        setContentPane(panel1);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();

        tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Contact", "Address", "Purchase Date"}, 0);
        clientTable.setModel(tableModel);

        searchField.addCaretListener(e -> filterClients());
        addButton.addActionListener(e -> addClient());
        updateButton.addActionListener(e -> updateClient());
        removeButton.addActionListener(e -> removeClient());
        topClientsButton.addActionListener(e -> showTopClients());
        sortComboBox.addActionListener(this::sortClients);
        viewClientButton.addActionListener(e -> openClientProfile());

        clientTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && clientTable.getSelectedRow() != -1) {
                    openClientProfile();
                }
            }
        });

        loadClients();
        setVisible(true);
    }

    private void loadClients() {
        filterClients();
    }

    private void filterClients() {
        String prefix = searchField.getText();
        String orderBy = "ClientFullName ASC";
        String selectedSort = (String) sortComboBox.getSelectedItem();
        if (selectedSort != null) {
            switch (selectedSort) {
                case "Name A-Z" -> orderBy = "ClientFullName ASC";
                case "Name Z-A" -> orderBy = "ClientFullName DESC";
                case "Latest Purchase" -> orderBy = "ClientPurchaseDate DESC";
                case "Oldest Purchase" -> orderBy = "ClientPurchaseDate ASC";
            }
        }

        try {
            tableModel.setRowCount(0);
            String sql = "SELECT * FROM Clients WHERE ClientFullName LIKE ? ORDER BY " + orderBy;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, prefix + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("ClientID"),
                        rs.getString("ClientFullName"),
                        rs.getString("ClientContactInfo"),
                        rs.getString("ClientAddress"),
                        rs.getDate("ClientPurchaseDate")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading clients: " + e.getMessage());
        }
    }

    private void sortClients(ActionEvent e) {
        filterClients();
    }

    private void addClient() {
        JTextField name = new JTextField();
        JTextField contact = new JTextField();
        JTextField address = new JTextField();
        JTextField purchaseDate = new JTextField("YYYY-MM-DD");

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Full Name:")); panel.add(name);
        panel.add(new JLabel("Contact Info:")); panel.add(contact);
        panel.add(new JLabel("Address:")); panel.add(address);
        panel.add(new JLabel("Purchase Date:")); panel.add(purchaseDate);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Client", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                conn.setAutoCommit(false);

                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Clients (ClientFullName, ClientContactInfo, ClientAddress, ClientPurchaseDate) VALUES (?, ?, ?, ?)"
                );
                ps.setString(1, name.getText());
                ps.setString(2, contact.getText());
                ps.setString(3, address.getText());
                ps.setDate(4, Date.valueOf(purchaseDate.getText()));
                ps.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this, "Client added successfully!");
                loadClients();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Error adding client: " + e.getMessage());
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void updateClient() {
        int row = clientTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a client to update.");
            return;
        }

        int clientId = (int) tableModel.getValueAt(row, 0);
        JTextField name = new JTextField((String) tableModel.getValueAt(row, 1));
        JTextField contact = new JTextField((String) tableModel.getValueAt(row, 2));
        JTextField address = new JTextField((String) tableModel.getValueAt(row, 3));
        JTextField purchaseDate = new JTextField(tableModel.getValueAt(row, 4).toString());

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Full Name:")); panel.add(name);
        panel.add(new JLabel("Contact Info:")); panel.add(contact);
        panel.add(new JLabel("Address:")); panel.add(address);
        panel.add(new JLabel("Purchase Date:")); panel.add(purchaseDate);

        int result = JOptionPane.showConfirmDialog(this, panel, "Update Client", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                conn.setAutoCommit(false);

                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Clients SET ClientFullName=?, ClientContactInfo=?, ClientAddress=?, ClientPurchaseDate=? WHERE ClientID=?"
                );
                ps.setString(1, name.getText());
                ps.setString(2, contact.getText());
                ps.setString(3, address.getText());
                ps.setDate(4, Date.valueOf(purchaseDate.getText()));
                ps.setInt(5, clientId);
                ps.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this, "Client updated successfully!");
                loadClients();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Error updating client: " + e.getMessage());
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void removeClient() {
        int row = clientTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a client to remove.");
            return;
        }

        int clientId = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this client?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                conn.setAutoCommit(false);

                PreparedStatement ps = conn.prepareStatement("DELETE FROM Clients WHERE ClientID = ?");
                ps.setInt(1, clientId);
                ps.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this, "Client removed successfully!");
                loadClients();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Error removing client: " + e.getMessage());
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void showTopClients() {
        int limit = switch ((String) topClientDropdown.getSelectedItem()) {
            case "Top 5" -> 5;
            case "Top 10" -> 10;
            default -> 3;
        };

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.ClientFullName, SUM(s.ProductPrice * s.QuantitySold) AS TotalSales " +
                            "FROM Clients c JOIN Sales s ON c.ClientID = s.ClientID " +
                            "GROUP BY c.ClientID " +
                            "ORDER BY TotalSales DESC " +
                            "LIMIT ?"
            );
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("Top Clients by Sales:\n");
            while (rs.next()) {
                sb.append(rs.getString("ClientFullName")).append(" - $")
                        .append(String.format("%.2f", rs.getDouble("TotalSales"))).append("\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading top clients: " + e.getMessage());
        }
    }

    private void openClientProfile() {
        int selectedRow = clientTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a client to view their profile.");
            return;
        }
        String clientName = (String) tableModel.getValueAt(selectedRow, 1);
        new ClientProfileViewer(conn, clientName);
    }
}
