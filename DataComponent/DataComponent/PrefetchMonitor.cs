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
using System.Collections;
using System.Collections.Generic;
using System.Text;
using KState.Util;
using Oatc.OpenMI.Sdk.Backbone;
using OpenMI.Standard;

namespace KState.DataComponent
{
    /// Provides an object that monitors the output links and tracks the most
    /// recent request interval for each link+quantity (which usually
    /// corresponds to the time step that a quantity is being request at along
    /// a link) and can be asked for what the next time a quantity will likely
    /// be requested along a link.
    /// 
    /// The PrefetchManager uses an instance of this class internally.
    public class PrefetchMonitor
    {
        private readonly Dictionary<string, LinkQuantityInfo> items;
        private readonly ITimeSpan timeHorizon;
        private readonly TraceFile traceFile;

        public PrefetchMonitor(ArrayList outputLinks, TraceFile traceFile, ITimeSpan timeHorizon)
        {
            this.traceFile = traceFile;
            this.timeHorizon = timeHorizon;
            items = new Dictionary<string, LinkQuantityInfo>();

            foreach (DataOutputLink link in outputLinks)
            {
                var linkId = getLinkId(link.link);
                var linkInfo = new LinkQuantityInfo(linkId);
                items[linkId] = linkInfo;
            }
        }

        public ITimeSpan getTimeHorizon()
        {
            return timeHorizon;
        }

        private String getLinkId(ILink link)
        {
            return link.ID + link.SourceQuantity.ID;
        }

        public TimeStamp lastRequestedTime(String linkId, String quantityName)
        {
            var linkInfo = items[linkId + quantityName];
            return linkInfo.getLastRequestedTime();
        }

        public void updateLastRequestedTime(ILink link, TimeStamp time)
        {
            traceFile.Append(string.Format("Prefetch:UpdateLastRequestedTime:Link{0}={1}", link.ID, time.ModifiedJulianDay));

            // we name each link+quantity item as the concatenation of the link's
            // id and the quantity name so that each pair has a unique name
            var linkId = getLinkId(link);

            // get the link info object for this link+quantity combination
            var linkInfo = items[linkId];

            // calculate the time delta from the most recent requested time,
            // which is basically our estimate of the time step length
            var delta = time.ModifiedJulianDay - linkInfo.getLastRequestedTime().ModifiedJulianDay;

            // log it
            traceFile.Append(string.Format("Prefetch:StepLengthEst:Link{0}={1}", link.ID, delta));

            // update the link
            linkInfo.setDelta(delta);

            // update the most recent requested time
            linkInfo.setLastRequestedTime(time);
        }

        public bool timeIsFetched(ILink link, TimeStamp time)
        {
            var linkId = getLinkId(link);
            return items[linkId].timeIsFetched(time);
        }

        public void addFetchedTime(ILink link, TimeStamp time)
        {
            var linkId = getLinkId(link);
            items[linkId].addFetchedTime(time);
        }

        public TimeStamp latestFetchTime(ILink link)
        {
            var linkId = getLinkId(link);
            return items[linkId].latestFetchTime();
        }

        public double estTimeReq(ILink link)
        {
            var linkId = getLinkId(link);
            var info = items[linkId];
            return info.getDelta();
        }

        public TimeStamp nextEstTimeReq(TimeStamp latestTime, String id)
        {
            var info = items[id];
            return new TimeStamp(latestTime.ModifiedJulianDay + info.getDelta());
        }

        public TimeStamp nextEstTimeReq(TimeStamp latestTime, String linkId, String quantityName)
        {
            return nextEstTimeReq(latestTime, linkId + quantityName);
        }

        public double getLongestRequestInterval()
        {
            double longest = 0;
            foreach (var linkInfo in items.Values)
            {
                if (linkInfo.getDelta() > longest)
                    longest = linkInfo.getDelta();
            }
            return longest;
        }

