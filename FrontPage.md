# AlertMe for Android #

A simple tool for Android phones to interact with their [AlertMe](http://www.alertme.com) home security/automation/smart energy system.

This uses the AlertMe API to interact with your systems remotely. You can arm/disarm/night-arm remotely as well as turn on/off the smart plug.

## Dependancies ##
  * [Android XMLRPC](http://code.google.com/p/android-xmlrpc/)

## Files of particular interest ##

  * alertme/api/AlertMeServer.java - does the grunt work; represents API functions
  * server.py - for testing purposes (alter AlertMeServer.java) to use the localhost URI rather than the live one

# Credits #

  * [AlertMePI](http://code.google.com/p/alertmepi/): first really demonstrating how the AlertMe API works (before getting official documentation)
  * [Android XMLRPC](http://code.google.com/p/android-xmlrpc/): interactions with the API