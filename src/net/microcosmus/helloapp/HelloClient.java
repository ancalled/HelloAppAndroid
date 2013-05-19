package net.microcosmus.helloapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Base64;
import android.util.Log;
import net.microcosmus.helloapp.domain.AppVersion;
import net.microcosmus.helloapp.domain.Campaign;
import net.microcosmus.helloapp.domain.DiscountApplyResult;
import net.microcosmus.helloapp.domain.User;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class HelloClient {

    public static final String SCHEME = "http";

    public static final String HOST = "helloapp.microcosmus.net";
    public static final int PORT = 80;

//    public static final String HOST = "10.0.2.2";
//    public static final int PORT = 8080;

    public static final String SERVER_URL = SCHEME + "://" + HOST + ":" + PORT + "/helloapp";
    public static final String CAMPAIGNS_URL = SERVER_URL + "/customer/api/campaigns";
    public static final String CAMPAIGN_ICON_URL = SERVER_URL + "/images/camp-prev/%d.png";
    public static final String APPLY_CAMPAIGN_URL = SERVER_URL + "/customer/api/apply-campaign?userId=%d&campaignId=%d&confirmerCode=%s";

    public static final String APPLICATION_VERSION = "http://kinok.org/helloapp/builds/version.json";
    public static final String APPLICATION_DOWNLOAD_URL = "http://kinok.org/helloapp";

    private static User user;


    private HelloClient() {
    }

    public static void authorize() {
        //todo auth request

        user = new User();
        user.setId(1L);
        user.setName("testuser1");
    }

    public static User getUser() {
        return user;
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

    public static List<Campaign> parseCampaigns(String json) throws JSONException {
        JSONArray jsonArray = new JSONArray(json);

        List<Campaign> result = new ArrayList<Campaign>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject discountObj = jsonArray.getJSONObject(i);
            Campaign с = new Campaign();
            с.setId(discountObj.getLong("id"));
            с.setTitle(discountObj.getString("title"));
            с.setRate(discountObj.getInt("rate"));
//            с.setGoodThrough(obj.getString("goodThrough"));

            JSONObject companyObj = discountObj.getJSONObject("company");
            с.setPlace(companyObj.getString("name"));

            result.add(с);
        }

        return result;
    }

    public static DiscountApplyResult parseDiscountApplyResult(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);

        DiscountApplyResult result = new DiscountApplyResult();

        String strStatus = obj.getString("status");
        DiscountApplyResult.Status status = DiscountApplyResult.Status.valueOf(strStatus);
        result.setStatus(status);

        if (status == DiscountApplyResult.Status.OK) {
            result.setId(obj.getLong("appliedId"));
        }

        return result;
    }

    public static AppVersion parseVersion(String json) throws JSONException {
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


    //    --------------------------------------------------------------


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


    public static class RequestBuilder {

        private static final Map<String, String> params = new TreeMap<String, String>();

        private final String scheme;
        private final String host;
        private final int port;
        private final String contextPath;
        private final String token;

//
        private RequestBuilder(String scheme, String host, int port, String contextPath, String token) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.contextPath = contextPath;
            this.token = token;
        }



        public RequestBuilder param(String name, String value) {
            params.put(name, value);
            return this;
        }

        public String build() {
            StringBuilder buf = new StringBuilder();
            for (String key: params.keySet()) {
                buf.append(key).append("=").append(params.get(key));
            }

            buf.append("h=").append(buildHash(buf.toString()));

            return buf.toString();
        }

        private String buildHash(String url) {
            try {
                return calcHash(url + "token=" + token);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return null;
        }

        private String calcHash(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = data.getBytes("UTF-8");
            byte[] digest = md.digest(bytes);
            return Base64.encodeToString(digest, Base64.DEFAULT);
        }

        public static RequestBuilder create(String scheme, String host, int port, String contextPath, String token) {
            return new RequestBuilder(scheme, host, port, contextPath, token);
        }
    }


//------------------------------------------------------------


    public static final String CONSUMER_KEY = "1243";
    public static final String CONSUMER_SECRET = "abgb";
    public static final String ACCESS_TOKEN = "12434";
    public static final String TOKEN_SECRET = "3535";


    public static void signWithOAuth() throws IOException,
            OAuthCommunicationException,
            OAuthExpectationFailedException,
            OAuthMessageSignerException {

        // create a consumer object and configure it with the access
        // token and token secret obtained from the service provider
        OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
        consumer.setTokenWithSecret(ACCESS_TOKEN, TOKEN_SECRET);

        // create an HTTP request to a protected resource
        URL url = new URL("http://example.com/protected");
        HttpURLConnection request = (HttpURLConnection) url.openConnection();

        // sign the request
        consumer.sign(request);

        // send the request
        request.connect();
    }


}
