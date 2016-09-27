//  Copyright (c) 2016 PayPal. All rights reserved.

package io.card.cordova.sdk;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;

import com.mobile.connect.PWConnect;
import com.mobile.connect.exception.PWError;
import com.mobile.connect.exception.PWException;
import com.mobile.connect.exception.PWProviderNotInitializedException;
import com.mobile.connect.listener.PWTokenObtainedListener;
import com.mobile.connect.listener.PWTransactionListener;
import com.mobile.connect.payment.PWPaymentParams;
import com.mobile.connect.payment.credit.PWCreditCardType;
import com.mobile.connect.provider.PWTransaction;
import com.mobile.connect.service.PWProviderBinder;

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;

public class CardIOCordovaPlugin extends CordovaPlugin implements PWTokenObtainedListener, PWTransactionListener {

    private CallbackContext callbackContext;
    private Activity activity = null;
    private static final int REQUEST_CARD_SCAN = 10;

    /* 
     *  Peach payments connectivity - Installation of SDK and setup.
     */
    private PWProviderBinder _binder;
    private boolean currentTokenization = false;
    private static final String APPLICATIONIDENTIFIER = "8a82941756a2ab6f0156bc3694223c0a";
    private static final String PROFILETOKEN = "53ae27246b9511e69fdb316df49128d9";

    private JSONObject cardDetails = new JSONObject();
    private String token;

