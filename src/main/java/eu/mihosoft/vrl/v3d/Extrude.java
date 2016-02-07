/**
 * Extrude.java
 *
 * Copyright 2014-2014 Michael Hoffer info@michaelhoffer.de. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer info@michaelhoffer.de "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer info@michaelhoffer.de OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Michael Hoffer
 * info@michaelhoffer.de.
 */
package eu.mihosoft.vrl.v3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;

// TODO: Auto-generated Javadoc
/**
 * Extrudes concave and convex polygons.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class Extrude {

    /**
     * Instantiates a new extrude.
     */
    private Extrude() {
        throw new AssertionError("Don't instantiate me!", null);
    }

    /**
     * Extrudes the specified path (convex or concave polygon without holes or
     * intersections, specified in CCW) into the specified direction.
     *
     * @param dir direction
     * @param points path (convex or concave polygon without holes or
     * intersections)
     *
     * @return a CSG object that consists of the extruded polygon
     */
    public static CSG points(Vector3d dir, Vector3d... points) {

        return extrude(dir, Polygon.fromPoints(toCCW(Arrays.asList(points))));
    }

    /**
     * Extrudes the specified path (convex or concave polygon without holes or
     * intersections, specified in CCW) into the specified direction.
     *
     * @param dir direction
     * @param points path (convex or concave polygon without holes or
     * intersections)
     *
     * @return a CSG object that consists of the extruded polygon
     */
    public static CSG points(Vector3d dir, List<Vector3d> points) {

        List<Vector3d> newList = new ArrayList<>(points);

        return extrude(dir, Polygon.fromPoints(toCCW(newList)));
    }
    
    /**
     * Extrude.
     *
     * @param dir the dir
     * @param polygon1 the polygon1
     * @return the csg
     */
    private static CSG extrude(Vector3d dir, Polygon polygon1) {
        List<Polygon> newPolygons = new ArrayList<>();
        
        if (dir.z<0 || polygon1.vertices.size()<3) {
            throw new IllegalArgumentException("z < 0 currently not supported for extrude: " + dir);
        }
        
		List<Vertex> points = polygon1.vertices;
		System.out.println("Extrude points: "+points.size()+" points in polygon");
		int n=points.size();
		boolean sign=false;
		// Load up all of the exterior edges
		CSG nonMonotone=null;
		ArrayList<Vector3d> firstPoints = new ArrayList<Vector3d>();
		int lastIndex=0;
		for ( int i = 0; i < (n-2); i++) {
			
			// check for turning polygon
			double dx1 = points.get((i+2)).getX()-points.get((i+1)).getX();
	        double dy1 = points.get((i+2)).getY()-points.get((i+1)).getY();
	        double dx2 = points.get(i).getX()-points.get((i+1)).getX();
	        double dy2 = points.get(i).getY()-points.get((i+1)).getY();
	        double zcrossproduct = dx1*dy2 - dy1*dx2;
	        if (i==0){
	            sign=zcrossproduct>0;
	            firstPoints.add(points.get(i).pos);
	        }
	        else
	        {
	            if (sign!=(zcrossproduct>0)){
	            	// found point where polygon should be broken
	            	sign=zcrossproduct>0;
	            	firstPoints.add(points.get(i).pos);
	            	firstPoints.add(points.get(i+1).pos);

	            	
	            	//Process the rest of the polygon
            		CSG newExtrusion = extrude(dir,Polygon.fromPoints(firstPoints));
            		
            		if(nonMonotone==null)
            			nonMonotone=newExtrusion;
            		else
            			try{
            				nonMonotone=nonMonotone.union(newExtrusion);
            			}catch (Exception ex){
            				System.out.println("Failed sub-polygon "+firstPoints.size());
            				ex.printStackTrace();
            			}
            		i+=2;// skip the loop ahead to the next start
            		lastIndex=i;
            		firstPoints.clear();
            		firstPoints.add(points.get(i).pos);
	            	//reset this iteration to process a monotone polygon
	            }else{
	            	firstPoints.add(points.get(i).pos);
	            }
	        }
		}
		ArrayList<Vector3d> restPoints = new ArrayList<Vector3d>();
    	for(int j=lastIndex;j<n;j++){
    		restPoints.add(points.get(j).pos);
    	}
    	int x=0;
		while(restPoints.size()<3){
			//fill out a full polygon
			restPoints.add(points.get(x++).pos);
		}
		System.out.println("Start-polygon "+restPoints.size());
    	polygon1 = Polygon.fromPoints(restPoints);
		
		CSG extrude;
        try{
    		System.out.println("All points convex in "+polygon1.vertices.size());
            newPolygons.addAll(PolygonUtil.concaveToConvex(polygon1));
            Polygon polygon2 = polygon1.translated(dir);

            int numvertices = polygon1.vertices.size();
            for (int i = 0; i < numvertices; i++) {

                int nexti = (i + 1) % numvertices;

                Vector3d bottomV1 = polygon1.vertices.get(i).pos;
                Vector3d topV1 = polygon2.vertices.get(i).pos;
                Vector3d bottomV2 = polygon1.vertices.get(nexti).pos;
                Vector3d topV2 = polygon2.vertices.get(nexti).pos;

                List<Vector3d> pPoints = Arrays.asList(bottomV2, topV2, topV1, bottomV1);

                newPolygons.add(Polygon.fromPoints(pPoints, polygon1.getStorage()));

            }

            polygon2 = polygon2.flipped();
            List<Polygon> topPolygons = PolygonUtil.concaveToConvex(polygon2);

            newPolygons.addAll(topPolygons);
            extrude =CSG.fromPolygons(newPolygons);
	        if(nonMonotone!=null){
	        	extrude=extrude.union(nonMonotone);
	        }
        }catch(Exception ex){
        	return nonMonotone;
        }
        return extrude;

    }

    /**
     * To ccw.
     *
     * @param points the points
     * @return the list
     */
    public static List<Vector3d> toCCW(List<Vector3d> points) {

        List<Vector3d> result = new ArrayList<>(points);

        if (!isCCW(Polygon.fromPoints(result))) {
            Collections.reverse(result);
        }

        return result;
    }

    /**
     * To cw.
     *
     * @param points the points
     * @return the list
     */
    static List<Vector3d> toCW(List<Vector3d> points) {

        List<Vector3d> result = new ArrayList<>(points);

        if (isCCW(Polygon.fromPoints(result))) {
            Collections.reverse(result);
        }

        return result;
    }

    /**
     * Checks if is ccw.
     *
     * @param polygon the polygon
     * @return true, if is ccw
     */
    public static boolean isCCW(Polygon polygon) {
        // thanks to Sepp Reiter for explaining me the algorithm!
        
        if (polygon.vertices.size() < 3) {
            throw new IllegalArgumentException("Only polygons with at least 3 vertices are supported!");
        }

        // search highest left vertex
        int highestLeftVertexIndex = 0;
        Vertex highestLeftVertex = polygon.vertices.get(0);
        for (int i = 0; i < polygon.vertices.size(); i++) {
            Vertex v = polygon.vertices.get(i);

            if (v.pos.y > highestLeftVertex.pos.y) {
                highestLeftVertex = v;
                highestLeftVertexIndex = i;
            } else if (v.pos.y == highestLeftVertex.pos.y
                    && v.pos.x < highestLeftVertex.pos.x) {
                highestLeftVertex = v;
                highestLeftVertexIndex = i;
            }
        }

        // determine next and previous vertex indices
        int nextVertexIndex = (highestLeftVertexIndex + 1) % polygon.vertices.size();
        int prevVertexIndex = highestLeftVertexIndex - 1;
        if (prevVertexIndex < 0) {
            prevVertexIndex = polygon.vertices.size() - 1;
        }
        Vertex nextVertex = polygon.vertices.get(nextVertexIndex);
        Vertex prevVertex = polygon.vertices.get(prevVertexIndex);

        // edge 1
        double a1 = normalizedX(highestLeftVertex.pos, nextVertex.pos);

        // edge 2
        double a2 = normalizedX(highestLeftVertex.pos, prevVertex.pos);

        // select vertex with lowest x value
        int selectedVIndex;

        if (a2 > a1) {
            selectedVIndex = nextVertexIndex;
        } else {
            selectedVIndex = prevVertexIndex;
        }

        if (selectedVIndex == 0
                && highestLeftVertexIndex == polygon.vertices.size() - 1) {
            selectedVIndex = polygon.vertices.size();
        }

        if (highestLeftVertexIndex == 0
                && selectedVIndex == polygon.vertices.size() - 1) {
            highestLeftVertexIndex = polygon.vertices.size();
        }

        // indicates whether edge points from highestLeftVertexIndex towards
        // the sel index (ccw)
        return selectedVIndex > highestLeftVertexIndex;
    }

    /**
     * Normalized x.
     *
     * @param v1 the v1
     * @param v2 the v2
     * @return the double
     */
    private static double normalizedX(Vector3d v1, Vector3d v2) {
        Vector3d v2MinusV1 = v2.minus(v1);

        return v2MinusV1.dividedBy(v2MinusV1.magnitude()).times(Vector3d.X_ONE).x;
    }

