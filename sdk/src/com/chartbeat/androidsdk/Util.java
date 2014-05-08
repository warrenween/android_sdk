package com.chartbeat.androidsdk;

import java.security.SecureRandom;

import android.util.Base64;


/**
 * Static utility functions used elsewhere in the API.
 * @author bjorn
 *
 */
public final class Util {
	private static SecureRandom random = new SecureRandom();
	private Util() {}

	/**
	 * creates a high-entropy string of length numChars according that is
	 * safe to use in the Chartbeat API.
	 */
	public static synchronized String randomChars(int numchars) {
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
