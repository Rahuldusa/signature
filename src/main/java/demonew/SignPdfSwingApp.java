package demonew;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SignPdfSwingApp extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextField selectedPathField;
    private JTable fileTable;
    private DefaultTableModel model;
    private List<File> pdfFiles = new ArrayList<>();
    private File destinationFolder;
    private JButton nextButton;
    private JTextField fileDestinationField;
    private String selectedCertificateAlias;
    private String currentProcessType; // Add this line

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public SignPdfSwingApp() {
        setTitle("iiiQBets Digital Signing Software");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        showLogo();

        setContentPane(mainPanel);
        setVisible(true);

        // Transition to main screen after 3 seconds
        Timer timer = new Timer(3000, e -> showMainScreen());
        timer.setRepeats(false);
        timer.start();
    }

    private void showLogo() {
        JPanel logoPanel = new JPanel(new GridBagLayout());
        JLabel logoLabel = new JLabel();
        logoLabel.setIcon(loadImageIcon("src/main/java/images/iiiqbets1.png")); // Adjust the path
        logoPanel.add(logoLabel);
        mainPanel.add(logoPanel, "LogoScreen");
        cardLayout.show(mainPanel, "LogoScreen");
    }

    private void showMainScreen() {
        JPanel mainScreenPanel = new JPanel(new BorderLayout());
        mainScreenPanel.setBorder(BorderFactory.createEmptyBorder(0, 240, 20, 240)); // Reduced bottom padding

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel mainLogoLabel = new JLabel();
        mainLogoLabel.setIcon(loadImageIcon("src/main/java/images/iqbetss.png")); // Adjust the path
        mainLogoLabel.setHorizontalAlignment(JLabel.CENTER);
        mainScreenPanel.add(mainLogoLabel, BorderLayout.NORTH);

        JLabel titleLabel = new JLabel("Digital Signing Software");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 25));
        titleLabel.setForeground(Color.BLUE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(titleLabel);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Add some vertical space

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton signPDFButton = createButton("Sign PDF", "src/main/java/images/pen2.png", new Color(23, 162, 184)); // Adjust the path
        signPDFButton.addActionListener(e -> {
            currentProcessType = "SignPDF"; // Set current process type
            showSignPDFScreen();
        });
        buttonPanel.add(signPDFButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Add some vertical space

        JButton signProtectPDFButton = createButton("Sign and Protect PDF", "src/main/java/images/finger4.png", new Color(252, 157, 3)); // Adjust the path
        signProtectPDFButton.addActionListener(e -> {
            currentProcessType = "SignProtectPDF"; // Set current process type
            showSignProtectPDFScreen();
        });
        buttonPanel.add(signProtectPDFButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Add some vertical space

        JButton protectPDFButton = createButton("Protect PDF", "src/main/java/images/lock1.png", new Color(40, 167, 69)); // Adjust the path
        protectPDFButton.addActionListener(e -> {
            currentProcessType = "ProtectPDF"; // Set current process type
            showProtectPDFScreen();
        });
        buttonPanel.add(protectPDFButton);

        centerPanel.add(buttonPanel);
        mainScreenPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(mainScreenPanel, "MainScreen");
        cardLayout.show(mainPanel, "MainScreen");
    }

    private JButton createButton(String text, String iconPath, Color backgroundColor) {
        JButton button = new JButton(text, new ImageIcon(iconPath));
        button.setPreferredSize(new Dimension(250, 50)); // Set button size
        button.setMaximumSize(new Dimension(250, 50)); // Ensure maximum size is respected
        button.setFont(new Font("Arial", Font.BOLD, 14)); // Adjusted font size
        button.setForeground(Color.WHITE);
        button.setBackground(backgroundColor);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }

    private JPanel createHeaderPanel(boolean isProtection) {
        JPanel headerPanel = new JPanel(new BorderLayout());

        JPanel stepsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stepsPanel.add(createStepPanel("Files", "src/main/java/images/folder2.png", true, isProtection));
        stepsPanel.add(createStepPanel("Signing", "src/main/java/images/pen2.png", false, isProtection));

        JLabel homeIcon = new JLabel(loadImageIcon("src/main/java/images/icon1.png"));
        homeIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        homeIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                confirmHome();
            }
        });

        headerPanel.add(stepsPanel, BorderLayout.WEST);
        headerPanel.add(homeIcon, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createStepPanel(String stepName, String iconPath, boolean active, boolean isProtection) {
        JPanel stepPanel = new JPanel(new BorderLayout());
        JLabel stepIcon = new JLabel(loadImageIcon(iconPath));
        String displayName = isProtection && stepName.equals("Signing") ? "Protected" : stepName;
        JLabel stepLabel = new JLabel(displayName, SwingConstants.CENTER);
        stepLabel.setFont(new Font("Arial", Font.BOLD, 16));
        stepPanel.add(stepIcon, BorderLayout.CENTER);
        stepPanel.add(stepLabel, BorderLayout.SOUTH);
        if (active) {
            stepIcon.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            stepLabel.setForeground(Color.BLUE);
        } else {
            stepIcon.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
            stepLabel.setForeground(Color.GRAY);
        }
        return stepPanel;
    }

    private ImageIcon loadImageIcon(String path) {
        File imgFile = new File(path);
        if (imgFile.exists()) {
            return new ImageIcon(imgFile.getAbsolutePath());
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    private void showSignPDFScreen() {
        JPanel signPDFPanel = createCommonPanel("Files", "Signing", new Color(23, 162, 184), Color.RED, Color.GREEN, false, false, false);
        mainPanel.add(signPDFPanel, "SignPDFScreen");
        cardLayout.show(mainPanel, "SignPDFScreen");
    }

    private void showSignProtectPDFScreen() {
        JPanel signProtectPDFPanel = createCommonPanel("Files", "Signing", new Color(252, 157, 3), Color.RED, Color.GREEN, false, false, false);
        mainPanel.add(signProtectPDFPanel, "SignProtectPDFScreen");
        cardLayout.show(mainPanel, "SignProtectPDFScreen");
    }

    private void showProtectPDFScreen() {
        JPanel protectPDFPanel = new JPanel(new BorderLayout());
        protectPDFPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        protectPDFPanel.add(createHeaderPanel(true), BorderLayout.NORTH);

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));

        JPanel selectPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JButton selectFileButton = new JButton("Select File");
        styleButton(selectFileButton, new Color(40, 167, 69));
        selectFileButton.addActionListener(this::selectFile);

        JButton selectFolderButton = new JButton("Select Folder");
        styleButton(selectFolderButton, new Color(40, 167, 69));
        selectFolderButton.addActionListener(this::selectFolder);

        selectedPathField = new JTextField("Selected Path", 44);
        selectedPathField.setEditable(false);
        selectedPathField.setHorizontalAlignment(JTextField.CENTER);

        Dimension buttonSize = new Dimension(150, 40); // Set size for buttons
        selectFileButton.setPreferredSize(buttonSize);
        selectFolderButton.setPreferredSize(buttonSize);
        selectedPathField.setPreferredSize(buttonSize); // Set the same size for text field

        // Add the selectFileButton to the panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        selectPanel.add(selectFileButton, gbc);

        // Add the selectFolderButton to the panel
        gbc.gridx = 1;
        gbc.gridy = 0;
        selectPanel.add(selectFolderButton, gbc);

        // Add the selectedPathField to the panel, it will expand to fill the remaining space
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        selectPanel.add(selectedPathField, gbc);

        filePanel.add(selectPanel);

        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Make the delete button column editable
            }
        };
        fileTable = new JTable(model);
        model.addColumn("File");
        model.addColumn("Delete");

        fileTable.getColumnModel().getColumn(1).setCellRenderer(new ButtonRenderer());
        fileTable.getColumnModel().getColumn(1).setCellEditor(new ButtonEditor(new JCheckBox()));

        TableColumn deleteColumn = fileTable.getColumnModel().getColumn(1);
        deleteColumn.setPreferredWidth(80); // Set the preferred width of the delete button column
        deleteColumn.setMaxWidth(80); // Set the maximum width of the delete button column

        JScrollPane scrollPane = new JScrollPane(fileTable);
        filePanel.add(scrollPane);
        protectPDFPanel.add(filePanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout()); // Align to the bottom right
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0)); // Add padding to the bottom

        // Align Previous, Cancel, and Finish buttons beside Select Destination button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton previousButton = new JButton("Previous");
        styleButton(previousButton, new Color(40, 167, 69));
        previousButton.setPreferredSize(new Dimension(110, 30)); // Decreased width for button
        previousButton.addActionListener(e -> showMainScreen());
        JButton cancelButton = new JButton("Cancel");
        styleButton(cancelButton, Color.RED);
        cancelButton.setPreferredSize(new Dimension(110, 30)); // Decreased width for button
        cancelButton.addActionListener(this::cancelSelection);
        nextButton = new JButton("Finish");
        styleButton(nextButton, Color.GREEN);
        nextButton.setPreferredSize(new Dimension(110, 30)); // Decreased width for button
        nextButton.setEnabled(false); // Disable initially
        nextButton.addActionListener(e -> {
            try {
                finishProcess(true, null);
            } catch (java.io.IOException e1) {
                e1.printStackTrace();
            }
        });

        rightPanel.add(previousButton);
        rightPanel.add(cancelButton);
        rightPanel.add(nextButton);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // Include Destination button only in the Protect PDF screen
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Use FlowLayout to align side by side

        JButton selectDestinationButton = new JButton("Destination");
        styleButton(selectDestinationButton, new Color(40, 167, 69));
        selectDestinationButton.setPreferredSize(new Dimension(120, 30)); // Increased width for button
        selectDestinationButton.addActionListener(this::selectDestination);
        leftPanel.add(selectDestinationButton);

        // Add the fileDestinationField beside the Destination button
        fileDestinationField = new JTextField(25); // Further decreased width for Protected PDF page
        fileDestinationField.setPreferredSize(new Dimension(150, 30)); // Adjust width for Protected PDF page
        fileDestinationField.setEditable(false);
        leftPanel.add(fileDestinationField);

        bottomPanel.add(leftPanel, BorderLayout.WEST);

        protectPDFPanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(protectPDFPanel, "ProtectPDFScreen");
        cardLayout.show(mainPanel, "ProtectPDFScreen");
    }



    private JPanel createCommonPanel(String firstStep, String secondStep, Color buttonColor, Color cancelColor, Color nextColor, boolean isProtectionOnly, boolean isProtection, boolean includeDestination) {
        JPanel commonPanel = new JPanel(new BorderLayout());
        commonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        commonPanel.add(createHeaderPanel(isProtection), BorderLayout.NORTH);

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));

        JPanel selectPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JButton selectFileButton = new JButton("Select File");
        styleButton(selectFileButton, buttonColor);
        selectFileButton.addActionListener(this::selectFile);

        JButton selectFolderButton = new JButton("Select Folder");
        styleButton(selectFolderButton, buttonColor);
        selectFolderButton.addActionListener(this::selectFolder);

        selectedPathField = new JTextField("Selected Path", 44);
        selectedPathField.setEditable(false);
        selectedPathField.setHorizontalAlignment(JTextField.CENTER);

        Dimension buttonSize = new Dimension(150, 40); // Set size for buttons
        selectFileButton.setPreferredSize(buttonSize);
        selectFolderButton.setPreferredSize(buttonSize);
        selectedPathField.setPreferredSize(buttonSize); // Set the same size for text field

        // Add the selectFileButton to the panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        selectPanel.add(selectFileButton, gbc);

        // Add the selectFolderButton to the panel
        gbc.gridx = 1;
        gbc.gridy = 0;
        selectPanel.add(selectFolderButton, gbc);

        // Add the selectedPathField to the panel, it will expand to fill the remaining space
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        selectPanel.add(selectedPathField, gbc);

        filePanel.add(selectPanel);

        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Make the delete button column editable
            }
        };
        fileTable = new JTable(model);
        model.addColumn("File");
        model.addColumn("Delete");

        fileTable.getColumnModel().getColumn(1).setCellRenderer(new ButtonRenderer());
        fileTable.getColumnModel().getColumn(1).setCellEditor(new ButtonEditor(new JCheckBox()));

        TableColumn deleteColumn = fileTable.getColumnModel().getColumn(1);
        deleteColumn.setPreferredWidth(80); // Set the preferred width of the delete button column
        deleteColumn.setMaxWidth(80); // Set the maximum width of the delete button column

        JScrollPane scrollPane = new JScrollPane(fileTable);
        filePanel.add(scrollPane);
        commonPanel.add(filePanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout()); // Align to the bottom right
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0)); // Add padding to the bottom

        // Align Previous, Cancel, and Finish buttons beside Select Destination button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton previousButton = new JButton("Previous");
        styleButton(previousButton, buttonColor);
        previousButton.addActionListener(e -> showMainScreen());
        JButton cancelButton = new JButton("Cancel");
        styleButton(cancelButton, cancelColor);
        cancelButton.addActionListener(this::cancelSelection);
        nextButton = new JButton("Next");
        styleButton(nextButton, nextColor);
        nextButton.setEnabled(false); // Disable initially
        nextButton.addActionListener(e -> {
            if (isProtectionOnly) {
                try {
                    finishProcess(true, null);
                } catch (java.io.IOException e1) {
                    e1.printStackTrace();
                }
            } else {
                showSignaturePage(isProtectionOnly);
            }
        });

        rightPanel.add(previousButton);
        rightPanel.add(cancelButton);
        rightPanel.add(nextButton);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // Include Destination button only in the Protect PDF screen
        if (includeDestination) {
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton selectDestinationButton = new JButton("Destination");
            styleButton(selectDestinationButton, buttonColor);
            selectDestinationButton.addActionListener(this::selectDestination);
            leftPanel.add(selectDestinationButton);
            bottomPanel.add(leftPanel, BorderLayout.WEST);
        }

        commonPanel.add(bottomPanel, BorderLayout.SOUTH);

        return commonPanel;
    }

    private void confirmHome() {
        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to go back to Home?", "Confirm Home", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            clearAllChanges();
            showMainScreen();
        }
    }

    private void clearAllChanges() {
        model.setRowCount(0);
        selectedPathField.setText("");
        pdfFiles.clear();
        destinationFolder = null;
        updateNextButtonState();
    }

    private void showSignaturePage(boolean isProtectionOnly) {
        JPanel signaturePanel = new JPanel(new BorderLayout());
        signaturePanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        signaturePanel.add(createHeaderPanel(isProtectionOnly), BorderLayout.NORTH);

        JPanel signatureOptionsPanel = new JPanel();
        signatureOptionsPanel.setLayout(new BoxLayout(signatureOptionsPanel, BoxLayout.Y_AXIS));
        signatureOptionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        signatureOptionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Adding Destination button and path field
        JPanel destinationPanel = new JPanel();
        destinationPanel.setLayout(new BoxLayout(destinationPanel, BoxLayout.X_AXIS)); // Aligns components horizontally
        JButton selectDestinationButton = new JButton("Destination");
        selectDestinationButton.setPreferredSize(new Dimension(120, 50));
        styleButton(selectDestinationButton, new Color(23, 162, 184));
        selectDestinationButton.addActionListener(this::selectDestination);
        destinationPanel.add(selectDestinationButton);
        signatureOptionsPanel.add(Box.createVerticalStrut(30));

        // Increase the height of the path text field
        fileDestinationField = new JTextField(50);
        fileDestinationField.setPreferredSize(new Dimension(200, 20)); // Increase height
        fileDestinationField.setEditable(false);
        destinationPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing between the button and text field
        destinationPanel.add(fileDestinationField);

        signatureOptionsPanel.add(destinationPanel);
        signatureOptionsPanel.add(Box.createVerticalStrut(50)); // Add some vertical spacing between the destination panel and the next component

        // Adding Select Signature button
        JButton selectSignatureButton = new JButton("Select Signature");
        styleButton(selectSignatureButton, new Color(23, 162, 184));
        selectSignatureButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectSignatureButton.addActionListener(e -> selectCertificateAlias());
        signatureOptionsPanel.add(selectSignatureButton);
        signatureOptionsPanel.add(Box.createVerticalStrut(20)); // Add some spacing between components

        // Adding Page Selection Options
        JComboBox<String> pageSelectionOptions = new JComboBox<>(new String[]{"First Page", "Last Page", "All Pages", "Customized Pages"});
        pageSelectionOptions.setAlignmentX(Component.CENTER_ALIGNMENT);
        pageSelectionOptions.setMaximumSize(new Dimension(200, 30)); // Set the preferred size of the JComboBox
        signatureOptionsPanel.add(pageSelectionOptions);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(signatureOptionsPanel, gbc);
        signaturePanel.add(centerPanel, BorderLayout.CENTER);

        // Adding Previous, Cancel, and Finish buttons
        JPanel bottomPanel = new JPanel(new BorderLayout()); // Align to the right

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton previousButton = new JButton("Previous");
        styleButton(previousButton, new Color(23, 162, 184));
        previousButton.addActionListener(e -> cardLayout.show(mainPanel, "SignPDFScreen"));
        JButton cancelButton = new JButton("Cancel");
        styleButton(cancelButton, Color.RED);
        cancelButton.addActionListener(this::cancelSelection);
        JButton finishButton = new JButton("Finish");
        styleButton(finishButton, Color.GREEN);
        finishButton.addActionListener(e -> {
            try {
                finishProcess(isProtectionOnly, pageSelectionOptions.getSelectedItem().toString());
            } catch (java.io.IOException e1) {
                e1.printStackTrace();
            }
        });

        rightPanel.add(previousButton);
        rightPanel.add(cancelButton);
        rightPanel.add(finishButton);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // Add the bottom panel to the bottom of the signature panel
        signaturePanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(signaturePanel, "SignaturePage");
        cardLayout.show(mainPanel, "SignaturePage");
    }

    private void styleButton(JButton button, Color backgroundColor) {
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(150, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void selectFile(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }

            @Override
            public String getDescription() {
                return "PDF Files (*.pdf)";
            }
        });
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            selectedPathField.setText(selectedFile.getAbsolutePath());
            addFileToTable(selectedFile);
            pdfFiles.add(selectedFile);
            updateNextButtonState();
        }
    }

    private void selectFolder(ActionEvent event) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = folderChooser.getSelectedFile();
            selectedPathField.setText(selectedFolder.getAbsolutePath());
            addFilesFromFolderToTable(selectedFolder);
            pdfFiles.addAll(listFiles(selectedFolder));
            updateNextButtonState();
        }
    }

    private List<File> listFiles(File folder) {
        List<File> filesList = new ArrayList<>();
        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pdf");
            }
        });
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    filesList.add(file);
                } else if (file.isDirectory()) {
                    filesList.addAll(listFiles(file));
                }
            }
        }
        return filesList;
    }

    private void addFilesFromFolderToTable(File folder) {
        List<File> files = listFiles(folder);
        for (File file : files) {
            addFileToTable(file);
        }
    }

    private void addFileToTable(File file) {
        model.addRow(new Object[]{file.getName(), "Delete"});
    }

    private void selectDestination(ActionEvent event) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = folderChooser.getSelectedFile();
            File newSubFolder;
            if ("SignPDF".equals(currentProcessType)) {
                newSubFolder = new File(selectedFolder, "Signed_PDFs");
            } else if ("SignProtectPDF".equals(currentProcessType)) {
                newSubFolder = new File(selectedFolder, "SignProtected_PDFs");
            } else if ("ProtectPDF".equals(currentProcessType)) {
                newSubFolder = new File(selectedFolder, "Protected_PDFs");
            } else {
                newSubFolder = selectedFolder;
            }
            if (!newSubFolder.exists()) {
                newSubFolder.mkdirs();
            }
            destinationFolder = newSubFolder;
            fileDestinationField.setText(destinationFolder.getAbsolutePath());
        }
    }

    private void cancelSelection(ActionEvent event) {
        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel?", "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            model.setRowCount(0);
            selectedPathField.setText("");
            pdfFiles.clear();
            updateNextButtonState();
        }
    }

    private void finishProcess(boolean isProtectionOnly, String pageOption) throws java.io.IOException {
        if (pdfFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No file or folder selected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (destinationFolder == null) {
            File desktop = new File(System.getProperty("user.home"), "Desktop");
            if (isProtectionOnly) {
                destinationFolder = new File(desktop, "Protected_PDFs");
            } else {
                destinationFolder = new File(desktop, "Signed_PDFs");
            }
            if (!destinationFolder.exists()) {
                destinationFolder.mkdirs();
            }
        }

        for (File file : pdfFiles) {
            if (isProtectionOnly) {
                String password = extractPassword(file);
                if (password != null) {
                    try {
                        addPasswordProtection(file, password, destinationFolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Failed to add password to " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No valid password found in file name for " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                try {
                    String password = JOptionPane.showInputDialog(this, "Enter the password for the digital signature:");
                    if (password != null && !password.isEmpty()) {
                        signPdf(file, pageOption, password);
                    } else {
                        JOptionPane.showMessageDialog(this, "Password is required to sign the PDF.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Failed to sign " + file.getName() + "\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("PDF's Successfully Processed!");
        label.setFont(new Font("Arial", Font.BOLD, 20));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createVerticalStrut(20));
        panel.add(label);
        panel.add(Box.createVerticalStrut(20));
        JOptionPane.showMessageDialog(this, panel, "Successful", JOptionPane.INFORMATION_MESSAGE);
        cardLayout.show(mainPanel, "MainScreen");

        model.setRowCount(0);
        selectedPathField.setText("");
        pdfFiles.clear();
        destinationFolder = null;
    }

    private String extractPassword(File file) {
        String baseName = file.getName();
        String[] nameParts = baseName.split("_");
        if (nameParts.length > 1) {
            return nameParts[nameParts.length - 1].replace(".pdf", "");
        }
        return null;
    }

    private String extractBaseFileName(File file) {
        String baseName = file.getName();
        int lastUnderscoreIndex = baseName.lastIndexOf('_');
        if (lastUnderscoreIndex != -1) {
            return baseName.substring(0, lastUnderscoreIndex) + ".pdf";
        }
        return baseName;
    }

    private void addPasswordProtection(File file, String password, File destinationFolder) throws IOException, java.io.IOException {
        PDDocument document = PDDocument.load(file);
        StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(password, password, null);
        protectionPolicy.setEncryptionKeyLength(128);
        protectionPolicy.setPermissions(new org.apache.pdfbox.pdmodel.encryption.AccessPermission());
        document.protect(protectionPolicy);

        String outputFileName = extractBaseFileName(file);
        File outputFile = new File(destinationFolder, outputFileName);
        document.save(outputFile);
        document.close();
    }

    private void updateNextButtonState() {
        if (!pdfFiles.isEmpty()) {
            nextButton.setEnabled(true);
        } else {
            nextButton.setEnabled(false);
        }
    }

    private void signPdf(File file, String pageOption, String password) throws Exception {
        if (selectedCertificateAlias == null) {
            throw new Exception("No certificate alias selected.");
        }

        System.out.println("Starting signing process for file: " + file.getAbsolutePath());
        String dest = new File(destinationFolder, extractBaseFileName(file)).getAbsolutePath();
        System.out.println("Destination file: " + dest);

        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);
        System.out.println("Loaded keystore");

        KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(selectedCertificateAlias, new KeyStore.PasswordProtection(password.toCharArray()));
        if (pkEntry == null) {
            throw new Exception("No key found for alias: " + selectedCertificateAlias);
        }

        PrivateKey pk = pkEntry.getPrivateKey();
        System.out.println("Private key type: " + pk.getClass().getName());

        Certificate[] chain = pkEntry.getCertificateChain();
        if (chain == null) {
            throw new Exception("No certificate chain found for alias: " + selectedCertificateAlias);
        }
        System.out.println("Certificate chain length: " + chain.length);

        IExternalSignature pks;
        if (pk.getClass().getName().equals("sun.security.mscapi.CPrivateKey")) {
            pks = new MSCAPISignature(pk, "SHA1"); // Using SHA1withRSA
        } else if (pk instanceof java.security.interfaces.RSAPrivateKey) {
            pks = new PrivateKeySignature(pk, DigestAlgorithms.SHA256, "BC");
        } else if (pk instanceof java.security.interfaces.DSAPrivateKey) {
            pks = new PrivateKeySignature(pk, DigestAlgorithms.SHA256, "BC");
        } else if (pk instanceof java.security.interfaces.ECPrivateKey) {
            pks = new PrivateKeySignature(pk, DigestAlgorithms.SHA256, "BC");
        } else {
            throw new Exception("Unsupported key type: " + pk.getClass().getName());
        }
        System.out.println("Initialized external signature");

        IExternalDigest digest = new BouncyCastleDigest();

        try (PdfReader reader = new PdfReader(file.getAbsolutePath());
             FileOutputStream os = new FileOutputStream(dest)) {
            System.out.println("Opened PDF reader and output stream");

            PdfSigner signer = new PdfSigner(reader, os, new StampingProperties());

            PdfSignatureAppearance appearance = signer.getSignatureAppearance()
                    .setReason("Testing")
                    .setLocation("Location")
                    .setPageRect(new Rectangle(350, 40, 200, 100))
                    .setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);

            PdfDocument pdfDoc = signer.getDocument();
            int totalPages = pdfDoc.getNumberOfPages();
            System.out.println("Total pages in document: " + totalPages);

            if ("First Page".equals(pageOption)) {
                appearance.setPageNumber(1);
                signer.setFieldName("sig");
                signer.setCertificationLevel(PdfSigner.NOT_CERTIFIED);
                signer.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
                System.out.println("Signed first page");
            } else if ("Last Page".equals(pageOption)) {
                appearance.setPageNumber(totalPages);
                signer.setFieldName("sig");
                signer.setCertificationLevel(PdfSigner.NOT_CERTIFIED);
                signer.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
                System.out.println("Signed last page");
            } else if ("All Pages".equals(pageOption)) {
                for (int i = 1; i <= totalPages; i++) {
                    try (PdfReader pageReader = new PdfReader(file.getAbsolutePath());
                         FileOutputStream pageOs = new FileOutputStream(dest.replace(".pdf", "_page_" + i + ".pdf"))) {
                        PdfSigner pageSigner = new PdfSigner(pageReader, pageOs, new StampingProperties());
                        pageSigner.getSignatureAppearance()
                                .setReason("Testing")
                                .setLocation("Location")
                                .setPageRect(new Rectangle(650, 35, 200, 100))
                                .setPageNumber(i)
                                .setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION)
                                .setCertificate(chain[0]);
                        pageSigner.setFieldName("sig_" + i);
                        pageSigner.setCertificationLevel(PdfSigner.NOT_CERTIFIED);
                        pageSigner.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
                        System.out.println("Signed page " + i);
                    }
                }
            }
        }
    }

    private String selectCertificateAlias() {
        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            if (!aliases.hasMoreElements()) {
                throw new Exception("No aliases found in the keystore.");
            }

            DefaultListModel<String> listModel = new DefaultListModel<>();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = ks.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509Cert = (X509Certificate) cert;
                    boolean[] keyUsage = x509Cert.getKeyUsage();
                    if (keyUsage != null && keyUsage[0]) {
                        listModel.addElement(alias);
                    }
                }
            }

            JList<String> certList = new JList<>(listModel);
            certList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(certList);
            scrollPane.setPreferredSize(new Dimension(400, 200));

            int result = JOptionPane.showOptionDialog(this, scrollPane, "Select a Certificate",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (result == JOptionPane.OK_OPTION && certList.getSelectedValue() != null) {
                selectedCertificateAlias = certList.getSelectedValue(); // Store the selected alias
                return selectedCertificateAlias;
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load keystore or select certificate alias.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private class MSCAPISignature implements IExternalSignature {
        private PrivateKey privateKey;
        private String hashAlgorithm;

        public MSCAPISignature(PrivateKey privateKey, String hashAlgorithm) {
            this.privateKey = privateKey;
            this.hashAlgorithm = hashAlgorithm;
        }

        @Override
        public String getHashAlgorithm() {
            return hashAlgorithm;
        }

        @Override
        public String getEncryptionAlgorithm() {
            return "RSA";
        }

        @Override
        public byte[] sign(byte[] message) throws GeneralSecurityException {
            Signature signature;
            if (hashAlgorithm.equals("SHA256")) {
                signature = Signature.getInstance("SHA1withRSA", "SunMSCAPI"); // Fallback to SHA1withRSA
            } else {
                signature = Signature.getInstance(hashAlgorithm + "withRSA", "SunMSCAPI");
            }
            signature.initSign(privateKey);
            signature.update(message);
            return signature.sign();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignPdfSwingApp());
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private String label;
        private boolean isPushed;
        private JButton button;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                    int selectedRow = fileTable.getSelectedRow();
                    model.removeRow(selectedRow);
                    pdfFiles.remove(selectedRow);
                    updateNextButtonState();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }
}
