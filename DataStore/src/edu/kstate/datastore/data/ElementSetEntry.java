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

package edu.kstate.datastore.data;

import com.hazelcast.nio.DataSerializable;
import edu.kstate.datastore.util.ByteUtil;
import edu.kstate.datastore.util.IElementSet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ElementSetEntry implements DataSerializable {
    private static final long serialVersionUID = 1L;
    private String elementSetId;
    private byte[] dataBytes;
    private int dataLength;

    /**
     * Required for serialization;
     */
    public ElementSetEntry() {
    }

    public ElementSetEntry(IElementSet elementSet) {
        this.elementSetId = elementSet.getID();

        // pack the element id's into an array
        String[] ids = new String[elementSet.getElementCount()];
        for (int i = 0; i < ids.length; i++)
            ids[i] = elementSet.getElementID(i);

        // serialize the array into a byte array
        this.dataBytes = ByteUtil.toByta(ids);
        this.dataLength = dataBytes.length;
    }

    public String getElementSetId() {
        return elementSetId;
    }

    public String[] getElementIds() {
        return ByteUtil.toStringA(this.dataBytes);
    }

    @Override
    public void readData(DataInput in) throws IOException {
        this.elementSetId = in.readUTF();
        this.dataLength = in.readInt();
        this.dataBytes = new byte[this.dataLength];
        in.readFully(this.dataBytes);
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        out.writeUTF(this.elementSetId);
        out.writeInt(this.dataLength);
        out.write(this.dataBytes);
    }

    @Override
    public String toString() {
        return String.format("%s", elementSetId);
    }
}
