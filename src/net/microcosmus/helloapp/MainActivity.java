package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.bugsense.trace.BugSenseHandler;
import com.example.AndroidTest.R;
import com.google.analytics.tracking.android.EasyTracker;
import net.microcosmus.helloapp.domain.Discount;
import org.json.JSONException;

import java.util.List;

public class MainActivity extends Activity {

    private LayoutInflater inflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        EasyTracker.getInstance().setContext(this);

        setContentView(R.layout.main);
        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        HelloClient.authorize();
    }


    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
        BugSenseHandler.initAndStartSession(MainActivity.this, "49791bfe");

        if (isNetworkAvailable()) {
            clearDiscountView();
            showWaiter();
            retrieveDiscounts();

        } else {
            String erMes = getResources().getString(R.string.internet_access);
            Toast toast = Toast.makeText(this, erMes, Toast.LENGTH_LONG);
            toast.show();
        }

    }


    private boolean isNetworkAvailable() {
        ConnectivityManager manager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }


    private void clearDiscountView() {
        LinearLayout listView = (LinearLayout) findViewById(R.id.listView);
        listView.removeAllViews();
    }


    private void showWaiter() {
        ProgressBar waiter = (ProgressBar) findViewById(R.id.progressBar);
        waiter.setVisibility(View.VISIBLE);
    }

    private void hideWaiter() {
        ProgressBar waiter = (ProgressBar) findViewById(R.id.progressBar);
        waiter.setVisibility(View.GONE);
    }

    private void addDiscounts(List<Discount> discounts) {
        if (discounts == null) return;

        LinearLayout listView = (LinearLayout) findViewById(R.id.listView);
        for (Discount d : discounts) {
            Log.i("MainActivity", "Discount: " + d.getTitle() + "\t" + d.getPlace());
            View view = createDiscountRow(d, listView);
            listView.addView(view);

            ImageView imageView = (ImageView) view.findViewById(R.id.discountIcon);
            downloadIcon(d, imageView);
        }
    }


    private View createDiscountRow(final Discount d, ViewGroup parent) {

        View view = inflater.inflate(R.layout.discount_row, parent, false);

        TextView titleView = (TextView) view.findViewById(R.id.discountTitle);
        TextView placeView = (TextView) view.findViewById(R.id.discountPlace);
        TextView rateView = (TextView) view.findViewById(R.id.discountRate);

        titleView.setText(d.getTitle());
        placeView.setText(d.getPlace());
        rateView.setText("-" + d.getRate() + "%");

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.setBackgroundResource(
                        motionEvent.getAction() == MotionEvent.ACTION_DOWN ?
                                R.drawable.discount_row_touched :
                                R.drawable.discount_row);
                return false;
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDiscountActivity(d);
            }
        });

        return view;
    }


    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
        BugSenseHandler.closeSession(MainActivity.this);
    }


    private void retrieveDiscounts() {
        new AsyncTask<String, Void, List<Discount>>() {
            @Override
            protected List<Discount> doInBackground(String... params) {
                String response = HelloClient.doGet(params[0]);
                try {
                    return HelloClient.parseDiscount(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<Discount> discounts) {
                hideWaiter();
                addDiscounts(discounts);
            }

        }.execute(HelloClient.CAMPAIGNS_URL);
    }


    private void downloadIcon(Discount d, final ImageView imageView) {
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


    private void showDiscountActivity(Discount d) {
        Intent intent = new Intent(this, DiscountActivity.class);
        intent.putExtra("discount", d);
        startActivity(intent);
    }


}

