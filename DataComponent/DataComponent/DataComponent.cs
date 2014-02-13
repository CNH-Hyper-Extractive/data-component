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
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading;
using Hazelcast.Client;
using KState.Util;
using Oatc.OpenMI.Sdk.Backbone;
using Oatc.OpenMI.Sdk.DevelopmentSupport;
using Oatc.OpenMI.Sdk.Spatial;
using Oatc.OpenMI.Sdk.Wrapper;
using OpenMI.Standard;
using TimeSpan = Oatc.OpenMI.Sdk.Backbone.TimeSpan;

namespace KState.DataComponent
{
    /// <summary>
    ///     The LinkableRunEngine  implements the run time part of the ILinkableComponent interface.
    ///     The remaining methods are implemented in the derived LinkableEngine class. There are
    ///     historical reasons for splitting the functionality between the two classes.
    ///     The LinkableRunEngine class and the LinkableEngine class could be merged,
    ///     but for the time being these are keeps as they are in order to support backward compatibility.
    ///     This assembly must be .NET 4.0 because that is what the Hazelcast.Client is built for.
    /// </summary>
    [Serializable]
    public class DataComponent : LinkableComponent, IListener
    {
        private readonly List<string> elementSetsPut = new List<string>();
        private CacheManager cacheManager;
        private HazelcastClient client;
        private string compositionId;

        protected ArrayList dataInputLinks;
        protected ArrayList dataOutputLinks;
        private bool enablePrefetching;
        private string filePath;

        /// <summary>
        ///     True if the Initialize method was invoked
        /// </summary>
        protected bool initializeWasInvoked;

        private List<InputExchangeItem> inputs = new List<InputExchangeItem>();

        /// <summary>
        ///     True if the component is gathering data from other LinkableComponents
        /// </summary>
        protected bool isBusy;

        private string modelDescription;
        private string modelId;
        private List<OutputExchangeItem> outputs = new List<OutputExchangeItem>();
        private string perId;
        private PrefetchManager prefetchHelper;

        /// <summary>
        ///     True if the Prepare method was invoked
        /// </summary>
        protected bool prepareForCompotationWasInvoked;

        /// <summary>
        ///     Arraylist of published event types
        /// </summary>
        protected ArrayList publishedEventTypes;

        private Stopwatch runtimeWatch;
        private string scenarioId;
        private double simulationEndTime;
        private double simulationStartTime;
        private Statistics statistics;

        /// <summary>
        ///     used when comparing time in the IsLater method (see property TimeEpsilon)
        /// </summary>
        protected double timeEpsilon; // used when comparing time in the IsLater method (see property TimeEpsilon)

        private TraceFile traceFile;

        /// <summary>
        ///     The current validateion error message
        /// </summary>
        protected ArrayList validationErrorMessages;

        /// <summary>
        ///     Current validation string from the Validate method
        /// </summary>
        protected ArrayList validationWarningMessages;

        private int version = 2;
        private WebServiceManager webServiceManager;


        /// <summary>
        ///     Constructor method for the LinkableRunEngine class
        /// </summary>
        public DataComponent()
        {
            initializeWasInvoked = false;
            prepareForCompotationWasInvoked = false;
            timeEpsilon = 0.10*1.0/(3600.0*24.0);

            publishedEventTypes = new ArrayList();
            publishedEventTypes.Add(EventType.DataChanged);
            publishedEventTypes.Add(EventType.Informative);
            publishedEventTypes.Add(EventType.SourceAfterGetValuesCall);
            publishedEventTypes.Add(EventType.SourceBeforeGetValuesReturn);
            publishedEventTypes.Add(EventType.TargetAfterGetValuesReturn);
            publishedEventTypes.Add(EventType.TargetBeforeGetValuesCall);

            validationWarningMessages = new ArrayList();
            validationErrorMessages = new ArrayList();

            dataInputLinks = new ArrayList();
            dataOutputLinks = new ArrayList();
        }

        #region Prepare

