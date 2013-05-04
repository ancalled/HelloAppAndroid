package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import com.example.AndroidTest.R;
import com.google.analytics.tracking.android.EasyTracker;
import net.microcosmus.helloapp.domain.Discount;
import net.microcosmus.helloapp.domain.User;
import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends Activity {


    @Override
    protected void onStart() {
        super.onStart();

        EasyTracker.getInstance().activityStart(this);
        BugSenseHandler.initAndStartSession(MainActivity.this, "49791bfe");

    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        EasyTracker.getInstance().setContext(this);

        setContentView(R.layout.main);

        HelloClient.authorize();
    }


    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("MainActivity.onResume");

        if (isNetworkAvailable()) {

            retrieveDiscounts();


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


    public static View createDiscountRow(final Discount d, final User u, final Context context, ViewGroup parent) {
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
                intent.putExtra("user", u);

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


        HelloClient.downloadBitmap(String.format(HelloClient.CAMPAIGN_ICON_URL,
                d.getId()), imageView);

        return view;
    }


    @Override
    protected void onStop() {
        super.onStop();

        EasyTracker.getInstance().activityStop(this);
        BugSenseHandler.closeSession(MainActivity.this);
    }

    private void retrieveDiscounts() {

        LinearLayout listView = (LinearLayout) findViewById(R.id.listView);

        final WeakReference<LinearLayout> layoutRef = new WeakReference<LinearLayout>(listView);
        final WeakReference<Context> contextRef = new WeakReference<Context>(this);
        final WeakReference<ViewGroup> parentRef = new WeakReference<ViewGroup>(listView);

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

                if (discounts != null) {
                    LinearLayout layout = layoutRef.get();
                    Context context = contextRef.get();
                    ViewGroup parent = parentRef.get();

                    User user = HelloClient.getUser();

                    if (context != null && layout != null && parent != null) {
                        for (Discount d : discounts) {
                            Log.i("Client", "Disc: " + d.getTitle() + "\t" + d.getPlace());
                            View view = MainActivity.createDiscountRow(d, user, context, parent);
                            layout.addView(view);
                        }
                    }
                }

            }
        }.execute(HelloClient.CAMPAIGNS_URL);
    }
}

