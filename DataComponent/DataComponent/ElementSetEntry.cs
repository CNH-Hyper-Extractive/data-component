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
    public class ElementSetEntry : DataSerializable
    {
        public ElementSetEntry()
        {
        }

        public ElementSetEntry(IElementSet elementSet)
        {
            ElementSetId = elementSet.ID;

            // pack the element id's into an array
            var ids = new String[elementSet.ElementCount];
            for (var i = 0; i < ids.Length; i++)
                ids[i] = elementSet.GetElementID(i);

            // serialize the array into a byte array
            DataBytes = ByteUtil.toByta(ids);
            DataLength = DataBytes.Length;
        }

        public String ElementSetId { get; set; }
        public byte[] DataBytes { get; set; }
        public int DataLength { get; set; }

        public void readData(IDataInput din)
        {
            ElementSetId = din.readUTF();
            DataLength = din.readInt();
            DataBytes = new byte[DataLength];
            din.readFully(DataBytes);
        }

        public void writeData(IDataOutput dout)
        {
            dout.writeUTF(ElementSetId);
            dout.writeInt(DataLength);
            dout.write(DataBytes);
        }

        public String toString()
        {
            return String.Format("{0}", ElementSetId);
        }
    }
}