//    public static void main(String[] args) {
//        System.out.println("1 CCW: " + isCCW(Polygon.fromPoints(
//                new Vector3d(-1, -1),
//                new Vector3d(0, -1),
//                new Vector3d(1, 0),
//                new Vector3d(1, 1)
//        )));
//
//        System.out.println("3 CCW: " + isCCW(Polygon.fromPoints(
//                new Vector3d(1, 1),
//                new Vector3d(1, 0),
//                new Vector3d(0, -1),
//                new Vector3d(-1, -1)
//        )));
//
//        System.out.println("2 CCW: " + isCCW(Polygon.fromPoints(
//                new Vector3d(0, -1),
//                new Vector3d(1, 0),
//                new Vector3d(1, 1),
//                new Vector3d(-1, -1)
//        )));
//
//        System.out.println("4 CCW: " + isCCW(Polygon.fromPoints(
//                new Vector3d(-1, -1),
//                new Vector3d(-1, 1),
//                new Vector3d(0, 0)
//        )));
//
//        System.out.println("5 CCW: " + isCCW(Polygon.fromPoints(
//                new Vector3d(0, 0),
//                new Vector3d(0, 1),
//                new Vector3d(0.5, 0.5),
//                new Vector3d(1, 1.1),
//                new Vector3d(1, 0)
//        )));
//    }
}
