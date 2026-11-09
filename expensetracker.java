
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ExpenseTracker extends JFrame {

    // Update PASSWORD with your MySQL root password.
    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/expensedb?serverTimezone=Asia/Kolkata";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Afrinsyed@630";

    Color blue = new Color(45, 105, 220);
    Color background = new Color(240, 244, 250);

    ArrayList<String> categories = new ArrayList<>(
        Arrays.asList("Food", "Travel", "Shopping", "Education")
    );

    ArrayList<Expense> expenses = new ArrayList<>();

    double budget = 10000;

    JLabel totalLabel = new JLabel();
    JLabel monthlyLabel = new JLabel();
    JLabel budgetLabel = new JLabel();
    JLabel remainingLabel = new JLabel();

    DefaultTableModel tableModel;
    JTable expenseTable;

    DefaultListModel<String> categoryModel =
        new DefaultListModel<>();

    JList<String> categoryList;

    JTextField descriptionField = new JTextField(12);
    JTextField amountField = new JTextField(8);
    JComboBox<String> categoryBox = new JComboBox<>();

    JTextField budgetField = new JTextField("10000", 10);
    JTextArea reportArea = new JTextArea();

    public ExpenseTracker() {

        // Prepare the database and load saved information before showing the UI.
        try {
            initializeDatabase();
            loadDataFromDatabase();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not connect to MySQL. Check that MySQL Server is running, "
                + "the database exists, and DB_PASSWORD is correct.\n"
                + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            System.exit(1);
            return;
        }

        setTitle("Personal Expense Tracker");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(background);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard", createDashboard());
        tabs.addTab("Expenses", createExpensesPanel());
        tabs.addTab("Categories", createCategoriesPanel());
        tabs.addTab("Reports", createReportsPanel());

        add(tabs);

        refreshAll();

        setVisible(true);
    }

    // EXPENSE MODEL

    class Expense {
        int id;
        String description;
        String category;
        double amount;
        LocalDate date;

        Expense(int id, String description, String category,
                double amount, LocalDate date) {
            this.id = id;
            this.description = description;
            this.category = category;
            this.amount = amount;
            this.date = date;
        }
    }

    // MYSQL DATABASE METHODS

    Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    void initializeDatabase() throws SQLException {
        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS categories (" +
                "category_name VARCHAR(100) PRIMARY KEY)"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS expenses (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "description VARCHAR(255) NOT NULL, " +
                "category VARCHAR(100) NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "expense_date DATE NOT NULL)"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS app_settings (" +
                "setting_key VARCHAR(50) PRIMARY KEY, " +
                "setting_value VARCHAR(100) NOT NULL)"
            );

            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM categories")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO categories(category_name) VALUES (?)")) {
                        for (String category :
                                Arrays.asList("Food", "Travel", "Shopping", "Education")) {
                            ps.setString(1, category);
                            ps.executeUpdate();
                        }
                    }
                }
            }

            st.executeUpdate(
                "INSERT IGNORE INTO app_settings(setting_key, setting_value) " +
                "VALUES ('monthly_budget', '10000')"
            );
        }
    }

    void loadDataFromDatabase() throws SQLException {
        categories.clear();
        expenses.clear();

        try (Connection con = getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT category_name FROM categories ORDER BY category_name");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(rs.getString("category_name"));
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id, description, category, amount, expense_date " +
                    "FROM expenses ORDER BY expense_date DESC, id DESC");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expenses.add(new Expense(
                        rs.getInt("id"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getDouble("amount"),
                        rs.getDate("expense_date").toLocalDate()
                    ));
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT setting_value FROM app_settings " +
                    "WHERE setting_key = 'monthly_budget'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    budget = Double.parseDouble(rs.getString(1));
                }
            }
        }
        budgetField.setText(String.valueOf(budget));
    }

    int insertExpense(String description, String category,
                      double amount, LocalDate date) throws SQLException {
        String sql = "INSERT INTO expenses " +
            "(description, category, amount, expense_date) VALUES (?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                 sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, description);
            ps.setString(2, category);
            ps.setDouble(3, amount);
            ps.setDate(4, java.sql.Date.valueOf(date));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("MySQL did not return the new expense ID.");
    }

    void updateExpenseInDatabase(Expense expense) throws SQLException {
        String sql = "UPDATE expenses SET description=?, category=?, " +
            "amount=?, expense_date=? WHERE id=?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, expense.description);
            ps.setString(2, expense.category);
            ps.setDouble(3, expense.amount);
            ps.setDate(4, java.sql.Date.valueOf(expense.date));
            ps.setInt(5, expense.id);
            ps.executeUpdate();
        }
    }

    void deleteExpenseFromDatabase(int id) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM expenses WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    void saveBudgetToDatabase(double newBudget) throws SQLException {
        String sql = "INSERT INTO app_settings(setting_key, setting_value) " +
            "VALUES ('monthly_budget', ?) " +
            "ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(newBudget));
            ps.executeUpdate();
        }
    }

    // DASHBOARD

    JPanel createDashboard() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("Expense Dashboard");
        heading.setFont(new Font("Arial", Font.BOLD, 26));

        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 15));
        cards.setBackground(background);

        cards.add(createCard("Total Expenses", totalLabel));
        cards.add(createCard("This Month", monthlyLabel));
        cards.add(createCard("Total Budget", budgetLabel));
        cards.add(createCard("Remaining", remainingLabel));

        JPanel top = new JPanel(new BorderLayout(10, 15));
        top.setBackground(background);
        top.add(heading, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);

        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBackground(Color.WHITE);
        chartContainer.setBorder(
            BorderFactory.createTitledBorder("Category-wise Expenses")
        );

        chartContainer.add(new PieChart(), BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(chartContainer, BorderLayout.CENTER);

        return panel;
    }

    JPanel createCard(String title, JLabel value) {

        JPanel card = new JPanel(new BorderLayout(5, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 235)),
                BorderFactory.createEmptyBorder(20, 15, 20, 15)
            )
        );

        JLabel name = new JLabel(title);
        name.setFont(new Font("Arial", Font.BOLD, 14));

        value.setFont(new Font("Arial", Font.BOLD, 22));
        value.setForeground(blue);

        card.add(name, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    // EXPENSES PANEL

    JPanel createExpensesPanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));

        form.add(new JLabel("Description:"));
        form.add(descriptionField);

        form.add(new JLabel("Amount:"));
        form.add(amountField);

        form.add(new JLabel("Category:"));
        form.add(categoryBox);

        JButton addButton = new JButton("Add Expense");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");

        addButton.addActionListener(e -> addExpense());
        editButton.addActionListener(e -> editExpense());
        deleteButton.addActionListener(e -> deleteExpense());

        form.add(addButton);
        form.add(editButton);
        form.add(deleteButton);

        tableModel = new DefaultTableModel(
            new String[]{"Description", "Category", "Amount", "Date"}, 0
        ) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        expenseTable = new JTable(tableModel);
        expenseTable.setRowHeight(28);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(expenseTable), BorderLayout.CENTER);

        return panel;
    }

    void addExpense() {

        try {
            String description = descriptionField.getText().trim();
            String amountText = amountField.getText().trim();

            if (description.isEmpty() || amountText.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please enter description and amount.");
                return;
            }

            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this,
                    "Amount must be greater than zero.");
                return;
            }

            String category = (String) categoryBox.getSelectedItem();

            if (category == null) {
                JOptionPane.showMessageDialog(this,
                    "Please add a category first.");
                return;
            }

            int newId = insertExpense(
                description, category, amount, LocalDate.now()
            );
            expenses.add(new Expense(
                newId, description, category, amount, LocalDate.now()
            ));

            descriptionField.setText("");
            amountField.setText("");

            refreshAll();

            JOptionPane.showMessageDialog(this,
                "Expense added successfully!");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid amount.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    void editExpense() {

        int row = expenseTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an expense.");
            return;
        }

        row = expenseTable.convertRowIndexToModel(row);
        Expense expense = expenses.get(row);

        String description = JOptionPane.showInputDialog(
            this, "Enter description:", expense.description
        );

        if (description == null) return;

        description = description.trim();

        if (description.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Description cannot be empty.");
            return;
        }

        String amountText = JOptionPane.showInputDialog(
            this, "Enter amount:", expense.amount
        );

        if (amountText == null) return;

        try {
            double amount = Double.parseDouble(amountText.trim());

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this,
                    "Amount must be greater than zero.");
                return;
            }

            String[] categoryOptions =
                categories.toArray(new String[0]);

            String category = (String) JOptionPane.showInputDialog(
                this,
                "Select category:",
                "Edit Category",
                JOptionPane.QUESTION_MESSAGE,
                null,
                categoryOptions,
                expense.category
            );

            if (category == null) return;

            Expense updated = new Expense(
                expense.id, description, category, amount, expense.date
            );
            updateExpenseInDatabase(updated);

            expense.description = description;
            expense.amount = amount;
            expense.category = category;

            refreshAll();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid amount.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    void deleteExpense() {

        int row = expenseTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an expense.");
            return;
        }

        row = expenseTable.convertRowIndexToModel(row);
        Expense selectedExpense = expenses.get(row);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this expense?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                deleteExpenseFromDatabase(selectedExpense.id);
                expenses.remove(row);
                refreshAll();
            } catch (SQLException ex) {
                showDatabaseError(ex);
            }
        }
    }

    // CATEGORIES PANEL

    JPanel createCategoriesPanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("Manage Categories");
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        categoryList = new JList<>(categoryModel);
        categoryList.setFont(new Font("Arial", Font.PLAIN, 17));

        JButton addButton = new JButton("Add Category");
        JButton deleteButton = new JButton("Delete Category");

        addButton.addActionListener(e -> addCategory());
        deleteButton.addActionListener(e -> deleteCategory());

        JPanel buttons = new JPanel();
        buttons.add(addButton);
        buttons.add(deleteButton);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    void addCategory() {

        String input = JOptionPane.showInputDialog(
            this, "Enter category name:"
        );

        if (input == null) return;

        final String name = input.trim();

        boolean exists = false;

        for (String c : categories) {
            if (c.equalsIgnoreCase(name)) {
                exists = true;
                break;
            }
        }

        if (!name.isEmpty() && !exists) {
            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO categories(category_name) VALUES (?)")) {
                ps.setString(1, name);
                ps.executeUpdate();
                categories.add(name);
                refreshAll();

                JOptionPane.showMessageDialog(this,
                    "Category added successfully!");
            } catch (SQLException ex) {
                showDatabaseError(ex);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Enter a valid, unique category.");
        }
    }

    void deleteCategory() {

        String selected = categoryList.getSelectedValue();

        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a category.");
            return;
        }

        for (Expense expense : expenses) {
            if (expense.category.equals(selected)) {
                JOptionPane.showMessageDialog(this,
                    "This category is used by an expense.");
                return;
            }
        }

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM categories WHERE category_name=?")) {
            ps.setString(1, selected);
            ps.executeUpdate();
            categories.remove(selected);
            refreshAll();
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    // REPORTS PANEL

    JPanel createReportsPanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controls.add(new JLabel("Set Monthly Budget:"));
        controls.add(budgetField);

        JButton setBudget = new JButton("Update Budget");
        JButton lowButton = new JButton("Low to High");
        JButton highButton = new JButton("High to Low");

        setBudget.addActionListener(e -> updateBudget());
        lowButton.addActionListener(e -> sortExpenses(true));
        highButton.addActionListener(e -> sortExpenses(false));

        controls.add(setBudget);
        controls.add(lowButton);
        controls.add(highButton);

        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        return panel;
    }

    void updateBudget() {

        try {
            double newBudget =
                Double.parseDouble(budgetField.getText().trim());

            if (newBudget <= 0) {
                JOptionPane.showMessageDialog(this,
                    "Budget must be greater than zero.");
                return;
            }

            saveBudgetToDatabase(newBudget);
            budget = newBudget;
            refreshAll();

            JOptionPane.showMessageDialog(this,
                "Budget updated successfully!");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Enter a valid budget.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    void sortExpenses(boolean ascending) {

        if (ascending) {
            expenses.sort(
                Comparator.comparingDouble(e -> e.amount)
            );
        } else {
            expenses.sort(
                (a, b) -> Double.compare(b.amount, a.amount)
            );
        }

        refreshAll();
    }

    void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
            "Database operation failed:\n" + ex.getMessage(),
            "MySQL Error", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }

    // REFRESH ALL DATA

    void refreshAll() {

        double total = 0;
        double monthly = 0;

        YearMonth currentMonth = YearMonth.now();

        for (Expense expense : expenses) {

            total += expense.amount;

            if (YearMonth.from(expense.date).equals(currentMonth)) {
                monthly += expense.amount;
            }
        }

        totalLabel.setText(String.format("Rs. %.2f", total));
        monthlyLabel.setText(String.format("Rs. %.2f", monthly));
        budgetLabel.setText(String.format("Rs. %.2f", budget));

        double remaining = budget - monthly;

        remainingLabel.setText(
            String.format("Rs. %.2f", remaining)
        );

        remainingLabel.setForeground(
            remaining < 0 ? Color.RED : new Color(30, 150, 80)
        );

        refreshExpenseTable();
        refreshCategories();
        refreshCategoryBox();
        refreshReport();

        repaint();
    }

    void refreshExpenseTable() {

        if (tableModel == null) return;

        tableModel.setRowCount(0);

        for (Expense expense : expenses) {

            tableModel.addRow(new Object[]{
                expense.description,
                expense.category,
                String.format("Rs. %.2f", expense.amount),
                expense.date
            });
        }
    }

    void refreshCategories() {

        if (categoryModel == null) return;

        categoryModel.clear();

        for (String category : categories) {
            categoryModel.addElement(category);
        }
    }

    void refreshCategoryBox() {

        if (categoryBox == null) return;

        String selected = (String) categoryBox.getSelectedItem();

        categoryBox.removeAllItems();

        for (String category : categories) {
            categoryBox.addItem(category);
        }

        if (selected != null) {
            categoryBox.setSelectedItem(selected);
        }
    }

    void refreshReport() {
        double total = 0, monthly = 0;
        StringBuilder report = new StringBuilder("PERSONAL EXPENSE REPORT\n============================\n\n");

        for (Expense e : expenses) {
            total += e.amount;
            if (YearMonth.from(e.date).equals(YearMonth.now())) monthly += e.amount;
        }

        report.append(String.format(
            "Total Expenses : Rs. %.2f\nThis Month : Rs. %.2f\nMonthly Budget : Rs. %.2f\nRemaining : Rs. %.2f\n\n",
            total, monthly, budget, budget - monthly));
        report.append("EXPENSE DETAILS\n----------------------------\n");

        for (Expense e : expenses)
            report.append(String.format("%s | %s | Rs. %.2f | %s\n",
                e.description, e.category, e.amount, e.date));

        reportArea.setText(report.toString());
    }

    // CATEGORY PIE CHART

    class PieChart extends JPanel {
        Color[] colors = {Color.BLUE, Color.GREEN, Color.ORANGE,
                          Color.MAGENTA, Color.RED, Color.CYAN};

        PieChart() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(500, 350));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Map<String, Double> data = new LinkedHashMap<>();

            for (Expense e : expenses)
                data.put(e.category, data.getOrDefault(e.category, 0.0) + e.amount);

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total == 0) {
                g.drawString("No expenses available", 60, 140);
                return;
            }

            int angle = 0, i = 0;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                int part = (int) (e.getValue() / total * 360);
                g.setColor(colors[i % colors.length]);
                g.fillArc(40, 40, 230, 230, angle, part);
                g.fillRect(300, 50 + i * 30, 15, 15);
                g.setColor(Color.BLACK);
                g.drawString(e.getKey() + " - Rs. " + String.format("%.2f", e.getValue()),
                             325, 63 + i * 30);
                angle += part;
                i++;
            }
        }
    }

    // MAIN METHOD

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new ExpenseTracker();
        });
    }
}
