# Toolbox library
This is a collection of commonly used functions in Android apps from BaltiApps.  
Feel free to use and contribute.

## How to get it
### Prerequisite
App should have an "app instance class" extending `Application` class.  
That class should be registered in the `android:name` component of the application manifest.  
Example: 
```
class AppInstance: Application() {
...
}
```
```
<manifest ...>

    <application
        ...
        android:name=".AppInstance">
        
        ...
        
        </application>

</manifest>
``` 
### Import the library
1. Clone this repo/download as zip and extract.
2. In your Android Studio project, goto `File -> New -> Import module`
3. Navigate to the the extracted folder and click `Ok`  
   **Verification**: go to `build.gradle` of your `app` module and check if dependencies have this:
   ```
   implementation project(path: ':baltitoolbox')
   ```
   Go to `settings.gradle` file and check if the module is included:
   ```
   include ':app', ':baltitoolbox'
   ```
4. Register the `init` function in `onCreate` of your app instance class
   ```
   import balti.module.baltitoolbox.ToolboxHQ
   class AppInstance: Application() {
       ...
       override fun onCreate() {
           ...
           ToolboxHQ.init(this)
       }
   }
   ```
5. Done! Now you can use this library

## Available functions
All functions are "static". Hence they can be used from anywhere in the app.  
