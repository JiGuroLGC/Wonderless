package com.jiguro.wonderless;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SettingsActivity extends Activity {

    private static final String KEY_SUPPRESS_TOAST = "suppress_toast";
    private static final String KEY_AGREED         = "user_agreed";
    private static final String SP_NAME            = "module_sp";

    private Switch switchUpdate;
    private Switch switchHideIcon;
    private Switch switchSuppressToast;
    private Switch switchUseRoot;
    private EditText etSuCommand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 此处省略部分代码...

        // 沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.action_bar_background));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        setContentView(R.layout.activity_settings);

        // ActionBar 返回箭头
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle("设置");
            ab.setElevation(4);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // 返回按钮点击
        ImageView backButton = (ImageView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // 统一 SharedPreferences
        final SharedPreferences sp = getSharedPreferences(MainActivity.SP_NAME, MODE_PRIVATE);

        // 自动检查更新
        switchUpdate = (Switch) findViewById(R.id.switch_update);
        switchUpdate.setChecked(sp.getBoolean(MainActivity.KEY_AUTO_UPDATE, true));
        switchUpdate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sp.edit().putBoolean(MainActivity.KEY_AUTO_UPDATE, isChecked).apply();
                if (isChecked) {
                    new Thread(new Runnable() {
                        public void run() {
                            MainActivity.checkUpdate(SettingsActivity.this);
                        }
                    }).start();
                }
            }
        });

        // 隐藏桌面图标
        switchHideIcon = (Switch) findViewById(R.id.switch_hide_icon);
        switchHideIcon.setChecked(sp.getBoolean(MainActivity.KEY_HIDE_ICON, false));
        switchHideIcon.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sp.edit().putBoolean(MainActivity.KEY_HIDE_ICON, isChecked).apply();
                MainActivity.setLauncherIconVisible(getApplicationContext(), !isChecked);
                Toast.makeText(SettingsActivity.this,
                        isChecked ? "桌面图标已隐藏，可通过模块管理器访问" : "桌面图标已恢复显示",
                        Toast.LENGTH_LONG).show();
            }
        });

        // 屏蔽启动提示
        switchSuppressToast = (Switch) findViewById(R.id.switch_suppress_toast);
        switchSuppressToast.setChecked(sp.getBoolean(KEY_SUPPRESS_TOAST, false));
        switchSuppressToast.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sp.edit().putBoolean(KEY_SUPPRESS_TOAST, isChecked).apply();
                if (checkStoragePermission()) {
                    ensureToastConfigInCorrectPlace(isChecked);
                } else {
                    requestStoragePermission();
                }
            }
        });

        // 使用 Root 权限
        switchUseRoot = (Switch) findViewById(R.id.switch_use_root);
        etSuCommand   = (EditText) findViewById(R.id.et_su_command);

        switchUseRoot.setChecked(sp.getBoolean("use_root", false));
        etSuCommand.setText(sp.getString("su_command", ""));
        etSuCommand.setVisibility(View.VISIBLE);

        switchUseRoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    PermissionHelper.requestRootPermission(SettingsActivity.this,
                        new PermissionHelper.PermissionCallback() {
                            public void onPermissionResult(boolean granted, String type) {
                                if (granted) {
                                    sp.edit().putBoolean("use_root", true).apply();
                                    etSuCommand.setVisibility(View.VISIBLE);
                                    ensureToastConfigInCorrectPlace(
                                            sp.getBoolean(KEY_SUPPRESS_TOAST, false));
                                    Toast.makeText(SettingsActivity.this,
                                            "已启用Root权限模式", Toast.LENGTH_SHORT).show();
                                } else {
                                    switchUseRoot.setChecked(false);
                                    sp.edit().putBoolean("use_root", false).apply();
                                    etSuCommand.setVisibility(View.GONE);
                                }
                            }
                        });
                } else {
                    sp.edit().putBoolean("use_root", false).apply();
                    etSuCommand.setVisibility(View.VISIBLE);
                    PermissionHelper.resetPermissionStatus(SettingsActivity.this);

                    // 关闭 Root 时，把私有目录 toast_config 移到公共目录并删除私有
                    String su = sp.getString("su_command", "").trim();
                    String cmd = su.length() == 0 ? "su" : su;

                    String[] pkgs = {
                        "com.magicalstory.AppStore",
                        "com.magicalstory.cleaner",
                        "com.magicalstory.days",
                        "com.magicalstory.scanner",
                        "com.magicalstory.toolbox"
                    };

                    File publicDir  = new File(Environment.getExternalStorageDirectory(), ".wonderless");
                    File publicFile = new File(publicDir, "toast_config");

                    for (String pkg : pkgs) {
                        String privateFile = "/storage/emulated/0/Android/data/" + pkg + "/files/.wonderless/toast_config";
                        try {
                            Runtime.getRuntime().exec(new String[]{
                                cmd, "-c",
                                "if [ -f '" + privateFile + "' ]; then " +
                                "  mkdir -p '" + publicDir.getAbsolutePath() + "' && " +
                                "  mv '" + privateFile + "' '" + publicFile.getAbsolutePath() + "' ; " +
                                "fi"
                            }).waitFor();
                            Runtime.getRuntime().exec(new String[]{cmd, "-c", "rm -f '" + privateFile + "'"}).waitFor();
                        } catch (Exception ignored) {}
                    }

                    ensureToastConfigInCorrectPlace(sp.getBoolean(KEY_SUPPRESS_TOAST, false));
                    Toast.makeText(SettingsActivity.this, "已关闭Root权限模式", Toast.LENGTH_SHORT).show();
                }
            }
        });

        etSuCommand.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                sp.edit().putString("su_command", s.toString().trim()).apply();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // 撤回软件使用声明
        final Switch switchRevoke = (Switch) findViewById(R.id.switch_revoke_agreement);
        switchRevoke.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    switchRevoke.setChecked(false); // 防二次触发
                    new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("确认撤回")
                        .setMessage("确定要撤回软件使用声明吗？软件将会重载，您需再次同意声明才能使用。")
                        .setCancelable(false)
                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.Editor editor =
                                    getSharedPreferences(MainActivity.SP_NAME, MODE_PRIVATE).edit();
                                editor.putBoolean(KEY_AGREED, false);
                                editor.commit();
                                finish();
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                }
            }
        });
    }

    /* ================== 权限相关 ================== */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ensureToastConfigInCorrectPlace(switchSuppressToast.isChecked());
            } else {
                Toast.makeText(this, "权限被拒绝，无法保存设置", Toast.LENGTH_SHORT).show();
                switchSuppressToast.setChecked(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ================== 统一处理 toast_config ================== */
    private void ensureToastConfigInCorrectPlace(boolean suppressToast) {
        SharedPreferences sp = getSharedPreferences(MainActivity.SP_NAME, MODE_PRIVATE);
        boolean hasRootMode = sp.getBoolean("use_root", false);

        File publicDir  = new File(Environment.getExternalStorageDirectory(), ".wonderless");
        File publicFile = new File(publicDir, "toast_config");

        String[] pkgs = {
            "com.magicalstory.AppStore",
            "com.magicalstory.cleaner",
            "com.magicalstory.days",
            "com.magicalstory.scanner",
            "com.magicalstory.toolbox"
        };

        if (suppressToast) {
            if (hasRootMode) {
                String su = sp.getString("su_command", "").trim();
                String cmd = su.length() == 0 ? "su" : su;
                for (String pkg : pkgs) {
                    String dir  = "/storage/emulated/0/Android/data/" + pkg + "/files/.wonderless";
                    String file = dir + "/toast_config";
                    try {
                        Runtime.getRuntime().exec(new String[]{
                            cmd, "-c",
                            "mkdir -p '" + dir + "' && echo 1 > '" + file + "' && chmod 777 '" + file + "'"
                        }).waitFor();
                    } catch (Exception ignored) {}
                }
                if (publicFile.exists()) publicFile.delete();
            } else {
                try {
                    if (!publicDir.exists()) publicDir.mkdirs();
                    FileOutputStream fos = new FileOutputStream(publicFile);
                    fos.write("1".getBytes());
                    fos.close();
                } catch (IOException ignored) {}
            }
        } else {
            if (hasRootMode) {
                String su = sp.getString("su_command", "").trim();
                String cmd = su.length() == 0 ? "su" : su;
                for (String pkg : pkgs) {
                    String file = "/storage/emulated/0/Android/data/" + pkg + "/files/.wonderless/toast_config";
                    try {
                        Runtime.getRuntime().exec(new String[]{cmd, "-c", "rm -f '" + file + "'"}).waitFor();
                    } catch (Exception ignored) {}
                }
            }
            if (publicFile.exists()) publicFile.delete();
        }
    }

    /* ================== 检测 ================== */
    
    // 此处省略部分代码...

    private void showTamperedAppDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("安全检测异常")
            .setMessage("检测到应用修改痕迹或存在安全风险！\n为了您的系统安全，程序将会自动退出。\n请下载正版软件或清空存储重试。")
            .setCancelable(false)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .create();
        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    if (dialog.isShowing()) dialog.dismiss();
                    finish();
                }
            }
        }, 3000);
    }
}
