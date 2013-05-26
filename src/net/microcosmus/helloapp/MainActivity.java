package net.microcosmus.helloapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.EasyTracker;
import net.microcosmus.helloapp.HelloApp.R;
import net.microcosmus.helloapp.domain.AppVersion;
import net.microcosmus.helloapp.domain.Campaign;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "user-pref";
    public static final String ICON_FILE_PATH_TMPL = "camp-%d-icon.jpg";

    public static final String CAT = "MainActivity";
    public static final String PARAM_WHEN_DATA_RETRIEVED = "when-data-retrieved";
    public static final long SECONDS_IN_DAY = 24 * 60 * 60;
    public static final long DATA_EXPIRES_AFTER_SECONDS = SECONDS_IN_DAY;

    private LayoutInflater inflater;
    private CampaignStorage storage;
    private boolean networkAvailable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        EasyTracker.getInstance().setContext(this);

        setContentView(R.layout.main);
        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        HelloClient.authorize(); //todo this is test stuff!

        final AppVersion appVersion = getAppVersion();

        TextView versionInfoView = (TextView) findViewById(R.id.maVersionInfo);
        versionInfoView.setText(appVersion.getVersionName() + " (Test)");

        storage = new CampaignStorage(this);

        boolean loadDataFromLocalStorage = true;
        networkAvailable = checkNetworkAvailable();

        if (networkAvailable) {

            Date time = getWhenDataRetrieved();

            if (time == null || time.before(getExpirationLimit())) {
                asyncCheckNewerVersion(appVersion);

                Log.i(CAT, "Campaigns are out of date, sending request for newer data...");
                clearDiscountView();
                showDownloadProgress();
                downloadCampaigns();

                loadDataFromLocalStorage = false;
            }
        }

        if (loadDataFromLocalStorage) {
            Log.i(CAT, "Retrieving campaigns from storage...");

            List<Campaign> campaigns = storage.getCampaigns();
            if (campaigns != null && !campaigns.isEmpty()) {
                Log.i(CAT, "Got " + campaigns.size() + " campaigns.");
                addCampaignsToView(campaigns);

            } else {
                String erMes = getResources().getString(R.string.internet_access);
                Toast toast = Toast.makeText(this, erMes, Toast.LENGTH_LONG);
                toast.show();
            }

        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
        BugSenseHandler.initAndStartSession(MainActivity.this, "49791bfe");
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
        BugSenseHandler.closeSession(MainActivity.this);
    }

    //----------------------------------------------------


    private void showDiscountActivity(Campaign c) {
        if (c == null) return;
        Intent intent = new Intent(this, DiscountActivity.class);
        intent.putExtra("campaign", c);
        startActivity(intent);
    }

    //--------------------------------------------


    private void showDownloadProgress() {
        ProgressBar waiter = (ProgressBar) findViewById(R.id.progressBar);
        waiter.setVisibility(View.VISIBLE);
    }

    private void hideDownloadProgress() {
        ProgressBar waiter = (ProgressBar) findViewById(R.id.progressBar);
        waiter.setVisibility(View.GONE);
    }

    //--------------------------------------------


    private void clearDiscountView() {
        LinearLayout listView = (LinearLayout) findViewById(R.id.listView);
        listView.removeAllViews();
    }

    private void addCampaignsToView(List<Campaign> campaigns) {
        if (campaigns == null) return;

        LinearLayout listView = (LinearLayout) findViewById(R.id.listView);
        for (Campaign c : campaigns) {
            Log.i(CAT, "Discount: " + c.getTitle() + "\t" + c.getPlace());

            View view = createCampaignView(c, listView);
            listView.addView(view);

            ImageView campaignThumbs = (ImageView) view.findViewById(R.id.campaignThumbnail);
            ImageView discountIcon = (ImageView) view.findViewById(R.id.discountIcon);

            CanvasUtils.buildDiscountRateIcon(discountIcon, c.getRate(), 56);

            Bitmap bitmap = getIconFromFile(c.getId());
            if (bitmap != null) {
                campaignThumbs.setImageBitmap(bitmap);

            } else {
                if (networkAvailable) {
                    downloadCampaignIcon(c, campaignThumbs);
                }
            }
        }
    }

    private View createCampaignView(final Campaign с, ViewGroup parent) {

        View view = inflater.inflate(R.layout.campaign, parent, false);

        TextView titleView = (TextView) view.findViewById(R.id.discountTitle);
        TextView placeView = (TextView) view.findViewById(R.id.discountPlace);

        titleView.setText(с.getTitle());
        placeView.setText(с.getPlace());

        ImageView edgeImg = (ImageView) view.findViewById(R.id.edgeImg);
        CanvasUtils.buildBentEdge(edgeImg, 10);

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


    //--------------------------------------------


    private void downloadCampaigns() {
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

                hideDownloadProgress();
                if (campaigns != null) {
                    addCampaignsToView(campaigns);
                    storage.clearCampaigns();
                    storage.persistCampaigns(campaigns);
                    saveWhenDataRetrieved(new Date());
                }
            }

        }.execute(HelloClient.CAMPAIGNS_URL /*url*/);
    }


    private void downloadCampaignIcon(final Campaign c, final ImageView imageView) {
        String url = String.format(HelloClient.CAMPAIGN_ICON_URL, c.getId());

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

                saveIconToFile(c.getId(), bitmap);
                imageView.setImageBitmap(bitmap);
            }
        }.execute(url);
    }

    private void saveWhenDataRetrieved(Date date) {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PARAM_WHEN_DATA_RETRIEVED, date.getTime());
        editor.commit();
    }

    private Date getWhenDataRetrieved() {
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
        long time = pref.getLong(PARAM_WHEN_DATA_RETRIEVED, 0);
        return time > 0 ? new Date(time) : null;
    }

    private Date getExpirationLimit() {
        long now = System.currentTimeMillis();
        return new Date(now - DATA_EXPIRES_AFTER_SECONDS * 1000);
    }

    //--------------------------------------------


    public static Bitmap getIconFromFile(long campaignId) {
        File file = new File(getAppFilesHome(), String.format(ICON_FILE_PATH_TMPL, campaignId));
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }

        return null;
    }

    public static void saveIconToFile(long campaignId, Bitmap bitmap) {
        if (bitmap == null) return;
        File file = new File(getAppFilesHome(), String.format(ICON_FILE_PATH_TMPL, campaignId));
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Exception e) {
            Log.w(CAT, "Error writing " + file, e);
        }
    }

    public static String getAppFilesHome() {
        File appFiles = new File(Environment.getExternalStorageDirectory(),
                "/Android/data/net.microcosmus.helloapp/files");
        if (!appFiles.exists()) {
            if (!appFiles.mkdirs()) {
                Log.w(CAT, "Could not create app files home!");
            }
        }

        return appFiles.getAbsolutePath();
    }


    //--------------------------------------------

    private boolean checkNetworkAvailable() {
        ConnectivityManager manager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }


    //--------------------------------------------


    private void asyncCheckNewerVersion(final AppVersion appVersion) {
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

    //--------------------------------------------


    private boolean checkAuthorized() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long userId = settings.getLong("user-id", -1);
        return userId > 0;
    }


}

