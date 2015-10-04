package com.example.pallavi.catchupcontacts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ShowContacts extends ActionBarActivity {


    private static final String TAG = "myLogs";
    Map<String, String> mapContacted;
    List<String> listIgnored;
    List<String> listLater;
    Calendar cal = Calendar.getInstance();
    private ImageLoader mImageLoader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_contacts);
        mapContacted = new HashMap<>();
        listIgnored = new ArrayList<>();
        listLater = new ArrayList<>();
        populateContactsList();
        mImageLoader = new ImageLoader(ShowContacts.this, getListPreferredItemHeight()) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return loadContactPhotoThumbnail((String) data, getImageSize());
            }
        };

        // Set a placeholder loading image for the image loader
        mImageLoader.setLoadingImage(R.drawable.ic_contact_picture_holo_light);

        // Add a cache to the image loader
        mImageLoader.addImageCache(ShowContacts.this.getSupportFragmentManager(), 0.1f);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_contacts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.about:
                startActivity(new Intent(this, About.class));
                return true;
            case R.id.help:
                startActivity(new Intent(this, Help.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing CatchUp")
                .setMessage("Are you sure you want to close this application?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String contactedData = Util.readFromFile(Util.fNameContacted, ShowContacts.this);
                        String updatedContactedData = Util.getUpdatedContactedData(contactedData, mapContacted);

                        Util.writeToFile(updatedContactedData, Util.fNameContacted, ShowContacts.this);

                        String ignoredData = Util.readFromFile(Util.fNameIgnored, ShowContacts.this);
                        String updatedIData = Util.getUpdatedLaterOrIgnore(ignoredData, listIgnored);
                        Util.writeToFile(updatedIData, Util.fNameIgnored, ShowContacts.this);

                        String laterData = Util.readFromFile(Util.fNameLater, ShowContacts.this);
                        String updatedLData = Util.getUpdatedLaterOrIgnore(laterData, listLater);
                        Util.writeToFile(updatedLData, Util.fNameLater, ShowContacts.this);

                        finish();
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();

        // In the case onPause() is called during a fling the image loader is
        // un-paused to let any remaining background work complete.
        mImageLoader.setPauseWork(false);
    }

    /**
     * Gets the preferred height for each item in the ListView, in pixels, after accounting for
     * screen density. ImageLoader uses this value to resize thumbnail images to match the ListView
     * item height.
     *
     * @return The preferred height in pixels, based on the current theme.
     */
    private int getListPreferredItemHeight() {
        final TypedValue typedValue = new TypedValue();

        // Resolve list item preferred height theme attribute into typedValue
        ShowContacts.this.getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, typedValue, true);

        // Create a new DisplayMetrics object
        final DisplayMetrics metrics = new DisplayMetrics();

        // Populate the DisplayMetrics
        ShowContacts.this.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // Return theme value based on DisplayMetrics
        return (int) typedValue.getDimension(metrics);
    }

    private void populateContactsList(){

        List<phCall> callLogList = phCall.getCallDetails(ShowContacts.this);
        Log.d(TAG, callLogList.toString());

//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DATE, -2);
//        Date recentDate = cal.getTime();

        List<phContact> phContactsList = phContact.getPriorityContacts(callLogList, ShowContacts.this);
        ContactsListAdapter adapter = new ContactsListAdapter(this, phContactsList);
        ListView listView = (ListView) findViewById(R.id.lvContacts);
        listView.setAdapter(adapter);

    }

    private class ContactsListAdapter extends ArrayAdapter<phContact> {

        List<phContact> contactsList;
        public ContactsListAdapter(Context context, List<phContact> cList) {
            super(context, 0, cList);
            contactsList = cList;
        }

        ArrayList<Integer> trackLaterPos = new ArrayList<>();
        ArrayList<Integer> trackContactedPos = new ArrayList<>();
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            phContact contact = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            final int pos = position;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_contact, parent, false);
            }
            final ViewHolder holder = new ViewHolder();
            // Lookup view for data population
            holder.tvContactName = (TextView) convertView.findViewById(R.id.contactName);
            holder.imgCatchup = (ImageView) convertView.findViewById(R.id.imgCatchUp);
            holder.imgRCatchup = (ImageView) convertView.findViewById(R.id.imgRCatchUp);
            holder.imgLater = (ImageView) convertView.findViewById(R.id.imgLater);
            holder.imgRLater = (ImageView) convertView.findViewById(R.id.imgRLater);
            holder.imgIgnore = (ImageView) convertView.findViewById(R.id.imgIgnore);
            holder.icon = (QuickContactBadge) convertView.findViewById(android.R.id.icon);

            // Populate the data into the template view using the data object
            holder.tvContactName.setText(contact.name);

            holder.imgLater.setTag(holder);
            holder.imgIgnore.setTag(holder);
            holder.imgCatchup.setTag(holder);

            final phContact pc = contactsList.get(pos);

            holder.imgLater.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listLater.add(pc.contact_id);
                    holder.imgRLater.setImageDrawable(ShowContacts.this.getResources().getDrawable(R.drawable.imgdone));
                    trackLaterPos.add(pos);
                    Toast.makeText(ShowContacts.this,
                            pc.name + ", added in reminder list", Toast.LENGTH_LONG).show();
