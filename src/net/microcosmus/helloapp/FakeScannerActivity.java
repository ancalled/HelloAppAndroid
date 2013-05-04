package net.microcosmus.helloapp;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import com.example.AndroidTest.R;
import net.microcosmus.helloapp.domain.Discount;


public class FakeScannerActivity extends Activity {

    private boolean barcodeScanned = false;

    private Discount discount;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            discount = (Discount) extras.getSerializable("discount");
        }

        setContentView(R.layout.fake_scanner);
        startFakeScanner();
    }


    private void startFakeScanner() {
        Log.d("QRScanner", "Starting fake scanner");

        final TextView scanText = (TextView) findViewById(R.id.scanText);
        scanText.setText("");

        Button scanButton = (Button) findViewById(R.id.ScanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!barcodeScanned) {

                    scanText.setText("Scanning...");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    scanText.setText("Done.");

                    String text = "100" + discount.getId();

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
