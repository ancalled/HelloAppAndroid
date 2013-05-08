package net.microcosmus.helloapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.bugsense.trace.BugSenseHandler;
import com.example.AndroidTest.R;
import com.google.analytics.tracking.android.EasyTracker;
import net.microcosmus.helloapp.domain.AppVersion;
import net.microcosmus.helloapp.domain.Campaign;
import org.json.JSONException;

import java.util.List;

public class MainActivity extends Activity {

    public static final String CAT = "MainActivity";

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
            checkNewerVersion();

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

    private void addCampaigns(List<Campaign> campaigns) {
        if (campaigns == null) return;

        LinearLayout listView = (LinearLayout) findViewById(R.id.listView);
        for (Campaign c : campaigns) {
            Log.i(CAT, "Discount: " + c.getTitle() + "\t" + c.getPlace());
            View view = createCampaign(c, listView);
            listView.addView(view);

            ImageView imageView = (ImageView) view.findViewById(R.id.discountIcon);
            downloadIcon(c, imageView);
        }
    }


    private View createCampaign(final Campaign d, ViewGroup parent) {

        View view = inflater.inflate(R.layout.campaign, parent, false);

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
        new AsyncTask<String, Void, List<Campaign>>() {
            @Override
            protected List<Campaign> doInBackground(String... params) {
                String response = HelloClient.doGet(params[0]);
                try {
                    return HelloClient.parseCampaigns(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<Campaign> campaigns) {
                hideWaiter();
                addCampaigns(campaigns);
            }

        }.execute(HelloClient.CAMPAIGNS_URL);
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


    private void showDiscountActivity(Campaign c) {
        Intent intent = new Intent(this, DiscountActivity.class);
        intent.putExtra("campaign", c);
        startActivity(intent);
    }


    private void checkNewerVersion() {
        final AppVersion appVersion = getAppVersion();
        Log.d(CAT, "Current app version: " + appVersion.getVersion() + " (" + appVersion.getVersionName() + ")");

        new AsyncTask<String, Void, AppVersion>() {
            @Override
            protected AppVersion doInBackground(String... params) {
                String response = HelloClient.doGet(params[0]);
                try {
                    return HelloClient.parseVersion(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(AppVersion version) {
                if (version == null) {
                    Log.e(CAT, "Version is null");
                    return;
                }

                Log.d(CAT, "Available app version: " + version.getVersion() + " (" + version.getVersionName() + ")");

                if (version.getMinVersion() > appVersion.getVersion()) {
                    showForceDownloadNewerVersion(appVersion.getVersionName());
                } else if (version.getVersion() > appVersion.getVersion()) {
                    showDownloadNewerVersion(appVersion.getVersionName());
                }

            }

        }.execute(HelloClient.APPLICATION_VERSION);
    }


    private AppVersion getAppVersion() {
        PackageManager pm = getPackageManager();
        final PackageInfo pinfo;
        try {
            pinfo = pm.getPackageInfo(getPackageName(), 0);

            AppVersion version = new AppVersion();
            version.setVersion(pinfo.versionCode);
            version.setVersionName(pinfo.versionName);
            return version;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    private void showDownloadNewerVersion(String newVersionName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String mes = getResources().getString(R.string.update_to_newer_version);
        mes = String.format(mes, newVersionName);

        builder.setTitle(R.string.updater_title)
                .setMessage(mes);

        builder.setPositiveButton(R.string.updater_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(HelloClient.APPLICATION_DOWNLOAD_URL));
                startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.updater_canacel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        builder.create().show();
    }

    private void showForceDownloadNewerVersion(String newVersionName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String mes = getResources().getString(R.string.force_update_to_newer_version);
        mes = String.format(mes, newVersionName);

        builder.setTitle(R.string.updater_title)
                .setMessage(mes);

        builder.setPositiveButton(R.string.updater_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(HelloClient.APPLICATION_DOWNLOAD_URL));
                startActivity(intent);
            }
        });
//        builder.setNegativeButton(R.string.updater_canacel, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                // User cancelled the dialog
//            }
//        });

        builder.create().show();
    }



}

