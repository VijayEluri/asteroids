import java.awt.RenderingHints;
import java.awt.MediaTracker;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Font;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.imageio.*;
import java.util.*;
import java.io.*;
import net.phys2d.raw.shapes.*;
import net.phys2d.math.*;
import net.phys2d.raw.*;

public class Java2DDisplay implements Display {
	private Frame frame;
	private BufferStrategy strategy;
	private Graphics2D buf;
	private int width, height;
	private long resizetime = Long.MAX_VALUE;
	private float xo, yo;
	private double sx = 1, sy = 1;
	private Image orig, bg;
	private MediaTracker tracker;
	private HashMap<String,BufferedImage> cache;

	public Java2DDisplay(Frame f) {
		frame = f;
		width = frame.getWidth();
		height = frame.getHeight();
		frame.setIgnoreRepaint(true);
		frame.setVisible(true);
		frame.createBufferStrategy(2);
		tracker = new MediaTracker(frame);
		cache = new HashMap<String,BufferedImage>();
		strategy = frame.getBufferStrategy();
		buf = (Graphics2D)strategy.getDrawGraphics();
		frame.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				resizetime = System.currentTimeMillis();
				sx = frame.getSize().getWidth() / width;
				sy = frame.getSize().getHeight() / height;
			}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
	}

	public void setCenter(ROVector2f center) {
		xo = center.getX();
		yo = center.getY();
	}

	public Vector2f getCenter() {
		return new Vector2f(xo, yo);
	}

	public void drawWorld(World world) {
		BodyList bodies = world.getBodies();
		for (int i=0; i < bodies.size(); i++)
			if (bodies.get(i) instanceof Textured)
				drawTextured((Textured)bodies.get(i));
			else if (bodies.get(i) instanceof Drawable)
				drawDrawable((Drawable)bodies.get(i));
	}

	public void drawDrawable(Drawable thing) {
		if (isVisible(thing.getPosition(), thing.getRadius()))
			thing.drawTo(buf, xo, yo);
	}

	public void drawTextured(Textured thing) {
		if (!isVisible(thing.getPosition(), thing.getRadius()))
			return;
		BufferedImage i = loadImage(thing.getTexturePath());
		float x = thing.getPosition().getX() - xo;
		float y = thing.getPosition().getY() - yo;
		float scale = thing.getTextureScaleFactor();
		Vector2f c = thing.getTextureCenter();
		// TODO: find some way to cache the rotated images
		AffineTransform trans = AffineTransform.getTranslateInstance
			(x-c.getX()*scale, y-c.getY()*scale);
		trans.concatenate(AffineTransform.getScaleInstance(scale, scale));
		trans.concatenate(AffineTransform.getRotateInstance
			(thing.getRotation(), c.getX(), c.getY()));
		buf.drawImage(i, trans, null);
	}

	public void drawString(String text, Font fon, Color color, ROVector2f v) {
		buf.setFont(fon);
		buf.setColor(color);
		buf.drawString(text, v.getX(), v.getY());
	}

	public void show() {
		buf.dispose();
		strategy.show();
		clearBuffer();
		// don't rescale the background while resizing
		if (System.currentTimeMillis() - resizetime > 100) {
			rescaleBackground();
			resizetime = Long.MAX_VALUE;
		}
		buf.scale(sx,sy);
	}

	public void clearBuffer() {
		buf = (Graphics2D)strategy.getDrawGraphics();
		if (bg == null) {
			buf.setColor(Color.white);
			buf.fillRect(0, 0, (int)(sx*width), (int)(sy*height));
		} else
			buf.drawImage(bg,0,0,frame);
		buf.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public void setBackground(String path) {
		orig = frame.getToolkit().getImage(path);
		tracker.addImage(orig, 0);
		rescaleBackground();
	}

	private void rescaleBackground() {
		bg = orig.getScaledInstance(
			(int)(sx*width),(int)(sy*height),Image.SCALE_FAST);
		tracker.addImage(bg, 0);
		try {
			tracker.waitForID(0);
		} catch (Exception e) {
			System.out.println("E: what?");
		}
	}

	private boolean isVisible(ROVector2f v, float r) {
		float x = v.getX() - xo, y = v.getY() - yo;
		return x > -r && x < width+r && y > -r && y < height+r;
	}

	private BufferedImage loadImage(String path) {
		BufferedImage i = cache.get(path);
		// TODO: test if the caching actually helps or not
		if (i == null)
			try {
				System.out.println("read: " + path);
				i = ImageIO.read(new File(path));
				cache.put(path, i);
			} catch (Exception e) {
				System.out.println("Invalid image path.");
			} 
		return i;
	}
}
