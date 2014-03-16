Chartbeat Android SDK
---------------------

The Android SDK for Chartbeat is designed to be a simple, effecient, method
of accessing the chartbeat SDK from an Android app. The design requirements
include:

- Minimal system impact
- Simple instalation and setup
- Broad comaptibility
- Adhear to the same overall design as the iOS SDK so that cross-platform installation is easier to understand.

In addition, to the extent it does not contradict the above principles, we
the SDK is designed with Android design principls and flow in mind. The
Android developer should feel at home with this SDK.

How to Use the SDK
------------------

**Requirements:** To use the SDK, you must have a Chartbeat AccountID. 

**Overview:**
Once you include the jar in your project in the usual manner, you can use
the CBTracker class's static functions to start and stop the tracker, and
update it information about the application state. For example, once the
application has started, you will need to set the ViewId every time the view
changes. **Important: the tracker will not start until you have called
trackView at least once.** You can also set a good deal of optional information
such as the author and so on.


