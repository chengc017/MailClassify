/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
//import org.apache.poi.hslf.extractor.PowerPointExtractor;
//import org.apache.poi.hssf.extractor.ExcelExtractor;
//import org.apache.poi.hwpf.extractor.WordExtractor;
//import org.apache.poi.poifs.filesystem.POIFSFileSystem;
//import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class TextStriper {

//    private static PDFTextStripper stripper;
//
//    static {
//        try {
//            stripper = new PDFTextStripper();
//            stripper.setLineSeparator("\n");
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
    
    public static String extractOpenDocument(InputStream in) throws IOException,
            ParserConfigurationException, SAXException {
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(false);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            ZipInputStream zis = new ZipInputStream(in);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null && !ze.getName().equals("content.xml")) {
                ze = zis.getNextEntry();
            }
            
            xmlReader.setContentHandler(contentHandler);
            try {
                xmlReader.parse(new InputSource(zis));
            } finally {
                zis.close();
            }
            return contentHandler.getContent();
        } finally {
            in.close();
        }
    }
    
    private static OpenOfficeContentHandler contentHandler =
                    new OpenOfficeContentHandler();

    private static class OpenOfficeContentHandler extends DefaultHandler {

        private StringBuffer content;
        private boolean appendChar;

        public OpenOfficeContentHandler() {
            content = new StringBuffer();
            appendChar = false;
        }

        /**
         * Returns the text content extracted from parsed content.xml
         */
        public String getContent() {
            return content.toString();
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                String rawName, Attributes atts)
                throws SAXException {
            if (rawName.startsWith("text:")) {
                appendChar = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (appendChar) {
                content.append(ch, start, length).append(" ");
            }
        }

        @Override
        public void endElement(java.lang.String namespaceURI,
                java.lang.String localName,
                java.lang.String qName)
                throws SAXException {
            appendChar = false;
        }
    }
    
    public static String extractHTML(String html) throws IOException {
        Document doc = Jsoup.parse(html);
        doc = doc.normalise();
        String text = doc.text();
        return text;
    }

//    public static String extractPDF(InputStream in) throws IOException {
//        String text = "";
//        PDDocument document = PDDocument.load(in);
//        try {
//            PDDocumentInformation pdfinfo = document.getDocumentInformation();
//            text += Stringer.simplifica(pdfinfo.getAuthor()) + "\n";
//            text += Stringer.simplifica(pdfinfo.getTitle()) + "\n";
//            text += Stringer.simplifica(pdfinfo.getSubject()) + "\n";
//            StringWriter writer = new StringWriter();
//            stripper.writeText(document, writer);
//            writer.flush();
//            text += writer.toString();
//        } finally {
//            document.close();
//            in.close();
//            return text;
//        }
//    }

//    public static String extractMSWord(InputStream in)
//            throws IOException, Exception {
//        try {
//            WordExtractor extractor = new WordExtractor(in);
//            String text = "";
//            for (String paragraph : extractor.getParagraphText()) {
//                text += paragraph + "\n";
//            }
//            return text;
//        } catch (Exception exception) {
//            String message = exception.getMessage();
//            if (message.equals("The document is really a RTF file")) {
//                in.reset();
//                return extractRTF(in);
//            } else if (message.startsWith("The supplied data appears to be in the Office 2007+ XML.")) {
//                in.reset();
//                return extractMSExcel2007(in);
//            } else {
//                throw exception;
//            }
//        } finally {
//            in.close();
//        }
//    }
    
//    public static String extractMSExcel(InputStream in) throws IOException {
//        try {
//            POIFSFileSystem fs = new POIFSFileSystem(in);
//            ExcelExtractor extractor = new ExcelExtractor(fs);
//            extractor.setFormulasNotResults(true);
//            extractor.setIncludeSheetNames(false);
//            String text = extractor.getText();
//            return text;
//        } finally {
//            in.close();
//        }
//    }
    
//    public static String extractMSPowerPoint(InputStream in)
//            throws IOException, Exception {
//        try {
//            PowerPointExtractor extractor = new PowerPointExtractor(in);
//            String text = extractor.getNotes()  + "\n";
//            text += extractor.getText();
//            return text;
//        } finally {
//            in.close();
//        }
//    }

//    public static String extractMSExcel2007(InputStream in) throws IOException {
//        try {
//            XSSFWorkbook wb = new XSSFWorkbook(in);
//            XSSFExcelExtractor extractor = new XSSFExcelExtractor(wb);
//            extractor.setFormulasNotResults(true);
//            extractor.setIncludeSheetNames(false);
//            String text = extractor.getText();
//            return text;
//        } finally {
//            in.close();
//        }
//    }
    
    public static String extractInputStream(InputStream in) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            int code;
            while ((code = in.read()) != -1) {
                writer.write(code);
            }
            String text = writer.toString();
            return text;
        } finally {
            in.close();
        }
    }
    
    public static String extractXML(String xml)
            throws ParserConfigurationException, SAXException, IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
        String text = extractXML(in);
        in.close();
        return text;
    }
    
        
    public static String extractRTF(InputStream is) throws Exception {
        try {
            DefaultStyledDocument styledDoc = new DefaultStyledDocument();
            new RTFEditorKit().read(is, styledDoc, 0);
            return styledDoc.getText(0, styledDoc.getLength());
        } finally {
            is.close();
        }
    }
    
    public static String extractXML(InputStream in)
            throws ParserConfigurationException, SAXException, IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuild = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuild.parse(in);
            Element parentNode = doc.getDocumentElement();
            String text = extractTextChildren(parentNode);
            return text;
        } finally {
            in.close();
        }
    }

    private static String extractTextChildren(Node parentNode) {
        NodeList childNodes = parentNode.getChildNodes();
        String result = new String();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                result += node.getNodeValue() + "\n";
            }
            result += extractTextChildren(node);
        }
        return result;
    }
    
    public static String getText(Message message)
            throws IOException, MessagingException {
        Object content = message.getContent();
        String contentType = message.getContentType();
        return getText(content, contentType);
    }
    
    private static String getText(Object content, String contentType)
            throws MessagingException, IOException {
        contentType = contentType.toLowerCase();
        if (content instanceof String) {
            if (contentType.startsWith("text/plain")) {
                String text = (String) content;
                return text;
            } else if (contentType.startsWith("text/html")) {
                String html = (String) content;
                String text = extractHTML(html);
                return text;
            } else {
                return "";
            }
        } else if (content instanceof MimeMessage) {
            try {
                MimeMessage mimeMessage = (MimeMessage) content;
                content = mimeMessage.getContent();
                contentType = mimeMessage.getContentType();
                String text = getText(content, contentType);
                return text;
            } catch (MessagingException exception) {
                return "";
            }
        } else if (content instanceof MimeMultipart) {
            MimeMultipart mimeMultipart = (MimeMultipart) content;
            String text = "";
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                try {
                    BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                    content = bodyPart.getContent();
                    contentType = bodyPart.getContentType();
                    text += getText(content, contentType) + "\n";
                } catch (Exception exception) {
                }
            }
            return text;
        } else if (content instanceof InputStream) {
            InputStream inputStream = (InputStream) content;
            if (contentType.startsWith("text/html")) {
                String html = extractInputStream(inputStream);
                String text = extractHTML(html);
                return text;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }
}
