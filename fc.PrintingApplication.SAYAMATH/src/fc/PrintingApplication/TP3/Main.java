// Copyright (c) 2016,2017 Frederic Claux, Universite de Limoges.
package fc.PrintingApplication.TP3;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.owens.oobjloader.builder.Build;
import com.owens.oobjloader.builder.Face;
import com.owens.oobjloader.builder.FaceVertex;
import com.owens.oobjloader.parser.Parse;

import fc.GLObjects.GLError;
import fc.GLObjects.GLProgram;
import fc.GLObjects.GLRenderTarget;
import fc.GLObjects.GLShaderMatrixParameter;
import fc.Math.AABB;
import fc.Math.Matrix;
import fc.Math.Vec2f;
import fc.Math.Vec2i;
import fc.Math.Vec3f;



public class Main
{

	static float PIXEL_SIZE = 0.05f;									//Taille d'un pixel
	static int H_SIZE = 0;												//Taille horizontale de l'image
	static int V_SIZE = 0;												//Taille verticale de l'image
	static float BUSE = 0.4f;											//Taille de la buse
	static int BACKGROUND_COLOR = Color.BLACK.getRGB();					//Couleur du fond

	//====================================================================

	/*
	 * Fonction testant si un triangle intersecte un plan
	 */

	public static boolean testTriPlaneIntersection(Face face, float height) {


		if( (face.vertices.get(0).v.z < height && face.vertices.get(1).v.z < height && face.vertices.get(2).v.z < height) 
				|| (face.vertices.get(0).v.z > height && face.vertices.get(1).v.z > height && face.vertices.get(2).v.z > height) )

			return false;
		return true;
	}

	//====================================================================

	/*
	 * Fonction testant si un segment intersecte un plan
	 */

	public static boolean testSegPlaneIntersection(Vec3f v1, Vec3f v2, float height) {
		if((v1.z < height && v2.z < height) || (v1.z > height && v2.z > height))
			return false;
		return true;
	}

	/*
	 * Fonction testant si un segment est colinéaire à un plan
	 */

	//====================================================================

	public static boolean testSegColineaire(Vec3f v1, Vec3f v2, float height){
		if(v1.z == height && v2.z==height)
			return true;
		return false;
	}

	/*
	 * Fonction calculant l'intersection entre un plan et un segment
	 */

	//====================================================================

	public static Vec3f edgePlanIntersection(float height, Vec3f v1, Vec3f v2) {

		Vec3f s1=v1;
		Vec3f s2=v2;

		if(v1.z>v2.z) {
			s1 = v1;
			s2 = v2;
		}else {
			s1 = v2;
			s2 = v1;
		}

		Vec3f normale = new Vec3f(0.0f,0.0f,-1.0f);
		Vec3f point = new Vec3f(0.0f,0.0f,height);
		float d = -normale.dot(point);
		Vec3f dir = s2.sub(s1).norm();
		float A = normale.dot(dir);
		float B = normale.dot(s1)+ d;
		float t = -B / A;

		Vec3f p = s1.add(dir.mul(t));
		return p;


	}


	//====================================================================

	/*
	 * Fonction retournant le segment obtenu par l'intersection entre le plan et un triangle
	 */

	public static Segment getSegment(float height, Face face,ArrayList<FaceVertex> vertices) {
		ArrayList<Vec3f> segCoord = new ArrayList<Vec3f>();

		Vec3f v0 = new Vec3f(face.vertices.get(0).v.x,face.vertices.get(0).v.y,face.vertices.get(0).v.z);
		Vec3f v1 = new Vec3f(face.vertices.get(1).v.x,face.vertices.get(1).v.y,face.vertices.get(1).v.z);
		Vec3f v2 = new Vec3f(face.vertices.get(2).v.x,face.vertices.get(2).v.y,face.vertices.get(2).v.z);

		//Si un segment est colinéaire, on le retourne 

		if(testSegColineaire(v0, v1, height)) {
			return new Segment(v0,v1);
		}

		if(testSegColineaire(v0, v2, height)) {
			return new Segment(v0,v2);
		}

		if(testSegColineaire(v1, v2, height)) {
			return new Segment(v1,v2);
		}

		if(testSegPlaneIntersection(v0,v1,height)) {
			segCoord.add(edgePlanIntersection(height, v0, v1));
		}

		if(testSegPlaneIntersection(v0,v2,height)) {
			segCoord.add(edgePlanIntersection(height, v0, v2));
		}

		if(testSegPlaneIntersection(v1,v2,height)) {
			segCoord.add(edgePlanIntersection(height, v1, v2));
		}
		Vec3f s1 =segCoord.get(0) ,s2=segCoord.get(1);

		return new Segment(s1,s2);


	}

