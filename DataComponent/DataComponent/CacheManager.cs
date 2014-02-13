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

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;
using Hazelcast.Client;
using KState.Util;
using Oatc.OpenMI.Sdk.Backbone;
using OpenMI.Standard;

namespace KState.DataComponent
{
    public class CacheManager
    {
        private readonly HazelcastClient client;
        private readonly List<string> elementSetsPut;
        private readonly string scenarioId;
        private readonly Statistics statistics;
        private readonly TraceFile traceFile;
        private readonly WebServiceManager webServiceManager;

        public CacheManager(Statistics statistics, TraceFile traceFile, HazelcastClient client, string scenarioId, List<string> elementSetsPut, WebServiceManager webServiceManager)
        {
            this.statistics = statistics;
            this.traceFile = traceFile;
            this.client = client;
            this.scenarioId = scenarioId;
            this.elementSetsPut = elementSetsPut;
            this.webServiceManager = webServiceManager;
        }

        /**
         * Returns the ValueSet that corresponds to requestTime.
         */

        public IValueSet GetValues(ITime requestedTime, IQuantity quantity, IElementSet elementSet, PrefetchManager prefetchManager, ILink link)
        {
            try
            {
                // generate the key of the buffer entry we're looking for
                var key = ValueSetEntry.CreateKey(webServiceManager.FindServiceIdForQuantity(quantity.ID), quantity.ID, elementSet.ID, requestedTime, scenarioId);

                traceFile.Append("GetValues: " + key);

                var mapValueSet = client.getMap<string, ValueSetEntry>("valueSet");
                var mapElementSet = client.getMap<string, ElementSetEntry>("elementSet");
                var queueValueSetRequest = client.getQueue<ValueSetRequestEntry>("valueSetRequest");

                // record this request
                statistics.Add("GetValuesCount", 1);

                // measure our wait time
                var waitStopwatch = Stopwatch.StartNew();

                // see if the requested values are in the cache
                var valueSetEntry = mapValueSet.get(key);

                // if the value set does not exist then request it
                if (valueSetEntry == null)
                {
                    traceFile.Append("Requesting Value Set: " + key);

                    // insert the element set if necessary
                    if (elementSetsPut.Contains(elementSet.ID) == false)
                    {
                        elementSetsPut.Add(elementSet.ID);
                        mapElementSet.put(elementSet.ID, new ElementSetEntry(elementSet));
                    }

                    while (valueSetEntry == null)
                    {
                        // if we're prefetching, we may have already requested this
                        // value set in a previous prefetch that hasn't been fulfilled
                        // yet in which case we do not want to issue the request again.
                        if (prefetchManager.timeIsFetched(link, (TimeStamp)requestedTime) == false)
                        {
                            // insert the request into the queue
                            var insertRequestStopwatch = Stopwatch.StartNew();

                            // create the request entry
                            var valueSetRequestEntry = new ValueSetRequestEntry(webServiceManager.FindServiceIdForQuantity(quantity.ID), quantity.ID, elementSet.ID, Utils.ITimeToDateTime(requestedTime), scenarioId);

                            // BLOCKING
                            queueValueSetRequest.put(valueSetRequestEntry);

                            statistics.Add("RequestInsertTimeMS", insertRequestStopwatch.ElapsedMilliseconds);
                            traceFile.Append("RequestInsertTime:" + string.Format("{0:0.0}", insertRequestStopwatch.ElapsedMilliseconds) + "ms");
                        }

                        // PERFORMANCE STUDY: start delay at 100 and double each time
                        // we check and it's not available

                        // poll for the value set
                        var delay = 1000;
                        while (true)
                        {
                            // we know that the value set is not in the cache since
                            // we just checked that above, so wait immediately after
                            // making the request
                            Thread.Sleep(delay);

                            valueSetEntry = mapValueSet.get(key);
                            if (valueSetEntry != null)
                            {
                                break;
                            }
                            //delay *= 2;
                            traceFile.Append(string.Format("Waiting ({0}) For Value Set: ({1})", delay, key));

                            if (delay > 20000)
                            {
                                statistics.Add("RequestRetry", 1);
                                break;
                            }
                        }
                    }
                }

                statistics.Add("CacheWaitTimeMS", waitStopwatch.ElapsedMilliseconds);
                traceFile.Append("WaitTime:" + string.Format("{0:0.0}", waitStopwatch.ElapsedMilliseconds) + "ms");

                return new ScalarSet(valueSetEntry.Values());
            }
            catch (Exception e)
            {
                traceFile.Exception(e);
                return null;
            }
        }
    }
}