package fr.inria.tyrex.wifiscaninterval;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


class ScanRecorder {

    private Context mContext;
    private WifiManager mWifiManager;


    // mScans will grow over the time.
    // This app is not built for running during hours.
    private LinkedHashMap<Long, List<ScanResult>> mScans;

    private boolean mIsScanning;

     ScanRecorder(Context context) {

        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mScans = new LinkedHashMap<>();

        context.registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mIsScanning = false;
    }

    boolean isScanning() {
        return mIsScanning;
    }

    int getScansNumber() {
        return mScans.size();
    }

    void start() {
        mWifiManager.startScan();
        mIsScanning = true;
        mScans.clear();
    }


    void stop() {
        mIsScanning = false;
    }

    void destroy() {
        mContext.unregisterReceiver(mWifiScanReceiver);
    }


    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) {

            if (!mIsScanning) return;

            List<ScanResult> scanResults = mWifiManager.getScanResults();
            mScans.put(System.currentTimeMillis(), scanResults);

            mWifiManager.startScan();
        }
    };


    // Get Mean of sampling rate in seconds
    Double getMeanSampling() {

        if(mScans.size() == 0) return null;

        Long[] timestamps = mScans.keySet().toArray(new Long[mScans.size()]);
        double meanDt = 0;
        long prevTimestamp = timestamps[0];
        for (int i = 1; i < timestamps.length; i++) {
            long curTimestamp = timestamps[i];
            long dT = curTimestamp - prevTimestamp;
            meanDt = (meanDt * (i - 1) + dT) / i;
            prevTimestamp = curTimestamp;
        }
        return meanDt / 1000;
    }

    @TargetApi(17)
    double[] getStatsOnSensorSampling() {

        Map<String, Long> prevScanResultList = new HashMap<>();
        List<Double> listOfDt = new ArrayList<>();

        for (Map.Entry<Long, List<ScanResult>> scanEntry : mScans.entrySet()) {
            Map<String, Long> currentScanResultList = new HashMap<>();

            for (ScanResult scan : scanEntry.getValue()) {
                currentScanResultList.put(scan.BSSID, scan.timestamp);
            }

            Map<String, Long> intersection = new HashMap<>(prevScanResultList);
            intersection.keySet().retainAll(currentScanResultList.keySet());

            for (Map.Entry<String, Long> prevScans : intersection.entrySet()) {
                double previousScanTimestamp = prevScans.getValue();
                double currentScanTimestamp = currentScanResultList.get(prevScans.getKey());
                if(previousScanTimestamp == currentScanTimestamp) continue;
                listOfDt.add((currentScanTimestamp - previousScanTimestamp)/1e6);
            }

            prevScanResultList = currentScanResultList;
        }

        if(listOfDt.size() == 0) return null;

        return Utils.statsOnVector(listOfDt);
    }


}
