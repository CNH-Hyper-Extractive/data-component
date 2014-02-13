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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ValueSetEntry implements DataSerializable {
    private static final long serialVersionUID = 1L;
    private String webServiceId;
    private String quantityId;
    private String timeStamp;
    private String elementSetId;
    private String scenarioId;
    private boolean needsUpload;
    private byte[] dataBytes;
    private int dataLength;

    /**
     * Required for serialization;
     */
    public ValueSetEntry() {
    }

    public ValueSetEntry(String webServiceId, String quantityId, String timeStamp, String elementSetId, String scenarioId, double[] values) {
        this.webServiceId = webServiceId;
        this.quantityId = quantityId;
        this.timeStamp = timeStamp;
        this.elementSetId = elementSetId;
        this.scenarioId = scenarioId;
        this.needsUpload = false;
        this.dataBytes = ByteUtil.toByta(values);
        this.dataLength = dataBytes.length;
    }

    public static String createKey(ValueSetEntry entry) {
        return createKey(entry.getWebServiceId(), entry.getQuantityId(), entry.getElementSetId(), entry.getTimeStamp(), entry.getScenarioId());
    }

    public static String createKey(String webServiceId, String quantityId, String elementSetId, String timeStamp, String scenarioId) {
        return String.format("%s%s%s%s%s", webServiceId, quantityId, elementSetId, timeStamp, scenarioId);
    }

    public String getWebServiceId() {
        return webServiceId;
    }

    public String getQuantityId() {
        return quantityId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getElementSetId() {
        return elementSetId;
    }

    public String getScenarioId() {
        return this.scenarioId;
    }

    public double[] getValues() {
        return ByteUtil.toDoubleA(this.dataBytes);
    }

    public boolean getNeedsUpload() {
        return this.needsUpload;
    }

    public void setNeedsUpload(boolean value) {
        this.needsUpload = value;
        ;
    }

    @Override
    public void readData(DataInput in) throws IOException {
        this.webServiceId = in.readUTF();
        this.quantityId = in.readUTF();
        this.timeStamp = in.readUTF();
        this.elementSetId = in.readUTF();
        this.scenarioId = in.readUTF();
        this.needsUpload = in.readBoolean();
        this.dataLength = in.readInt();
        this.dataBytes = new byte[this.dataLength];
        in.readFully(this.dataBytes);
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        out.writeUTF(this.webServiceId);
        out.writeUTF(this.quantityId);
        out.writeUTF(this.timeStamp);
        out.writeUTF(this.elementSetId);
        out.writeUTF(this.scenarioId);
        out.writeBoolean(this.needsUpload);
        out.writeInt(this.dataLength);
        out.write(this.dataBytes);
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s:%s:%s:%d:%s", webServiceId, quantityId, timeStamp, elementSetId, scenarioId, dataLength, needsUpload == true ? "true" : "false");
    }
}