        /// <summary>
        ///     Prepare. This method will be invoked after end of configuration and before the first GetValues call
        /// </summary>
        public override void Prepare()
        {
            try
            {
                if (!initializeWasInvoked)
                {
                    throw new Exception("PrepareForComputation method in SmartWrapper cannot be invoked before the Initialize method has been invoked");
                }

                Validate();

                if (validationErrorMessages.Count > 0)
                {
                    var errorMessage = "";
                    foreach (string str in validationErrorMessages)
                    {
                        errorMessage += "Error: " + str + ". ";
                    }

                    throw new Exception(errorMessage);
                }

                /*foreach (CacheOutputLink cacheOutputLink in _cacheOutputLinks)
                {
                    cacheOutputLink.UpdateBuffer();
                }*/

                // start the link monitor
                //LinkMonitor.Instance.Start(this.cacheOutputLinks, this.traceFile);

                // MUST BE DONE AFTER LNKS ADDED
                prefetchHelper = new PrefetchManager(traceFile, statistics, client, scenarioId, dataOutputLinks, TimeHorizon, enablePrefetching, webServiceManager);

                //PrefetchMonitor.Instance.Start(this.dataOutputLinks, this.traceFile);
                //PrefetchMonitor.Instance.setTimeHorizon(this.TimeHorizon);


                prepareForCompotationWasInvoked = true;
            }
            catch (Exception e)
            {
                var message = "Exception in LinkableComponent. ";
                message += "ComponentID: " + ComponentID + "\n";
                throw new Exception(message, e);
            }
        }

        #endregion

        #region Initialize

        private string GetUniqueId()
        {
            return Convert.ToInt32(compositionId).ToString("D3") + Convert.ToInt32(perId).ToString("D3");
        }

        private string GetFixedId()
        {
            return Convert.ToInt32("0").ToString("D3") + Convert.ToInt32("0").ToString("D3");
        }

        public override void Initialize(IArgument[] properties)
        {
            // read the component arguments from the omi file
            var arguments = new Dictionary<string, string>();
            for (var i = 0; i < properties.Length; i++)
            {
                arguments.Add(properties[i].Key, properties[i].Value);
            }

            // obtain our parameters (assume all parameters are specified
            // via a file or the omi file)
            enablePrefetching = Convert.ToBoolean(arguments["param_enable_prefetching"]);
            var instanceAddress = arguments["param_instance_address"];
            var instanceAssignment = arguments["param_instance_assignment"];
            compositionId = arguments["param_composition_id"];
            perId = arguments["param_per_id"];
            var configFile = arguments["param_config_file"];

            // generate a unique id to use as the scenario id
            //this.scenarioId = "S" + GetUniqueId();
            scenarioId = "S" + GetFixedId();

            // remember our path
            filePath = Path.GetDirectoryName(Path.GetFullPath(configFile));

            // each instance uses a different port which is based on the
            // instance's id number
            var port = 5701 + Convert.ToInt32(instanceAssignment);

            // read the element set file
            var elementSets = ElementSetReader.read("ElementSet.xml");

            // read the config file
            var componentProperties = ComponentProperties.read(configFile, this, elementSets);
            var extras = componentProperties.getExtras();
            foreach (var nextKey in extras.Keys)
            {
                arguments[nextKey] = extras[nextKey];
            }

            // set our data members
            simulationStartTime = componentProperties.getStartDateTime();
            simulationEndTime = componentProperties.getEndDateTime();
            inputs = componentProperties.getInputExchangeItems();
            outputs = componentProperties.getOutputExchangeItems();
            modelId = componentProperties.getModelId();
            modelDescription = componentProperties.getModelDescription();

            // create the trace file
            traceFile = new TraceFile(ModelID + "-" + compositionId + "-" + scenarioId);
            traceFile.Append("Initialize");
            traceFile.Append("Version: " + ModelID + " v" + version);
            traceFile.Append("Instance Address: " + instanceAddress);
            traceFile.Append("Instance Assignment: " + instanceAssignment);
            traceFile.Append("Instance Port: " + port);
            traceFile.Append("Composition ID: " + compositionId);
            traceFile.Append("Per ID: " + perId);
            traceFile.Append("Enable Prefetching: " + enablePrefetching);
            traceFile.Append("Path: " + filePath);
            traceFile.Append("ScenarioId: " + scenarioId);
            traceFile.Append("UniqueId: " + GetUniqueId());

            webServiceManager = new WebServiceManager();
            webServiceManager.Read("WebServices.xml");

            // give the instances a chance to start - PERFORMANCE STUDY
            /*int delay = 1000 * (30 + Convert.ToInt32(this.compositionId));
            this.traceFile.Append("Pausing startup: " + delay);
            System.Threading.Thread.Sleep(delay);*/

            /*if (this.compositionId == "0" && this.perId == "0")
            {
                this.traceFile.Append("NO Extended startup delay");
            }
            else
            {
                this.traceFile.Append("EXTENDED startup delay");
                System.Threading.Thread.Sleep(1000 * 60 * 15);
            }*/

            traceFile.Append("Connecting to instance");
            ConnectToInstance(instanceAddress, Convert.ToString(port), webServiceManager.WebServiceInfoList);

            traceFile.Append("Finishing startup");

            statistics = new Statistics(compositionId, perId);

            cacheManager = new CacheManager(statistics, traceFile, client, scenarioId, elementSetsPut, webServiceManager);

            initializeWasInvoked = true;

            runtimeWatch = new Stopwatch();
            runtimeWatch.Start();

            traceFile.Append("Finished initialization");
        }

