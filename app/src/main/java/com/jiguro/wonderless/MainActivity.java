package com.jiguro.wonderless;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.json.JSONObject;

public class MainActivity extends Activity {

    // 包名常量
    private static final String PKG_APPSTORE = "com.magicalstory.AppStore";
    private static final String PKG_CLEANER = "com.magicalstory.cleaner";
    private static final String PKG_DAYS = "com.magicalstory.days";
    private static final String PKG_SCANNER = "com.magicalstory.scanner";
    private static final String PKG_TOOLBOX = "com.magicalstory.toolbox";

    public static final String SP_NAME = "module_sp";
    private static final String KEY_AGREED = "user_agreed";
    private static final String KEY_XZ_APK_DELETED = "xz_apk_deleted";
    public static final String KEY_AUTO_UPDATE = "auto_update_enabled";
    public static final String KEY_HIDE_ICON = "hide_app_icon";

    private static final String UPDATE_URL = 
    "https://gitee.com/JiGuro/wonderless/raw/master/update.json";
    private static final String GITHUB_URL = "https://github.com/JiGuroLGC/Wonderless";
    private static final String GITEE_URL = "https://gitee.com/JiGuro/wonderless";

    // 桌面图标别名的完整类名
    private static final String ALIAS_ACTIVITY_NAME = "com.jiguro.wonderless.LauncherAlias";

