package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.AndroidTest.R;
import net.microcosmus.helloapp.domain.Discount;
import net.microcosmus.helloapp.scanner.QRScannerActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class DiscountActivity extends Activity {

    public static final int INTENT_REQUEST_REF = 100;

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM");

    private boolean qrDetected = false;
    private Discount discount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.discount);


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            discount = (Discount) extras.getSerializable("discount");

            if (discount != null) {

                TextView placeView = (TextView) findViewById(R.id.ddPlace);
                placeView.setText(discount.getPlace());

                TextView descrView = (TextView) findViewById(R.id.ddDiscount);
                descrView.setText(discount.getTitle());

                TextView rateView = (TextView) findViewById(R.id.ddDiscountRate);
                rateView.setText("-" + discount.getRate() + "%");

                TextView expiresView = (TextView) findViewById(R.id.ddExpires);
                expiresView.setText("");
//                expiresView.setText(DATE_FORMAT.format(discount.getGoodThrough()));

                final Button confirmBtn = (Button) findViewById(R.id.ddConfirmBtn);
                final TextView messageView = (TextView) findViewById(R.id.ddApplyResult);
                final TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);
                final TextView scanResult = (TextView) findViewById(R.id.ddScanResult);
                final ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);

                confirmBtn.setVisibility(View.VISIBLE);
                scanResult.setVisibility(View.GONE);
                messageView.setVisibility(View.GONE);
                numberView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);

                final Context ctx = this;
                confirmBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showQRScanner(ctx, discount);
                    }
                });

            }
        }
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.discount_menu, menu);
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


    private void showQRScanner(Context context, Discount d) {
        Intent intent = new Intent(context, QRScannerActivity.class);
        intent.putExtra("discount", d);
        startActivityForResult(intent, INTENT_REQUEST_REF);
    }

    // Function to read the result from newly created activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_REQUEST_REF) {
            qrDetected = true;
            String confirmCode = (String) data.getExtras().get("text");

            Button confirmBtn = (Button) findViewById(R.id.ddConfirmBtn);
            TextView messageView = (TextView) findViewById(R.id.ddApplyResult);
            TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);
            TextView scanResult = (TextView) findViewById(R.id.ddScanResult);
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);

            confirmBtn.setVisibility(View.GONE);
            scanResult.setVisibility(View.VISIBLE);

            scanResult.setText(confirmCode);


            progressBar.setVisibility(View.VISIBLE);
            confirmBtn.setVisibility(View.GONE);
            HelloClient.applyDiscount(discount.getId(), confirmCode, messageView, numberView, progressBar);
        }

    }


}
