# NekoSMS

A pattern-based text message blocker for Android.

## Example configuration

Block all messages that contain the word `spam`:
> Pattern: `spam`  
> Field: `Body`  
> Mode: `Contains`

Block all messages coming from numbers that start with `12345`:
> Pattern: `12345`  
> Field: `Sender`  
> Mode: `Starts with`

Block all messages starting with `the` and ending with `game`:
> Pattern: `^the.*game$`  
> Field: `Body`  
> Mode: `Regular expression`

## Requirements

- A rooted phone running Android 4.4 KitKat or newer
- [Xposed framework](http://forum.xda-developers.com/xposed/xposed-installer-versions-changelog-t2714053)
  ([Lollipop version](http://forum.xda-developers.com/showthread.php?t=3034811))

## Note

NekoSMS was designed for devices running stock Android. If your ROM has made 
significant changes to the way Android internally handles SMS messages, this 
app might not work.

## License

All code is licensed under [GPLv3](http://www.gnu.org/licenses/gpl-3.0.txt). 
Images are copyright their respective owners.

## Q&A

### Why is it called NekoSMS?

No reason in particular. I wanted a name that had "SMS" in it, and "neko" was the 
first word that popped into my head. Hence, NekoSMS.

### Why does this app require root/Xposed?

[Starting from Android 4.4](http://android-developers.blogspot.in/2013/10/getting-your-sms-apps-ready-for-kitkat.html), 
only the default SMS app has the ability to intercept SMS messages. In order to 
bypass this restriction, code must be modified at the OS level, which requires root.

### Does NekoSMS work with (insert SMS app here)?

As long as your ROM supports it, yes. There is no app-specific SMS blocking 
code, so if it works with one SMS app, it will work with them all.

### How does it work? (for nerds)

It hooks the internal Android class, `com.android.internal.telephony.InboundSmsHandler`. 
This class is responsible for taking the raw SMS data received by the phone and 
dispatching it to the default SMS app, then broadcasting it to all apps capable of 
reading SMS messages.

Essentially, this app intercepts the data right before it is sent to the default 
SMS app, then runs it through the user-defined filters. If the message matches 
a filter, it is discarded and no broadcasts are sent.

### How does it work? (for non-nerds)

[Unicorn magic.](https://www.youtube.com/watch?v=wwZ4suij8oM)

### Hey! Are you trying to steal my personal data?!

[Does this look like the face of evil to you?](http://i.imgur.com/rOYrxsN.gif)

The code is fully open source, so feel free to check for yourself! :-)

### Nyaa?

[Nyaa!](http://i.imgur.com/EUkvvOl.jpg)