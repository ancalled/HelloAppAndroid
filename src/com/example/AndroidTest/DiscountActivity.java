package com.example.AndroidTest;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class DiscountActivity extends Activity {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.discount);

        final HelloClient client = new HelloClient();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final Discount discount = (Discount) extras.getSerializable("discount");
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

                final Button applyButton = (Button) findViewById(R.id.ddApplyBtn);
                final TextView messageView = (TextView) findViewById(R.id.ddApplyResult);
                final TextView numberView = (TextView) findViewById(R.id.ddApplyResultNumber);
                final ProgressBar progressBar = (ProgressBar) findViewById(R.id.ddProgressBar);

                applyButton.setVisibility(View.VISIBLE);
                messageView.setVisibility(View.GONE);
                numberView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);

                applyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        progressBar.setVisibility(View.VISIBLE);
                        applyButton.setVisibility(View.GONE);

                        long userId = 1L;
                        client.applyDiscount(userId, discount.getId(), messageView, numberView, progressBar);
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


}
