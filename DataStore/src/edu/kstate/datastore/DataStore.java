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

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.monitor.LocalMapOperationStats;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.LocalQueueOperationStats;
import com.hazelcast.monitor.LocalQueueStats;
import edu.kstate.datastore.data.ValueSetEntry;
import edu.kstate.datastore.data.ValueSetRequestEntry;
import edu.kstate.datastore.data.WebServiceEntry;
import edu.kstate.datastore.listeners.ValueSetEntryListener;
import edu.kstate.datastore.listeners.ValueSetItemListener;
import edu.kstate.datastore.listeners.ValueSetRequestItemListener;
import edu.kstate.datastore.listeners.WebServiceEntryListener;
import edu.kstate.datastore.util.Misc;

public class DataStore {

    private static HazelcastInstance startInstance(int id, int port, String masterAddress, String instanceAddress, int clientCount) throws Exception {
        Config cfg = new Config();
        cfg.setPort(port);
        cfg.setPortAutoIncrement(true); // TRUE for multi-instance nodes

        QueueConfig queueConfig = new QueueConfig();
        queueConfig.setName("valueSetRequest");
        queueConfig.setMaxSizePerJVM(1);
        cfg.addQueueConfig(queueConfig);
        Misc.logInfo(DataStore.class, queueConfig.toString());

        queueConfig = new QueueConfig();
        queueConfig.setName("valueSet");
        queueConfig.setMaxSizePerJVM(1);
        cfg.addQueueConfig(queueConfig);
        Misc.logInfo(DataStore.class, queueConfig.toString());

        NetworkConfig network = cfg.getNetworkConfig();
        Join join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);
        Misc.logInfo(DataStore.class, network.toString());

        // use a specific address if we were given one
        if (instanceAddress != null) {
            Interfaces interfaces = new Interfaces();
            interfaces.setEnabled(true);
            interfaces.addInterface(instanceAddress);
            network.setInterfaces(interfaces);
        }

        if (id == 0) {
            Misc.logInfo(DataStore.class, "Configured as master at " + masterAddress);
        } else {
            join.getTcpIpConfig().setRequiredMember(masterAddress + ":" + 5701);
            join.getTcpIpConfig().setEnabled(true);
            Misc.logInfo(DataStore.class, "Connecting to master at " + masterAddress + ":" + 5701);
        }

        // there should be the same number of client worker threads as clients
        int coreExecutorThreadCount = 32;
        cfg.setProperty("hazelcast.executor.client.thread.count", String.valueOf(Math.max(coreExecutorThreadCount, clientCount)));

        // get periodic updates to stdout
        cfg.setProperty("hazelcast.log.state", "true");

        MapConfig mapConfig = cfg.getMapConfig("valueSet");
        mapConfig.setBackupCount(0);

