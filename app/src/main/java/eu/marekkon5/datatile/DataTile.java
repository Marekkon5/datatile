package eu.marekkon5.datatile;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Calendar;

public class DataTile extends TileService {


    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onClick() {
        super.onClick();
        toggleData(!isDataOn());
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //ignore lol
        }
        updateTile();
    }



    // Update tile info
    void updateTile() {
        NetworkUsage networkUsage = getDataUsage();
        Tile tile = getQsTile();
        tile.setLabel("Mobile data");
        if (networkUsage == null) {
            tile.setSubtitle("N/A");
        } else {
            tile.setSubtitle(DataTile.filesize(networkUsage.todayRx + networkUsage.todayTx));
        }
        tile.setState(isDataOn() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    // Toggle data (requires root)
    private void toggleData(boolean on) {
        String command = "svc data enable";
        if (!on) {
            command = "svc data disable";
        }

        try {
            ProcessBuilder builder = new ProcessBuilder("su", "-c", command);
            Process proc = builder.start();
            proc.wait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Check if data on: https://github.com/CasperVerswijvelt/Better-Internet-Tiles/blob/main/app/src/main/java/be/casperverswijvelt/unifiedinternetqs/util/Util.kt
    private boolean isDataOn() {
        ConnectivityManager connectivityManager = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        try {
            Class c = Class.forName(connectivityManager.getClass().getName());
            Method method = c.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            boolean result = (boolean)method.invoke(connectivityManager);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback
        return Settings.Secure.getInt(getContentResolver(), "mobile_data", 1) == 1;
    }

    NetworkUsage getDataUsage() {
        try {
            NetworkStatsManager statsManager = this.getSystemService(NetworkStatsManager.class);
            long currentTime = System.currentTimeMillis();
            NetworkStats.Bucket totalStats = statsManager.querySummaryForDevice(0, null, 0, currentTime);

            // Get today stats
            Calendar calendar = Calendar.getInstance();
            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);
            int seconds = calendar.get(Calendar.SECOND);
            long offset = (seconds + minutes * 60 + hours * 3600) * 1000;
            NetworkStats.Bucket todayStats = statsManager.querySummaryForDevice(0, null, currentTime - offset, currentTime);

            NetworkUsage usage = new NetworkUsage(todayStats.getRxBytes(), todayStats.getTxBytes(), totalStats.getRxBytes(), totalStats.getTxBytes());
            return usage;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    class NetworkUsage {
        long todayRx;
        long todayTx;
        long totalRx;
        long totalTx;

        NetworkUsage(long todayRx, long todayTx, long totalRx, long totalTx) {
            this.todayRx = todayRx;
            this.todayTx = todayTx;
            this.totalRx = totalRx;
            this.totalTx = totalTx;
        }
    }

    // Source: https://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc
    public static String filesize(long size) {
        if(size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}

