package com.example.pallavi.catchupcontacts;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class phContact{
    String name;
    String contact_id;
    String phone_number;
    String photo_uri;
    String lookup_key;
    String _id;

    private static final String TAG = "phContactLogs";
    public phContact(){

    }

    public phContact(String name, String contact_id, String phone_number, String photo_uri, String lookup_key, String _id){
        this.name = name;
        this.contact_id = contact_id;
        this.phone_number = phone_number;
        this.photo_uri = photo_uri;
        this.lookup_key = lookup_key;
        this._id = _id;
    }

    public String toString(){
        return "\nName:--- "+name +" \nContact id:--- "+contact_id+" \nPhone_number:--- "+phone_number+" \nPhoto uri :--- "+photo_uri + "\n";
    }

    public static List<phContact> getPriorityContacts(List<phCall> callLogList, Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
//        String dateInString = "03-05-2015 10:20:56";
//
//
//        Date recentDate = null;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -5);
        Date recentDate = cal.getTime();
        List<String> recentCallContactsIds = new ArrayList<String>();
        List<String> formerCallContactsIds = new ArrayList<String>();
        List<String> allCallContactsIds = new ArrayList<String>();
        Calendar cal1 = Calendar.getInstance();
        Set<String> uniqueIds = new HashSet<>();
        Map<String, Double> mapIdpval = new HashMap<String, Double>();
        Map<String, Double> orderedMapIdpval = new LinkedHashMap<>();
//        try {
//            recentDate = sdf.parse(dateInString);
//
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }

        cal1.add(Calendar.DATE, -14);
        Date formerDate = cal1.getTime();

        // make a list of contacts who called after former and recent dates
        for (int i = 0; i < callLogList.size(); i++) {
            phCall iCall = callLogList.get(i);
            Date calldate = iCall.dateTime;
            allCallContactsIds.add(iCall.contact_id);
            //add contact id to allCalls id list
            if (calldate.after(formerDate)) {
                // make a list of after contact ids
                formerCallContactsIds.add(iCall.contact_id);
                uniqueIds.add(iCall.contact_id);
            }
            if (calldate.after(recentDate)) {
                recentCallContactsIds.add(iCall.contact_id);
            }
        }

        //get the contacted contacts and update list
        String contactedData = Util.readFromFile(Util.fNameContacted, context);
        Log.d(TAG, contactedData);
        if(!contactedData.isEmpty()) {
            String[] c_contact_ids = contactedData.split(";");
            for (String str : c_contact_ids) {
                String[] s = str.split("=");
                String id = s[0];
                Date date;
                try {
                    date = sdf.parse(s[1]);
                    if (date.after(formerDate)) {
                        if (!formerCallContactsIds.contains(id)) {
                            formerCallContactsIds.add(id);
                            uniqueIds.add(id);
                        }
                    }
                    if (date.after(recentDate)) {
                        if (!recentCallContactsIds.contains(id)) {
                            recentCallContactsIds.add(id);
                        }
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
        }


        //create a map with their id and priority factor
        for (String idItem : uniqueIds) {
            double formerOcc = Collections.frequency(formerCallContactsIds, idItem);
            double recentOcc = Collections.frequency(recentCallContactsIds, idItem);
            double allOcc = Collections.frequency(allCallContactsIds, idItem);
            double priorityVal = (recentOcc / formerOcc) + (1/15)*(1/Math.log(allOcc+2));
            mapIdpval.put(idItem, priorityVal);
        }

        //get the ignored contacts
        String ignoredData = Util.readFromFile(Util.fNameIgnored, context);
        if(!ignoredData.isEmpty()) {
            String[] icontact_ids = ignoredData.split(";");
            for (String cId : icontact_ids) {
                if (mapIdpval.containsKey(cId)) {
                    mapIdpval.remove(cId);
                }
            }
        }



        //sort the map with priority factor
        List<Map.Entry<String, Double>> entries = new ArrayList<Map.Entry<String, Double>>(mapIdpval.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> lhs, Map.Entry<String, Double> rhs) {
                return lhs.getValue().compareTo(rhs.getValue());
            }
        });


        // get the ordered map
        for (Map.Entry<String, Double> entry : entries) {
            orderedMapIdpval.put(entry.getKey(), entry.getValue());
        }

        // make an ordered contact list according to priority
        List<phContact> pContactList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : orderedMapIdpval.entrySet()) {
            String contactId = entry.getKey();
            phContact ac = getContactFromId(contactId, context);
            if(ac!=null) {
                pContactList.add(ac);
            }
        }



        //Log.d(TAG, pContactList.toString());
        return pContactList;
    }

    private static phContact getContactFromId(String id, Context context) {
        if(id!=null) {
            String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                    ContactsContract.CommonDataKinds.Phone._ID};
//        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
//                Uri.encode(id));
            Cursor c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

            int indexName = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY);
            int indexNumber = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int contactId = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int photoUri = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI);
            int lookUPkey = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY);
            int xid = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);

            if (c.moveToFirst()) {
                String name = c.getString(indexName);
                String cid = c.getString(contactId);
                String phNum = c.getString(indexNumber);
                String pURI = c.getString(photoUri);
                String lKey = c.getString(lookUPkey);
                String _id = c.getString(xid);
                phContact con = new phContact(name, cid, phNum, pURI,lKey,_id);
                return con;
            }
        }
        return null;
    }

//    public List<phContact> getContacts() {
//
//        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
//        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
//                ContactsContract.CommonDataKinds.Phone.NUMBER,
//                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
//                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI};
//
//        Cursor people = getContentResolver().query(uri, projection, null, null, null);
//
//        int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY);
//        int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
//        int contactId = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
//        int photoUri = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI);
//
//        List<phContact> phoneContacts = new ArrayList<phContact>();
//        people.moveToFirst();
//        do {
//            String name   = people.getString(indexName);
//            String cid = people.getString(contactId);
//            String phNum = people.getString(indexNumber);
//            String pURI = people.getString(photoUri);
//            phContact c = new phContact(name, cid, phNum, pURI);
//            phoneContacts.add(c);
//        } while (people.moveToNext());
//
//        people.close();
//
//        return phoneContacts;
//
//    }





}
