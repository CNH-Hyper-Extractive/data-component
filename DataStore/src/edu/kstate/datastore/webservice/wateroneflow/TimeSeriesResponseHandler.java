// -----------------------------------------------------------------------
//  Copyright (c) 2014 Tom Bulatewicz, Kansas State University
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in all
//  copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.
// -----------------------------------------------------------------------

package edu.kstate.datastore.webservice.wateroneflow;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TimeSeriesResponseHandler extends DefaultHandler {
    private StringBuilder xpath;
    private double value;

    public TimeSeriesResponseHandler() {
        this.xpath = new StringBuilder();
    }

    public double getValue() {
        return this.value;
    }

    private void addToXPath(String elementName) {
        xpath.append("/").append(elementName);
    }

    private void removeFromXPath(String elementName) throws SAXException {
        if (xpath.length() == 0)
            return;

        if (xpath.toString().endsWith(elementName) == false)
            throw new SAXException("XPath error: [" + xpath + "], [" + elementName + "]");

        // the +1 is for the leading slash
        xpath.delete(xpath.length() - (elementName.length() + 1), xpath.length());
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (length == 0)
            return;

        String s = new String(ch, start, length);
        final String xpathString = xpath.toString();
        if (xpathString.equals("/timeSeriesResponse/timeSeries/values/value") == true) {
            this.value = Double.parseDouble(s);
        }
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        addToXPath(qName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        removeFromXPath(qName);
    }
}
