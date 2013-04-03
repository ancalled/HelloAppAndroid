package com.example.AndroidTest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isNetworkAvailable()) {

            HelloClient client = new HelloClient();

            LinearLayout listView = (LinearLayout) findViewById(R.id.listView);

            client.retreiveDiscounts(listView, this, listView);

        } else {
            String text = getResources().getString(R.string.internet_access);
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public static View createDiscountRow(final Discount d, final Context context, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.discount_row, parent, false);

        TextView titleView = (TextView) view.findViewById(R.id.discountTitle);
        titleView.setText(d.getTitle());

        TextView placeView = (TextView) view.findViewById(R.id.discountPlace);
        placeView.setText(d.getPlace());

        TextView rateView = (TextView) view.findViewById(R.id.discountRate);
        rateView.setText("-" + d.getRate() + "%");

        ImageView imageView = (ImageView) view.findViewById(R.id.discountIcon);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, DiscountActivity.class);
                intent.putExtra("discount", d);
                context.startActivity(intent);
            }
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    view.setBackgroundResource(R.drawable.discount_row_touched);

                } else /*if (action == MotionEvent.ACTION_UP) */ {
                    view.setBackgroundResource(R.drawable.discount_row);

                }

                return false;
            }
        });


        HelloClient client = new HelloClient();
        client.downloadBitmap(String.format(HelloClient.DISCOUNT_ICON_URL,
                d.getId()), imageView);

        return view;
    }


}

