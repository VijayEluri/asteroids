package asteroids.bodies;

public interface Enhancable {
	public void multiplyHealth(float health);
	public void setHealth(float health);
	public void addMissiles(int num);
	public void gainInvincibility(int time, int warn);
	public void gainBeams(int beams, int max);
	public void upgradeWeapons();
	public void raiseShields();
}
