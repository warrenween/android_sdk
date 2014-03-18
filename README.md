Chartbeat Android SDK
---------------------

The Android SDK for Chartbeat is designed to be a simple, effecient, method
of accessing the chartbeat SDK from an Android app. The design requirements
include:

- Minimal system impact
- Simple instalation and setup
- Broad comaptibility
- Adhear to the same overall design as the iOS SDK so that cross-platform installation is easier to understand.

In addition, to the extent it does not contradict the above principles,
the SDK is designed with Android design principls and flow in mind. The
Android developer should feel at home with this SDK.

How to Use the SDK
------------------

**Requirements:** To use the SDK, you must have a Chartbeat AccountID, and your application must have the android.permission.INTERNET and android.permission.ACCESS_NETWORK_STATE permissions.

**Overview:**
Once you include the jar in your project in the usual manner, you can use
the Tracker class's static functions to start and stop the tracker, and
update it with information about the application state. For example, once the
application has started, you will need to set the ViewId every time the view
changes. **Important: the tracker will not start until you have called
trackView at least once.** You can also set a good deal of optional information
such as the author and so on.

**Tracker Class:** The tracker class is the only class you will need to access
to use the SDK.

* Start by initializing the tracker with one of the startTrackerWithaccountId() functions.
* Next, if you have information about how the user was referred to your app,
you can call setAppReferrer() with a string indicating how the user was referred.
* Whenever a user enters a view, you must call trackView, typically from your
Activity's onResume() function.
* If you wish to have the SDK properly handle situations where the app goes leaves
the foreground, you should also call userLeftView, which can be done from your
activity's onPause() function.
* You will usually want to call userInteracted() from your onUserInteraction() function,
as well, and any time the user types, you will want to call userTyped()

There are also a variety of other methods for seting optional information, such as
author and section; however the above functions should be considered most important.
