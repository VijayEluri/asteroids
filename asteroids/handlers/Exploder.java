/*
 * Asteroids - APCS Final Project
 *
 * This source is provided under the terms of the BSD License.
 *
 * Copyright (c) 2008, Evan Hang, William Ho, Eric Liang, Sean Webster
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * The authors' names may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package asteroids.handlers;
import java.util.*;
import net.phys2d.raw.*;
import net.phys2d.math.*;
import static net.phys2d.math.MathUtil.*;
import asteroids.bodies.*;
import asteroids.weapons.*;
import asteroids.display.*;
import static asteroids.Util.*;

/**
 * Listens for collision events and processes them accordingly.
 */
public class Exploder implements CollisionListener {
	// queue of explosions that will likey be removed soon
	private Queue<Explosion> explosionQueue = new LinkedList<Explosion>();
	// prevent rare double/triple collisionevents from creating multiple explosions
	private Set<Body> exploded = new HashSet<Body>();
	// statistics listeners
	private List<Stats> stats = new LinkedList<Stats>();
	private World world;
	private Display display;
	private CollisionGrouper grouper;
	private final static int MAX_BODIES = 300;
	private final static float MAX_RADIAL_DEVIATION = 10, COLLIDE_BOUNDS = 150, MAX_J = 2;

	/**
	 * phys2d will not collide bodies with any matching bits...
	 * we use this to stop runaway explosions...
	 * there are 63 bits/groups available for use so they wrap around
	 */
	private class CollisionGrouper {
		private long nextmask = 1l;

		/**
		 * @return	Bitmask of next available group.
		 */
		public long findGroup(Body b) {
			if (b.getBitmask() != 0)
				return b.getBitmask();
			nextmask = nextmask << 1;
			if (nextmask == 0)
				nextmask = 1l;
			return nextmask;
		}
	}

	public Exploder(World w, Display d) {
		world = w;
		display = d;
		grouper = new CollisionGrouper();
	}

	/**
	 * Clears the anti-explosion-duplication list.
	 */
	public void endFrame() {
		exploded.clear();
	}

	/**
	 * Add a Stats to the list of Stats.
	 * @param	s	The Stats to add.
	 */
	public void addStatsListener(Stats s) {
		stats.add(s);
	}

	/**
	 * Clears the queue of collisions so the world is not overloaded.
	 * @param	event	The collision event between two bodies.
	 */
	public void collisionOccured(CollisionEvent event) {
		while (!explosionQueue.isEmpty() && explosionQueue.peek().dead())
			world.remove(explosionQueue.remove());
		if (event.getBodyA() instanceof Explodable)
			((Explodable)event.getBodyA()).collided(event);
		if (event.getBodyB() instanceof Explodable)
			((Explodable)event.getBodyB()).collided(event);
		if (event.getBodyA() instanceof Explodable)
			tryExplode(event.getBodyA(), event.getBodyB(), event);
		if (event.getBodyB() instanceof Explodable)
			tryExplode(event.getBodyB(), event.getBodyA(), event);
	}

	/**
	 * Collision algorithm between the two bodies.
	 * @param	body	The body that has collided.
	 * @param	other	The other body that it has collided with.
	 * @param	event	The collision event between body one and two.
	 */
	private void tryExplode(Body body, Body other, CollisionEvent event) {
		if (exploded.contains(body)) // don't explode anything twice
			return;
		exploded.add(body);
		Explodable e = (Explodable)body;
		if (other instanceof Weapon) {
			for (Stats stat : stats)
				stat.hit(body, event);
		}
		// don't explode some offscreen or non-exploding bodies
		if (!isStuck(body, other) && (!e.canExplode()
				|| !display.inView(body.getPosition(), e.getRadius()+COLLIDE_BOUNDS)
				&& !(body instanceof Weapon))) {
			return;
		}
		world.remove(body);
		if (other instanceof Weapon)
			for (Stats stat : stats)
				stat.kill(((Weapon)other).getOrigin(), body, event);
		Body rem = e.getRemnant();
		if (!(rem instanceof Explosion) && world.getBodies().size() > MAX_BODIES)
			return;
		List<Body> f = e.getFragments();
		long group = grouper.findGroup(body);
		if (rem instanceof Explosion) // gah, another special case
			explosionQueue.add((Explosion)rem);
		if (rem != null)
			rem.addBit(group);
		if (!(other instanceof Ship || body instanceof Weapon))
			other.addBit(group);
		// user-related stuff automatically passes the group limit
		if (!(body instanceof Ship || other instanceof Ship
				|| other instanceof Weapon || rem instanceof Explosion)) {
			body.addBit(group);
		}
		// not really J
		float J = other.getMass() / body.getMass() *
				   (body.getRestitution() + other.getRestitution()) / 2;
		J = Math.min(MAX_J, J);
		Vector2f v = sub(body.getVelocity(),(scale(other.getVelocity(),-J)));
		if (f != null) {
			for (Body b : f) {
				b.addBit(group);
				double theta = Math.random()*2*Math.PI;
				float sx = body.getPosition().getX();
				float sy = body.getPosition().getY();
				sx += e.getRadius() * (float)Math.sin(theta) / 2;
				sy -= e.getRadius() * (float)Math.cos(theta) / 2;
				sx += range(-MAX_RADIAL_DEVIATION, MAX_RADIAL_DEVIATION);
				sy -= range(-MAX_RADIAL_DEVIATION, MAX_RADIAL_DEVIATION);
				b.setRotation((float)(2 * Math.PI * Math.random()));
				b.adjustAngularVelocity(range(
				   -body.getAngularVelocity(), body.getAngularVelocity()));
				b.adjustVelocity(scale(direction(theta),
					(float)Math.sqrt(Math.random()*2*v.length())));
				if (!(body instanceof Ship)) // looks bad onscreen
					b.adjustVelocity(v);
				b.setPosition(sx, sy);
				world.add(b);
			}
		}
		if (rem != null) {
			if (rem instanceof Explosion && !(body instanceof Ship))
				rem.setPosition(event.getPoint().getX(),
					event.getPoint().getY());
			else
				rem.setPosition(body.getPosition().getX(),
					body.getPosition().getY());
			rem.adjustVelocity(v);
			rem.setRotation(body.getRotation());
			rem.adjustAngularVelocity(body.getAngularVelocity());
			world.add(rem);
		}
	}

	/**
	 * @return	Damage done to a body.
	 */
	public static double getDamage(CollisionEvent e, Body victim) {
		Body other = e.getBodyA() == victim ? e.getBodyB() : e.getBodyA();
		float multiplier = 1;
		if (victim instanceof Ship && ((Ship)victim).isCloaked())
			multiplier = 2;
		if (other instanceof Weapon)
			return multiplier * ((Weapon)other).getDamage();
		else if (other instanceof PowerUp) // got killed this way before :(
			return 0;
		double vmod = sub(victim.getVelocity(),other.getVelocity()).lengthSquared();
		return Math.min(other.getMass(),victim.getMass()) * vmod / 1e7;
	}

	/**
	 * Checks if the two bodies are stuck.
	 * @param	body	First body.
	 * @param	other	Second body.
	 * @return	True if both bodies are stuck to each other.
	 */
	public boolean isStuck(Body body, Body other) {
		if (!(body instanceof Asteroid && other instanceof Asteroid))
			return false; // too risky to evaluate
		Visible e = (Visible)body;
		float diff = sub(body.getPosition(),other.getPosition()).length();
		if (diff < e.getRadius() / 2)
			return true;
		return false;
	}
}
