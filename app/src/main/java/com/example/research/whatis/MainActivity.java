package com.example.research.whatis;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.os.StrictMode;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    protected boolean _taken = true;
    File sdImageMainDirectory;
    String OCRedText = "";
    String synonym = "";
    private SQLiteHelper dbHelper;
    HttpURLConnection connection = null;
    int httpCode = 0;

    protected static final String PHOTO_TAKEN = "photo_taken";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);

        dbHelper = new SQLiteHelper(this);

        //dbHelper.getAllWords().toString();
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

        Log.e("CLicked Menu", String.valueOf(id));
        Log.e("CLicked action", String.valueOf(R.id.action_settings));
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            TextView view = (TextView) findViewById(R.id.textView);
            view.setVisibility(View.GONE);
            ListView lView = (ListView) findViewById(R.id.listView);
            lView.setVisibility(View.VISIBLE);

            ArrayList<String> storedWords = dbHelper.getAllWords();
            Log.e("SQLite", storedWords.toString());
            ArrayAdapter arrayAdapter = new ArrayAdapter(this, R.layout.activity_listview, R.id.wordsLabel, storedWords);

            lView.setAdapter(arrayAdapter);
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
        intent.putExtra("outPutURI", outputFileUri);

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
                    Log.d("API", "data.getExtras().get(\"data\")" + data.getExtras().get("outPutURI"));
                    Log.d("API", "sdImageDir" + sdImageMainDirectory);

                    Bitmap photo = (Bitmap) data.getExtras().get("data");

                    StoreImage(this, photo);

                    try {
                        invokeOCRAPI();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

//                    Toast.makeText(this, "Response: " + OCRedText, Toast.LENGTH_LONG).show();
                    Log.d("API", "Response: " + OCRedText);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //finish();
                //startActivity(new Intent(CameraCapture.this, Home.class));
        }
    }

    public void StoreImage(Context mContext, Bitmap bm) {
//        Toast.makeText(this, "Storing image....", Toast.LENGTH_LONG).show();
        Log.d("API", "Stroging image " + OCRedText);
        System.out.println("Storing image...: " + OCRedText);

        try {

            FileOutputStream out = new FileOutputStream(sdImageMainDirectory.getPath());
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            bm.recycle();
            // cleaning up
            //
//            bm = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), imageLoc);
//            FileOutputStream out = new FileOutputStream(imageDir);
//            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
//            bm.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public String invokeOCRAPI() throws IOException, JSONException {
//        String license_code = "31CB2366-603F-4F19-BEFB-1C961348DBA0";
//        String user_name = "VIVEKHARIKRISHNANR";
//        String ocrURL = "http://www.ocrwebservice.com/restservices/processDocument?gettext=true";

//        Toast.makeText(this, "Invoking OCR", Toast.LENGTH_LONG).show();
        Log.d("API", "Inokving OCR: ");

        final ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "Loading ...", "Converting to text.", true, false);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String apiKey="nh9tSJg3Mf";
                String langCode="en";
                final OCRServiceAPI apiClient = new OCRServiceAPI(apiKey);
                apiClient.convertToText(langCode, sdImageMainDirectory.getPath());

                // Doing UI related code in UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();

                        // Showing response dialog
//                        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
//                        alert.setMessage(apiClient.getResponseText());
                        OCRedText = apiClient.getResponseText();
                        OCRedText = OCRedText.replaceAll("\\W", "");
                        Log.e("OCRedText", OCRedText);

                        try {
                            invokeSynonymsAPI();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


//                        alert.setPositiveButton(
//                                "OK",
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick( DialogInterface dialog, int id) {
//                                    }
//                                });
//
////                         Setting dialog title related from response code
//                        if (apiClient.getResponseCode() == 200) {
//                            alert.setTitle("Success");
//                        } else {
//                            alert.setTitle("Faild");
//                        }
//
//                        alert.show();
                    }
                });
            }
        });
        thread.start();
        //
        //MediaStore.Files.getContentUri(Uri.fromFile(sdImageMainDirectory).toString());
