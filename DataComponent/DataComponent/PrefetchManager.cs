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

using System.Collections;
using Hazelcast.Client;
using Hazelcast.Core;
using KState.Util;
using Oatc.OpenMI.Sdk.Backbone;
using OpenMI.Standard;

namespace KState.DataComponent
{
    /// The prefetch limit is based on the time horizon, so the time horizon of
    /// each model must match the time horizon of the composition.
    public class PrefetchManager
    {
        private readonly bool isEnabled;
        private readonly IMap<string, ValueSetEntry> mapValueSet;
        private readonly PrefetchMonitor prefetchMonitor;
        private readonly IQueue<ValueSetRequestEntry> queueValueSetRequest;
        private readonly string scenarioId;
        private readonly Statistics statistics;
        private readonly TraceFile traceFile;
        private readonly WebServiceManager webServiceManager;
        private HazelcastClient hazelcastClient;

        public PrefetchManager(TraceFile traceFile, Statistics statistics, HazelcastClient hazelcastClient, string scenarioId, ArrayList outputLinks, ITimeSpan timeHorizon, bool isEnabled, WebServiceManager webServiceManager)
        {
            this.traceFile = traceFile;
            this.statistics = statistics;
            this.hazelcastClient = hazelcastClient;
            this.scenarioId = scenarioId;
            this.isEnabled = isEnabled;
            this.webServiceManager = webServiceManager;

            mapValueSet = hazelcastClient.getMap<string, ValueSetEntry>("valueSet");
            queueValueSetRequest = hazelcastClient.getQueue<ValueSetRequestEntry>("valueSetRequest");

            prefetchMonitor = new PrefetchMonitor(outputLinks, traceFile, timeHorizon);
        }

        public bool timeIsFetched(ILink link, TimeStamp time)
        {
            return prefetchMonitor.timeIsFetched(link, time);
        }

        public void addFetchedTime(ILink link, TimeStamp time)
        {
            prefetchMonitor.addFetchedTime(link, time);
        }

        public void updateLastRequestedTime(ILink link, TimeStamp time)
        {
            prefetchMonitor.updateLastRequestedTime(link, time);
        }

        private double CalculateTimePercentage(ITimeStamp start, ITimeStamp current, ITimeStamp stop)
        {
            var startD = start.ModifiedJulianDay;
            var stopD = stop.ModifiedJulianDay;
            var currentD = current.ModifiedJulianDay;

            if (currentD > stopD)
                return 100.0;

            stopD -= startD;
            currentD -= startD;

            return (currentD/stopD)*100.0;
        }

        public void Update(ILink link)
        {
            // don't do anything if we're disabled
            if (isEnabled == false)
            {
                return;
            }

            // see when this quantity was last requested on this link
            var lastRequestedTime = prefetchMonitor.lastRequestedTime(link.ID, link.SourceQuantity.ID);

            // if it never has been requested, then no one needs this
            // yet, and we can't do any prefetching at all since we don't
            // know what time to start prefetching until we get the
            // first request
            if (lastRequestedTime.ModifiedJulianDay == 0)
            {
                return;
            }

            // there has been at least one request for values for this
            // quantity, so we should prefetch up to the limit
            var linkId = link.ID;
            var quantityId = link.SourceQuantity.ID;
            var elementSet = link.TargetElementSet;

            // start prefetching from the latest fetched time on this link
            var prefetchTime = prefetchMonitor.latestFetchTime(link);

            // see what the time limit is for how far we want to prefetch
            var prefetchLimit = prefetchMonitor.getTimeLimit();

            // prefetch up to the prefetch limit
            while (true)
            {
                // see what the next estimated request time after the latest
                // prefetch time is and what the prefetch limit is
                prefetchTime = prefetchMonitor.nextEstTimeReq(prefetchTime, linkId, quantityId);

                // stop when we pass the prefetch limit
                if (prefetchTime.ModifiedJulianDay > prefetchLimit.ModifiedJulianDay)
                {
                    break;
                }

                // see if we've already requested this time for this
                // link ourselves, in which case we don't need to bother checking
                if (prefetchMonitor.timeIsFetched(link, prefetchTime) == true)
                {
                    continue;
                }

                // log the prefetch status
                var prefetchCompletion = CalculateTimePercentage(prefetchMonitor.getTimeHorizon().Start, prefetchTime, prefetchLimit);
                traceFile.Append(string.Format("Prefetch:Link{0}/{1}:{2}-{3}:{4}%", linkId, quantityId, prefetchTime, prefetchLimit, (int)prefetchCompletion));

                // generate the key for the next time to prefetchh
                var key = ValueSetEntry.CreateKey(webServiceManager.FindServiceIdForQuantity(quantityId), quantityId, elementSet.ID, prefetchTime, scenarioId);

                // see if it's already in the buffer
                if (mapValueSet.containsKey(key) == true)
                {
                    // it must have been fetched by someone else, so update our state
                    prefetchMonitor.addFetchedTime(link, prefetchTime);
                    continue;
                }

                // create a request for the next time to prefetch
                var valueSetRequestEntry = new ValueSetRequestEntry(webServiceManager.FindServiceIdForQuantity(quantityId), quantityId, elementSet.ID, prefetchTime, scenarioId);

                // attempt to insert the request immediately (non-blocking)
                if (queueValueSetRequest.offer(valueSetRequestEntry) == true)
                {
                    traceFile.Append(string.Format("Prefetch:Requested:Link{0}/{1}:{2}", linkId, quantityId, prefetchTime));
                    statistics.Add("Prefetch:RequestCount", 1);

                    // remember that this time has been prefetched
                    prefetchMonitor.addFetchedTime(link, prefetchTime);
                }
                else
                {
                    traceFile.Append(string.Format("Prefetch:RequestOfferFailed:Link{0}/{1}:{2}", linkId, quantityId, prefetchTime));
                    statistics.Add("Prefetch:RequestOfferFail", 1);

                    // stop trying to prefetch if the request queue is full
                    break;
                }
            }
        }
    }
}