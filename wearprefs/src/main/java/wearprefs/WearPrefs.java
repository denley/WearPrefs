package wearprefs;

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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class WearPrefs implements SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    /** A preference key to store the DataApi path for each SharedPreferences file */
    private static final String KEY_PATH = "wearprefs_path";

    /** The DataApi path for the default SharedPreferences file */
    private static final String PATH_DEFAULT = "/default_wearprefs";

    /** The DataApi path prefix for non-default SharedPreferences files */
    private static final String PATH_PREFIX = "/wearprefs_";

    /**
     * Initializes WearPrefs synchronization for the default SharedPreferences file.
     *
     * @param context   The context containing the SharedPreferences file.
     */
    public static void init(final Context context){
        getInstance(context).initDefault();
    }

    /**
     * Initializes WearPrefs synchronization for the SharedPreferences file with the given name.
     *
     * @param context       The context containing the SharedPreferences file.
     * @param prefsFileName The name of the file to sync.
     */
    public static void init(final Context context, final String prefsFileName){
        getInstance(context).initFor(prefsFileName);
    }



    /** Instance singleton */
    private static WearPrefs sInstance = null;

    /** Retrieves the instance singleton, creating it if necessary */
    private static WearPrefs getInstance(final Context context){
        if(sInstance==null){
            sInstance = new WearPrefs(context);
        }

        return sInstance;
    }



    private Context context;

    /**
     * A cache for SharedPreferences objects, so that they may be
     * retrieved by looking up their DataApi path.
     */
    private final Map<String, SharedPreferences> mSharedPreferenceCache = new HashMap<>();

    private GoogleApiClient mApiClient;

    private WearPrefs(final Context context){
        this.context = context;

        // build and connect to the Wearable API
        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();
    }

    private void initDefault(){
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        initFor(prefs, PATH_DEFAULT);
    }

    private void initFor(final String prefsFileName){
        final SharedPreferences prefs = context.getSharedPreferences(
                prefsFileName,
                Context.MODE_PRIVATE);

        initFor(prefs, PATH_PREFIX + prefsFileName);
    }

    private void initFor(final SharedPreferences prefs, final String path){
        prefs.edit().putString(KEY_PATH, path).apply();
        mSharedPreferenceCache.put(path, prefs);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mApiClient, this);

        new Thread(){
            public void run(){
                copyAllPreferencesToLocal();
            }
        }.start();
    }

    private void copyAllPreferencesToLocal(){
        for(String path:mSharedPreferenceCache.keySet()) {
            copyAllPreferencesToLocalForPath(path);
        }
    }

    private void copyAllPreferencesToLocalForPath(String path){
        final SharedPreferences prefs = mSharedPreferenceCache.get(path);
        final DataItemBuffer buffer = Wearable.DataApi.getDataItems(
                mApiClient,
                Uri.parse("wear:" + path))
                .await();

        if(buffer.getCount() > 0) {
            final DataMap map = DataMap.fromByteArray(buffer.get(0).getData());
            loadPrefsFromDataMap(prefs, map);
        }
    }


    @Override public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                                    String key) {

        // Ignore path preference changes, this is only used internally
        if(key.equalsIgnoreCase(KEY_PATH)){
            return;
        }

        new Thread(){
            public void run(){
                final String path = sharedPreferences.getString(KEY_PATH, PATH_DEFAULT);

                // Synchronize on the cache, so that data isn't received simultaneously
                synchronized (mSharedPreferenceCache){
                    final PutDataMapRequest request = PutDataMapRequest.create(path);

                    // Copy all preferences to a DataMap
                    final Map<String, ?> prefsMap = sharedPreferences.getAll();
                    final DataMap dataMap = request.getDataMap();
                    for(String key:prefsMap.keySet()) {
                        saveObject(dataMap, key, prefsMap.get(key));
                    }

                    Wearable.DataApi.putDataItem(
                            mApiClient,
                            request.asPutDataRequest());
                }
            }
        }.start();
    }

    @Override public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event:dataEvents){
            final DataItem changedDataItem = event.getDataItem();
            final String path = changedDataItem.getUri().getPath();
            final SharedPreferences prefs = mSharedPreferenceCache.get(path);

            if(prefs!=null){
                final DataMap data = DataMap.fromByteArray(changedDataItem.getData());
                loadPrefsFromDataMap(prefs, data);
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void loadPrefsFromDataMap(final SharedPreferences prefs, final DataMap data){
        new Thread(){
            public void run(){

                // Synchronize on the cache, so that data isn't sent simultaneously
                synchronized (mSharedPreferenceCache) {
                    final SharedPreferences.Editor editor = prefs.edit();
                    for (String key : data.keySet()) {
                        saveObject(editor, key, data.get(key));
                    }
                    prefs.unregisterOnSharedPreferenceChangeListener(WearPrefs.this);

                    try {
                        editor.commit();
                    } finally {
                        prefs.registerOnSharedPreferenceChangeListener(WearPrefs.this);
                    }
                }
            }
        }.start();
    }

    /**
     * Determines the type of the given object and saves it into
     * the given preference file accordingly
     */
    private void saveObject(final SharedPreferences.Editor editor,
                            final String key,
                            final Object o){

        if(o instanceof String){
            editor.putString(key, (String)o);
        }else if(o instanceof Integer){
            editor.putInt(key, (Integer)o);
        }else if(o instanceof Boolean){
            editor.putBoolean(key, (Boolean)o);
        }else if(o instanceof Long){
            editor.putLong(key, (Long)o);
        }else if(o instanceof Float){
            editor.putFloat(key, (Float)o);
        }else if(o instanceof ArrayList){
            final Set asSet = new HashSet();
            asSet.addAll((ArrayList)o);

            editor.putStringSet(key, asSet);
        }else{
            throw new IllegalArgumentException("SharedPreferences does not accept "
                    +o.getClass().getName()+ " objects");
        }
    }

    /** Determines the type of the given object and saves it into the given DataMap accordingly */
    private void saveObject(final DataMap editor,
                            final String key,
                            final Object o){

        if(o instanceof String){
            editor.putString(key, (String)o);
        }else if(o instanceof Integer){
            editor.putInt(key, (Integer)o);
        }else if(o instanceof Boolean){
            editor.putBoolean(key, (Boolean)o);
        }else if(o instanceof Long){
            editor.putLong(key, (Long)o);
        }else if(o instanceof Float){
            editor.putFloat(key, (Float)o);
        }else if(o instanceof Set){
            final ArrayList asList = new ArrayList();
            asList.addAll((Set) o);
            editor.putStringArrayList(key, asList);
        }else{
            throw new IllegalArgumentException("SharedPreferences does not accept "
                    +o.getClass().getName()+ " objects");
        }
    }

    @Override public void onConnectionSuspended(int i) {}

    @Override public void onConnectionFailed(ConnectionResult connectionResult) {}

}

