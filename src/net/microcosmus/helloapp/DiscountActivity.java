package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import net.microcosmus.helloapp.domain.User;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static net.microcosmus.helloapp.HelloClient.ACTION_APPLY_DISCOUNT;
import static net.microcosmus.helloapp.HelloClient.RequestType.POST;


public class DiscountActivity extends Activity {

    public static final String CAT = "DiscountActivity";


    public static final int INTENT_REQUEST_REF = 100;
    public static final int SIGN_LENGTH = 7;

    private Tracker mGaTracker;

    private Campaign campaign;
    private boolean debugMode;

    private final HelloClient helloClient = new HelloClient();

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
        final User user = (User) extras.getSerializable("user");

        if (campaign == null || user == null) return;

        helloClient.setUser(user);

        TextView placeView = (TextView) findViewById(R.id.ddPlace);
        if (placeView == null) {
            Log.e(CAT, "placeView is null!");
            return;
        }
        placeView.setText(campaign.getPlace());

        TextView descrView = (TextView) findViewById(R.id.ddDiscount);
        descrView.setText(campaign.getTitle());

        ImageView campaignIcon = (ImageView) findViewById(R.id.ddCampaignImage);
        Bitmap bitmap = MainActivity.getIconFromFile(campaign.getId());
        if (bitmap != null) {
            campaignIcon.setImageBitmap(bitmap);
        }

        ImageView edgeImg = (ImageView) findViewById(R.id.ddEdgeImg);
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

                if (campaign.getNeedConfirm()) {
                    showQRScanner(DiscountActivity.this, campaign, price, debugMode);

                } else if (campaign.getNeedSign()) {
                    String sign = generateSign(campaign, user, price);
                    showApplyView();
                    showSign(sign, price);

                } else {
                    showApplyView();
                    applyDiscount(campaign.getId(), null);
                }
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

            if (resultCode == RESULT_OK) {
                showApplyView();

                String confirmCode = (String) data.getExtras().get("text");
                applyDiscount(campaign.getId(), confirmCode);
            }
        }
    }

    private void showApplyView() {
        setContentView(R.layout.apply_discount);

        ImageView edgeImg = (ImageView) findViewById(R.id.triagEdge);
        CanvasUtils.buildBentEdge(edgeImg, 12);


        ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);
        LinearLayout successPanel = (LinearLayout) findViewById(R.id.applySuccessPanel);
        LinearLayout errorPanel = (LinearLayout) findViewById(R.id.applyErrorPanel);

        progressBar.setVisibility(View.VISIBLE);
        successPanel.setVisibility(View.GONE);
        errorPanel.setVisibility(View.GONE);
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

        Log.i(CAT, "Applying discount: campId: " + campaignId + ", confirmCode: " + confirmCode);

        helloClient.apiCall(new HelloClient.ApiTask(POST, ACTION_APPLY_DISCOUNT) {
            @Override
            protected void onResponse(String response) {
                if (response == null) return;


                DiscountApplyResult result = null;
                try {
                    result = HelloClient.parseDiscountApplyResult(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                LinearLayout successPanel = (LinearLayout) findViewById(R.id.applySuccessPanel);
                LinearLayout errorPanel = (LinearLayout) findViewById(R.id.applyErrorPanel);

                ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);
                progressBar.setVisibility(View.GONE);

                if (result != null && result.getStatus() == DiscountApplyResult.Status.OK) {
                    successPanel.setVisibility(View.VISIBLE);
                    errorPanel.setVisibility(View.GONE);

                    TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);
//                    TextView numberRevView = (TextView) findViewById(R.id.ddApplyResultInverted);

                    String text = Long.toString(result.getId());
                    numberView.setText(text);
//                    numberRevView.setText(text);

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
        }.param("campaignId", campaignId)
                .param("userId", helloClient.getUser().getId())
                .param("confirmerCode", confirmCode != null ? confirmCode : "none")
        );

    }

    private void showSign(String sign, int price) {
        LinearLayout successPanel = (LinearLayout) findViewById(R.id.applySuccessPanel);
        LinearLayout errorPanel = (LinearLayout) findViewById(R.id.applyErrorPanel);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);

//        if (successPanel != null && errorPanel != null && progressBar != null) {
        successPanel.setVisibility(View.VISIBLE);
        errorPanel.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
//        }

        TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);
//        TextView numberRevView = (TextView) findViewById(R.id.ddApplyResultInverted);
        TextView purchaseText = (TextView) findViewById(R.id.ddApplyResult);
        TextView ddActivated = (TextView) findViewById(R.id.ddActivated);

        numberView.setText(sign);
//        numberRevView.setText(sign);

        String priceWODiscountLabel = getResources().getString(R.string.priceWODiscountLabel);
        String priceTenge = getResources().getString(R.string.priceTenge);

        purchaseText.setText(String.format(priceTenge, price));
        ddActivated.setText(priceWODiscountLabel);
    }

    private static String generateSign(Campaign campaign, User user, int price) {
        if (campaign == null || user == null) return null;
        String data = campaign.getId() + ":" + user.getId() + ":" + user.getToken() + ":" + price;

        String hash = null;
        try {
            hash = HelloClient.calcHash(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (hash != null && hash.length() > SIGN_LENGTH) {
            return hash.substring(hash.length() - SIGN_LENGTH, hash.length());
        }

        return null;
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


}
