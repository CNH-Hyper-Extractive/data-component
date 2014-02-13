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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ValueSetRequestEntry implements DataSerializable {
    private static final long serialVersionUID = 1L;
    private String webServiceId;
    private String quantityId;
    private String elementSetId;
    private String scenarioId;
    private String timeStamp;
    private boolean isFetching;

    /**
     * Required for serialization;
     */
    public ValueSetRequestEntry() {

    }

    public ValueSetRequestEntry(String webServiceId, String quantityId, String elementSetId, String timeStamp, String scenarioId) {
        this.webServiceId = webServiceId;
        this.quantityId = quantityId;
        this.elementSetId = elementSetId;
        this.timeStamp = timeStamp;
        this.scenarioId = scenarioId;
    }

    public static String createKey(String webServiceId, String quantityId, String elementSetId, String timeStamp, String scenarioId) {
        return String.format("{%s}{%s}{%s}{%s}{%s}", webServiceId, quantityId, elementSetId, timeStamp, scenarioId);
    }

    public static String createKey(ValueSetRequestEntry entry) {
        return createKey(entry.getWebServiceId(), entry.getQuantityId(), entry.getElementSetId(), entry.getTimeStamp(), entry.getScenarioId());
    }

    public String getWebServiceId() {
        return this.webServiceId;
    }

    public String getQuantityId() {
        return this.quantityId;
    }

    public String getElementSetId() {
        return this.elementSetId;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public String getScenarioId() {
        return this.scenarioId;
    }

    public boolean getIsFetching() {
        return this.isFetching;
    }

    public void setIsFetching(boolean isFetching) {
        this.isFetching = isFetching;
    }

    @Override
    public void readData(DataInput in) throws IOException {
        this.webServiceId = in.readUTF();
        this.quantityId = in.readUTF();
        this.elementSetId = in.readUTF();
        this.timeStamp = in.readUTF();
        this.scenarioId = in.readUTF();
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        out.writeUTF(this.webServiceId);
        out.writeUTF(this.quantityId);
        out.writeUTF(this.elementSetId);
        out.writeUTF(this.timeStamp);
        out.writeUTF(this.scenarioId);
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s:%s:%s", this.webServiceId, this.quantityId, this.elementSetId, this.timeStamp, this.scenarioId);
    }
}
