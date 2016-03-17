package com.ocr.labinal.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.activeandroid.query.Select;
import com.ocr.labinal.Constants;
import com.ocr.labinal.MainActivity;
import com.ocr.labinal.SetPointsActivity;
import com.ocr.labinal.TelephoneChangeActivity;
import com.ocr.labinal.model.Microlog;
import com.ocr.labinal.model.PlantEvent;
import com.ocr.labinal.model.SetPoint;
import com.ocr.labinal.model.Telephone;

/**
 * Receiver that listens to SMS and then formats the result and saves into our database
 */
public class MessageReceiver extends BroadcastReceiver {
    private final String TAG = MessageReceiver.class.getSimpleName();
    private Context mContext;

    public static final String EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle pudsBundle = intent.getExtras();
        Object[] pdus = (Object[]) pudsBundle.get("pdus");
        SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
        mContext = context;
//        Log.i(TAG, messages.getMessageBody());
        if (messages.getMessageBody().contains("LABINAL")) {
//            Log.i(TAG, "tried to abort");
            abortBroadcast();
            String phoneNumber = formatMessage(messages);
            Intent newIntent = new Intent(context, MainActivity.class);
            //startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("01S?2")) {
//            Log.i(TAG, "SetPoints received");
            String phoneNumber = formatMessageSetPoints(messages);
            Intent newIntent = new Intent(context, SetPointsActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("T2")) { // these are the immediate response when you change a phone
//            Log.i(TAG, "Telephone 1 Received");
            String phoneNumber = formatMessageTelephoneNumberSingle(messages, 4, 0);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("T3")) {
//            Log.i(TAG, "Telephone 2 Received");
            String phoneNumber = formatMessageTelephoneNumberSingle(messages, 4, 1);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("T4")) {
//            Log.i(TAG, "Telephone 3 Received");
            String phoneNumber = formatMessageTelephoneNumberSingle(messages, 4, 2);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("T?2")) { // these are the normal consult phone response
//            Log.i(TAG, "Telephone 1 Received");
            String phoneNumber = formatMessageTelephoneNumberSingle(messages, 5, 0);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("T?3")) {
//            Log.i(TAG, "Telephone 2 Received");
            String phoneNumber = formatMessageTelephoneNumberSingle(messages, 5, 1);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("T?4")) {
//            Log.i(TAG, "Telephone 3 Received");
            String phoneNumber = formatMessageTelephoneNumberSingle(messages, 5, 2);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("S#01")) {
//            Log.i(TAG, "report to 1 telephone");
            String phoneNumber = formatMessageNumberOfTelephonesToReport(messages, 1);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("S#02")) {
//            Log.i(TAG, "report to 1 telephone");
            String phoneNumber = formatMessageNumberOfTelephonesToReport(messages, 2);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("S#03")) {
//            Log.i(TAG, "report to 1 telephone");
            String phoneNumber = formatMessageNumberOfTelephonesToReport(messages, 3);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        } else if (messages.getMessageBody().contains("S?1")) {
//            Log.i(TAG, "report to 1 telephone");
            String phoneNumber = formatMessageNumberOfTelephonesToReportAll(messages);
            Intent newIntent = new Intent(context, TelephoneChangeActivity.class);
            startNewActivityOnTop(context, newIntent, phoneNumber);
        }
    }

    private void startNewActivityOnTop(Context context, Intent intent, String phoneNumber) {
        intent.putExtra(Constants.EXTRA_COMES_FROM_RECEIVER, true);
        intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Formats an status message and saves it into the database
     *
     * @param smsMessage unformatted message
     * @return phoneNumber from where the messages comes
     */
    private String formatMessage(SmsMessage smsMessage) {
        String sms = smsMessage.getMessageBody();
        String[] splitMessage = sms.split("/");
        String temporalAddress = null;
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

                temporalAddress = smsMessage.getOriginatingAddress(); // this will be the phone number
                if (temporalAddress.length() > 10) {
                    temporalAddress = smsMessage.getOriginatingAddress().substring(3, 13);
                }

                saveEvent(smsMessage, micrologId, stateFull, originFull, upsState, minutesOnBattery, minutesOnTransfer, plantFailure, temporalAddress);

                saveMicrologID(temporalAddress, micrologId, stateFull);

            }

        } catch (Exception e) {
            e.printStackTrace();
//            Log.e(TAG, "sms must not be well formatted");
        }

