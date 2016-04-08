package com.ocr.labinal;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.maps.model.LatLng;
import com.ocr.labinal.events.EditNameEvent;
import com.ocr.labinal.events.GoToDetailEvent;
import com.ocr.labinal.model.Microlog;
import com.ocr.labinal.model.PlantEvent;
import com.ocr.labinal.model.Temperature;
import com.ocr.labinal.receivers.MessageReceiver;
import com.ocr.labinal.utilities.AndroidBus;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * create an instance of this fragment.
 */
public class DetailFragment extends Fragment {


    private final String TAG = MainActivity.class.getSimpleName();

    public static Bus bus;

    private Microlog mMicrolog;

    private String mContactName;
    private String mTelephoneNumber;

    private final String STATUS = "S";

    SmsManager smsManager;

    private boolean isSomethingSelected = false;

    List<Temperature> mTemperatures;
    List<PlantEvent> mEvents;

    boolean comesFromReceiver = false;

    private ShowcaseView mShowcaseView;
    private int mShowCaseCounter = 0;

    private LatLng mLatLng;

    @Bind(com.ocr.labinal.R.id.textViewContactName)
    TextView textViewContactName;

    @Bind(com.ocr.labinal.R.id.textViewTelephone)
    TextView textViewTelephone;
    @Bind(R.id.textViewState)
    TextView mTextViewState;
    @Bind(R.id.textViewGenerator)
    TextView mTextViewGenerator;
    @Bind(R.id.textViewBattery)
    TextView mTextViewBattery;
    @Bind(R.id.textViewMinutesOnBattery)
    TextView mTextViewMinutesOnBattery;
    @Bind(R.id.textViewCFE)
    TextView mTextViewCFE;
    @Bind(R.id.textViewGEN)
    TextView mTextViewGEN;
    @Bind(R.id.textViewPROD)
    TextView mTextViewPROD;
    @Bind(R.id.textViewTime)
    TextView mTextViewTime;

    @Bind(com.ocr.labinal.R.id.textViewNoInfo)
    TextView textViewNoInfo;

    @Bind(com.ocr.labinal.R.id.textViewLastUpdateDate)
    TextView textViewLastUpdateDate;

    @Bind(com.ocr.labinal.R.id.lastDataContainer)
    LinearLayout lastDataContainer;

    @Bind(com.ocr.labinal.R.id.buttonUpdateState)
    Button mButtonUpdateSelected;
    @Bind(R.id.fab_edit)
    FloatingActionButton mFabEdit;


