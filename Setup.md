#Setting up your environment to get things going

# Prerequisite #

I'm assuming you are using Eclipse for this, so replace Eclipse-specific steps with ones appropriate for your environment

# Environment #

Installing stuff; happy times:

  * Android SDK: http://developer.android.com/sdk/index.html
  * Eclipse: http://www.eclipse.org/
  * Android Development Tools for Eclipse: http://developer.android.com/tools/sdk/eclipse-adt.html


## Dependancies ##

  * http://code.google.com/p/android-xmlrpc/

Get it if you aren't going to alter AlertMeServer.java, which is the only file actually communicating with the API.

# Build #

The spoon:

  1. Decompress the gzip file for the Android XMLRPC source. Import it as a new project in Eclipse. Set 'Is Library' flag in the project properties
  1. Compile and export Android XMLRPC as a library. This should produce a .jar file
  1. Copy this resulting .jar into Alert Droid's /lib directory
  1. Select the Alert Droid project properties and select 'Java Build Path'. Select 'Add External Jars' and select the XMLRPC jar file, and confirm with 'OK'
  1. Alert Droid should now compile after integrating Android XMLRPC


Some tuts:

  * Creating library projects and using them: http://www.vogella.com/articles/AndroidLibraryProjects/article.html
  * Importing jars to Android projects: http://stackoverflow.com/questions/1334802/how-can-i-use-external-jars-in-an-android-project