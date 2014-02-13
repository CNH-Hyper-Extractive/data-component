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
import com.hazelcast.core.MapEntry;
import edu.kstate.datastore.data.ElementSetEntry;
import edu.kstate.datastore.data.ValueSetEntry;
import edu.kstate.datastore.data.WebServiceEntry;
import edu.kstate.datastore.util.Misc;
import edu.kstate.datastore.webservice.simplerest.SimpleRestService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DeliveryThread extends Thread {

    private HazelcastInstance instance;
    private ThreadPoolExecutor threadPool;
    private boolean stopRequested;
    private long deliveryPacketSizeB;

    public DeliveryThread(HazelcastInstance instance, long deliveryPacketSizeB, int clientCount) {
        this.instance = instance;
        this.deliveryPacketSizeB = deliveryPacketSizeB;
        this.threadPool = new ThreadPoolExecutor(clientCount, clientCount, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(clientCount));
    }

    public void requestStop() {
        Misc.logInfo(this.getClass(), "Stop Requested");
        this.stopRequested = true;
    }

    private HashMap<String, ElementSetEntry> cacheElementSet = new HashMap<String, ElementSetEntry>();
    private HashMap<String, WebServiceEntry> cacheWebService = new HashMap<String, WebServiceEntry>();

    private ElementSetEntry getElementSetEntry(String id) {
        if(cacheElementSet.containsKey(id) == true) {
            return cacheElementSet.get(id);
        }
        else {
            IMap<String, ElementSetEntry> distributedMap = this.instance.getMap("elementSet");
            ElementSetEntry entry = distributedMap.get(id);
            cacheElementSet.put(id, entry);
            return entry;
        }
    }

    private WebServiceEntry getWebServiceEntry(String id) {
        if(cacheWebService.containsKey(id) == true) {
            return cacheWebService.get(id);
        }
        else {
            IMap<String, WebServiceEntry> distributedMap = this.instance.getMap("webService");
            WebServiceEntry entry = distributedMap.get(id);
            cacheWebService.put(id, entry);
            return entry;
        }
    }

    private void eventLoop() throws Exception {

        IMap<String, ValueSetEntry> mapValueSet = this.instance.getMap("valueSet");
        PriorityBlockingQueue<QueueItem> priorityBlockingQueue = new PriorityBlockingQueue<QueueItem>();

        int noEntriesDelay = 1000;
        while (true) {

            // TODO: only shutdown if this data store has uploaded at least as many
            // value sets as it has collected

            // if we've been requested to stop then as long as there aren't
            // any unsent entries we can go ahead and shut down. if there
            // are then we have to keep running
            if (stopRequested == true) {
                int entriesInQueue = priorityBlockingQueue.size();
                if (entriesInQueue == 0) {
                    Misc.logInfo(this.getClass(), "Stop requested and no entries left to deliver");
                    threadPool.shutdown();
                    break;
                } else {
                    Misc.logInfo(this.getClass(), "Stop requested but " + entriesInQueue + " left in queue");
                }
            }

            // TODO: FIX THIS so that it gathers value sets per web service,
            // right now it assumes there is only one web service.

            // make a pass over the local map and update our priority queue.
            // a full pass takes time (have seen up to 250 ms with 40k entries
            // on a busy core) so we don't want to do this too much. any unsent
            // entries that are found are marked as sent in the map and added
            // to our priority queue.
            Misc.logInfo(this.getClass(), "Start gather");
            long startGather = System.currentTimeMillis();
            Set<String> localKeys = mapValueSet.localKeySet();
            for (String key : localKeys) {
                ValueSetEntry entry = mapValueSet.get(key);
                if (entry == null) {
                    continue;
                }
                if (entry.getNeedsUpload() == false) {
                    continue;
                }
                MapEntry<String, ValueSetEntry> mapEntry = mapValueSet.getMapEntry(key);
                if (mapEntry != null) {

                    // mark the entry as not needing uploading in the value set map
                    ValueSetEntry unsentEntry = mapEntry.getValue();
                    unsentEntry.setNeedsUpload(false);
                    mapValueSet.put(key, unsentEntry);

                    // add the entry to our priority queue
                    priorityBlockingQueue.add(new QueueItem(mapEntry.getCreationTime(), mapEntry.getValue()));
                }
            }
            long gatherMs = System.currentTimeMillis() - startGather;
            Misc.logInfo(this.getClass(), String.format("Stop gather (%d MS)", gatherMs));
            Statistics.getInstance().add("Delivery-GatherMS", gatherMs);

            // NOTE: we could also just encode the value sets on the fly so
            // that we don't have to worry about trying to estimate the
            // encoded size.

            // figure out how many entries to remove from the queue to meet
            // our minimum delivery size
            int setCount = 0;
            int elementCount = 0;
            boolean isEnoughToSend = false;
            Iterator i = priorityBlockingQueue.iterator();
            while(i.hasNext() == true) {
                QueueItem nextItem = (QueueItem)i.next();
                setCount += 1;
                elementCount += nextItem.getEntry().getValues().length;

                // assume each entry is serialized to 49 bytes and we want to
                // send 11 MB in each web service call - TODO: multiplier
                // should come from the web service entry
                if(elementCount * 49 >= deliveryPacketSizeB) {
                    isEnoughToSend = true;
                    break;
                }
            }

            // at this point we may have collected some more entries and
            // may or may not have met the minimum send size, so only send
            // if we have enough entries (or if we're trying to shut down).
            if (isEnoughToSend == true || this.stopRequested == true) {

                // remove the items that we want to send from the priority
                // queue. the drainTo method will return them in sorted order.
                final ArrayList<QueueItem> entriesToSend = new ArrayList<QueueItem>();
                priorityBlockingQueue.drainTo(entriesToSend, setCount);

                Misc.logInfo(this.getClass(), String.format("Sending:%d(WebQ:%d)", entriesToSend.size(), this.threadPool.getQueue().size()));

                // record some statistics
                Statistics.getInstance().add("Delivery-SetValues-Call-Count", 1);
                Statistics.getInstance().add("Delivery-SetValues-ValueSet-Count", entriesToSend.size());

                // record the number of individual values we upload
                int totalValueCount = 0;
                for (QueueItem nextQueueItem : entriesToSend) {
                    totalValueCount += nextQueueItem.getEntry().getValues().length;
                }
                Statistics.getInstance().add("Delivery-SetValues-Value-Count", totalValueCount);

                // record how long each entry was in the buffer. the residence
                // time is the duration from when the entry was added to when
                // entries were removed and sent.
                long now = System.currentTimeMillis();
                for (QueueItem nextQueueItem : entriesToSend) {

                    long residenceMs = now - nextQueueItem.getCreationTime();
                    Statistics.getInstance().add("Buffer-TimeResidence-MS", residenceMs);
                }

                // collect all the element sets that we'll need for sending the
                // value sets
                final HashMap<String, String[]> elementSetIds = new HashMap<String, String[]>();
                for (QueueItem nextQueueItem : entriesToSend) {
                    ElementSetEntry elementSetEntry = getElementSetEntry(nextQueueItem.getEntry().getElementSetId());
                    elementSetIds.put(elementSetEntry.getElementSetId(), elementSetEntry.getElementIds());
                }

                final WebServiceEntry webServiceEntry = getWebServiceEntry("TestWebService");

                // create a runnable task
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        performDelivery(elementSetIds, webServiceEntry, entriesToSend);
                    }
                };

                // wait here until we have a thread to handle the send
                while (threadPool.getQueue().remainingCapacity() == 0) {
                    Misc.logInfo(this.getClass(), "Too many active, paused delivery");
                    Thread.sleep(2000);
                }

                threadPool.submit(task);

                //Thread.sleep(10);

                // reset our exponential backoff delay
                noEntriesDelay = 1000;
            } else {

                // if we didn't find enough entries to send something then pause
                Misc.logInfo(this.getClass(), String.format("Not enough entries to send(%d), paused delivery (%d)", priorityBlockingQueue.size(), noEntriesDelay));
                Thread.sleep(noEntriesDelay);
                noEntriesDelay = Math.min(noEntriesDelay * 2, 60000); // exponential backoff bounded at 60 sec
            }
        }
    }

    private static void performDelivery(HashMap<String, String[]> elementIds, WebServiceEntry webServiceEntry, ArrayList<QueueItem> queueEntries) {

        final ArrayList<ValueSetEntry> entries = new ArrayList<ValueSetEntry>();
        for (QueueItem nextQueueItem : queueEntries) {
            entries.add(nextQueueItem.getEntry());
        }

        try {
            SimpleRestService.setValuesSync(elementIds, webServiceEntry.getUrl() + "/set", entries);
        } catch (Exception e) {

            // if the send operation fails, we need to put the
            // entries back into the map. we assume failure is
            // uncommon.
            Misc.logException(DeliveryThread.class, e);
        }

        // record the total time from when an entry was added to
        // the buffer to when it was delivered to the web service
        long now = System.currentTimeMillis();
        for (QueueItem nextQueueItem : queueEntries) {
            long residenceMs = now - nextQueueItem.getCreationTime();
            Statistics.getInstance().add("Buffer-TimeEndToEnd-MS", residenceMs);
        }
    }

    public void run() {
        try {
            this.eventLoop();
        } catch (Exception e) {
            Misc.logException(this.getClass(), e);
        }
        Misc.logInfo(this.getClass(), "Stopped");
    }

    private class QueueItem implements Comparable {
        private long creationTime;
        private ValueSetEntry entry;

        public QueueItem(long creationTime, ValueSetEntry valueSetEntry) {
            this.creationTime = creationTime;
            this.entry = valueSetEntry;
        }

        public long getCreationTime() {
            return this.creationTime;
        }

        public ValueSetEntry getEntry() {
            return this.entry;
        }

        @Override
        public int compareTo(Object o) {
            return (int) (this.creationTime - ((QueueItem) o).getCreationTime());
        }
    }
}