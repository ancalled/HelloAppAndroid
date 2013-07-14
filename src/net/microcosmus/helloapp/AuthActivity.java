package net.microcosmus.helloapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.analytics.tracking.android.EasyTracker;
import net.microcosmus.helloapp.HelloApp.R;
import net.microcosmus.helloapp.domain.User;

import static net.microcosmus.helloapp.HelloClient.*;
import static net.microcosmus.helloapp.HelloClient.RequestType.POST;

public class AuthActivity extends Activity {


    public static final String STATUS_OK = "OK";
    public static final String STATUS_FAIL = "FAIL";
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";


    private ProgressBar progressBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        EasyTracker.getInstance().setContext(this);

        setContentView(R.layout.authorize);

        final EditText loginText = (EditText) findViewById(R.id.athLogin);
        final EditText passText = (EditText) findViewById(R.id.athPass);

        loginText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    hideKeyboard(loginText);
                    return true;
                }
                return false;
            }
        });

        passText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    hideKeyboard(passText);
                    return true;
                }
                return false;
            }
        });

        Button authButton = (Button) findViewById(R.id.athButton);
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String login = loginText.getText().toString();
                String pass = passText.getText().toString();

                if (!login.isEmpty() && !pass.isEmpty()) {
                    hideKeyboard(passText);
                    authorize(login.trim(), pass.trim());
                }
            }
        });

        Button regButton = (Button) findViewById(R.id.regButton);
        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String login = loginText.getText().toString();
                String pass = passText.getText().toString();

                if (!login.isEmpty() && !pass.isEmpty()) {
                    hideKeyboard(passText);
                    register(login.trim(), pass.trim());
                }
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.athProgressBar);
        progressBar.setVisibility(View.GONE);
    }


    private void authorize(String login, String pass) {
        progressBar.setVisibility(View.VISIBLE);

        new HelloClient.ApiTask(POST, API_URL, ACTION_AUTH) {
            @Override
            protected void onResponse(String s) {
                progressBar.setVisibility(View.GONE);
                TextView authResult = (TextView) findViewById(R.id.authResult);
                authResult.setVisibility(View.VISIBLE);

                String status = HelloClient.parseStatus(s);
                if (STATUS_OK.equals(status)) {

                    User user = HelloClient.parseUser(s);
                    if (user != null) {
                        authResult.setText(getResources().getString(R.string.authOk));
                        backToMain(user);
                    } else {
                        authResult.setText(getResources().getString(R.string.authError));
                    }

                } else if (STATUS_FAIL.equals(status)) {
                    authResult.setText(getResources().getString(R.string.authFail));

                } else {
                    authResult.setText(getResources().getString(R.string.authError));
                }
            }
        }.param("l", login)
                .param("p", pass)
                .execute();
    }


    private void register(String login, String pass) {
        progressBar.setVisibility(View.VISIBLE);

        new HelloClient.ApiTask(POST, API_URL, ACTION_REGISTER) {
            @Override
            protected void onResponse(String s) {
                progressBar.setVisibility(View.GONE);
                TextView authResult = (TextView) findViewById(R.id.authResult);
                authResult.setVisibility(View.VISIBLE);

                String status = HelloClient.parseStatus(s);

                if (STATUS_OK.equals(status)) {

                    User user = HelloClient.parseUser(s);
                    if (user != null) {
                        authResult.setText(getResources().getString(R.string.authOk));
                        backToMain(user);
                    } else {
                        authResult.setText(getResources().getString(R.string.authFail));
                    }

                } else if (USER_ALREADY_EXISTS.equals(status)) {
                    authResult.setText(getResources().getString(R.string.authAlreadyExists));

                } else if (STATUS_FAIL.equals(status)) {
                    authResult.setText(getResources().getString(R.string.authFail));

                } else {
                    authResult.setText(getResources().getString(R.string.authError));
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


    private void hideKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

}