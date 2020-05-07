package com.example.probaharta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentController;
import androidx.fragment.app.FragmentFactory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.probaharta.R;


import com.davemorrissey.labs.subscaleview.ImageSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IAOrientationListener;
import com.indooratlas.android.sdk.IAOrientationRequest;
import com.indooratlas.android.sdk.IARegion;
//import com.indooratlas.android.sdk.R;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;

import java.io.File;

public class MainActivity extends FragmentActivity  {

    private final int CODE_PERMISSIONS = 1;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE =1;
    private static final String TAG= " EXEMPLU";

    private static final float dotRadius=1.0f;


    private IALocationManager locationManager;
    private IAFloorPlan mFloorPlan;
    private DownloadManager downloadManager;
    private BlueDot imageview;
    private long mDownloadID;

        private IALocationListener locationListener = new IALocationListenerSupport() {

            @Override
            public void onLocationChanged(IALocation iaLocation) {
                Log.d(TAG, "location is : " + iaLocation.getLatitude() + " , " + iaLocation.getLongitude());
                if (imageview != null && imageview.isReady()) {
                    IALatLng latLng = new IALatLng(iaLocation.getLatitude(), iaLocation.getLongitude());
                    PointF point = mFloorPlan.coordinateToPoint(latLng);
                    imageview.setDotCenter(point);
                    imageview.setRadius1(mFloorPlan.getMetersToPixels() * iaLocation.getAccuracy());
                    imageview.postInvalidate();
                }
            }
        };

        private IAOrientationListener mOrientationListener = new IAOrientationListener() {
            @Override
            public void onHeadingChanged(long timestamp, double heading) {
                if (mFloorPlan != null) {
                    imageview.setHeading(heading - mFloorPlan.getBearing());
                }
            }

            @Override
            public void onOrientationChange(long l, double[] doubles) {

            }
        };


        private IARegion.Listener mRegionListener = new IARegion.Listener() {
            @Override
            public void onEnterRegion(IARegion iaRegion) {
                if (iaRegion.getType() == IARegion.TYPE_FLOOR_PLAN) {
                    String id = iaRegion.getId();
                    Log.d(TAG, "floorPlan changed to " + id);
                    Toast.makeText(MainActivity.this, id, Toast.LENGTH_SHORT).show();
                    fetchFloorPlan(iaRegion.getFloorPlan());
                }
            }

            @Override
            public void onExitRegion(IARegion iaRegion) {

            }
        };


       // @SuppressLint("WrongViewCast")
        @Override
        protected void onCreate (Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            findViewById(android.R.id.content).setKeepScreenOn(true);

            imageview = findViewById(R.id.imageView);

            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            locationManager = IALocationManager.create(this);

            Utils.shareTraceId(findViewById(R.id.imageView), MainActivity.this, locationManager);


            //---------------------------------
            String[] neededPermissions = {
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            ActivityCompat.requestPermissions(this, neededPermissions, CODE_PERMISSIONS);
            locationManager = IALocationManager.create(this);
        }
        protected void onResume ()

        {
            super.onResume();
            ensurePermissions();
            //receive location update
            locationManager.requestLocationUpdates(IALocationRequest.create(), locationListener);
            locationManager.registerRegionListener(mRegionListener);
            IAOrientationRequest orientationRequest = new IAOrientationRequest(10f, 10f);
            locationManager.registerOrientationListener(orientationRequest, mOrientationListener);
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        protected void onPause ()
        {
            super.onPause();
            locationManager.removeLocationUpdates(locationListener);
            locationManager.unregisterRegionListener(mRegionListener);
            locationManager.unregisterOrientationListener(mOrientationListener);
            unregisterReceiver(onComplete);
        }
        protected void onDestroy ()
        {
            super.onDestroy();
            locationManager.destroy();
        }


        private BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                if (id != id) {
                    Log.d(TAG, "ignore");
                    return;
                }
                Log.w(TAG, "SUCCES!!");
                Bundle extras = intent.getExtras();

                if (extras == null) {
                    Log.w(TAG, "can t show");
                    return;
                }
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
                Cursor c = (Cursor) new DownloadManager.Query();

                if (c.moveToFirst()) {
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        String filePath = c.getString(c.getColumnIndex(
                                DownloadManager.COLUMN_LOCAL_URI));
                        showFloorPlanImage(filePath);
                    }
                }
                c.close();
            }
        };
        private void showFloorPlanImage (String filePath){

            Log.w(TAG, "showFloorPlanImage: " + filePath);
            imageview.setRadius2(mFloorPlan.getMetersToPixels() * dotRadius);
            imageview.setImage(ImageSource.uri(filePath));
        }
        private void fetchFloorPlan (IAFloorPlan floorPlan)
        {
            mFloorPlan = floorPlan;
            String fileName = mFloorPlan.getId() + ".img";
            String filePath = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + fileName;

            File file = new File(filePath);
            if (!file.exists()) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(floorPlan.getUrl()));
                request.setDescription("FloorPlan from IndoorAtlas");
                request.setTitle("Floor plan");
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                mDownloadID = downloadManager.enqueue(request);
            } else {
                showFloorPlanImage(filePath);
            }
        }

        private void ensurePermissions ()
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }
        }
        @Override
        public void onRequestPermissionsResult ( int requestCode, String[] permissions,
        int[] grantResults)
        {
            if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
                if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.storage_permission_denied_message, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

