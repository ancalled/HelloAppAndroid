package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.google.analytics.tracking.android.EasyTracker;
import net.microcosmus.helloapp.HelloApp.R;
import net.microcosmus.helloapp.domain.User;
import org.json.JSONException;

import static net.microcosmus.helloapp.HelloClient.ACTION_AUTH;
import static net.microcosmus.helloapp.HelloClient.RequestType.POST;
import static net.microcosmus.helloapp.HelloClient.SERVER_URL;

public class AuthActivity extends Activity {


    private ProgressBar progressBar;

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

        progressBar = (ProgressBar) findViewById(R.id.athProgressBar);
        progressBar.setVisibility(View.GONE);

    }


    private void authorize(String login, String pass) {
        progressBar.setVisibility(View.VISIBLE);

        new HelloClient.ApiTask(POST, SERVER_URL + "/customer/api-", ACTION_AUTH) {
            @Override
            protected void onResponse(String s) {
                progressBar.setVisibility(View.GONE);

                User user = null;
                try {
                    user = HelloClient.parseUser(s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (user != null) {
                    backToMain(user);
                }
            }
        }.param("l", login)
                .param("p", pass)
                .execute();
    }


    private void backToMain(User user) {

        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("user", user);
        setResult(RESULT_OK, intent);

        finish();
    }


}