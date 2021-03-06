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
import com.piro.bezier.BezierPath;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.svg.*;
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
	private static IExtrusion extrusionEngine = new IExtrusion() {
		/**
		 * Extrudes the specified path (convex or concave polygon without holes
		 * or intersections, specified in CCW) into the specified direction.
		 *
		 * @param dir
		 *            direction
		 * @param points
		 *            path (convex or concave polygon without holes or
		 *            intersections)
		 *
		 * @return a CSG object that consists of the extruded polygon
		 */
		public CSG points(Vector3d dir, List<Vector3d> points) {

			List<Vector3d> newList = new ArrayList<>(points);

			return extrude(dir, Polygon.fromPoints(toCCW(newList)));
		}

		/**
		 * Extrude.
		 *
		 * @param dir
		 *            the dir
		 * @param polygon1
		 *            the polygon1
		 * @return the csg
		 */
		private CSG extrude(Vector3d dir, Polygon polygon1) {

			return monotoneExtrude(dir, polygon1);
		}

		private CSG monotoneExtrude(Vector3d dir, Polygon polygon1) {
			List<Polygon> newPolygons = new ArrayList<>();
			CSG extrude;

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
			extrude = CSG.fromPolygons(newPolygons);

			return extrude;
		}

		@Override
		public CSG extrude(Vector3d dir, List<Vector3d> points) {
			return points(dir, points);
		}
	};

	/**
	 * Instantiates a new extrude.
	 */
	private Extrude() {
		throw new AssertionError("Don't instantiate me!", null);
	}

	public static CSG points(Vector3d dir, List<Vector3d> points) {

		return getExtrusionEngine().extrude(dir, points);
	}

	/**
	 * Extrudes the specified path (convex or concave polygon without holes or
	 * intersections, specified in CCW) into the specified direction.
	 *
	 * @param dir
	 *            direction
	 * @param points
	 *            path (convex or concave polygon without holes or
	 *            intersections)
	 *
	 * @return a CSG object that consists of the extruded polygon
	 */
	public static CSG points(Vector3d dir, Vector3d... points) {

		return points(dir, Arrays.asList(points));
	}

	/**
	 * To ccw.
	 *
	 * @param points
	 *            the points
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
	 * @param points
	 *            the points
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
	 * @param polygon
	 *            the polygon
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
			} else if (v.pos.y == highestLeftVertex.pos.y && v.pos.x < highestLeftVertex.pos.x) {
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

		if (selectedVIndex == 0 && highestLeftVertexIndex == polygon.vertices.size() - 1) {
			selectedVIndex = polygon.vertices.size();
		}

		if (highestLeftVertexIndex == 0 && selectedVIndex == polygon.vertices.size() - 1) {
			highestLeftVertexIndex = polygon.vertices.size();
		}

		// indicates whether edge points from highestLeftVertexIndex towards
		// the sel index (ccw)
		return selectedVIndex > highestLeftVertexIndex;
	}

	/**
	 * Normalized x.
	 *
	 * @param v1
	 *            the v1
	 * @param v2
	 *            the v2
	 * @return the double
	 */
	private static double normalizedX(Vector3d v1, Vector3d v2) {
		Vector3d v2MinusV1 = v2.minus(v1);

		return v2MinusV1.dividedBy(v2MinusV1.magnitude()).times(Vector3d.X_ONE).x;
	}

	public static IExtrusion getExtrusionEngine() {
		return extrusionEngine;
	}

	public static void setExtrusionEngine(IExtrusion extrusionEngine) {
		Extrude.extrusionEngine = extrusionEngine;
	}
	
	

	public static ArrayList<Transform> bezierToTransforms(BezierPath pathA,  BezierPath pathB, int iterations){
		ArrayList<Transform> p = new ArrayList<Transform>();
		double x=0,y=0,z=0;
		double lastx=0,lasty=0,lastz=0;
		
		for (double i = 0.01; i< iterations-1; i ++) {
			Vector3d pointA = pathA.eval((float) i/(iterations-1));
			Vector3d pointB = pathB.eval((float) i/(iterations-1));
			
			x=pointA.x;
			y=pointA.y;
			z=pointB.y;
			Transform t = new Transform();
			t.translateX(x);
			t.translateY(y);
			t.translateZ(z);
	
			double ydiff = y-lasty;
			double zdiff = z-lastz;
			double xdiff = x-lastx;
				
			//t.rotX(45-Math.toDegrees(Math.atan2(zdiff,ydiff)))
	
			double rise = zdiff;
			double run = Math.sqrt((ydiff*ydiff) +(xdiff*xdiff));
			double rotz = 90-Math.toDegrees(Math.atan2(xdiff,ydiff));
			double roty = Math.toDegrees(Math.atan2(rise,run));
	
			t.rotZ(-rotz);
			t.rotY(roty);
			
			//println "z = "+rotz+" y = "+roty
			p.add(t);
			lastx=x;
			lasty=y;
			lastz=z;
		}
		Vector3d pointA = pathA.eval((float) 0.99999);
		Vector3d pointB = pathB.eval((float) 0.99999);
		
		x=pointA.x;
		y=pointA.y;
		z=pointB.y;
		Transform t = new Transform();
		t.translateX(x);
		t.translateY(y);
		t.translateZ(z);

		double ydiff = y-lasty;
		double zdiff = z-lastz;
		double xdiff = x-lastx;

		double rise = zdiff;
		double run = Math.sqrt((ydiff*ydiff) +(xdiff*xdiff));
		double rotz = 90-Math.toDegrees(Math.atan2(xdiff,ydiff));
		double roty = Math.toDegrees(Math.atan2(rise,run));

		t.rotZ(-rotz);
		t.rotY(roty);
		p.add(t);

		return p;
	}
	
	public static 	ArrayList<Transform> bezierToTransforms(ArrayList<Double> controlA, ArrayList<Double> controlB,
				ArrayList<Double> endPoint, int iterations) {
			BezierPath path = new BezierPath();
			path.parsePathString("C " + controlA.get(0) + "," + controlA.get(1) + " " + controlB.get(0) + ","
					+ controlB.get(1) + " " + endPoint.get(0) + "," + endPoint.get(1));
			BezierPath path2 = new BezierPath();
			path2.parsePathString("C " + controlA.get(0) + "," + controlA.get(2) + " " + controlB.get(0) + ","
					+ controlB.get(2) + " " + endPoint.get(0) + "," + endPoint.get(2));
	
			return bezierToTransforms(path, path2, iterations);
		}
	

	
	public static ArrayList<CSG>  revolve(CSG slice,double radius ,int numSlices){
		return revolve(slice,radius,360.0,numSlices);
	}
	
	public static ArrayList<CSG>  revolve(CSG slice,double radius,double archLen ,int numSlices){
		ArrayList<CSG> parts = new ArrayList<CSG> ();
		double increment= archLen/((double)numSlices);
		for(int i=0;i<archLen+increment;i+=increment){
			parts.add(slice
					.movey(radius)
					.rotz(i)
			);
		}
		for(int i=0;i<parts.size()-1;i++){
			CSG sweep = parts.get(i)
						.union(parts.get(i+1))
						.hull();
			parts.set(i,sweep);
		}
		
		return parts;
	}
	
	public static ArrayList<CSG>  bezier(CSG slice, ArrayList<Double> controlA, ArrayList<Double> controlB,ArrayList<Double> endPoint, int numSlices){
		ArrayList<CSG> parts =new 	ArrayList<CSG> ();
		
		for(int i=0;i<numSlices;i++){
			parts.add(0,slice.clone());
		}
		return bezier(parts,controlA,controlB,endPoint);
	}
	
	public static ArrayList<CSG>  bezier(ArrayList<CSG>   s, ArrayList<Double> controlA, ArrayList<Double> controlB,ArrayList<Double> endPoint){
		ArrayList<CSG> slice = moveBezier(s,controlA,controlB,endPoint);
	
		for(int i=0;i<slice.size()-1;i++){
			CSG sweep = slice.get(i)
						.union(slice.get(i+1))
						.hull();
			slice.set(i,sweep);
		}
		
		return slice;
	}
	public static ArrayList<CSG>  linear(ArrayList<CSG>   s,ArrayList<Double> endPoint){
		ArrayList<Double> start = (ArrayList<Double>) Arrays.asList(0.0,0.0,0.0);
		return bezier(s,start,endPoint,endPoint);
	}
	
	public static ArrayList<CSG>  linear(CSG   s,ArrayList<Double> endPoint, int numSlices){
		ArrayList<Double> start = (ArrayList<Double>) Arrays.asList(0.0,0.0,0.0);
		
		return bezier(s,start,endPoint,endPoint,numSlices);
	}
	
	public static ArrayList<CSG>  move(ArrayList<CSG> slice,ArrayList<Transform> p ){
		ArrayList<CSG> s = new ArrayList<CSG> ();
		//s.add(slice.get(0));
		for(int i=0;i<slice.size() && i<p.size();i++){
			s.add(slice.get(i)
					.transformed(p.get(i))
			);
		}
		return s;
	}
	public static ArrayList<CSG>  move(CSG slice,ArrayList<Transform> p ){
		ArrayList<CSG> bits = new ArrayList<CSG> ();
		for(Transform t:p){
			bits.add(slice.clone());
		}
		return move(bits,p);
	}
	
	public static ArrayList<CSG>  moveBezier(CSG slice, ArrayList<Double> controlA, ArrayList<Double> controlB,ArrayList<Double> endPoint, int numSlices){
		ArrayList<Transform> p =bezierToTransforms( controlA, controlB,endPoint,   numSlices);
		
		return move(slice,p);
	}
	
	
	public static ArrayList<CSG>  moveBezier(ArrayList<CSG> slice, ArrayList<Double> controlA, ArrayList<Double> controlB,ArrayList<Double> endPoint){
	
		int numSlices = slice.size();
		ArrayList<Transform> p =bezierToTransforms( controlA, controlB,endPoint,   numSlices);
		return move(slice,p);
		
	}
	
	public static ArrayList<CSG>  moveBezier(CSG slice, BezierPath pathA, int numSlices){
		Vector3d pointA = pathA.eval((float) 1.0);
		String zpath = "C 0,0 "+pointA.x+","+pointA.y+" "+pointA.x+","+pointA.y;
		BezierPath pathB = new BezierPath();
		pathB.parsePathString(zpath);
		
		return moveBezier(slice,pathA,pathB,numSlices);
		
		
	}
	
	public static ArrayList<CSG>  moveBezier(CSG slice, BezierPath pathA,  BezierPath pathB,  int iterations){
	
		ArrayList<Transform> p =bezierToTransforms( pathA,   pathB,  iterations);
		return move(slice,p);
	
	}
}
