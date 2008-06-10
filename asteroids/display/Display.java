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

package asteroids.display;
import java.awt.*;
import java.util.*;
import java.net.URL;
import net.phys2d.raw.*;
import net.phys2d.math.*;
import static net.phys2d.math.MathUtil.*;
import static asteroids.Util.*;

public abstract class Display {
	protected MediaTracker tracker;
	protected int index = 1;
	protected HashMap<String,Image> cache;
	protected Frame frame;
	protected final Dimension dim;
	protected final int ORIGINAL_WIDTH, ORIGINAL_HEIGHT;
	protected final String dir = getClass().getResource("/asteroids/").toString();

	public Display(Frame f, Dimension d) {
		frame = f;
		dim = d;
		ORIGINAL_WIDTH = (int)dim.getWidth();
		ORIGINAL_HEIGHT = (int)dim.getHeight();
		cache = new HashMap<String,Image>();
		tracker = new MediaTracker(frame);
		frame.setVisible(true);
	}

	/**
	 * Draws all drawable bodies in the world to an offscreen buffer.
	 */
	public void drawWorld(World world) {
		BodyList bodies = world.getBodies();
		for (int i=0; i < bodies.size(); i++)
			if (bodies.get(i) instanceof Textured)
				drawTextured((Textured)bodies.get(i));
			else if (bodies.get(i) instanceof Drawable)
				drawDrawable((Drawable)bodies.get(i));
	}

	/**
	 * @param center The new origin of the display.
	 */
	public abstract void setCenter(ROVector2f center);

	/**
	 * Tells a drawable object to draw itself.
	 * @param thing The drawable object.
	 */
	public abstract void drawDrawable(Drawable thing);

	/**
	 * Draws a Textured object about the center.
	 * @param thing The textured object to be drawn.
	 */
	public abstract void drawTextured(Textured thing);

	/**
	 * Renders the contents of the offscreen buffer to the window and
	 * resets the offscreen buffer to the background.
	 */
	public abstract void show();

	/**
	 * @return Valid Graphics2D for the current frame.
	 */
	public abstract Graphics2D getGraphics();

	/**
	 * @return True if the object is visible from the last set center.
	 * @param test The coordinate of the object.
	 * @param r The radius of the object.
	 */
	public abstract boolean inView(ROVector2f test, float r);

	/**
	 * @param o The origin used for testing viewability.
	 */
	public abstract boolean inViewFrom(ROVector2f o, ROVector2f test, float r);

	/**
	 * Sets the background image, which will be drawn scaled to the offscreen
	 * buffer before anything else.
	 * @param path Path to the image to be set as the background.
	 */
	public abstract	void setBackground(String path);

	public Image loadImage(String path) {
		Image i = cache.get(path);
		if (i == null)
			try {
				i = frame.getToolkit().createImage(new URL(dir+path));
				cache.put(path, i);
				tracker.addImage(i, index++);
				tracker.waitForAll();
			} catch (Exception e) {
				System.err.println(e);
			}
		return i;
	}


	/**
	 * @param o The center of the screen.
	 * @param dim The dimensions of the screen.
	 * @param v The absolute location of the object.
	 * @param r The visible radius of the object.
	 */
	protected static boolean isVisible(ROVector2f o, Dimension dim,
			ROVector2f v, float r) {
		Vector2f rel = MathUtil.sub(v, o);
		return rel.getX() > -r && rel.getX() < dim.getWidth()+r
			&& rel.getY() > -r && rel.getY() < dim.getHeight()+r;
	}

	/**
	 * Get offscreen coords for a shape of radius r.
	 * @param r The radius of the new object.
	 * @param b The maximum distance from the display boundary.
	 * @param o The origin of the area to be considered.
	 */
	public ROVector2f getOffscreenCoords(float r, float b, ROVector2f o) {
		ROVector2f v = o;
		while (true) {
			float x = range(-b-dim.getWidth()/2, b+dim.getWidth()*3/2);
			float y = range(-b-dim.getHeight()/2, b+dim.getHeight()*3/2);
			v = MathUtil.sub(o, v(-x-r, -y-r));
			if (!inView(v,r))
				return v;
		}
	}

	/**
	 * Like getOffscreenCoords, but more random and allows onscreen coords.
	 */
	public ROVector2f getRandomCoords(float b, ROVector2f o) {
		float x = range(-b-dim.getWidth()/2, b+dim.getWidth()*3/2);
		float y = range(-b-dim.getHeight()/2, b+dim.getHeight()*3/2);
		ROVector2f v = scale(sub(o, v(-x,-y)), range(.5,2));
		return v;
	}

	public Dimension getDimension() {
		return dim;
	}

	public int w(int modifier) {
		return (int)(dim.getWidth()+modifier);
	}

	public int h(int modifier) {
		return (int)(dim.getHeight()+modifier);
	}
}
