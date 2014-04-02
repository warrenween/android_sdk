Chartbeat Android SDK
---------------------

The Android SDK for Chartbeat is designed to be a simple, effecient, method
of accessing the chartbeat SDK from an Android app. The design requirements
include:

* Minimal system impact
* Simple installation and setup
* Broad compatibility
* Adhere to the same overall design as the iOS SDK so that cross-platform
installation is easier to understand.

In addition, to the extent it does not contradict the above principles,
the SDK is designed with Android design principls and flow in mind. The
Android developer should feel at home with this SDK.

How to Use the SDK
------------------

**Requirements:** To use the SDK, you must have a Chartbeat AccountID, and
your application must have the android.permission.INTERNET and
android.permission.ACCESS_NETWORK_STATE permissions. The SDK requires API
Level 8 (Android 2.2).

**Overview:**
Once you include the jar in your project in the usual manner, all functionality
is available through the com.chartbeat.androidsdk.Tracker class. You access this
class's static functions to start and stop the tracker, update it with information
about application state and so on. You can also set a good deal of optional
information such as the author and so on.

**Tracker Class:** The tracker class (com.chartbeat.androidsdk.Tracker) is
the only class you will need to access to use the SDK. All of the methods you
need to access are public and static.

**Basic Usage:**
* Start by initializing the tracker with one of the startTrackerWithaccountId() functions.
You only need to do this once. However, if you have multiple entry points into your app,
you may call this from any of them and all but the first call will be ignored.
* Next, if you have information about how the user was referred to your app,
you can call setAppReferrer() with a string indicating how the user was referred. Be sure
to do this after calling startTrackerWithaccountId() but before calling trackView() for
the first time.
* Whenever a user enters an activity, you must call trackView(), typically from your
activity's onResume() function. Without calling this function, tracker will never
send any information to chartbeat. If your app has multiple activities, be sure to
do this in each one.
* If you wish to have the SDK properly handle situations where the app leaves
the foreground, you should also call userLeftView(), which can be done from your
activity's onPause() function. If your app has multiple activities, be sure to
do this in each one.
* You will usually want to call userInteracted() from your activity's
onUserInteraction() function,
as well, and any time the user types, you will want to call userTyped(). If your app
has multiple activities, be sure to do this in each one.

There are also a variety of other methods for setting optional information, such as
author and section; however the above functions are required. For a complete list, as
well as detailed usage information about the above functions,
see the Javadocs for com.chartbeat.androidsdk.Tracker.


**GeoLocation:** If you wish to have the API automatically use
GEOLocation information, simply include the android.permission.ACCESS_COARSE_LOCATION
and/or the android.permission.ACCESS_FINE_LOCATION permissions.
With these available, chartbeat will automatically access cached location
data. Because cached data is used, the library will never turn the GPU hardware
on or access this hardware directly. If you require fine-grained or up-to-date
GEOLocation information, simply turn the GPS on yourself and turn it off when you have
zeroed in on the current location. The Location information is updated whenever
trackView() is called.