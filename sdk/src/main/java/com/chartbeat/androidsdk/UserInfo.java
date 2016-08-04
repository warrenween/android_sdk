package com.chartbeat.androidsdk;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Observable;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import rx.Observer;
import rx.schedulers.Schedulers;

/**
 * @author bjorn
 * @author Mike Dai Wang
 *
 */
final class UserInfo {
	private static final String TAG = "Chartbeat userdata";

    private static final int USER_ID_CHAR_LENGTH = 18;
    private static final int DAYS_TO_TRACK_VISITS = 16;
    private static final int DAYS_TO_REMEMBER_USER = 30;
    private static final int MINUTES_TO_TRACK_NEW_USER = 30;

    private static final String KEY_USER_ID = "userid";
    private static final String KEY_USER_CREATION_BY_ID = "created-";
    private static final String KEY_USER_LAST_VISIT_TIME_BY_ID = "visits-";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    protected SharedPreferences prefs;
	
	private final String userID;
	private GregorianCalendar userCreated;
	private boolean isNewUser;
	private TreeSet<GregorianCalendar> visitedDates;

	UserInfo(Context context) {
        if (context == null) {
            throw new NullPointerException("Activity or application context cannot be null");
        }

        this.prefs = context.getSharedPreferences(ChartBeatTracker.CHARTBEAT_PREFS, Context.MODE_PRIVATE);

        String storedUserID = prefs.getString(KEY_USER_ID, null);
        String userCreatedString = prefs.getString(KEY_USER_CREATION_BY_ID + storedUserID, null);

        GregorianCalendar creationDate;

        if( storedUserID == null || userCreatedString == null) {
            storedUserID = createUser();
            creationDate = today();
            storeUser(storedUserID, creationDate);
            isNewUser = true;
        } else {
            creationDate = getCreationDate(userCreatedString);

            GregorianCalendar thirtyDaysAgo = pastDate(DAYS_TO_REMEMBER_USER, 0, 0);

            if (creationDate.compareTo(thirtyDaysAgo) >= 0) {
                isNewUser = false;
            } else {
                isNewUser = true;

                if (storedUserID == null) {
                    storedUserID = createUser();
                }

                storeUser(storedUserID, today());

                rx.Observable.timer(MINUTES_TO_TRACK_NEW_USER, TimeUnit.MINUTES)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(new Observer<Long>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onNext(Long aLong) {
                                markUserAsOld();
                            }
                        });
            }
        }

        this.userID = storedUserID;
        this.userCreated = creationDate;