    @OnClick(com.ocr.labinal.R.id.buttonUpdateState)
    public void buttonClicked() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(com.ocr.labinal.R.string.dialog_title_state))
                .setMessage(getString(com.ocr.labinal.R.string.dialog_message_confirm_sms))
                .setCancelable(false)
                .setPositiveButton(getString(com.ocr.labinal.R.string.dialog_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getUpdatedSensorInfo(mTelephoneNumber);
                    }
                })
                .setNegativeButton(getString(com.ocr.labinal.R.string.dialog_no), null)
                .show();
    }

    public DetailFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(com.ocr.labinal.R.layout.fragment_detail, container, false);

        bus = new AndroidBus();
        bus.register(this);

        ButterKnife.bind(this, view);

        mFabEdit.hide();

        //drawThermometer();

        lastDataContainer.setVisibility(View.GONE);

        Intent intent = getActivity().getIntent();
        comesFromReceiver = intent.getBooleanExtra(Constants.EXTRA_COMES_FROM_RECEIVER, false);
        if (comesFromReceiver) {
            mTelephoneNumber = intent.getStringExtra(MessageReceiver.EXTRA_PHONE_NUMBER);
            mMicrolog = getMicrologByPhoneNumber(mTelephoneNumber);
            mContactName = mMicrolog.getName();
        }

        isSomethingSelected = MainActivity.getIsSomethingSelected();
        // if there is nothing selected we don't want to show the detail or the edit button
        if (!isSomethingSelected) {
            textViewNoInfo.setText(com.ocr.labinal.R.string.no_microlog_selected);
            mButtonUpdateSelected.setVisibility(View.INVISIBLE);
        } else {
            mFabEdit.show();
        }


        smsManager = SmsManager.getDefault();

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * In here we define what happens when we click a microlog from the recycler view
     *
     * @param event
     */
    @Subscribe
    public void micrologClicked(GoToDetailEvent event) {
        if (event != null) {
            isSomethingSelected = true;
            mFabEdit.show();
            mMicrolog = event.getMicrolog();
            mTelephoneNumber = mMicrolog.getSensorPhoneNumber();
            mContactName = mMicrolog.getName();

            textViewTelephone.setText(mTelephoneNumber);
            textViewContactName.setText(mContactName);

            mButtonUpdateSelected.setVisibility(View.VISIBLE);
            getExistingEventsForCurrent();
            //getExistingTemperaturesForMain();
        }
    }

    private void getExistingEventsForCurrent() {
        mEvents = getPlantEventList(mTelephoneNumber);
        if (mEvents != null && !mEvents.isEmpty()) {
            lastDataContainer.setVisibility(View.VISIBLE);
            textViewNoInfo.setVisibility(View.GONE);

            PlantEvent plantEvent = mEvents.get(mEvents.size() - 1);

            /**
             * Plant failure is a production failure
             */
            if (plantEvent.isPlantFailure()) {
                mTextViewState.setTextColor(getResources().getColor(com.ocr.labinal.R.color.red_600));
                mTextViewState.setText("Falla");

                mTextViewPROD.setTextColor(getResources().getColor(com.ocr.labinal.R.color.red_600));
                mTextViewPROD.setText(R.string.state_off);
//            } else if (plantEvent.getState().equalsIgnoreCase("alarma")) {
//                mTextViewState.setTextColor(getResources().getColor(com.ocr.labinal.R.color.red_600));
//                mTextViewState.setText(plantEvent.getState());
            } else {
                mTextViewState.setTextColor(getResources().getColor(com.ocr.labinal.R.color.green_700));
                mTextViewState.setText(plantEvent.getState());

                mTextViewPROD.setTextColor(getResources().getColor(com.ocr.labinal.R.color.green_700));
                mTextViewPROD.setText(R.string.state_on);
            }


            /**
             * Power origin can be CFE, generador, bateria
             */
            if (plantEvent.getPowerOrigin().equalsIgnoreCase("CFE")) {
                mTextViewCFE.setTextColor(getResources().getColor(com.ocr.labinal.R.color.green_700));
                mTextViewCFE.setText(R.string.state_on);

                mTextViewGEN.setTextColor(getResources().getColor(com.ocr.labinal.R.color.red_600));
                mTextViewGEN.setText(R.string.state_off);
            } else if (plantEvent.getPowerOrigin().equalsIgnoreCase("generador") || plantEvent.getPowerOrigin().equalsIgnoreCase("bateria")) {
                mTextViewGEN.setTextColor(getResources().getColor(com.ocr.labinal.R.color.green_700));
                mTextViewGEN.setText(R.string.state_on);

                mTextViewCFE.setTextColor(getResources().getColor(com.ocr.labinal.R.color.red_600));
                mTextViewCFE.setText(R.string.state_off);
            } else if (plantEvent.getPowerOrigin().equalsIgnoreCase("CFE Generador")) { // both on
                mTextViewCFE.setTextColor(getResources().getColor(com.ocr.labinal.R.color.green_700));
                mTextViewCFE.setText(R.string.state_on);

                mTextViewGEN.setTextColor(getResources().getColor(com.ocr.labinal.R.color.green_700));
                mTextViewGEN.setText(R.string.state_on);
            }

//            if (plantEvent.getState().equalsIgnoreCase(""))
//
//                mTextViewGenerator.setText(plantEvent.getPowerOrigin());
//            mTextViewBattery.setText(plantEvent.getUpsState());

            String secondsString;
            if (plantEvent.getMinutesOnTransfer() > 0) {
                secondsString = String.valueOf(plantEvent.getMinutesOnTransfer() * 60) + "seg on Transfer";
            } else if (plantEvent.getMinutesOnBattery() > 0) {
                secondsString = String.valueOf(plantEvent.getMinutesOnBattery() * 60) + "seg on Battery";
            } else {
                secondsString = "0";
            }

            mTextViewMinutesOnBattery.setText(secondsString);

            Calendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(plantEvent.getTimeInMillis());

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.getDefault());
            simpleDateFormat.setCalendar(calendar);

            textViewLastUpdateDate.setText(simpleDateFormat.format(calendar.getTime()));

            if (comesFromReceiver) {
//                Firebase myFirebaseRef = new Firebase(getResources().getString(com.ocr.labinal.R.string.firebase_url));
//                Calendar c = Calendar.getInstance();
//                myFirebaseRef.child(mTelephoneNumber).child(String.valueOf(c.getTimeInMillis())).setValue(plantEvent);

                // clear the flag
                comesFromReceiver = false;

            }

            plantEvent.getMicrologId();
            plantEvent.getState();
            plantEvent.getPowerOrigin();
            plantEvent.getMinutesOnBattery();
            plantEvent.getMinutesOnTransfer();
            plantEvent.getTimeInMillis();
            plantEvent.isPlantFailure();


        } else {
            textViewNoInfo.setVisibility(View.VISIBLE);
            textViewNoInfo.setText(getResources().getString(R.string.main_no_info));
            lastDataContainer.setVisibility(View.GONE);
        }
    }

    private List<PlantEvent> getPlantEventList(String telephoneNumber) {
        return new Select().from(PlantEvent.class).where("sensorPhoneNumber = ?", telephoneNumber).orderBy("timeInMillis ASC").execute();
    }

    public void getUpdatedSensorInfo(String telephoneNumber) {
        try {
//            Log.d("SMS send", "sending message to " + telephoneNumber);
            smsManager.sendTextMessage(telephoneNumber, null, STATUS, null, null);
            Toast.makeText(getActivity(), getString(com.ocr.labinal.R.string.toast_message_send), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getActivity(), getString(com.ocr.labinal.R.string.toast_message_not_send), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    public Microlog getMicrologByPhoneNumber(String phoneNumber) {
        return new Select().from(Microlog.class).where("sensorPhoneNumber = ?", phoneNumber).executeSingle();
    }

    public List<Temperature> getTemperatureList(String telephoneNumber) {
        return new Select().from(Temperature.class).where("sensorPhoneNumber = ?", telephoneNumber).orderBy("timestamp ASC").execute();
    }

    @Subscribe
    public void updateLatLng(LatLng latLng) {
        mLatLng = latLng;
    }

    @OnClick(R.id.fab_edit)
    public void buttonEditClicked() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit, null);

        final EditText editTextName = (EditText) view.findViewById(R.id.editTextDialogEditName);
        final EditText editTextLat = (EditText) view.findViewById(R.id.editTextDialogEditLat);
        final EditText editTextLon = (EditText) view.findViewById(R.id.editTextDialogEditLon);

        ImageButton imageButtonActualCords = (ImageButton) view.findViewById(R.id.imageButtonActualCords);
        imageButtonActualCords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLatLng != null) {
                    editTextLat.setText(String.valueOf(mLatLng.latitude));
                    editTextLon.setText(String.valueOf(mLatLng.longitude));
                }
            }
        });

        if (mMicrolog != null) {
            editTextName.setText(mMicrolog.getName());
            editTextLat.setText(String.valueOf(mMicrolog.getLatitude()));
            editTextLon.setText(String.valueOf(mMicrolog.getLongitude()));
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setView(view);

        builder.setTitle(R.string.dialog_edit_sensor_title);
        builder.setPositiveButton(R.string.dialog_update_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!editTextName.getText().toString().isEmpty()) {
                    mMicrolog.setName(editTextName.getText().toString());
                    mMicrolog.setLatitude(Double.parseDouble(editTextLat.getText().toString()));
                    mMicrolog.setLongitude(Double.parseDouble(editTextLon.getText().toString()));
                    mMicrolog.save();

                    mTelephoneNumber = mMicrolog.getSensorPhoneNumber();
                    mContactName = mMicrolog.getName();

                    textViewTelephone.setText(mTelephoneNumber);
                    textViewContactName.setText(mContactName);

                    MapAndListFragment.mapBus.post(new EditNameEvent());
                }
            }
        });
        builder.setNegativeButton(R.string.dialog_update_negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();

    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (!isSomethingSelected) {
            menu.getItem(1).setEnabled(false);
            menu.getItem(2).setEnabled(false);
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(com.ocr.labinal.R.menu.menu_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == com.ocr.labinal.R.id.action_telephones) {
            Intent intent = new Intent(getActivity(), TelephoneChangeActivity.class);
            intent.putExtra(Constants.EXTRA_TELEPHONE_NUMBER, mTelephoneNumber);
            startActivity(intent);
        } else if (id == com.ocr.labinal.R.id.action_history) {
            Intent intent = new Intent(getActivity(), HistoryActivity.class);
            intent.putExtra(Constants.EXTRA_TELEPHONE_NUMBER, mTelephoneNumber);
            startActivity(intent);
        } else if (id == com.ocr.labinal.R.id.action_detail_help) {
            showShowcaseHelp();
        }

        return super.onOptionsItemSelected(item);
    }


    private void showShowcaseHelp() {
        // this is to put the button on the right
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, margin);

        mShowcaseView = new ShowcaseView.Builder(getActivity())
                .setTarget(new ViewTarget(getActivity().findViewById(com.ocr.labinal.R.id.buttonUpdateState)))
                .setContentText(getString(com.ocr.labinal.R.string.help_detail_update))
                .setStyle(com.ocr.labinal.R.style.CustomShowcaseTheme4)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {


                        switch (mShowCaseCounter) {
                            case 0:
                                mShowcaseView.setShowcase(new ViewTarget(getActivity().findViewById(com.ocr.labinal.R.id.lastDataContainer)), true);
                                mShowcaseView.setContentText(getString(com.ocr.labinal.R.string.help_detail_container));

                                break;
                            case 1:
                                mShowcaseView.hide();
//                setAlpha(1.0f, textView1, textView2, textView3);
                                mShowCaseCounter = -1;
                                break;
                        }
                        mShowCaseCounter++;
                    }
                })
                .build();
        mShowcaseView.setButtonText(getString(com.ocr.labinal.R.string.next));
        mShowcaseView.setHideOnTouchOutside(true);
        mShowcaseView.setButtonPosition(lps);

    }


}