        private void ConnectToInstance(string instanceAddr, string instancePort, List<WebServiceEntry> webServiceInfoList)
        {
            var clientConfig = new ClientConfig();
            clientConfig.addAddress(instanceAddr + ":" + instancePort);
            clientConfig.GroupConfig = new GroupConfig("dev", "dev-pass");
            clientConfig.TypeConverter = new MyTypeConverter();

            // connect to the hazelcast instance
            while (client == null)
            {
                try
                {
                    client = HazelcastClient.newHazelcastClient(clientConfig);
                }
                catch (Exception)
                {
                    traceFile.Append("Failed to connect to " + instanceAddr + ":" + instancePort);
                    Thread.Sleep(5000);
                }
            }

            // tell the instances that we're active
            var mapClient = client.getMap<string, string>("client");
            mapClient.put(GetUniqueId(), "active");

            var mapWebServiceInfo = client.getMap<String, WebServiceEntry>("webService");
            foreach (var nextWebServiceInfo in webServiceInfoList)
            {
                mapWebServiceInfo.put(nextWebServiceInfo.Id, nextWebServiceInfo);
            }
        }

        private void DisconnectFromInstance()
        {
            // tell the instances that we're finished
            var mapClient = client.getMap<string, string>("client");
            mapClient.remove(GetUniqueId());

            // don't shutdown
            while (true)
            {
                traceFile.Append("Finished, waiting for instance to finish");
                Thread.Sleep(30*1000);
            }

            // shut down the client
            //this.client.doShutdown();
        }

        #endregion

        #region Finish

        public override void Finish()
        {
            statistics.Add("RUNTIME_MS", runtimeWatch.ElapsedMilliseconds);

            traceFile.Append(statistics.ToXml().ToString());
            traceFile.Append(statistics.ToCsv().ToString());

            try
            {
                Encoding encoding = new UTF8Encoding();
                File.WriteAllBytes(Path.Combine(filePath, "DCProfile-" + GetUniqueId() + ".csv"), encoding.GetBytes(statistics.ToCsv().ToString()));
            }
            catch (Exception)
            {
            }

            DisconnectFromInstance();
        }

        #endregion

        public override ITimeStamp EarliestInputTime
        {
            get
            {
                // not used, but expected by the UI when showing model properties
                return new TimeStamp(simulationStartTime);
            }
        }

        /// <summary>
        ///     This _timeEpsilon variable is used when comparing the current time in the engine with
        ///     the time specified in the parameters for the GetValue method.
        ///     if ( requestedTime > engineTime + _timeEpsilon) then PerformTimestep()..
        ///     The default values for _timeEpsilon is double.Epsilon = 4.94065645841247E-324
        ///     The default value may be too small for some engines, in which case the _timeEpsilon can
        ///     be changed the class that you have inherited from LinkableRunEngine og LinkableEngine.
        /// </summary>
        public double TimeEpsilon
        {
            get { return timeEpsilon; }
            set { timeEpsilon = value; }
        }

        public override string ComponentDescription
        {
            get { return modelDescription; }
        }

        public override string ComponentID
        {
            get { return modelId; }
        }

