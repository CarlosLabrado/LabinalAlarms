package com.ocr.labinal;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.ocr.labinal.utilities.AndroidBus;
import com.squareup.otto.Bus;

import butterknife.Bind;


public class SplashActivity extends AppCompatActivity {

    private final String TAG = SplashActivity.class.getSimpleName();

    public static Bus bus;

    private Handler mHandler;


    @Bind(com.ocr.labinal.R.id.newContactContainer)
    LinearLayout newContactContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ocr.labinal.R.layout.activity_splash);

//        ButterKnife.bind(this);

//        newContactContainer.setVisibility(View.GONE);

        bus = new AndroidBus();
        bus.register(this);

        mHandler = new Handler();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startMainActivity();
            }
        }, 1000);



    }

//    /**
//     * comes from the custom adapter, what we want is the element id to pass into the next activity
//     *
//     * @param event
//     */
//    @Subscribe
//    public void sensorClicked(final SensorClickedEvent event) {
//        if (event.getResultCode() == 1) {
//            if (event.isDelete()) {
//                new AlertDialog.Builder(this)
//                        .setTitle(getString(R.string.dialog_title_delete))
//                        .setMessage(getString(R.string.dialog_message_confirm_delete))
//                        .setCancelable(false)
//                        .setPositiveButton(getString(R.string.dialog_yes_delete), new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                deleteMicrolog(event.getElementId());
//                            }
//                        })
//                        .setNegativeButton(getString(R.string.dialog_no), null)
//                        .show();
//            } else {
//                Intent intent = new Intent(this, MainActivity.class);
//                intent.putExtra(Constants.EXTRA_COMES_FROM_RECEIVER, false);
//                intent.putExtra(Constants.EXTRA_SELECTED_ID, event.getElementId());
//                startActivity(intent);
//            }
//        }
//
//    }










    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
