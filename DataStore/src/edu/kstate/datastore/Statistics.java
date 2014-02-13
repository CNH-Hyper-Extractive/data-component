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

package edu.kstate.datastore;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Hashtable;

public class Statistics {
    private static Statistics instance;
    public String id;
    private Hashtable<String, ArrayList<Double>> statistics;

    private Statistics() {
        statistics = new Hashtable<String, ArrayList<Double>>();
        id = "0";
    }

    public static Statistics getInstance() {
        if (instance == null)
            instance = new Statistics();
        return instance;
    }

    public void add(String statistic, double value) {

        // find the arraylist for the given statistic
        ArrayList<Double> values = statistics.get(statistic);

        // create an array list to hold the values if necessary
        if (values == null) {
            values = new ArrayList<Double>();
            statistics.put(statistic, values);
        }

        // add the new value
        values.add(value);
    }

    private String generateReportItemForCsv(String name, ArrayList<Double> values) {
        double sum = 0;
        long count = 0;

        // don't use an iterator since it may cause a concurrent modification
        // exception
        //for (Double value : values) {
        int valueCount = values.size();
        for(int i=0; i<valueCount; i++) {
            Double value = values.get(i);
            if (value == null) {
                System.out.println("ERROR: NULL stat value in: " + name);
                continue;
            }

            sum += value;
            count++;
        }
        double average = 0;
        if (count > 0)
            average = sum / count;

        StringBuffer sb = new StringBuffer();
        sb.append(id);
        sb.append("," + name);
        sb.append(",").append(String.format("%.2f", sum));
        sb.append(",").append(String.format("%.2f", average));
        sb.append(",").append(count);
        return sb.toString();
    }

    public StringBuffer toCsv() throws Exception {
        StringBuffer sb = new StringBuffer();

        // don't just enumerate the keyset because any changes will
        // cause a concurrent modification exception
        String[] keySet = statistics.keySet().toArray(new String[0]);
        for (int i = 0; i < keySet.length; i++) {
            String key = keySet[i];
            ArrayList<Double> values = statistics.get(key);
            sb.append(generateReportItemForCsv(key, values)).append("\r\n");
        }

        return sb;
    }

    public void writeCsv(String filename) throws Exception {
        String csv = this.toCsv().toString();
        FileWriter fileWriter = new FileWriter(filename);
        BufferedWriter out = new BufferedWriter(fileWriter);
        out.write(csv);
        out.close();
    }
/*
    private String generateReportItemForXml(String name, ArrayList<Double> values)
	{
		double sum = 0;
		long count = 0;
		for (Double value : values)
		{
			sum += value;
			count++;
		}
		double average = 0;
		if (count > 0)
			average = sum / count;

		StringBuffer sb = new StringBuffer();
		sb.append("<Statistic name=\"").append(name).append("\" ");
		sb.append("sum=\"").append(String.format("%.2f", sum)).append("\" ");
		sb.append("mean=\"").append(String.format("%.2f", average)).append("\" ");
		sb.append("count=\"").append(count).append("\" />");
		return sb.toString();
	}

	public StringBuffer toXml() throws Exception
	{
		StringBuffer sb = new StringBuffer();

		sb.append("<Statistics>\r\n");
		for (String name : statistics.keySet())
		{
			ArrayList<Double> values = statistics.get(name);
			sb.append(generateReportItemForXml(name, values)).append("\r\n");
		}
		sb.append("</Statistics>\r\n");

		return sb;
	}
	*/
}