        /// <summary>
        ///     Model descscription
        /// </summary>
        public override string ModelDescription
        {
            get { return "DataComponent"; }
        }

        /// <summary>
        ///     Model ID
        /// </summary>
        public override string ModelID
        {
            get { return "DataComponent"; }
        }

        /// <summary>
        ///     Time Horizon
        /// </summary>
        public override ITimeSpan TimeHorizon
        {
            get { return new TimeSpan(new TimeStamp(simulationStartTime), new TimeStamp(simulationEndTime)); }
        }

        public override int InputExchangeItemCount
        {
            get
            {
                if (inputs == null)
                    return 0;
                return inputs.Count;
            }
        }

        /// <summary>
        ///     Number of output exchange items
        /// </summary>
        public override int OutputExchangeItemCount
        {
            get
            {
                if (outputs == null)
                    return 0;
                return outputs.Count;
            }
        }

        public EventType GetAcceptedEventType(int acceptedEventTypeIndex)
        {
            return EventType.Other;
        }

        public int GetAcceptedEventTypeCount()
        {
            return 0;
        }

        public void OnEvent(IEvent e)
        {
            try
            {
                traceFile.Append("OnEvent:From:" + e.Sender.ModelID);

                var component = e.Sender;

                // look through all the input links and call getvalues on all input
                // links connected to the event's sender component
                foreach (DataInputLink link in dataInputLinks)
                {
                    // see if this link is from the component that raised the event
                    if (link.link.SourceComponent.ModelID == component.ModelID)
                    {
                        // get the new valueset
                        var valueSet = e.Sender.GetValues(e.SimulationTime, link.link.ID);

                        // log it
                        traceFile.Append("OnEvent:ValueSet(" + valueSet.Count + ")");

                        // create a value set entry
                        var scalarSet = (ScalarSet)valueSet;
                        var values = scalarSet.data;
                        var entry = new ValueSetEntry(webServiceManager.FindServiceIdForQuantity(link.link.SourceQuantity.ID), link.link.SourceQuantity.ID, link.link.SourceElementSet.ID, e.SimulationTime, scenarioId, values, true);

                        // measure how long it takes to insert the value set
                        var insertStopwatch = Stopwatch.StartNew();

                        // inser the value set
                        AddValueSetEntry(entry, link.link.SourceElementSet);

                        // record the insert time
                        statistics.Add("EntryInsertTimeMS", insertStopwatch.ElapsedMilliseconds);

                        // log the insert time
                        traceFile.Append("EntryInsertTimeMS:" + insertStopwatch.ElapsedMilliseconds);
                    }
                }
            }
            catch (Exception exception)
            {
                var message = "Exception in DataCollectorComponent:OnEvent:" + e;
                throw new Exception(message, exception);
            }
        }

        public override IValueSet GetValues(ITime time, string LinkID)
        {
            try
            {
                CheckTimeArgumentInGetvaluesMethod(time);
                SendSourceAfterGetValuesCallEvent(time, LinkID);
                IValueSet engineResult = new ScalarSet();

                // find the output link being requested
                var link = FindLinkWithId(LinkID);

                // log this call
                traceFile.Append("GetValues:" + link.link.SourceQuantity.ID + "@" + ((TimeStamp)time).ModifiedJulianDay);

                // update our link monitor
                prefetchHelper.updateLastRequestedTime(link.link, (TimeStamp)time);

                // retrieve the result from the link
                engineResult = link.GetValue(time, prefetchHelper);

                SendEvent(EventType.SourceBeforeGetValuesReturn);
                return engineResult;
            }
            catch (Exception e)
            {
                var message = "Exception in LinkableComponent:ID: ";
                message += ComponentID;
                throw new Exception(message, e);
            }
        }