//        byte bytes[] = FileUtils.readFileToByteArray(sdImageMainDirectory);
//
//
//        URL url = new URL(ocrURL);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setDoOutput(true);
//        connection.setDoInput(true);
//        connection.setRequestMethod("POST");
//        connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((user_name + ":" + license_code).getBytes(), Base64.DEFAULT));
//        connection.setRequestProperty("Content-Type", "application/json");
//        connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
//
//        Toast.makeText(this, "OCR Connection" + connection.toString(), Toast.LENGTH_LONG).show();
//        Log.d("API", "OCR Connection: " + connection.toString());
//
//        OutputStream stream = connection.getOutputStream();
//        stream.write(bytes);
//        stream.close();
//
//        int httpCode = connection.getResponseCode();
//        // Success request
//        if (httpCode == HttpURLConnection.HTTP_OK) {
//            Log.d("OCR API", "Success");
//            // Get response stream
//            String jsonResponse = GetResponseToString(connection.getInputStream());
//            JSONObject reader = new JSONObject(jsonResponse);
//            JSONArray text = reader.getJSONArray("OCRText");
//            Log.d("OCR API", text.toString());
//            if (text.length() > 0) {
//                OCRedText = text.get(0).toString();
//            } else {
//                OCRedText = "";
//            }
//
//            File root = new File(Environment
//                    .getExternalStorageDirectory()
//                    + File.separator + "WhatisCache" + File.separator);
//            root.mkdirs();
//            File f = new File(root, "OCRedText.txt");
//
//            FileWriter writer = new FileWriter(f);
//            writer.append(OCRedText);
//            writer.flush();
//            writer.close();
//        } else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
//            Log.d("OCR API", "OCR Error Message: Unauthorizied request");
//            System.out.println("OCR Error Message: Unauthorizied request");
//        } else {
//            // Error occurred
//            String jsonResponse = GetResponseToString(connection.getErrorStream());
//
//            JSONObject reader = new JSONObject(jsonResponse);
//            JSONArray text = reader.getJSONArray("ErrorMessage");
//            Log.d("OCR API", "OCR Error");
//            Log.d("OCR API", text.toString());
//
//            OCRedText = text.get(0).toString();
//            // Error message
//            Toast.makeText(MainActivity.this, "Error Message: " + OCRedText, Toast.LENGTH_LONG).show();
//            System.out.println();
//        }
//
//        connection.disconnect();
        //OCRedText = "happy";
        return OCRedText;
    }

    public String invokeSynonymsAPI() throws IOException, JSONException {
        if(OCRedText.length()>0) {
            final ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "Loading ...", "Fetching synonym for " + OCRedText, true, false);
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String synonymURL = "http://words.bighugelabs.com/api/2/4bbcc4ae52f1e82bd08e683a72665f7b/";

                    synonymURL += OCRedText.toString() + "/json";
                    synonymURL = synonymURL.replaceAll("\\s", "");

                    Log.d("API", "Invokes Synonyms" + synonymURL);

                    URL url = null;
                    try {
                        url = new URL(synonymURL);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setRequestMethod("GET");

                        connection.setRequestProperty("Content-Type", "application/json");
                        //connection.setRequestProperty("Content-Length", Integer.toString(OCRedText.length()));

                        OutputStream stream = connection.getOutputStream();
                        stream.close();

                        //fabulous/json

                        httpCode = connection.getResponseCode();


                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Doing UI related code in UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            // Success request
                            try {
                                if (httpCode == HttpURLConnection.HTTP_OK) {
                                    // Get response stream
                                    String jsonResponse = GetResponseToString(connection.getInputStream());
                                    JSONObject reader = new JSONObject(jsonResponse);
                                    Log.d("Synonyms JSONResp ", jsonResponse.toString());
                                    Log.d("Synonyms Reader", reader.toString());

                                    JSONArray text = null;
                                    try {
                                        JSONObject obj = reader.getJSONObject("adjective");
                                        text = obj.getJSONArray("syn");
                                    } catch (JSONException e) {
                                        e.printStackTrace();

                                        try {
                                            JSONObject obj = reader.getJSONObject("noun");
                                            text = obj.getJSONArray("syn");
                                        } catch(JSONException e1) {
                                            JSONObject obj = reader.getJSONObject("verb");
                                            text = obj.getJSONArray("syn");
                                        }
                                    }

                                    if (text != null) {
                                        synonym = text.get(0).toString();
                                    } else {
                                        synonym = "<ERROR IN API>";
                                    }

                                    FileOutputStream fos = openFileOutput("synonym.txt", Context.MODE_APPEND);
                                    fos.write(synonym.getBytes());
                                    fos.close();
                                } else {
                                    // Error occurred
                                    String jsonResponse = GetResponseToString(connection.getErrorStream());

                                    JSONObject reader = new JSONObject(jsonResponse);
                                    JSONArray text = reader.getJSONArray("ErrorMessage");

                                    synonym = text.get(0).toString();
                                    // Error message
//                                    Toast.makeText(MainActivity.this, "Error Message: " + synonym, Toast.LENGTH_LONG).show();
                                    System.out.println();
                                }

                                connection.disconnect();

                                TextView view = (TextView) findViewById(R.id.textView);
                                view.setVisibility(View.VISIBLE);
                                ListView lView = (ListView) findViewById(R.id.listView);
                                lView.setVisibility(View.GONE);

                                view.setText(OCRedText + ":" + synonym);
                                dbHelper.insertWord(OCRedText, synonym, "", "http://words.bighugelabs.com");
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            thread.start();
        }
        else {
            synonym= "";
        }

        return synonym;
    }

    private static String GetResponseToString(InputStream inputStream) throws IOException {
        InputStreamReader responseStream = new InputStreamReader(inputStream);

        BufferedReader br = new BufferedReader(responseStream);
        StringBuffer strBuff = new StringBuffer();
        String s;
        while ((s = br.readLine()) != null) {
            strBuff.append(s);
        }

        return strBuff.toString();
    }

}