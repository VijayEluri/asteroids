package asteroids.bodies;
import asteroids.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.*;
import asteroids.display.*;
import asteroids.weapons.*;
import asteroids.handlers.*;
import net.phys2d.math.*;
import net.phys2d.raw.*;
import net.phys2d.raw.shapes.*;
import java.util.List;
import java.util.ArrayList;
import static asteroids.Util.*;

public class Ship extends Body
		implements Drawable, Textured, Explodable, KeyListener {
	
	protected static ROVector2f[] poly = {v(-1,-28), v(3,-24), v(5,-16), v(6,-9), v(5,-3), v(8,-4), v(23,-4), v(23,0), v(9,8), v(5,4), v(6,13), v(-8,13), v(-8,4), v(-10,8), v(-24,1), v(-24,-4), v(-9,-4), v(-5,-2), v(-6,-8), v(-6,-16), v(-4,-25)};
	protected static Shape shape = new Polygon(poly);
	protected static double MAX = 1;
	protected Explosion explosion;
	protected double hull = MAX;
	protected int thrust;
	protected float accel, torque;
	protected boolean fire, explode;
	protected long lastFired;
	protected World world;
	protected int textStatus = Integer.MAX_VALUE; // for blinking only
	protected long warningStart; // end of warning -> not invincible
	protected long invincibleEnd; // end of invincibility -> warning(warntime)
	protected WeaponSys weapons;
	protected MissileSys missiles;
	public int deaths;

	public void reset() {
		BodyList excluded = getExcludedList();
		while (excluded.size() > 0)
			removeExcludedBody(excluded.get(0));
		setRotation(0);
		setPosition(0,0);
		adjustVelocity(MathUtil.sub(v(0,0),getVelocity()));
		adjustAngularVelocity(-getAngularVelocity());
		accel = torque = lastFired = 0;
		fire = explode = false;
		warningStart = invincibleEnd = 0;
		hull = MAX;
		thrust = 0;
		weapons.setRandomWeaponType();
	}

	public static void setMax(double damage) {
		MAX = damage;
	}

	public Ship(World w, Stats s) {
		super("Your ship", shape, 1000f);
		world = w;
		weapons = new WeaponSys(s);
		missiles = new MissileSys(new Missile(),s);
		setRotDamping(4000);
	}

	public void gainInvincibility(int time, int warn) {
		invincibleEnd = Timer.gameTime() + time;
		warningStart = invincibleEnd - warn;
	}

	public void collided(CollisionEvent event) {
		if (!isInvincible())
			hull -= Exploder.getDamage(event, this);
		explode = hull < 0;
	}

	public boolean canExplode() {
		return explode && !isInvincible();
	}
	
	// canExplode but also tracking explosions
	public boolean dead() {
		return canExplode() && explosion != null && explosion.dead();
	}

	public int getTrust() {
		return thrust;
	}

	public float getTextureScaleFactor() {
		return 1.0f;
	}

	public Body getRemnant() {
		// assume 1 death if explode
		deaths++;
		return explosion = new LargeExplosion(1.5f);
	}

	public List<Body> getFragments() {
		List<Body> f = new ArrayList<Body>(11);
		for (int i=0; i < 11; i++) {
			HexAsteroid tmp = new HexAsteroid(range(4,9));
			tmp.setColor(Color.GRAY);
			f.add(tmp);
		}
		return f;
	}

	public Color statusColor() {
		long time = Timer.gameTime();
		if (isInvincible()) {
			if (time < warningStart || textStatus-- % 10 > 5)
				return Color.GREEN;
		}
		if (getDamage() < .2)
			return Color.RED;
		else if (getDamage() < .6)
			return Color.YELLOW;
		return AbstractGame.COLOR;
	}

	public String getTexturePath() {
		return thrust > 0 ? "pixmaps/ship-t.png" : "pixmaps/ship.png";
	}

	public Vector2f getTextureCenter() {
		return v(33,32);
	}

	/**
	 * @return Percent damage from max.
	 */
	public double getDamage() {
		return hull < 0 ? 0 : hull/MAX;
	}

	public boolean isInvincible() {
		return invincibleEnd > Timer.gameTime();
	}

	public void drawTo(Graphics2D g2d, ROVector2f o) {
		Polygon poly = (Polygon)getShape();
		g2d.setColor(Color.black);
		ROVector2f[] verts = poly.getVertices(getPosition(), getRotation());
		int[] xcoords = new int[verts.length];
		int[] ycoords = new int[verts.length];
		for (int i=0; i < verts.length; i++) {
			xcoords[i] = (int)(verts[i].getX() - o.getX());
			ycoords[i] = (int)(verts[i].getY() - o.getY());
		}
		g2d.fillPolygon(xcoords, ycoords, verts.length);
	}

	public float getRadius() {
		return 45;
	}
	
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT: torque = -8e-5f; break;
			case KeyEvent.VK_RIGHT: torque = 8e-5f; break;
			case KeyEvent.VK_UP: accel = 10; break;
			case KeyEvent.VK_DOWN: accel = -5; break;
			case KeyEvent.VK_SPACE: fire = true; break;
		}
		if (e.getKeyChar() == '\'')
			fire = true;
	}

	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT: torque = 0; break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN: accel = 0; break;
			case KeyEvent.VK_SPACE: fire = false; break;
		}
		if (e.getKeyChar() == '\'')
			fire = false;
	}

	public void endFrame() {
		super.endFrame();
		float v = getVelocity().length();
		setDamping(v < 50 ? 0 : v < 100 ? .1f : .5f);
		thrust--;
		accel();
		torque();
		if(fire == true) fire();
		weapons.update(world);
	}

	protected void accel() {
		if (accel > 0)
			thrust = 5;
		Vector2f dir = direction(getRotation());
		addForce(v(accel*getMass()*dir.getX(),accel*getMass()*dir.getY()));
	}

	protected void torque() {
		// setTorque() is unpredictable with varied dt
		adjustAngularVelocity(getMass()*torque);
	}

	protected void fire() {
		weapons.fire(this, world);
	}

	public void keyTyped(KeyEvent e) {
		// don't care
	}
	
	public void setArmor(double num) {
		hull = num;
	}
}
