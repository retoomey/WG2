package org.wdssii.gui;

import org.wdssii.geom.D3;

/** Class to do  some geometric calculation math */
public class GeometricCalc {

	/** Given near and far point and height find point on sphere, useful
	 * for turning mouse into location on earth.
	 * @param nearP
	 * @param farP
	 * @param heightKMS
	 * @return Point on sphere
	 */
	public static D3 pointOnSphere(D3 nearP, D3 farP, double heightKMS) {
		 // Line from nearP to farP
		  final float x1 = (float) nearP.x; 
		  final float y1 = (float) nearP.y; 
		  final float z1 = (float) nearP.z;
		  final float x2 = (float) farP.x; 
		  final float y2 = (float) farP.y; 
		  final float z2 = (float) farP.z;
		  
		  // Sphere with center zero, radius r
		  float x3 = 0.0f; float y3 = 0.0f; float z3 = 0.0f;
		  double r = D3.EARTH_RADIUS_KMS+heightKMS;
		  
		  // Linear algebra. Solving sphere/line intersection.
		  // Either we have zero, one or two intersections
		  //
		  // x1,y1,z1 ==     P1 coordinates (point of line)
		  // x2,y2,z3 ==     P2 coordinates (point of line)
		  // x3,y3,z3, r ==  P3 center of sphere and radius
		  float a, b, c, mu, i;
		  final float xd = (x2-x1);
		  final float yd = (y2-y1);
		  final float zd = (z2-z1);
		  a = (xd*xd)+(yd*yd)+(zd*zd);
		  b = 2*((x2-x1)*(x1-x3) + (y2-y1)*(y1-y3) + (z2-z1)*(z1-z3));
		  c = (float) ((x3*x3) + (y3*y3) +
		      (z3*z3) + (x1*x1) +
		      (y1*y1) + (z1*z1) -
		      2* (x3*x1 + y3*y1 + z3*z1) - (r*r));
		  i = b*b-4*a*c;                     // Determinant

		  D3 p;
		  if (i == 0.0){                     // One intersections
		    mu = -b/(2*a);
		    //p = code::CPoint(x1+mu*(x2-x1), y1+mu*(y2-y1), z1+mu*(z2-z1));
		    p = new D3(x1+mu*(x2-x1), y1+mu*(y2-y1), z1+mu*(z2-z1));
		  }else if (i > 0.0){                // Two intersections

		    // First intersection closest to nearP
		    mu = (float) ((-b - Math.sqrt( (b*b) - 4*a*c)) / (2*a));
		    p = new D3(x1+mu*(x2-x1), y1+mu*(y2-y1), z1+mu*(z2-z1));

		    // Second intersection (other side of sphere)
		    //mu = (-b + sqrt( square(b) - 4*a*c)) / (2*a);
		    //p = new D3(x1+mu*(x2-x1), y1+mu*(y2-y1), z1+mu*(z2-z1));
		  }else {                            // Zero intersections
		    p = new D3(0,0,0); //  Caller should check for this case
		  }

		  return p;

	}
	
}
