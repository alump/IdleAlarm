# IdleAlarm Add-on for Vaadin 7

[![Build Status](http://siika.fi:8888/jenkins/job/IdleAlarm%20(Vaadin)/badge/icon)](http://siika.fi:8888/jenkins/job/IdleAlarm%20(Vaadin)/)

IdleAlarm is Vaadin add-on designed to be used with Vaadin's idle timeout feature. It adds alarm that is shown to user
before sessions gets removed because of long idling. Message shown to user, and how long before idle expire it's shown,
can be configured by server side API. Actual logic is fully on client side, to prevent pushing idle timeout to future.

## Online demo

Try the add-on demo at http://app.siika.fi/IdleAlarmDemo

## Developer Guide
```java
// Show default warning 60s before session will be expired
IdleAlarm.get().setSecondsBefore(60);

// Or construct your own message
IdleAlarm.get().setSecondsBefore(60)
    .setMessage("Your session will expire in less than "
        + IdleAlarmFormatting.SECS_TO_TIMEOUT
        + " seconds. Please click anywhere to extend session.");
```

For more examples, check demo project from GitHub.

## Download release

Official releases of this add-on are available at Vaadin Directory. For Maven instructions, download and reviews, go to
http://vaadin.com/addon/idlealarm

## Release notes

### Version 0.1.1 (2015-07-16)
- Errors (compile and bundling) in 0.1.0 fixed
- SCSS values can be now defined by variables
- Content mode of warnings added to demo app
- Minor improvement to default styling

### Version 0.1.0 (2015-07-15)
- Initial release
- OSGi bundled
- Server side API to define message (time to timeout, timeout length and time sense reset template variables available)
- IdleCountdownLabel for idle debugging (updates once a second, supports same variables as IdleAlarm)

## Roadmap

High priority features:
- Location of alarm to be defined by server side API (top left, ... center, ... bottom right)

Lower priority features:
- TODO

## Building and running demo

git clone https://github.com/alump/IdleAlarm.git
mvn clean install
cd idlealarm-demo
mvn jetty:run

To see the demo, navigate to http://localhost:8080/

## Development with Eclipse IDE

For further development of this add-on, the following tool-chain is recommended:
- Eclipse IDE
- m2e wtp plug-in (install it from Eclipse Marketplace)
- Vaadin Eclipse plug-in (install it from Eclipse Marketplace)
- JRebel Eclipse plug-in (install it from Eclipse Marketplace)
- Chrome browser

### Importing project

Choose File > Import... > Existing Maven Projects

Note that Eclipse may give "Plugin execution not covered by lifecycle configuration" errors for pom.xml. Use "Permanently mark goal resources in pom.xml as ignored in Eclipse build" quick-fix to mark these errors as permanently ignored in your project. Do not worry, the project still works fine. 

### Debugging server-side

If you have not already compiled the widgetset, do it now by running vaadin:install Maven target for IdleAlarm project.

If you have a JRebel license, it makes on the fly code changes faster. Just add JRebel nature to your idlealarm-demo project by clicking project with right mouse button and choosing JRebel > Add JRebel Nature

To debug project and make code modifications on the fly in the server-side, right-click the idlealarm-demo project and choose Debug As > Debug on Server. Navigate to http://localhost:8080 to see the application.

### Debugging client-side

The most common way of debugging and making changes to the client-side code is dev-mode. To create debug configuration for it, open idlealarm-demo project properties and click "Create Development Mode Launch" button on the Vaadin tab. Right-click newly added "GWT development mode for idlealarm-demo.launch" and choose Debug As > Debug Configurations... Open up Classpath tab for the development mode configuration and choose User Entries. Click Advanced... and select Add Folders. Choose Java and Resources under idlealarm-addon/src/main and click ok. Now you are ready to start debugging the client-side code by clicking debug. Click Launch Default Browser button in the GWT Development Mode in the launched application. Now you can modify and breakpoints to client-side classes and see changes by reloading the web page. 

Another way of debugging client-side is superdev mode. To enable it, uncomment devModeRedirectEnabled line from the end of DemoWidgetSet.gwt.xml located under idlealarm-demo resources folder and compile the widgetset once by running vaadin:compile Maven target for idlealarm-demo. Refresh idlealarm-demo project resources by right clicking the project and choosing Refresh. Click "Create SuperDevMode Launch" button on the Vaadin tab of the idlealarm-demo project properties panel to create superder mode code server launch configuration and modify the class path as instructed above. After starting the code server by running SuperDevMode launch as Java application, you can navigate to http://localhost:8080/?superdevmode. Now all code changes you do to your client side will get compiled as soon as you reload the web page. You can also access Java-sources and set breakpoints inside Chrome if you enable source maps from inspector settings.

## Issue tracking

The issues for this add-on are tracked on its github.com page at https://github.com/alump/IdleAlarm/issues
All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome, but there are no guarantees that they are accepted as such. Process for contributing is the following:
- Fork this project
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- Refer to the fixed issue in commit
- Send a pull request for the original project
- Comment on the original issue that you have implemented a fix for it

## License & Author

Add-on is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

IdleAlarm extension is written by Sami Viitanen <sami.viitanen@gmail.com>