    private ServiceConnection _serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            _binder = (PWProviderBinder) service;
            // we have a connection to the service
            try {
                _binder.initializeProvider(PWConnect.PWProviderMode.TEST,
                        APPLICATIONIDENTIFIER, PROFILETOKEN);
            } catch (PWException ee) {
                // error initializing the provider
                ee.printStackTrace();
            }
        }
     
        @Override
        public void onServiceDisconnected(ComponentName name) {
            _binder = null;
        }
    };

    /* 
     *  End peach payments connectivity.
     *  Start of chargeToken to obtain peach payments token.
     */

    private void chargeToken(final CordovaArgs args, final CallbackContext callbackContext) {
        PWPaymentParams paymentParams = null;
        String cardholderName = null, cardNumber = null, expiryYear = null, expiryMonth = null, cvv = null;
        PWCreditCardType cardType = null;
        try{
            cardholderName = cardDetails.getString("cardholderName");
            cardType = (PWCreditCardType)cardDetails.get("cardType");
            cardNumber = cardDetails.getString("cardNumber");
            expiryYear = cardDetails.getString("expiryYear");
            expiryMonth = cardDetails.getString("expiryMonth");
            cvv = cardDetails.getString("cvv");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            paymentParams = _binder
                    .getPaymentParamsFactory()
                    .createCreditCardTokenizationParams(cardholderName, cardType, cardNumber,  expiryYear,  expiryMonth, cvv);

        } catch (PWProviderNotInitializedException e) {
            //setStatusText("Error: Provider not initialized!");
            e.printStackTrace();
            return;
        } catch (PWException e) {
            //setStatusText("Error: Invalid Parameters!");
            e.printStackTrace();
            return;
        }

        currentTokenization = true;

        try {
            _binder.createAndRegisterObtainTokenTransaction(paymentParams);
        } catch (PWException e) {
            //setStatusText("Error: Could not contact Gateway!");
            e.printStackTrace();
        }

        _binder.addTokenObtainedListener(this);
        _binder.addTransactionListener(this);

    }

    /* 
     *  End peach payments.
     */

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        this.activity = this.cordova.getActivity();
        boolean retValue = true;
        if (action.equals("scan")) {
            this.scan(args);
        } else if (action.equals("canScan")) {
            this.canScan(args);
        } else if (action.equals("version")) {
            this.callbackContext.success(CardIOActivity.sdkVersion());
        } else if (action.equals("chargeToken")) {
            this.chargeToken(args,callbackContext);
        } else {
            retValue = false;
        }

        return retValue;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void prepareToRender(JSONArray args) throws JSONException {
        this.callbackContext.success();
    }

    private void scan(CordovaArgs args) throws JSONException {
        Intent scanIntent = new Intent(this.activity, CardIOActivity.class);
        JSONObject configurations = args.getJSONObject(0);
        // customize these values to suit your needs.
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, this.getConfiguration(configurations, "requireExpiry", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, this.getConfiguration(configurations, "requireCVV", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, this.getConfiguration(configurations, "requirePostalCode", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, this.getConfiguration(configurations, "supressManual", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_RESTRICT_POSTAL_CODE_TO_NUMERIC_ONLY, this.getConfiguration(configurations, "restrictPostalCodeToNumericOnly", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, this.getConfiguration(configurations, "keepApplicationTheme", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CARDHOLDER_NAME, this.getConfiguration(configurations, "requireCardholderName", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_USE_CARDIO_LOGO, this.getConfiguration(configurations, "useCardIOLogo", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_SCAN_INSTRUCTIONS, this.getConfiguration(configurations, "scanInstructions", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_NO_CAMERA, this.getConfiguration(configurations, "noCamera", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_SCAN_EXPIRY, this.getConfiguration(configurations, "scanExpiry", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_LANGUAGE_OR_LOCALE, this.getConfiguration(configurations, "languageOrLocale", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_GUIDE_COLOR, this.getConfiguration(configurations, "guideColor", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_SUPPRESS_CONFIRMATION, this.getConfiguration(configurations, "suppressConfirmation", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_HIDE_CARDIO_LOGO, this.getConfiguration(configurations, "hideCardIOLogo", false)); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_SUPPRESS_SCAN, this.getConfiguration(configurations, "suppressScan", false)); // default: false
        this.cordova.startActivityForResult(this, scanIntent, REQUEST_CARD_SCAN);
    }

    private void canScan(CordovaArgs args) throws JSONException {
        if (CardIOActivity.canReadCardWithCamera()) {
            // This is where we return if scanning is enabled.
            this.callbackContext.success("Card Scanning is enabled");
        } else {
            this.callbackContext.error("Card Scanning is not enabled");
        }
    }

    // onActivityResult
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (REQUEST_CARD_SCAN == requestCode) {
            if (resultCode == CardIOActivity.RESULT_CARD_INFO) {
                CreditCard scanResult = null;
                if (intent.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
                    scanResult = intent
                            .getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
                    cardDetails = this.toJSONObject(scanResult);
                    this.callbackContext.success(this.toJSONObject(scanResult));
                } else {
                    this.callbackContext
                            .error("card was scanned but no result");
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                this.callbackContext.error("card scan cancelled");
            } else {
                this.callbackContext.error(resultCode);
            }
        }
    }

    private JSONObject toJSONObject(CreditCard card) {
        JSONObject scanCard = new JSONObject();
        try {
            scanCard.put("cardType", card.getCardType());
            scanCard.put("redactedCardNumber", card.getRedactedCardNumber());
            scanCard.put("cardNumber", card.cardNumber);
            scanCard.put("expiryMonth", card.expiryMonth);
            scanCard.put("expiryYear", card.expiryYear);
            scanCard.put("cvv", card.cvv);
            scanCard.put("postalCode", card.postalCode);
            scanCard.put("cardholderName", card.cardholderName);
        } catch (JSONException e) {
            scanCard = null;
        }

        return scanCard;
    }

    private <T> T getConfiguration(JSONObject configurations, String name, T defaultValue) {
        if (configurations.has(name)) {
            try {
                return (T)configurations.get(name);
            } catch (JSONException ex) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    @Override
    public void creationAndRegistrationSucceeded(PWTransaction transaction) {
        // check if it is our tokenization transaction
        if(currentTokenization) {
            // execute it
            try {
                _binder.obtainToken(transaction);
            } catch (PWException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void obtainedToken(String token, PWTransaction transaction) {
        // store the token.
        this.token = token;
    }

    @Override
    public void transactionSucceeded(PWTransaction pwTransaction) {
        // Notify on transaction success.
    }

    @Override
    public void transactionFailed(PWTransaction pwTransaction, PWError pwError) {

    }

    @Override
    public void creationAndRegistrationFailed(PWTransaction pwTransaction, PWError pwError) {

    }
}
