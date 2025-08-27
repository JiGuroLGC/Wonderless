package com.jiguro.wonderless;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class PermissionHelper {

    private static final String PREF_KEY_ROOT_GRANTED = "root_granted";

    public interface PermissionCallback {
        void onPermissionResult(boolean granted, String type);
    }
    
    // 对用户申请root权限
    public static void requestRootPermission(final Context context, final PermissionCallback callback) {
        new Thread(new Runnable() {
            public void run() {
                boolean granted = false;
                try {
                    SharedPreferences sp = context.getSharedPreferences(MainActivity.SP_NAME, Context.MODE_PRIVATE);
                    String customSu = sp.getString("su_command", "").trim();
                    String command = customSu.isEmpty() ? "su" : customSu;

                    Process process = Runtime.getRuntime().exec(command + " -c id");
                    int exitCode = process.waitFor();
                    granted = exitCode == 0;
                } catch (Exception e) {
                    granted = false;
                }

                final boolean finalGranted = granted;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        SharedPreferences.Editor ed = context.getSharedPreferences(MainActivity.SP_NAME, Context.MODE_PRIVATE).edit();
                        if (finalGranted) {
                            Toast.makeText(context, "Root权限获取成功", Toast.LENGTH_SHORT).show();
                            ed.putBoolean(PREF_KEY_ROOT_GRANTED, true);
                        } else {
                            Toast.makeText(context, "Root权限获取失败，请尝试对不奇妙应用移除Root隐藏", Toast.LENGTH_SHORT).show();
                            ed.putBoolean(PREF_KEY_ROOT_GRANTED, false);
                        }
                        ed.apply();
                        callback.onPermissionResult(finalGranted, "root");
                    }
                });
            }
        }).start();
    }

    public static void requestPermissionsAutomatically(Context context, PermissionCallback callback) {
        requestRootPermission(context, callback);
    }

    public static boolean hasAnyPermission(Context context) {
        SharedPreferences sp = context.getSharedPreferences(MainActivity.SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_ROOT_GRANTED, false);
    }

    public static void resetPermissionStatus(Context context) {
        context.getSharedPreferences(MainActivity.SP_NAME, Context.MODE_PRIVATE)
               .edit()
               .putBoolean(PREF_KEY_ROOT_GRANTED, false)
               .apply();
    }
}
