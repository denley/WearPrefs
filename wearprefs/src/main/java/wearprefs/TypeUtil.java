package wearprefs;

import android.content.SharedPreferences;

import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Denley on 27/02/2015.
 */
public class TypeUtil {

    /**
     * Determines the type of the given object and saves it into
     * the given preference file accordingly
     */
    public static void saveObject(final SharedPreferences.Editor editor,
                            final String key,
                            final Object o){

        if(o==null) {
            editor.remove(key);
        }else if(o instanceof String){
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
    public static void saveObject(final DataMap editor, final String key, final Object o){

        if(o==null) {
            editor.remove(key);
        }else if(o instanceof String){
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

}
