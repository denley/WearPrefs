# WearPrefs
Allows you to easily sync SharedPreferences files between an Android app and a paired Android Wear app. Useful for creating settings that apply across devices.

## Getting Started

Add the following line to your gradle dependencies for your handheld and wearable modules (both of them).
```
compile ''
```

Initialize the sync by adding the following line to your `Application` or main `Activity`'s `onCreate` method. You must do this in both apps (your handheld module and your mobile module).
```java
WearPrefs.init(this)
```

You can also specify a SharedPreferences file name when initializing. The file name must be the same on both the handheld and wearable devices. You can also sync multiple files by adding extra calls to `init`.
```java
WearPrefs.init(this, "my_prefs_file")
```

