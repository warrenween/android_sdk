package com.chartbeat.androidsdk;

import android.util.Base64;

import java.security.SecureRandom;

/**
 * Created by Mike Dai Wang on 2016-02-04.
 */
final class SecurityUtils {
	private static SecureRandom random = new SecureRandom();
	private SecurityUtils() {}

	/**
	 * creates a high-entropy string of length numChars according that is
	 * safe to use in the Chartbeat API.
	 */
	static synchronized String randomChars(int numchars) {
		// make sure the user requested at least one char:
		if( numchars <= 0 )
			throw new RuntimeException("need at least one character");
		// round up to the next multiple of 3.
		int s = numchars;
		if( s % 3 != 0 )
			s = s-s%3 + 3;

		// get random bytes
		byte[] b = new byte[ 4*s/3 ];
		random.nextBytes(b);

		// convert bytes to string:
		String ret = Base64.encodeToString(b, Base64.NO_PADDING | Base64.NO_WRAP );

		//checks:
		if( ret.length() < numchars )
			throw new RuntimeException();
		if( ret.contains("=") )
			throw new RuntimeException();

		// here's where we chartbeat-ify it:
		return ret.replaceAll("\\/", ".").replaceAll("\\+", "_").substring(0,numchars);
	}
}
