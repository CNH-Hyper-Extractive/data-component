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
using System.Threading;
using Oatc.OpenMI.Sdk.Backbone;
using Oatc.OpenMI.Sdk.Wrapper;
using OpenMI.Standard;
using TimeSpan = Oatc.OpenMI.Sdk.Backbone.TimeSpan;

namespace KState.SampleComponent
{
    [Serializable]
    internal class Engine : IEngine
    {
        private readonly ILinkableComponent component;
        private double currentTime;
        private List<InputExchangeItem> inputs = new List<InputExchangeItem>();
        private String modelDescription;
        private String modelId;
        private List<OutputExchangeItem> outputs = new List<OutputExchangeItem>();
        private int processingTime;
        private double simulationEndTime;
        private double simulationStartTime;
        private double timeStepLength;
        private TraceFile traceFile;
        private double[] values;
        private String version = "1";

        public Engine(ILinkableComponent component)
        {
            this.component = component;
        }

        public void Initialize(Hashtable properties)
        {
            try
            {
                // read the element set file
                var elementSets = ElementSetReader.read("ElementSet.xml");

                // read the config file
                var filename = (String)properties["ConfigFile"];
                var componentProperties = ComponentProperties.read(filename, component, elementSets);
                var extras = componentProperties.getExtras();
                foreach (var nextKey in extras.Keys)
                    properties[nextKey] = extras[nextKey];

                // save the standard properties
                timeStepLength = componentProperties.getTimeStepInSeconds();
                inputs = componentProperties.getInputExchangeItems();
                outputs = componentProperties.getOutputExchangeItems();
                simulationStartTime = componentProperties.getStartDateTime();
                simulationEndTime = componentProperties.getEndDateTime();
                modelId = componentProperties.GetModelId();
                modelDescription = componentProperties.getModelDescription();

                // save any extra properties
                processingTime = Int32.Parse((String)properties["processingTime"]);

                // setup the log file
                traceFile = new TraceFile(modelId);

                traceFile.Append("Initialize");
                traceFile.Append("Version: " + modelId + " v" + version);
                traceFile.Append("TimeHorizon:" + simulationStartTime + "-" + simulationEndTime);
                traceFile.Append("TimeStep:" + timeStepLength);
                traceFile.Append("ProcessingTime:" + processingTime);
            }
            catch (Exception e)
            {
                traceFile.Exception(e);
            }
        }

        public bool PerformTimeStep()
        {
            traceFile.Append("PerformTimeStep @ " + GetCurrentTime());

            var ct = (TimeStamp)GetCurrentTime();
            currentTime = ct.ModifiedJulianDay + (timeStepLength/86400.0);

            // add a delay
            Thread.Sleep(processingTime*1000);

            return true;
        }

        public void Finish()
        {
            traceFile.Append("Finish");
            traceFile.Stop();
        }

        public IValueSet GetValues(string QuantityID, string ElementSetID)
        {
            // find the element set
            IElementSet elementSet = null;
            foreach (var item in outputs)
            {
                if (item.ElementSet.ID == ElementSetID == true)
                {
                    elementSet = item.ElementSet;
                    break;
                }
            }

            // create a value set of missing values
            if (values == null)
            {
                values = new double[elementSet.ElementCount];
                for (var i = 0; i < values.Length; i++)
                {
                    values[i] = GetMissingValueDefinition();
                }
            }

            var sb = new StringBuilder();
            foreach (var nextValue in values)
            {
                sb.AppendFormat("{0},", nextValue);
            }

            traceFile.Append("GetValues: " + QuantityID + "/" + ElementSetID + "/" + currentTime + " (" + sb + ")");

            return new ScalarSet(values);
        }

        public void SetValues(string QuantityID, string ElementSetID, IValueSet values)
        {
            this.values = ((ScalarSet)values).data;

            var sb = new StringBuilder();
            foreach (var nextValue in this.values)
            {
                sb.AppendFormat("{0},", nextValue);
            }

            traceFile.Append("SetValues: " + QuantityID + "/" + ElementSetID + "/" + currentTime + " (" + sb + ")");
        }

        public string GetComponentID()
        {
            return GetModelID();
        }

        public string GetComponentDescription()
        {
            return GetModelDescription();
        }

        public string GetModelID()
        {
            return modelId;
        }

        public string GetModelDescription()
        {
            return modelDescription;
        }

        public InputExchangeItem GetInputExchangeItem(int exchangeItemIndex)
        {
            return inputs[exchangeItemIndex];
        }

        public OutputExchangeItem GetOutputExchangeItem(int exchangeItemIndex)
        {
            return outputs[exchangeItemIndex];
        }

        public int GetInputExchangeItemCount()
        {
            if (inputs == null)
                return 0;
            return inputs.Count;
        }

        public int GetOutputExchangeItemCount()
        {
            if (outputs == null)
                return 0;
            return outputs.Count;
        }

        public ITimeSpan GetTimeHorizon()
        {
            return new TimeSpan(new TimeStamp(simulationStartTime), new TimeStamp(simulationEndTime));
        }

        public void Dispose()
        {
        }

        public ITime GetCurrentTime()
        {
            if (currentTime == 0)
                currentTime = simulationStartTime;
            return new TimeStamp(currentTime);
        }

        public ITimeStamp GetEarliestNeededTime()
        {
            if (currentTime > 0)
                return new TimeStamp(currentTime);
            return new TimeStamp(simulationStartTime);
        }

        public ITime GetInputTime(string QuantityID, string ElementSetID)
        {
            return new TimeStamp(currentTime);
        }

        public double GetMissingValueDefinition()
        {
            return 999;
        }
    }
}