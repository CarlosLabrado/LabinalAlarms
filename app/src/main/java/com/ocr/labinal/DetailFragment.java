package com.ocr.labinal;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.firebase.client.Firebase;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.ocr.labinal.events.GoToDetailEvent;
import com.ocr.labinal.model.Microlog;
import com.ocr.labinal.model.Temperature;
import com.ocr.labinal.receivers.MessageReceiver;
import com.ocr.labinal.utilities.AndroidBus;
import com.ocr.labinal.utilities.Tools;
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

    private Microlog microlog;

    private String mContactName;
    private String mTelephoneNumber;

    private final String STATUS = "S";

    SmsManager smsManager;

    private boolean isSomethingSelected = false;

    List<Temperature> mTemperatures;

    boolean comesFromReceiver = false;

    private ShowcaseView mShowcaseView;
    private int mShowCaseCounter = 0;


    @Bind(com.ocr.labinal.R.id.textViewContactName)
    TextView textViewContactName;

    @Bind(com.ocr.labinal.R.id.textViewTelephone)
    TextView textViewTelephone;

    @Bind(com.ocr.labinal.R.id.textViewLastKnownTemp)
    TextView textViewLastKnownTemp;

    @Bind(com.ocr.labinal.R.id.textViewStatus)
    TextView textViewStatus;

    @Bind(com.ocr.labinal.R.id.textViewHumidity)
    TextView textViewHumidity;

    @Bind(com.ocr.labinal.R.id.textViewNoInfo)
    TextView textViewNoInfo;

    @Bind(com.ocr.labinal.R.id.textViewLastUpdateDate)
    TextView textViewLastUpdateDate;

    @Bind(com.ocr.labinal.R.id.lastDataContainer)
    LinearLayout lastDataContainer;

    @Bind(com.ocr.labinal.R.id.seekBarThermometer)
    SeekBar seekBarThermometer;

    @Bind(com.ocr.labinal.R.id.buttonUpdateState)
    Button mButtonUpdateSelected;

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

        drawThermometer();

        lastDataContainer.setVisibility(View.GONE);

        Intent intent = getActivity().getIntent();
        comesFromReceiver = intent.getBooleanExtra(Constants.EXTRA_COMES_FROM_RECEIVER, false);
        if (comesFromReceiver) {
            mTelephoneNumber = intent.getStringExtra(MessageReceiver.EXTRA_PHONE_NUMBER);
            microlog = getMicrologByPhoneNumber(mTelephoneNumber);
            mContactName = microlog.name;
        }

        isSomethingSelected = MainActivity.getIsSomethingSelected();
        if (!isSomethingSelected) {
            textViewNoInfo.setText(com.ocr.labinal.R.string.no_microlog_selected);
            mButtonUpdateSelected.setVisibility(View.INVISIBLE);
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
            microlog = event.getMicrolog();
            mTelephoneNumber = microlog.sensorPhoneNumber;
            mContactName = microlog.name;

            textViewTelephone.setText(mTelephoneNumber);
            textViewContactName.setText(mContactName);

            mButtonUpdateSelected.setVisibility(View.VISIBLE);
            getExistingTemperaturesForMain();


        }
    }


    private void getExistingTemperaturesForMain() {
        mTemperatures = getTemperatureList(mTelephoneNumber);
        if (mTemperatures != null && !mTemperatures.isEmpty()) {
            lastDataContainer.setVisibility(View.VISIBLE);
            textViewNoInfo.setVisibility(View.GONE);
            Temperature temperature = mTemperatures.get(mTemperatures.size() - 1);

            textViewLastKnownTemp.setText(String.valueOf(temperature.tempInFahrenheit) + " \u00B0 F");

            seekBarThermometer.setProgress((int) temperature.tempInFahrenheit);

            textViewStatus.setText(temperature.status);
            switch (temperature.status) {
                case "Normal":
                    textViewStatus.setTextColor(getResources().getColor(com.ocr.labinal.R.color.green_700));
                    break;
                case "Atencion":
                    textViewStatus.setTextColor(getResources().getColor(com.ocr.labinal.R.color.yellow_700));
                    break;
                case "Advertencia":
                    textViewStatus.setTextColor(getResources().getColor(com.ocr.labinal.R.color.orange_500));
                    break;
                case "Alarma":
                    textViewStatus.setTextColor(getResources().getColor(com.ocr.labinal.R.color.red_600));
                    break;
            }
            textViewHumidity.setText(String.valueOf(temperature.humidity) + "%");

            Calendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(temperature.timestamp);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.getDefault());
            simpleDateFormat.setCalendar(calendar);

            textViewLastUpdateDate.setText(simpleDateFormat.format(calendar.getTime()));

            if (comesFromReceiver) {
                Firebase myFirebaseRef = new Firebase(getResources().getString(com.ocr.labinal.R.string.firebase_url));
                Calendar c = Calendar.getInstance();
                myFirebaseRef.child(mTelephoneNumber).child(String.valueOf(c.getTimeInMillis())).setValue(temperature);

                // clear the flag
                comesFromReceiver = false;

            }
        } else {
            textViewNoInfo.setVisibility(View.VISIBLE);
            lastDataContainer.setVisibility(View.GONE);
        }
    }

    /**
     * creates a gradient to show the shades from red to green trying to simulate a thermometer,
     * assigns this gradient to the seekBar and prevents it from capturing on touch events
     */
    private void drawThermometer() {
        seekBarThermometer.setClickable(false);
        seekBarThermometer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                Log.d(TAG, "PROGRESS CHANGED " + progress);
                seekBarThermometer.setThumb(writeOnDrawable(com.ocr.labinal.R.drawable.thumb, String.valueOf(progress)));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarThermometer.setFocusable(false);
        seekBarThermometer.setOnTouchListener(new View.OnTouchListener() {
                                                  @Override
                                                  public boolean onTouch(View view, MotionEvent motionEvent) {
                                                      return true;
                                                  }
                                              }
        );
    }

    /**
     * Writes text on the Thumb drawable
     *
     * @param drawableId the resource Id
     * @param text       text to be drawn
     * @return bitmap
     */
    public BitmapDrawable writeOnDrawable(int drawableId, String text) {

        Bitmap bm = BitmapFactory.decodeResource(getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(Tools.fromDpToPx(14));


        Canvas canvas = new Canvas(bm);
        canvas.save();

        int x = (bm.getWidth() - canvas.getWidth()) / 2;
        int y = (bm.getHeight() + canvas.getHeight()) / 2;

        Log.d("X and Y", "x=" + x + " y=" + y);
        canvas.rotate(90f);

        canvas.drawText(text, 5, -y / 4, paint);

        canvas.restore();

        return new BitmapDrawable(getResources(), bm);
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


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (!isSomethingSelected) {
            menu.getItem(1).setEnabled(false);
            menu.getItem(2).setEnabled(false);
            menu.getItem(3).setEnabled(false);
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
        } else if (id == com.ocr.labinal.R.id.action_alerts) {
            Intent intent = new Intent(getActivity(), SetPointsActivity.class);
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
