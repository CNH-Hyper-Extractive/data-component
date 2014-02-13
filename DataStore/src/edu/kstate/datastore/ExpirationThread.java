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
import edu.kstate.datastore.data.ValueSetEntry;
import edu.kstate.datastore.util.Misc;

import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

public class ExpirationThread extends Thread {

    private HazelcastInstance instance;
    private long maxLocalValueSetMapCostB;
    private boolean stopRequested;

    public ExpirationThread(HazelcastInstance instance, long maxLocalValueSetMapCostB) {
        Misc.logInfo(this.getClass(), "Start");
        this.instance = instance;
        this.maxLocalValueSetMapCostB = maxLocalValueSetMapCostB;
    }

    public void requestStop() {
        Misc.logInfo(this.getClass(), "Stop");
        this.stopRequested = true;
    }

    /*private String nextLeastAccessedDate() {
        String minimumKey = null;
        long minimumAccessDate = 0;

        IMap<String, ValueSetEntry> mapValueSet = this.instance.getMap("valueSet");

        Set<String> localKeys = mapValueSet.localKeySet();
        for (String key : localKeys) {
            // get the next entry
            ValueSetEntry entry = mapValueSet.get(key);

            if (entry == null)
                continue;

            // don't expire entries that need to be uploaded
            if (entry.getNeedsUpload() == true)
                continue;

            MapEntry<String, ValueSetEntry> mapEntry = mapValueSet.getMapEntry(key);

            // don't expire entries that have never been accessed - TODO:
            // verify that hits
            // are only incremented on a get and not a put
            if (mapEntry.getHits() == 0)
                continue;

            // the first one starts as the minimum
            if (minimumKey == null) {
                minimumKey = key;
                minimumAccessDate = mapEntry.getLastAccessTime();
            } else {
                if (mapEntry.getLastAccessTime() < minimumAccessDate) {
                    minimumKey = key;
                    minimumAccessDate = mapEntry.getLastAccessTime();
                }
            }
        }

        return minimumKey;
    }*/

    public void run() {
        try {
            Misc.logInfo(this.getClass(), "Started");

            IMap<String, ValueSetEntry> mapValueSet = this.instance.getMap("valueSet");

            while (true) {

                // see if the local data size is more than 90% of the max
                /*while (mapValueSet.getLocalMapStats().getOwnedEntryMemoryCost() > 0.90 * (double) maxMaxSizeB) {
                    String key = nextLeastAccessedDate();
                    mapValueSet.remove(key);

                    Misc.logInfo(this.getClass(), "Removed: " + key);
                }*/

                // if we're over 90% full then perform an expiration
                double percentFull = mapValueSet.getLocalMapStats().getOwnedEntryMemoryCost() / (double)maxLocalValueSetMapCostB;
                Misc.logInfo(this.getClass(), String.format("Map is %d%% full", (int)(percentFull * 100.0)));

                if (percentFull > 0.90) {

                    PriorityBlockingQueue<QueueItem> priorityBlockingQueue = new PriorityBlockingQueue<QueueItem>();

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

                        // skip ones that haven't been uploaded yet
                        if (entry.getNeedsUpload() == true) {
                            continue;
                        }

                        MapEntry<String, ValueSetEntry> mapEntry = mapValueSet.getMapEntry(key);
                        if (mapEntry != null) {

                            // skip ones that haven't been accessed yet
                            if(mapEntry.getHits() == 0) {
                                continue;
                            }

                            // add the entry to our priority queue
                            priorityBlockingQueue.add(new QueueItem(mapEntry.getKey(), mapEntry.getHits(), mapEntry.getCreationTime(), mapEntry.getCost()));
                        }
                    }
                    long gatherMs = System.currentTimeMillis() - startGather;
                    Misc.logInfo(this.getClass(), String.format("Stop gather (%d MS)", gatherMs));
                    Statistics.getInstance().add("Expiration-GatherMS", gatherMs);

                    // if we found any to expire then expire at most 10% of the
                    // memory
                    if (priorityBlockingQueue.size() > 0) {
                        long maxMemoryToExpireB = (long) (mapValueSet.getLocalMapStats().getOwnedEntryMemoryCost() * 0.10);
                        Misc.logInfo(this.getClass(), String.format("Found %d entries to expire, expiring %dB", priorityBlockingQueue.size(), maxMemoryToExpireB));
                        Statistics.getInstance().add("Expiration-Event", 1);

                        long memoryExpiredB = 0;
                        while (memoryExpiredB <= maxMemoryToExpireB && priorityBlockingQueue.size() > 0) {
                            QueueItem nextItem = priorityBlockingQueue.poll();
                            mapValueSet.remove(nextItem.getValueSetKey());
                            memoryExpiredB += nextItem.getMemoryCost();

                            //Misc.logInfo(this.getClass(), String.format("Expired: %s", nextItem.getValueSetKey()));
                        }
                    }
                    else {
                        Misc.logInfo(this.getClass(), String.format("Found no entries to expire"));
                    }
                }

                // wait before checking again
                Thread.sleep(60000);

                if (stopRequested == true) {
                    break;
                }
            }
        } catch (Exception e) {
            Misc.logException(this.getClass(), e);
        }

        Misc.logInfo(this.getClass(), "Stopped");
    }

    private class QueueItem implements Comparable {
        private String valueSetKey;
        private int hitCount;
        private long creationTime;
        private long memoryCost;

        public QueueItem(String valueSetKey, int hitCount, long creationTime, long memoryCost) {
            this.valueSetKey = valueSetKey;
            this.hitCount = hitCount;
            this.creationTime = creationTime;
            this.memoryCost = memoryCost;
        }

        public String getValueSetKey() {
            return this.valueSetKey;
        }

        public int getHitCount() {
            return this.hitCount;
        }

        public long getCreationTime() {
            return this.creationTime;
        }

        public long getMemoryCost() {
            return this.memoryCost;
        }

        /**
         * Compares this object with the specified object for order. Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         * if this is less, then return negative
         */
        @Override
        public int compareTo(Object o) {
            // don't base on the hit count, since new, un-hit items will be
            // expired!
            QueueItem item = (QueueItem)o;
            //if(this.hitCount == item.getHitCount()) {
                return (int) (this.creationTime - item.getCreationTime());
            //}
            //else {
            //    return this.hitCount - item.getHitCount();
            //}
        }
    }
}