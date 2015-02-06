package wearprefs.sample;

//
// The MIT License (MIT)
//
// Copyright (c) 2015 Denley Bihari
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import java.util.Map;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView mText;
    private SharedPreferences mPrefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup layout
        setContentView(R.layout.activity_main);
        mText = (TextView) findViewById(R.id.text_main);

        // Listen for prefs changes
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        onSharedPreferenceChanged(mPrefs, null);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override protected void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Print out all key:value pairs to the screen

        final Map<String, ?> allPrefs = mPrefs.getAll();

        final StringBuilder sb = new StringBuilder();
        for(String k:allPrefs.keySet()){
            sb.append(k).append(": ").append(allPrefs.get(k)).append("\n");
        }

        mText.setText(sb);
    }

}
