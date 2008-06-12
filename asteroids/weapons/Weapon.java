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

package asteroids.weapons;
import java.util.*;
import net.phys2d.raw.*;
import net.phys2d.raw.shapes.*;
import asteroids.bodies.*;
import asteroids.display.*;
import asteroids.handlers.Timer;

/**
 * Weapons are emitted from the ship by a weapon system.
 * Weapons are handled as a special case by Exploder.
 */
public abstract class Weapon extends Body implements Textured, Explodable {
	private boolean exploded;
	private long MAX_LIFETIME = 10000;
	private long startTime = Timer.gameTime();
	protected float lastFire = 0;
	protected boolean canFire = false;
	protected int level = 0;
	protected Ship ship;
	public final static int MAX_LEVEL = 2;

	public Weapon(DynamicShape weap, float mass) {
		super(weap, mass);
	}

	public int getLevel() {
		return level;
	}

	public Ship getOrigin() {
		return ship;
	}

	public void setOrigin(Ship s) {
		ship = s;
	}

	public void setLevel(int l) {
		if (level > MAX_LEVEL || level < 0)
			throw new IllegalArgumentException();
		level = l;
	}

	public void incrementLevel() {
		if (level < 3)
			level++;
	}

	public abstract float getSpeed();
	public abstract float getDamage();
	public abstract float getReloadTime();

	public int getBurstLength() {
		return 0;
	}

	public int getNum() {
		return 1;
	}
	
	public void collided(CollisionEvent event) {}
	
	public boolean canExplode() {
		return true;
	}
	
	public List<Body> explode() {
		List<Body> f = new LinkedList<Body>();
		exploded = true;
		return f;
	}

	public boolean exploded() {
		if (Timer.gameTime() - startTime > MAX_LIFETIME)
			return true;
		return exploded;
	}
}