        visitedDates = getVisitDates(this.userID);
	}

    private String createUser() {
        String newUserID = SecurityUtils.randomChars(USER_ID_CHAR_LENGTH);

        return newUserID;
    }

    private void storeUser(String userID, GregorianCalendar userCreated) {
        String usrCreatedString = DATE_FORMAT.format(userCreated.getTime());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_ID, userID);
        editor.putString(KEY_USER_CREATION_BY_ID + userID, usrCreatedString);
        editor.commit();
    }

    private GregorianCalendar getCreationDate(String createdDateString) {
        GregorianCalendar creationDate = new GregorianCalendar();
        try {
            creationDate.setTime(DATE_FORMAT.parse(createdDateString));
        } catch( ParseException pe ) {
            Logger.e(TAG, "Date created has become corrupt: " + createdDateString );
            creationDate = today();
        }

        return creationDate;
    }

    private TreeSet<GregorianCalendar> getVisitDates(String userID) {
        TreeSet visits = new TreeSet<>();
        String storedVisits = prefs.getString(KEY_USER_LAST_VISIT_TIME_BY_ID + userID, null);

        if( storedVisits == null ) {
            return visits;
        }

        GregorianCalendar startDate = today();
        startDate.add(GregorianCalendar.DATE, -DAYS_TO_TRACK_VISITS);

        Logger.d(TAG, "Retrieving user visited dates: " + storedVisits);

        String[] visitStrings = storedVisits.split(",");

        for(String dateString : visitStrings) {
            GregorianCalendar calendar = new GregorianCalendar();
            try {
                calendar.setTime(DATE_FORMAT.parse(dateString));

                if(calendar.after(startDate)) {
                    visits.add(calendar);
                }

            } catch (ParseException e) {
                Logger.e(TAG, "error reading date in user info: " + e );
            }
        }

        return visits;
    }

    private static String encodeVisitDates(Set<GregorianCalendar> dates) {
        boolean isFirstEntry = true;
        String encodedString = "";
        for( GregorianCalendar calendar : dates ) {
            String dateString = DATE_FORMAT.format(calendar.getTime());

            if (isFirstEntry) {
                encodedString = dateString;
                isFirstEntry = false;
            } else {
                encodedString = encodedString + "," + dateString;
            }
        }

        return encodedString;
    }

    boolean isNewUser() {
		return isNewUser;
	}
	
	String getUserID() {
		return userID;
	}
	
	void visited() {
		GregorianCalendar visitDate = today();
		if(visitedDates.add(visitDate)) {
            String encodedDateString = encodeVisitDates(visitedDates);
			Logger.d(TAG, "Storing user visited dates: " + encodedDateString);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(KEY_USER_LAST_VISIT_TIME_BY_ID + userID, encodedDateString);
			editor.commit();
		}
	}
	
	String getUserVisitFrequencyString() {
		GregorianCalendar today = today();
		GregorianCalendar cal[] = new GregorianCalendar[16];
		int validDates = 0;
		for( int i=0; i<16; ++i ) {
			cal[i] = (GregorianCalendar) today.clone();
			cal[i].add(GregorianCalendar.DATE, -i);
			if( cal[i].compareTo(userCreated) >= 0 )
				validDates = i;
		}
		int b =  (visitedDates.contains(cal[15])?1:0) << 3
			| (visitedDates.contains(cal[14])?1:0) << 2
			| (visitedDates.contains(cal[13])?1:0) << 1
			| (visitedDates.contains(cal[12])?1:0) << 0 ;
		int c =  (visitedDates.contains(cal[11])?1:0) << 3
			| (visitedDates.contains(cal[10])?1:0) << 2
			| (visitedDates.contains(cal[ 9])?1:0) << 1
			| (visitedDates.contains(cal[ 8])?1:0) << 0 ;
		int d =  (visitedDates.contains(cal[ 7])?1:0) << 3
			| (visitedDates.contains(cal[ 6])?1:0) << 2
			| (visitedDates.contains(cal[ 5])?1:0) << 1
			| (visitedDates.contains(cal[ 4])?1:0) << 0 ;
		int e =  (visitedDates.contains(cal[ 3])?1:0) << 3
			| (visitedDates.contains(cal[ 2])?1:0) << 2
			| (visitedDates.contains(cal[ 1])?1:0) << 1
			| (visitedDates.contains(cal[ 0])?1:0) << 0 ;
		
		return new String( new char[] { toHexDigit(validDates), toHexDigit(b), toHexDigit(c), toHexDigit(d), toHexDigit(e) } );
	}
	
	private static GregorianCalendar today() {
		GregorianCalendar gc = new GregorianCalendar();
		gc = new GregorianCalendar( gc.get(GregorianCalendar.YEAR), gc.get(GregorianCalendar.MONTH), gc.get(GregorianCalendar.DAY_OF_MONTH) );
		return gc;
	}

    private static GregorianCalendar pastDate(final int daysInPast, final int hoursInPast, final int minutesInPast) {
        return new GregorianCalendar() {{
            add(Calendar.DAY_OF_YEAR, -daysInPast);
            set(Calendar.HOUR_OF_DAY, -hoursInPast);
            set(Calendar.MINUTE, -minutesInPast);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0);
        }};
    }
	
	static final char toHexDigit( int i ) {
		if( i <= 9 && i >= 0 ) {
			return (char) ('0' + i) ;
		} else if( i > 9 && i < 16 ) {
			return (char) ('A' + i - 10 );
		} else {
			throw new RuntimeException( "I is not in hex digit range: " + i );
		}
	}

	void markUserAsOld() {
		isNewUser = false;
	}

    LinkedHashMap<String, String> toPingParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();

        params.put(QueryKeys.USER_ID, getUserID());

        params.put(QueryKeys.IS_NEW_USER, isNewUser() ? "1" : "0");

        params.put(QueryKeys.VISIT_FREQUENCY, getUserVisitFrequencyString());

        return params;
    }
}