        // start the hazelcast instance
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);

        return instance;
    }

    private static void waitForThreadToStop(Thread thread) throws Exception {
        while (thread.isAlive() == true) {
            Misc.logInfo(DataStore.class, "Waiting for thread to stop: " + thread.getClass());
            Thread.sleep(5000);
        }
    }

    public static void main(String[] args) {

        try {

            // set the default values
            int id = 0;
            String masterAddress = null;
            String instanceAddress = null;
            int clientCount = 16;
            boolean enableAssembly = false;
            long deliveryPacketSizeB = 11L * 1024L * 1024L; // 11MB
            long maxLocalValueSetMapCostB = 3L * 1024L * 1024L * 1024L; // 3GB

            if (args.length % 2 != 0) {
                Misc.logInfo(DataStore.class, "Invalid arguments");
            }

            int i = 0;
            while (i < args.length) {

                // get the next option/value pair
                String option = args[i];
                String value = args[i + 1];
                i += 2;

                Misc.logInfo(DataStore.class, "Parameter: " + option + " = " + value);

                if (option.equals("masterAddress") == true) {
                    masterAddress = value;
                }

                if (option.equals("instanceAddress") == true) {
                    instanceAddress = value;
                }

                if (option.equals("id") == true) {
                    id = Integer.parseInt(value);
                }

                if (option.equals("clientCount") == true) {
                    clientCount = Integer.parseInt(value);
                }

                if (option.equals("maxLocalValueSetMapCostB") == true) {
                    maxLocalValueSetMapCostB = Long.parseLong(value);
                }

                if (option.equals("deliveryPacketSizeB") == true) {
                    deliveryPacketSizeB = Long.parseLong(value);
                }

                if (option.equals("enableAssembly") == true) {
                    enableAssembly = Boolean.parseBoolean(value);
                }
            }

            // setup the statistics object
            Statistics.getInstance().id = String.valueOf(id);

            // tell non-masters to give the master a little time to start to
            // help ensure the other instances find the master
            if (id > 0) {
                Thread.sleep(1000 * (5 + id));
            }

            // each instance uses a different port which is based on the
            // instance's id number
            int port = 5701 + id;

            // start the hazelcast instance and wait for any other instances to start
            HazelcastInstance instance = startInstance(id, port, masterAddress, instanceAddress, clientCount);

            // create the map that lists all active clients
            IMap<String, String> mapClient = instance.getMap("client");

            // create the map that stores web service information
            IMap<String, WebServiceEntry> mapWebService = instance.getMap("webService");
            mapWebService.addEntryListener(new WebServiceEntryListener(), false);

            // create the map where the data entries are stored
            IMap<String, ValueSetEntry> mapValueSet = instance.getMap("valueSet");
            mapValueSet.addEntryListener(new ValueSetEntryListener(), false);

            // create the queue that will gate access to the map
            IQueue<ValueSetEntry> queueValueSet = instance.getQueue("valueSet");
            queueValueSet.addItemListener(new ValueSetItemListener(), false);

            // create the queue for value set requests
            IQueue<ValueSetRequestEntry> queueValueSetRequest = instance.getQueue("valueSetRequest");
            queueValueSetRequest.addItemListener(new ValueSetRequestItemListener(), false);

            // start the delivery thread
            DeliveryThread deliveryThread = new DeliveryThread(instance, deliveryPacketSizeB, clientCount);
            deliveryThread.start();

            // start the fetch thread
            FetchThread fetchThread = new FetchThread(instance, maxLocalValueSetMapCostB, enableAssembly, clientCount);
            fetchThread.start();

            // start the queue-to-map thread
            QueueToMapThread queueToMapThread = new QueueToMapThread(instance, maxLocalValueSetMapCostB);
            queueToMapThread.start();

            // start the expiration thread
            ExpirationThread expirationThread = new ExpirationThread(instance, maxLocalValueSetMapCostB);
            expirationThread.start();

            // wait for a client to connect
            while (mapClient.size() == 0) {
                Thread.sleep(5000);
            }

            // wait for all clients to complete
            while (mapClient.size() > 0) {
                Misc.logInfo(DataStore.class, "Active clients: " + mapClient.size());

                double mapValueSetOps = calculateOpsPerSecond(mapValueSet.getLocalMapStats());
                double mapWebServiceOps = calculateOpsPerSecond(mapWebService.getLocalMapStats());
                double queueValueSetOps = calculateOpsPerSecond(queueValueSet.getLocalQueueStats());
                double queueValueSetRequestOps = calculateOpsPerSecond(queueValueSetRequest.getLocalQueueStats());

                Misc.logInfo(DataStore.class, String.format("MapValueSetOpsPerSec:      %.2f", mapValueSetOps));
                Misc.logInfo(DataStore.class, String.format("MapWebServiceOpsPerSec:    %.2f", mapWebServiceOps));
                Misc.logInfo(DataStore.class, String.format("QueueValueSetOpsPerSec:    %.2f", queueValueSetOps));
                Misc.logInfo(DataStore.class, String.format("QueueValueSetReqOpsPerSec: %.2f", queueValueSetRequestOps));

                Statistics.getInstance().add("MapValueSetOpsPerSec", mapValueSetOps);
                Statistics.getInstance().add("MapWebServiceOpsPerSec", mapWebServiceOps);
                Statistics.getInstance().add("QueueValueSetOpsPerSec", queueValueSetOps);
                Statistics.getInstance().add("QueueValueSetRequestOpsPerSec", queueValueSetRequestOps);

                Misc.logInfo(DataStore.class, Statistics.getInstance().toCsv().toString());
                Thread.sleep(60000);
            }

            Misc.logInfo(DataStore.class, "No active clients, starting shutdown");

            // ask the delivery thread to stop and wait for it
            deliveryThread.requestStop();
            waitForThreadToStop(deliveryThread);

            // ask the queue-to-map thread to stop and wait for it
            queueToMapThread.requestStop();
            waitForThreadToStop(queueToMapThread);

            // ask the fetch thread to stop and wait for it
            fetchThread.requestStop();
            waitForThreadToStop(fetchThread);

            // ask the expiration thread to stop and wait for it
            expirationThread.requestStop();
            waitForThreadToStop(expirationThread);

            // generate a csv of the statistics
            Statistics.getInstance().writeCsv("DataStoreProfile.csv");

        } catch (Exception e) {

            Misc.logInfo(DataStore.class, "Exception in event loop");
            Misc.logException(DataStore.class, e);
        }

        Misc.logInfo(DataStore.class, "Hazelcast.shutdownAll");
        Hazelcast.shutdownAll();
    }

    private static double calculateOpsPerSecond(LocalMapStats mapStats) {
        LocalMapOperationStats stats = mapStats.getOperationStats();
        long ops = stats.total();
        long time = stats.getPeriodEnd() - stats.getPeriodStart();
        return (double) ops / ((double) time / 1000.0);
    }

    private static double calculateOpsPerSecond(LocalQueueStats queueStats) {
        LocalQueueOperationStats stats = queueStats.getOperationStats();
        long ops = stats.getNumberOfPolls() + stats.getNumberOfOffers() + stats.getNumberOfEmptyPolls() + stats.getNumberOfRejectedOffers();
        long time = stats.getPeriodEnd() - stats.getPeriodStart();
        return (double) ops / ((double) time / 1000.0);
    }
}
