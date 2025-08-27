package com.jiguro.wonderless;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

public class ConfigManager {

    /* ========== 路径常量 ========== */
    private static final String OLD_DIR  = "/storage/emulated/0/.wonderless";
    private static final String OLD_FILE = OLD_DIR + "/config.json";

    /* ========== 运行时路径 ========== */
    private static String sConfigFile;   // 完整文件路径

    /* ========== 内部工具，拿到最终路径 ========== */
    private static String getConfigFile(Context ctx) {
        if (sConfigFile == null) {
            // 新路径
            File newDir = new File(ctx.getExternalFilesDir(null), ".wonderless");
            File newFile = new File(newDir, "config.json");

            // 尝试创建目录和空文件，看是否成功
            boolean ok = false;
            try {
                if (!newDir.exists()) newDir.mkdirs();
                if (newDir.exists()) {
                    if (!newFile.exists()) newFile.createNewFile();
                    ok = newFile.exists() && newFile.canWrite();
                }
            } catch (Throwable ignore) { /* 任何异常都视为失败 */ }

            sConfigFile = ok ? newFile.getAbsolutePath() : OLD_FILE;
        }
        return sConfigFile;
    }

    /* ========== 读 ========== */
    public static JSONObject getConfig(Context ctx) {
        File file = new File(getConfigFile(ctx));
        if (!file.exists()) return new JSONObject();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buf = new byte[(int) file.length()];
            fis.read(buf);
            return new JSONObject(new String(buf, "UTF-8"));
        } catch (IOException | JSONException e) {
            Log.e("ConfigManager", "读取配置失败", e);
            return new JSONObject();
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
        }
    }

    /* ========== 写 ========== */
    public static void saveConfig(Context ctx, JSONObject cfg) {
        File file = new File(getConfigFile(ctx));
        FileOutputStream fos = null;
        try {
            File dir = file.getParentFile();
            if (!dir.exists()) dir.mkdirs();
            fos = new FileOutputStream(file);
            fos.write(cfg.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            Log.e("ConfigManager", "保存配置失败", e);
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
        }
    }

    /* ========== get/put ========== */
    public static boolean getBoolean(Context ctx, String key, boolean def) {
        try { return getConfig(ctx).getBoolean(key); } catch (JSONException e) { return def; }
    }
    public static void putBoolean(Context ctx, String key, boolean val) {
        try { JSONObject c = getConfig(ctx); c.put(key, val); saveConfig(ctx, c); } catch (JSONException ignored) {}
    }
    public static int getInt(Context ctx, String key, int def) {
        try { return getConfig(ctx).getInt(key); } catch (JSONException e) { return def; }
    }
    public static void putInt(Context ctx, String key, int value) {
        // 根据 key 取对应限额
        int limit;
        switch (key) {
            case "custom_coin":
            case "custom_concern":
            case "custom_fans":
            case "custom_paynum":
                limit = 1_000_000_000;
                break;
            case "custom_level":
                limit = 10_000;
                break;
            default:
                limit = Integer.MAX_VALUE;   // 其余字段不设限额
        }

        // 超限直接丢弃
        if (value > limit) {
            Log.w("ConfigManager", key + " 超过限额 " + limit + "，已忽略。");
            return;
        }

        // 正常保存
        try {
            JSONObject cfg = getConfig(ctx);
            cfg.put(key, value);
            saveConfig(ctx, cfg);
        } catch (JSONException e) {
            Log.e("ConfigManager", "写入整数值失败", e);
        }
    }

    /* ---------- String 读写 ---------- */
    public static String getString(Context ctx, String key, String def) {
        try { return getConfig(ctx).getString(key); } catch (JSONException e) { return def; }
    }
    public static void putString(Context ctx, String key, String value) {
        try {
            JSONObject cfg = getConfig(ctx);
            cfg.put(key, value);
            saveConfig(ctx, cfg);
        } catch (JSONException ignored) {}
    }


}
