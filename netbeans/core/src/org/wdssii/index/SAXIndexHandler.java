package org.wdssii.index;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author lakshman
 */
public class SAXIndexHandler extends DefaultHandler {

    static class ElementHandler {

        private String elementName;
        private StringBuilder text = new StringBuilder();

        ElementHandler(String elementName) {
            this.elementName = elementName;
        }

        void handleCharacters(char[] str, int start, int len) {
            text.append(str, start, len);
        }

        String getText() {
            return text.toString();
        }
    }
    private Index index;
    private ElementHandler item;
    private ElementHandler time;
    private ElementHandler params;
    private ElementHandler selections;
    private ElementHandler[] all;
    private ElementHandler current;
    private String frac;

    private void reinit() {
        item = new ElementHandler("item");
        time = new ElementHandler("time");
        params = new ElementHandler("params");
        selections = new ElementHandler("selections");
        all = new ElementHandler[]{item, time, params, selections};
        current = null;
        frac = null;
    }

    public SAXIndexHandler(Index index) {
        this.index = index;
        reinit();
    }

    @Override
    public void characters(char[] buf, int start, int len) throws SAXException {
        if (current != null) {
            current.handleCharacters(buf, start, len);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {
        if (qualifiedName.equals("item")) {
            addRecord();
        }
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
        if (qualifiedName.equals("records")) {
            for (int i = 0; i < atts.getLength(); ++i) {
                handleRecordsAttribute(atts.getLocalName(i), atts.getValue(i));
            }
        }
        if (qualifiedName.equals("item")) {
            reinit();
        }

        // Make sure we're null so if there's no ElementHandler for this tag, 
        // we don't use the old current.
        current = null;
        for (ElementHandler e : all) {
            if (e.elementName.equals(qualifiedName)) {
                current = e;
            }
        }
        if (current == time) {
            frac = atts.getValue("fractional");
        }
    }

    /**
     * Over-ride this method if you want to handle the attributes of the enclosing 'records' element
     * @param localName
     * @param value
     */
    protected void handleRecordsAttribute(String localName, String value) {
        // nothing to do
    }

    private void addRecord() {
        String[] paramList = new String[]{params.getText()};
        String[] changes = new String[]{null};
        IndexRecord rec = IndexRecord.createIndexRecord(time.getText(), frac, paramList, changes, selections.getText(), index.getIndexLocation());
        index.addRecord(rec);
    }
}