	//====================================================================

	/*
	 * Fonction permettant de récupérer la tranche à la hauteur "height"
	 */

	public static ArrayList<Segment> slicing(ArrayList<Face> faces, ArrayList<FaceVertex> vertices ,float height) {
		ArrayList<Segment> segList=new ArrayList<Segment>();

		for(Face face : faces) {
			//Si le triangle intersecte le plan, on calcule le segment représentant l'intersection
			if(testTriPlaneIntersection(face, height))
				segList.add(getSegment(height,face,vertices));
		}
		return segList;
	}

	//====================================================================

	/*
	 * Fonction testant si deux points sont égaux
	 */

	public static boolean areLinked(Vec2f v1, Vec2f v2) {
		if(v1.equals(v2))
			return true;
		return false;
	}


	//====================================================================

	/*
	 * Fonction permettant de lier les segments d'un tranche de sorte à créer un contour
	 */

	public static void  linkSegments(Contour contour, ArrayList<Segment> segmentsList) {
		for(int i =0 ; i<segmentsList.size();i++){
			Segment nextSeg = segmentsList.get(i);
			if(!nextSeg.traversed) {

				//Si un segment partage une extrémité avec les segments aux extrémités de la liste
				//on ajoute le segment à la liste
				if(areLinked(contour.contourVertices.get(0), nextSeg.v1)) {
					nextSeg.traversed = true;
					contour.contourVertices.add(0,nextSeg.v2);
					segmentsList.remove(i);
					linkSegments(contour, segmentsList);
					continue;
				}

				if(areLinked(contour.contourVertices.get(0), nextSeg.v2)) {
					nextSeg.traversed = true;
					contour.contourVertices.add(0,nextSeg.v1);
					segmentsList.remove(i);
					linkSegments( contour, segmentsList);
					continue;
				}

				if(areLinked(contour.contourVertices.get(contour.contourVertices.size()-1), nextSeg.v1)) {
					nextSeg.traversed = true;
					contour.contourVertices.add(nextSeg.v2);
					segmentsList.remove(i);
					linkSegments(contour, segmentsList);
					continue;
				}

				if(areLinked(contour.contourVertices.get(contour.contourVertices.size()-1), nextSeg.v2)) {
					nextSeg.traversed = true;
					contour.contourVertices.add(nextSeg.v1);
					segmentsList.remove(i);
					linkSegments( contour, segmentsList);
					continue;
				}
			}
		}
	}

	//====================================================================

	/*
	 * Fonction récupérant les tranches de l'objet
	 */

