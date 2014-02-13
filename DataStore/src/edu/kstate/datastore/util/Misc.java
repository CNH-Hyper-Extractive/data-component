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

package edu.kstate.datastore.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.hazelcast.core.IMap;

public class Misc
{
	public static boolean maxDataExceeded(long maxSizeB, IMap map) {
		return (maxSizeB - map.getLocalMapStats().getOwnedEntryMemoryCost() < 0);
	}

	public static String formatDateForXml(Calendar c)
	{
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return formatter.format(c.getTime());
	}

	public static void logInfo(Class<?> c, String s)
	{
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		System.out.println(formatter.format(new Date()) + " " + c.getSimpleName() + " " + s);
	}
	
	public static void logException(Class<?> c, Exception e)
	{
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		System.out.println(formatter.format(new Date()) + " " + c.getSimpleName() + " " + e.getMessage());
		for(StackTraceElement nextLine : e.getStackTrace()) {
			System.out.println(formatter.format(new Date()) + " " + c.getSimpleName() + " " + nextLine.toString());			
		}
	}
	
	public static String formatDateForXml(Date d)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return formatDateForXml(c);
	}

	public static String formatDateForXml(long ms)
	{
		Date d = new Date();
		d.setTime(ms);
		return formatDateForXml(d);
	}

	public static Date parseDateFromXml(String s) throws Exception
	{
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return format.parse(s);
	}

	public static String formatDouble(double d, int precision)
	{
		precision++;
		String result = String.valueOf(d);
		final int point = result.indexOf('.');
		if (point > 0)
		{
			for (int i = 0; i < precision; i++)
				result += "0";
			result = result.substring(0, point + precision);
		}
		return result;
	}

	public static Calendar createCalendar(int year, int month, int day)
	{
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, month);
		c.set(Calendar.DAY_OF_MONTH, day);
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		return c;
	}

	/**
	 * return true if Time A is before Time B (evaluates (Ta < Tb). Both time
	 * spans and time stamps can be tested.
	 * 
	 * @param ta First ITime to compare
	 * @param tb Second ITime to compare
	 * @return True if (Ta < Tb)
	 */
	/*public static boolean isBefore(ITime ta, ITime tb)
	{
		double a;
		double b;

		if (ta instanceof ITimeSpan)
		{
			a = ((ITimeSpan) ta).getEnd().getModifiedJulianDay();
		}
		else
		{
			a = ((ITimeStamp) ta).getModifiedJulianDay();
		}

		if (tb instanceof ITimeSpan)
		{
			b = ((ITimeSpan) tb).getStart().getModifiedJulianDay();
		}
		else
		{
			b = ((ITimeStamp) tb).getModifiedJulianDay();
		}

		return (a < b);
	}*/

	public static String newGuid()
	{
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

	public static String nullIfEmpty(String s)
	{
		if (s == null)
			return null;
		if (s.length() == 0)
			return null;
		return s;
	}

	/*public static double readDateTimeString(String s) throws Exception
	{
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		java.util.Date date = format.parse(s);
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return CalendarConverter.gregorian2ModifiedJulian(c);
	}*/

	/**
	 * Checks if the specified node has the specified name (ignoring case) and
	 * throws an exception when it does not.
	 */
	/*public static void forceNodeName(Node aNode, String aName) throws PersistenceException
	{
		if (!aNode.getNodeName().equalsIgnoreCase(aName))
		{
			throw new PersistenceException(INVALID_XML_STRUCTURE_EXPECTED_TAG_S_BUT_FOUND_TAG_S, aName,
					aNode.getNodeName());
		}
	}*/

	/**
	 * Finds the child node of the specified parent node that has a certain tag
	 * name. If no matching node is found an exception will be thrown.
	 */
	/*public static Node findChildNode(Node aParentNode, String aName) throws PersistenceException
	{
		return findChildNode(aParentNode, aName, true);
	}*/

	/**
	 * Finds the child node of the specified parent node that has a certain tag
	 * name. If no node is found and mustExist is false, null will be returned.
	 * When mustExist is true and the node is not found the method will throw an
	 * exception.
	 */
	/*public static Node findChildNode(Node aParentNode, String aName, boolean mustExist) throws PersistenceException
	{
		NodeList children = aParentNode.getChildNodes();

		for (int i = 0; i < children.getLength(); i++)
		{
			if (children.item(i).getNodeName().equalsIgnoreCase(aName))
			{
				return children.item(i);
			}
		}

		if (!mustExist)
		{
			return null;
		}

		throw new PersistenceException(INVALID_XML_STRUCTURE_REQUIRED_TAG_S_NOT_FOUND, aName);
	}*/

	/**
	 * Finds the child node of the specified parent node that has a certain tag
	 * name and return its (text) value. If no matching node is found an
	 * exception will be thrown.
	 */
	/*public static String findChildNodeValue(Node aParentNode, String aName) throws PersistenceException
	{
		return findChildNodeValue(aParentNode, aName, true);
	}*/

	/**
	 * Finds the child node of the specified parent node that has a certain tag
	 * name and return its (text) value. If no node is found and mustExist is
	 * false, an empty string will be returned. When mustExist is true and the
	 * node is not found the method will throw an exception.
	 */
	/*public static String findChildNodeValue(Node aParentNode, String aName, boolean mustExist)
			throws PersistenceException
	{
		Node n = findChildNode(aParentNode, aName, mustExist);
		return getNodeValue(n, aName);
	}*/

	/*public static String getNodeValue(Node n, String aName) throws PersistenceException
	{
		if (n != null)
		{
			try
			{
				return URLDecoder.decode(n.getFirstChild().getNodeValue(), "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				throw new PersistenceException(INVALID_XML_STRUCTURE_UNSUPPORTED_ENCODING_OF_TAG_S_S_VALUE, aName);
			}
		}
		else
		{
			return "";
		}
	}*/

	public static String getHostName()
	{
		InetAddress addr;
		try
		{
			addr = InetAddress.getLocalHost();
			return addr.getHostName();
		}
		catch (UnknownHostException e)
		{
			return "unknown";
		}
	}

	public static String getHostAddress()
	{
		InetAddress addr;
		try
		{
			addr = InetAddress.getLocalHost();
			return addr.getHostAddress();
		}
		catch (UnknownHostException e)
		{
			return "unknown";
		}
	}

	public static Document readXmlDocument(String filename) throws Exception
	{
		FileInputStream file = new FileInputStream(filename);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse(file);
		file.close();
		return doc;
	}

	public static void writeXmlDocument(Document doc, String filename) throws Exception
	{
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		FileOutputStream outFile = new FileOutputStream(filename);
		StreamResult result = new StreamResult(outFile);
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		outFile.close();
	}
}
