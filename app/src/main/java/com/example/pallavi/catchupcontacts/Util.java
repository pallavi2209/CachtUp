package com.example.pallavi.catchupcontacts;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

/**
 * Created by pallavi on 5/5/15.
 */
public class Util {

    public static String fNameContacted = "contacted.txt";
    public static String fNameIgnored = "ignored.txt";
    public static String fNameLater = "later.txt";

    public static void writeToFile(String data, String filename, Context ctxt) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(ctxt.openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    public static String readFromFile(String filename, Context ctxt) {

        String ret = "";

        try {
            InputStream inputStream = ctxt.openFileInput(filename);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("show contacts activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("show contacts activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static String getUpdatedContactedData(String oldData, Map<String, String> map){

        String returnStr = "";
        if(!oldData.isEmpty()) {
            String[] items = oldData.split(";");
            for (String str : items) {
                String[] s = str.split("=");
                String id = s[0];
                String date = s[1];
                if (!map.containsKey(id)) {
                    map.put(id, date);
                }
            }
        }

        for(Map.Entry<String, String> entry: map.entrySet()){
            returnStr += entry.getKey() + "=" + entry.getValue() + ";";
        }

        return returnStr;

    }

    public static String getUpdatedLaterOrIgnore(String oldData, List<String> listNewData ){
        String retStr = "";
        if(!oldData.isEmpty()) {
            String[] items = oldData.split(";");
            for (String str : items) {
                if (!listNewData.contains(str)) {
                    listNewData.add(str);
                }
            }
        }

        for (String str : listNewData){
            retStr += str + ";";
        }

        return retStr;

    }


    /**
     * Uses static final constants to detect if the device's platform version is Gingerbread or
     * later.
     */
    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    /**
     * Uses static final constants to detect if the device's platform version is Honeycomb or
     * later.
     */
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * Uses static final constants to detect if the device's platform version is Honeycomb MR1 or
     * later.
     */
    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /**
     * Uses static final constants to detect if the device's platform version is ICS or
     * later.
     */
    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
}
