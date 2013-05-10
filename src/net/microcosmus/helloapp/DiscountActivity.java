package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import net.microcosmus.helloapp.HelloApp.R;
import net.microcosmus.helloapp.domain.Campaign;
import net.microcosmus.helloapp.domain.DiscountApplyResult;
import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;


public class DiscountActivity extends Activity {

    public static final String CAT = "DiscountActivity";


    public static final int INTENT_REQUEST_REF = 100;

    private Tracker mGaTracker;


    private Campaign campaign;
    private boolean debugMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        GoogleAnalytics mGaInstance = GoogleAnalytics.getInstance(this);
        mGaTracker = mGaInstance.getTracker("UA-40626076-1");
        debugMode = getIsDebug();
        Log.d("DiscountActivity", "Debug: " + debugMode);

        setContentView(R.layout.discount);

        Bundle extras = getIntent().getExtras();
        campaign = (Campaign) extras.getSerializable("campaign");
        if (campaign == null) return;

        TextView placeView = (TextView) findViewById(R.id.ddPlace);
        if (placeView == null) {
            Log.e(CAT, "placeView is null!");
            return;
        }
        placeView.setText(campaign.getPlace());

        TextView descrView = (TextView) findViewById(R.id.ddDiscount);
        descrView.setText(campaign.getTitle());

        TextView discountRate = (TextView) findViewById(R.id.discountRate);
        discountRate.setText("-" + campaign.getRate() + "%");

        Button confirmBtn = (Button) findViewById(R.id.ddConfirmBtn);
        LinearLayout scanResultPanel = (LinearLayout) findViewById(R.id.discountConfirmerPanel);
        LinearLayout applyResultPanel = (LinearLayout) findViewById(R.id.discountApplyResultPanel);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);
        EditText priceInput = (EditText) findViewById(R.id.priceInput);
        final TextView prcDisc = (TextView) findViewById(R.id.priceWithDiscount);

        confirmBtn.setVisibility(View.VISIBLE);
        scanResultPanel.setVisibility(View.GONE);
        applyResultPanel.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showQRScanner(DiscountActivity.this, campaign, debugMode);
            }
        });

        priceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                char[] chars = new char[editable.length()];
                editable.getChars(0, editable.length(), chars, 0);
                String text = new String(chars);
                int price = Integer.parseInt(text);
                int withDiscount = price * (100 - campaign.getRate()) / 100;
                prcDisc.setText("" + withDiscount);
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        mGaTracker.sendView("DiscountActivity: id: " + campaign.getId() + ", place: " + campaign.getPlace());
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.discount_menu, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_exit:
                moveTaskToBack(true);
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private void showQRScanner(Context context, Campaign d, boolean debugMode) {
        Intent intent = new Intent(context, debugMode ?
                FakeScannerActivity.class :
                QRScannerActivity.class);
        intent.putExtra("campaign", d);
        startActivityForResult(intent, INTENT_REQUEST_REF);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_REQUEST_REF) {
            if (resultCode == RESULT_OK) {
                String confCode = (String) data.getExtras().get("text");

                Button confirmBtn = (Button) findViewById(R.id.ddConfirmBtn);
                LinearLayout scanResultPanel = (LinearLayout) findViewById(R.id.discountConfirmerPanel);
                LinearLayout applyResultPanel = (LinearLayout) findViewById(R.id.discountApplyResultPanel);
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);
                TextView scanResult = (TextView) findViewById(R.id.ddScanResult);

                confirmBtn.setVisibility(View.GONE);
                scanResultPanel.setVisibility(View.VISIBLE);
                applyResultPanel.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                scanResult.setText(confCode);

                applyDiscount(campaign.getId(), confCode);
            }
        }

    }

    private boolean getIsDebug() {
        PackageManager pm = getPackageManager();
        final PackageInfo pinfo;
        try {
            pinfo = pm.getPackageInfo(getPackageName(), 0);
            return (pinfo.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }


    private void applyDiscount(long campaignId, String confirmCode) {

        new AsyncTask<String, Void, DiscountApplyResult>() {

            @Override
            protected DiscountApplyResult doInBackground(String... params) {
                String response = HelloClient.doPost(params[0]);
                try {
                    return HelloClient.parseDiscountApplyResult(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(DiscountApplyResult result) {
                if (result == null) return;

                Button confirmBtn = (Button) findViewById(R.id.ddConfirmBtn);
                LinearLayout scanResultPanel = (LinearLayout) findViewById(R.id.discountConfirmerPanel);
                LinearLayout applyResultPanel = (LinearLayout) findViewById(R.id.discountApplyResultPanel);
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);
                TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);

                if (result.getStatus() == DiscountApplyResult.Status.OK) {

                    confirmBtn.setVisibility(View.GONE);
                    scanResultPanel.setVisibility(View.VISIBLE);
                    applyResultPanel.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    numberView.setText(Long.toString(result.getId()));
                } else {
                    TextView applyResult = (TextView) findViewById(R.id.ddApplyResult);

                    confirmBtn.setVisibility(View.GONE);
                    scanResultPanel.setVisibility(View.VISIBLE);
                    applyResultPanel.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);

                    int mesNo = getErMessage(result.getStatus());
                    if (mesNo > 0) {
                        applyResult.setText(getResources().getString(mesNo));
                    }

                }
            }

        }.execute(buildUrl(
                HelloClient.getUser().getId(),
                campaignId,
                confirmCode
        ));
    }

    private static int getErMessage(DiscountApplyResult.Status status) {
        switch (status) {
            case NO_USER_FOUND:
                return R.string.erNoUserFound;
            case NO_CONFIRMER_FOUND:
                return R.string.erNoConfirmerFound;
            case NO_DISCOUNT_FOUND:
                return R.string.erNoDiscountFound;
            case CONFIRMER_IS_NOT_OF_THIS_COMPANY:
                return R.string.erConfirmerIsNotOfThisCompany;
            case COULD_NOT_APPLY:
                return R.string.erCouldNotApply;

        }

        return -1;
    }


    private static String buildUrl(long userId, long campaignId, String confirmerCode) {
        try {
            URI uri = new URI(
                    HelloClient.SCHEME,
                    null,
                    HelloClient.HOST,
                    HelloClient.PORT,
                    "/helloapp/customer/api/apply-campaign",
                    String.format("userId=%d&campaignId=%d&confirmerCode=%s", userId, campaignId, confirmerCode),
                    null
            );
            return uri.toURL().toString();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

}
