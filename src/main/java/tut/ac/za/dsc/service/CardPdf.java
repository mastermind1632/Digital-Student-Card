package tut.ac.za.dsc.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Font;
import com.itextpdf.text.Element;
import java.io.ByteArrayOutputStream;

import tut.ac.za.dsc.domain.Student;

/**
 * Builds a printable card in the standard bank-card size (85.6 x 54 mm): page 1 is the front with the photo,
 * page 2 the back with the verification QR code.
 */
public final class CardPdf {

    private static final float W = 242.65f;   // 85.6 mm
    private static final float H = 153.07f;   // 54 mm
    private static final BaseColor BASE = new BaseColor(0x3B, 0x2A, 0x8C);
    private static final BaseColor LINE = new BaseColor(0x6E, 0x5B, 0xD9);
    private static final BaseColor INK = new BaseColor(0x16, 0x18, 0x2B);
    private static final BaseColor MUTED = new BaseColor(0x62, 0x64, 0x7A);
    private static final BaseColor LILAC = new BaseColor(0xD9, 0xD1, 0xFF);

    private CardPdf() {
    }

    public static byte[] build(Student s, byte[] photoJpeg, String verifyUrl, String institution, String office) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(new Rectangle(W, H), 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        doc.addTitle("Student card - " + s.getFullName());
        doc.addAuthor(institution);
        doc.open();
        BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
        BaseFont regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
        PdfContentByte cb = writer.getDirectContent();

        // ---------------- front ----------------
        cb.setColorFill(BASE);
        cb.roundRectangle(0, 0, W, H, 9);
        cb.fill();
        guilloche(cb);

        text(cb, bold, 7.5f, BaseColor.WHITE, institution, 12, H - 18);
        text(cb, regular, 6f, LILAC, "STUDENT CARD", 12, H - 27);

        // photo in a white frame
        float pw = 58, ph = 77, px = 12, py = 16;
        cb.setColorFill(BaseColor.WHITE);
        cb.roundRectangle(px - 2, py - 2, pw + 4, ph + 4, 4);
        cb.fill();
        if (photoJpeg != null) {
            Image img = Image.getInstance(photoJpeg);
            img.scaleAbsolute(pw, ph);
            img.setAbsolutePosition(px, py);
            cb.addImage(img);
        } else {
            text(cb, bold, 22, LINE, s.getInitials(), px + 14, py + 30);
        }

        float tx = 82;
        Font nameFont = new Font(bold, 11, Font.NORMAL, BaseColor.WHITE);
        ColumnText ct = new ColumnText(cb);
        ct.setSimpleColumn(new Phrase(s.getFullName(), nameFont), tx, H - 70, W - 10, H - 34, 12, Element.ALIGN_LEFT);
        ct.go();
        text(cb, regular, 5.5f, LILAC, "Student number", tx, 72);
        text(cb, bold, 12, BaseColor.WHITE, s.getStudentNumberDisplay(), tx, 60);
        text(cb, regular, 5.5f, LILAC, fit(s.getQualification(), 48), tx, 46);
        text(cb, regular, 5.5f, LILAC, fit(s.getCampus(), 48), tx, 38);
        text(cb, regular, 5.5f, LILAC, "Valid until", tx, 24);
        text(cb, bold, 8, BaseColor.WHITE, s.getExpiryDateText(), tx, 15);

        // ---------------- back ----------------
        doc.newPage();
        cb.setColorFill(BaseColor.WHITE);
        cb.rectangle(0, 0, W, H);
        cb.fill();
        cb.setColorFill(BASE);
        cb.rectangle(0, H - 8, W, 8);
        cb.fill();

        Image qr = Image.getInstance(QrCodes.png(verifyUrl, 360));
        qr.scaleAbsolute(92, 92);
        qr.setAbsolutePosition(W - 104, 30);
        cb.addImage(qr);
        text(cb, regular, 5.5f, MUTED, "Scan to check this card", W - 101, 22);

        text(cb, bold, 8, INK, "If found, please return to", 12, H - 26);
        text(cb, regular, 6.5f, INK, fit(office, 40), 12, H - 36);
        text(cb, regular, 6.5f, INK, fit(institution, 40), 12, H - 45);

        text(cb, regular, 5.5f, MUTED, "Issued", 12, 62);
        text(cb, bold, 7, INK, s.getIssueDateText(), 12, 53);
        text(cb, regular, 5.5f, MUTED, "Card version", 12, 40);
        text(cb, bold, 7, INK, String.valueOf(s.getCardVersion()), 12, 31);
        text(cb, regular, 4.6f, MUTED, "This card remains the property of the institution.", 12, 14);
        doc.close();
        return out.toByteArray();
    }

    /** Fine wavy lines like the security printing on banknotes and ID documents. */
    private static void guilloche(PdfContentByte cb) {
        cb.saveState();
        cb.setColorStroke(LINE);
        cb.setLineWidth(0.35f);
        for (int k = 0; k < 14; k++) {
            float base = 18 + k * 9.5f;
            cb.moveTo(0, base);
            for (int x = 0; x <= (int) W; x += 6) {
                double y = base + 5 * Math.sin(x / 17.0 + k * 0.7) + 2.5 * Math.sin(x / 7.0 + k);
                cb.lineTo(x, (float) y);
            }
            cb.stroke();
        }
        cb.restoreState();
    }

    private static void text(PdfContentByte cb, BaseFont font, float size, BaseColor color, String text, float x, float y) {
        if (text == null) return;
        cb.beginText();
        cb.setFontAndSize(font, size);
        cb.setColorFill(color);
        cb.setTextMatrix(x, y);
        cb.showText(text);
        cb.endText();
    }

    private static String fit(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