        return temporalAddress;
    }

    /**
     * Saves an event to the database
     */
    private void saveEvent(SmsMessage smsMessage, String micrologId, String stateFull, String originFull, String upsState, int minutesOnBattery, int minutesOnTransfer, boolean plantFailure, String sensorPhoneNumber) {
        PlantEvent plantEvent = new PlantEvent(
                sensorPhoneNumber,
                micrologId,
                stateFull,
                originFull,
                upsState,
                minutesOnBattery,
                minutesOnTransfer,
                plantFailure,
                smsMessage.getTimestampMillis()
        );
        plantEvent.save();
    }

    private void saveMicrologID(String temporalAddress, String micrologId, String stateString) {
        Microlog microlog = new Select().
                from(Microlog.class).
                where("sensorPhoneNumber = ?", temporalAddress).
                executeSingle();

        if (microlog == null) {
            microlog = new Microlog(temporalAddress, micrologId, temporalAddress, stateString, 0, 0, 0);
            microlog.save();
        } else {
            microlog.setSensorId(micrologId);
            microlog.setLastState(stateString);
            microlog.save();
        }
    }

    /**
     * Formats an setPoints special sms message that contains all the temperatures
     *
     * @param smsMessage unformatted sms message
     * @return phone number from the sms
     */
    private String formatMessageSetPoints(SmsMessage smsMessage) {
        String sms = smsMessage.getMessageBody();
        String temporalAddress = null;
        Double[] setPoints = new Double[3];
        try {
            setPoints[0] = (double) Integer.parseInt(sms.substring(5, 9), 16);
            setPoints[1] = (double) Integer.parseInt(sms.substring(9, 13), 16);
            setPoints[2] = (double) Integer.parseInt(sms.substring(13, 17), 16);

            temporalAddress = smsMessage.getOriginatingAddress();
            if (temporalAddress.length() > 10) {
                temporalAddress = smsMessage.getOriginatingAddress().substring(3, 13);
            }

            saveSetPoints(temporalAddress, setPoints, smsMessage.getTimestampMillis());

        } catch (Exception e) {
            e.printStackTrace();
//            Log.e(TAG, "sms must not be well formatted");
        }

        return temporalAddress;
    }

    private void saveSetPoints(String phoneNumber, Double[] setPoints, long timestampMillis) {
        Microlog microlog = new Select().
                from(Microlog.class).
                where("sensorPhoneNumber = ?", phoneNumber).executeSingle();
        for (int i = 0; i < setPoints.length; i++) {
            SetPoint existingSetPoint = new Select().
                    from(SetPoint.class).
                    where("phoneNumber = ?", phoneNumber).
                    and("setPointNumber = ?", i).executeSingle();
            if (existingSetPoint != null) {
                existingSetPoint.micrologId = microlog.sensorId;
                existingSetPoint.tempInFahrenheit = setPoints[i];
                existingSetPoint.timestamp = timestampMillis;
                existingSetPoint.verified = true;
                existingSetPoint.save();
            } else {
                SetPoint setPoint = new SetPoint(
                        phoneNumber,
                        microlog.sensorId,
                        i,
                        setPoints[i],
                        timestampMillis,
                        true
                );
                setPoint.save();
            }
        }
    }

    /**
     * Formats a sms that contains a single phone number to report
     *
     * @param smsMessage unformatted sms
     * @param subString  is the number of spaces from the beginning that we have to "ignore" to
     *                   create our phone
     * @param phoneIndex index to save in the database 1, 2, 3
     * @return phone number from sms
     */
    private String formatMessageTelephoneNumberSingle(SmsMessage smsMessage, int subString, int phoneIndex) {
        String sms = smsMessage.getMessageBody();
        String temporalAddress = null;
        try {
            String phoneToReport = sms.substring(subString, sms.length());

            temporalAddress = smsMessage.getOriginatingAddress();
            if (temporalAddress.length() > 10) {
                temporalAddress = smsMessage.getOriginatingAddress().substring(3, 13);
            }

            saveReportingTelephoneNumber(temporalAddress, phoneIndex, phoneToReport, smsMessage.getTimestampMillis());

        } catch (Exception e) {
            e.printStackTrace();
//            Log.e(TAG, "sms must not be well formatted");
        }

        return temporalAddress;
    }

    public void saveReportingTelephoneNumber(String micrologPhone, int phoneIndex, String phoneToReport, long date) {
        Telephone telephone = new Select().
                from(Telephone.class).
                where("sensorPhoneNumber = ?", micrologPhone).
                and("phoneIndex = ?", phoneIndex).executeSingle();

        if (telephone != null) {
            telephone.phoneNumber = phoneToReport;
            telephone.date = date;
            telephone.verified = true;
            telephone.save();
        } else {
            Telephone newTelephone = new Telephone(
                    micrologPhone,
                    phoneIndex,
                    phoneToReport,
                    date,
                    true
            );
            newTelephone.save();
        }
    }

    /**
     * Formats an sms that contains the number of phones that the sensor will report to
     *
     * @param smsMessage     sms
     * @param numberOfPhones number of phones that the sensor will report to
     * @return sensor phone number
     */
    private String formatMessageNumberOfTelephonesToReport(SmsMessage smsMessage, int numberOfPhones) {
        String temporalAddress = null;
        try {
            temporalAddress = smsMessage.getOriginatingAddress();
            if (temporalAddress.length() > 10) {
                temporalAddress = smsMessage.getOriginatingAddress().substring(3, 13);
            }

            saveNumberOfPhonesToReport(temporalAddress, numberOfPhones);

        } catch (Exception e) {
            e.printStackTrace();
//            Log.e(TAG, "sms must not be well formatted");
        }

        return temporalAddress;
    }

    public void saveNumberOfPhonesToReport(String temporalAddress, int numberOfPhones) {
        Microlog microlog = new Select().from(Microlog.class).
                where("sensorPhoneNumber = ?", temporalAddress).
                executeSingle();

        if (microlog != null) {
            microlog.numberOfPhonesToReport = numberOfPhones;
            microlog.save();
        }

    }

    /**
     * Formats an sms that contains the number of phones that the sensor will report to
     *
     * @param smsMessage sms
     * @return sensor phone number
     */
    private String formatMessageNumberOfTelephonesToReportAll(SmsMessage smsMessage) {
        String temporalAddress = null;
        int numberOfPhones;
        try {
            String tempNumber = smsMessage.getMessageBody().substring(31, 33);
            numberOfPhones = Integer.parseInt(tempNumber);
            temporalAddress = smsMessage.getOriginatingAddress();
            if (temporalAddress.length() > 10) {
                temporalAddress = smsMessage.getOriginatingAddress().substring(3, 13);
            }

            saveNumberOfPhonesToReport(temporalAddress, numberOfPhones);

        } catch (Exception e) {
            e.printStackTrace();
//            Log.e(TAG, "sms must not be well formatted");
        }

        return temporalAddress;
    }

}
