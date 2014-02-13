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
using OpenMI.Standard;
using ValueType = OpenMI.Standard.ValueType;

namespace KState.DataComponent
{
    [Serializable]
    public class DataInputLink
    {
        /// <summary>
        ///     Reference to the Link
        /// </summary>
        protected ILink _link;

        public DataInputLink()
        {
        }

        public DataInputLink(ILink iLink)
        {
            _link = iLink;
        }

        /// <summary>
        ///     The ILink object contained in the SmartLink
        /// </summary>
        public ILink link
        {
            get { return _link; }
        }

        public virtual string[] GetErrors()
        {
            var link = this.link;
            var messages = new ArrayList();

            // check valuetype
            if (link.SourceQuantity.ValueType != ValueType.Scalar || link.TargetQuantity.ValueType != ValueType.Scalar)
            {
                if (this is DataInputLink)
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