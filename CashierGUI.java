import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class CashierGUI extends JFrame {
    private String folderPath = ""; 
    private final String FILE_NAME = "sales_log.csv";
    private final String EXPENSE_FILE = "expenses_log.csv";
    private final String CONFIG_FILE = "config.properties";
    private String configFileName; 
    
    private ArrayList<CartItem> cart = new ArrayList<>();
    
    private JTextArea display;
    private JLabel totalLabel;
    private JLabel pathLabel; 
    private JLabel titleLabel; 
    private JPanel actionSidebar; 
    private JPanel rightCartPanel; 
    private JPanel productPanel;
    private JScrollPane productScroll;
    private JScrollPane displayScroll; 
    private JPanel topBar;
    
    private boolean isDarkMode = true;
    private boolean adminHideRp = false; 
    private boolean isMenuExpanded = false; 
    private String lang = "EN";

    private String storeName = "MYBEANS CASHIER"; 
    private ArrayList<String> buyersList = new ArrayList<>(); 

    private final Color DARK_BG = new Color(30, 30, 40); 
    private final Color DARK_SIDE = new Color(42, 42, 53); 
    private final Color DARK_TEXT = new Color(235, 235, 245);
    
    private final Color LIGHT_BG = new Color(245, 247, 250); 
    private final Color LIGHT_SIDE = new Color(255, 255, 255); 
    private final Color LIGHT_TEXT = new Color(40, 40, 45); 

    private Color accentColor = new Color(188, 158, 130); 
    
    private final Color SUCCESS_COLOR = new Color(67, 160, 71);
    private final Color DANGER_COLOR = new Color(211, 47, 47);
    private final Color INFO_COLOR = new Color(30, 136, 229);

    private ArrayList<String> items = new ArrayList<>();
    private ArrayList<Integer> prices = new ArrayList<>();
    private ArrayList<Integer> costs = new ArrayList<>();

    public CashierGUI(String configFile) {
        this.configFileName = configFile;
        loadConfig();

        setTitle(storeName);
        setSize(1200, 750); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        setupTopBar();
        setupActionSidebar();  
        setupCartArea();       
        setupProductArea();    
        setupStatusBar();      

        applyTheme();
        initializeFiles();

        if (isMenuExpanded) {
            isMenuExpanded = false; 
            toggleLayout();
        } else {
            rightCartPanel.setPreferredSize(new Dimension(550, 0));
            productPanel.setLayout(new GridLayout(0, 1, 10, 10));
        }
    }

    private String t(String en, String id) {
        return lang.equals("ID") ? id : en;
    }

    private void loadConfig() {
        Properties prop = new Properties();
        File configFile = new File(configFileName);
        
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                prop.load(fis);
                folderPath = prop.getProperty("saveDir", "");
                storeName = prop.getProperty("StoreName", "MYBEANS CASHIER");
                isMenuExpanded = Boolean.parseBoolean(prop.getProperty("MenuExpanded", "false"));
                isDarkMode = Boolean.parseBoolean(prop.getProperty("DarkMode", "true"));
                lang = prop.getProperty("Language", "EN");
                
                if (prop.containsKey("AccentColor")) {
                    accentColor = new Color(Integer.parseInt(prop.getProperty("AccentColor")));
                }
                
                String b = prop.getProperty("Buyers", "");
                if (!b.isEmpty()) buyersList.addAll(Arrays.asList(b.split(",")));
                
                if (prop.containsKey("ItemCount")) {
                    int count = Integer.parseInt(prop.getProperty("ItemCount"));
                    for (int i = 0; i < count; i++) {
                        items.add(prop.getProperty("ItemName_" + i));
                        prices.add(Integer.parseInt(prop.getProperty("ItemPrice_" + i)));
                        costs.add(Integer.parseInt(prop.getProperty("ItemCost_" + i)));
                    }
                }
            } catch (Exception e) { }
        }

        if (folderPath.isEmpty()) {
            promptForFolder();
        }
    }

    private void promptForFolder() {
        JOptionPane.showMessageDialog(null, 
            "Configuring Profile: " + configFileName.replace(".properties", "") + "\n\nPlease select a folder to save this profile's CSV data.", 
            "Profile Setup", 
            JOptionPane.INFORMATION_MESSAGE);
            
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Data Output Folder for " + configFileName);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            folderPath = fc.getSelectedFile().getAbsolutePath() + File.separator;
            saveConfig();
            updatePathLabel();
        } else if (folderPath.isEmpty()) {
            folderPath = ""; 
            updatePathLabel(); 
        }
    }

    private void saveConfig() {
        Properties prop = new Properties();
        prop.setProperty("saveDir", folderPath);
        prop.setProperty("StoreName", storeName);
        prop.setProperty("MenuExpanded", String.valueOf(isMenuExpanded));
        prop.setProperty("DarkMode", String.valueOf(isDarkMode));
        prop.setProperty("Language", lang);
        prop.setProperty("AccentColor", String.valueOf(accentColor.getRGB()));
        prop.setProperty("Buyers", String.join(",", buyersList));
        
        prop.setProperty("ItemCount", String.valueOf(items.size()));
        for (int i = 0; i < items.size(); i++) {
            prop.setProperty("ItemName_" + i, items.get(i));
            prop.setProperty("ItemPrice_" + i, String.valueOf(prices.get(i)));
            prop.setProperty("ItemCost_" + i, String.valueOf(costs.get(i)));
        }
        
        try (FileOutputStream fos = new FileOutputStream(configFileName)) {
            prop.store(fos, storeName + " Configuration Profile");
        } catch (Exception e) { }
    }

    private void refreshWholeUI() {
        getContentPane().removeAll();
        setupTopBar();
        setupActionSidebar();
        setupCartArea();
        setupProductArea();
        setupStatusBar();
        applyTheme();
        
        if (isMenuExpanded) {
            isMenuExpanded = false;
            toggleLayout();
        } else {
            rightCartPanel.setPreferredSize(new Dimension(550, 0));
            productPanel.setLayout(new GridLayout(0, 1, 10, 10));
        }
        
        revalidate();
        repaint();
    }

    private void setupTopBar() {
        topBar = new JPanel(new BorderLayout());
        topBar.setBorder(new EmptyBorder(15, 20, 5, 20));

        titleLabel = new JLabel(storeName.toUpperCase());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(accentColor);
        
        topBar.add(titleLabel, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);
    }

    private void setupActionSidebar() {
        actionSidebar = new JPanel(new GridLayout(6, 1, 10, 10)); 
        actionSidebar.setBorder(new EmptyBorder(10, 15, 10, 5));
        actionSidebar.setPreferredSize(new Dimension(180, 0));
        actionSidebar.setOpaque(false);

        JButton btnClear = createStyledButton(t("Clear Cart", "Kosongkan"), DANGER_COLOR);
        btnClear.addActionListener(e -> { cart.clear(); refreshUI(); });

        JButton btnAdmin = createStyledButton(t("Admin Panel", "Panel Admin"), new Color(251, 140, 0));
        btnAdmin.addActionListener(e -> openAdminPanel());

        JButton btnReport = createStyledButton(t("Sales Report", "Laporan Penjualan"), INFO_COLOR);
        btnReport.addActionListener(e -> openSalesReport());

        JButton btnToggleLayout = createStyledButton(t("Toggle Layout", "Ubah Tampilan"), new Color(80, 80, 80));
        btnToggleLayout.addActionListener(e -> toggleLayout());

        JButton btnTheme = createStyledButton(t("Toggle Theme", "Ubah Tema"), Color.DARK_GRAY);
        btnTheme.addActionListener(e -> { 
            isDarkMode = !isDarkMode; 
            saveConfig();
            applyTheme(); 
        });

        JButton btnAccent = createStyledButton(t("Edit Accent", "Ubah Warna"), new Color(100, 100, 100));
        btnAccent.addActionListener(e -> {
            Color newColor = CircularColorPicker.showDialog(this, accentColor);
            if (newColor != null) {
                accentColor = newColor;
                saveConfig();
                applyTheme();
            }
        });

        actionSidebar.add(btnClear);
        actionSidebar.add(btnAdmin);
        actionSidebar.add(btnReport);
        actionSidebar.add(btnToggleLayout);
        actionSidebar.add(btnTheme);
        actionSidebar.add(btnAccent);

        add(actionSidebar, BorderLayout.WEST);
    }

    private void setupCartArea() {
        rightCartPanel = new JPanel(new BorderLayout(0, 10));
        rightCartPanel.setBorder(new EmptyBorder(10, 5, 10, 20));
        rightCartPanel.setOpaque(false);

        display = new JTextArea();
        display.setFont(new Font("Consolas", Font.PLAIN, 16)); 
        display.setEditable(false);
        display.setMargin(new Insets(10, 10, 10, 10));

        displayScroll = new JScrollPane(display);
        displayScroll.setBorder(new LineBorder(accentColor, 1));

        JPanel checkoutContainer = new JPanel(new BorderLayout(0, 10));
        checkoutContainer.setOpaque(false);
        checkoutContainer.setBorder(new EmptyBorder(10, 0, 0, 0));

        totalLabel = new JLabel("Total: Rp 0", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 36)); 
        totalLabel.setForeground(accentColor); 

        JButton btnCheckout = createStyledButton(t("Checkout", "Bayar"), SUCCESS_COLOR);
        btnCheckout.setPreferredSize(new Dimension(0, 70)); 
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 22)); 
        btnCheckout.addActionListener(e -> checkout());

        checkoutContainer.add(totalLabel, BorderLayout.NORTH);
        checkoutContainer.add(btnCheckout, BorderLayout.CENTER);

        rightCartPanel.add(displayScroll, BorderLayout.CENTER);
        rightCartPanel.add(checkoutContainer, BorderLayout.SOUTH);
        
        add(rightCartPanel, BorderLayout.EAST);
    }

    private void setupProductArea() {
        productPanel = new JPanel(); 
        productPanel.setOpaque(false);
        
        JPanel productWrapper = new JPanel(new BorderLayout());
        productWrapper.setOpaque(false);
        productWrapper.add(productPanel, BorderLayout.NORTH);

        productScroll = new JScrollPane(productWrapper);
        productScroll.setBorder(new EmptyBorder(0, 5, 0, 5));
        productScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        add(productScroll, BorderLayout.CENTER);
        rebuildMenuPanel();
    }

    private void toggleLayout() {
        isMenuExpanded = !isMenuExpanded;

        if (isMenuExpanded) {
            rightCartPanel.setPreferredSize(new Dimension(340, 0));
            productPanel.setLayout(new GridLayout(0, 5, 10, 10)); 
        } else {
            rightCartPanel.setPreferredSize(new Dimension(550, 0));
            productPanel.setLayout(new GridLayout(0, 1, 10, 10)); 
        }
        
        rebuildMenuPanel(); 
        saveConfig();
        revalidate();
        repaint();
    }

    private void rebuildMenuPanel() {
        productPanel.removeAll();

        Color cardBg = isDarkMode ? DARK_SIDE : LIGHT_SIDE;
        Color textColor = isDarkMode ? DARK_TEXT : LIGHT_TEXT;
        Color subTextColor = isDarkMode ? new Color(170, 170, 180) : new Color(100, 100, 110);

        for (int i = 0; i < items.size(); i++) {
            final int idx = i;
            
            JPanel itemCard = new JPanel(new BorderLayout(10, 5));
            itemCard.setBackground(cardBg);
            itemCard.setBorder(new CompoundBorder(
                new LineBorder(accentColor, 1, true),
                new EmptyBorder(10, 10, 10, 10)
            ));

            JLabel nameLbl = new JLabel(items.get(i));
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
            nameLbl.setForeground(textColor);
            
            JLabel priceLbl = new JLabel("Rp " + formatRp(prices.get(i)));
            priceLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            priceLbl.setForeground(subTextColor);
            
            JPanel textPanel = new JPanel(new GridLayout(2, 1));
            textPanel.setOpaque(false);
            textPanel.add(nameLbl);
            textPanel.add(priceLbl);

            JButton btnMinus = new JButton("-");
            btnMinus.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnMinus.setMargin(new Insets(4, 6, 4, 6));
            btnMinus.setFocusPainted(false);
            
            JLabel lblQty = new JLabel("0", SwingConstants.CENTER);
            lblQty.setPreferredSize(new Dimension(25, 25));
            lblQty.setForeground(textColor);
            lblQty.setFont(new Font("Segoe UI", Font.BOLD, 16));
            
            JButton btnPlus = new JButton("+");
            btnPlus.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnPlus.setMargin(new Insets(4, 6, 4, 6));
            btnPlus.setFocusPainted(false);

            JButton btnAdd = new JButton();
            btnAdd.setBackground(accentColor);
            btnAdd.setForeground(Color.WHITE);
            btnAdd.setFocusPainted(false);
            btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnMinus.addActionListener(e -> {
                int q = Integer.parseInt(lblQty.getText());
                if (q > 0) lblQty.setText(String.valueOf(q - 1));
            });
            btnPlus.addActionListener(e -> {
                int q = Integer.parseInt(lblQty.getText());
                lblQty.setText(String.valueOf(q + 1));
            });
            
            btnAdd.addActionListener(e -> {
                int q = Integer.parseInt(lblQty.getText());
                if (q == 0) return; 

                boolean found = false;
                for (CartItem ci : cart) {
                    if (ci.name.equals(items.get(idx))) {
                        ci.qty += q;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    cart.add(new CartItem(items.get(idx), q, prices.get(idx), costs.get(idx)));
                }
                lblQty.setText("0"); 
                refreshUI();
            });

            if (isMenuExpanded) {
                nameLbl.setHorizontalAlignment(SwingConstants.CENTER);
                priceLbl.setHorizontalAlignment(SwingConstants.CENTER);
                
                btnAdd.setText(t("Add", "Tambah"));
                btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnAdd.setPreferredSize(null);
                
                JPanel actionContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
                actionContainer.setOpaque(false);
                actionContainer.add(btnMinus);
                actionContainer.add(lblQty);
                actionContainer.add(btnPlus);
                actionContainer.add(btnAdd);
                
                itemCard.add(textPanel, BorderLayout.CENTER);
                itemCard.add(actionContainer, BorderLayout.SOUTH);
            } else {
                nameLbl.setHorizontalAlignment(SwingConstants.LEFT);
                priceLbl.setHorizontalAlignment(SwingConstants.LEFT);
                
                btnAdd.setText("+");
                btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 22));
                btnAdd.setPreferredSize(new Dimension(65, 0)); 
                
                JPanel actionContainer = new JPanel(new BorderLayout(10, 0));
                actionContainer.setOpaque(false);

                JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 12)); 
                qtyPanel.setOpaque(false);
                qtyPanel.add(btnMinus);
                qtyPanel.add(lblQty);
                qtyPanel.add(btnPlus);
                
                actionContainer.add(qtyPanel, BorderLayout.CENTER);
                actionContainer.add(btnAdd, BorderLayout.EAST);
                
                itemCard.add(textPanel, BorderLayout.CENTER);
                itemCard.add(actionContainer, BorderLayout.EAST);
            }

            productPanel.add(itemCard);
        }
        productPanel.revalidate();
        productPanel.repaint();
    }

    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setOpaque(false); 
        statusBar.setBorder(new EmptyBorder(0, 10, 5, 10));
        
        pathLabel = new JLabel();
        pathLabel.setFont(new Font("Consolas", Font.ITALIC, 13));
        updatePathLabel();
        
        statusBar.add(pathLabel);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void updatePathLabel() {
        if (pathLabel != null) {
            String displayPath = folderPath.isEmpty() ? new File("").getAbsolutePath() + File.separator : folderPath;
            pathLabel.setText(t("Database: ", "Basis Data: ") + displayPath + t("  |  Profile: ", "  |  Profil: ") + configFileName);
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void applyTheme() {
        Color bgColor = isDarkMode ? DARK_BG : LIGHT_BG;
        Color sideColor = isDarkMode ? DARK_SIDE : LIGHT_SIDE;
        Color textColor = isDarkMode ? DARK_TEXT : LIGHT_TEXT;

        getContentPane().setBackground(bgColor);
        topBar.setBackground(bgColor);
        
        productScroll.getViewport().setBackground(bgColor);
        productScroll.setBackground(bgColor);
        
        display.setBackground(sideColor);
        display.setForeground(textColor);
        
        titleLabel.setForeground(accentColor);
        totalLabel.setForeground(accentColor);
        displayScroll.setBorder(new LineBorder(accentColor, 1));

        if (pathLabel != null) {
            pathLabel.setForeground(isDarkMode ? new Color(150, 150, 160) : new Color(100, 100, 110));
        }

        rebuildMenuPanel(); 
        SwingUtilities.updateComponentTreeUI(this);
    }

    private String formatRp(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    private void refreshUI() {
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (CartItem item : cart) {
            sb.append(String.format("%-18s x%d Rp %s\n", item.name, item.qty, formatRp(item.getPrice())));
            total += item.getPrice();
        }
        display.setText(sb.toString());
        totalLabel.setText("Total: Rp " + formatRp(total));
    }

    private void checkout() {
        if (cart.isEmpty()) return;
        
        String customerName = JOptionPane.showInputDialog(this, t("Enter Customer Name:", "Masukkan Nama Pelanggan:"), t("Checkout", "Bayar"), JOptionPane.PLAIN_MESSAGE);
        if (customerName == null) return; 
        customerName = customerName.trim().isEmpty() ? t("Guest", "Tamu") : customerName.replace(",", " "); 

        String date = LocalDate.now().toString();
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        int totalToPrint = 0;
        ArrayList<CartItem> printedCart = new ArrayList<>(cart);

        try (FileWriter fw = new FileWriter(folderPath + FILE_NAME, true)) {
            for (CartItem item : cart) {
                fw.write(date + "," + time + "," + item.name + "," + item.qty + "," + 
                         item.getPrice() + "," + customerName + "," + item.getCost() + "\n");
                totalToPrint += item.getPrice();
            }
            cart.clear(); 
            refreshUI();
            
            int printOpt = JOptionPane.showConfirmDialog(this, t("Checkout Success! Print Receipt?", "Pembayaran Sukses! Cetak Struk?"), t("Print", "Cetak"), JOptionPane.YES_NO_OPTION);
            if (printOpt == JOptionPane.YES_OPTION) {
                printReceipt(customerName, date, time, totalToPrint, printedCart);
            }
            
        } catch (Exception ex) { 
            JOptionPane.showMessageDialog(this, t("Error: ", "Kesalahan: ") + ex.getMessage()); 
        }
    }

    private void printReceipt(String cust, String date, String time, int total, ArrayList<CartItem> pCart) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(new ReceiptPrintable(cust, date, time, total, pCart));
        if (pj.printDialog()) {
            try {
                pj.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, t("Print Error: ", "Gagal Cetak: ") + e.getMessage());
            }
        }
    }

    private class ReceiptPrintable implements Printable {
        String cust, date, time;
        int total;
        ArrayList<CartItem> pCart;

        public ReceiptPrintable(String c, String d, String t, int tot, ArrayList<CartItem> cartData) {
            this.cust = c;
            this.date = d;
            this.time = t;
            this.total = tot;
            this.pCart = cartData;
        }

        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
            if (pageIndex > 0) return NO_SUCH_PAGE;

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            Font font = new Font("Monospaced", Font.PLAIN, 10);
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics(font);
            int y = 20;

            String centeredStore = String.format("%1$20s", storeName);
            g2d.drawString(centeredStore, 10, y);
            y += metrics.getHeight() * 2;

            g2d.drawString(t("Date: ", "Tanggal: ") + date, 10, y);
            y += metrics.getHeight();
            g2d.drawString(t("Time: ", "Waktu: ") + time, 10, y);
            y += metrics.getHeight();
            g2d.drawString(t("Cust: ", "Pelanggan: ") + cust, 10, y);
            y += metrics.getHeight();
            
            g2d.drawString("--------------------------", 10, y);
            y += metrics.getHeight();

            for (CartItem item : pCart) {
                String line1 = item.qty + "x " + item.name;
                g2d.drawString(line1, 10, y);
                y += metrics.getHeight();
                String line2 = String.format("%26s", "Rp " + formatRp(item.getPrice()));
                g2d.drawString(line2, 10, y);
                y += metrics.getHeight();
            }

            g2d.drawString("--------------------------", 10, y);
            y += metrics.getHeight();

            g2d.setFont(new Font("Monospaced", Font.BOLD, 10));
            String totalStr = "TOTAL: Rp " + formatRp(total);
            g2d.drawString(totalStr, 10, y);
            y += metrics.getHeight() * 2;

            g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2d.drawString(t("      Thank You!      ", "     Terima Kasih!    "), 10, y);

            return PAGE_EXISTS;
        }
    }

    private void openSalesReport() {
        JDialog reportDialog = new JDialog(this, t("Sales Report", "Laporan Penjualan"), true);
        reportDialog.setSize(950, 600);
        reportDialog.setLayout(new BorderLayout(10, 10));
        reportDialog.setLocationRelativeTo(this);

        Color bgColor = isDarkMode ? DARK_BG : LIGHT_BG;
        Color sideColor = isDarkMode ? DARK_SIDE : LIGHT_SIDE;
        Color textColor = isDarkMode ? DARK_TEXT : LIGHT_TEXT;
        reportDialog.getContentPane().setBackground(bgColor);

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        JComboBox<String> dateSelector = new JComboBox<>();
        dateSelector.setBackground(sideColor);
        dateSelector.setForeground(textColor);
        dateSelector.addItem(t("Select a Filter...", "Pilih Filter..."));
        dateSelector.addItem(t("All Time", "Semua Waktu"));
        dateSelector.addItem(t("Last 7 Days", "7 Hari Terakhir"));
        dateSelector.addItem(t("Last 30 Days", "30 Hari Terakhir"));

        JCheckBox profitToggle = new JCheckBox(t("Show Profit", "Tampilkan Untung"));
        profitToggle.setOpaque(false);
        profitToggle.setForeground(textColor);

        String[] columns = {t("Date", "Tanggal"), t("Time", "Waktu"), t("Item", "Barang"), t("Qty", "Jml"), t("Amount Paid", "Total Bayar"), t("Customer", "Pelanggan"), t("Unit Cost", "Harga Modal")};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        JTable table = new JTable(tableModel);
        table.setBackground(sideColor);
        table.setForeground(textColor);
        table.getTableHeader().setBackground(sideColor.darker());
        table.getTableHeader().setForeground(textColor);
        
        JTextArea summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        summaryArea.setBackground(sideColor);
        summaryArea.setForeground(textColor);

        Set<String> uniqueDates = new TreeSet<>(Collections.reverseOrder());
        try (BufferedReader br = new BufferedReader(new FileReader(folderPath + FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] d = line.split(",");
                if (d.length > 0 && !d[0].equalsIgnoreCase("Date") && !d[0].equalsIgnoreCase("Tanggal")) {
                    uniqueDates.add(d[0]);
                }
            }
            for (String date : uniqueDates) dateSelector.addItem(date);
        } catch (Exception e) { }

        Runnable loadTableData = () -> {
            String selected = (String) dateSelector.getSelectedItem();
            if (selected == null || selected.equals(t("Select a Filter...", "Pilih Filter..."))) return;

            tableModel.setRowCount(0);
            LocalDate today = LocalDate.now();
            int cups = 0, totalGross = 0, totalNet = 0;
            Map<String, Integer> itemsSold = new HashMap<>();

            try (BufferedReader br = new BufferedReader(new FileReader(folderPath + FILE_NAME))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue; 
                    
                    String[] d = line.split(",");
                    if (d.length < 7) continue; 
                    
                    String dateStr = d[0].trim();
                    if (dateStr.equalsIgnoreCase("Date") || dateStr.equalsIgnoreCase("Tanggal")) continue; 

                    try {
                        LocalDate rowDate = LocalDate.parse(dateStr);
                        boolean match = false;

                        if (selected.equals(t("All Time", "Semua Waktu"))) { match = true; } 
                        else if (selected.equals(t("Last 7 Days", "7 Hari Terakhir"))) { match = !rowDate.isBefore(today.minusDays(7)); } 
                        else if (selected.equals(t("Last 30 Days", "30 Hari Terakhir"))) { match = !rowDate.isBefore(today.minusDays(30)); } 
                        else { match = dateStr.equals(selected); }

                        if (!match) continue;

                        tableModel.addRow(new Object[]{
                            d[0].trim(), d[1].trim(), d[2].trim(), d[3].trim(), 
                            d[4].trim(), d[5].trim(), d[6].trim()
                        });
                        
                        int qty = Integer.parseInt(d[3].trim()); 
                        int amountPaid = Integer.parseInt(d[4].trim()); 
                        int cogs = Integer.parseInt(d[6].trim()); 
                        int profit = amountPaid - cogs;

                        cups += qty;
                        totalGross += amountPaid;
                        totalNet += profit;
                        
                        String itemName = d[2].trim();
                        itemsSold.put(itemName, itemsSold.getOrDefault(itemName, 0) + qty);

                    } catch (Exception parseError) { }
                }

                StringBuilder sb = new StringBuilder(t("SUMMARY: ", "RINGKASAN: ") + selected + "\n\n");
                sb.append(t("Total Items Sold: ", "Total Barang Terjual: ")).append(cups).append("\n\n");
                sb.append(t("PRODUCT BREAKDOWN\n", "RINCIAN PRODUK\n"));
                itemsSold.forEach((name, count) -> sb.append(name).append(": ").append(count).append(t(" sold\n", " terjual\n")));
                
                if (profitToggle.isSelected()) {
                    sb.append(t("\nFINANCIAL DATA\n", "\nDATA KEUANGAN\n"));
                    sb.append(t("Gross Revenue: Rp ", "Pendapatan Kotor: Rp ")).append(formatRp(totalGross)).append("\n");
                    sb.append(t("Net Profit:    Rp ", "Laba Bersih:      Rp ")).append(formatRp(totalNet)).append("\n");
                }

                summaryArea.setText(sb.toString());
            } catch (Exception ex) { }
        };

        dateSelector.addActionListener(e -> loadTableData.run());
        profitToggle.addActionListener(e -> loadTableData.run());

        JButton btnReprint = new JButton(t("Print Receipt", "Cetak Struk"));
        btnReprint.setBackground(SUCCESS_COLOR);
        btnReprint.setForeground(Color.WHITE);
        btnReprint.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                String targetDate = tableModel.getValueAt(selectedRow, 0).toString();
                String targetTime = tableModel.getValueAt(selectedRow, 1).toString();
                String targetCust = tableModel.getValueAt(selectedRow, 5).toString();

                ArrayList<CartItem> pCart = new ArrayList<>();
                int totalToPrint = 0;

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    String rDate = tableModel.getValueAt(i, 0).toString();
                    String rTime = tableModel.getValueAt(i, 1).toString();
                    String rCust = tableModel.getValueAt(i, 5).toString();

                    if (rDate.equals(targetDate) && rTime.equals(targetTime) && rCust.equals(targetCust)) {
                        String name = tableModel.getValueAt(i, 2).toString();
                        int q = Integer.parseInt(tableModel.getValueAt(i, 3).toString());
                        int tPaid = Integer.parseInt(tableModel.getValueAt(i, 4).toString());
                        int tCost = Integer.parseInt(tableModel.getValueAt(i, 6).toString());
                        
                        int uPrice = q > 0 ? tPaid / q : 0;
                        int uCost = q > 0 ? tCost / q : 0;

                        pCart.add(new CartItem(name, q, uPrice, uCost));
                        totalToPrint += tPaid;
                    }
                }
                printReceipt(targetCust, targetDate, targetTime, totalToPrint, pCart);
            } else {
                JOptionPane.showMessageDialog(reportDialog, t("Please select a row first.", "Pilih baris terlebih dahulu."));
            }
        });

        JButton btnEdit = new JButton(t("Edit Selected", "Ubah Pilihan"));
        btnEdit.setBackground(INFO_COLOR);
        btnEdit.setForeground(Color.WHITE);
        btnEdit.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                editSalesRecord(reportDialog, tableModel, selectedRow, loadTableData);
            } else { JOptionPane.showMessageDialog(reportDialog, t("Please select a row first.", "Pilih baris terlebih dahulu.")); }
        });

        JButton btnDelete = new JButton(t("Delete Selected", "Hapus Pilihan"));
        btnDelete.setBackground(DANGER_COLOR);
        btnDelete.setForeground(Color.WHITE);
        btnDelete.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                if (JOptionPane.showConfirmDialog(reportDialog, t("Delete this transaction?", "Hapus transaksi ini?"), "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    deleteSalesRecord(tableModel, selectedRow);
                    loadTableData.run();
                }
            } else { JOptionPane.showMessageDialog(reportDialog, t("Please select a row first.", "Pilih baris terlebih dahulu.")); }
        });

        JLabel filterLbl = new JLabel(t("Filter: ", "Penyaring: "));
        filterLbl.setForeground(textColor);
        topPanel.add(filterLbl);
        topPanel.add(dateSelector);
        topPanel.add(profitToggle);
        reportDialog.add(topPanel, BorderLayout.NORTH);
        
        JScrollPane tblScroll = new JScrollPane(table);
        tblScroll.getViewport().setBackground(sideColor);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tblScroll, new JScrollPane(summaryArea));
        splitPane.setDividerLocation(320);
        reportDialog.add(splitPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(btnReprint);
        bottomPanel.add(btnEdit);
        bottomPanel.add(btnDelete);
        reportDialog.add(bottomPanel, BorderLayout.SOUTH);

        reportDialog.setVisible(true);
    }

    private void editSalesRecord(JDialog parent, DefaultTableModel model, int rowIndex, Runnable reloadData) {
        String oldDate = model.getValueAt(rowIndex, 0).toString();
        String oldTime = model.getValueAt(rowIndex, 1).toString();
        String oldItem = model.getValueAt(rowIndex, 2).toString();
        String oldQty = model.getValueAt(rowIndex, 3).toString();
        String oldPaid = model.getValueAt(rowIndex, 4).toString();
        String oldCust = model.getValueAt(rowIndex, 5).toString();
        String oldCOGS = model.getValueAt(rowIndex, 6).toString();

        String matchStr = String.join(",", oldDate, oldTime, oldItem, oldQty, oldPaid, oldCust, oldCOGS);

        JTextField dateField = new JTextField(oldDate);
        JTextField timeField = new JTextField(oldTime);
        JTextField itemField = new JTextField(oldItem);
        JTextField qtyField = new JTextField(oldQty);
        JTextField paidField = new JTextField(oldPaid);
        JTextField custField = new JTextField(oldCust);
        JTextField cogsField = new JTextField(oldCOGS);

        Object[] message = {
            t("Date (YYYY-MM-DD):", "Tanggal (YYYY-MM-DD):"), dateField,
            t("Time (HH:mm:ss):", "Waktu (HH:mm:ss):"), timeField,
            t("Item Name:", "Nama Barang:"), itemField,
            t("Quantity:", "Jumlah:"), qtyField,
            t("Amount Paid (Revenue):", "Total Bayar (Pendapatan):"), paidField,
            t("Customer / Day:", "Pelanggan / Hari:"), custField,
            t("Unit Cost (Rp):", "Harga Modal (Rp):"), cogsField
        };

        int option = JOptionPane.showConfirmDialog(parent, message, t("Edit Transaction", "Ubah Transaksi"), JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String newStr = String.join(",", 
                dateField.getText().trim(), timeField.getText().trim(), itemField.getText().trim(), 
                qtyField.getText().trim(), paidField.getText().trim(), custField.getText().trim(), cogsField.getText().trim()
            );

            List<String> lines = new ArrayList<>();
            boolean replaced = false;

            try (BufferedReader br = new BufferedReader(new FileReader(folderPath + FILE_NAME))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!replaced && line.equals(matchStr)) {
                        lines.add(newStr);
                        replaced = true; 
                    } else {
                        lines.add(line);
                    }
                }
            } catch (Exception e) { }

            try (FileWriter fw = new FileWriter(folderPath + FILE_NAME)) {
                for (String l : lines) fw.write(l + "\n");
            } catch (Exception e) { }

            reloadData.run();
        }
    }

    private void deleteSalesRecord(DefaultTableModel model, int rowIndex) {
        String matchStr = String.join(",", 
            model.getValueAt(rowIndex, 0).toString(), model.getValueAt(rowIndex, 1).toString(),
            model.getValueAt(rowIndex, 2).toString(), model.getValueAt(rowIndex, 3).toString(),
            model.getValueAt(rowIndex, 4).toString(), model.getValueAt(rowIndex, 5).toString(),
            model.getValueAt(rowIndex, 6).toString()
        );

        List<String> lines = new ArrayList<>();
        boolean deleted = false;

        try (BufferedReader br = new BufferedReader(new FileReader(folderPath + FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!deleted && line.equals(matchStr)) {
                    deleted = true; 
                    continue;
                }
                lines.add(line);
            }
        } catch (Exception e) { }

        try (FileWriter fw = new FileWriter(folderPath + FILE_NAME)) {
            for (String l : lines) fw.write(l + "\n");
        } catch (Exception e) { }
    }

    private void openAdminPanel() {
        JDialog adminDialog = new JDialog(this, t("Admin Panel", "Panel Admin"), true);
        adminDialog.setSize(1350, 750); 
        adminDialog.setLayout(new BorderLayout(10, 10));
        adminDialog.setLocationRelativeTo(this);

        Color bgColor = isDarkMode ? DARK_BG : LIGHT_BG;
        Color sideColor = isDarkMode ? DARK_SIDE : LIGHT_SIDE;
        Color textColor = isDarkMode ? DARK_TEXT : LIGHT_TEXT;
        adminDialog.getContentPane().setBackground(bgColor);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        headerPanel.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT); 
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); 
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel(t("Financial Dashboard", "Dasbor Keuangan"));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(accentColor);
        
        JPanel titleRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        titleRightPanel.setOpaque(false);

        JButton btnTogglePrivacy = new JButton(adminHideRp ? t("Show Rp", "Tampil Rp") : t("Hide Rp", "Sembunyi Rp"));
        btnTogglePrivacy.setBackground(Color.DARK_GRAY);
        btnTogglePrivacy.setForeground(Color.WHITE);
        btnTogglePrivacy.setFocusPainted(false);
        
        JButton btnLanguage = new JButton(t("Language: EN", "Bahasa: ID"));
        btnLanguage.setBackground(Color.DARK_GRAY);
        btnLanguage.setForeground(Color.WHITE);
        btnLanguage.setFocusPainted(false);
        btnLanguage.addActionListener(e -> {
            lang = lang.equals("EN") ? "ID" : "EN";
            saveConfig();
            adminDialog.dispose();
            refreshWholeUI();
            openAdminPanel();
        });

        titleRightPanel.add(btnTogglePrivacy);
        titleRightPanel.add(btnLanguage);

        titleRow.add(lblTitle, BorderLayout.WEST);
        titleRow.add(titleRightPanel, BorderLayout.EAST);

        JLabel financialLabel = new JLabel(t("Loading...", "Memuat..."));
        financialLabel.setAlignmentX(Component.LEFT_ALIGNMENT); 
        financialLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        financialLabel.setForeground(SUCCESS_COLOR); 
        financialLabel.setBorder(new EmptyBorder(10, 0, 5, 0));

        JLabel contributionLabel = new JLabel(t("Loading contributions...", "Memuat kontribusi..."));
        contributionLabel.setAlignmentX(Component.LEFT_ALIGNMENT); 
        contributionLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        contributionLabel.setForeground(isDarkMode ? new Color(170, 170, 180) : Color.GRAY);
        contributionLabel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JPanel productProfitsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        productProfitsContainer.setBackground(bgColor);
        
        JScrollPane ppScroll = new JScrollPane(productProfitsContainer);
        ppScroll.setAlignmentX(Component.LEFT_ALIGNMENT); 
        ppScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ppScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        ppScroll.setBorder(null);
        ppScroll.setPreferredSize(new Dimension(0, 80));
        ppScroll.getViewport().setBackground(bgColor);

        headerPanel.add(titleRow);
        headerPanel.add(financialLabel);
        headerPanel.add(contributionLabel);
        headerPanel.add(ppScroll);

        adminDialog.add(headerPanel, BorderLayout.NORTH);

        String[] columns = {"No", t("Item", "Barang"), t("Quantity", "Jumlah"), t("Desc", "Deskripsi"), t("Price", "Harga"), t("Buyer", "Pembeli")};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setBackground(sideColor);
        table.setForeground(textColor);
        table.getTableHeader().setBackground(sideColor.darker());
        table.getTableHeader().setForeground(textColor);
        
        JScrollPane tblScroll = new JScrollPane(table);
        tblScroll.getViewport().setBackground(sideColor);
        adminDialog.add(tblScroll, BorderLayout.CENTER);

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setOpaque(false);
        
        JPanel formPanel = new JPanel(new GridLayout(2, 6, 5, 5));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        formPanel.setOpaque(false);
        
        JTextField itemField = new JTextField(); JTextField qtyField = new JTextField();
        JTextField descField = new JTextField(); JTextField priceField = new JTextField();
        
        JComboBox<String> buyerCombo = new JComboBox<>();
        buyerCombo.addItem(t("- Select -", "- Pilih -"));
        for (String b : buyersList) {
            buyerCombo.addItem(b);
        }

        JLabel l1 = new JLabel(t("Item", "Barang")); l1.setForeground(textColor);
        JLabel l2 = new JLabel(t("Quantity", "Jumlah")); l2.setForeground(textColor);
        JLabel l3 = new JLabel(t("Desc", "Deskripsi")); l3.setForeground(textColor);
        JLabel l4 = new JLabel(t("Price (Rp)", "Harga (Rp)")); l4.setForeground(textColor);
        JLabel l5 = new JLabel(t("Buyer", "Pembeli")); l5.setForeground(textColor);
        JLabel l6 = new JLabel("");
        
        formPanel.add(l1); formPanel.add(l2); formPanel.add(l3); 
        formPanel.add(l4); formPanel.add(l5); formPanel.add(l6); 
        formPanel.add(itemField); formPanel.add(qtyField); formPanel.add(descField); 
        formPanel.add(priceField); formPanel.add(buyerCombo);

        JButton btnAdd = new JButton(t("Add Expense", "Tambah Pengeluaran"));
        formPanel.add(btnAdd);
        bottomWrapper.add(formPanel, BorderLayout.CENTER);

        JPanel adminActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        adminActions.setOpaque(false);
        
        JButton btnEditStore = new JButton(t("Edit Store Name", "Ubah Nama Toko"));
        JButton btnEditBuyers = new JButton(t("Edit Buyers", "Ubah Pembeli"));
        JButton btnAddMenu = new JButton(t("Add Menu Item", "Tambah Menu"));
        JButton btnDeleteMenu = new JButton(t("Delete Menu Item", "Hapus Menu"));
        btnDeleteMenu.setBackground(DANGER_COLOR); btnDeleteMenu.setForeground(Color.WHITE);
        JButton btnEditPrice = new JButton(t("Edit Price", "Ubah Harga"));
        JButton btnEditExpense = new JButton(t("Edit Expense", "Ubah Pengeluaran"));
        btnEditExpense.setBackground(INFO_COLOR); btnEditExpense.setForeground(Color.WHITE);
        JButton btnDeleteExpense = new JButton(t("Delete Expense", "Hapus Pengeluaran"));
        btnDeleteExpense.setBackground(DANGER_COLOR); btnDeleteExpense.setForeground(Color.WHITE);
        JButton btnSetFolder = new JButton(t("Change Folder", "Ubah Folder"));

        adminActions.add(btnEditStore);
        adminActions.add(btnEditBuyers);
        adminActions.add(btnAddMenu); 
        adminActions.add(btnDeleteMenu); 
        adminActions.add(btnEditPrice); 
        adminActions.add(btnDeleteExpense); 
        adminActions.add(btnEditExpense); 
        adminActions.add(btnSetFolder);

        bottomWrapper.add(adminActions, BorderLayout.SOUTH);
        adminDialog.add(bottomWrapper, BorderLayout.SOUTH);

        Runnable reloadData = () -> {
            tableModel.setRowCount(0);
            Map<String, Integer> contributions = new HashMap<>();
            Map<String, Integer> productProfits = new HashMap<>();
            Map<String, Integer> productQty = new HashMap<>(); 
            int totalPengeluaran = 0, totalOmset = 0, totalCOGS = 0;

            try (BufferedReader br = new BufferedReader(new FileReader(folderPath + EXPENSE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("No")) continue;
                    String[] d = line.split(",");
                    if (d.length >= 6) {
                        tableModel.addRow(d);
                        int harga = Integer.parseInt(d[4]);
                        totalPengeluaran += harga;
                        contributions.put(d[5], contributions.getOrDefault(d[5], 0) + harga);
                    }
                }
            } catch (Exception e) { }

            try (BufferedReader br = new BufferedReader(new FileReader(folderPath + FILE_NAME))) {
                String line; 
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] d = line.split(",");
                    if (d.length >= 7) { 
                        if (d[0].equalsIgnoreCase("Date") || d[0].equalsIgnoreCase("Tanggal")) continue; 
                        try {
                            int qty = Integer.parseInt(d[3].trim()); 
                            int paid = Integer.parseInt(d[4].trim()); 
                            int cogs = Integer.parseInt(d[6].trim()); 
                            int itemProfit = paid - cogs; 

                            totalOmset += paid;
                            totalCOGS += cogs;
                            
                            String itemName = d[2].trim();
                            productProfits.put(itemName, productProfits.getOrDefault(itemName, 0) + itemProfit);
                            productQty.put(itemName, productQty.getOrDefault(itemName, 0) + qty); 
                        } catch (Exception parseEx) { }
                    }
                }
            } catch (Exception e) { }

            String omsetStr = adminHideRp ? "$$$.$$$" : formatRp(totalOmset);
            String pengeluaranStr = adminHideRp ? "$$$.$$$" : formatRp(totalPengeluaran);
            String netProfitStr = adminHideRp ? "$$$.$$$" : formatRp(totalOmset - totalPengeluaran);
            String totalUntungStr = adminHideRp ? "$$$.$$$" : formatRp(totalOmset - totalCOGS);

            financialLabel.setText(String.format(t("Gross Revenue: Rp ", "Total Omset: Rp ") + "%s" + t("   |   Total Expense: Rp ", "   |   Total Pengeluaran: Rp ") + "%s" + t("   ||   Net Profit: Rp ", "   ||   Laba Bersih: Rp ") + "%s" + t("   |   Total Profit: Rp ", "   |   Total Untung: Rp ") + "%s", 
                omsetStr, pengeluaranStr, netProfitStr, totalUntungStr));

            StringBuilder cbText = new StringBuilder(t("Member Contributions: ", "Kontribusi Anggota: "));
            contributions.forEach((name, total) -> cbText.append(name).append(": Rp ").append(adminHideRp ? "$$$.$$$" : formatRp(total)).append("  |  "));
            contributionLabel.setText(cbText.length() > 25 ? cbText.toString() : t("No expenses logged yet.", "Belum ada pengeluaran dicatat."));

            productProfitsContainer.removeAll();
            productProfits.forEach((name, profit) -> {
                JPanel card = new JPanel(new BorderLayout(0, 5));
                card.setBackground(sideColor); 
                card.setBorder(new CompoundBorder(
                    new LineBorder(accentColor, 1, true),
                    new EmptyBorder(10, 20, 10, 20) 
                ));
                
                int qtySold = productQty.getOrDefault(name, 0);
                JLabel lblName = new JLabel(name + " (" + qtySold + t(" sold)", " terjual)"), SwingConstants.CENTER);
                lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
                lblName.setForeground(textColor); 
                
                String profitStr = adminHideRp ? "Rp $$$.$$$" : "Rp " + formatRp(profit);
                JLabel lblProfit = new JLabel(profitStr, SwingConstants.CENTER);
                lblProfit.setFont(new Font("Segoe UI", Font.BOLD, 13)); 
                lblProfit.setForeground(SUCCESS_COLOR); 
                
                card.add(lblName, BorderLayout.NORTH);
                card.add(lblProfit, BorderLayout.SOUTH);
                productProfitsContainer.add(card);
            });
            productProfitsContainer.revalidate();
            productProfitsContainer.repaint();
        };

        btnTogglePrivacy.addActionListener(e -> {
            adminHideRp = !adminHideRp;
            btnTogglePrivacy.setText(adminHideRp ? t("Show Rp", "Tampil Rp") : t("Hide Rp", "Sembunyi Rp"));
            reloadData.run();
        });

        btnAdd.addActionListener(e -> {
            if (buyerCombo.getSelectedIndex() <= 0) {
                JOptionPane.showMessageDialog(adminDialog, t("Please select a valid buyer.", "Pilih pembeli yang valid."), "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String item = itemField.getText().replace(",", " ");
                String qty = qtyField.getText().replace(",", " ");
                String desc = descField.getText().replace(",", " ");
                int price = Integer.parseInt(priceField.getText());
                String buyer = buyerCombo.getSelectedItem().toString();
                int no = tableModel.getRowCount() + 1;

                try (FileWriter fw = new FileWriter(folderPath + EXPENSE_FILE, true)) {
                    fw.write(no + "," + item + "," + qty + "," + desc + "," + price + "," + buyer + "\n");
                }
                
                itemField.setText(""); qtyField.setText(""); descField.setText(""); priceField.setText("");
                buyerCombo.setSelectedIndex(0);
                reloadData.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(adminDialog, t("Please ensure Price is a valid number.", "Pastikan Harga adalah angka yang valid."));
            }
        });

        btnEditStore.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(adminDialog, t("Enter New Store Name:", "Masukkan Nama Toko Baru:"), storeName);
            if (newName != null && !newName.trim().isEmpty()) {
                storeName = newName.trim();
                titleLabel.setText(storeName.toUpperCase());
                setTitle(storeName);
                saveConfig();
            }
        });

        btnEditBuyers.addActionListener(e -> openEditBuyersDialog(adminDialog));
        btnAddMenu.addActionListener(e -> openAddMenuDialog());
        btnEditPrice.addActionListener(e -> openEditPriceDialog());
        btnDeleteMenu.addActionListener(e -> openDeleteMenuDialog());
        
        btnSetFolder.addActionListener(e -> {
            promptForFolder();
            initializeFiles();
            JOptionPane.showMessageDialog(adminDialog, "Restart the Admin Panel to load data from the new folder.");
            adminDialog.dispose();
        });

        btnDeleteExpense.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                if (JOptionPane.showConfirmDialog(adminDialog, t("Delete this expense?", "Hapus pengeluaran ini?"), "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    tableModel.removeRow(selectedRow);
                    rewriteExpenseFile(tableModel);
                    reloadData.run();
                }
            } else { JOptionPane.showMessageDialog(adminDialog, t("Please select a row first.", "Pilih terlebih dahulu.")); }
        });

        btnEditExpense.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                editExpenseRecord(adminDialog, tableModel, selectedRow, reloadData);
            } else { JOptionPane.showMessageDialog(adminDialog, t("Please select a row first.", "Pilih terlebih dahulu.")); }
        });

        reloadData.run();
        adminDialog.setVisible(true);
    }

    private void openEditBuyersDialog(JDialog parent) {
        JTextArea textArea = new JTextArea(10, 20);
        textArea.setText(String.join("\n", buyersList));
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        Object[] msg = {
            t("Enter Buyers (Type ONE name per line):", "Masukkan Pembeli (Ketik SATU nama per baris):"), scrollPane
        };
        
        if (JOptionPane.showConfirmDialog(parent, msg, t("Edit Buyers", "Ubah Pembeli"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            buyersList.clear();
            for (String line : textArea.getText().split("\n")) {
                if (!line.trim().isEmpty()) {
                    buyersList.add(line.trim());
                }
            }
            saveConfig();
            JOptionPane.showMessageDialog(parent, t("Buyers updated! Close and reopen the Admin Panel to see changes.", "Pembeli diperbarui! Tutup dan buka kembali Panel Admin untuk melihat perubahan."));
        }
    }

    private void editExpenseRecord(JDialog parent, DefaultTableModel model, int rowIndex, Runnable reloadData) {
        String oldItem = model.getValueAt(rowIndex, 1).toString();
        String oldQty = model.getValueAt(rowIndex, 2).toString();
        String oldDesc = model.getValueAt(rowIndex, 3).toString();
        String oldHarga = model.getValueAt(rowIndex, 4).toString();
        String oldBuyer = model.getValueAt(rowIndex, 5).toString();

        JTextField itemField = new JTextField(oldItem);
        JTextField qtyField = new JTextField(oldQty);
        JTextField descField = new JTextField(oldDesc);
        JTextField hargaField = new JTextField(oldHarga);
        
        JComboBox<String> buyerCombo = new JComboBox<>();
        buyerCombo.addItem(t("- Select -", "- Pilih -"));
        for (String b : buyersList) buyerCombo.addItem(b);
        buyerCombo.setSelectedItem(oldBuyer);

        Object[] message = {
            t("Item:", "Barang:"), itemField,
            t("Quantity:", "Jumlah:"), qtyField,
            t("Desc:", "Deskripsi:"), descField,
            t("Price (Rp):", "Harga (Rp):"), hargaField,
            t("Buyer:", "Pembeli:"), buyerCombo
        };

        if (JOptionPane.showConfirmDialog(parent, message, t("Edit Expense", "Ubah Pengeluaran"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (buyerCombo.getSelectedIndex() <= 0) {
                JOptionPane.showMessageDialog(parent, t("Please select a valid buyer.", "Pilih pembeli yang valid."), "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(hargaField.getText().replace(".", "").replace(",", ""));
                
                model.setValueAt(itemField.getText().replace(",", " "), rowIndex, 1);
                model.setValueAt(qtyField.getText().replace(",", " "), rowIndex, 2);
                model.setValueAt(descField.getText().replace(",", " "), rowIndex, 3);
                model.setValueAt(hargaField.getText().replace(".", "").replace(",", ""), rowIndex, 4);
                model.setValueAt(buyerCombo.getSelectedItem().toString(), rowIndex, 5);
                
                rewriteExpenseFile(model);
                reloadData.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, t("Please ensure Price is a valid number.", "Pastikan Harga adalah angka yang valid."));
            }
        }
    }

    private void openAddMenuDialog() {
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField costField = new JTextField();
        
        Object[] message = {
            t("Item Name:", "Nama Barang:"), nameField,
            t("Customer Price (Rp):", "Harga Jual (Rp):"), priceField,
            t("Unit Cost to Make (Rp):", "Harga Modal (Rp):"), costField
        };

        int option = JOptionPane.showConfirmDialog(this, message, t("Add Menu Item", "Tambah Menu"), JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().replace(",", " ");
                int price = Integer.parseInt(priceField.getText().replace(".", "").replace(",", ""));
                int cost = Integer.parseInt(costField.getText().replace(".", "").replace(",", ""));
                
                items.add(name);
                prices.add(price);
                costs.add(cost);
                
                saveConfig();
                rebuildMenuPanel(); 
                JOptionPane.showMessageDialog(this, "Success!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, t("Please ensure Price is a valid number.", "Pastikan Harga adalah angka yang valid."));
            }
        }
    }

    private void openEditPriceDialog() {
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items available to edit.");
            return;
        }

        JDialog dialog = new JDialog(this, t("Edit Price", "Ubah Harga"), true);
        dialog.setSize(500, 400);
        
        JPanel gridPanel = new JPanel(new GridLayout(items.size() + 1, 3, 10, 10));
        gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        gridPanel.add(new JLabel(t("Item Name", "Nama Barang")));
        gridPanel.add(new JLabel(t("Price (Rp)", "Harga (Rp)")));
        gridPanel.add(new JLabel(t("Unit Cost (Rp)", "Harga Modal (Rp)")));

        JTextField[] priceFields = new JTextField[items.size()];
        JTextField[] costFields = new JTextField[items.size()];

        for (int i = 0; i < items.size(); i++) {
            gridPanel.add(new JLabel(items.get(i)));
            priceFields[i] = new JTextField(String.valueOf(prices.get(i)));
            costFields[i] = new JTextField(String.valueOf(costs.get(i)));
            gridPanel.add(priceFields[i]);
            gridPanel.add(costFields[i]);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            try {
                for (int i = 0; i < items.size(); i++) {
                    prices.set(i, Integer.parseInt(priceFields[i].getText().replace(".", "").replace(",", "")));
                    costs.set(i, Integer.parseInt(costFields[i].getText().replace(".", "").replace(",", "")));
                }
                saveConfig();
                rebuildMenuPanel();
                JOptionPane.showMessageDialog(dialog, t("Prices updated successfully!", "Harga berhasil diperbarui!"));
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, t("Please ensure Price is a valid number.", "Pastikan Harga adalah angka yang valid."));
            }
        });

        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel();
        btnPanel.add(saveBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }

    private void openDeleteMenuDialog() {
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items available to delete.");
            return;
        }

        JComboBox<String> itemCombo = new JComboBox<>(items.toArray(new String[0]));
        
        Object[] message = {
            t("Select Menu Item to Delete:", "Pilih Menu untuk Dihapus:"), itemCombo,
            t("WARNING: This will permanently remove the item from your cashier system.", "PERINGATAN: Menu akan dihapus secara permanen dari sistem."),
            t("Past sales records for this item will remain in the CSV files.", "Catatan penjualan untuk barang ini akan tetap ada di file CSV.")
        };

        int option = JOptionPane.showConfirmDialog(this, message, t("Delete Menu Item", "Hapus Menu"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            int index = itemCombo.getSelectedIndex();
            if (index >= 0) {
                String deletedName = items.get(index);
                
                items.remove(index);
                prices.remove(index);
                costs.remove(index);
                
                saveConfig(); 
                rebuildMenuPanel();
                
                JOptionPane.showMessageDialog(this, deletedName + " removed.");
            }
        }
    }

    private void rewriteExpenseFile(DefaultTableModel model) {
        try (FileWriter fw = new FileWriter(folderPath + EXPENSE_FILE)) {
            fw.write("No,Item,Jumlah,Desc,Harga,YangBeli\n");
            for (int i = 0; i < model.getRowCount(); i++) {
                fw.write((i + 1) + "," + model.getValueAt(i, 1) + "," + model.getValueAt(i, 2) + "," + 
                         model.getValueAt(i, 3) + "," + model.getValueAt(i, 4) + "," + model.getValueAt(i, 5) + "\n");
            }
        } catch (Exception e) { }
    }

    private void initializeFiles() {
        try {
            File sFile = new File(folderPath + FILE_NAME);
            if (!sFile.exists()) {
                FileWriter w = new FileWriter(sFile);
                w.write("Date,Time,Item,Quantity,AmountPaid,CustomerName,UnitCost\n");
                w.close();
            }
            File eFile = new File(folderPath + EXPENSE_FILE);
            if (!eFile.exists()) {
                FileWriter w = new FileWriter(eFile);
                w.write("No,Item,Jumlah,Desc,Harga,YangBeli\n");
                w.close();
            }
        } catch (Exception e) { }
    }

    class CartItem {
        String name; int qty, price, cost;
        CartItem(String n, int q, int p, int c) { name = n; qty = q; price = p; cost = c; }
        int getPrice() { return qty * price; }
        int getCost() { return qty * cost; }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { }

        File dir = new File(System.getProperty("user.dir"));
        File[] propFiles = dir.listFiles((d, name) -> name.endsWith(".properties"));
        ArrayList<String> profiles = new ArrayList<>();
        
        if (propFiles != null) {
            for (File f : propFiles) {
                profiles.add(f.getName().replace(".properties", ""));
            }
        }
        if (!profiles.contains("default")) profiles.add(0, "default");
        profiles.add("+ Create New Profile...");

        String selected = (String) JOptionPane.showInputDialog(null, 
            "Select or create a Workspace Profile:\n(Pilih atau buat Profil Ruang Kerja:)", 
            "Cashier Launcher", 
            JOptionPane.PLAIN_MESSAGE, 
            null, 
            profiles.toArray(), 
            profiles.get(0));

        if (selected == null) System.exit(0);

        String profileName = selected;
        if (selected.equals("+ Create New Profile...")) {
            profileName = JOptionPane.showInputDialog("Enter new profile name / Masukkan nama profil baru:");
            if (profileName == null || profileName.trim().isEmpty()) System.exit(0);
            profileName = profileName.trim().replaceAll("[^a-zA-Z0-9_\\-]", ""); 
        }

        String finalConfigName = profileName + ".properties";

        SwingUtilities.invokeLater(() -> new CashierGUI(finalConfigName).setVisible(true));
    }
}

class CircularColorPicker extends JDialog {
    private Color selectedColor;
    private JTextField hexField;
    private JPanel previewPanel;
    private boolean isUpdating = false;

    public static Color showDialog(Component parent, Color initialColor) {
        Frame frame = JOptionPane.getFrameForComponent(parent);
        CircularColorPicker picker = new CircularColorPicker(frame, initialColor);
        picker.setVisible(true);
        return picker.selectedColor;
    }

    private CircularColorPicker(Frame parent, Color initial) {
        super(parent, "HEX Picker", true);
        this.selectedColor = initial;
        setLayout(new BorderLayout(10, 10));
        setSize(280, 360);
        setLocationRelativeTo(parent);
        setResizable(false);

        int imgSize = 240;
        BufferedImage wheelImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        int cx = imgSize / 2;
        int cy = imgSize / 2;
        int radius = imgSize / 2;
        for (int x = 0; x < imgSize; x++) {
            for (int y = 0; y < imgSize; y++) {
                int dx = x - cx;
                int dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist <= radius) {
                    float hue = (float) (Math.atan2(dy, dx) / (2 * Math.PI));
                    if (hue < 0) hue += 1;
                    float sat = (float) (dist / radius);
                    wheelImage.setRGB(x, y, Color.HSBtoRGB(hue, sat, 1.0f));
                } else {
                    wheelImage.setRGB(x, y, 0x00000000);
                }
            }
        }

        JPanel wheelPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(wheelImage, 0, 0, null);
            }
        };
        wheelPanel.setPreferredSize(new Dimension(imgSize, imgSize));
        
        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) { updateColorFromMouse(e.getX(), e.getY(), cx, cy, radius); }
            public void mouseDragged(MouseEvent e) { updateColorFromMouse(e.getX(), e.getY(), cx, cy, radius); }
        };
        wheelPanel.addMouseListener(ma);
        wheelPanel.addMouseMotionListener(ma);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.add(wheelPanel);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel hexPanel = new JPanel(new BorderLayout(5, 5));
        hexField = new JTextField(String.format("#%06X", (0xFFFFFF & selectedColor.getRGB())));
        hexField.setFont(new Font("Monospaced", Font.BOLD, 16));
        hexField.setHorizontalAlignment(JTextField.CENTER);
        
        hexField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateFromHex(); }
            public void removeUpdate(DocumentEvent e) { updateFromHex(); }
            public void changedUpdate(DocumentEvent e) { updateFromHex(); }
        });

        previewPanel = new JPanel();
        previewPanel.setPreferredSize(new Dimension(40, 40));
        previewPanel.setBackground(selectedColor);
        previewPanel.setBorder(new LineBorder(Color.GRAY));

        hexPanel.add(new JLabel("HEX:", SwingConstants.CENTER), BorderLayout.NORTH);
        hexPanel.add(hexField, BorderLayout.CENTER);
        hexPanel.add(previewPanel, BorderLayout.EAST);

        JButton okButton = new JButton("Select");
        okButton.addActionListener(e -> dispose());
        
        bottomPanel.add(hexPanel, BorderLayout.CENTER);
        bottomPanel.add(okButton, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateColorFromMouse(int x, int y, int cx, int cy, int radius) {
        int dx = x - cx;
        int dy = y - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= radius) {
            float hue = (float) (Math.atan2(dy, dx) / (2 * Math.PI));
            if (hue < 0) hue += 1;
            float sat = (float) (dist / radius);
            selectedColor = Color.getHSBColor(hue, sat, 1.0f);
            
            isUpdating = true;
            hexField.setText(String.format("#%06X", (0xFFFFFF & selectedColor.getRGB())));
            previewPanel.setBackground(selectedColor);
            isUpdating = false;
        }
    }

    private void updateFromHex() {
        if (isUpdating) return;
        try {
            String hex = hexField.getText().trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() == 6) {
                selectedColor = new Color(Integer.parseInt(hex, 16));
                previewPanel.setBackground(selectedColor);
            }
        } catch (Exception ex) { }
    }
}