	public static ArrayList<ArrayList<Contour>> getSlices(String filename)
	{	

		//Contours de chacune des tranches
		ArrayList<ArrayList<Contour>> contoursOfAllSlice = new ArrayList<ArrayList<Contour>>();

		try
		{
			Build builder = new Build();
			Parse obj = new Parse(builder, new File(filename).toURI().toURL());
			

			// Enumeration des sommets

			ArrayList<FaceVertex> vertices=new ArrayList<FaceVertex>();

			for (FaceVertex vertex : builder.faceVerticeList)
			{


				vertices.add(vertex);
			}

			// Enumeration des faces (souvent des triangles, mais peuvent comporter plus de sommets dans certains cas)

			ArrayList<Face> faces = new ArrayList<Face>(); 


			for (Face face : builder.faces)
			{

				// Parcours des triangles de cette face

				for (int i=1; i <= (face.vertices.size() - 2); i++)
				{
					faces.add(face);
				}
			}
			System.out.println("Starting slicing");

			//Création de la boîte englobante

			AABB box = new AABB();
			for(FaceVertex vertex : vertices) {
				box.enlarge(new Vec3f(vertex.v.x,vertex.v.y,vertex.v.z));	
			}
			box.addMargin(new Vec3f(10,10,0));

			H_SIZE = (int) Math.ceil((box.getMax().x - box.getMin().x) / PIXEL_SIZE);
			V_SIZE = (int) Math.ceil((box.getMax().y - box.getMin().y) / PIXEL_SIZE);



			float z = box.getMin().z+0.2f;

			//Récupération des tranches

			while( z < box.getMax().z) {

				//Récupération des segments provenant des intersections
				ArrayList<Segment> segmentsList = slicing(faces, vertices, z);

				//Liste de contours de la tranche actuelle
				ArrayList<Contour> contoursList = new ArrayList<Contour>();

				int nbContours = 0;

				//Création des contours en reliant les segments
				for(int i =0 ; i<segmentsList.size();i++){
					Segment root = segmentsList.get(i);
					if(!root.traversed) {
						root.traversed = true;
						ArrayList<Vec2f> l= new ArrayList<Vec2f>();
						l.add(root.v1);
						l.add(root.v2);
						segmentsList.remove(i);
						Contour contour = new Contour(l);
						contoursList.add(contour);
						linkSegments(contoursList.get(nbContours),segmentsList);
						nbContours++;
					}
				}

				z+=0.2f;
				contoursOfAllSlice.add(contoursList);

			}
		}
		catch (java.io.FileNotFoundException e)
		{
			System.out.println("FileNotFoundException loading file "+filename+", e=" + e);
			e.printStackTrace();
		}
		catch (java.io.IOException e)
		{
			System.out.println("IOException loading file "+filename+", e=" + e);
			e.printStackTrace();
		}
		System.out.println("Retrieved all slices");
		return contoursOfAllSlice;
	}



	//====================================================================

	/*
	 * Fonction permettant de récupérer le pixel dont la distance à un segment est la plus grande 
	 * Utilisé pour la fonction Divide and Conquer
	 */

	public static int getPointMaxDist(ArrayList<Vec2i> points) {
		Vec2f start = new Vec2f(points.get(0).x,points.get(0).y);
		Vec2f end = new Vec2f(points.get(points.size()-1).x,points.get(points.size()-1).y);
		Vec2f segment = end.sub(start);

		float distance =  -Float.MAX_VALUE;
		int indice=0;
		for(int i =1;i<points.size() -1; i++) {
			Vec2f temp = new Vec2f(points.get(i).x,points.get(i).y);

			//On fait une projection du pixel courant sur le segment pour calculer la distance
			Vec2f a = temp.sub(start);
			float dot = segment.dot(a);
			Vec2f proj = start.add((segment.mul(1.0f/segment.length()).mul(dot)));
			float f = proj.sub(temp).length();

			if(f > distance) {
				distance=  f;
				indice = i;
			}
		}

		if(distance <= 1.5f)
			return -1;
		return indice;

	}

	//====================================================================

	/*
	 * Fonction permettant de smooth le contour à l'aide d'une approche Divide and Conquer
	 */

