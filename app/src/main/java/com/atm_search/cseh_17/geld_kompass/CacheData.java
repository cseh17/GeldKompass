package com.atm_search.cseh_17.geld_kompass;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

public final class CacheData {
    private CacheData() {
    }

    public static void writeAtmData(Context mContext, String key, LinkedList<AtmDataStructure> mObject) throws IOException {

        FileOutputStream fos = mContext.openFileOutput(key, mContext.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(mObject);
        oos.close();
        fos.close();
    }

    public static void writeObject(Context mContext, String key, Object mObject) throws IOException {

        FileOutputStream fos = mContext.openFileOutput(key, mContext.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(mObject);
        oos.close();
        fos.close();
    }

    public static Object readObject(Context mContext, String key) throws IOException, ClassNotFoundException {

        FileInputStream fis = mContext.openFileInput(key);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object mObject = ois.readObject();
        return mObject;
    }
}
