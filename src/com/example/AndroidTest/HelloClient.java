package com.example.AndroidTest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class HelloClient {


    //    public static final String SERVER_URL = "http://10.0.2.2:8080/helloapp";
    public static final String SERVER_URL = "http://helloapp.microcosmus.net/helloapp";
    public static final String DISCOUNTS_URL = SERVER_URL + "/discounts";

    public static final String DISCOUNT_ICON_URL = SERVER_URL + "/resources/icons/%d.png";
    public static final String APPLY_DISCOUNT_URL = SERVER_URL + "/apply-discount?userId=%d&discountId=%d";

    public void retreiveDiscounts(LinearLayout layout, Context context, ViewGroup parent) {
        DiscountsTask task = new DiscountsTask(/*adapter*/layout, context, parent);
        task.execute(DISCOUNTS_URL);
    }

    public void applyDiscount(long userId, long discountId, TextView messageView, TextView numberView, ProgressBar progressBar) {
        ApplyDiscountsTask task = new ApplyDiscountsTask(messageView, numberView, progressBar);
        task.execute(String.format(APPLY_DISCOUNT_URL, userId, discountId));
    }


    public void downloadBitmap(String url, ImageView imageView) {
        BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
        task.execute(url);
    }


    public static class DiscountsTask extends AsyncTask<String, Void, List<Discount>> {

        private final WeakReference<LinearLayout> layoutRef;
        private final WeakReference<Context> contextRef;
        private final WeakReference<ViewGroup> parentRef;

        public DiscountsTask(LinearLayout layout, Context context, ViewGroup parent) {
            this.layoutRef = new WeakReference<LinearLayout>(layout);
            this.contextRef = new WeakReference<Context>(context);
            this.parentRef = new WeakReference<ViewGroup>(parent);
        }

        @Override
        // Actual download method, run in the task thread
        protected List<Discount> doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            String response = doGet(params[0]);
            try {
                return parseDiscount(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(List<Discount> discounts) {
            if (discounts != null) {
                LinearLayout layout = layoutRef.get();
                Context context = contextRef.get();
                ViewGroup parent = parentRef.get();

                if (context != null && layout != null && parent != null) {
                    for (Discount d : discounts) {
                        Log.i("Client", "Disc: " + d.getTitle() + "\t" + d.getPlace());
                        View view = MainActivity.createDiscountRow(d, context, parent);
                        layout.addView(view);
                    }
                }
            }
        }

        static List<Discount> parseDiscount(String json) throws JSONException {
            JSONArray jsonArray = new JSONArray(json);

            List<Discount> result = new ArrayList<Discount>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Discount d = new Discount();
                d.setId(obj.getLong("id"));
                d.setTitle(obj.getString("title"));
                d.setPlace(obj.getString("place"));
                d.setRate(obj.getInt("rate"));
//            d.setGoodThrough(obj.getString("goodThrough"));
                result.add(d);
            }

            return result;
        }
    }


    public static class ApplyDiscountsTask extends AsyncTask<String, Void, ApplyResult> {

        private final WeakReference<TextView> messageViewRef;
        private final WeakReference<TextView> numberViewRef;
        private final WeakReference<ProgressBar> progressBarRef;

        public ApplyDiscountsTask(TextView messageView, TextView numberView, ProgressBar progressBar) {
            this.messageViewRef = new WeakReference<TextView>(messageView);
            this.numberViewRef = new WeakReference<TextView>(numberView);
            this.progressBarRef = new WeakReference<ProgressBar>(progressBar);
        }

        @Override
        // Actual download method, run in the task thread
        protected ApplyResult doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            String response = doPost(params[0]);
            try {
                return parseResult(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(ApplyResult result) {
            if (result != null) {

                TextView messageView = messageViewRef.get();
                TextView numberView = numberViewRef.get();
                ProgressBar progressBar = progressBarRef.get();

                if (messageView != null && numberView != null && progressBar != null) {

                    messageView.clearComposingText();
                    if (result.getStatus() == ApplyResult.Status.OK) {
                        progressBar.setVisibility(View.GONE);
                        messageView.setVisibility(View.VISIBLE);
                        numberView.setVisibility(View.VISIBLE);

                        numberView.setText(Long.toString(result.getAppliedId()));
                    }
                }
            }
        }

        static ApplyResult parseResult(String json) throws JSONException {
            JSONObject jsonObject = new JSONObject(json);

            ApplyResult result = new ApplyResult();
            result.setAppliedId(jsonObject.getLong("appliedId"));
            String strStatus = jsonObject.getString("status");
            result.setStatus(ApplyResult.Status.valueOf(strStatus));

            return result;
        }
    }


    public static class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;

        public BitmapDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        // Actual download method, run in the task thread
        protected Bitmap doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            return downloadBitmap(params[0]);
        }

        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

        static Bitmap downloadBitmap(String url) {
            final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            final HttpGet getRequest = new HttpGet(url);

            try {
                HttpResponse response = client.execute(getRequest);
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
                    return null;
                }

                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = null;
                    try {
                        inputStream = entity.getContent();
                        final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Log.i("ImageDownloader", "Got bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        return bitmap;
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        entity.consumeContent();
                    }
                }
            } catch (Exception e) {
                // Could provide a more explicit error message for IOException or IllegalStateException
                getRequest.abort();
                Log.w("ImageDownloader", "Error while retrieving bitmap from " + url);
            } finally {
                if (client != null) {
                    client.close();
                }
            }
            return null;
        }
    }

    public static enum RequestType {
        GET, POST
    }


    public static String doGet(String url) {
        return doRequest(url, RequestType.GET);
    }

    public static String doPost(String url) {
        return doRequest(url, RequestType.POST);
    }

    public static String doRequest(String url, RequestType requestType) {
        try {
            // Create a new HTTP Client
            DefaultHttpClient client = new DefaultHttpClient();
            // Setup the get request

            HttpRequestBase request;
            if (requestType == RequestType.GET) {
                request = new HttpGet(url);
            } else {
                request = new HttpPost(url);
            }

            // Execute the request in the client
            HttpResponse response = client.execute(request);
            // Grab the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent(), "UTF-8"));

            StringBuilder buf = new StringBuilder();
            while (true) {
                String line = reader.readLine();

                if (line == null) break;

                buf.append(line).append("\n");
            }

            String respText = buf.toString();

            Log.i("Client", "Got response: \n" + respText);

            // Instantiate a JSON object from the request response

            return respText;

        } catch (Exception e) {
            // In your production code handle any errors and catch the individual exceptions
            e.printStackTrace();
        }

        return null;
    }

}
