package fr.inria.tyrex.wifiscaninterval;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScanIntervalActivity extends AppCompatActivity {

    private final static int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private final static int NOTIFICATION_ID = 1;

    private NotificationManager mNotificationManager;
    private ScanRecorder mRecorder;
    private Handler mDisplayHandler;

    private Button mStartStopButton;
    private TextView mScansNumberTextView;
    private TextView mScanSamplingTextView;
    private TextView mStatsAverageTextView;
    private TextView mStatsMinTextView;
    private TextView mStatsMaxTextView;

    private boolean mIsStartClickedAndNoPermission;


    public ScanIntervalActivity() {
        mIsStartClickedAndNoPermission = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_interval);

        mRecorder = new ScanRecorder(this);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mStartStopButton = (Button) findViewById(R.id.start_stop);
        mScansNumberTextView = (TextView) findViewById(R.id.scans_number);
        mScanSamplingTextView = (TextView) findViewById(R.id.scan_sampling);
        TextView informationTextView = (TextView) findViewById(R.id.information);
        informationTextView.setText(Html.fromHtml(getString(R.string.information)));
        informationTextView.setMovementMethod(LinkMovementMethod.getInstance());

        if (Build.VERSION.SDK_INT < 17) {
            findViewById(R.id.stats).setVisibility(View.GONE);
        } else {
            mStatsAverageTextView = (TextView) findViewById(R.id.stats_average);
            mStatsMinTextView = (TextView) findViewById(R.id.stats_min);
            mStatsMaxTextView = (TextView) findViewById(R.id.stats_max);
        }


        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mRecorder.isScanning()) {
                    if (ContextCompat.checkSelfPermission(ScanIntervalActivity.this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(ScanIntervalActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                        mIsStartClickedAndNoPermission = true;
                    } else {
                        startScan();
                    }
                } else {
                    stopScan();

                    if (mRecorder.getScansNumber() >= 2) {
                        send();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && mIsStartClickedAndNoPermission) {
            mIsStartClickedAndNoPermission = false;
            startScan();
        }
    }


    private void startScan() {

        mRecorder.start();

        mStartStopButton.setText(R.string.stop);
        mScanSamplingTextView.setText(R.string.api_sampling_rate_default);
        mStatsAverageTextView.setText(R.string.stats_average_default);
        mStatsMinTextView.setText(R.string.stats_min_default);
        mStatsMaxTextView.setText(R.string.stats_max_default);

        mDisplayHandler = new Handler();
        mDisplayHandler.post(mDisplayRunnable);

        // Manage notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_network_check_white_24dp)
                        .setContentTitle(getString(R.string.notification_title))
                        .setOngoing(true);
        Intent resultIntent = new Intent(this, ScanIntervalActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }


    private void stopScan() {
        mRecorder.stop();
        mStartStopButton.setText(R.string.start);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        mRecorder.destroy();
    }


    private Runnable mDisplayRunnable = new Runnable() {

        @Override
        public void run() {

            int scansNumber = mRecorder.getScansNumber();

            mScansNumberTextView.setText(String.valueOf(scansNumber));

            if (scansNumber >= 2) {
                mScanSamplingTextView.setText(String.format(Locale.US, "%.2f s",
                        mRecorder.getMeanSampling()));
            }

            if (scansNumber >= 2 && Build.VERSION.SDK_INT >= 17) {
                double[] stats = mRecorder.getStatsOnSensorSampling();

                mStatsAverageTextView.setText(
                        String.format(Locale.US, getString(R.string.stats_average), stats[0]));
                mStatsMinTextView.setText(
                        String.format(Locale.US, getString(R.string.stats_min), stats[1]));
                mStatsMaxTextView.setText(
                        String.format(Locale.US, getString(R.string.stats_max), stats[2]));
            }

            if (mRecorder.isScanning()) {
                mDisplayHandler.postDelayed(mDisplayRunnable, 1000);
            }

        }
    };


    private void send() {

        double[] stats = mRecorder.getStatsOnSensorSampling();

        final Map<String, String> params = new HashMap<>();
        params.put("device", Utils.getDeviceName());
        params.put("buildDisplay", Build.DISPLAY);
        params.put("averageSamplingRateApi", String.format(Locale.US, "%.2f",
                mRecorder.getMeanSampling()));
        params.put("numberOfScans", String.valueOf(mRecorder.getScansNumber()));
        params.put("averageSamplingRateSensor", String.format(Locale.US, "%.2f", stats[0]));
        params.put("minSamplingRateSensor", String.format(Locale.US, "%.2f", stats[1]));
        params.put("maxSamplingRateSensor", String.format(Locale.US, "%.2f", stats[2]));

        StringRequest sr = new StringRequest(Request.Method.POST, getString(R.string.ws_url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(getApplicationContext(), R.string.toast_sharing,
                                Toast.LENGTH_SHORT).show();
                    }
                }, null) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };

        Volley.newRequestQueue(this).add(sr);
    }

}