        /// Calculates the prefetch time limit. The limit is the minimum
        /// prefetch time across all links plus some multiple of the largest
        /// request interval. In addition, the prefetch limit has a maximum
        /// value of the composition's ending time.
        public ITimeStamp getTimeLimit()
        {
            // find the minimum time that all links are fetched to
            var minimumFetchedTime = Double.MaxValue;
            foreach (var linkInfo in items.Values)
            {
                var fetchedTime = linkInfo.latestFetchTime().ModifiedJulianDay;
                if (fetchedTime < minimumFetchedTime)
                    minimumFetchedTime = fetchedTime;
            }

            // calculate the new prefetch limit based on what has already been
            // fetched
            ITimeStamp prefetchLimit = new TimeStamp(0);
            if (minimumFetchedTime != 0)
                prefetchLimit = new TimeStamp(minimumFetchedTime + getLongestRequestInterval());

            traceFile.Append(string.Format("Prefetch:Time:Min:{0}:Longest:{1}:End:{2}", minimumFetchedTime, getLongestRequestInterval(), timeHorizon.End.ModifiedJulianDay));

            // make sure the limit is not later than the ending time of the
            // composition
            if (prefetchLimit.ModifiedJulianDay > timeHorizon.End.ModifiedJulianDay)
                prefetchLimit = timeHorizon.End;

            return prefetchLimit;
        }

        public String summary()
        {
            var sb = new StringBuilder();

            sb.Append("LinkMonitor:" + "\r\n");
            sb.Append("  longest: " + getLongestRequestInterval() + "\r\n");
            sb.Append("  items (" + items.Count + "):\r\n");

            foreach (var linkInfo in items.Values)
                sb.Append("    " + linkInfo.getId() + ":" + linkInfo.getLastRequestedTime() + ":" + linkInfo.getDelta());

            return sb.ToString();
        }

        public void clearFetchedTimes()
        {
            foreach (var linkInfo in items.Values)
                linkInfo.clearFetchedTimes();
        }

        /**
         * Simulates updates across various links.
         */

        public static bool unitTest()
        {
            try
            {
                // assume two links
                //String linkOneId = "L1";
                //String linkTwoId = "L2";

                // assume three quantities
                //String linkOneQuantityA = "QA";
                //String linkOneQuantityB = "QB";
                //String linkTwoQuantityC = "QC";
                /*
                double currentTime = CalendarConverter.gregorian2ModifiedJulian(Calendar.getInstance());

                // simulate updates for some number of days
                for (int i = 0; i < 21; i++)
                {
                    // summarize the monitor's state
                    //System.out.println(LinkMonitor.getInstance().summary());

                    ILink linkA = new Link(null, linkOneQuantityA);
                    ILink linkB = new Link(null, linkOneQuantityB);
                    // ILink linkC = new Link(null, linkOneQuantityC);

                    // pretend that link one is requested at a daily time step
                    LinkMonitor.getInstance().updateLastRequestedTime(linkA, new TimeStamp(currentTime));
                    LinkMonitor.getInstance().updateLastRequestedTime(linkB, new TimeStamp(currentTime));

                    // pretend link two is requested at a weekly time step
                    // if (i % 7 == 0)
                    // LinkMonitor.getInstance().update(linkTwoId,
                    // linkTwoQuantityC, new TimeStamp(currentTime));

                    currentTime += 1.0;
                }*/

                return true;
            }
            catch (Exception e)
            {
                //System.err.println(e.getMessage());
                return false;
            }
        }

        private class LinkQuantityInfo
        {
            private readonly List<TimeStamp> fetchedTimes;
            private readonly String id;
            private double delta;
            private TimeStamp lastRequestedTime;

            public LinkQuantityInfo(String id)
            {
                this.id = id;
                lastRequestedTime = new TimeStamp(0);
                fetchedTimes = new List<TimeStamp>();
                delta = 0;
            }

            public String getId()
            {
                return id;
            }

            public void setLastRequestedTime(TimeStamp value)
            {
                lastRequestedTime = value;
            }

            public TimeStamp getLastRequestedTime()
            {
                return lastRequestedTime;
            }

            public void addFetchedTime(TimeStamp value)
            {
                fetchedTimes.Add(value);
            }

            public bool timeIsFetched(TimeStamp value)
            {
                return fetchedTimes.Contains(value);
            }

            public TimeStamp latestFetchTime()
            {
                var latest = new TimeStamp(0);
                foreach (var time in fetchedTimes)
                    if (time.ModifiedJulianDay > latest.ModifiedJulianDay)
                        latest = time;
                return latest;
            }

            public void setDelta(double value)
            {
                delta = value;
            }

            public double getDelta()
            {
                return delta;
            }

            public void clearFetchedTimes()
            {
                fetchedTimes.Clear();
            }
        }
    }
}