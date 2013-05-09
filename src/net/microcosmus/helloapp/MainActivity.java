package net.microcosmus.helloapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
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

        final AppVersion appVersion = getAppVersion();

        TextView versionInfoView = (TextView) findViewById(R.id.versionInfo);
        versionInfoView.setText(appVersion.getVersionName() + " (Test)");

        if (isNetworkAvailable()) {

            checkNewerVersion(appVersion);

            clearDiscountView();
            showWaiter();
            retrieveDiscounts();

        } else {
            String erMes = getResources().getString(R.string.internet_access);
            Toast toast = Toast.makeText(this, erMes, Toast.LENGTH_LONG);
            toast.show();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
        BugSenseHandler.initAndStartSession(MainActivity.this, "49791bfe");



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

            ImageView campaignThumbs = (ImageView) view.findViewById(R.id.campaignThumbnail);
            ImageView discountIcon = (ImageView) view.findViewById(R.id.discountIcon);

            buildIconImage(discountIcon, c.getRate());
            downloadIcon(c, campaignThumbs);

        }
    }


    private View createCampaign(final Campaign с, ViewGroup parent) {

        View view = inflater.inflate(R.layout.campaign, parent, false);

        TextView titleView = (TextView) view.findViewById(R.id.discountTitle);
        TextView placeView = (TextView) view.findViewById(R.id.discountPlace);
//        TextView rateView = (TextView) view.findViewById(R.id.discountRate);

        titleView.setText(с.getTitle());
        placeView.setText(с.getPlace());
//        rateView.setText("-" + d.getRate() + "%");

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
                showDiscountActivity(с);
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


    private void buildIconImage(ImageView img, int rate) {
        img.setBackgroundColor(Color.TRANSPARENT);

        int width = 42;
        int height = 42;
        int x = width / 2;
        int y = height / 2;
        int rad1 = width / 2;
        int rad2 = width / 2 - 2;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Paint p0 = new Paint();
        p0.setARGB(256, 255, 255, 255);
        RectF rectF = new RectF();
        rectF.set(0,0, width, height);
        c.drawRoundRect(rectF, 0, 0, p0);

        Paint p1 = new Paint();
        p1.setColor(Color.WHITE);
        p1.setAntiAlias(true);
        c.drawCircle(x, y, rad1, p1);

        Paint p2 = new Paint();
        p2.setColor(Color.parseColor("#DD3E53"));
        p2.setAntiAlias(true);
        c.drawCircle(x, y, rad2, p2);

        float textHeight = 14;
        p1.setTextSize(textHeight);
        String rateText = rate + "%";
        float textWidth = p1.measureText(rateText);
        c.drawText(rateText, x - textWidth / 2, y + 5, p1);

        img.setBackgroundDrawable(new BitmapDrawable(bmp));

    }

    private void showDiscountActivity(Campaign c) {
        if (c == null) return;
        Intent intent = new Intent(this, DiscountActivity.class);
        intent.putExtra("campaign", c);
        startActivity(intent);
    }


    private void checkNewerVersion(final AppVersion appVersion) {
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
                    showForceDownloadNewerVersion(version.getVersionName());

                } else if (version.getVersion() > appVersion.getVersion()) {
                    showDownloadNewerVersion(version.getVersionName());
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

