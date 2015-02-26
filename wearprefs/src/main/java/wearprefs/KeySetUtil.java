package wearprefs;

import android.net.Uri;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class KeySetUtil {

    private static final String KEY_KEY_SET = "key_set";

    public static ArrayList<String> getKeySet(GoogleApiClient apiClient, String path) {
        final DataItemBuffer buffer = Wearable.DataApi.getDataItems(
                apiClient,
                Uri.parse("wear:" + path))
                .await();

        ArrayList<String> keySet = null;
        try {
            if (buffer.getCount() > 0) {
                final DataMap map = DataMap.fromByteArray(buffer.get(0).getData());
                keySet = map.getStringArrayList(KEY_KEY_SET);
            }
        }finally {
            buffer.release();
        }

        return keySet==null?new ArrayList<String>():keySet;
    }

    private static void setKeySet(GoogleApiClient apiClient, String path, ArrayList<String> keySet) {
        final PutDataMapRequest request = PutDataMapRequest.create(path);
        final DataMap dataMap = request.getDataMap();
        dataMap.putStringArrayList(KEY_KEY_SET, keySet);

        Wearable.DataApi.putDataItem(apiClient, request.asPutDataRequest()).await();
    }

    public static void addToKeySet(GoogleApiClient apiClient, String path, String key) {
        final ArrayList<String> keySet = getKeySet(apiClient, path);

        if(!keySet.contains(key)){
            keySet.add(key);
            setKeySet(apiClient, path, keySet);
        }
    }

}
