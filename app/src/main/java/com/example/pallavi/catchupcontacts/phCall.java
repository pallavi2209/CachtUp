package com.example.pallavi.catchupcontacts;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by pallavi on 5/4/15.
 */
public class phCall{

    String call_id;
    String pNumber;
    String type;
    Date dateTime;
    String duration;
    String contact_id;

    public phCall(){}

    public phCall(String call_id, String pNumber, String type, Date dateTime, String duration, Context context){
        this.call_id = call_id;
        this.pNumber = pNumber;
        this.type = type;
        this.dateTime = dateTime;
        this.duration = duration;
        this.contact_id = getContactIdFromNumber(pNumber, context);
    }

    public String toString(){
        return "\nCall id:--- "+call_id +" \nPhone number:--- "+pNumber+" \nCall Type:--- "+type+" \nDate Time :--- "+dateTime.toString() +  " \n Duration : ---- " + duration + " \n Contact id: --- " + contact_id + "\n";
    }


    public String getContactIdFromNumber(String number, Context ctxt) {
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone._ID};
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(number));
        Cursor c = ctxt.getContentResolver().query(contactUri, projection,
                null, null, null);
        if (c.moveToFirst()) {
            String contactId=c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));
            return contactId;
        }
        return null;
    }

    public static List<phCall> getCallDetails(Context ctxt) {

        StringBuffer sb = new StringBuffer();
        Cursor managedCursor = ctxt.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);
        int id = managedCursor.getColumnIndex( CallLog.Calls._ID );
        int number = managedCursor.getColumnIndex( CallLog.Calls.NUMBER );
        int type = managedCursor.getColumnIndex( CallLog.Calls.TYPE );
        int date = managedCursor.getColumnIndex( CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex( CallLog.Calls.DURATION);
        sb.append( "Call Details :");
        List<phCall> callLogList = new ArrayList<phCall>();
        while ( managedCursor.moveToNext() ) {
            String phNumber = managedCursor.getString( number );
            String callType = managedCursor.getString( type );
            String callDate = managedCursor.getString( date );
            Date callDayTime = new Date(Long.valueOf(callDate));
            String callDuration = managedCursor.getString( duration );
            String callId = managedCursor.getString( id );
            String dir = null;
            int dircode = Integer.parseInt( callType );
            switch( dircode ) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;

                case CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }

            phCall cl = new phCall(callId, phNumber, callType, callDayTime, callDuration, ctxt);
            callLogList.add(cl);

            sb.append( "\nPhone Number:--- "+phNumber +" \nCall Type:--- "+dir+" \nCall Date:--- "+callDayTime+" \nCall duration in sec :--- "+callDuration + "id: " + callId );
            sb.append("\n----------------------------------");
        }
        managedCursor.close();
        return  callLogList;
    }
}
