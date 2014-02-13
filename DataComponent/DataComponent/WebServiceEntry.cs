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

namespace KState.DataComponent
{
    // This class is cross-platform serializable
    public class WebServiceEntry : DataSerializable
    {
        public WebServiceEntry()
        {
        }

        public WebServiceEntry(string id, string type, string description, String url, String processingTime, String maxRequests, String quantities)
        {
            Id = id;
            Type = type;
            Description = description;
            Url = url;
            ProcessingTime = processingTime;
            MaxRequests = maxRequests;
            Quantities = quantities;
        }

        public String Id { get; set; }
        public String Type { get; set; }
        public String Description { get; set; }
        public String Url { get; set; }
        public String ProcessingTime { get; set; }
        public String MaxRequests { get; set; }
        public String Quantities { get; set; }

        public void readData(IDataInput din)
        {
            Id = din.readUTF();
            Type = din.readUTF();
            Description = din.readUTF();
            Url = din.readUTF();
            ProcessingTime = din.readUTF();
            MaxRequests = din.readUTF();
            Quantities = din.readUTF();
        }

        public void writeData(IDataOutput dout)
        {
            dout.writeUTF(Id);
            dout.writeUTF(Type);
            dout.writeUTF(Description);
            dout.writeUTF(Url);
            dout.writeUTF(ProcessingTime);
            dout.writeUTF(MaxRequests);
            dout.writeUTF(Quantities);
        }

        public override String ToString()
        {
            return string.Format("{0}:{1}:{2}:{3}", Id, Type, Url, ProcessingTime);
        }
    }
}