package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.AndroidTest.R;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import net.microcosmus.helloapp.domain.Campaign;
import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;


public class DiscountActivity extends Activity {

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
        placeView.setText(campaign.getPlace());

        TextView descrView = (TextView) findViewById(R.id.ddDiscount);
        descrView.setText(campaign.getTitle());

        TextView rateView = (TextView) findViewById(R.id.ddDiscountRate);
        rateView.setText("-" + campaign.getRate() + "%");

//        TextView expiresView = (TextView) findViewById(R.id.ddExpires);
//        expiresView.setText("");

        Button confirmBtn = (Button) findViewById(R.id.ddConfirmBtn);
        TextView messageView = (TextView) findViewById(R.id.ddApplyResult);
        TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);
        TextView scanResult = (TextView) findViewById(R.id.ddScanResult);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);

        confirmBtn.setVisibility(View.VISIBLE);
        scanResult.setVisibility(View.GONE);
        messageView.setVisibility(View.GONE);
        numberView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showQRScanner(DiscountActivity.this, campaign, debugMode);
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
            String confCode = (String) data.getExtras().get("text");

            Button confirmBtn = (Button) findViewById(R.id.ddConfirmBtn);
            TextView scanResult = (TextView) findViewById(R.id.ddScanResult);
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);

            confirmBtn.setVisibility(View.GONE);
            scanResult.setVisibility(View.VISIBLE);
            scanResult.setText(confCode);
            progressBar.setVisibility(View.VISIBLE);
            confirmBtn.setVisibility(View.GONE);

            applyDiscount(campaign.getId(), confCode);
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

                TextView messageView = (TextView) findViewById(R.id.ddApplyResult);
                TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);
                messageView.clearComposingText();

                if (result.getStatus() == DiscountApplyResult.Status.OK) {
                    progressBar.setVisibility(View.GONE);
                    messageView.setVisibility(View.VISIBLE);
                    numberView.setVisibility(View.VISIBLE);
                    numberView.setText(Long.toString(result.getId()));
                }
            }

        }.execute(buildUrl(
                HelloClient.getUser().getId(),
                campaignId,
                confirmCode
        ));
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