        public override void AddLink(ILink newLink)
        {
            try
            {
                if (initializeWasInvoked == false)
                {
                    throw new Exception("AddLink method in the SmartWrapper cannot be invoked before the Initialize method has been invoked");
                }
                if (prepareForCompotationWasInvoked)
                {
                    throw new Exception("AddLink method in the SmartWrapper cannot be invoked after the PrepareForComputation method has been invoked");
                }

                if (newLink.TargetComponent == this)
                {
                    dataInputLinks.Add(CreateInputLink(newLink));

                    // listen for data changed events on this link
                    newLink.SourceComponent.Subscribe(this, EventType.DataChanged);
                }
                else if (newLink.SourceComponent == this)
                {
                    dataOutputLinks.Add(CreateOutputLink(newLink));
                }
                else
                {
                    throw new Exception("SourceComponent.ID or TargetComponent.ID in Link does not match the Component ID for the component to which the Link was added");
                }
            }
            catch (Exception e)
            {
                var message = "Exception in LinkableComponent. ";
                message += "ComponentID: " + ComponentID + "\n";
                throw new Exception(message, e);
            }
        }

        public virtual DataInputLink CreateInputLink(ILink link)
        {
            return new DataInputLink(link);
        }

        public virtual DataOutputLink CreateOutputLink(ILink link)
        {
            var cacheOutputLink = new DataOutputLink(link, cacheManager); //, this.prefetchHelper);
            cacheOutputLink.Initialize();
            return cacheOutputLink;
        }

        public override void Dispose()
        {
        }

        private DataOutputLink FindLinkWithId(String linkId)
        {
            for (var i = 0; i < dataOutputLinks.Count; i++)
            {
                var cacheLink = (DataOutputLink)dataOutputLinks[i];
                if (cacheLink.link.ID == linkId == true)
                {
                    return cacheLink;
                }
            }
            return null;
        }

        public override void RemoveLink(string LinkID)
        {
            try
            {
                if (!initializeWasInvoked)
                {
                    throw new Exception("Illegal invocation of RemoveLink method before invocation of Initialize method");
                }

                if (prepareForCompotationWasInvoked)
                {
                    throw new Exception("Illegal invocation of RemoveLink method after invocation of Prepare method");
                }


                var index = -999;
                for (var i = 0; i < dataInputLinks.Count; i++)
                {
                    if (((SmartInputLink)dataInputLinks[i]).link.ID == LinkID)
                    {
                        index = i;
                        break;
                    }
                }

                if (index != -999)
                {
                    dataInputLinks.RemoveAt(index);
                }
                else
                {
                    for (var i = 0; i < dataOutputLinks.Count; i++)
                    {
                        if (((SmartOutputLink)dataOutputLinks[i]).link.ID == LinkID)
                        {
                            index = i;
                            break;
                        }
                    }
                    dataOutputLinks.RemoveAt(index);
                }

                if (index == -999)
                {
                    throw new Exception("Failed to find link.ID in internal link lists in method RemoveLink()");
                }
            }
            catch (Exception e)
            {
                var message = "Exception in LinkableComponent. ";
                message += "ComponentID: " + ComponentID + "\n";
                throw new Exception(message, e);
            }
        }

        public override EventType GetPublishedEventType(int providedEventTypeIndex)
        {
            return (EventType)publishedEventTypes[providedEventTypeIndex];
        }

        public override int GetPublishedEventTypeCount()
        {
            return publishedEventTypes.Count;
        }

        public static TimeStamp TimeToTimeStamp(ITime time)
        {
            TimeStamp t;

            if (time is ITimeStamp)
            {
                t = new TimeStamp(((ITimeStamp)time).ModifiedJulianDay);
            }
            else
            {
                t = new TimeStamp(((ITimeSpan)time).End.ModifiedJulianDay);
            }

            return t;
        }

        /// <summary>
        ///     Will compare two times. If the first argument t1, is later than the second argument t2
        ///     the method will return true. Otherwise false will be returned. t1 and t2 can be of types
        ///     ITimeSpan or ITimeStamp.
        /// </summary>
        /// <param name="t1">First time</param>
        /// <param name="t2">Second time</param>
        /// <returns>isLater</returns>
        protected bool IsLater(ITime t1, ITime t2)
        {
            double mt1, mt2;
            var isLater = false;

            mt1 = TimeToTimeStamp(t1).ModifiedJulianDay;
            mt2 = TimeToTimeStamp(t2).ModifiedJulianDay;

            if (mt1 > mt2 + timeEpsilon)
            {
                isLater = true;
            }
            else
            {
                isLater = false;
            }

            return isLater;
        }

