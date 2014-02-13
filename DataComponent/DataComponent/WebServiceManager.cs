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

using System.Collections.Generic;
using System.Xml;

namespace KState.DataComponent
{
    public class WebServiceManager
    {
        private List<WebServiceEntry> webServiceInfoList;

        public List<WebServiceEntry> WebServiceInfoList
        {
            get { return webServiceInfoList; }
        }

        public void Read(string path)
        {
            webServiceInfoList = new List<WebServiceEntry>();

            var doc = new XmlDocument();
            doc.Load(path);

            var webServicesNode = doc.SelectSingleNode("WebServices");
            foreach (XmlNode nextWebServiceNode in webServicesNode.SelectNodes("WebService"))
            {
                var webServiceInfo = new WebServiceEntry();
                webServiceInfo.Id = nextWebServiceNode.SelectSingleNode("ID").InnerText;
                webServiceInfo.Type = nextWebServiceNode.SelectSingleNode("Type").InnerText;
                webServiceInfo.Description = nextWebServiceNode.SelectSingleNode("Description").InnerText;
                webServiceInfo.Url = nextWebServiceNode.SelectSingleNode("Url").InnerText;
                webServiceInfo.ProcessingTime = nextWebServiceNode.SelectSingleNode("ProcessingTime").InnerText;
                webServiceInfo.MaxRequests = nextWebServiceNode.SelectSingleNode("MaxRequests").InnerText;

                webServiceInfo.Quantities = "";
                foreach (XmlNode nextQuantityNode in nextWebServiceNode.SelectNodes("Quantities/QuantityID"))
                {
                    if (webServiceInfo.Quantities.Length > 0)
                        webServiceInfo.Quantities += ",";
                    webServiceInfo.Quantities += nextQuantityNode.InnerText;
                }

                webServiceInfoList.Add(webServiceInfo);
            }
        }

        public string FindServiceIdForQuantity(string quantityId)
        {
            foreach (var nextEntry in webServiceInfoList)
            {
                if (nextEntry.Quantities.Contains(quantityId) == true)
                {
                    return nextEntry.Id;
                }
            }
            return null;
        }
    }
}