package demonew;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.FileNotFoundException;

public class CreatePdf {
    public static void main(String[] args) {
        String dest = "hello_world.pdf";
        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.add(new Paragraph("Hello, World!"));
            document.close();
            System.out.println("PDF Created!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}

