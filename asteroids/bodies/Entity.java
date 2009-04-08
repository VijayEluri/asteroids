/**
 * Small satellite that shoots ships.
 */

package asteroids.bodies;

import java.awt.Color;

import java.util.*;

import asteroids.*;
import asteroids.ai.*;
import asteroids.display.*;
import asteroids.handlers.*;
import asteroids.handlers.Timer;
import asteroids.weapons.*;

import net.phys2d.math.*;

import net.phys2d.raw.*;

import static asteroids.Util.*;

public abstract class Entity extends TexturedPolyBody implements Targetable, Automated, Drawable, Enhancable {
	protected static int CLOAK_DELAY = 75, CLOAK_MAX = 15000;
	protected long cloaktime = CLOAK_MAX, t = Timer.gameTime();
	protected WeaponSys missiles;
	protected WeaponSys weapons;
	protected World world;
	protected Shield shield, oldshield;
	protected Color color = Color.ORANGE;
	protected Explosion explosion;
	protected int deaths, numMissiles;
	protected float damage, torque, accel;
	protected boolean fire, launch, destruct;
	protected int cloak = Integer.MAX_VALUE;
	protected AI ai;
	protected int textStatus = Integer.MAX_VALUE; // for blinking only
	protected long warningStart; // end of warning -> not invincible
	protected long invincibleEnd; // end of invincibility -> warning(warntime)
	protected boolean raiseShield = true;

	public Entity(ROVector2f[] raw, String img, float nativesize, float size, float mass, World world, Weapon weapon) {
		super(raw, img, nativesize, size, mass);
		this.world = world;
		setRotDamping(mass*mass*mass/843750);
		weapons = new WeaponSys(this, world, weapon);
		missiles = new WeaponSys(this, world, new Missile(world));
		ai = new ShipAI(world, this);
		reset();
	}

	public void setAI(AI ai) {
		this.ai = ai;
		if (ai != null)
			ai.reset();
	}

	public void reset() {
		if (ai != null)
			ai.reset();
		BodyList excluded = getExcludedList();
		while (excluded.size() > 0)
			removeExcludedBody(excluded.get(0));
		setRotation(0);
		setPosition(0,0);
		adjustVelocity(MathUtil.sub(v(0,0),getVelocity()));
		adjustAngularVelocity(-getAngularVelocity());
		torque = damage = accel = 0;
		fire = launch = destruct = false;
		explosion = null;
		numMissiles = 0;
		cloak = Integer.MAX_VALUE;
		warningStart = invincibleEnd = 0;
		raiseShield = true;
		cloaktime = CLOAK_MAX;
	}

	public float getSpeedLimit() {
		return 50;
	}

	public long cloakTime() {
		return cloaktime;
	}

	public void cloak() {
		if (cloaktime > 1000)
			cloak = CLOAK_DELAY;
	}

	public void uncloak() {
		cloak = Integer.MAX_VALUE;
	}

	public void startFiring() {
		fire = true;
	}

	public void stopFiring() {
		fire = false;
	}

	public void startLaunching() {
		launch = true;
	}

	public void stopLaunching() {
		launch = false;
	}

	public boolean launchMissile() {
		if (numMissiles > 0) {
			if (missiles.fire()) {
				numMissiles--;
				return true;
			}
		}
		return false;
	}

	public int numDeaths() {
		return deaths;
	}

	public int numMissiles() {
		return numMissiles;
	}

	public void selfDestruct() {
		destruct = true;
		damage = getMaxArmor() + 1;
	}

	public boolean dead() {
		return destruct || canExplode() && explosion != null && explosion.dead();
	}

	protected void accel() {
		Vector2f dir = direction(getRotation());
		addForce(v(accel*getMass()*dir.getX(),accel*getMass()*dir.getY()));
	}

	public void setAccel(float accel) {
		this.accel =  accel;
	}

	public boolean fire() {
		return weapons.fire();
	}

	public float getWeaponSpeed() {
		return weapons.getWeaponSpeed();
	}

