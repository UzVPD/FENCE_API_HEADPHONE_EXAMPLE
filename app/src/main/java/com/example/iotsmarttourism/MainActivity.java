package com.example.iotsmarttourism;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity {

    private final String HEADPHONES_FENCE_KEY = "HEADPHONES_KEY";

    private final String TAG = getClass().getSimpleName();

    private PendingIntent pendingIntent;
    private FenceReceiver fenceReceiver;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        textView = findViewById(R.id.tv_logs);

        Intent intent = new Intent(FenceReceiver.FENCE_RECEIVER_ACTION);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 1, intent, PendingIntent.FLAG_MUTABLE);

        fenceReceiver = new FenceReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerFences();
        registerReceiver(fenceReceiver, new IntentFilter(FenceReceiver.FENCE_RECEIVER_ACTION));
    }


    private void checkPermissions() {
        // Checking for necessary permissions
        if (ContextCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.i(TAG, "Permission denied.");
            } else {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION},
                        1
                );
            }
        }
    }

    private void registerFences() {
        // Defined a fence
        AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);
        AwarenessFence walkingFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);

        // Create a combination fence to AND primitive fences.
        AwarenessFence walkingWithHeadphones = AwarenessFence.and(
                walkingFence, headphoneFence
        );

        // Registering a fence
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(HEADPHONES_FENCE_KEY, headphoneFence, pendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "Fence was successfully registered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Fence registeration failed. " + e);
                    }
                });
    }

    class FenceReceiver extends BroadcastReceiver {
        // for manual triggering Broadcast Receiver
        public static final String FENCE_RECEIVER_ACTION =
                "com.example.iotsmarttourism.FenceReceiver.FENCE_RECEIVER_ACTION";

        // Receiving current Fence State
        @Override
        public void onReceive(Context context, Intent intent) {
            FenceState fenceState = FenceState.extract(intent);

            if (TextUtils.equals(fenceState.getFenceKey(), HEADPHONES_FENCE_KEY)) {

                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        setHeadphoneStatus(HeadphoneState.PLUGGED_IN);
                        break;
                    case FenceState.FALSE:
                        setHeadphoneStatus(HeadphoneState.UNPLUGGED);
                        break;
                }
            }

        }


        // For visual purpose
        private void setHeadphoneStatus(int headphone) {
            if (headphone == HeadphoneState.PLUGGED_IN) {
                textView.setTextColor(Color.GREEN);
                textView.setText("Headphones plugged in");
            } else {
                textView.setTextColor(Color.RED);
                textView.setText("Headphones unplugged");
            }
        }

    }
}