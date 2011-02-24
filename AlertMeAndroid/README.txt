AlertMe for Android

Bluh bluh bluh.

If you don't know what AlertMe is then you probably don't have a use for this
application. It provides a way of monitoring (supposibly) your AlertMe system
without the web-version clunk.

So far there is support for multiple accounts and multiple hubs. A dummy server
is provided for testing/debugging purposes (change the server URI to
localhost).

The main grunt of this is org.darkgoddess.alertme.api.AlertMeServer which does
all the API calls via XMLRPC, so if you want to write your own interface at
least it does provide some documentation on what functions you *actually* can
do (DON'T ASK; ok, based on what rare sample API scripts) can do.  .. like
setHub: unless you have more than one hub would you know of that function?????
</rant>

As of yet, I only have vague plans of implementing the eco, greenie,
oh-it's-so-hot-to-be-a-hippie energy interfaces as with the second version of
the web interface but I think I'll try finding graphic libraries first for
the display of the device data. And possibly do remote smart-plug toggling.


I hope you enjoy this or at least find the code useful!


GO CREDITS! GO CREDITS!
XMLRPC:
http://code.google.com/p/android-xmlrpc/

Separated List adapter:
http://jsharkey.org/blog/2008/08/18/separating-lists-with-headers-in-android-09/

Found the icons:
http://findicons.com/pack/753/gnome_desktop



