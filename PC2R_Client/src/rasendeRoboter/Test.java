package rasendeRoboter;


public class Test {

	public static void main(String[] args) {
		Plateau p = new Plateau("(3,4,H)(3,4,B)(12,6,G)");
		System.out.println(p);
		System.out.println();
		Enigme e = new Enigme("(5,5,7,7,9,9,1,1,R)");
		System.out.println(e);
	}

}