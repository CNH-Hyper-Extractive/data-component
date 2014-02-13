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
using Oatc.OpenMI.Sdk.Backbone;
using Oatc.OpenMI.Sdk.Spatial;
using Oatc.OpenMI.Sdk.Wrapper;
using OpenMI.Standard;
using ValueType = OpenMI.Standard.ValueType;

namespace KState.DataComponent
{
    [Serializable]
    public class DataOutputLink
    {
        private readonly ILink _link;
        private readonly CacheManager cacheManager;
//        private PrefetchHelper prefetchHelper;
        private Hashtable _bufferStates;
        private LinearConversionDataOperation _linearDataOperation;
        private bool _useSpatialMapping;
        private ElementMapper elementMapper;

        public DataOutputLink(ILink iLink, CacheManager cacheManager) //, PrefetchHelper prefetchHelper)
        {
            _link = iLink;
            this.cacheManager = cacheManager;
//            this.prefetchHelper = prefetchHelper;
        }

        /// <summary>
        ///     The ILink object contained in the SmartLink
        /// </summary>
        public ILink link
        {
            get { return _link; }
        }

        public void Initialize()
        {
            _bufferStates = new Hashtable();

            _useSpatialMapping = false;
            _linearDataOperation = null;

            //Setup Spatial mapper - mapping method is set to default for now!
            var index = -1;
            var description = " ";
            for (var i = 0; i < link.DataOperationsCount; i++)
            {
                for (var n = 0; n < link.GetDataOperation(i).ArgumentCount; n++)
                {
                    if (link.GetDataOperation(i).GetArgument(n).Key == "Type" && link.GetDataOperation(i).GetArgument(n).Value == "SpatialMapping")
                    {
                        for (var m = 0; m < link.GetDataOperation(i).ArgumentCount; m++)
                        {
                            if (link.GetDataOperation(i).GetArgument(m).Key == "Description")
                            {
                                description = link.GetDataOperation(i).GetArgument(m).Value;
                                break;
                            }
                        }
                        index = i;
                        break;
                    }
                }
                if (index == i)
                {
                    break;
                }
            }

            if (index >= 0)
            {
                if (description == " ")
                {
                    throw new Exception("Missing key: \"Description\" in spatial dataoperation arguments");
                }
                _useSpatialMapping = true;
                elementMapper = new ElementMapper();
                elementMapper.Initialise(description, link.SourceElementSet, link.TargetElementSet);
            }

            //Prepare linear data operation
            for (var i = 0; i < link.DataOperationsCount; i++)
            {
                if (link.GetDataOperation(i).ID == (new LinearConversionDataOperation()).ID)
                {
                    _linearDataOperation = (LinearConversionDataOperation)link.GetDataOperation(i);
                    _linearDataOperation.Prepare();
                    break;
                }
            }

            //prepare SmartBufferDataOperation
            /*for (int i = 0; i < link.DataOperationsCount; i++)
            {
                if (link.GetDataOperation(i).ID == (new SmartBufferDataOperation()).ID)
                {
                    ((SmartBufferDataOperation)link.GetDataOperation(i)).Prepare();
                    smartBuffer.DoExtendedDataVerification = ((SmartBufferDataOperation)link.GetDataOperation(i)).DoExtendedValidation;
                    smartBuffer.RelaxationFactor = ((SmartBufferDataOperation)link.GetDataOperation(i)).RelaxationFactor;
                    break;
                }
            }*/
        }

        /// <summary>
        ///     Retrieves a value from the buffer that applies to the time passes as argument.
        ///     During this process the buffer will do temporal operations,
        ///     such as extrapolations, interpolations, or aggregation
        /// </summary>
        /// <param name="time">The time for which the values should apply</param>
        /// <returns>The values</returns>
        public virtual IValueSet GetValue(ITime time, PrefetchManager prefetchHelper)
        {
            // retrieve the values from the cache
            var values = cacheManager.GetValues(time, link.SourceQuantity, link.SourceElementSet, prefetchHelper, link);

            // mark this value as already fetched
            prefetchHelper.addFetchedTime(link, (TimeStamp)time);

            // perform any new prefetching based on this request
            prefetchHelper.Update(link);

            if (_linearDataOperation != null)
            {
                values = _linearDataOperation.PerformDataOperation(values);
            }

            return ConvertUnit(values);
        }


