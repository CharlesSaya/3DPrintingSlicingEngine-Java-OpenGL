package fc.PrintingApplication.TP3;

import java.util.ArrayList;

import fc.Math.Vec2f;
import fc.Math.Vec3f;

/*
 * Classe représentant un contour 
 */
public class Contour {
	public ArrayList<Vec2f> contourVertices;

	public Contour() {}
	
	/*
	 * Constructeur
	 */
	public Contour(ArrayList<Vec2f> contourList) {
		super();
		this.contourVertices = contourList;
	}
	
	
}
