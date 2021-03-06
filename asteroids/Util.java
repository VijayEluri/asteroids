/**
 * Common operation shortcuts to avoid cluttering the code.
 */

package asteroids;

import java.awt.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

import net.phys2d.math.*;

public class Util {
	public static final long BIT_SHIELD_PENETRATING = 1l;
	public static final long BIT_MIN_FREE = 2l;
	private static String id;
	private static long time;

	private Util() {
		// prevent construction
	}

	public static void mark(String id) {
		Util.id = id;
		time = System.nanoTime();
	}

	public static void oops(Throwable e) {
		e.printStackTrace();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		JOptionPane.showMessageDialog(null, sw.toString(), e.toString(), JOptionPane.DEFAULT_OPTION);
		System.exit(1);
	}

	public static File mktemp(String name) {
		File file = new File(System.getProperty("user.home") + "/" + name);
		if (!file.exists()) try {
			file.createNewFile();
		} catch (IOException e) {}
		if (!file.canWrite() || !file.canRead()) {
			file = new File(name);
			try {
				file.createNewFile();
			} catch (IOException e) {}
		}
		if (!file.canWrite() || !file.canRead())
			file = new File(System.getProperty("java.io.tmpdir") + "/" + name);
			if (!file.exists()) try {
				file.createNewFile();
			} catch (IOException e) {}
		return file;
	}

	public static void report() {
		System.out.println(((System.nanoTime() - time) / 1e6) + "ms @ " + id);
	}

	public static Vector2f v(Number x, Number y) {
		return new Vector2f(x.floatValue(), y.floatValue());
	}

	public static Dimension d(Number x, Number y) {
		return new Dimension(x.intValue(), y.intValue());
	}

	public static Vector2f v(Dimension d) {
		return v(d.getWidth(), d.getHeight());
	}

	public static Vector2f v(Point p) {
		return v(p.getX(), p.getY());
	}

	public static Dimension d(Vector2f d) {
		return d(d.getX(), d.getY());
	}

	public static float range(Number minR, Number maxR) {
		float min = minR.floatValue();
		float max = maxR.floatValue();
		return (float)(min+(max-min)*Math.random());
	}

	public static Vector2f negate(ROVector2f r) {
		return v(-r.getX(), -r.getY());
	}

	public static Vector2f direction(Number rotation) {
		return v(Math.sin(rotation.doubleValue()),
		        -Math.cos(rotation.doubleValue()));
	}

	public static boolean oneIn(int num) {
		return num*Math.random() < 1;
	}

	public static Color randomColor() {
		return new Color((int)range(1,255),(int)range(1,255),(int)range(1,255));
	}
}
