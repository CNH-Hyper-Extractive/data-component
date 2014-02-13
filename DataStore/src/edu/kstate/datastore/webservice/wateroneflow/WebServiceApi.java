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

import edu.kstate.datastore.Statistics;
import edu.kstate.datastore.webservice.TrustModifier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class WebServiceApi {

    public static final String API_10 = "1.0";
    public static final String API_11 = "1.1";

    public static String getSites(String url, String apiVersion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<GetSites xmlns=\"http://www.cuahsi.org/his/" + apiVersion + "/ws/\">");
        sb.append("<site></site>");
        sb.append("<authToken></authToken>");
        sb.append("</GetSites>");
        byte[] request = createSoapEnvelope(sb.toString()).getBytes();
        byte[] response = callWebService(url, request, "http://www.cuahsi.org/his/" + apiVersion + "/ws/GetSites");
        return new String(response);
    }

    public static String getSite(String url, String siteName, String apiVersion) {
        String serviceName = null;
        StringBuilder sb = new StringBuilder();
        if (apiVersion.equals(API_10) == true) {
            sb.append("<GetSiteInfoObject xmlns=\"http://www.cuahsi.org/his/1.0/ws/\">");
            sb.append("<site>" + siteName + "</site>");
            sb.append("<authToken></authToken>");
            sb.append("</GetSiteInfoObject>");
            serviceName = "http://www.cuahsi.org/his/1.0/ws/GetSiteInfoObject";
        }
        if (apiVersion.equals(API_11) == true) {
            sb.append("<GetSitesObject xmlns=\"http://www.cuahsi.org/his/1.1/ws/\">");
            sb.append("<site><string>" + siteName + "</string></site>");
            sb.append("<authToken></authToken>");
            sb.append("</GetSitesObject>");
            serviceName = "http://www.cuahsi.org/his/1.1/ws/GetSitesObject";
        }
        byte[] request = createSoapEnvelope(sb.toString()).getBytes();
        byte[] response = callWebService(url, request, serviceName);
        return new String(response);
    }

    public static String getValues(String url, String variableName, String locationName, String timeStamp, String apiVersion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<GetValues xmlns=\"http://www.cuahsi.org/his/" + apiVersion + "/ws/\">");
        sb.append("<location>" + locationName + "</location>");
        sb.append("<variable>" + variableName + "</variable>");
        sb.append("<startDate>" + timeStamp + "</startDate>");
        sb.append("<endDate>" + timeStamp + "</endDate>");
        sb.append("<authToken></authToken>");
        sb.append("</GetValues>");
        byte[] request = createSoapEnvelope(sb.toString()).getBytes();
        byte[] response = callWebService(url, request, "http://www.cuahsi.org/his/" + apiVersion + "/ws/GetValues");
        return new String(response);
    }

    private static String createSoapEnvelope(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        sb.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        sb.append("<soap:Body>");
        sb.append(content);
        sb.append("</soap:Body>");
        sb.append("</soap:Envelope>");
        return sb.toString();
    }

    /**
     * Returns the web service response as a byte array or returns null if there
     * was an error.
     */
    public static byte[] callWebService(String url, byte[] request, String soapAction) {

        long startSendMs = System.currentTimeMillis();

        byte[] response = null;
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

            httpURLConnection.setConnectTimeout(5*1000);
            httpURLConnection.setReadTimeout(5*1000);

            // set the request properties
            httpURLConnection.setRequestProperty("Keep-Alive", "close");
            httpURLConnection.setRequestProperty("Connection", "close");
            httpURLConnection.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Configuration/CLDC-1.0");
            httpURLConnection.setRequestProperty("Content-Language", "en-us");
            httpURLConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            httpURLConnection.setRequestProperty("SOAPAction", soapAction);
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(request.length));

            TrustModifier.relaxHostChecking(httpURLConnection);

            // open an output stream
            connectionOutputStream = httpURLConnection.getOutputStream();

            // create a stream so we can stream the request to the server
            ByteArrayInputStream requestStream = new ByteArrayInputStream(request);

            // stream the request to the server
            int len;
            byte[] buffer = new byte[16184];
            while (-1 != (len = requestStream.read(buffer))) {
                connectionOutputStream.write(buffer, 0, len);
                connectionOutputStream.flush();
            }
            connectionOutputStream.flush();

            // get the response code from the server
            int rc = httpURLConnection.getResponseCode();

            // get the input stream
            connectionInputStream = httpURLConnection.getInputStream();

            // write the response to a byte array
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream(8092);
            while (-1 != (len = connectionInputStream.read(buffer))) {
                responseStream.write(buffer, 0, len);
            }
            response = responseStream.toByteArray();
            responseStream.close();

            if (rc == 301) {
                Map<String, List<String>> fields = httpURLConnection.getHeaderFields();
                for (String nextHeader : fields.keySet()) {
                    System.out.println(nextHeader + " = " + fields.get(nextHeader));
                }
            }

            if (rc != 200) {
                throw new Exception("HTTP ERROR");
            }

            long stopSendMs = System.currentTimeMillis();
            String serviceName = "Http";

            Statistics.getInstance().add(String.format("Web-%s-Time-MS", serviceName), stopSendMs - startSendMs);
            Statistics.getInstance().add(String.format("Web-%s-Data-Byte", serviceName),
                    request.length + response.length);

        } catch (Exception e) {
            System.out.println(e.getMessage());
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

        return response;
    }
}
