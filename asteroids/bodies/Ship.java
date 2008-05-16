package asteroids.bodies;
import asteroids.display.*;
import static asteroids.Util.*;
import net.phys2d.raw.*;
import net.phys2d.math.*;
import net.phys2d.raw.shapes.*;
import java.awt.Graphics2D;
import java.awt.Color;

public class Ship extends Body implements Drawable, Textured {
	
	private static ROVector2f[] poly = {v(0,-30), v(10,-5), v(32,-5), v(0,15), v(-32, 0), v(-10,-5)};
	private static Shape shape = new Polygon(poly);
	private double hull = 1;
	private int thrust;

	public Ship(float m) {
		super("Your ship", shape, m);
	}

	public float getTextureScaleFactor() {
		return 1.0f;
	}

	public String getTexturePath() {
		return thrust > 0 ? "pixmaps/ship-t.png" : "pixmaps/ship.png";
	}

	public Vector2f getTextureCenter() {
		return v(32,32);
	}

	public void incrThrust() {
		thrust = 5;
	}
	
	public void decrThrust() {
		thrust--;
	}

	public boolean survived(float damage) {
		hull -= Math.abs(damage);
		return hull > 0;
	}

	public double getDamage() {
		return hull;
	}

	public void setDamage(double condition) {
		hull = condition;
	}

	public void drawTo(Graphics2D g2d, float xo, float yo) {
		Polygon poly = (Polygon)getShape();
		g2d.setColor(Color.black);
		ROVector2f[] verts = poly.getVertices(getPosition(), getRotation());
		int[] xcoords = new int[verts.length];
		int[] ycoords = new int[verts.length];
		for (int i=0; i < verts.length; i++) {
			xcoords[i] = (int)(verts[i].getX() - xo);
			ycoords[i] = (int)(verts[i].getY() - yo);
		}
		g2d.fillPolygon(xcoords, ycoords, verts.length);
	}

	public Body fire() {
		Body c = new Sphere1(3);
		float ax = (float)(20*Math.sin(getRotation()));
		float ay = (float)(20*Math.cos(getRotation()));
		c.setPosition(getPosition().getX()+ax, getPosition().getY()-ay);
		c.adjustVelocity(v(20*ax,20*-ay));
		c.addExcludedBody(this);
		return c;
	}

	// the ship is all important
	public float getRadius() {
		return Float.POSITIVE_INFINITY;
	}
}
