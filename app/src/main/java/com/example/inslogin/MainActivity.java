package com.example.inslogin;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.inslogin.CustomerViews.AuthenticationDialog;
import com.example.inslogin.interfaces.AuthenticationListener;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements AuthenticationListener {

    private AuthenticationDialog auth_dialog;
    SharedPreferences prefs = null;
    Button btn_login = null;
    String token = null;

    TextView tv_name = null;
    ImageView pro_pic = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //check already have access token
        btn_login = (Button) findViewById(R.id.btn_login);
        tv_name = (TextView) findViewById(R.id.tv_name);
        pro_pic = (ImageView) findViewById(R.id.pro_pic);
        prefs = getSharedPreferences(Constants.PREF_NAME,MODE_PRIVATE);
        token = prefs.getString("token",null);
        if(token!=null){
            btn_login.setText("Logout");
            //get user information by access token;
            getUserInfoByAccessToken(token);
        }else{
            btn_login.setText("Instagram Login");
            findViewById(R.id.profile_layout).setVisibility(View.GONE);
        }
//        //get user information by access token;
//        getUserInfoByAccessToken(token);
    }

    private void getUserInfoByAccessToken(String token) {
        new RequestInstagramAPI().execute();
    }
    private class RequestInstagramAPI extends AsyncTask<Void,String,String>{

        @Override
        protected String doInBackground(Void... voids) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(Constants.GET_USER_INFO_URL+token);
            try{
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity httpEntity = response.getEntity();
                String json = EntityUtils.toString(httpEntity);
                return json;

            }catch(ClientProtocolException e){
               e.printStackTrace();
            } catch(IOException e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if(response!=null){
                try{
                    JSONObject json = new JSONObject(response);
                    Log.e("response",json.toString());
                    //we need the user id
                    JSONObject jsonData = json.getJSONObject("data");
                    if(jsonData.has("id")){
                        String id = jsonData.getString("id");
                        //save it in the shared preference
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("userId",id);
                        editor.apply();

                        //we can use the other data, profile pic
                        String user_name = jsonData.getString("username");
                        String profile_pic = jsonData.getString("profile_picture");
                        tv_name.setText(user_name);
                        Picasso.with(MainActivity.this).load(profile_pic).into(pro_pic);
                        findViewById(R.id.profile_layout).setVisibility(View.VISIBLE);
                    }
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCodeReceived(String auth_token) {
        if(auth_token == null){
            return;
        }
        //use the token for further
        //save the token in sharedPreference
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("token",auth_token);
        editor.apply();
        token = auth_token;
        btn_login.setText("Logout");
        //get user information by access token;
        getUserInfoByAccessToken(token);
    }

    public void after_click_login(View view){
        if(token!=null){
            //clear the shared preference value
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            btn_login.setText("Instagram Login");
            token = null;
            findViewById(R.id.profile_layout).setVisibility(View.GONE);
        }else{
            auth_dialog = new AuthenticationDialog(this, this);
            auth_dialog.setCancelable(true);
            auth_dialog.show();
        }

    }
}