	public static ArrayList<Vec2i>  divideConquer(ArrayList<Vec2i> points,int middlePoint) {
		ArrayList<Vec2i> updatedPoints = new ArrayList<Vec2i>();

		ArrayList<Vec2i> subList1 = new ArrayList<Vec2i>(points.subList(0, middlePoint));
		ArrayList<Vec2i> subList2 = new ArrayList<Vec2i>(points.subList(middlePoint,points.size()));
		ArrayList<Vec2i> leftList =  new ArrayList<Vec2i>();
		ArrayList<Vec2i> rightList =  new ArrayList<Vec2i>();

		int indice1 =0, indice2 = 0;
		if(subList1.size()>2) {
			indice1 = getPointMaxDist(subList1);
			if(indice1!=-1) 
				leftList = divideConquer(subList1,indice1);

		}

		if(subList2.size()>2) {
			indice2 = getPointMaxDist(subList2);
			if(indice2!=-1) 
				rightList = divideConquer(subList2,indice2);


		}
		updatedPoints.addAll(leftList);
		updatedPoints.add(points.get(middlePoint));
		updatedPoints.addAll(rightList);
		return updatedPoints;

	}

	//====================================================================

	/*
	 * Fonction permettant d'éroder un pixel si nécessaire
	 */

	public static boolean erodeFunction(BufferedImage unfilteredImage,BufferedImage image, int x, int y, int amount) {
		boolean changed = false;
		outerloop:
			for( int j =-amount; j<=amount;j++){
				for(int i=-amount;i<=amount;i++){
					if(x+i<0 || x+i>=image.getWidth() || y+j<0 || y+j>=image.getHeight())
						continue;

					//Si un des pixels voisins du pixel courant est noir, on érode celui-ci
					if(unfilteredImage.getRGB(x+i,y+j) == BACKGROUND_COLOR){	
						image.setRGB(x,y,Color.BLACK.getRGB());
						changed=true;
						break outerloop;
					}

				}
			}

		if(!changed) {
			image.setRGB(x,y, unfilteredImage.getRGB(x,y));
			return false;
		}
		return true;

	}

	//====================================================================


	/*
	 * Fonction permettant d'éroder les contours d'une image
	 * Retourne un BufferedImage de l'image érodée
	 */

	public static BufferedImage erode(BufferedImage imageUnfiltered, int c,int amount) {

		//booleen pour savoir si l'image est vide
		
		boolean notEmpty = false;				
		BufferedImage imageFiltered =null;

		int width = imageUnfiltered.getWidth();
		int height = imageUnfiltered.getHeight();

		imageFiltered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for(int y=0;y<height;y++){
			for(int x=0;x<width;x++){
				if(imageUnfiltered.getRGB(x, y) !=BACKGROUND_COLOR)
					notEmpty=erodeFunction(imageUnfiltered, imageFiltered, x, y, amount);

			}
		}

		//Si l'image est vide, on retourne null
		
		if(!notEmpty)
			return null;

		return imageFiltered;

	}


	//====================================================================

	/*
	 * Fonction permettant de vérifier si un pixel est un pixel de contour
	 */

	public static boolean contourPixel(BufferedImage image, Vec2i v) {

		//Si le pixel courant possède un pixel noir autour de lui (H,G,B,D), il est un pixel contour
		
		if(image.getRGB(v.x, v.y-1) == BACKGROUND_COLOR || image.getRGB(v.x+1, v.y) == BACKGROUND_COLOR
				||image.getRGB(v.x, v.y+1) ==BACKGROUND_COLOR  || image.getRGB(v.x-1, v.y) == BACKGROUND_COLOR
				)
			return true	;			

		return false;
	}

	//====================================================================

	/*
	 * Fonction permettant de savoir si un pixel est contenu dans la liste des contours
	 */

	public static boolean contains(ArrayList<ArrayList<Vec2i>> points, Vec2i v) {
		for(ArrayList<Vec2i> list : points)
			for(Vec2i p : list)
				if(p.equals(v))
					return true;
		return false;
	}

	//====================================================================

	/*
	 * Fonction permettant de savoir si un pixel est contenu dans un contour
	 */

	public static boolean containsInContour(ArrayList<Vec2i> points, Vec2i v) {
		for(Vec2i p : points)
			if(p.equals(v))
				return true;
		return false;
	}

	//====================================================================

	/*
	 * Fonction permettant de récupérer un pixel de départ de contour si il en existe encore
	 */

