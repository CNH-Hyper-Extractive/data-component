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

package edu.kstate.datastore.listeners;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import edu.kstate.datastore.data.WebServiceEntry;
import edu.kstate.datastore.util.Misc;

public class WebServiceEntryListener implements EntryListener<String, WebServiceEntry> {

	@Override
	public void entryAdded(EntryEvent<String, WebServiceEntry> event) {
		// only for debugging
		//WebServiceEntry entry = (WebServiceEntry) event.getValue();
		//Misc.logInfo(this.getClass(), "Added: " + entry.toString());
	}

	@Override
	public void entryEvicted(EntryEvent<String, WebServiceEntry> event) {
	}

	@Override
	public void entryRemoved(EntryEvent<String, WebServiceEntry> event) {
	}

	@Override
	public void entryUpdated(EntryEvent<String, WebServiceEntry> event) {
	}
}
