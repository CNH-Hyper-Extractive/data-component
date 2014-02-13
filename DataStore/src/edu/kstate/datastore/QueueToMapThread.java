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

package edu.kstate.datastore;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import edu.kstate.datastore.data.ValueSetEntry;
import edu.kstate.datastore.util.Misc;

import java.util.concurrent.TimeUnit;

public class QueueToMapThread extends Thread {

    private HazelcastInstance instance;
    private long maxLocalValueSetMapCostB;
    private boolean stopRequested;

    public QueueToMapThread(HazelcastInstance instance, long maxLocalValueSetMapCostB) {
        this.instance = instance;
        this.maxLocalValueSetMapCostB = maxLocalValueSetMapCostB;
    }

    public void requestStop() {
        Misc.logInfo(this.getClass(), "Stop Requested");
        this.stopRequested = true;
    }

    private void eventLoop() throws Exception {

        IMap<String, ValueSetEntry> mapValueSet = this.instance.getMap("valueSet");
        IQueue<ValueSetEntry> queueValueSet = this.instance.getQueue("valueSet");

        while (true) {

            if (stopRequested == true) {
                int entriesInQueue = queueValueSet.size();
                if(entriesInQueue > 0) {
                    Misc.logInfo(this.getClass(), "Warning: Stopping with " + entriesInQueue + " left in queue");
                }
                break;
                /*if (entriesInQueue == 0) {
                    Misc.logInfo(this.getClass(), "Stop requested and queue is empty");
                    break;
                } else {
                    Misc.logInfo(this.getClass(), "Stop requested but " + entriesInQueue + " left in queue");
                }*/
            }

            // if the valueset map is full then don't move anything into it
            if (Misc.maxDataExceeded(this.maxLocalValueSetMapCostB, mapValueSet) == true) {
                Misc.logInfo(this.getClass(), "No more space in map, paused moving from queue");
                Thread.sleep(1000);
                continue;
            }

            // remove the next item from the queue and pause if there are no
            // entries in the queue
            // BLOCKING - up to one minute (so that we can check for stop)
            ValueSetEntry nextEntry = queueValueSet.poll(60, TimeUnit.SECONDS);

            // polling
/*            ValueSetEntry nextEntry = queueValueSet.poll();
            if (nextEntry == null) {
                Thread.sleep(1000);
                continue;
            }
*/
            // move the entry from the queue into the map
            if(nextEntry != null) {
                mapValueSet.put(ValueSetEntry.createKey(nextEntry), nextEntry);
            }
            else
            {
                Misc.logInfo(this.getClass(), "Queue is empty");
                //Thread.sleep(100);
            }
            //Misc.logInfo(this.getClass(), "Queue > Map: " + nextEntry.toString());

//            Thread.sleep(10);
        }
    }

    public void run() {
        try {
            eventLoop();
        } catch (Exception e) {
            Misc.logException(this.getClass(), e);
        }
        Misc.logInfo(this.getClass(), "Stopped");
    }
}
