package com.family.guard;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;

public class FreezeActivity extends Activity {
    private Vibrator vibrator;
    private BroadcastReceiver stopFreezeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                             WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                             WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF000000);
        layout.setGravity(Gravity.CENTER);

        TextView tv = new TextView(this);
        tv.setText("SYSTEM CRITICAL ERROR\n\nDEVICE FROZEN");
        tv.setTextColor(0xFFFF0000);
        tv.setTextSize(30);
        tv.setGravity(Gravity.CENTER);
        layout.addView(tv);

        setContentView(layout);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }

        stopFreezeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (vibrator != null) vibrator.cancel();
                finish();
            }
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopFreezeReceiver, new IntentFilter("ACTION_STOP_FREEZE"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopFreezeReceiver, new IntentFilter("ACTION_STOP_FREEZE"));
        }
    }

    @Override
    public void onBackPressed() { }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vibrator != null) vibrator.cancel();
        try { unregisterReceiver(stopFreezeReceiver); } catch (Exception e) {}
    }
}
