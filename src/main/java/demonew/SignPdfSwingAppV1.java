package demonew;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SignPdfSwingAppV1 {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("PDF Signer");
        JButton selectPdfButton = new JButton("Select PDF");
        JButton selectSignatureButton = new JButton("Select Digital Signature");
        JButton signButton = new JButton("Sign PDF");
        JTextField pdfPathField = new JTextField(30);
        JTextField signaturePathField = new JTextField(30);

        selectPdfButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    pdfPathField.setText(selectedFile.getAbsolutePath());
                }
            }
        });

        selectSignatureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayLocalCertificates(frame, signaturePathField);
            }
        });

        signButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String pdfPath = pdfPathField.getText();
                String signatureAlias = signaturePathField.getText();
                if (!pdfPath.isEmpty() && !signatureAlias.isEmpty()) {
                    String password = JOptionPane.showInputDialog(frame, "Enter the password for the digital signature:");
                    if (password != null && !password.isEmpty()) {
                        try {
                            signPdf(pdfPath, signatureAlias, password);
                            JOptionPane.showMessageDialog(frame, "PDF signed successfully!");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame, "Error signing PDF: " + ex.getMessage());
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "Password is required to sign the PDF.");
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Please select both a PDF file and a digital signature.");
                }
            }
        });

        JPanel panel = new JPanel();
        panel.add(selectPdfButton);
        panel.add(pdfPathField);
        panel.add(selectSignatureButton);
        panel.add(signaturePathField);
        panel.add(signButton);

        frame.add(panel);
        frame.setSize(800, 150);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void displayLocalCertificates(JFrame frame, JTextField signaturePathField) {
        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            if (!aliases.hasMoreElements()) {
                throw new Exception("No aliases found in the keystore.");
            }

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));

            List<String> validAliases = new ArrayList<>();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = ks.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509Cert = (X509Certificate) cert;
                    boolean[] keyUsage = x509Cert.getKeyUsage();
                    if (keyUsage != null && keyUsage[0]) { // keyUsage[0] indicates digitalSignature
                        validAliases.add(alias);
                        String details = "<html>Alias: " + alias + "<br>Issuer: " + x509Cert.getIssuerDN().getName()
                                + "<br>Valid From: " + x509Cert.getNotBefore() + " to " + x509Cert.getNotAfter() + "</html>";

                        JPanel certPanel = new JPanel(new BorderLayout());
                        certPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                        certPanel.add(new JLabel(new ImageIcon("E:\\java\\DSC_Project\\src\\main\\java\\demonew\\download.png")), BorderLayout.WEST);
                        certPanel.add(new JLabel(details), BorderLayout.CENTER);
                        panel.add(certPanel);
                    }
                }
            }

            JScrollPane scrollPane = new JScrollPane(panel);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            int result = JOptionPane.showOptionDialog(frame, scrollPane, "Select a Certificate",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (result == JOptionPane.OK_OPTION) {
                if (!validAliases.isEmpty()) {
                    signaturePathField.setText(validAliases.get(0));
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error displaying certificates: " + ex.getMessage());
        }
    }

    private static void signPdf(String pdfPath, String alias, String password) throws Exception {
        String dest = "signed_" + new File(pdfPath).getName();

        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);

        PrivateKey pk = (PrivateKey) ks.getKey(alias, password.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);

        try (PdfReader reader = new PdfReader(pdfPath);
             FileOutputStream os = new FileOutputStream(dest)) {
            PdfSigner signer = new PdfSigner(reader, os, new StampingProperties());

            PdfSignatureAppearance appearance = signer.getSignatureAppearance()
                    .setReason("Testing")
                    .setLocation("Location")
                    .setPageRect(new Rectangle(36, 748, 200, 100)) // Adjust the rectangle to place at the desired position
                    .setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);
            signer.setFieldName("sig");

            IExternalSignature pks = new PrivateKeySignature(pk, DigestAlgorithms.SHA256, "BC");
            IExternalDigest digest = new BouncyCastleDigest();

            signer.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
        }
    }
}
