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
import java.awt.*;
import net.phys2d.math.*;
import static net.phys2d.math.MathUtil.*;
import asteroids.*;
import asteroids.bodies.*;
import asteroids.display.*;
import static asteroids.Util.*;

/**
 * Draws a small line that points towards another ship.
 */
public class Pointer {
	private Display display;
	private Ship o;
	private Explodable target;
	private int rad;

	public Pointer(Ship t, Explodable x, Display d) {
		o = t;
		target = x;
		display = d;
	}

	/**
	 * Updates position and draws to the screen.
	 */
	public void drawTo(Graphics2D g2d) {
		if (target instanceof Ship)
			g2d.setColor(((Ship)target).statusColor());
		else
			g2d.setColor(AbstractGame.COLOR);
		rad = Math.min(display.w(0),display.h(0))*9/20;
		if (o.canExplode() || target.canExplode())
			return;
		Vector2f origin = sub(o.getPosition(), scale(v(display.getDimension()),.5f));
		if (display.inViewFrom(origin, target.getPosition(), target.getRadius()))
			return;
		Vector2f delta = sub(target.getPosition(), o.getPosition());
		double xo = display.w(0)/2;
		double yo = display.h(0)/2;
		double m = Math.sqrt(delta.length())/2;
		delta.normalise();
		g2d.drawLine((int)(xo - 5  + rad*delta.getX()),
					 (int)(yo - 15 + rad*delta.getY()),
					 (int)(xo - 5  + (rad-m)*delta.getX()),
					 (int)(yo - 15 + (rad-m)*delta.getY()));
	}
}