        public static string ITimeToString(ITime time)
        {
            string timeString;

            if (time is ITimeStamp)
            {
                timeString = (CalendarConverter.ModifiedJulian2Gregorian(((ITimeStamp)time).ModifiedJulianDay)).ToString();
            }
            else if (time is ITimeSpan)
            {
                timeString = "[" + (CalendarConverter.ModifiedJulian2Gregorian(((ITimeSpan)time).Start.ModifiedJulianDay)) + ", " + (CalendarConverter.ModifiedJulian2Gregorian(((ITimeSpan)time).End.ModifiedJulianDay)) + "]";
            }
            else
            {
                throw new Exception("Illigal type used for time, must be OpenMI.Standard.ITimeStamp or OpenMI.Standard.TimeSpan");
            }

            return timeString;
        }

        /// <summary>
        ///     Get an input exchange item
        /// </summary>
        /// <param name="index">index of the requested input exchange item</param>
        /// <returns>The input exchange item</returns>
        public override IInputExchangeItem GetInputExchangeItem(int index)
        {
            var inputExchangeItem = inputs[index];
            return inputExchangeItem;
        }

        /// <summary>
        ///     get a output exchange item
        /// </summary>
        /// <param name="index">index number of the requested output exchange item</param>
        /// <returns>The requested exchange item</returns>
        public override IOutputExchangeItem GetOutputExchangeItem(int index)
        {
            var outputExchangeItem = outputs[index];

            //Add dataoperations to outputExchangeItems
            var elementMapper = new ElementMapper();
            var dataOperations = new ArrayList();
            dataOperations = elementMapper.GetAvailableDataOperations(outputExchangeItem.ElementSet.ElementType);
            bool spatialDataOperationExists;
            bool linearConversionDataOperationExists;
            bool smartBufferDataOperationExists;
            foreach (IDataOperation dataOperation in dataOperations)
            {
                spatialDataOperationExists = false;
                foreach (IDataOperation existingDataOperation in outputExchangeItem.DataOperations)
                {
                    if (dataOperation.ID == existingDataOperation.ID)
                    {
                        spatialDataOperationExists = true;
                    }
                }

                if (!spatialDataOperationExists)
                {
                    outputExchangeItem.AddDataOperation(dataOperation);
                }
            }

            IDataOperation linearConversionDataOperation = new LinearConversionDataOperation();
            linearConversionDataOperationExists = false;
            foreach (IDataOperation existingDataOperation in outputExchangeItem.DataOperations)
            {
                if (linearConversionDataOperation.ID == existingDataOperation.ID)
                {
                    linearConversionDataOperationExists = true;
                }
            }

            if (!linearConversionDataOperationExists)
            {
                outputExchangeItem.AddDataOperation(new LinearConversionDataOperation());
            }

            IDataOperation smartBufferDataOperaion = new SmartBufferDataOperation();
            smartBufferDataOperationExists = false;
            foreach (IDataOperation existingDataOperation in outputExchangeItem.DataOperations)
            {
                if (smartBufferDataOperaion.ID == existingDataOperation.ID)
                {
                    smartBufferDataOperationExists = true;
                }
            }

            if (!smartBufferDataOperationExists)
            {
                outputExchangeItem.AddDataOperation(new SmartBufferDataOperation());
            }

            return outputExchangeItem;
        }

        public void AddValueSetEntry(ValueSetEntry valueSetEntry, IElementSet elementSet)
        {
            // add the element set (before adding the entry in case it
            // gets sent right away)
            if (elementSetsPut.Contains(elementSet.ID) == false)
            {
                elementSetsPut.Add(elementSet.ID);
                var elementSetMap = client.getMap<String, ElementSetEntry>("elementSet");
                elementSetMap.put(elementSet.ID, new ElementSetEntry(elementSet));
            }

            var queueValueSet = client.getQueue<ValueSetEntry>("valueSet");


            // POLLING WITH EXP BACKOFF - too much backoff to keep the queue filled with i1c200p1
            /*int delay = 2000;
            while (true)
            {
                bool inserted = queueValueSet.offer(valueSetEntry);
                if (inserted == true)
                {
                    break;
                }
                else
                {
                    // if the queue is full then the instance is overloaded so
                    // back off for awhile
                    this.traceFile.Append(String.Format("Waiting ({0}) To Insert ValueSet: {1}", delay, valueSetEntry.ToString()));
                    System.Threading.Thread.Sleep(delay);
                    delay *= 2;
                }
            }*/

            // OR

            // blocking put operation so that the put goes through as soon as there
            // is space
            queueValueSet.put(valueSetEntry);
        }

