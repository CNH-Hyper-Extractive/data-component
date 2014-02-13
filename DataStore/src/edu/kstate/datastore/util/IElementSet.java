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

/**
 * Data exchange between components in OpenMI is always related to one or more
 * elements in a space, either geo-referenced or not. An element set in OpenMI
 * can be anything from a one-dimensional array of points, line segments, poly
 * lines or polygons, through to an array of three-dimensional volumes. As a
 * special case, a cloud of IDBased elements (without co-ordinates) is also
 * supported thus allowing exchange of arbitrary data that is not related to
 * space in any way.
 * <p/>
 * <p/>The IElementSet interface has been defined to describe, in a finite
 * element sense, the space where the values apply, while preserving a course
 * granularity level of the interface.
 * <p/>
 * <p/>Conceptually, IElementSet is composed of an ordered list of elements
 * having a common type. The geometry of each element can be described by an
 * ordered list of vertices. The shape of three-dimensional elements (i.e.
 * volumes or polyhedrons) can be queried by face. If the element set is
 * geo-referenced (i.e. the SpatialReference is not Null), co-ordinates (X,Y,Z)
 * can be obtained for each vertex of an element. The ElementType is an
 * enumeration, listed in Table 1. Data not related to spatial representation
 * can be described by composing an element set containing one (or more) IDBased
 * elements, without any geo-reference.
 * <p/>
 * <p/>Note that IElementSet can be used to query the geometric description of a
 * model schematization, but an implementation does not necessarily provide all
 * topological knowledge on inter-element connections.
 * <p/>
 * <p/>The interface of a spatial reference (ISpatialReference) only contains a
 * String ID. No other properties and methods have been defined, as the OpenGIS
 * SpatialReferenceSystem specification (OGC 2002) provides an excellent
 * standard for this purpose.
 * <p/>
 * <p/>The element set and the element are identified by a String ID. The ID is
 * intended to be useful in terms of an end user. This is particularly useful
 * for configuration as well as for providing specific logging information.
 * However, the properties of an element (its vertices and/or faces) are
 * obtained using an integer index (elementIndex, faceIndex and vertexIndex).
 * This functionality is introduced because an element set is basically an
 * ordered list of elements, an element may have faces and an element (or a
 * face) is an ordered list of vertices. The integer index indicates the
 * location of the element/vertex in the array list.
 * <p/>
 * <p/>While most models encapsulate static element sets, some advanced models
 * might contain dynamic elements (e.g. waves). A version number has been
 * introduced to enable tracking of changes over time. If the version changes,
 * the element set might need to be queried again during the computation
 * process.
 */

public interface IElementSet {

    /**
     * Shape Type of an Elementset.
     */
    public enum ElementType
    {
        /**
         * Identifier based.
         */
        IDBased(0),
        /**
         * Points.
         */
        XYPoint(1),
        /**
         * Lines.
         */
        XYLine(2),
        /**
         * Polylines.
         */
        XYPolyLine(3),
        /**
         * Polygons.
         */
        XYPolygon(4),
        /**
         * 3D Points.
         */
        XYZPoint(5),
        /**
         * 3D Lines.
         */
        XYZLine(6),
        /**
         * 3D Polylines.
         */
        XYZPolyLine(7),
        /**
         * 3D Polygons.
         */
        XYZPolygon(8),
        /**
         * 3D Polyhedrons.
         */
        XYZPolyhydron(9);

        private final int value;


        /**
         * Minimal constructor to use enum with int flag.
         *
         * @param value The integer flag value
         */
        ElementType(int value)
        {
            this.value = value;
        }


        /**
         * int flag value to a given enum base type.
         *
         * @return The int flag value
         */
        public int getValue()
        {
            return value;
        }
    }

    /**
     * Identification String
     * <p/>EXAMPLE: <p/>"River Branch 34", "Node 34"
     */
    String getID();

    /**
     * Additional descriptive information
     */
    String getDescription();

    /**
     * The SpatialReference defines the spatial reference to be used in
     * association with the coordinates in the ElementSet. For all ElementSet
     * Types except ElementType.IDBased a spatial reference must be defined. For
     * ElementSets of type ElementType.IDBased the SpatialReference property may
     * be null.
     * <p/>EXAMPLE: <p/>SpatialReference.ID = "WG84" or "Local coordinate
     * system".
     */
    //ISpatialReference getSpatialReference();

    /**
     * ElementType of the elementset.
     */
    ElementType getElementType();

    /**
     * Number of elements in the ElementSet
     */
    int getElementCount();

    /**
     * The current version number for the populated ElementSet. The version must
     * be incremented if anything inside the ElementSet is changed.
     */
    int getVersion();

    /**
     * Index of element 'ElementID' in the elementset. Indexes start from zero.
     * There are not restrictions to how elements are ordered.
     *
     * @param elementID Identification String for the element for which the
     *                  element index is requested. If no element in the
     *                  ElementSet has the specified elementID, an exception
     *                  must be thrown. .
     */
    int getElementIndex(String elementID);

    /**
     * Returns ID of 'ElementIndex'-th element in the ElementSet. Indexes start
     * from zero.
     *
     * @param elementIndex The element index for which the element ID is
     *                     requested. If the element index is outside the range
     *                     [0, number of elements -1], and exception must be
     *                     thrown. .
     */
    String getElementID(int elementIndex);

    /**
     * Number of vertices for the element specified by the elementIndex.
     * <p/>If the GetVertexCount()method is invoked for ElementSets of type
     * ElementType.IDBased, an exception must be thrown.
     *
     * @param elementIndex <p/>The element index for the element for which the
     *                     number of vertices is requested.
     *                     <p/>
     *                     <p/>If the element index is outside the range [0,
     *                     number of elements -1], and exception must be thrown.
     *                     .
     * @return Number of vertices in element defined by the elementIndex.
     */
    int getVertexCount(int elementIndex);

    /**
     * Returns the number of faces in an element.
     *
     * @param elementIndex <p/>Index for the element
     *                     <p/>
     *                     <p/>If the element index is outside the range [0,
     *                     number of elements -1], and exception must be thrown.
     *                     .
     * @return Number of faces.
     */
    int getFaceCount(int elementIndex);


    /**
     * Gives an array with the vertex indices for a face.
     *
     * @param elementIndex Element index.
     * @param faceIndex    Face index.
     * @return The vertex indices for this face.
     */
    int[] getFaceVertexIndices(int elementIndex, int faceIndex);

    /**
     * X-coord for the vertex with VertexIndex of the element with ElementIndex.
     *
     * @param elementIndex element index.
     * @param vertexIndex  vertex index in the element with index ElementIndex.
     */

    double getXCoordinate(int elementIndex, int vertexIndex);

    /**
     * Y-coord for the vertex with VertexIndex of the element with
     * ElementIndex.
     *
     * @param elementIndex element index.
     * @param vertexIndex  vertex index in the element with index ElementIndex.
     */

    double getYCoordinate(int elementIndex, int vertexIndex);

    /**
     * Z-coord for the vertex with VertexIndex of the element with
     * ElementIndex.
     *
     * @param elementIndex element index.
     * @param vertexIndex  vertex index in the element with index ElementIndex.
     */
    double getZCoordinate(int elementIndex, int vertexIndex);

}
