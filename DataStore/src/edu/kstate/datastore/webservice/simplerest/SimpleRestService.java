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

import edu.kstate.datastore.Statistics;
import edu.kstate.datastore.data.ValueSetEntry;
import edu.kstate.datastore.util.Misc;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

// http://localhost/ObservationsService.php
// http://10.5.0.50/ObservationsService.php

public class SimpleRestService {

    public static void setValuesSync(HashMap<String, String[]> elementIds, String url, ArrayList<ValueSetEntry> entries) {
        try {
            byte[] request = SimpleXmlWriter.writeRequestForSetValues(elementIds, entries);
            callWebService(url, request, "SetValues");
        } catch (Exception e) {
            Misc.logException(SimpleRestService.class, e);
        }
    }

    public static double[] getValuesSync(String url, String quantityId, String elementSetIds[], String timeStamp) {
        try {
            byte[] request = SimpleXmlWriter.writeRequestForGetValues(quantityId, elementSetIds,
                    timeStamp);
            SimpleXmlHandler xmlHandler = callWebService(url, request, "GetValues");
            return xmlHandler.getValues();
        } catch (Exception e) {
            Misc.logException(SimpleRestService.class, e);
            return null;
        }
    }

    private static SimpleXmlHandler callWebService(String url, byte[] request, String serviceName) {

        SimpleXmlHandler xmlHandler = null;
        HttpURLConnection httpURLConnection = null;
        InputStream connectionInputStream = null;
        OutputStream connectionOutputStream = null;

        try {

            // create a connection to the URL
            URL connectUrl = new URL(url);
            httpURLConnection = (HttpURLConnection) connectUrl.openConnection();

            // configure the request
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("POST");

            // enable chunked streaming mode so that the the data is streamed to
            // the web service instead of loaded into memory and sent at once
            httpURLConnection.setChunkedStreamingMode(16000);

            // set the request properties
            httpURLConnection.setRequestProperty("Keep-Alive", "close");
            httpURLConnection.setRequestProperty("Connection", "close");
            httpURLConnection.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Configuration/CLDC-1.0");
            httpURLConnection.setRequestProperty("Content-Language", "en-CA");
            httpURLConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            httpURLConnection.setRequestProperty("SOAPAction", "http://tempuri.org/" + serviceName);
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(request.length));

            // open an output stream
            connectionOutputStream = httpURLConnection.getOutputStream();

            // measure how long it takes to send the data
            long startSendMs = System.currentTimeMillis();

            // create a stream so we can stream the request to the server
            ByteArrayInputStream requestStream = new ByteArrayInputStream(request);

            // stream the request to the server
            int len;
            byte[] buffer = new byte[16000];
            while (-1 != (len = requestStream.read(buffer))) {
                connectionOutputStream.write(buffer, 0, len);
                connectionOutputStream.flush();
            }
            connectionOutputStream.flush();

            // get the response code from the server
            int rc = httpURLConnection.getResponseCode();

            // measure how long it takes to send the data
            long stopSendMs = System.currentTimeMillis();

            // throw the error here so that we get the error message
            if (rc != 200) {
                throw new IOException("Http Error:" + rc);
            }

            // measure how long it takes to receive the response
            long startReceiveMs = System.currentTimeMillis();

            // get the input stream
            connectionInputStream = httpURLConnection.getInputStream();

            // write the response to a byte array
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream(8092);
            while (-1 != (len = connectionInputStream.read(buffer)))
                responseStream.write(buffer, 0, len);
            byte[] response = responseStream.toByteArray();
            responseStream.close();

            //String s0 = new String(request);
            //String s1 = new String(response);

            //Misc.logInfo(SimpleRestService.class, s1);

            // measure how long it takes to receive the response
            long stopReceiveMs = System.currentTimeMillis();

            // measure how long it takes to parse the response
            long startParseMs = System.currentTimeMillis();

            // create the handler and input source
            xmlHandler = new SimpleXmlHandler();
            InputSource source = new InputSource();
            source.setByteStream(new ByteArrayInputStream(response));

            // create the reader and begin parsing
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(xmlHandler);
            reader.setErrorHandler(xmlHandler);
            reader.parse(source);

            // measure how long it takes to parse the response
            long stopParseMs = System.currentTimeMillis();

            // record the number of bytes in the web service request
            Statistics.getInstance().add(String.format("Web-%s-DataSent-Byte", serviceName), request.length);

            // record the number of bytes in the web service response
            Statistics.getInstance().add(String.format("Web-%s-DataReceived-Byte", serviceName), response.length);

            // record the request and response sum
            Statistics.getInstance().add(String.format("Web-%s-Data-Byte", serviceName),
                    request.length + response.length);

            Statistics.getInstance().add(String.format("Web-%s-TimeSend-MS", serviceName), stopSendMs - startSendMs);

            Statistics.getInstance().add(String.format("Web-%s-TimeReceive-MS", serviceName),
                    stopReceiveMs - startReceiveMs);

            Statistics.getInstance().add(String.format("Web-%s-Time-MS", serviceName), stopReceiveMs - startSendMs);

            Statistics.getInstance().add(String.format("Web-%s-TimeParse-MS", serviceName), stopParseMs - startParseMs);

            Statistics.getInstance().add(String.format("Web-%s-TimeService-MS", serviceName),
                    xmlHandler.getServiceTime());

        } catch (Exception e) {
            Misc.logException(SimpleRestService.class, e);
        } finally {

            // close the streams
            try {
                if (connectionInputStream != null)
                    connectionInputStream.close();
                if (connectionOutputStream != null)
                    connectionOutputStream.close();
                if (httpURLConnection != null)
                    httpURLConnection.disconnect();

            } catch (IOException e) {
                // we don't care if we fail to close these
            }
        }

        return xmlHandler;
    }
}
