package net.microcosmus.helloapp;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import net.microcosmus.helloapp.HelloApp.R;
import net.microcosmus.helloapp.domain.Campaign;


public class FakeScannerActivity extends Activity {

    private boolean barcodeScanned = false;

    private Campaign campaign;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Bundle extras = getIntent().getExtras();
        if (extras == null) return;

        campaign = (Campaign) extras.getSerializable("campaign");
        Integer price = (Integer) extras.getSerializable("price");

        setContentView(R.layout.fake_scanner);

        if (price != null) {
            TextView priceTxt = (TextView) findViewById(R.id.fsPrice);
            TextView priceRevTxt = (TextView) findViewById(R.id.fsPriceReverted);

            priceTxt.setText("" + price);
            priceRevTxt.setText("" + price);
        }

        Button scanButton = (Button) findViewById(R.id.fsScanButton);
        final ImageView scanView = (ImageView) findViewById(R.id.fsScanView);


        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!barcodeScanned) {
                    Log.d("QRScanner", "Starting fake scanner");

                    scanView.setImageResource(R.drawable.fake_detection);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String text = "100" + campaign.getId();

                    Log.d("QRScanner", "Detected: " + text);

                    barcodeScanned = true;
                    backToDiscount(text);
                }
            }
        });
    }


    private void backToDiscount(String text) {
        Log.d("QRScanner", "Back to discount activity...");
        Intent intent = new Intent(this, FakeScannerActivity.class);
        intent.putExtra("text", text);
        setResult(RESULT_OK, intent);
        finish();
    }


}
