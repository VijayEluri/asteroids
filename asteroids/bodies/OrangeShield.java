package asteroids.bodies;

import asteroids.ai.*;

public class OrangeShield extends Shield {
	public OrangeShield(Entity ship) {
		super(ship);
	}

	public String getTexturePath() {
		return damage > getMax() ? "" : "pixmaps/orange-shield.png";
	}

	public float getMax() {
		return 2f;
	}
}