	public static Vec2i startContour(ArrayList<ArrayList<Vec2i>> points, BufferedImage slice) {
		int width = slice.getWidth();
		int height = slice.getHeight();


		float distance = Float.MAX_VALUE;
		float currentDistance = 0;
		Vec2i start = null;
		for(int y =1 ; y<height-1;y++) {
			for(int x =1 ; x<width-1;x++) {
				if(slice.getRGB(x, y) == BACKGROUND_COLOR)
					continue;

				//Pixel le plus en haut à gauche
				
				currentDistance= x *x +y *y ;
				if(currentDistance < distance) {
					
					//Si le pixel n'a pas encore été parcouru est qu'il est un contour
					
					if(!contains(points,new Vec2i(x,y)) && contourPixel(slice, new Vec2i(x,y))) {
						distance = currentDistance;
						start = new Vec2i(x,y);
					}
				}
			}
		}
		return start;
	}

	//====================================================================


	/*
	 * Fonction permettant de récupérer le prochain pixel du contour courant
	 */

	public static Vec2i getNext(BufferedImage image,Vec2i startTexel, Vec2i v, ArrayList<Vec2i> points){
		
		//On regarde les voisins autour
		
		ArrayList<Vec2i> vNexts = new ArrayList<Vec2i>();
		vNexts.add(new Vec2i(v.x,v.y-1));				
		vNexts.add(new Vec2i(v.x+1,v.y-1));

		vNexts.add(new Vec2i(v.x+1,v.y));				
		vNexts.add(new Vec2i(v.x+1,v.y+1));

		vNexts.add(new Vec2i(v.x,v.y+1));				
		vNexts.add(new Vec2i(v.x-1,v.y+1));

		vNexts.add(new Vec2i(v.x-1,v.y));			
		vNexts.add(new Vec2i(v.x-1,v.y-1));

		for(int i =0 ; i<8;i++) {
			Vec2i n = vNexts.get(i);

			//Si le pixel voisin n'est pas noir
			
			if(image.getRGB(n.x, n.y)!=BACKGROUND_COLOR) {
				
				//Si le pixel est le pixel début, on le retourne
				if(n.equals(startTexel))
					return n;
				
				//Si le pixel voisin n'a pas été parcouru et qu'il est contour, on le retourne
				if(!containsInContour(points, n) && contourPixel(image, n)) 
					return n;
			}
		}	

		Vec2i potential = null;
		int cpt=2;		

		//Si on a fait tout le tour sans trouver de pixel candidat, on refait le chemin inverse
		//pour trouver un pixel candidat
		

		ArrayList<Vec2i> pointsReparcourus = new ArrayList<Vec2i>();
		while(potential==null && points.size()-cpt >=0) {	
			
			//Pixel précédent
			Vec2i rewind = points.get(points.size()-cpt);
			pointsReparcourus.add(rewind);
			
			//On regarde les pixels voisins
			vNexts = new ArrayList<Vec2i>();
			vNexts.add(new Vec2i(rewind.x,rewind.y-1));				
			vNexts.add(new Vec2i(rewind.x+1,v.y-1));

			vNexts.add(new Vec2i(rewind.x+1,rewind.y));				
			vNexts.add(new Vec2i(rewind.x+1,rewind.y+1));

			vNexts.add(new Vec2i(rewind.x,rewind.y+1));				
			vNexts.add(new Vec2i(rewind.x-1,rewind.y+1));

			vNexts.add(new Vec2i(rewind.x-1,rewind.y));			
			vNexts.add(new Vec2i(rewind.x-1,rewind.y-1));

			for(int i =0 ; i<8;i++) {
				Vec2i n = vNexts.get(i);
					
				//Si un pixel est candidat pour être contour, on le retourne
				if(image.getRGB(n.x, n.y)!=BACKGROUND_COLOR) {
					if(n.equals(startTexel)) {
						potential= n;
						break ;
					}

					if(!containsInContour(points, n) && contourPixel(image, n)) {
						potential= n;
						break;
					}
				}
			}	

			cpt+=1;
		}
		
		//On rajoute les points reparcouru dans la liste de points
		
		points.addAll(pointsReparcourus);

		return potential;

	}

