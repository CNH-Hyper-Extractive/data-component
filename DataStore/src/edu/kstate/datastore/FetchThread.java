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
import edu.kstate.datastore.data.ElementSetEntry;
import edu.kstate.datastore.data.ValueSetEntry;
import edu.kstate.datastore.data.ValueSetRequestEntry;
import edu.kstate.datastore.data.WebServiceEntry;
import edu.kstate.datastore.util.Misc;
import edu.kstate.datastore.webservice.ServiceAdapter;
import edu.kstate.datastore.webservice.wateroneflow.WaterOneFlow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FetchThread extends Thread {

    private HazelcastInstance instance;
    private ThreadPoolExecutor threadPool;
    private boolean stopRequested;
    private long maxLocalValueSetMapCostB = 0;
    private HashMap<String, ElementSetEntry> cacheElementSet = new HashMap<String, ElementSetEntry>();
    private HashMap<String, WebServiceEntry> cacheWebService = new HashMap<String, WebServiceEntry>();
    private boolean enableAssembly;
    private LinkedList<String> requestHistory = new LinkedList<String>();
    private int clientCount;
    private HashMap<String,ServiceAdapter> serviceAdapters = new HashMap<String, ServiceAdapter>();

    public FetchThread(HazelcastInstance instance, long maxLocalValueSetMapCostB, boolean enableAssembly, int clientCount) {
        this.instance = instance;
        this.maxLocalValueSetMapCostB = maxLocalValueSetMapCostB;
        this.enableAssembly = enableAssembly;
        this.clientCount = clientCount;
        this.threadPool = new ThreadPoolExecutor(clientCount, clientCount, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(clientCount));
    }

    private void performFetch(ValueSetRequestEntry valueSetRequestEntry, WebServiceEntry webServiceEntry, ElementSetEntry elementSetEntry, IMap<String, ValueSetEntry> mapValueSet) {

        String webServiceId = valueSetRequestEntry.getWebServiceId();
        String quantityId = valueSetRequestEntry.getQuantityId();
        String elementSetId = valueSetRequestEntry.getElementSetId();
        String timeStamp = valueSetRequestEntry.getTimeStamp();
        String scenarioId = valueSetRequestEntry.getScenarioId();
        String[] elementIds = elementSetEntry.getElementIds();

        ServiceAdapter serviceAdapter = null;
        if (this.serviceAdapters.containsKey(webServiceEntry.getId()) == true) {
            serviceAdapter = this.serviceAdapters.get(webServiceEntry.getId());
        }
        else {
            if (webServiceEntry.getType().equals("WaterOneFlow1.0") == true) {
                serviceAdapter = new WaterOneFlow(webServiceEntry.getUrl(), "1.0");
                serviceAdapter.start();
                this.serviceAdapters.put(webServiceEntry.getId(), serviceAdapter);
            }
            if (webServiceEntry.getType().equals("WaterOneFlow1.1") == true) {
                serviceAdapter = new WaterOneFlow(webServiceEntry.getUrl(), "1.1");
                serviceAdapter.start();
                this.serviceAdapters.put(webServiceEntry.getId(), serviceAdapter);
            }
            // TODO: add support for the REST service used in the performance study
        }

        // record the number of times we call the GetValues web service
        Statistics.getInstance().add("Fetch-GetValues-Call-Count", 1);

        // send while blocking this thread
        double[] values = serviceAdapter.getValues(quantityId, elementIds, timeStamp);

        // create an entry for the received data
        ValueSetEntry entry = new ValueSetEntry(webServiceId, quantityId, timeStamp, elementSetId, scenarioId, values);

        // place it in the map
        mapValueSet.put(ValueSetEntry.createKey(entry), entry);

        // record the number of value sets we download - right now we only
        // download one at a time
        Statistics.getInstance().add("Fetch-GetValues-ValueSet-Count", 1);

        // record the number of individual values we download
        Statistics.getInstance().add("Fetch-GetValues-Value-Count", values.length);
    }

    public void requestStop() {
        Misc.logInfo(this.getClass(), "Stop Requested");
        this.stopRequested = true;
    }

    private ElementSetEntry getElementSetEntry(String id) {
        if (cacheElementSet.containsKey(id) == true) {
            return cacheElementSet.get(id);
        } else {
            IMap<String, ElementSetEntry> distributedMap = this.instance.getMap("elementSet");
            ElementSetEntry entry = distributedMap.get(id);
            cacheElementSet.put(id, entry);
            return entry;
        }
    }

    private WebServiceEntry getWebServiceEntry(String id) {
        if (cacheWebService.containsKey(id) == true) {
            return cacheWebService.get(id);
        } else {
            IMap<String, WebServiceEntry> distributedMap = this.instance.getMap("webService");
            WebServiceEntry entry = distributedMap.get(id);
            cacheWebService.put(id, entry);
            return entry;
        }
    }

    private void eventLoop() throws Exception {

        // get references to our distributed data structures
        final IQueue<ValueSetRequestEntry> queueValueSetRequest = this.instance.getQueue("valueSetRequest");
        final IMap<String, ValueSetEntry> mapValueSet = this.instance.getMap("valueSet");

        // continuously remove requests from the queue and call the
        // web services. this loop needs to execute very quickly in
        // order to keep up with the requests, so don't do anything
        // that takes any time in here.
        while (true) {

            // we should only stop if there are no outstanding requests
            // in our local portion of the request map
            if (stopRequested == true) {
                int entriesInQueue = queueValueSetRequest.size();
                if (entriesInQueue > 0) {
                    Misc.logInfo(this.getClass(), "Warning: Stopping with " + entriesInQueue + " left in queue");
                }
                threadPool.shutdown();
                for (ServiceAdapter nextAdapter : this.serviceAdapters.values()) {
                    nextAdapter.stop();
                }
                break;
                /*
                int entriesInQueue = queueValueSetRequest.size();
                if (entriesInQueue == 0) {
                    Misc.logInfo(this.getClass(), "Stop requested and no entries left to fetch");
                    threadPool.shutdown();
                    break;
                } else {
                    Misc.logInfo(this.getClass(), "Stop requested but " + entriesInQueue + " left in queue");
                }*/
            }

            if (threadPool.getQueue().remainingCapacity() == 0) {
                Misc.logInfo(this.getClass(), "Too many active, paused fetching");
                Statistics.getInstance().add("Fetch-Limit-WaitMS", 100);
                Thread.sleep(100); // short delay, we're just checking a local queue data structure
                continue;
            }

            // don't remove any requests from the queue unless we have space
            // in the value set map for them. it's computationally expensive
            // to calculate whether or not adding a new value set will put
            // us over the limit, so instead we check for when the limit is
            // passed (so there might be up to one value set's worth of data
            // over the limit in the map).
            if (Misc.maxDataExceeded(this.maxLocalValueSetMapCostB, mapValueSet) == true) {
                Misc.logInfo(this.getClass(), "No more space in map, paused fetching");
                Thread.sleep(1000); // wait longer, since we need an expiration to run
                continue;
            }

            // get the next request entry from the queue (which
            // will be null if it's empty)
            final ValueSetRequestEntry nextEntry = queueValueSetRequest.poll(60, TimeUnit.SECONDS);
            if (nextEntry == null) {
                Misc.logInfo(this.getClass(), "Request queue is empty");
                //Thread.sleep(1000); // wait longer, since we're hitting the global queue
                continue;
            }

            // see if there is an outstanding request for this entry
            if (requestHistory.contains(ValueSetRequestEntry.createKey(nextEntry)) == true) {
                Misc.logInfo(this.getClass(), "ValueSet already requested, not fetching");
                continue;
            }

            // it's possible that another entry that was
            // ahead in line in the queue already requested
            // this value set so make sure it doesn't exist
            // before we make the request
            if (mapValueSet.containsKey(ValueSetEntry.createKey(nextEntry.getWebServiceId(), nextEntry.getQuantityId(), nextEntry.getElementSetId(), nextEntry.getTimeStamp(), nextEntry.getScenarioId())) == true) {
                Misc.logInfo(this.getClass(), "ValueSet already in map, not fetching");
                continue;
            }

            final WebServiceEntry webServiceEntry = this.getWebServiceEntry(nextEntry.getWebServiceId());
            final ElementSetEntry elementSetEntry = this.getElementSetEntry(nextEntry.getElementSetId());

            // TODO: expire element sets from the cache occasionally

            // TODO: we shouldn't be pausing these lookups when there are no
            // available web service calls since they don't rely on them.

            if (enableAssembly == true) {

                // we may have all the values being requested in other value sets,
                // so we should check to see if the elements in the set being
                // requested are in any of the element sets associated with the
                // value sets in the memory. we step through each element set in
                // the cache, and through each element therein, and each element
                // being searched for. we record the element set id for the set
                // in which we found each element and the index where we found it.
                String[] elementIds = elementSetEntry.getElementIds();
                String[] elementIdsSourceMap = new String[elementIds.length];
                int[] elementIdsSourceMapIndex = new int[elementIds.length];
                for (ElementSetEntry nextElementSet : cacheElementSet.values()) {
                    String[] nextElementIds = nextElementSet.getElementIds();
                    for (int j = 0; j < nextElementIds.length; j++) {
                        for (int i = 0; i < elementIds.length; i++) {
                            if (elementIds[i].equals(nextElementIds[j]) == true) {
                                elementIdsSourceMap[i] = nextElementSet.getElementSetId();
                                elementIdsSourceMapIndex[i] = j;
                                // TOOD: ideally we would remember all the element sets that could be a source for each element
                            }
                        }
                    }
                }

                // at this point we know which values are present. if any values are
                // not available then we'll need to call the web service
                boolean isAvailable = true;
                for (int i = 0; i < elementIdsSourceMap.length; i++) {
                    if (elementIdsSourceMap[i] == null) {
                        isAvailable = false;
                        break;
                    }
                }

                // if there are value sets that cover all the elements in the set
                // being requested, then check to see if value sets exist for the
                // requested point in time
                boolean valuesAvailable = true;
                double[] values = new double[elementIds.length];
                if (isAvailable == true) {

                    // make a list of all the element set id's that contain the
                    // elements being requested
                    HashMap<String, String> elementSetSourceIds = new HashMap<String, String>();
                    for (int i = 0; i < elementIdsSourceMap.length; i++) {
                        if (elementSetSourceIds.containsKey(elementIdsSourceMap[i]) == false) {
                            elementSetSourceIds.put(elementIdsSourceMap[i], elementIdsSourceMap[i]);
                        }
                    }

                    // now see if there is a value set for the requested time for
                    // each element set we need
                    for (String nextElementSetId : elementSetSourceIds.values()) {

                        // generate the key of the value set we're looking for
                        String nextKey = ValueSetEntry.createKey(nextEntry.getWebServiceId(), nextEntry.getQuantityId(), nextElementSetId, nextEntry.getTimeStamp(), nextEntry.getScenarioId());

                        // see if the value set exists in the memory
                        ValueSetEntry nextValueSet = mapValueSet.get(nextKey);
                        if (nextValueSet != null) {

                            // grab all the values that we need from this value set
                            for (int i = 0; i < elementIdsSourceMap.length; i++) {
                                if (elementIdsSourceMap[i].equals(nextElementSetId) == true) {
                                    values[i] = nextValueSet.getValues()[elementIdsSourceMapIndex[i]];
                                }
                            }
                        } else {
                            valuesAvailable = false;
                            break;
                        }
                    }
                }

                if (valuesAvailable == true) {

                    // we got all the values so insert the value set
                    Misc.logInfo(this.getClass(), String.format("Assembled:%s", nextEntry.toString()));
                    ValueSetEntry assembledEntry = new ValueSetEntry(nextEntry.getWebServiceId(), nextEntry.getQuantityId(), nextEntry.getTimeStamp(), nextEntry.getElementSetId(), nextEntry.getScenarioId(), values);
                    mapValueSet.put(ValueSetEntry.createKey(assembledEntry), assembledEntry);

                } else {

                    // all of the requested values are not in the memory so we need
                    // to call the web service
                    //Misc.logInfo(this.getClass(), String.format("Fetch:%s(ReqQ:%d,WebQ:%d)", nextEntry.toString(), queueValueSetRequest.size(), this.threadPool.getQueue().size()));

                    // create a runnable task
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            performFetch(nextEntry, webServiceEntry, elementSetEntry, mapValueSet);
                        }
                    };

                    // add this send to the thread pool
                    threadPool.submit(task);
                }
            } else {

                // assemly is turned off, so just make the request
                //Misc.logInfo(this.getClass(), String.format("Fetch:%s(ReqQ:%d,WebQ:%d)", nextEntry.toString(), queueValueSetRequest.size(), this.threadPool.getQueue().size()));

                requestHistory.addFirst(ValueSetRequestEntry.createKey(nextEntry));
                if(requestHistory.size() > this.clientCount)
                {
                    requestHistory.removeLast();
                }

                // create a runnable task
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        performFetch(nextEntry, webServiceEntry, elementSetEntry, mapValueSet);
                    }
                };

                // add this send to the thread pool
                threadPool.submit(task);
            }

            //Thread.sleep(10);
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
}