//                    notifyDataSetChanged();
                }
            });

            holder.imgCatchup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mapContacted.put(pc.contact_id, cal.getTime().toString());
                    holder.imgRCatchup.setImageDrawable(ShowContacts.this.getResources().getDrawable(R.drawable.imgdone));
                    trackContactedPos.add(pos);
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("smsto:" + Uri.encode(pc.phone_number)));
                    startActivity(intent);
                    notifyDataSetChanged();
                }
            });

            holder.imgIgnore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listIgnored.add(pc.contact_id);
                    contactsList.remove(pos);
                    notifyDataSetChanged();
                }
            });

            if(trackLaterPos.contains(Integer.valueOf(position))){
                holder.imgRLater.setImageDrawable(ShowContacts.this.getResources().getDrawable(R.drawable.imgdone));
            }else{
                holder.imgRLater.setImageDrawable(null);
            }

            if(trackContactedPos.contains(Integer.valueOf(position))){
                holder.imgRCatchup.setImageDrawable(ShowContacts.this.getResources().getDrawable(R.drawable.imgdone));
            }else{
                holder.imgRCatchup.setImageDrawable(null);
            }

            // Generates the contact lookup Uri
            final Uri contactUri = ContactsContract.Contacts.getLookupUri(
                    Long.parseLong(pc._id),
                    pc.lookup_key);

            // Binds the contact's lookup Uri to the QuickContactBadge
            holder.icon.assignContactUri(contactUri);

            // Loads the thumbnail image pointed to by photoUri into the QuickContactBadge in a
            // background worker thread
            mImageLoader.loadImage(pc.photo_uri, holder.icon);

//            Animation animationY = new TranslateAnimation(0, 0, parent.getHeight()/4, 0);
//            animationY.setDuration(1000);
//            convertView.startAnimation(animationY);
//            animationY = null;
            // Return the completed view to render on screen
            return convertView;
        }

        private class ViewHolder {
            TextView tvContactName;
            ImageView imgCatchup;
            ImageView imgRCatchup;
            ImageView imgLater;
            ImageView imgRLater;
            ImageView imgIgnore;

            QuickContactBadge icon;
        }
    }

    private void getSMS(){
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                String msgData = "";
                for(int idx=0;idx<cursor.getColumnCount();idx++)
                {
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                }
                // use msgData
            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }
    }

    private Bitmap loadContactPhotoThumbnail(String photoData, int imageSize) {

        // Ensures the Fragment is still added to an activity. As this method is called in a
        // background thread, there's the possibility the Fragment is no longer attached and
        // added to an activity. If so, no need to spend resources loading the contact photo.
        if (ShowContacts.this == null) {
            return null;
        }

        // Instantiates an AssetFileDescriptor. Given a content Uri pointing to an image file, the
        // ContentResolver can return an AssetFileDescriptor for the file.
        AssetFileDescriptor afd = null;

        // This "try" block catches an Exception if the file descriptor returned from the Contacts
        // Provider doesn't point to an existing file.
        try {
            Uri thumbUri;
            // If Android 3.0 or later, converts the Uri passed as a string to a Uri object.
            if (Util.hasHoneycomb()) {
                thumbUri = Uri.parse(photoData);
            } else {
                // For versions prior to Android 3.0, appends the string argument to the content
                // Uri for the Contacts table.
                final Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, photoData);

                // Appends the content Uri for the Contacts.Photo table to the previously
                // constructed contact Uri to yield a content URI for the thumbnail image
                thumbUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
            }
            // Retrieves a file descriptor from the Contacts Provider. To learn more about this
            // feature, read the reference documentation for
            // ContentResolver#openAssetFileDescriptor.
            afd = ShowContacts.this.getContentResolver().openAssetFileDescriptor(thumbUri, "r");

            // Gets a FileDescriptor from the AssetFileDescriptor. A BitmapFactory object can
            // decode the contents of a file pointed to by a FileDescriptor into a Bitmap.
            FileDescriptor fileDescriptor = afd.getFileDescriptor();

            if (fileDescriptor != null) {
                // Decodes a Bitmap from the image pointed to by the FileDescriptor, and scales it
                // to the specified width and height
                return ImageLoader.decodeSampledBitmapFromDescriptor(
                        fileDescriptor, imageSize, imageSize);
            }
        } catch (FileNotFoundException e) {
            // If the file pointed to by the thumbnail URI doesn't exist, or the file can't be
            // opened in "read" mode, ContentResolver.openAssetFileDescriptor throws a
            // FileNotFoundException.
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Contact photo thumbnail not found for contact " + photoData
                        + ": " + e.toString());
            }
        } finally {
            // If an AssetFileDescriptor was returned, try to close it
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                    // Closing a file descriptor might cause an IOException if the file is
                    // already closed. Nothing extra is needed to handle this.
                }
            }
        }

        // If the decoding failed, returns null
        return null;
    }

}