        /// <summary>
        ///     Validate the component
        /// </summary>
        /// <returns>Empty string if no warnings were issued, or a description if there were warnings</returns>
        public override string Validate()
        {
            validationErrorMessages.Clear();
            validationWarningMessages.Clear();

            foreach (DataInputLink link in dataInputLinks)
            {
                validationErrorMessages.AddRange(link.GetErrors());
                validationWarningMessages.AddRange(link.GetWarnings());
            }

            foreach (DataOutputLink link in dataOutputLinks)
            {
                validationErrorMessages.AddRange(link.GetErrors());
                validationWarningMessages.AddRange(link.GetWarnings());
            }

            var validationString = "";
            foreach (string str in validationErrorMessages)
            {
                validationString += "Error: " + str + " ";
            }

            foreach (string str in validationWarningMessages)
            {
                validationString += "Warning: " + str + ". ";
            }

            return validationString;
        }

        private void CheckTimeArgumentInGetvaluesMethod(ITime time)
        {
            if (time is ITimeSpan)
            {
                //if (this._engineApiAccess is IEngine)
                {
                    if (IsLater(TimeHorizon.Start, ((ITimeSpan)time).Start))
                    {
                        throw new Exception("GetValues method was invoked using a time argument that representes a time before the allowed time horizon");
                    }
                    if (IsLater(((ITimeSpan)time).End, TimeHorizon.End))
                    {
                        throw new Exception("GetValues method was invoked using a time argument that representes a time that is after the allowed time horizon");
                    }
                }
            }
            else if (time is ITimeStamp)
            {
                //if (this._engineApiAccess is IEngine)
                {
                    if (IsLater(TimeHorizon.Start, time))
                    {
                        throw new Exception("GetValues method was invoked using a time argument that representes a time before the allowed time horizon");
                    }
                    if (IsLater(time, TimeHorizon.End))
                    {
                        throw new Exception("GetValues method was invoked using a time argument that representes a time that is after the allowed time horizon");
                    }
                }
            }
            else
            {
                throw new Exception("Illegal data type for time was used in argument to GetValues method. Type must be OpenMI.Standard.ITimeStamp or ITimeSpan");
            }
        }

        private void SendSourceAfterGetValuesCallEvent(ITime time, string LinkID)
        {
            /*Oatc.OpenMI.Sdk.Backbone.Event eventA = new Oatc.OpenMI.Sdk.Backbone.Event(EventType.SourceAfterGetValuesCall);
            eventA.Description = "GetValues(t = " + ITimeToString(time) + ", ";
            eventA.Description += "LinkID: " + LinkID; //TODO: QS = " + _smartOutputLinkSet.GetLink(LinkID).SourceQuantity.ID + " ,QT = " + _smartOutputLinkSet.GetLink(LinkID).TargetQuantity.ID;
            eventA.Description += ") <<<===";
            eventA.Sender = this;
            eventA.SimulationTime = TimeToTimeStamp(_engineApiAccess.GetCurrentTime());
            eventA.SetAttribute("GetValues time argument : ", ITimeToString(time));
            SendEvent(eventA);*/
        }

        private void SendEvent(EventType eventType)
        {
            /*Oatc.OpenMI.Sdk.Backbone.Event eventD = new Oatc.OpenMI.Sdk.Backbone.Event(eventType);
            eventD.Description = eventType.ToString();
            eventD.Sender = this;
            eventD.SimulationTime = TimeToTimeStamp(_engineApiAccess.GetCurrentTime());
            SendEvent(eventD);*/
        }

        #region IManagedState

        public virtual string KeepCurrentState()
        {
            throw new Exception("KeepCurrentState was called but the engine does not implement IManageState");
        }

        public virtual void RestoreState(string stateID)
        {
            throw new Exception("RestoreState was called but the engine does not implement IManageState");
        }

        public virtual void ClearState(string stateID)
        {
            throw new Exception("ClearState was called but the engine does not implement IManageState");
        }

        #endregion
    }
}