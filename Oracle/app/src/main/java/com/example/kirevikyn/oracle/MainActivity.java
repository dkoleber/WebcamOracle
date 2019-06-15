package com.example.kirevikyn.oracle;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.android.volley.toolbox.HttpHeaderParser.*;

public class MainActivity extends AppCompatActivity {

    private String serverURL = "http://192.168.1.3:8000/cam/";//"73.3.165.101:8000";
    private String serverPassword = "pwd";
    private int maxWidth = 640;
    private int maxHeight = 480;
    private AppCompatImageView cameraImage;
    private boolean autosave = true;
    private boolean saveEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            saveEnabled = true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 0);
        }



        Button settingsButton = (Button)findViewById(R.id.settingsButton);
        Button capturebutton = (Button)findViewById(R.id.captureButton);
        cameraImage = (AppCompatImageView)findViewById(R.id.cameraImage);

        final Context mainContext = this;

        capturebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
                serverURL = "http://" + prefs.getString("serverSetting","localhost") + ":8000/cam/";
                serverPassword = prefs.getString("passwordSetting", "test");
                autosave = prefs.getBoolean("save_enable",true);
                decodeImage(mainContext);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mainContext, SettingsActivity.class);
                startActivity(intent);
            }
        });



    }
    @Override
    protected void onResume() {
        super.onResume();
        //decodeImage(this);
    }
    private void decodeImage(final Context context){

        RequestQueue queue = Volley.newRequestQueue(context);
        ByteArrayRequest request = new ByteArrayRequest(Request.Method.GET, serverURL,
                new Response.Listener<byte[]>() {
                    @Override
                    public void onResponse(byte[] response) {

                        Bitmap decoded = decodeAESBytes(response);
                        cameraImage.setImageBitmap(decoded);

                        if(autosave && saveEnabled){
                            //saveToStorage(decoded);
                            //Log.i("derek", "autosaving");
                            savePic(decoded);
                        }

                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("derek","connection failed: " + error.getMessage());
                    }
                });

        queue.add(request);
    }

    private void getImage(Context context){
        RequestQueue queue = Volley.newRequestQueue(context);
        ImageRequest request = new ImageRequest(serverURL,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        //Log.i("derek","successful connection");
                        cameraImage.setImageBitmap(response);
                    }
                },maxWidth,maxHeight, null,
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("derek","connection failed: " + error.getMessage());
                    }
                });

        queue.add(request);
    }

    private class GetImageButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {

        }
    }

    private class SettingsButtonListener implements View.OnClickListener{

        @Override
        public void onClick(View view) {

        }
    }

    private byte[] getKey(){
        byte[] bytes = new byte[16];
        for(int i = 0;i<16;i++){
            bytes[i] = 0;
        }
        try {
            //Log.i("derek", "password: " + serverPassword);
            byte[] password_bytes = serverPassword.getBytes("UTF-8");
            //Log.i("derek", "unfilled bytes: " + bytesToString(password_bytes));
            for (int i = 0; i < password_bytes.length && i < 16; i++) {
                bytes[i] = password_bytes[i];
            }
        }
        catch(Exception e){
            Log.i("derek",e.getMessage());
        }
        return bytes;
    }

    private Bitmap decodeAESBytes(byte[] bytes){


        try{
            byte[] iv = Arrays.copyOfRange(bytes,0,16);
            int unpadded_bytes_count = getInt(Arrays.copyOfRange(bytes,16,48));
            byte[] ciphertext = Arrays.copyOfRange(bytes,48,bytes.length);

            byte[] key_bytes = getKey();
            SecretKeySpec key = new SecretKeySpec(key_bytes,"AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");

            cipher.init(Cipher.DECRYPT_MODE,key, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(ciphertext);
            byte[] unpadded = Arrays.copyOfRange(decrypted,0, unpadded_bytes_count);

            return BitmapFactory.decodeByteArray(unpadded,0, unpadded.length);
        }
        catch(InvalidAlgorithmParameterException|NoSuchPaddingException|NoSuchAlgorithmException|InvalidKeyException|IllegalBlockSizeException|BadPaddingException e){
            Log.i("derek",e.getMessage());
            return null;
        }

    }

    public class ByteArrayRequest extends Request<byte[]> {
        private final Response.Listener<byte[]> mListener;


        public ByteArrayRequest(String url, Response.Listener<byte[]> listener,
                                Response.ErrorListener errorListener) {
            this(Method.GET, url, listener, errorListener);
        }

        public ByteArrayRequest(int method, String url, Response.Listener<byte[]> listener, Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            mListener = listener;

        }
        @Override
        protected Response parseNetworkResponse(NetworkResponse response) {
            return Response.success(response.data, parseCacheHeaders(response));
        }

        @Override
        protected void deliverResponse(byte[] response) {
            if(null != mListener){
                mListener.onResponse(response);
            }
        }

        @Override
        public String getBodyContentType() {
            return "application/octet-stream";
        }

    }
    private String bytesToString(byte[] bytes){
        return bytesToString(bytes,0,bytes.length);
    }
    private String bytesToString(byte[] bytes, int start, int num){
        String startBytes = "";
        for(int i = start;i<num+start;i++){
            startBytes += "x" + getHex(bytes[i]) + " ";
        }
        return startBytes;
    }
    private String getHex(byte bt){
        String s =  Integer.toHexString(bt);
        if (s.length() == 1){
            s = "0" + s;
        }
        if (s.length() > 2){
            s = s.substring(s.length()-2);
        }
        return s;
    }
    private int getInt(byte[] bytes){
        return new BigInteger(bytes).intValue();
    }

    private void saveToStorage(Bitmap image){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        File directory = cw.getDir("Oracle", Context.MODE_PRIVATE);
        File mypath=new File(directory,"profile.jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void savePhoto(Bitmap image, Context context){
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera/");
            Log.i("derek", dir.toString());
            File file = File.createTempFile("test", ".png", dir);
            try (FileOutputStream out = new FileOutputStream(file)) {
                image.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (IOException e) {
                e.printStackTrace();
            }
            addPicToGallery(context,file.getPath());
        }catch(Exception e) {
            Log.i("derek", e.getMessage());
        }

    }

    public static void addPicToGallery(Context context, String photoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(photoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    public void savePic(Bitmap image){
        String savedUrl = MediaStore.Images.Media.insertImage(getContentResolver(), image, "test" , "test desc");
        //Log.i("derek",savedUrl);
    }

}







