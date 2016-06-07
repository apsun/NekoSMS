# NekoSMS

A pattern-based text message blocker for Android.

## Requirements

- A rooted phone running Android 4.4 KitKat or newer
- [Xposed framework](http://forum.xda-developers.com/xposed)

## Example filter rules

Block all messages...

...containing the word `spam`:  
\[`Body`\] \[`Contains`\] \[`spam`\]

...coming from numbers in the format `100XX`:  
\[`Sender`\] \[`Wildcard`\] \[`100??`\]

...coming from `12345`, and containing either `megane` or `poi`:  
\[`Sender`\] \[`Equals`\] \[`12345`\] + \[`Body`\] \[`Regular expression`\] \[`megane|poi`\]

## Notes

NekoSMS was designed for devices running stock Android. If your ROM has made
significant changes to the way Android internally handles SMS messages, this
app might not work.

Additionally, when matching against the sender field, you must use the
*unformatted* sender number. In most SMS apps, this info can be obtained by
viewing the extended message info. If the value begins with a `+`, you must
include the `+` in your filter pattern.

## Q&A

### Why is it called NekoSMS?

No reason in particular. I wanted a name that had "SMS" in it, and "neko" was the
first word that popped into my head. Hence, NekoSMS.

### Why does this app require Xposed?

[Starting from Android 4.4, only the default SMS app has the ability to intercept SMS messages]
(http://android-developers.blogspot.in/2013/10/getting-your-sms-apps-ready-for-kitkat.html).
In order to bypass this restriction, code must be modified at the OS level.

### What are the permissions used for?

- `READ_SMS`, `WRITE_SMS` for obvious reasons
- `VIBRATE` for notifications
- `WRITE_EXTERNAL_STORAGE` for importing/exporting backups

### Does NekoSMS work with (insert SMS app here)?

As long as your ROM supports it, yes. There is no app-specific SMS blocking
code, so if it works with one SMS app, it will work with them all.

### How does it work? (for nerds)

It hooks the internal Android class, [`com.android.internal.telephony.InboundSmsHandler`]
(https://android.googlesource.com/platform/frameworks/opt/telephony/+/master/src/java/com/android/internal/telephony/InboundSmsHandler.java).
This class is responsible for taking the raw SMS data received by the phone and
dispatching it to the default SMS app, then broadcasting it to all apps capable of
reading SMS messages.

Essentially, this app intercepts the data right before it is sent to the default
SMS app, then runs it through the user-defined filters. If the message matches
a filter, the broadcast is dropped.

### How does it work? (for non-nerds)

[Unicorn magic.](https://www.youtube.com/watch?v=wwZ4suij8oM)

### Hey! Are you trying to steal my personal data?!

[Does this look like the face of evil to you?](http://i.imgur.com/rOYrxsN.gif)

The code is fully open source, so feel free to check for yourself! :-)

### Nyaa?

[Nyaa!](http://i.imgur.com/EUkvvOl.jpg)

## License

All code is licensed under [GPLv3](http://www.gnu.org/licenses/gpl-3.0.txt).
Icons are from Google's [material icons library](https://design.google.com/icons/).
