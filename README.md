[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-WearPrefs-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1570)

# WearPrefs
Allows you to easily sync SharedPreferences files between an Android app and a paired Android Wear app. Useful for creating settings that apply across devices.

## Usage

Add the following line to your gradle dependencies for your handheld and wearable modules (both of them).
```
compile 'me.denley.wearprefs:WearPrefs:1.1.2'
```

Initialize the sync by adding the following line to your `Application`'s `onCreate` method. You must do this in both apps (your handheld module and your mobile module).
```java
WearPrefs.init(this)
```

You can also specify a SharedPreferences file name when initializing. The file name must be the same on both the handheld and wearable devices. You can sync multiple files making multiple calls to `init` with different file names.
```java
WearPrefs.init(this, "my_prefs_file")
```

## License
```
The MIT License (MIT)

Copyright (c) 2015 Denley Bihari

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```