package fr.inria.tyrex.wifiscaninterval;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver = null;

    private long mLastTimeScan;
    private float mInterval;

    private Map<String, Long> mRealLastTimeScan = new HashMap<>();
    private float mRealInterval;

    private TextView mRealIntervalTextView, mIntervalTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRealIntervalTextView = (TextView) findViewById(R.id.real_time);
        mIntervalTextView = (TextView) findViewById(R.id.time);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mLastTimeScan = System.currentTimeMillis();

        mRealIntervalTextView.setText(String.format(getString(R.string.seconds), 0f));
        mIntervalTextView.setText(String.format(getString(R.string.seconds), 0f));

        mWifiManager.startScan();
        mWifiScanReceiver = new WifiScanReceiver();
        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mWifiScanReceiver);
        mWifiScanReceiver = null;
    }


    private class WifiScanReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {

            mInterval = (float) ((System.currentTimeMillis() - mLastTimeScan) / 1000.0); // in seconds
            mLastTimeScan = System.currentTimeMillis();

            if (mWifiScanReceiver != null) {
                mWifiManager.startScan();
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {

                mRealInterval = 0;
                int realIterations = 0;
                for (ScanResult scan : mWifiManager.getScanResults()) {

//                    Log.v("DEBUG", "Scan: " + scan.SSID + " " + scan.timestamp);
                    if (mRealLastTimeScan.containsKey(scan.BSSID)) {

                        float realDiffTime = (scan.timestamp - mRealLastTimeScan.get(scan.BSSID)) / 1000000;

                        if (realDiffTime < mInterval * 2) {
                            mRealInterval += (realDiffTime - mRealInterval) / (1 + realIterations);
                            realIterations++;
                        }
                    }

                    mRealLastTimeScan.put(scan.BSSID, scan.timestamp);
                }
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRealIntervalTextView.setText(String.format(getString(R.string.seconds), mRealInterval));
                    mIntervalTextView.setText(String.format(getString(R.string.seconds), mInterval));
                }
            });

        }
    }
}
