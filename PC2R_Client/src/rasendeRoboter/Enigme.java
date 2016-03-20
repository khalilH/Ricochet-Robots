package rasendeRoboter;

import java.awt.Point;

public class Enigme {
	/* robots[0] = rouge */
	/* robots[1] = bleu*/
	/* robots[2] = jaune */
	/* robots[3] = vert */
	private Point[] robots;
	private String cible;
	
	public Enigme(String enigme) {
		robots = new Point[4];
		String tmp = enigme.substring(1, enigme.length()-1);
		String tmpTab[] = tmp.split(",");
		for (int i = 0; i < 4 ; i++) {
			int x = Integer.parseInt(tmpTab[2 * i]);
			int y = Integer.parseInt(tmpTab[2 * i + 1]);
			robots[i] = new Point(x,y);
		}
		cible = tmpTab[8];
	}
	
	public String toString() {
		String ret = "";
		for (int i = 0; i<robots.length; i++) {
			ret += "("+robots[i].getX()+","+robots[i].getY()+")\n";
		}
		ret += cible+"\n";
		return ret;
	}
}