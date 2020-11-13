package com.lopicard.sonial;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private GoogleApiClient googleApiClient;

    private CallbackManager callbackManager;
    private FacebookCallback<LoginResult> facebookCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        {
            GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
            this.googleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, options).build();
        }

        {
            this.callbackManager = CallbackManager.Factory.create();
            this.facebookCallback = new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    GraphRequest graphRequest = GraphRequest.newMeRequest(
                            loginResult.getAccessToken(),
                            new GraphRequest.GraphJSONObjectCallback() {
                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {
                                    try {
                                        Misc.Log(object.toString());
                                        SignInActivity.this.setDataAndGo(object.optString("name", null), object.optString("email", null), object.optJSONObject("picture").optJSONObject("data").optString("url", null));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                    );

                    Bundle bundle = new Bundle();
                    bundle.putString("fields", "id, name, email, cover, link, hometown, picture");
                    graphRequest.setParameters(bundle);
                    graphRequest.executeAsync();
                }

                @Override
                public void onCancel() {
                    Misc.Log("onCancel");
                }

                @Override
                public void onError(FacebookException error) {
                    Misc.Log(error.toString());
                }
            };

            LoginManager.getInstance().registerCallback(this.callbackManager, this.facebookCallback);
        }

        this.findViewById(R.id.btnGoogleSignIn).setOnClickListener(this);
        this.findViewById(R.id.btnFacebookSignIn).setOnClickListener(this);
        this.findViewById(R.id.btnRegister).setOnClickListener(this);
        this.findViewById(R.id.btnSkip).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200) {
            this.handleGoogleSignIn(Auth.GoogleSignInApi.getSignInResultFromIntent(data));
        } else
            this.callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Misc.Log("onConnectionFailed");
    }

    @Override
    public void onClick(View view) {
        try {
            if (view.getId() == R.id.btnSkip)
                this.moveTo(MainActivity.class);
            else if (view.getId() == R.id.btnRegister)
                this.moveTo(RegisterActivity.class);
            else if (view.getId() == R.id.btnFacebookSignIn)
                LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
            else if (view.getId() == R.id.btnGoogleSignIn)
                this.startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(this.googleApiClient), 200);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleGoogleSignIn(GoogleSignInResult signInResultFromIntent) {
        Misc.Log("GoogleSignIn: %b", signInResultFromIntent.isSuccess());

        if (signInResultFromIntent.isSuccess()) {
            GoogleSignInAccount account = signInResultFromIntent.getSignInAccount();
            this.setDataAndGo(account.getDisplayName(), account.getEmail(), ((account.getPhotoUrl() == null) ? null : account.getPhotoUrl().toString()));
        } else
            Misc.Log("Google SignIn error, %s", signInResultFromIntent.getStatus().toString());
    }

    private void setDataAndGo(String name, String email, String photo) {
        Address address = new Address();
        address.Name = name;
        address.Email = email;
        address.Photo = photo;

        Applica.Current.TempAddress = address;
        this.moveTo(RegisterActivity.class);
    }

    private void moveTo(Class<?> cls) {
        Misc.startActivity(this, cls);
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
