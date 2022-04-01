package com.roc.googlesignindemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // for one-tap sign in
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private static final int REQ_ONE_TAP = 2;  // Can be any integer unique to the Activity.

    // for google sign in
    private GoogleSignInClient mGoogleSignInClient;
    private static final int REQUEST_CODE_GOOGLE_SIGN_IN = 1; /* unique request id */

    private static final String TAG = "FUCK";
    private String webClientId = "1098155987904-qacv95n47rvt453v3snvhs92g2bpfst5.apps.googleusercontent.com";   // use web client id, not android client id
    private TextView text;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = findViewById(R.id.text);
        button = findViewById(R.id.button);
        button.setOnClickListener(this);

        initOneTapSignIn();
    }

    void initOneTapSignIn() {
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
//                .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
//                        .setSupported(true)
//                        .build())
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(webClientId)
                        // Only show accounts previously used to sign in.
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                // Automatically sign in when exactly one credential is retrieved.
//                .setAutoSelectEnabled(true)
                .build();
    }

    private void requestOneTapSignIn() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult result) {
                        try {
                            startIntentSenderForResult(
                                    result.getPendingIntent().getIntentSender(), REQ_ONE_TAP,
                                    null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // No saved credentials found. Launch the One Tap sign-up flow, or
                        // do nothing and continue presenting the signed-out UI.
                        Log.d(TAG, "oneTapClient.beginSignIn failed, " + e.getLocalizedMessage());
                    }
                });
    }

    private void requestGoogleSignIn() {
        GetSignInIntentRequest request =
                GetSignInIntentRequest.builder()
                        .setServerClientId(webClientId)
                        .build();

        Identity.getSignInClient(this)
                .getSignInIntent(request)
                .addOnSuccessListener(
                        result -> {
                            try {
                                startIntentSenderForResult(
                                        result.getIntentSender(),
                                        REQUEST_CODE_GOOGLE_SIGN_IN,
                                        /* fillInIntent= */ null,
                                        /* flagsMask= */ 0,
                                        /* flagsValue= */ 0,
                                        /* extraFlags= */ 0,
                                        /* options= */ null);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "Google Sign-in failed");
                            }
                        })
                .addOnFailureListener(
                        e -> {
                            Log.e(TAG, "Google Sign-in failed", e);
                        });
    }

    @Override
    public void onClick(View view) {
        requestGoogleSignIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_ONE_TAP:
                try {
                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                    String idToken = credential.getGoogleIdToken();
                    String username = credential.getId();
                    String password = credential.getPassword();
                    if (idToken !=  null) {
                        // Got an ID token from Google. Use it to authenticate
                        // with your backend.
                        text.setText(idToken);
                        Log.d(TAG, "Got ID token: " + idToken);
                    }
                    if (username != null) {
                        Log.d(TAG, "Got user name: " + username);
                    }
//                    if (password != null) {
//                        // Got a saved username and password. Use them to authenticate
//                        // with your backend.
//                        Log.d(TAG, "Got password: " + password);
//                    }
                } catch (ApiException e) {
                    // ...
                    switch (e.getStatusCode()) {
                        case CommonStatusCodes
                                .CANCELED:
                            Log.d(TAG, "user canceled one-tap sign in");
                            break;
                        case CommonStatusCodes.NETWORK_ERROR:
                            Log.d(TAG, "one-tap encountered a network error, try again");
                            break;
                        default:
                            Log.d(TAG, "not google accounts, " + e.getLocalizedMessage());
                            break;
                    }
                }
                break;
            case REQUEST_CODE_GOOGLE_SIGN_IN:
                try {
                    SignInCredential credential = Identity.getSignInClient(this).getSignInCredentialFromIntent(data);
                    // Signed in successfully - show authenticated UI
                    String idToken = credential.getGoogleIdToken();
                    if (idToken != null) {
                        text.setText(idToken);
                        Log.d(TAG, "Got ID token: " + idToken);
                    }
                } catch (ApiException e) {
                    // The ApiException status code indicates the detailed failure reason.
                    Log.d(TAG, "getSignInCredentialFromIntent failed, " + e.getLocalizedMessage());
                }
                break;
        }
    }
}
