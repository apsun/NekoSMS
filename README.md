# NekoSMS

A regular expression based text message blocker for Android.

## Requirements

- A rooted phone running Android 4.4 KitKat or newer
- [Xposed framework](http://forum.xda-developers.com/xposed/xposed-installer-versions-changelog-t2714053)
  ([Lollipop version](http://forum.xda-developers.com/showthread.php?t=3034811))

## Note

NekoSMS was designed for devices running stock Android. If your OEM has modified 
how Android handles SMS messages at the ROM level, this app will *not* work.

## License

All code is licensed under [GPLv3](http://www.gnu.org/licenses/gpl-3.0.txt). 
Images are copyright their respective owners.

## Q&A

### Why is it called NekoSMS?

No reason in particular. I wanted a name that had "SMS" in it, and "neko" was the 
first word that popped into my head. Hence, NekoSMS.

### Why does this app require root?

[Starting from Android 4.4](http://android-developers.blogspot.in/2013/10/getting-your-sms-apps-ready-for-kitkat.html), 
only the default SMS app has the ability to intercept SMS messages, meaning that 
you could no longer use your favorite SMS app with a 3rd party SMS blocker.

In order to work around this restriction, code must be modified at the OS level, 
which requires root access.

### How does it work? (for nerds)

It hooks the internal Android class, `com.android.internal.telephony.InboundSmsHandler`. 
This class is responsible for taking the raw SMS data received by the phone and 
dispatching it to the default SMS app, then broadcasting it to all apps capable of 
reading SMS messages.

Essentially, this app intercepts the data right before it is sent to the default 
SMS app, then runs it through the user-defined filters. If the message matches 
any filter, it is discarded and no broadcasts are sent, even to the default SMS app.

### How does it work? (for non-nerds)

[Unicorn magic.](https://www.youtube.com/watch?v=wwZ4suij8oM)

### Hey! Are you trying to steal my personal data?!

[Does this look like the face of evil to you?](http://i.imgur.com/rOYrxsN.gif)

The code is fully open source, so feel free to check for yourself! :-)

### Nyaa?

[Nyaa!](http://i.imgur.com/EUkvvOl.jpg)