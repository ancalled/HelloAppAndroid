package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
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

        ImageView campaignIcon = (ImageView) findViewById(R.id.ddCampaignImage);
        downloadIcon(campaign, campaignIcon);

        ImageView edgeImg = (ImageView) findViewById(R.id.edgeImg2);
        CanvasUtils.buildBentEdge(edgeImg, 12);

        ImageView btnTriang = (ImageView) findViewById(R.id.btnTriang);
        CanvasUtils.buildBtnEdge(btnTriang, 15, 40);


        ImageView discountIcon = (ImageView) findViewById(R.id.ddDiscRateIcon);
        CanvasUtils.buildDiscountRateIcon(discountIcon, campaign.getRate(), 56);

        final EditText priceInput = (EditText) findViewById(R.id.priceInput);

        Button confirmBtn = (Button) findViewById(R.id.ddConfirmBtn);
        confirmBtn.setVisibility(View.VISIBLE);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String priceTxt = priceInput.getText().toString();
                int price = priceTxt != null && !priceTxt.isEmpty() ? Integer.parseInt(priceTxt) : 0;
                showQRScanner(DiscountActivity.this, campaign, price, debugMode);
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


    private void showQRScanner(Context context, Campaign d, int price, boolean debugMode) {
        Intent intent = new Intent(context, debugMode ?
                FakeScannerActivity.class :
                QRScannerActivity.class);
        intent.putExtra("campaign", d);
        intent.putExtra("price", price);
        startActivityForResult(intent, INTENT_REQUEST_REF);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_REQUEST_REF) {

            setContentView(R.layout.apply_discount);

            ImageView edgeImg = (ImageView) findViewById(R.id.triagEdge);
            CanvasUtils.buildBentEdge(edgeImg, 12);


            if (resultCode == RESULT_OK) {
                String confCode = (String) data.getExtras().get("text");

                ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);
                LinearLayout successPanel = (LinearLayout) findViewById(R.id.applySuccessPanel);
                LinearLayout errorPanel = (LinearLayout) findViewById(R.id.applyErrorPanel);

                progressBar.setVisibility(View.VISIBLE);
                successPanel.setVisibility(View.GONE);
                errorPanel.setVisibility(View.GONE);

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

                LinearLayout successPanel = (LinearLayout) findViewById(R.id.applySuccessPanel);
                LinearLayout errorPanel = (LinearLayout) findViewById(R.id.applyErrorPanel);

                ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);
                progressBar.setVisibility(View.GONE);

                if (result != null && result.getStatus() == DiscountApplyResult.Status.OK) {
                    successPanel.setVisibility(View.VISIBLE);
                    errorPanel.setVisibility(View.GONE);

                    TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);
                    TextView numberRevView = (TextView) findViewById(R.id.ddApplyResultInverted);

                    String text = Long.toString(result.getId());
                    numberView.setText(text);
                    numberRevView.setText(text);

                } else {
                    successPanel.setVisibility(View.VISIBLE);
                    errorPanel.setVisibility(View.GONE);

                    TextView applyResult = (TextView) findViewById(R.id.ddApplyError);

                    if (result != null) {
                        int mesNo = getErMessage(result.getStatus());
                        if (mesNo > 0) {
                            applyResult.setText(getResources().getString(mesNo));
                        } else {
                            applyResult.setText(getResources().getString(R.string.unknownError));
                        }
                    } else {
                        applyResult.setText(getResources().getString(R.string.unknownError));
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


    private void downloadIcon(Campaign d, final ImageView imageView) {
        String url = String.format(HelloClient.CAMPAIGN_ICON_URL, d.getId());

        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                return HelloClient.downloadBitmap(params[0]);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (isCancelled()) {
                    bitmap = null;
                }

                imageView.setImageBitmap(bitmap);
            }
        }.execute(url);
    }
}
