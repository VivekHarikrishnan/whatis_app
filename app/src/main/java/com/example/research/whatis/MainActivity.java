package com.example.research.whatis;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    protected boolean _taken = true;
    File sdImageMainDirectory;
    String OCRedText = "";
    String synonym = "";

    protected static final String PHOTO_TAKEN = "photo_taken";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                takePicture();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public void takePicture() {
        File root = new File(Environment
                .getExternalStorageDirectory()
                + File.separator + "WhatisCache" + File.separator);
        root.mkdirs();
        sdImageMainDirectory = new File(root, "cap.jpg");

        startCameraActivity();
    }

    protected void startCameraActivity() {

        Uri outputFileUri = Uri.fromFile(sdImageMainDirectory);

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case 0:
                finish();
                break;

            case -1:

                try {
                    StoreImage(this, Uri.parse(data.toURI()),
                            sdImageMainDirectory);

                    Toast.makeText(this, "Response: " + OCRedText, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //finish();
                //startActivity(new Intent(CameraCapture.this, Home.class));
        }
    }

    public void StoreImage(Context mContext, Uri imageLoc, File imageDir) {
        Toast.makeText(this, "Storing image....", Toast.LENGTH_LONG).show();
        Bitmap bm = null;
        try {
            bm = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), imageLoc);
            FileOutputStream out = new FileOutputStream(imageDir);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            bm.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            invokeOCRAPI();
            invokeSynonymsAPI();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String invokeOCRAPI() throws IOException, JSONException {
        String license_code = "31CB2366-603F-4F19-BEFB-1C961348DBA0";
        String user_name =  "VIVEKHARIKRISHNANR";
        String ocrURL = "http://www.ocrwebservice.com/restservices/processDocument?gettext=true";

        //MediaStore.Files.getContentUri(Uri.fromFile(sdImageMainDirectory).toString());
        byte bytes[] = FileUtils.readFileToByteArray(sdImageMainDirectory);

        URL url = new URL(ocrURL);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((user_name + ":" + license_code).getBytes(), Base64.DEFAULT));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));

        OutputStream stream = connection.getOutputStream();
        stream.write(bytes);
        stream.close();

        int httpCode = connection.getResponseCode();
        Toast.makeText(this, "Invoking OCR", Toast.LENGTH_LONG).show();

        // Success request
        if (httpCode == HttpURLConnection.HTTP_OK)
        {
            // Get response stream
            String jsonResponse = GetResponseToString(connection.getInputStream());
            JSONObject reader = new JSONObject(jsonResponse);
            JSONArray text  = reader.getJSONArray("OCRText");

            OCRedText = text.get(0).toString();

            File root = new File(Environment
                    .getExternalStorageDirectory()
                    + File.separator + "WhatisCache" + File.separator);
            root.mkdirs();
            File f = new File(root, "OCRedText.txt");

            FileWriter writer = new FileWriter(f);
            writer.append(OCRedText);
            writer.flush();
            writer.close();
        }
        else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED)
        {
            System.out.println("OCR Error Message: Unauthorizied request");
        }
        else
        {
            // Error occurred
            String jsonResponse = GetResponseToString(connection.getErrorStream());

            JSONObject reader = new JSONObject(jsonResponse);
            JSONArray text  = reader.getJSONArray("ErrorMessage");

            OCRedText = text.get(0).toString();
            // Error message
            Toast.makeText(MainActivity.this, "Error Message: " + OCRedText, Toast.LENGTH_LONG).show();
            System.out.println();
        }

        connection.disconnect();

        return OCRedText;
    }

    public String invokeSynonymsAPI() throws IOException, JSONException {
        String synonymURL = "http://words.bighugelabs.com/api/2/4bbcc4ae52f1e82bd08e683a72665f7b/";

        synonymURL += OCRedText.toString() + "/json";

        URL url = new URL(synonymURL);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", Integer.toString(OCRedText.length()));

        OutputStream stream = connection.getOutputStream();
        stream.close();

        //fabulous/json

        int httpCode = connection.getResponseCode();
        Toast.makeText(this, "Invoking Synonyms", Toast.LENGTH_LONG).show();

        // Success request
        if (httpCode == HttpURLConnection.HTTP_OK)
        {
            // Get response stream
            String jsonResponse = GetResponseToString(connection.getInputStream());
            JSONObject reader = new JSONObject(jsonResponse);
            JSONArray text  = reader.getJSONArray("sim");

            synonym = text.get(0).toString();

            FileOutputStream fos = openFileOutput("synonym.txt", Context.MODE_APPEND);
            fos.write(synonym.getBytes());
            fos.close();
        }
        else
        {
            // Error occurred
            String jsonResponse = GetResponseToString(connection.getErrorStream());

            JSONObject reader = new JSONObject(jsonResponse);
            JSONArray text  = reader.getJSONArray("ErrorMessage");

            synonym = text.get(0).toString();
            // Error message
            Toast.makeText(MainActivity.this, "Error Message: " + synonym, Toast.LENGTH_LONG).show();
            System.out.println();
        }

        connection.disconnect();

        return synonym;
    }

    private static String GetResponseToString(InputStream inputStream) throws IOException
    {
        InputStreamReader responseStream  = new InputStreamReader(inputStream);

        BufferedReader br = new BufferedReader(responseStream);
        StringBuffer strBuff = new StringBuffer();
        String s;
        while ( ( s = br.readLine() ) != null ) {
            strBuff.append(s);
        }

        return strBuff.toString();
    }
}