	//====================================================================

	/*
	 * Fonction permettant de générer un contour à partir d'une image
	 */

	public static void generateContour(BufferedImage eroded,Graphics2D canvas, Color color){
		ArrayList<ArrayList<Vec2i>> points = new ArrayList<ArrayList<Vec2i>>();

		//Pixel de départ d'un contour
		
		Vec2i startpixel = startContour(points,eroded);

		//Tant qu'il y a des contours
		
		while(startpixel !=null) {
			ArrayList<Vec2i> contour = new ArrayList<Vec2i>();

			contour.add(startpixel);
			
			//On calcule le pixel prochain
			
			Vec2i nextPixel = getNext(eroded,startpixel, startpixel,contour);

			//Tant qu'il existe un pixel suivant ou que l'on est pas revenu sur le pixel de début
			
			while(nextPixel!=null && !nextPixel.equals(startpixel)) {
				contour.add(nextPixel);				
				nextPixel =  getNext(eroded,startpixel, nextPixel,contour);	
			}

			points.add(contour);
			startpixel = startContour(points,eroded);
		}

		//On applique le smoothing sur les points
		
		ArrayList<ArrayList<Vec2i>> updatedPoints = new ArrayList<ArrayList<Vec2i>>();
		for (ArrayList<Vec2i> list : points) {
			ArrayList<Vec2i> up = new ArrayList<Vec2i>();
			int middlePoint = list.size()/2;
			up.add(list.get(0));
			up.addAll(divideConquer(list,middlePoint));
			updatedPoints.add(up);
			up.add(list.get(list.size()-1));

		}

		//On dessine les segments
		
		canvas.setColor(color);
		for(ArrayList<Vec2i> list : updatedPoints) {
			for(int i =0 ; i< list.size()-1;i++) {
				canvas.drawLine(list.get(i).x, list.get(i).y, list.get(i+1).x, list.get(i+1).y);
			}
		}
		canvas.dispose();
	}

	//====================================================================

	/*
	 * Fonction permettant de générer les chemins de la buse à partir d'une image
	 * Utilise la fonction generateContour()
	 */

