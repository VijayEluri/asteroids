/**
 * "Blue flash" explosion remnant.
 */

package asteroids.weapons;

import asteroids.handlers.*;

import asteroids.ai.*;

import net.phys2d.math.*;

import static asteroids.Util.*;

public class ShieldFailing extends Explosion {
	private static double FRAMETIME = 200;
	private static int FRAMES = 8;
	private double inittime = Timer.gameTime();
	private int frame = 1;
	private float scale = 1;
	private Targetable ship;

	public ShieldFailing(Targetable ship, float radius) {
		super(TrackingMode.NONE);
		this.ship = ship;
		scale = radius / 10;
	}

	public float getRadius() {
		return 30 * scale;
	}

	public Vector2f getTextureCenter() {
		return v(21,20);
	}

	public String getTexturePath() {
		if (dead())
			return null;
		return "pixmaps/exp2/" + frame + ".png";
	}

	public void endFrame() {
		super.endFrame();
		frame = 1 + (int)((Timer.gameTime() - inittime)/FRAMETIME*FRAMES);
		setPosition(ship.getPosition().getX(), ship.getPosition().getY());
	}

	public boolean dead() {
		return frame > FRAMES;
	}

	public float getTextureScaleFactor() {
		return scale;
	}
}
