package com.example.probaharta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentContainer;

import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentController;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentManager;

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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
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

import com.example.probaharta.MainActivty2;
import java.io.File;
import java.util.Locale;

public class Main extends FragmentActivity implements IALocationListener {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private final int CODE_PERMISSIONS = 1;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE =1;
    private static final String TAG= " EXEMPLU";

    private static final float dotRadius=1.0f;
    private Fragment fragment;

    private IALocationManager locationManager;
    private IAFloorPlan mFloorPlan;
    private DownloadManager downloadManager;
    private BlueDot imageview;
    private long mDownloadID;
    private ScrollView mScrollView;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    private long mRequestStartTime;

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
                Toast.makeText(Main.this, id, Toast.LENGTH_SHORT).show();
                fetchFloorPlan(iaRegion.getFloorPlan());
            }
        }

        @Override
        public void onExitRegion(IARegion iaRegion) {

        }
    };

    public Main(FragmentManager supportFragmentManager) {
    }

    public Main() {

    }


    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

       setContentView(R.layout.activity_main);

      // findViewById(android.R.id.content).setKeepScreenOn(true);
        imageview = findViewById(R.id.imageView);


        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        locationManager = IALocationManager.create(this);

        Utils.shareTraceId(findViewById(R.id.imageView), Main.this, locationManager);


        //---------------------------------
        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
       // ActivityCompat.requestPermissions(this, neededPermissions, CODE_PERMISSIONS);
       // locationManager = IALocationManager.create(this);
        ViewGroup container = null;
        //return inflater.inflate(R.layout.activity_main,container,false);
    }
    protected void onDestroy ()
    {
        super.onDestroy();
        locationManager.destroy();
    }
    protected void onResume ()

    {
        super.onResume();
        ensurePermissions();
        //receive location update


        locationManager.requestLocationUpdates(IALocationRequest.create(), this);
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
    { if ( Build.VERSION.SDK_INT >= 23){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED  ){
            requestPermissions(new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return ;
        }
    }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
    }
    @Override
    public void onRequestPermissionsResult ( int requestCode, String[] permissions,
                                             int[] grantResults)
    {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ensurePermissions();
                } else {
                    // Permission Denied
                    Toast.makeText( this,"your message" , Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }




    @Override
    public void onLocationChanged(IALocation iaLocation) {
        Locale locale = new Locale.Builder().setRegion("RO").build();
        log(String.format(locale,"%f,%f, accuracy: %.2f, certainty: %.2f",
                iaLocation.getLatitude(), iaLocation.getLongitude(), iaLocation.getAccuracy()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {

            case IALocationManager.STATUS_AVAILABLE:

                log("onStatusChanged: Available");

                break;

            case IALocationManager.STATUS_LIMITED:

                log("onStatusChanged: Limited");

                break;

            case IALocationManager.STATUS_OUT_OF_SERVICE:

                log("onStatusChanged: Out of service");

                break;

            case IALocationManager.STATUS_TEMPORARILY_UNAVAILABLE:

                log("onStatusChanged: Temporarily unavailable");

        }
    }


    public void log(String msg)
    {
        double duration = mRequestStartTime != 0
                ? (SystemClock.elapsedRealtime() - mRequestStartTime) / 1e3 : 0d;
        //mLog.append(String.format(Locale.US, "\n[%06.2f]: %s", duration, msg));
       // mScrollView.smoothScrollBy(0, mLog.getBottom());
    }

}

