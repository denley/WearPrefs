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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class WearPrefs implements SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, NodeApi.NodeListener {

    // DataMap key names for transmitting preference values
    private static final String KEY_FILE_NAME = "file_name";
    private static final String KEY_KEY = "key";
    private static final String KEY_VALUE = "value";

    // Path prefixes, for constructing datamap paths
    private static final String PATH_PREFIX_DEFAULT = "/default_wearprefs_";
    private static final String PATH_PREFIX = "/wearprefs_";


    /**
     * Initializes WearPrefs synchronization for the default SharedPreferences file.
     *
     * @param context   The context containing the SharedPreferences file.
     */
    public static void init(@NonNull final Context context){
        getInstance(context).initDefault();
    }

    /**
     * Initializes WearPrefs synchronization for the SharedPreferences file with the given name.
     *
     * @param context       The context containing the SharedPreferences file.
     * @param prefsFileName The name of the file to sync.
     */
    public static void init(@NonNull final Context context, @NonNull final String prefsFileName){
        getInstance(context).initFor(prefsFileName);
    }



    /** Instance singleton */
    private static WearPrefs sInstance = null;

    /** Retrieves the instance singleton, creating it if necessary */
    private static WearPrefs getInstance(@NonNull final Context context){
        if(sInstance==null){
            sInstance = new WearPrefs(context);
        }

        return sInstance;
    }



    @NonNull private final Context context;

    /**
     * A cache for SharedPreferences objects, so that they may be
     * retrieved by looking up their DataApi path.
     */
    @NonNull private final Map<String, SharedPreferences> mSharedPreferenceCache = new HashMap<>();

    @NonNull private final GoogleApiClient mApiClient;

    private WearPrefs(@NonNull final Context context){
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
        initFor(prefs, null);
    }

    private void initFor(@NonNull final String prefsFileName){
        final SharedPreferences prefs = context.getSharedPreferences(
                prefsFileName,
                Context.MODE_PRIVATE);

        initFor(prefs, prefsFileName);
    }

    private void initFor(@NonNull final SharedPreferences prefs, @Nullable final String prefsFileName){
        final String pathPrefix = getPathPrefix(prefsFileName);

        prefs.edit().putString(KEY_FILE_NAME, prefsFileName).apply();
        mSharedPreferenceCache.put(pathPrefix, prefs);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mApiClient, this);
        Wearable.NodeApi.addListener(mApiClient, this);

        new Thread(){
            public void run(){
                copyAllPreferencesToLocal();
            }
        }.start();
    }

    @Override public void onPeerConnected(Node node) {
        new Thread(){
            public void run(){
                copyAllPreferencesToLocal();
            }
        }.start();
    }

    private void copyAllPreferencesToLocal(){
        for(String pathPrefix:mSharedPreferenceCache.keySet()) {
            copyAllPreferencesToLocalForPathPrefix(pathPrefix);
        }
    }

    private void copyAllPreferencesToLocalForPathPrefix(String pathPrefix){
        for(String key:KeySetUtil.getKeySet(mApiClient, pathPrefix)){
            copyPreferenceToLocal(pathPrefix + key);
        }
    }

    private void copyPreferenceToLocal(final String path) {
        final DataItemBuffer buffer = Wearable.DataApi.getDataItems(
                mApiClient,
                Uri.parse("wear:" + path))
                .await();

        try {
            copyPreferenceToLocal(buffer);
        }finally {
            buffer.release();
        }
    }

    private void copyPreferenceToLocal(DataItemBuffer buffer) {
        if (buffer.getCount() > 0) {
            final DataMap map = DataMap.fromByteArray(buffer.get(0).getData());
            loadPrefFromDataMap(map);
        }
    }

    @Override public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                                    final String updatedKey) {

        // Ignore path preference changes, this is only used internally
        if(updatedKey.equalsIgnoreCase(KEY_FILE_NAME)){
            return;
        }

        new Thread(){
            public void run(){
                onSharedPreferenceChangedAsync(sharedPreferences, updatedKey);
            }
        }.start();
    }

    private void onSharedPreferenceChangedAsync(final SharedPreferences sharedPreferences,
                                                final String updatedKey) {

        final String fileName = sharedPreferences.getString(KEY_FILE_NAME, null);
        final String pathPrefix = getPathPrefix(fileName);
        final String path = pathPrefix + updatedKey;

        // Synchronize on the cache, so that data isn't received simultaneously
        synchronized (mSharedPreferenceCache){
            updateValueRemote(sharedPreferences, updatedKey, fileName, path);
            KeySetUtil.addToKeySet(mApiClient, pathPrefix, updatedKey);
        }
    }

    private void updateValueRemote(final SharedPreferences sharedPreferences,
                                   final String key, final String fileName, final String path) {
        final PutDataMapRequest request = PutDataMapRequest.create(path);

        final Map<String, ?> prefsMap = sharedPreferences.getAll();
        final DataMap dataMap = request.getDataMap();
        dataMap.putString(KEY_FILE_NAME, fileName);
        dataMap.putString(KEY_KEY, key);
        TypeUtil.saveObject(dataMap, KEY_VALUE, prefsMap.get(key));

        Wearable.DataApi.putDataItem(
                mApiClient,
                request.asPutDataRequest());
    }

    @Override public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event:dataEvents){
            final DataItem changedDataItem = event.getDataItem();
            final DataMap data = DataMap.fromByteArray(changedDataItem.getData());
            loadPrefFromDataMap(data);
        }
    }

    private void loadPrefFromDataMap(final DataMap data){
        new Thread(){
            public void run(){
                loadPrefFromDataMapAsync(data);
            }
        }.start();
    }

    private void loadPrefFromDataMapAsync(@NonNull final DataMap data){
        final String key = data.get(KEY_KEY);
        final String fileName = data.getString(KEY_FILE_NAME);
        final Object value = data.get(KEY_VALUE);

        final SharedPreferences prefs = mSharedPreferenceCache.get(getPathPrefix(fileName));

        if(key==null || prefs==null) {
            return;
        }

        // Synchronize on the cache, so that data isn't sent simultaneously
        synchronized (mSharedPreferenceCache) {
            saveValueToPrefs(prefs, key, value);
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void saveValueToPrefs(SharedPreferences prefs, String key, Object value){
        final SharedPreferences.Editor editor = prefs.edit();
        TypeUtil.saveObject(editor, key, value);

        prefs.unregisterOnSharedPreferenceChangeListener(WearPrefs.this);
        try {
            editor.commit();
        } finally {
            prefs.registerOnSharedPreferenceChangeListener(WearPrefs.this);
        }
    }

    private String getPathPrefix(final String fileName) {
        return fileName==null
                ?PATH_PREFIX_DEFAULT
                :(PATH_PREFIX+fileName+"_");
    }

    @Override public void onConnectionSuspended(int i) { }
    @Override public void onConnectionFailed(ConnectionResult connectionResult) { }
    @Override public void onPeerDisconnected(Node node) { }
}

