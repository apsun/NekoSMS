# NekoSMS

A regular expression based text message blocker for Android.

## Requirements

- An Android device running 5.0 Lollipop or newer
- [Xposed framework 3.0](http://forum.xda-developers.com/showthread.php?t=3034811)

## Note

NekoSMS was designed for devices running stock Android. If your OEM has modified 
how Android handles SMS messages at the ROM level, this app will *not* work.

## Q&A

### Why is it called NekoSMS?

No reason in particular. I wanted a name that had "SMS" in it, and "neko" was the 
first word that popped into my head. Hence, NekoSMS.

### How does it work?

It hooks the internal Android class, `com.android.internal.telephony.InboundSmsHandler`. 
This class is responsible for taking the raw SMS data received by the phone and 
dispatching it to the default SMS app, then broadcasting it to all apps capable of 
reading SMS messages.

Essentially, this app intercepts the data right before it is sent to the default 
SMS app, then runs it through the user-defined filters. If the message matches 
any filter, it is discarded and no broadcasts are sent, even to the default SMS app.

### Hey! Are you trying to steal my personal data?!

[Does this look like the face of evil to you?](http://i.imgur.com/rOYrxsN.gif)

### Nyaa?

[Nyaa!](http://i.imgur.com/EUkvvOl.jpg)