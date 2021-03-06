package com.ocr.labinal;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.ocr.labinal.events.GoToDetailEvent;
import com.ocr.labinal.events.MarkerClickedEvent;
import com.ocr.labinal.events.RefreshMicrologsEvent;
import com.ocr.labinal.events.SelectContactFromPhoneEvent;
import com.ocr.labinal.events.SensorClickedEvent;
import com.ocr.labinal.model.Microlog;
import com.ocr.labinal.model.PlantEvent;
import com.ocr.labinal.model.Temperature;
import com.ocr.labinal.receivers.MessageReceiver;
import com.ocr.labinal.utilities.AndroidBus;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final String TAG = MainActivity.class.getSimpleName();


    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private LatLng mLatLng;


    public static Bus bus;

    private int selectedId;
    private Microlog microlog;

    private String mContactName;
    private String mTelephoneNumber;

    private final String STATUS = "S";

    SmsManager smsManager;

    public boolean isFirstRun = true;

    boolean comesFromReceiver = false;

    @BindView(com.ocr.labinal.R.id.toolbar)
    Toolbar toolbar;

    public static Boolean getIsSomethingSelected() {
        return isSomethingSelected;
    }

    private static Boolean isSomethingSelected;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ocr.labinal.R.layout.activity_main);

        ButterKnife.bind(this);

        SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(this);
        isFirstRun = settings.getBoolean(Constants.SP_IS_FIRST_TIME, true);

        if (isFirstRun) {
//            createTheHardcodedMicrologs();
            isFirstRun = false;
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(Constants.SP_IS_FIRST_TIME, isFirstRun);
            editor.commit();
        }

        bus = new AndroidBus();
        bus.register(this);

        isSomethingSelected = false;

        /** toolBar **/
        setUpToolBar();

        checkForLocationServicesEnabled();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds


        Intent intent = getIntent();
        comesFromReceiver = intent.getBooleanExtra(Constants.EXTRA_COMES_FROM_RECEIVER, false);
        if (comesFromReceiver) {
            mTelephoneNumber = intent.getStringExtra(MessageReceiver.EXTRA_PHONE_NUMBER);
            microlog = getMicrologByPhoneNumber(mTelephoneNumber);
            mContactName = microlog.name;


            new SearchForSMSHistory().execute(mTelephoneNumber);

        } else {
//            selectedId = intent.getIntExtra(Constants.EXTRA_SELECTED_ID, 0);
//            microlog = getMicrologs().get(selectedId);
//            mTelephoneNumber = microlog.sensorPhoneNumber;
//            mContactName = microlog.name;
        }


        bus = new AndroidBus();
        bus.register(this);

        smsManager = SmsManager.getDefault();


    }

    /**
     * Only runs the first time
     */
    private void createTheHardcodedMicrologs() {
        Microlog microlog = new Microlog(
                "6141849010",
                "01",
                "Planta 1",
                "Desconocido",
                3, 28.6522408, -106.1279873
        );
        microlog.save();
        Microlog microlog2 = new Microlog(
                "6141846841",
                "02",
                "Planta 2",
                "Desconocido",
                3, 28.7118695, -106.1136756
        );
        microlog2.save();
    }

    /**
     * checks for the GPS to be enabled
     */
    private void checkForLocationServicesEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(this);
            dialog.setTitle(getResources().getString(com.ocr.labinal.R.string.gps_network_not_enabled));
            dialog.setMessage(getResources().getString(com.ocr.labinal.R.string.gps_network_not_enabled_message));
            dialog.setPositiveButton(getResources().getString(com.ocr.labinal.R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(getString(com.ocr.labinal.R.string.dialog_cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                }
            });
            dialog.show();
        }
    }


    /**
     * A Microlog is kind of the main object of this app, from this we get temperatures
     *
     * @return a sensor list
     */
    public List<Microlog> getMicrologs() {
        return new Select().from(Microlog.class).execute();
    }

    public Microlog getMicrologByPhoneNumber(String phoneNumber) {
        return new Select().from(Microlog.class).where("sensorPhoneNumber = ?", phoneNumber).executeSingle();
    }


    /**
     * sets up the top bar
     */
    private void setUpToolBar() {
        setSupportActionBar(toolbar);
        setActionBarTitle(getString(com.ocr.labinal.R.string.app_name), null, false);
        getSupportActionBar().setElevation(0f);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        // enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /**
     * Gets called from the fragments onResume and its because only the first doesn't have the up
     * button on the actionBar
     *
     * @param title          The title to show on the ActionBar
     * @param subtitle       The subtitle to show on the ActionBar
     * @param showNavigateUp if true, shows the up button
     */
    public void setActionBarTitle(String title, String subtitle, boolean showNavigateUp) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            if (subtitle != null) {
                getSupportActionBar().setSubtitle(subtitle);
            } else {
                getSupportActionBar().setSubtitle(null);
            }
            if (showNavigateUp) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Location services connected.");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Log.d(TAG, "onConnected requesting Loc");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.d(TAG, "onConnected has last location");
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged location changed");
        handleNewLocation(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    /**
     * gets the location and then asks Map fragment to update it
     *
     * @param location current loc
     */
    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        mLatLng = new LatLng(currentLatitude, currentLongitude);

        Log.e(TAG, "ACCURACY " + String.valueOf(location.getAccuracy()));

        MapAndListFragment.mapBus.post(mLatLng);
        DetailFragment.bus.post(mLatLng);

    }

    /**
     * comes from the {@link MapAndListFragment#setUpMap()}
     *
     * @param event
     */
    @Subscribe
    public void MarkerClicked(MarkerClickedEvent event) {
        if (event != null && !event.getPhoneNumber().isEmpty()) {
            microlog = getMicrologByPhoneNumber(event.getPhoneNumber());
            mTelephoneNumber = microlog.getSensorPhoneNumber();
            mContactName = microlog.getName();
            isSomethingSelected = true;

            new SearchForSMSHistory().execute(mTelephoneNumber);
        }
    }

    /**
     * comes from the custom adapter, what we want is the element id to pass into the next activity
     * {@link com.ocr.labinal.custom.recyclerView.CustomAdapter.ViewHolder#ViewHolder(View)}
     *
     * @param event
     */
    @Subscribe
    public void RecyclerItemClicked(final SensorClickedEvent event) {
        if (event.getResultCode() == 1) {
            if (event.isDelete()) {
                isSomethingSelected = false;
                new AlertDialog.Builder(this)
                        .setTitle(getString(com.ocr.labinal.R.string.dialog_title_delete))
                        .setMessage(getString(com.ocr.labinal.R.string.dialog_message_confirm_delete))
                        .setCancelable(false)
                        .setPositiveButton(getString(com.ocr.labinal.R.string.dialog_yes_delete), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Event on MapFragment
                                MapAndListFragment.mapBus.post(event);
                            }
                        })
                        .setNegativeButton(getString(com.ocr.labinal.R.string.dialog_no), null)
                        .show();
            } else {
                isSomethingSelected = true;
                selectedId = event.getElementId();
                microlog = getMicrologs().get(selectedId);
                if (microlog == null) {
                    Log.d("MainActivity", "why is this null?");
                } else {
                    Log.d("MainActivity", microlog + microlog.name);

                }
                mTelephoneNumber = microlog.getSensorPhoneNumber();
                mContactName = microlog.getName();

                new SearchForSMSHistory().execute(mTelephoneNumber);
            }
        }
    }

    /**
     * AsyncTask for getting the sms history from the phone and get it into a local database
     */
    private class SearchForSMSHistory extends AsyncTask<String, Void, Void> {

        boolean micrologIdSaved = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            String telephoneToSearch = strings[0];


            Uri uri = Uri.parse("content://sms");
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            eraseEventsFromLocalDB();

            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    if (number.length() > 10) {
                        number = number.substring(number.length() - 10);
                    }
                    if (number.equalsIgnoreCase(telephoneToSearch)) {
                        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                        String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                        String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                        if (body.contains("LABINAL") && type.equalsIgnoreCase("1")) {
                            formatSMSMessage(telephoneToSearch, body, date);
                        }
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return null;
        }

        private void formatSMSMessage(String telephoneToSearch, String sms, String date) {
            String[] splitMessage = sms.split("/");

            try {
                if (!sms.contains("PRUEBA")) { //if is not a test
                    String micrologId = splitMessage[0].substring(splitMessage[0].lastIndexOf("PLANTA") + 7, 17);
                    String state = splitMessage[0].substring(splitMessage[0].length() - 3); // NOR or ALA
                    String stateFull = "";
                    if (state.equalsIgnoreCase("NOR")) {
                        stateFull = "Normal";
                    } else {
                        stateFull = "Alarma";
                    }
                    String origin = splitMessage[1].substring(splitMessage[1].length() - 3); // CFE, BAT, GEN
                    String originFull = "";
                    if (origin.equalsIgnoreCase("CFE")) {
                        originFull = "CFE";
                    } else if (origin.equalsIgnoreCase("BAT")) {
                        originFull = "Bateria";
                    } else {
                        originFull = "Generador";
                    }
                    int minutesOnBattery = 0;
                    int minutesOnTransfer = 0;
                    boolean plantFailure = false;
                    String upsState = "";
                    if (splitMessage[2].contains("GEN")) { // if is GEN we have to search on index 3 instead of 2
                        originFull = originFull + " Generador";
                        upsState = splitMessage[3].substring(splitMessage[3].length() - 3); // NOR, ALA

                        if (origin.equalsIgnoreCase("BAT")) {
                            String minutesOnBatteryString = splitMessage[3].substring(splitMessage[3].lastIndexOf("MIN") + 6, 20);
                            minutesOnBattery = Integer.parseInt(minutesOnBatteryString);
                            if (splitMessage[3].contains("FALLA")) {
                                plantFailure = true;
                            }
                        } else if (origin.equalsIgnoreCase("GEN")) {
                            String minutesOnTransferString = splitMessage[3].substring(splitMessage[3].lastIndexOf("MIN") + 6, 27);
                            minutesOnTransfer = Integer.parseInt(minutesOnTransferString);
                        }
                    } else {
                        upsState = splitMessage[2].substring(splitMessage[2].length() - 3); // NOR, ALA

                        if (origin.equalsIgnoreCase("BAT")) {
                            String minutesOnBatteryString = splitMessage[2].substring(splitMessage[2].lastIndexOf("MIN") + 6, 20);
                            minutesOnBattery = Integer.parseInt(minutesOnBatteryString);
                            if (splitMessage[2].contains("FALLA")) {
                                plantFailure = true;
                            }
                        } else if (origin.equalsIgnoreCase("GEN")) {
                            String minutesOnTransferString = splitMessage[2].substring(splitMessage[2].lastIndexOf("MIN") + 6, 27);
                            minutesOnTransfer = Integer.parseInt(minutesOnTransferString);
                        }
                    }

                    String upsStateFull = "";
                    if (upsState.equalsIgnoreCase("NOR")) {
                        upsStateFull = "Normal";
                    } else {
                        upsStateFull = "Alarma";
                    }
                    if (!micrologIdSaved) { // we just want to do this once
                        updateMicrologID(telephoneToSearch, micrologId, stateFull);
                    }
                    saveEvent(date, micrologId, stateFull, originFull, upsStateFull, minutesOnBattery, minutesOnTransfer, plantFailure, telephoneToSearch);
                }

            } catch (NumberFormatException e) {
                e.printStackTrace();
//                Log.e(TAG, "SMS must not be well formatted");
            }
        }

        /**
         * Saves an event to the database
         */
        private void saveEvent(String dateInMillisString, String micrologId, String stateFull, String originFull, String upsState, int minutesOnBattery, int minutesOnTransfer, boolean plantFailure, String sensorPhoneNumber) {
            ActiveAndroid.beginTransaction();
            try {
                PlantEvent plantEvent = new PlantEvent(
                        sensorPhoneNumber,
                        micrologId,
                        stateFull,
                        originFull,
                        upsState,
                        minutesOnBattery,
                        minutesOnTransfer,
                        plantFailure,
                        Long.parseLong(dateInMillisString)
                );
                plantEvent.save();
                ActiveAndroid.setTransactionSuccessful();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } finally {
                ActiveAndroid.endTransaction();
            }
        }

        private void saveTemperature(String micrologId, String stateString, String temperatureString, String relativeHumidityString, String temporalAddress, long date) {
            ActiveAndroid.beginTransaction();
            try {
                Temperature temperature = new Temperature(
                        temporalAddress,
                        micrologId,
                        stateString,
                        Double.parseDouble(temperatureString),
                        Double.parseDouble(relativeHumidityString),
                        date
                );
                temperature.save();
                ActiveAndroid.setTransactionSuccessful();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } finally {
                ActiveAndroid.endTransaction();
            }
        }

        /**
         * Database erase
         * Erases the db so we don't have to check if the reading already exists and don't put duplicates
         */
        private void eraseEventsFromLocalDB() {
            List<PlantEvent> eventList = new Select().from(PlantEvent.class).execute();
            if (eventList != null && eventList.size() > 0) {
                ActiveAndroid.beginTransaction();
                try {
                    new Delete().from(PlantEvent.class).execute();
                    ActiveAndroid.setTransactionSuccessful();
                } catch (Exception e) {
//                    Logger.e(e, "error deleting existing db");
                } finally {
                    ActiveAndroid.endTransaction();
                }
            }

        }

        /**
         * We only want to update the id and the state here
         *
         * @param phoneNumber self
         * @param micrologId  the id that we just got
         * @param stateString last reported state
         */
        private void updateMicrologID(String phoneNumber, String micrologId, String stateString) {
            Microlog microlog = new Select().
                    from(Microlog.class).
                    where("sensorPhoneNumber = ?", phoneNumber).
                    executeSingle();

            if (microlog != null) {
                microlog.sensorId = micrologId;
                microlog.lastState = stateString;
                microlog.save();
                micrologIdSaved = true;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            DetailFragment.bus.post(new GoToDetailEvent(microlog));
            TabFragment.bus.post(new GoToDetailEvent(microlog));
        }
    }

    @Subscribe
    public void startSelectContactFromPhoneIntent(SelectContactFromPhoneEvent event) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, Constants.ACTIVITY_RESULT_CONTACT);
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (reqCode == Constants.ACTIVITY_RESULT_CONTACT) {
            try {
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor cur = managedQuery(contactData, null, null, null, null);
                    ContentResolver contact_resolver = getContentResolver();

                    if (cur.moveToFirst()) {
                        String id = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String name;
                        String no;

                        Cursor phoneCur = contact_resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

                        if (phoneCur != null && phoneCur.moveToFirst()) {
                            name = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            no = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            if (no.length() > 10) {
                                no = no.substring(no.length() - 10);
                            }
                            Microlog microlog = new Microlog(
                                    no,
                                    null,
                                    name,
                                    null,
                                    3, 0, 0
                            );
                            microlog.save();
                            refreshMicrologsRecyclerView();
                        }


                        if (phoneCur != null) {
                            phoneCur.close();
                        }

//                        Log.e("Name and phone number", name + " : " + no);
                    }
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
//                Log.e(TAG, e.toString());
            }
        }

    }

    private void refreshMicrologsRecyclerView() {
        MapAndListFragment.mapBus.post(new RefreshMicrologsEvent());
        if (comesFromReceiver) {
            DetailFragment.bus.post(new GoToDetailEvent(microlog));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

    }
}
