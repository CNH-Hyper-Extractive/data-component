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

using System;
using System.Collections.Generic;
using System.Text;

namespace KState.DataComponent
{
    public class Statistics
    {
        private readonly string id1;
        private readonly string id2;
        private readonly Dictionary<string, List<double>> statistics;

        public Statistics(string id1, string id2)
        {
            statistics = new Dictionary<string, List<double>>();
            this.id1 = id1;
            this.id2 = id2;
        }

        public void Add(string name, double value)
        {
            if (statistics.ContainsKey(name) == true)
            {
                // find the arraylist for the given statistic
                var values = statistics[name];
                values.Add(value);
            }
            else
            {
                // create an array list to hold the values
                var values = new List<double>();
                values.Add(value);
                statistics[name] = values;
            }
        }

        private String GenerateReportItemXml(string name, List<double> values)
        {
            double sum = 0;
            long count = 0;
            foreach (var value in values)
            {
                sum += value;
                count++;
            }
            double average = 0;
            if (count > 0)
                average = sum/count;

            var sb = new StringBuilder();
            sb.Append("<Statistic name=\"").Append(name).Append("\" ");
            sb.Append("sum=\"").Append(sum.ToString("F2")).Append("\" ");
            sb.Append("mean=\"").Append(average.ToString("F2")).Append("\" ");
            sb.Append("count=\"").Append(count).Append("\" />");
            return sb.ToString();
        }

        private String GenerateReportItemXml(string name, string value)
        {
            var sb = new StringBuilder();
            sb.Append("<Statistic name=\"").Append(name).Append("\" ");
            sb.Append("value=\"").Append(value).Append("\" />");
            return sb.ToString();
        }

        public StringBuilder ToXml()
        {
            var sb = new StringBuilder();

            sb.Append("<Statistics>\r\n");

            foreach (var name in statistics.Keys)
            {
                var values = statistics[name];
                sb.Append(GenerateReportItemXml(name, values)).Append("\r\n");
            }
            sb.Append("</Statistics>\r\n");

            return sb;
        }


        private String GenerateReportItemCsv(string name, List<double> values)
        {
            double sum = 0;
            long count = 0;
            foreach (var value in values)
            {
                sum += value;
                count++;
            }
            double average = 0;
            if (count > 0)
                average = sum/count;

            var sb = new StringBuilder();
            sb.Append(id1.Trim());
            sb.Append("," + id2.Trim());
            sb.Append("," + name);
            sb.Append("," + sum.ToString("F2"));
            sb.Append("," + average.ToString("F2"));
            sb.Append("," + count);
            return sb.ToString();
        }

        public StringBuilder ToCsv()
        {
            var sb = new StringBuilder();

            foreach (var name in statistics.Keys)
            {
                var values = statistics[name];
                sb.Append(GenerateReportItemCsv(name, values)).Append("\r\n");
            }

            return sb;
        }
    }
}