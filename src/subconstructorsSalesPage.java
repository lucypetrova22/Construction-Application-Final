import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;

public class subconstructorsSalesPage extends JFrame {
    private final Connection conn;
    private JPanel panel1;
    private JTable salesTable;
    private JTextField searchField;
    private JComboBox<String> yearFilter;
    private JButton backButton;
    private JButton exportButton;
    private JButton addSaleButton;
    private JButton viewProfileButton;
    private JButton updateSaleButton;
    private JButton viewInsightsButton;
    private DefaultTableModel tableModel;

    public subconstructorsSalesPage(Connection connection) {
        this.conn = connection;

        setTitle("Subconstructor Sales");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(panel1); // << very important: load the panel1 from the GUI form

        // Setup table model for salesTable
        tableModel = new DefaultTableModel(new String[]{
                "Sale ID", "Product", "Subconstructor", "Price", "Quantity", "Total", "Sale Date"
        }, 0);
        salesTable.setModel(tableModel);

        setupListeners();
        loadSubconstructorSales();

        setVisible(true);
    }

    private void setupListeners() {
        backButton.addActionListener(e -> {
            dispose();
            new salespage(conn);
        });
        exportButton.addActionListener(e -> exportToCSV());
        addSaleButton.addActionListener(e -> addSaleFlow());
        viewProfileButton.addActionListener(e -> viewSubconstructorProfile());
        updateSaleButton.addActionListener(e -> updateSale());
        viewInsightsButton.addActionListener(e -> viewInsights());

        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                loadSubconstructorSales();
            }
        });
        yearFilter.addActionListener(e -> loadSubconstructorSales());
    }

    private void loadSubconstructorSales() {
        try {
            tableModel.setRowCount(0);
            String keyword = searchField.getText().trim().toLowerCase();
            String selectedYear = (String) yearFilter.getSelectedItem();
            LocalDate today = LocalDate.now();

            StringBuilder query = new StringBuilder(
                    "SELECT s.SaleID, p.ProductName, sc.CompanyName, " +
                            "s.ProductPrice, s.QuantitySold, (s.ProductPrice * s.QuantitySold) AS Total, s.SaleDate " +
                            "FROM Sales s JOIN Products p ON s.ProductID = p.ProductID " +
                            "JOIN Subconstructor sc ON s.SubconstructorID = sc.SubconstructorID WHERE s.ClientID IS NULL "
            );

            if (!keyword.isEmpty()) query.append("AND LOWER(sc.CompanyName) LIKE ? ");
            if (!"All".equals(selectedYear)) query.append("AND YEAR(s.SaleDate) = ? ");

            query.append("ORDER BY s.SaleDate DESC");

            PreparedStatement ps = conn.prepareStatement(query.toString());
            int paramIndex = 1;

            if (!keyword.isEmpty()) ps.setString(paramIndex++, "%" + keyword + "%");
            if (!"All".equals(selectedYear)) {
                int year = "This Year".equals(selectedYear) ? today.getYear() : Integer.parseInt(selectedYear);
                ps.setInt(paramIndex++, year);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("SaleID"),
                        rs.getString("ProductName"),
                        rs.getString("CompanyName"),
                        rs.getDouble("ProductPrice"),
                        rs.getInt("QuantitySold"),
                        rs.getDouble("Total"),
                        rs.getDate("SaleDate")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading sales: " + e.getMessage());
        }
    }

    private void exportToCSV() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                FileWriter writer = new FileWriter(fileChooser.getSelectedFile() + ".csv");
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.append(tableModel.getColumnName(i)).append(",");
                }
                writer.append("\n");
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        writer.append(tableModel.getValueAt(i, j).toString()).append(",");
                    }
                    writer.append("\n");
                }
                writer.flush();
                writer.close();
                JOptionPane.showMessageDialog(this, "Export successful!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
        }
    }

    private void addSaleFlow() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Is the Subconstructor already existing?", "Add Sale",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            addSaleToExistingSubconstructor();
        } else if (choice == JOptionPane.NO_OPTION) {
            addNewSubconstructorThenSale();
        }
    }

    private void addSaleToExistingSubconstructor() {
        try {
            String companyName = JOptionPane.showInputDialog(this, "Enter Subconstructor Company Name:");
            if (companyName == null || companyName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Company name cannot be empty!");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT SubconstructorID FROM Subconstructor WHERE CompanyName = ?");
            ps.setString(1, companyName.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int subId = rs.getInt("SubconstructorID");
                addSale(subId);
            } else {
                JOptionPane.showMessageDialog(this, "Subconstructor not found!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching subconstructor: " + e.getMessage());
        }
    }

    private void addNewSubconstructorThenSale() {
        JTextField companyNameField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField industryField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Company Name:"));
        panel.add(companyNameField);
        panel.add(new JLabel("Company Address:"));
        panel.add(addressField);
        panel.add(new JLabel("Industry:"));
        panel.add(industryField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Subconstructor", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Subconstructor (CompanyName, CompanyAddress, Industry, CompanyPurchaseDate, CompanyImage) " +
                                "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, companyNameField.getText().trim());
                ps.setString(2, addressField.getText().trim());
                ps.setString(3, industryField.getText().trim());
                ps.setDate(4, Date.valueOf(LocalDate.now()));
                ps.setNull(5, Types.BLOB);

                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int subId = rs.getInt(1);
                    JOptionPane.showMessageDialog(this, "Subconstructor added successfully!");
                    addSale(subId);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding subconstructor: " + e.getMessage());
            }
        }
    }

    private void addSale(int subconstructorId) {
        JTextField productIdField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Product ID:"));
        panel.add(productIdField);
        panel.add(new JLabel("Product Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Quantity Sold:"));
        panel.add(quantityField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Sale", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Sales (ProductID, ClientID, SubconstructorID, ProductPrice, QuantitySold, SaleDate) " +
                                "VALUES (?, NULL, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, Integer.parseInt(productIdField.getText().trim()));
                ps.setInt(2, subconstructorId);
                ps.setDouble(3, Double.parseDouble(priceField.getText().trim()));
                ps.setInt(4, Integer.parseInt(quantityField.getText().trim()));
                ps.setDate(5, Date.valueOf(LocalDate.now()));
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int saleId = rs.getInt(1);

                    double productPrice = Double.parseDouble(priceField.getText().trim());
                    int quantity = Integer.parseInt(quantityField.getText().trim());
                    double totalRevenue = productPrice * quantity;

                    PreparedStatement revenuePs = conn.prepareStatement(
                            "INSERT INTO Revenue (SaleID, RevenueAmount, RevenueDate, Adjustment, FinalRevenue) " +
                                    "VALUES (?, ?, ?, ?, ?)");
                    revenuePs.setInt(1, saleId);
                    revenuePs.setDouble(2, totalRevenue);
                    revenuePs.setDate(3, Date.valueOf(LocalDate.now()));
                    revenuePs.setDouble(4, 0.00);
                    revenuePs.setDouble(5, totalRevenue);
                    revenuePs.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Sale added successfully!");
                loadSubconstructorSales();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding sale: " + e.getMessage());
            }
        }
    }

    private void viewSubconstructorProfile() {
        int row = salesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a subconstructor first.");
            return;
        }

        String companyName = (String) tableModel.getValueAt(row, 2);

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT CompanyName, CompanyAddress, Industry, CompanyPurchaseDate, CompanyImage " +
                            "FROM Subconstructor WHERE CompanyName = ?");
            ps.setString(1, companyName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String address = rs.getString("CompanyAddress");
                String industry = rs.getString("Industry");
                Date purchaseDate = rs.getDate("CompanyPurchaseDate");
                InputStream imageStream = rs.getBinaryStream("CompanyImage");

                JPanel profilePanel = new JPanel(new BorderLayout(10, 10));
                JTextArea infoArea = new JTextArea(
                        "Company Name: " + companyName + "\n" +
                                "Address: " + address + "\n" +
                                "Industry: " + industry + "\n" +
                                "First Purchase: " + purchaseDate);
                infoArea.setEditable(false);
                infoArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
                profilePanel.add(infoArea, BorderLayout.CENTER);

                if (imageStream != null) {
                    Image image = ImageIO.read(imageStream);
                    ImageIcon icon = new ImageIcon(image.getScaledInstance(200, 200, Image.SCALE_SMOOTH));
                    JLabel imageLabel = new JLabel(icon);
                    profilePanel.add(imageLabel, BorderLayout.EAST);
                } else {
                    profilePanel.add(new JLabel("No profile image available."), BorderLayout.EAST);
                }

                JOptionPane.showMessageDialog(this, profilePanel, "Subconstructor Profile", JOptionPane.PLAIN_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Subconstructor not found.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading profile: " + e.getMessage());
        }
    }

    private void updateSale() {
        int row = salesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a sale first.");
            return;
        }

        int saleId = (int) tableModel.getValueAt(row, 0);
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("New Price:"));
        panel.add(priceField);
        panel.add(new JLabel("New Quantity:"));
        panel.add(quantityField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Update Sale", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double newPrice = Double.parseDouble(priceField.getText().trim());
                int newQuantity = Integer.parseInt(quantityField.getText().trim());
                double newTotal = newPrice * newQuantity;

                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Sales SET ProductPrice = ?, QuantitySold = ? WHERE SaleID = ?");
                ps.setDouble(1, newPrice);
                ps.setInt(2, newQuantity);
                ps.setInt(3, saleId);
                ps.executeUpdate();

                // Only update RevenueAmount
                PreparedStatement revenuePs = conn.prepareStatement(
                        "UPDATE Revenue SET RevenueAmount = ? WHERE SaleID = ?");
                revenuePs.setDouble(1, newTotal);
                revenuePs.setInt(2, saleId);
                revenuePs.executeUpdate();

                JOptionPane.showMessageDialog(this, "Sale and revenue updated successfully!");
                loadSubconstructorSales();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error updating sale: " + e.getMessage());
            }
        }
    }


    private void viewInsights() {
        try {
            String selectedYear = (String) yearFilter.getSelectedItem();
            LocalDate today = LocalDate.now();
            String query = "SELECT SUM(ProductPrice * QuantitySold) AS TotalSales FROM Sales WHERE ClientID IS NULL ";

            if (!"All".equals(selectedYear)) {
                int year = "This Year".equals(selectedYear) ? today.getYear() : Integer.parseInt(selectedYear);
                query += "AND YEAR(SaleDate) = " + year;
            }

            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double totalSales = rs.getDouble("TotalSales");
                JOptionPane.showMessageDialog(this, "Total Sales: $" + totalSales);
            } else {
                JOptionPane.showMessageDialog(this, "No sales data available.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error calculating insights: " + e.getMessage());
        }
    }
}
