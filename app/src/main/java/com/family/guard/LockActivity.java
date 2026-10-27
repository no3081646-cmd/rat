package com.family.guard;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LockActivity extends Activity {
    private BroadcastReceiver unlockReceiver;
    
    // Variabel buat Senter Kedip
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;
    private Handler flashHandler = new Handler(Looper.getMainLooper());
    private Runnable flashRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                isFlashOn = !isFlashOn;
                cameraManager.setTorchMode(cameraId, isFlashOn);
                flashHandler.postDelayed(this, 300); // Kedip tiap 300ms
            } catch (Exception e) { }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                             WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                             WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        String msg = getIntent().getStringExtra("msg");
        String realPin = getIntent().getStringExtra("pin");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF000000);
        layout.setPadding(50, 50, 50, 50);
        layout.setGravity(android.view.Gravity.CENTER);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(msg);
        tvMsg.setTextColor(0xFFFF0000);
        tvMsg.setTextSize(24);
        tvMsg.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvMsg);

        EditText etPin = new EditText(this);
        etPin.setHint("Masukkan PIN");
        etPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setTextColor(0xFFFFFFFF);
        etPin.setHintTextColor(0xFF888888);
        layout.addView(etPin);

        Button btnUnlock = new Button(this);
        btnUnlock.setText("BUKA KUNCI");
        btnUnlock.setOnClickListener(v -> {
            if (etPin.getText().toString().equals(realPin)) {
                finish();
            } else {
                Toast.makeText(this, "PIN SALAH!", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnUnlock);

        setContentView(layout);

        // Mulai Senter Kedip
        try {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            cameraId = cameraManager.getCameraIdList()[0]; // Ambil kamera belakang
            flashHandler.post(flashRunnable);
        } catch (Exception e) { }

        unlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, new IntentFilter("ACTION_UNLOCK_DEVICE"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(unlockReceiver, new IntentFilter("ACTION_UNLOCK_DEVICE"));
        }
    }

    @Override
    public void onBackPressed() { }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Matikan Senter & Hentikan Kedipan
        flashHandler.removeCallbacks(flashRunnable);
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (Exception e) { }

        try { unregisterReceiver(unlockReceiver); } catch (Exception e) {}
    }
}