        /// <summary>
        ///     Convert the units according the what is specified in the link
        /// </summary>
        /// <param name="values">The values</param>
        /// <returns>The unit converted values</returns>
        private IValueSet ConvertUnit(IValueSet values)
        {
            var aSource = link.SourceQuantity.Unit.ConversionFactorToSI;
            var bSource = link.SourceQuantity.Unit.OffSetToSI;
            var aTarget = link.TargetQuantity.Unit.ConversionFactorToSI;
            var bTarget = link.TargetQuantity.Unit.OffSetToSI;

            if (aSource != aTarget || bSource != bTarget)
            {
                if (values is IScalarSet)
                {
                    var x = new double[values.Count];

                    for (var i = 0; i < values.Count; i++)
                    {
                        x[i] = (((IScalarSet)values).GetScalar(i)*aSource + bSource - bTarget)/aTarget;
                    }

                    return new ScalarSet(x);
                }
                if (values is IVectorSet)
                {
                    var vectors = new ArrayList();

                    for (var i = 0; i < values.Count; i++)
                    {
                        var x = (((IVectorSet)values).GetVector(i).XComponent*aSource + bSource - bTarget)/aTarget;
                        var y = (((IVectorSet)values).GetVector(i).YComponent*aSource + bSource - bTarget)/aTarget;
                        var z = (((IVectorSet)values).GetVector(i).ZComponent*aSource + bSource - bTarget)/aTarget;

                        var newVector = new Vector(x, y, z);
                        vectors.Add(newVector);
                    }

                    return new VectorSet((Vector[])vectors.ToArray(typeof (Vector)));
                }
                throw new Exception("Type " + values.GetType().FullName + " not suppported for unit conversion");
            }

            return values;
        }

        public string[] GetErrors()
        {
            var link = this.link;
            var messages = new ArrayList();

            // check valuetype
            if (link.SourceQuantity.ValueType != ValueType.Scalar || link.TargetQuantity.ValueType != ValueType.Scalar)
            {
                if (this is DataOutputLink)
                {
                    messages.Add("Component " + link.TargetComponent.ComponentID + "does not support VectorSets");
                }
                else
                {
                    messages.Add("Component " + link.SourceComponent.ComponentID + "does not support VectorSets");
                }
            }

            // check unit
            if (link.SourceQuantity.Unit == null || link.TargetQuantity.Unit == null)
            {
                messages.Add("Unit  equals null in link from " + link.SourceComponent.ModelID + " to " + link.TargetComponent.ModelID);
            }
            else if (link.SourceQuantity.Unit.ConversionFactorToSI == 0.0 || link.TargetQuantity.Unit.ConversionFactorToSI == 0)
            {
                messages.Add("Unit conversion factor equals zero in link from " + link.SourceComponent.ModelID + " to " + link.TargetComponent.ModelID);
            }

            return (string[])messages.ToArray(typeof (string));
        }

        public string[] GetWarnings()
        {
            var link = this.link;
            var messages = new ArrayList();

            // check dimension
            if (!CompareDimensions(link.SourceQuantity.Dimension, link.TargetQuantity.Dimension))
            {
                messages.Add("Different dimensions used in link from " + link.SourceComponent.ModelID + " to " + link.TargetComponent.ModelID);
            }

            return (string[])messages.ToArray(typeof (string));
        }

        private bool CompareDimensions(IDimension dimension1, IDimension dimension2)
        {
            var isSameDimension = true;

            for (var i = 0; i < (int)DimensionBase.NUM_BASE_DIMENSIONS; i++)
            {
                if (dimension1.GetPower((DimensionBase)i) != dimension2.GetPower((DimensionBase)i))
                {
                    isSameDimension = false;
                }
            }
            return isSameDimension;
        }
    }
}