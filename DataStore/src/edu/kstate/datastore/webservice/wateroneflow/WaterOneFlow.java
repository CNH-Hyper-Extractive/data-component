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
import edu.kstate.datastore.webservice.ServiceAdapter;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WaterOneFlow implements ServiceAdapter {

    private ThreadPoolExecutor threadPool;
    private String url;
    private String apiVersion;
    private int activeCallCount;
    double[] values;

    public WaterOneFlow(String url, String apiVersion) {
        this.url = url;
        this.apiVersion = apiVersion;
    }

    public void start() {
        int poolSize = 25;
        this.threadPool = new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public void stop() {
        threadPool.shutdown();
    }

    private synchronized void updateActiveCallCount(int value) {
        this.activeCallCount += value;
    }

    public double[] getValues(final String variableName, final String[] siteNames, final String timeStamp) {

        long startSendMs = System.currentTimeMillis();

        this.activeCallCount = 0;
        this.values = new double[siteNames.length];

        for (int i = 0; i < siteNames.length; i++) {
            final int index = i;
            final String siteName = siteNames[i];
            updateActiveCallCount(1);
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    getValuesForSite(index, siteName, variableName, timeStamp, apiVersion);
                }
            };
            threadPool.submit(task);
        }

        while (this.activeCallCount > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
            if (this.activeCallCount > 0) {
                System.out.println("Waiting..(" + this.activeCallCount + ")");
            }
        }

        long stopSendMs = System.currentTimeMillis();
        Statistics.getInstance().add(String.format("Web-GetValues-Time-MS"), stopSendMs - startSendMs);

        System.out.print(variableName + ", " + timeStamp + ": ");
        for (double nextValue : values) {
            System.out.print(nextValue + ", ");
        }
        System.out.println();

        return values;
    }

    private void getValuesForSite(int index, String siteName, String variableName, String timeStamp, String version) {
        try {
            String responseXml = null;
            boolean success = false;

            // we keep retrying indefinitely (could place a limit on the number of attempts here)
            while (success == false) {
                try {
                    responseXml = WebServiceApi.getValues(url, variableName, siteName, timeStamp, version);
                    //System.out.println(responseXml);
                    success = true;
                } catch (Exception e) {
                    System.out.println(e);
                    success = false;
                }
            }

            // get the xml response inside the getvalues response
            GetValuesResponseHandler getValuesResponseHandler = new GetValuesResponseHandler();
            InputSource getValuesResponseSource = new InputSource();
            getValuesResponseSource.setByteStream(new ByteArrayInputStream(responseXml.getBytes()));
            XMLReader getValuesResponseReader = XMLReaderFactory.createXMLReader();
            getValuesResponseReader.setContentHandler(getValuesResponseHandler);
            getValuesResponseReader.setErrorHandler(getValuesResponseHandler);
            getValuesResponseReader.parse(getValuesResponseSource);
            String timeSeriesResponseXml = getValuesResponseHandler.getTimeSeriesXml();

            // parse the value from the time series response
            TimeSeriesResponseHandler timeSeriesResponseHandler = new TimeSeriesResponseHandler();
            InputSource timeSeriesResponseSource = new InputSource();
            timeSeriesResponseSource.setByteStream(new ByteArrayInputStream(timeSeriesResponseXml.getBytes()));
            XMLReader timeSeriesResponseReader = XMLReaderFactory.createXMLReader();
            timeSeriesResponseReader.setContentHandler(timeSeriesResponseHandler);
            timeSeriesResponseReader.setErrorHandler(timeSeriesResponseHandler);
            timeSeriesResponseReader.parse(timeSeriesResponseSource);
            double value = timeSeriesResponseHandler.getValue();

            values[index] = value;

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        updateActiveCallCount(-1);
    }
}
