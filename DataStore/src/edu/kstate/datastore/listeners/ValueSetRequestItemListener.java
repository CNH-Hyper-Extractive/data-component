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

import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import edu.kstate.datastore.Statistics;
import edu.kstate.datastore.data.ValueSetRequestEntry;

public class ValueSetRequestItemListener implements ItemListener<ValueSetRequestEntry> {

	@Override
	public void itemAdded(ItemEvent<ValueSetRequestEntry> item) {
		// only for debugging
		//Misc.log(this.getClass(), "Added: " + item.toString());
		Statistics.getInstance().add("ValueSetRequestItemAdded", 1);
	}

	@Override
	public void itemRemoved(ItemEvent<ValueSetRequestEntry> arg0) {
		Statistics.getInstance().add("ValueSetRequestItemRemoved", 1);
	}
}
