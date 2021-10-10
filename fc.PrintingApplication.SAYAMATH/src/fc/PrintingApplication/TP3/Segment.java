package fc.PrintingApplication.TP3;

import fc.Math.Vec2f;
import fc.Math.Vec3f;
/*
 * Classe représentant un segment
 */
public class Segment {
	
	public Vec2f v1,v2;
	public boolean traversed = false;	
	
	public Segment() {
	}

	/*
	 * Constructeur
	 */
	
	public Segment(Vec3f v1, Vec3f v2) {
		this.v1 = new Vec2f(v1.x,v1.y);
		this.v2 = new Vec2f(v2.x,v2.y);;
	}
	
	
	@Override
	public String toString() {
		return "Segment [" + v1.toString() + "|" + v2.toString() + "]";
	}
		
	
	
}