    // 卡片视图引用
    private TextView toggleAppStore, toggleCleaner, toggleDays, toggleScanner, toggleToolbox;
    private LinearLayout featuresAppStore, featuresCleaner, featuresDays, featuresScanner, featuresToolbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 沉浸式深色图标
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);   // 透明，让 ActionBar 颜色透出来
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;  // 黑色图标
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        // 删除云注入的xz.apk文件
        deleteXzApkIfNeeded();

        // 此处省略部分代码...

        // 用户协议检查
        if (!isUserAgreed()) {
            showAgreementDialog();
            return;
        }

        setContentView(R.layout.main);
        setTitle(activate() ? "不奇妙应用，神界已成" : "不奇妙应用，蓄势未发");

        // 初始化图标状态
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        boolean hideIcon = sp.getBoolean(KEY_HIDE_ICON, false);

        // 调用静态方法，传入当前上下文
        setLauncherIconVisible(this, !hideIcon); // 修改这里

        // 初始化卡片功能切换
        initCardToggles();

        // 加载一言
        initHitokotoText();

        // 设置官网链接点击事件
        TextView tvWebsite = findViewById(R.id.tv_website);
        if (tvWebsite != null) {
            tvWebsite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openWebsite("https://jigurolgc.github.io");
                    }
                });
        }

        // 设置头像点击事件
        ImageView ivAvatar = findViewById(R.id.iv_avatar);
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "戳我头干啥？你找死啊！", Toast.LENGTH_SHORT).show();
                    }
                });
        }

        // 设置软件图标点击事件
        setupAppIconClicks();

        // 启动后异步检查更新
        new Thread(new Runnable() {
                @Override
                public void run() {
                    checkUpdate(MainActivity.this);
                }
            }).start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_main);
        if (menuItem != null) {
            Drawable icon = menuItem.getIcon();
            if (icon != null) {
                int color = getResources().getColor(R.color.action_bar_menu_icon);
                icon.mutate().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.menu_github) {
            openWebsite(GITHUB_URL);
            return true;
        } else if (id == R.id.menu_gitee) {
            openWebsite(GITEE_URL);
            return true;
        } else if (id == R.id.menu_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteXzApkIfNeeded() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);

        // 检查是否已经记录过删除状态
        if (sp.getBoolean(KEY_XZ_APK_DELETED, false)) {
            return; // 已记录状态，不再尝试删除
        }

        File xzApk = new File(getCacheDir(), "xz.apk");

        if (xzApk.exists()) {
            // 文件存在，尝试删除
            if (xzApk.delete()) {
                // 删除成功，记录状态避免后续删除
                sp.edit().putBoolean(KEY_XZ_APK_DELETED, true).apply();
            }
        } else {
            // 文件不存在，记录状态避免后续检查
            sp.edit().putBoolean(KEY_XZ_APK_DELETED, true).apply();
        }
    }

    // 此处省略部分代码...

    private void showTamperedAppDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("安全检测异常")
            .setMessage("检测到应用修改痕迹或存在安全风险！\n为了您的系统安全，程序将会自动退出。\n请下载正版软件或清空存储重试。")
            .setCancelable(false)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .create();

        dialog.show();

        // 3秒后自动退出
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing() && !isDestroyed()) {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        finish();
                    }
                }
            }, 3000);
    }

    private void openWebsite(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开网站链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void initCardToggles() {
        // 获取视图引用
        toggleAppStore = findViewById(R.id.toggle_appstore);
        toggleCleaner = findViewById(R.id.toggle_cleaner);
        toggleDays = findViewById(R.id.toggle_days);
        toggleScanner = findViewById(R.id.toggle_scanner);
        toggleToolbox = findViewById(R.id.toggle_toolbox);

        featuresAppStore = findViewById(R.id.features_appstore);
        featuresCleaner = findViewById(R.id.features_cleaner);
        featuresDays = findViewById(R.id.features_days);
        featuresScanner = findViewById(R.id.features_scanner);
        featuresToolbox = findViewById(R.id.features_toolbox);

        // 设置点击监听器
        setToggleListener(toggleAppStore, featuresAppStore);
        setToggleListener(toggleCleaner, featuresCleaner);
        setToggleListener(toggleDays, featuresDays);
        setToggleListener(toggleScanner, featuresScanner);
        setToggleListener(toggleToolbox, featuresToolbox);

        // 初始状态设置
        setInitialIcons();
    }

    private void setInitialIcons() {
        // 初始设置为下拉箭头
        Drawable downArrow = getResources().getDrawable(R.drawable.ic_arrow_down);
        downArrow.setBounds(0, 0, downArrow.getIntrinsicWidth(), downArrow.getIntrinsicHeight());

        toggleAppStore.setCompoundDrawables(null, null, downArrow, null);
        toggleCleaner.setCompoundDrawables(null, null, downArrow, null);
        toggleDays.setCompoundDrawables(null, null, downArrow, null);
        toggleScanner.setCompoundDrawables(null, null, downArrow, null);
        toggleToolbox.setCompoundDrawables(null, null, downArrow, null);
    }

    private void setToggleListener(final TextView toggleView, final LinearLayout featuresLayout) {
        toggleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleCardFeatures(featuresLayout, toggleView);
                }
            });
    }

    private void toggleCardFeatures(LinearLayout features, TextView toggleView) {
        if (features.getVisibility() == View.VISIBLE) {
            features.setVisibility(View.GONE);
            toggleView.setText("详情");
            Drawable downArrow = getResources().getDrawable(R.drawable.ic_arrow_down);
            downArrow.setBounds(0, 0, downArrow.getIntrinsicWidth(), downArrow.getIntrinsicHeight());
            toggleView.setCompoundDrawables(null, null, downArrow, null);
        } else {
            features.setVisibility(View.VISIBLE);
            toggleView.setText("收起");
            Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_up);
            upArrow.setBounds(0, 0, upArrow.getIntrinsicWidth(), upArrow.getIntrinsicHeight());
            toggleView.setCompoundDrawables(null, null, upArrow, null);
        }
    }

    private void setupAppIconClicks() {
        // 获取图标引用
        ImageView iconAppStore = findViewById(R.id.icon_appstore);
        ImageView iconCleaner = findViewById(R.id.icon_cleaner);
        ImageView iconDays = findViewById(R.id.icon_days);
        ImageView iconScanner = findViewById(R.id.icon_scanner);
        ImageView iconToolbox = findViewById(R.id.icon_toolbox);

        // 设置点击监听
        setAppLaunchOnClick(iconAppStore, PKG_APPSTORE);
        setAppLaunchOnClick(iconCleaner, PKG_CLEANER);
        setAppLaunchOnClick(iconDays, PKG_DAYS);
        setAppLaunchOnClick(iconScanner, PKG_SCANNER);
        setAppLaunchOnClick(iconToolbox, PKG_TOOLBOX);
    }

    private void setAppLaunchOnClick(ImageView imageView, final String packageName) {
        imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchApp(packageName);
                }
            });
    }

    private void launchApp(String packageName) {
        try {
            // 尝试直接启动应用
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
            } else {
                // 应用未安装时提示
                Toast.makeText(this, getAppName(packageName) + " 未安装", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getAppName(String packageName) {
        // 简化的应用名称映射
        switch (packageName) {
            case PKG_APPSTORE: return "奇妙应用";
            case PKG_CLEANER: return "安卓清理君";
            case PKG_DAYS: return "朝花夕拾";
            case PKG_SCANNER: return "奇妙扫描";
            case PKG_TOOLBOX: return "奇妙工具箱";
            default: return "该应用";
        }
    }

    public static void checkUpdate(final Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (!sp.getBoolean(KEY_AUTO_UPDATE, true)) {
            return; // 用户关闭了自动检查
        }

        try {
            URL url = new URL(UPDATE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return;

            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            conn.disconnect();

            final UpdateInfo info = new UpdateInfo();
            JSONObject json = new JSONObject(sb.toString());
            info.versionName = json.getString("versionName");
            info.updateLog   = json.getString("updateLog");
            info.apkUrl      = json.getString("apkUrl");

            String local = ctx.getPackageManager()
                .getPackageInfo(ctx.getPackageName(), 0).versionName;

            if (!info.versionName.equals(local)) {
                if (ctx instanceof Activity) {
                    ((Activity) ctx).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showUpdateDialog(ctx, info);
                            }
                        });
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(new Runnable() {
                            @Override
                            public void run() {
                                showUpdateDialog(ctx, info);
                            }
                        });
                }
            }
        } catch (Exception e) {
            // 静默
        }
    }


    private static void showUpdateDialog(final Context ctx, final UpdateInfo info) {
        if (ctx instanceof Activity && ((Activity) ctx).isFinishing()) return;

        new AlertDialog.Builder(ctx)
            .setTitle("发现新版本 v" + info.versionName)
            .setMessage(info.updateLog)
            .setCancelable(false)
            .setPositiveButton("立即下载", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                                                     Uri.parse(info.apkUrl)));
                    } catch (Exception e) {
                        Toast.makeText(ctx, "无法打开下载链接", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("以后再说", null)
            .show();
    }

    private void initHitokotoText() {
        new Thread(new Runnable() {
                public void run() {
                    String sentence = "";
                    HttpURLConnection conn = null;
                    BufferedReader br = null;
                    try {
                        // 精心调制的一言API
                        URL url = new URL("https://v1.hitokoto.cn/?c=i&c=j&c=f&c=h&encode=text&max_length=30");
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(3000);
                        conn.setReadTimeout(3000);

                        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            br = new BufferedReader(
                                new InputStreamReader(conn.getInputStream(), "UTF-8"));
                            sentence = br.readLine();
                        }
                    } catch (IOException e) {
                        // 网络失败，从本地读取
                        sentence = getRandomSentenceFromAssets();
                    } finally {
                        // 手动关闭
                        if (br != null) {
                            try { br.close(); } catch (IOException ignored) {}
                        }
                        if (conn != null) {
                            conn.disconnect();
                        }
                    }
                    
                    // 实在不行就显示雷总的名言吧
                    if (sentence == null || sentence.trim().equals("")) {
                        sentence = "永远相信美好的事情即将发生...";
                    }

                    final String finalSentence = sentence;
                    runOnUiThread(new Runnable() {
                            public void run() {
                                TextView hitokotoText = (TextView) findViewById(R.id.hitokoto_text);
                                if (hitokotoText != null) {
                                    hitokotoText.setText(finalSentence);

                                    // 计算最大宽度：屏幕宽 - 64dp
                                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                                    int maxPx = screenWidth - (int)(64 * getResources().getDisplayMetrics().density + 0.5f);
                                    hitokotoText.setMaxWidth(maxPx);

                                    // 复制到剪贴板并提示
                                    hitokotoText.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                android.content.ClipboardManager cm =
                                                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("hitokoto", finalSentence));
                                                Toast.makeText(MainActivity.this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                }
                            }
                        });
                }
            }).start();
    }


    // 从assets读取随机句子的方法
    private String getRandomSentenceFromAssets() {
        List<String> sentences = new ArrayList<String>();
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = getAssets().open("words.txt");
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    sentences.add(line);
                }
            }
        } catch (IOException e) {
            // 读取失败返回空
            return null;
        } finally {
            // 手动关闭流
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }

        if (!sentences.isEmpty()) {
            Random random = new Random();
            return sentences.get(random.nextInt(sentences.size()));
        }
        return null;
    }

    private void startDownload(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开下载链接", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isUserAgreed() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        return sp.getBoolean(KEY_AGREED, false);
    }

    private void showAgreementDialog() {

        new AlertDialog.Builder(this)
            .setTitle("软件使用声明")
            .setMessage("作者：JiGuro（以下简称\"本人\"或\"声明者\"）\n" +
                        "欢迎使用\"不奇妙应用\"模块（以下简称\"本软件\"），这是一个对作用域软件（以下简称\"Hook 软件\"）进行研究的模块。在使用本软件前，请确保您已仔细阅读并完全理解并同意《软件使用声明》（以下简称\"本声明\"）。未成年人应在监护人的指导下阅读、理解并同意本声明后，方可使用本软件。如您不同意本声明的任何内容，请勿使用本软件。\n" +
                        "本软件代码系本人从互联网第三方公开渠道收集整理，分享仅供技术交流。依据《中华人民共和国计算机软件保护条例》相关规定，此软件仅用于学习和研究软件的设计思想与原理，严禁用于任何商业或非法目的。一旦学习研究目的达成，或用户决定不再用于学习研究目的，应立即将其从存储设备中彻底删除。用户需确保自身使用行为符合《中华人民共和国著作权法》、《中华人民共和国计算机软件保护条例》、《中华人民共和国网络安全法》、《中华人民共和国数据安全法》、《中华人民共和国个人信息保护法》等相关法律法规的规定，一切法律责任由使用者自行承担。\n" +
                        "知识产权严格受法律保护，请勿侵权。若本软件所 Hook 软件包含或基于开源软件，用户使用本软件时亦需遵守相关开源许可证的条款。本人倡导并大力支持正版软件，正版软件的使用不仅能确保良好的用户体验和稳定的性能，更是对软件开发者创新和努力的尊重与支持。如果您发现 Hook 软件对您有帮助或您喜欢它，请积极支持正版。\n" +
                        "在此，特别强调，本人分享本软件纯粹是为了学习交流之目的，不带有任何盈利意图。同时，用户应充分认识到，由于本软件代码来源的第三方属性及本人能力的局限性，本人无法对本软件的安全性（包括但不限于是否存在计算机病毒、恶意代码、后门程序、安全漏洞或侵犯隐私的功能）、合法性（如版权状态）提供任何形式的保证或担保。在任何情况下，声明者均不对因下载、安装、使用、无法使用或依赖本软件所导致的任何直接、间接、附带、特殊、惩罚性或后果性的损害（包括但不限于数据丢失、系统损坏、业务中断、利润损失、隐私泄露、法律纠纷等）承担任何责任，无论该等责任是基于合同、侵权（包括过失）、严格责任或其他法律理论产生，也无论声明者是否事先被告知该等损害的可能性。本人强烈建议用户弃用模块，并通过软件开发者官方渠道获取正版软件以确保安全性和合法性。本人承诺，本软件的分享过程中未主动植入任何计算机病毒、后门程序，也未故意设置任何用于侵犯用户隐私的功能。若用户发现本软件有此类问题，请务必及时联系本人，本人将立即采取删除等措施，坚决阻止本软件的进一步传播。\n" +
                        "在首次运行本软件时，用户将被强制要求通过本软件提供的交互界面（如弹出窗口）完整阅读本声明内容，并需主动选择\"同意\"或\"我已理解\"等类似选项，明确同意本声明内容。用户一旦运行或继续使用本软件，即视为已仔细阅读、完全理解且同意接受本声明的全部内容，并自愿承担因下载、安装、使用本软件所产生的一切法律责任和后果。请务必合法使用本软件，共同维护良好的网络环境和法律秩序。\n" +
                        "本声明的最终解释权归声明者所有。")
            .setCancelable(false)
            .setPositiveButton("我已阅读并同意", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = 
                        getSharedPreferences(SP_NAME, MODE_PRIVATE).edit();
                    editor.putBoolean(KEY_AGREED, true);
                    editor.apply();
                    dialog.dismiss();
                    recreate();
                }
            })
            .setNegativeButton("拒绝并退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            })
            .show();
    }

    private boolean activate() {
        return com.jiguro.wonderless.ModuleStatus.activated;
    }

    // 内部类用于存储更新信息
    private static class UpdateInfo {
        String versionName;
        String updateLog;
        String apkUrl;
    }

    /************************ 图标控制方法 ************************/

    /**
     * 设置桌面图标的可见性（仅控制桌面图标）
     */
    public static void setLauncherIconVisible(Context context, boolean visible) {
        ComponentName aliasComponent = new ComponentName(context, "com.jiguro.wonderless.LauncherAlias");
        PackageManager manager = context.getPackageManager();

        int currentState = manager.getComponentEnabledSetting(aliasComponent);
        int newState = visible ? 
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED : 
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        // 状态相同则无需操作
        if ((currentState == newState) || 
            (currentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && visible)) {
            return;
        }

        manager.setComponentEnabledSetting(
            aliasComponent, 
            newState, 
            PackageManager.DONT_KILL_APP
        );

        // 部分手机需要刷新桌面
        refreshLauncher(context);
    }

    /**
     * 刷新桌面应用列表
     */
    private static void refreshLauncher(Context context) {
        try {
            // 发送刷新广播（通用）
            Intent intent = new Intent("com.android.launcher3.action.REFRESH_LAUNCHER");
            context.sendBroadcast(intent);

            // 针对特定桌面（小米、华为等）
            Intent miuiIntent = new Intent("miui.intent.action.REFRESH_LAUNCHER");
            context.sendBroadcast(miuiIntent);

            Intent huaweiIntent = new Intent("com.huawei.android.launcher.action.REFRESH_LAUNCHER");
            context.sendBroadcast(huaweiIntent);

            // 标准方式
            Intent stdIntent = new Intent(Intent.ACTION_MAIN);
            stdIntent.addCategory(Intent.CATEGORY_HOME);
            context.sendBroadcast(stdIntent);
        } catch (Exception e) {
            // 忽略异常
        }
    }

}
