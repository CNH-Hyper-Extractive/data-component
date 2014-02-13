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

package edu.kstate.datastore.webservice.simplerest;

import edu.kstate.datastore.data.ValueSetEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class SimpleXmlWriter {

    public static byte[] writeRequestForGetValues(String quantityId, String[] elementSetIds,
                                                  String timeStamp) throws IOException {

        ByteArrayOutputStream stream = new ByteArrayOutputStream(8192);

        // start the request
        stream.write("<valueSetRequest>".getBytes("UTF-8"));

        // add the variable name
        stream.write(String.format("<quantityId>%s</quantityId>", quantityId).getBytes("UTF-8"));

        // add the time
        stream.write(String.format("<dateTime>%s</dateTime>", timeStamp).getBytes("UTF-8"));

        // add the element location id's
        stream.write("<values>".getBytes("UTF-8"));
        for (int i = 0; i < elementSetIds.length; i++) {
            stream.write(String.format("<value locationId=\"%s\" />", elementSetIds[i]).getBytes("UTF-8"));
        }
        stream.write("</values>".getBytes("UTF-8"));

        // end the request
        stream.write("</valueSetRequest>".getBytes("UTF-8"));

        return stream.toByteArray();
    }

    public static byte[] writeRequestForSetValues(HashMap<String, String[]> elementIds, ArrayList<ValueSetEntry> entries) throws IOException {

        ByteArrayOutputStream stream = new ByteArrayOutputStream(8192);

        DecimalFormat decimalFormat = new DecimalFormat("000000.000");

        // serialize the value sets
        stream.write("<valueSets>".getBytes("UTF-8"));
        for (ValueSetEntry entry : entries) {

            // get the element ids corresponding to the values
            String[] elementIdArray = elementIds.get(entry.getElementSetId());

            // start the value set
            stream.write("<valueSet>".getBytes("UTF-8"));

            // add the time of the value set
            stream.write(String.format("<timeStamp>%s</timeStamp>", entry.getTimeStamp()).getBytes("UTF-8"));

            // add all the values
            stream.write("<values>".getBytes("UTF-8"));
            double[] values = entry.getValues();
            for (int i = 0; i < values.length; i++) {
                stream.write(String.format("<value locationId=\"%s\">%s</value>", elementIdArray[i], decimalFormat.format(values[i])).getBytes("UTF-8"));
            }
            stream.write("</values>".getBytes("UTF-8"));

            // end the value set
            stream.write("</valueSet>".getBytes("UTF-8"));
        }

        // end the value sets
        stream.write("</valueSets>".getBytes("UTF-8"));

        return stream.toByteArray();
    }
}
