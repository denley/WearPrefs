[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-WearPrefs-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1570)

# WearPrefs
Allows you to easily sync SharedPreferences files between an Android app and a paired Android Wear app. Useful for creating settings that apply across devices.

## Getting Started

Add the following line to your gradle dependencies for your handheld and wearable modules (both of them).
```
compile 'me.denley.wearprefs:WearPrefs:1.1'
```

Initialize the sync by adding the following line to your `Application`'s `onCreate` method. You must do this in both apps (your handheld module and your mobile module).
```java
WearPrefs.init(this)
```

You can also specify a SharedPreferences file name when initializing. The file name must be the same on both the handheld and wearable devices. You can sync multiple files making multiple calls to `init` with different file names.
```java
WearPrefs.init(this, "my_prefs_file")
```
