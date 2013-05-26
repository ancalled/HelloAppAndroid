package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import com.google.analytics.tracking.android.EasyTracker;
import net.microcosmus.helloapp.HelloApp.R;
import net.microcosmus.helloapp.domain.User;
import org.json.JSONException;

public class AuthActivity extends Activity {


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        EasyTracker.getInstance().setContext(this);

        setContentView(R.layout.authorize);

        final EditText loginText = (EditText) findViewById(R.id.athLogin);
        final EditText passText = (EditText) findViewById(R.id.athPass);
        Button authButton = (Button) findViewById(R.id.athButton);
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String login = loginText.getText().toString();
                String pass = passText.getText().toString();

                if (!login.isEmpty() && !pass.isEmpty()) {
                    authorize(login, pass);
                }
            }
        });

    }

    private void authorize(String login, String pass) {
        String url = String.format(HelloClient.AUTH_URL, login, pass);

        new AsyncTask<String, Void, User>() {
            @Override
            protected User doInBackground(String... params) {
                try {
                    return HelloClient.parseUser(params[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(User user) {
                if (isCancelled()) {
                    user = null;
                }

                installUser(user);
            }
        }.execute(url);
    }

    private void installUser(User user) {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("user-id", user.getId());
        editor.putString("user-name", user.getName());
        editor.putString("uesr-token", user.getToken());

        // Commit the edits!
        editor.commit();
    }

}