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

// This class is cross-platform serializable.
public class WebServiceEntry implements DataSerializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String type;
    private String description;
    private String url;
    private String serviceTimeMS;
    private String maxRequests;
    private String quantities;

    /**
     * Required for serialization;
     */
    public WebServiceEntry() {
    }

    public WebServiceEntry(String id, String type, String description, String url, String serviceTimeMS, String maxRequests,
                           String quantities) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.url = url;
        this.serviceTimeMS = serviceTimeMS;
        this.maxRequests = maxRequests;
        this.quantities = quantities;
    }

    public String toString() {
        return id + ":" + type + ":" + url + ":" + quantities;
    }

    public String getId() {
        return this.id;
    }
    public String getType() {
        return this.type;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public long getServiceTimeMS() {
        return Long.parseLong(this.serviceTimeMS);
    }

    public void setServiceTimeMS(String value) {
        this.serviceTimeMS = value;
    }

    public String getMaxRequests() {
        return this.maxRequests;
    }

    public void setMaxRequests(String value) {
        this.maxRequests = value;
    }

    public String getQuantities() {
        return this.quantities;
    }

    public void setQuantities(String value) {
        this.quantities = value;
    }

    @Override
    public void readData(DataInput in) throws IOException {
        this.id = in.readUTF();
        this.type = in.readUTF();
        this.description = in.readUTF();
        this.url = in.readUTF();
        this.serviceTimeMS = in.readUTF();
        this.maxRequests = in.readUTF();
        this.quantities = in.readUTF();
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        out.writeUTF(this.id);
        out.writeUTF(this.type);
        out.writeUTF(this.description);
        out.writeUTF(this.url);
        out.writeUTF(this.serviceTimeMS);
        out.writeUTF(this.maxRequests);
        out.writeUTF(this.quantities);
    }
}