	public static void generatePaths(String imagePath,int c){
		try {
			System.out.println("Generating path for image  " + c);
			BufferedImage origin = ImageIO.read(new File(imagePath));
			Graphics2D canvas = origin.createGraphics();
			int buseSize = (int)(BUSE/PIXEL_SIZE);

			//On génère le périmètre
			
			BufferedImage eroded =  erode(origin,c, buseSize/2);
			generateContour( eroded,canvas,Color.BLUE);

			//On génére les coques tant que l'image n'est pas vide
			
			eroded = erode(eroded,c, buseSize);
			while(eroded !=null) {
				canvas = origin.createGraphics();
				generateContour(eroded ,canvas,Color.ORANGE);
				eroded = erode(eroded,c, buseSize);
			}

			File outputFile = new File(System.getProperty("user.dir"), "/results/chemin/final/tranche"+c+".png");
			ImageIO.write(origin, "PNG", outputFile);
			System.out.println("Generated path for image  " + c);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//====================================================================

	/*
	 * Fonction permettant d'effectuer le dégradé de couleur de la bitmap
	 */

	public static void coloration(int couche, int maxCouche) {
		System.out.println("Coloring image number "+couche);

		ImageIO.setUseCache(false);
		try {
			
			//Tranche actuelle
			
			BufferedImage slice = ImageIO.read(new File("results/chemin/final//tranche"+couche+".png"));
			BufferedImage beforeSlice = null;

			//Liste des BufferedImages parcourues
			
			ArrayList<BufferedImage> imgs = new ArrayList<>();

			if(couche - 4 >0 && couche  <maxCouche -4) {

				for(int y =0 ; y<V_SIZE ; y++) {
					for(int x = 0;x<H_SIZE ;x++) {
						if(slice.getRGB(x,y)==BACKGROUND_COLOR) 
							continue;		

						int nbrCouche=0;
						int cptI=0;

						int c,inc;

						if(couche >= maxCouche/2)
							inc = +1;
						else
							inc = -1;

						c = couche + inc;

						//On calcule le nombre de couche qui sépare le pixel courant de l'extérieur
						
						while(c>=0 && c<=maxCouche) {
							if(imgs.size()<=cptI) {
								
								//On récupère la tranche précedente ou suivante
								
								beforeSlice = ImageIO.read(new File("results/chemin/final/tranche"+c+".png"));
								imgs.add(beforeSlice) ;
							}else {
								beforeSlice =imgs.get(cptI);
							}

							cptI++;

							//Si le pixel de la tranche precédente ou suivante est noir, on arrête
							
							if(beforeSlice.getRGB(x,y)==BACKGROUND_COLOR) 
								break;
							nbrCouche++;
							c+=inc;
						}

						int colorIncr = (255-64)/(maxCouche/2);
						int red =0;
						if(nbrCouche <=4)
							red =255;
						else
							red= 255 - nbrCouche *colorIncr;

						slice.setRGB(x, y, (red<<16) | (0<<8) | 0);

					}
				}

			}

			File outputFile = new File(System.getProperty("user.dir"), "/results/chemin/final/tranche"+couche+".png");
			ImageIO.write(slice, "png", outputFile);
			System.out.println("Colored image number "+couche);


		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	//====================================================================

	/*
	 * Fonction permettant d'effectuer un remplissage de forme à l'aide de la méthode de Guthe
	 */

	public static void guthe(ArrayList<Contour> listContour ) {

		ArrayList<GLVec2iTriangle> listGLTriangles = new ArrayList<GLVec2iTriangle>();

		//On récupère les triangles à render
		
		for(Contour contour : listContour) {

			Vec2f p1 = contour.contourVertices.get(0);
			for(int i =1; i< contour.contourVertices.size()-1;i++) {	
				Vec2f p2 = contour.contourVertices.get(i);
				Vec2f p3 = contour.contourVertices.get(i+1);

				listGLTriangles.add(new GLVec2iTriangle(
						new Vec2i[]{new Vec2i((int)(p1.x/PIXEL_SIZE+H_SIZE/2), (int)(p1.y/PIXEL_SIZE+V_SIZE/2)),
								new Vec2i((int)(p2.x/PIXEL_SIZE+H_SIZE/2), (int)(p2.y/PIXEL_SIZE+V_SIZE/2)),
								new Vec2i((int)(p3.x/PIXEL_SIZE+H_SIZE/2), (int)(p3.y/PIXEL_SIZE+V_SIZE/2))}));
			}
		}

		//On fait une première passe pour inverser les bits du stencil buffer
		
		GL30.glEnable(GL30.GL_STENCIL_TEST);
		GL30.glStencilOp(GL30.GL_KEEP, GL30.GL_KEEP, GL30.GL_INVERT);
		GL30.glStencilFunc(GL30.GL_ALWAYS, 0, 0xFF);

		GL30.glColorMask(false,false,false,true);
		for(GLVec2iTriangle triangle : listGLTriangles)
			triangle.render();


		//Dans une seconde passe, si les bits sont différents de 0, on dessine le triangle
		
		GL30.glColorMask(true,true,true,true);
		GL30.glStencilFunc(GL30.GL_NOTEQUAL,0, 0xFF);
		for(GLVec2iTriangle triangle : listGLTriangles)
			triangle.render();

		GL30.glDisable(GL30.GL_STENCIL_TEST);

	}

	//====================================================================

	public static void checkGLErrorState()
	{
		int err = GL11.glGetError();
		if (err != 0)
			throw new IllegalStateException("OpenGL is in error state " + err);
	}

	//====================================================================

	/*
	 * Fonction main
	 */

	public static void main(String[] args)
	{
		//Contient les contours de toutes les tranches
		
		ArrayList<ArrayList<Contour>> contoursOfAllSlices = new ArrayList<ArrayList<Contour>>();
		contoursOfAllSlices = getSlices("objects/CuteOcto.obj");

		glfwInit();
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		long window = glfwCreateWindow(100, 100, "Dummy", NULL, NULL);

		glfwMakeContextCurrent(window);
		GL.createCapabilities();

		GLRenderTarget rt = new GLRenderTarget(H_SIZE, V_SIZE, GL30.GL_RGBA32I, GL30.GL_RGBA_INTEGER, GL11.GL_INT);

		GLProgram shader = new GLProgram()
		{
			@Override
			protected void preLinkStep()
			{
				glBindAttribLocation(m_ProgramId, 0, "in_Position");
			}
		};
		shader.init(new MyVShader(), new MyFShader());

		GLShaderMatrixParameter matParam = new GLShaderMatrixParameter("u_mvpMatrix");
		matParam.init(shader);
		rt.bind();

		// Création du render buffer pour le depth/stencil
		int rbo;
		rbo = GL30.glGenRenderbuffers();
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, H_SIZE, V_SIZE);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, rbo);  


		GL11.glViewport(0, 0, H_SIZE, V_SIZE);
		float[][][] pixels;
		int c =0;
		System.out.println("Starting guthe");

		for(ArrayList<Contour> listContour : contoursOfAllSlices) {

			glClearColor(0.0f,0.0f,0.0f,1.0f);
			GLError.check("glClearColor");
			
			GL30.glEnable(GL30.GL_STENCIL_TEST);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
			GLError.check("glClear");


			shader.begin();

			matParam.set(Matrix.createOrtho(0, H_SIZE, 0, V_SIZE, -1, 1));

			//on remplit les contour à l'aide de Guthe pour chaque tranche
			
			guthe(listContour);

			shader.end();
			GL30.glDisable(GL30.GL_STENCIL_TEST);



			pixels = rt.readBackAsFloat();

			BufferedImage img = new BufferedImage(H_SIZE, V_SIZE, BufferedImage.TYPE_INT_RGB);


			for (int y=0; y < pixels.length; y++)
			{
				for (int x=0; x < pixels[0].length; x++)
				{
					int r = (int)(pixels[y][x][0] * 255.0f);
					int g = (int)(pixels[y][x][1] * 255.0f);
					int b = (int)(pixels[y][x][2] * 255.0f);

					img.setRGB(x, y, (r<<16) | (g<<8) | b);
				}
			}



			File outputFile = new File(System.getProperty("user.dir"), "/results/chemin/final/tranche"+c+".png");
			try
			{
				ImageIO.write(img, "png", outputFile);
			}
			catch (IOException e)
			{
				System.out.println("Error, IOException caught: " + e.toString());
			}
			c++;
			


		}
		System.out.println("Guthe finished.");
		System.out.println();
		rt.unbind();
		rt.dispose();



		for(int i =0 ; i<c;i++) {
			coloration(i, c-1);
			System.out.println();

		}
		System.out.println("Coloration finished");
		System.out.println();

		//Création de thread pour séparer les générations de chemins
		
		//nombre de coeurs du PC
		int nbCores = Runtime.getRuntime().availableProcessors()-1;
		Worker[] workers = new Worker[nbCores];
		int i =0 ;
		while(i<c) {
			
			for(int j = 0; j<nbCores;j++) {
				workers[j] = new Worker(i++ , c-1);
			}
			
			for(Worker w : workers) {
				if(w._i < c)
					w.start();
				
			}	
				
			for(Worker w : workers) {
				if(w._i < c)
					try {
						w.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}				
			}	
		}
		System.out.println("Path generation finished.");

	}


	/*
	 * Classe implémentant un thread
	 */
	
	static class Worker extends Thread {
		public int _i, _c;

		public Worker(int i, int c) {
			_i = i;
			_c = c;
		}
		@Override
		public void run() {
			generatePaths("results/chemin/final/tranche"+_i+".png",_i);
			System.out.println();
		}

	}
}
