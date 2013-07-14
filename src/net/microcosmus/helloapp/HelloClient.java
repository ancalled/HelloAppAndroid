package net.microcosmus.helloapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import net.microcosmus.helloapp.domain.AppVersion;
import net.microcosmus.helloapp.domain.Campaign;
import net.microcosmus.helloapp.domain.DiscountApplyResult;
import net.microcosmus.helloapp.domain.User;
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
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class HelloClient {

    public static final String SCHEME = "http";

//    public static final String HOST = "helloapp.microcosmus.net";
//    public static final int PORT = 80;

    public static final String HOST = "10.0.2.2";
    public static final int PORT = 8080;

    public static final String SERVER_URL = SCHEME + "://" + HOST + ":" + PORT + "/helloapp";

    public static final String API_URL = SERVER_URL + "/customer/api";

    public static final String ACTION_REGISTER = "-register";
    public static final String ACTION_AUTH = "-auth";
    public static final String ACTION_CAMPAIGNS = "/campaigns";
    public static final String ACTION_APPLY_DISCOUNT = "/apply-campaign";

    public static final String CAMPAIGN_ICON_URL = SERVER_URL + "/images/camp-prev/%d.png";

    public static final String APPLICATION_VERSION = "http://kinok.org/helloapp/builds/version.json";
    public static final String APPLICATION_DOWNLOAD_URL = "http://kinok.org/helloapp";

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddhhmmss");


    private User user;


    public HelloClient() {
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }


    public static enum RequestType {
        GET, POST
    }

    public void authorize() {

    }


    public void apiCall(ApiTask task) {
        if (user != null && user.getId() != null && user.getToken() != null) {
            task.param("uid", Long.toString(user.getId()))
                    .param("t", TIMESTAMP_FORMAT.format(new Date()))
                    .execute(user.getToken());
        } else {
            task.param("t", TIMESTAMP_FORMAT.format(new Date()))
                    .execute();
        }
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


//    --------------------------------------------------------------


    public static Bitmap downloadBitmap(String url) {
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


//    ---------------------------------------------------------

    public static String parseStatus(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            return jsonObj.getString("status");
        } catch (JSONException e) {
            return null;
        }
    }

    public static User parseUser(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            User user = new User();

            user.setId(jsonObj.getLong("userId"));
            user.setName(jsonObj.getString("login"));
            user.setToken(jsonObj.getString("token"));

            return user;
        } catch (JSONException e) {
            return null;
        }
    }


    public static List<Campaign> parseCampaigns(String json) throws JSONException {
        JSONArray jsonArray = new JSONArray(json);

        List<Campaign> result = new ArrayList<Campaign>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject discountObj = jsonArray.getJSONObject(i);
            Campaign c = new Campaign();
            c.setId(discountObj.getLong("id"));
            c.setTitle(discountObj.getString("title"));
            c.setRate(discountObj.getInt("rate"));
            c.setGoodThrough(parseDate(discountObj, "goodThrough"));
            c.setStartFrom(parseDate(discountObj, "startFrom"));
            c.setNeedConfirm(discountObj.getBoolean("needConfirm"));

            JSONObject companyObj = discountObj.getJSONObject("company");
            c.setPlace(companyObj.getString("name"));

            result.add(c);
        }

        return result;
    }

    private static Date parseDate(JSONObject obj, String field) throws JSONException {
        String value = obj.getString(field);
        if (value != null) {
            try {
                return DATE_FORMAT.parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    public static DiscountApplyResult parseDiscountApplyResult(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);

        DiscountApplyResult result = new DiscountApplyResult();

        String strStatus = obj.getString("status");

        if ("auth-error".equals(strStatus)) return null;

        DiscountApplyResult.Status status = DiscountApplyResult.Status.valueOf(strStatus);
        result.setStatus(status);

        if (status == DiscountApplyResult.Status.OK) {
            result.setId(obj.getLong("appliedId"));
        }

        return result;
    }

    public static AppVersion parseVersion(String json) throws JSONException {
        if (json == null) return null;

        JSONObject obj = new JSONObject(json);

        AppVersion result = new AppVersion();

        int version = obj.getInt("version");
        int minVersion = obj.getInt("minVersion");
        String name = obj.getString("versionName");


        result.setVersion(version);
        result.setMinVersion(minVersion);
        result.setVersionName(name);

        return result;
    }


    public abstract static class ApiTask extends AsyncTask<String, Void, String> {

        private final RequestBuilder builder;
        private final RequestType type;

        public ApiTask(RequestType type, String action) {
            this(type, API_URL, action);
        }

        public ApiTask(RequestType type, String apiUrl, String action) {
            this.builder = new RequestBuilder(apiUrl + action);
            this.type = type;
        }

        public ApiTask param(String name, Object value) {
            builder.param(name, value.toString());
            return this;
        }


        @Override
        protected String doInBackground(String... params) {
            if (params != null && params.length > 0) {
                String token = params[0];
                builder.setToken(token);
            }

            String url = builder.build();

            Log.i("HelloClient", "Sending " + type + "-request to " + url);
            return doRequest(url, type);
        }

        @Override
        protected void onPostExecute(String s) {
            onResponse(s);
        }

        protected abstract void onResponse(String s);
    }


    public static class RequestBuilder {

        private static final Map<String, String> params = new TreeMap<String, String>();

        private final String url;
        private String token;

        public RequestBuilder(String url) {
            this.url = url;
        }

        public RequestBuilder param(String name, String value) {
            params.put(name, value);
            return this;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String build() {
            StringBuilder paramBuf = new StringBuilder();
            int i = 0;
            for (String key : params.keySet()) {
                paramBuf.append(key).append("=").append(params.get(key));
                if (++i < params.size()) {
                    paramBuf.append("&");
                }
            }
            String params = paramBuf.toString();

            StringBuilder urlBuf = new StringBuilder();
            urlBuf.append(url).append("?");
            urlBuf.append(params);

            if (token != null) {
                String hash = buildHash(params, token);
                urlBuf.append("&h=").append(encode(hash));
            }
            return urlBuf.toString();
        }

        public static String encode(String text) {
            try {
                return URLEncoder.encode(text, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String buildHash(String params, String token) {
            try {
                String data = params + "&token=" + token;
                return calcHash(data);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }


    }

    public static String calcHash(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = data.getBytes("UTF-8");
        byte[] digest = md.digest(bytes);
//            String hash = Base64.encodeToString(digest, Base64.DEFAULT);
//            if (hash.endsWith("\n")) {
//                hash = hash.substring(0, hash.length() - 1);
//            }
        return toHex(digest);
    }

    public static String toHex(byte[] bytes) {
        return String.format("%040x", new BigInteger(bytes));
    }


//------------------------------------------------------------


//    public static final String CONSUMER_KEY = "1243";
//    public static final String CONSUMER_SECRET = "abgb";
//    public static final String ACCESS_TOKEN = "12434";
//    public static final String TOKEN_SECRET = "3535";
//
//
//    public static void signWithOAuth() throws IOException,
//            OAuthCommunicationException,
//            OAuthExpectationFailedException,
//            OAuthMessageSignerException {
//
//        // create a consumer object and configure it with the access
//        // token and token secret obtained from the service provider
//        OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
//        consumer.setTokenWithSecret(ACCESS_TOKEN, TOKEN_SECRET);
//
//        // create an HTTP request to a protected resource
//        URL url = new URL("http://example.com/protected");
//        HttpURLConnection request = (HttpURLConnection) url.openConnection();
//
//        // sign the request
//        consumer.sign(request);
//
//        // send the request
//        request.connect();
//    }


}
