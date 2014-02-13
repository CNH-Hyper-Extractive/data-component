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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TestApp {

    public static void main(String[] args) {
        testNwisService();
        //testKansasService();
    }

    private static void testNwisService() {
        String url = "http://river.sdsc.edu/wateroneflow/NWIS/DailyValues.asmx";
        String[] siteNames = new String[]{"NWISDV:06866500", "NWISDV:06866900", "NWISDV:06865500"};
        String variableName = "NWISDV:00060";
        Calendar beginDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        beginDate.set(2014, 0, 1, 0, 0);
        endDate.set(2014, 0, 5, 0, 0);
        testGetSites(url, WebServiceApi.API_10);
        testGetSite(url, siteNames[0], WebServiceApi.API_10);
        testGetValues(url, variableName, siteNames, WebServiceApi.API_10, beginDate, endDate, Calendar.DAY_OF_YEAR);
    }

    private static void testKansasService() {
        String url = "https://xxxx.beocat.cis.ksu.edu/wateroneflow/cuahsi_1_1.php";
        String[] siteNames = new String[]{"ODM:0001", "ODM:0002", "ODM:0003"};
        String variableName = "ODM:Precipitation";
        Calendar beginDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        beginDate.set(2010, 0, 1, 0, 0);
        endDate.set(2014, 0, 1, 0, 0);
        testGetSites(url, WebServiceApi.API_11);
        testGetSite(url, siteNames[0], WebServiceApi.API_11);
        testGetValues(url, variableName, siteNames, WebServiceApi.API_11, beginDate, endDate, Calendar.YEAR);
    }

    private static void testGetValues(String url, String variableName, String[] siteNames, String apiVersion, Calendar beginDate, Calendar endDate, int increment) {
        WaterOneFlow waterOneFlow = new WaterOneFlow(url, apiVersion);
        waterOneFlow.start();
        while (beginDate.getTime().compareTo(endDate.getTime()) < 0) {
            String date = formatDateForXml(beginDate.getTime());
            waterOneFlow.getValues(variableName, siteNames, date);
            beginDate.roll(increment, 1);
        }
        waterOneFlow.stop();
    }

    private static void testGetSites(String url, String apiVersion) {
        String xml = WebServiceApi.getSites(url, apiVersion);
        System.out.println(xml);
    }

    private static void testGetSite(String url, String siteName, String apiVersion) {
        String xml = WebServiceApi.getSite(url, siteName, apiVersion);
        System.out.println(xml);
    }

    private static String formatDateForXml(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(date.getTime());
    }
}
