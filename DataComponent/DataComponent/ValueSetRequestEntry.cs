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
using Hazelcast.IO;
using KState.Util;
using OpenMI.Standard;

namespace KState.DataComponent
{
    // This class is cross-platform serializable
    public class ValueSetRequestEntry : DataSerializable
    {
        public ValueSetRequestEntry()
        {
        }

        public ValueSetRequestEntry(string webServiceId, string quantityId, string elementSetId, ITime timeStamp, string scenarioId)
            : this(webServiceId, quantityId, elementSetId, Utils.FormatDateForXml(Utils.ITimeToDateTime(timeStamp)), scenarioId)
        {
        }

        public ValueSetRequestEntry(string webServiceId, string quantityId, string elementSetId, DateTime timeStamp, string scenarioId)
            : this(webServiceId, quantityId, elementSetId, Utils.FormatDateForXml(timeStamp), scenarioId)
        {
        }

        public ValueSetRequestEntry(string webServiceId, string quantityId, string elementSetId, string timeStamp, string scenarioId)
        {
            WebServiceId = webServiceId;
            QuantityId = quantityId;
            ElementSetId = elementSetId;
            TimeStamp = timeStamp;
            ScenarioId = scenarioId;
        }

        public string WebServiceId { get; set; }
        public string QuantityId { get; set; }
        public string ElementSetId { get; set; }
        public string TimeStamp { get; set; }
        public string ScenarioId { get; set; }

        public void readData(IDataInput din)
        {
            WebServiceId = din.readUTF();
            QuantityId = din.readUTF();
            ElementSetId = din.readUTF();
            TimeStamp = din.readUTF();
            ScenarioId = din.readUTF();
        }

        public void writeData(IDataOutput dout)
        {
            dout.writeUTF(WebServiceId);
            dout.writeUTF(QuantityId);
            dout.writeUTF(ElementSetId);
            dout.writeUTF(TimeStamp);
            dout.writeUTF(ScenarioId);
        }

        public string toString()
        {
            return string.Format("{0}:{1}:{2}:{3}:{4}", WebServiceId, QuantityId, ElementSetId, TimeStamp, ScenarioId);
        }
    }
}