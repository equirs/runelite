package net.runelite.cache.util;

import lombok.Value;

@Value
public class Triangle3D
{
	int xa;
	int xb;
	int xc;
	int ya;
	int yb;
	int yc;
	int za;
	int zb;
	int zc;

	public double computeArea()
	{
		int xab = xb - xa;
		int yab = yb - ya;
		int zab = zb - za;
		int xac = xc - xa;
		int yac = yc - ya;
		int zac = zc - za;

		double sq1 = Math.pow(yab * zac - zab * yac, 2);
		double sq2 = Math.pow(zab * xac - xab * zac, 2);
		double sq3 = Math.pow(xab * yac - yab * xac, 2);

		return 0.5 * Math.sqrt(sq1 + sq2 + sq3);
	}
}