	public double health() {
		return Math.max(0, (getMaxArmor() - damage) / getMaxArmor());
	}

	public double shieldInfo() {
		return shield == null ? -1 : shield.health();
	}

	protected float getMaxArmor() {
		return 3;
	}

	protected void torque() {
		// setTorque() is unpredictable with varied dt
		adjustAngularVelocity(getMass()*torque);
	}

	public void modifyTorque(float t) {
		torque = t;
	}

	public void raiseShields() {
		raiseShield = true;
	}

	protected Shield getShield() {
		return new Shield(this);
	}

	protected void updateShield() {
		if (canTarget()) {
			if (shield == null && oldshield != null) {
				shield = oldshield;
				world.add(shield);
			}
		} else {
			if (shield != null) {
				world.remove(shield);
				oldshield = shield;
				shield = null;
			}
		}
		if (raiseShield) {
			if (shield != null)
				world.remove(shield);
			world.add(shield = getShield());
			raiseShield = false;
		}
		if (shield != null) {
			if (canExplode() || shield.canExplode()) {
				world.remove(shield);
				shield = null;
			} else {
				shield.setPosition(getPosition().getX(), getPosition().getY());
			}
		}
	}

	public void endFrame() {
		super.endFrame();
		updateShield();
		float v = getVelocity().length();
		float limit = getSpeedLimit();
		setDamping(v < limit ? 0 : v < limit*2 ? .1f : .5f);
		if (ai != null)
			ai.update();
		accel();
		torque();
		cloak--;
		if (fire)
			if (fire() && !canTarget())
				cloak = Integer.MAX_VALUE;
		if (launch)
			if (launchMissile() && !canTarget())
				cloak = Integer.MAX_VALUE;
		if (destruct)
			world.remove(this);
		long dt = Timer.gameTime() - t;
		t = Timer.gameTime();
		if (!canTarget())
			cloaktime -= dt;
		else
			cloaktime += dt / 3;
		if (cloaktime < 0) {
			uncloak();
			cloaktime = 0;
		} else if (cloaktime > CLOAK_MAX)
			cloaktime = CLOAK_MAX;
	}

	public void addStatsListener(Stats s) {
		weapons.addStatsListener(s);	
		missiles.addStatsListener(s);
	}

	public Body getRemnant() {
		deaths++;
		return explosion = new LargeExplosion(Explosion.TrackingMode.NONE, 1.5f);
	}

	public boolean canTarget() {
		return cloak > 0 || cloaktime == 0;
	}

	public int getPointValue() {
		return 30;
	}

	public Color getColor() {
		long time = Timer.gameTime();
		if (isInvincible()) {
			if (time < warningStart || textStatus-- % 10 > 5)
				return Color.GREEN;
		}
		if (health() < .2)
			return Color.RED;
		else if (health() < .6)
			return Color.YELLOW;
		return AbstractGame.COLOR;
	}

	public boolean canExplode() {
		return damage > getMaxArmor() && !isInvincible();
	}

	public void collided(CollisionEvent event) {
		if (!isInvincible())
			damage += Exploder.getDamage(event, this);
		updateShield();
	}

	public List<Body> getFragments() {
		double min = Math.sqrt(getMass())/10;
		double max = Math.sqrt(getMass())/4;
		List<Body> f = new ArrayList<Body>(11);
		for (int i=0; i < 11; i++) {
			HexAsteroid tmp = new HexAsteroid(range(min,max));
			tmp.setColor(Color.GRAY);
			f.add(tmp);
		}
		return f;
	}


	public void gainInvincibility(int time, int warn) {
		if (isInvincible())
			return;
		invincibleEnd = Timer.gameTime() + time;
		warningStart = invincibleEnd - warn;
	}

	public void upgradeWeapons() {
		weapons.upgrade();
	}

	public void setHealth(float health) {
		this.damage = getMaxArmor() - getMaxArmor() * health;
	}

	public void addMissiles(int num) {
		numMissiles += num;
	}

	public boolean isInvincible() {
		return invincibleEnd > Timer.gameTime();
	}

	public void setRandomWeaponType() {}
}