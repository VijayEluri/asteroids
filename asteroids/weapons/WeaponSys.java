/**
 * Handles mechanics of weapons fire from ships.
 */

package asteroids.weapons;

import java.util.*;
import java.util.Collections;

import asteroids.display.*;

import asteroids.handlers.*;
import asteroids.handlers.Timer;

import net.phys2d.math.*;

import net.phys2d.raw.*;

import static asteroids.Util.*;

public class WeaponSys {
	protected static float ANGULAR_DISTRIBUTION = (float)Math.PI/48;
	protected List<Stats> stats = new LinkedList<Stats>();
	protected Body origin;
	protected World world;
	protected Weapon weapon;
	protected long lastFired;
	protected LifeQueue fired = new LifeQueue();
	protected float burst;

	private class LifeQueue {
		Map<Long,Queue<Weapon>> types = new HashMap<Long,Queue<Weapon>>();	
		Set<Weapon> all = new HashSet<Weapon>();

		public void gc() {
			List<Long> remove = new ArrayList<Long>();
			for (Long type : types.keySet()) {
				Queue<Weapon> queue = types.get(type);
				while (!queue.isEmpty() && queue.peek().exploded()) {
					Weapon w = queue.remove();
					all.remove(w);
					world.remove(w);
					origin.removeExcludedBody(w);
				}
				if (queue.size() < 1)
					remove.add(type);
			}
			for (Long type : remove)
				types.remove(type);
		}

		public void add(Weapon w) {
			Long type = w.getLifetime();
			Queue<Weapon> queue = types.get(type);
			if (queue == null) {
				queue = new LinkedList<Weapon>();
				types.put(type, queue);
			}
			queue.add(w);
			all.add(w);
		}

		public Set<Weapon> getActive() {
			return Collections.unmodifiableSet(all);
		}
	}

	public WeaponSys(Body origin, World wo, Weapon w) {
		if (w != null)
			setWeaponType(w);
		else
			setRandomWeaponType();
		this.origin = origin;
		world = wo;
	}

	public void setRandomWeaponType() {
		switch ((int)(3*Math.random())) {
			case 0: setWeaponType(new Laser()); break;
			case 1: setWeaponType(new Laser2()); break;
			case 2: setWeaponType(new Laser3()); break;
		}
	}

	public void incrRandomWeaponLevel() {
		if (weapon instanceof Laser && (oneIn(3) || oneIn(3)))
			weapon.incrementLevel();
		else if (weapon instanceof Laser2 && oneIn(2))
			weapon.incrementLevel();
		if (oneIn(3))
			weapon.incrementLevel();
		if (oneIn(4))
			weapon.incrementLevel();
	}

	public void upgrade() {
		weapon.incrementLevel();
	}

	public void gc() {
		fired.gc();
	}

	public void setWeaponType(Weapon w) {
		weapon = w;
		lastFired = 0;
		burst = 0;
	}

	private boolean canFire() {
		long current = Timer.gameTime();
		long disp = current - lastFired;
		float test = Math.min(disp / weapon.getReloadTime(), weapon.getBurstLength());
		if (test > burst)
			burst = test;
		if (burst > 1 && disp > weapon.getReloadTime() / 4
				|| disp > weapon.getReloadTime()) {
			burst--;
			lastFired = current;
			return true;
		}
		return false;
	}

	public boolean fire() {
		return fire(origin.getRotation());
	}

	public boolean fire(float rotation) {
		if (!canFire())
			return false;
		float initialAngle = (weapon.getNum()-1)*ANGULAR_DISTRIBUTION/2;
		for (int i=0; i < weapon.getNum(); i++) {
			Weapon weap = makeWeapon(i*ANGULAR_DISTRIBUTION - initialAngle, rotation);
			world.add(weap);
			fired.add(weap);
		}
		return true;
	}

	public void addStatsListener(Stats s) {
		stats.add(s);
	}

	// postcondition: nothing is modified
	private Weapon makeWeapon(float angle, float originRotation) {
		Weapon c = weapon.duplicate();
		c.setOrigin(origin);
		c.setRotation(originRotation+angle);
		float xc = (float)Math.sin(originRotation+angle);
		float yc = (float)Math.cos(originRotation+angle);
		float sr = ((Visible)origin).getRadius(); // estimated length
		c.setPosition(origin.getPosition().getX()+xc*sr, origin.getPosition().getY()-yc*sr);
		c.adjustVelocity(v(weapon.getLaunchSpeed()*xc, weapon.getLaunchSpeed()*-yc));
		c.adjustVelocity((Vector2f)origin.getVelocity());
		c.addExcludedBody(origin);
		BodyList el = origin.getExcludedList();
		for (int i=0; i < el.size(); i++)
			c.addExcludedBody(el.get(i));
		for (Weapon f : fired.getActive())
			c.addExcludedBody(f);
		for (Stats stat : stats)
			stat.fired(c);
		return c;
	}

	public float getWeaponSpeed() {
		return weapon.getWeaponSpeed();
	}
}
