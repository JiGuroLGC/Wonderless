package com.jiguro.wonderless;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    // 共享偏好存储相关常量
    private static final String SP_NAME = "module_sp";
    private static final String KEY_AGREED = "user_agreed";
    private static final String KEY_XZ_APK_DELETED = "xz_apk_deleted"; // 标记xz.apk文件是否已删除
    private static final String UPDATE_URL = 
    "https://gitee.com/JiGuro/wonderless/raw/master/update.json"; // 更新检查URL

    // 卡片视图控件引用
    private TextView toggleAppStore, toggleCleaner, toggleDays, toggleScanner, toggleToolbox;
    private LinearLayout featuresAppStore, featuresCleaner, featuresDays, featuresScanner, featuresToolbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 启动时检查删除遗留文件
        deleteXzApkIfNeeded();
        
        // 此处省略部分代码...

        // 检查用户是否同意协议
        if (!isUserAgreed()) {
            showAgreementDialog(); // 显示协议弹窗
            return; // 未同意则中断后续初始化
        }

        setContentView(R.layout.main);
        // 根据激活状态设置标题
        setTitle(activate() ? "不奇妙应用，神界已成" : "不奇妙应用，蓄势未发");

        // 初始化卡片展开/收起功能
        initCardToggles();

        // 设置官网链接点击事件
        TextView tvWebsite = findViewById(R.id.tv_website);
        if (tvWebsite != null) {
            tvWebsite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openWebsite("https://jigurolgc.github.io"); // 打开作者官网
                    }
                });
        }

        // 头像点击事件
        ImageView ivAvatar = findViewById(R.id.iv_avatar);
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "戳我头干啥？你找死啊！", Toast.LENGTH_SHORT).show();
                    }
                });
        }

        // 启动异步更新检查
        new Thread(new Runnable() {
                @Override
                public void run() {
                    checkUpdate(); // 后台线程检查更新
                }
            }).start();
    }

    /**
     * 删除遗留的xz.apk文件 (因为之前脑袋一热加了个云注入)
     */
    private void deleteXzApkIfNeeded() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        
        // 检查是否已处理过该文件
        if (sp.getBoolean(KEY_XZ_APK_DELETED, false)) {
            return; // 已处理则跳过
        }
        
        File xzApk = new File(getCacheDir(), "xz.apk");
        
        if (xzApk.exists()) {
            // 存在则尝试删除
            if (xzApk.delete()) {
                // 删除成功记录状态
                sp.edit().putBoolean(KEY_XZ_APK_DELETED, true).apply();
            }
        } else {
            // 文件不存在也记录状态
            sp.edit().putBoolean(KEY_XZ_APK_DELETED, true).apply();
        }
    }

    /**
     * 显示安全风险警告对话框（3秒后强制退出）
     */
    private void showTamperedAppDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("安全检测异常")
            .setMessage("检测到应用修改痕迹或存在安全风险！\n为了您的系统安全，程序将会自动退出。\n请下载正版软件或清空存储重试。")
            .setCancelable(false)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish(); // 退出应用
                }
            })
            .create();

        dialog.show();

        // 设置3秒自动关闭
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing() && !isDestroyed()) {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        finish(); // 确保退出
                    }
                }
            }, 3000);
    }

    // 打开指定URL
    private void openWebsite(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开网站链接", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 初始化卡片切换功能
     */
    private void initCardToggles() {
        // 获取所有卡片控件引用
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

        // 绑定点击事件
        setToggleListener(toggleAppStore, featuresAppStore);
        setToggleListener(toggleCleaner, featuresCleaner);
        setToggleListener(toggleDays, featuresDays);
        setToggleListener(toggleScanner, featuresScanner);
        setToggleListener(toggleToolbox, featuresToolbox);

        // 初始化箭头图标
        setInitialIcons();
    }

    // 设置卡片初始状态（下拉箭头）
    private void setInitialIcons() {
        Drawable downArrow = getResources().getDrawable(R.drawable.ic_arrow_down);
        downArrow.setBounds(0, 0, downArrow.getIntrinsicWidth(), downArrow.getIntrinsicHeight());

        toggleAppStore.setCompoundDrawables(null, null, downArrow, null);
        toggleCleaner.setCompoundDrawables(null, null, downArrow, null);
        toggleDays.setCompoundDrawables(null, null, downArrow, null);
        toggleScanner.setCompoundDrawables(null, null, downArrow, null);
        toggleToolbox.setCompoundDrawables(null, null, downArrow, null);
    }

    // 绑定卡片点击监听器
    private void setToggleListener(final TextView toggleView, final LinearLayout featuresLayout) {
        toggleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleCardFeatures(featuresLayout, toggleView);
                }
            });
    }

    /**
     * 切换卡片展开/收起状态
     */
    private void toggleCardFeatures(LinearLayout features, TextView toggleView) {
        if (features.getVisibility() == View.VISIBLE) {
            // 收起状态
            features.setVisibility(View.GONE);
            toggleView.setText("详情");
            Drawable downArrow = getResources().getDrawable(R.drawable.ic_arrow_down);
            downArrow.setBounds(0, 0, downArrow.getIntrinsicWidth(), downArrow.getIntrinsicHeight());
            toggleView.setCompoundDrawables(null, null, downArrow, null);
        } else {
            // 展开状态
            features.setVisibility(View.VISIBLE);
            toggleView.setText("收起");
            Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_up);
            upArrow.setBounds(0, 0, upArrow.getIntrinsicWidth(), upArrow.getIntrinsicHeight());
            toggleView.setCompoundDrawables(null, null, upArrow, null);
        }
    }

    /**
     * 检查应用更新（网络操作）
     */
    private void checkUpdate() {
        try {
            URL url = new URL(UPDATE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000); // 5秒连接超时
            conn.setReadTimeout(5000);    // 5秒读取超时

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return; // 非200响应跳过
            }

            // 读取响应数据
            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            conn.disconnect();

            // 解析JSON更新信息
            JSONObject json = new JSONObject(sb.toString());
            final UpdateInfo info = new UpdateInfo();
            info.versionName = json.getString("versionName");
            info.updateLog = json.getString("updateLog");
            info.apkUrl = json.getString("apkUrl");

            // 获取当前版本
            String local = getPackageManager()
                .getPackageInfo(getPackageName(), 0).versionName;

            // 版本不一致时提示更新
            if (!info.versionName.equals(local)) {
                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showUpdateDialog(info); // 显示更新弹窗
                        }
                    });
            }
        } catch (Exception e) {
            // 静默处理网络异常
        }
    }

    // 显示更新对话框
    private void showUpdateDialog(final UpdateInfo info) {
        new AlertDialog.Builder(this)
            .setTitle("发现新版本 v" + info.versionName)
            .setMessage(info.updateLog)
            .setCancelable(false)
            .setPositiveButton("立即下载", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startDownload(info.apkUrl); // 启动下载
                    dialog.dismiss();
                }
            })
            .setNegativeButton("以后再说", null) // 无操作
            .show();
    }

    // 通过浏览器打开下载链接
    private void startDownload(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开下载链接", Toast.LENGTH_SHORT).show();
        }
    }

    // 检查用户是否同意协议
    private boolean isUserAgreed() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        return sp.getBoolean(KEY_AGREED, false);
    }

    /**
     * 显示用户协议对话框（首次启动必须同意）
     */
    private void showAgreementDialog() {
        new AlertDialog.Builder(this)
            .setTitle("软件使用声明")
            .setMessage("作者：JiGuro（以下简称\"本人\"或\"声明者\"）\n" +
                        "欢迎使用\"不奇妙应用\"模块（以下简称\"本软件\"）。在使用本软件前，请确保您已仔细阅读并完全理解并同意《软件使用声明》（以下简称\"本声明\"）。未成年人应在监护人的指导下阅读、理解并同意本声明后，方可使用本软件。如您不同意本声明的任何内容，请勿使用本软件。\n" +
                        "本软件资源系本人从互联网第三方公开渠道收集整理，分享仅供技术交流。依据《中华人民共和国计算机软件保护条例》相关规定，此软件仅用于学习和研究软件的设计思想与原理，严禁用于任何商业或非法目的。一旦学习研究目的达成，或用户决定不再用于学习研究目的，应立即将其从存储设备中彻底删除。用户需确保自身使用行为符合《中华人民共和国著作权法》、《中华人民共和国计算机软件保护条例》、《中华人民共和国网络安全法》、《中华人民共和国数据安全法》、《中华人民共和国个人信息保护法》等相关法律法规的规定，一切法律责任由使用者自行承担。\n" +
                        "知识产权严格受法律保护，请勿侵权。若本软件所hook的软件包含或基于开源软件，用户使用本软件时亦需遵守相关开源许可证的条款。本人倡导并大力支持正版软件，正版软件的使用不仅能确保良好的用户体验和稳定的性能，更是对软件开发者创新和努力的尊重与支持。如果您发现本软件对您有帮助或您喜欢它，请积极支持正版。\n" +
                        "在此，特别强调，本人分享本软件纯粹是为了学习交流之目的，不带有任何盈利意图。同时，用户应充分认识到，由于本软件来源的第三方属性及本人能力的局限性，本人无法对本软件的安全性（包括但不限于是否存在计算机病毒、恶意代码、后门程序、安全漏洞或侵犯隐私的功能）、合法性（如版权状态）提供任何形式的保证或担保。在任何情况下，声明者均不对因下载、安装、使用、无法使用或依赖本软件所导致的任何直接、间接、附带、特殊、惩罚性或后果性的损害（包括但不限于数据丢失、系统损坏、业务中断、利润损失、隐私泄露、法律纠纷等）承担任何责任，无论该等责任是基于合同、侵权（包括过失）、严格责任或其他法律理论产生，也无论声明者是否事先被告知该等损害的可能性。本人强烈建议用户通过软件开发者官方渠道获取正版软件以确保安全性和合法性。本人承诺，本软件的分享过程中未主动植入任何计算机病毒、后门程序，也未故意设置任何用于侵犯用户隐私的功能。若用户发现本软件有此类问题，请务必及时联系本人，本人将立即采取删除等措施，坚决阻止本软件的进一步传播。\n" +
                        "在首次运行本软件时，用户将被强制要求通过本软件提供的交互界面（如弹出窗口）完整阅读本声明内容，并需主动选择\"同意\"或\"我已理解\"等类似选项，明确同意本声明内容。用户一旦运行或继续使用本软件，即视为已仔细阅读、完全理解且同意接受本声明的全部内容，并自愿承担因下载、安装、使用本软件所产生的一切法律责任和后果。请务必合法使用本软件，共同维护良好的网络环境和法律秩序。\n" +
                        "本声明的最终解释权归声明者所有。")
            .setCancelable(false) // 必须交互
            .setPositiveButton("我已阅读并同意", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 记录同意状态
                    SharedPreferences.Editor editor = 
                        getSharedPreferences(SP_NAME, MODE_PRIVATE).edit();
                    editor.putBoolean(KEY_AGREED, true);
                    editor.apply();
                    dialog.dismiss();
                    recreate(); // 重启Activity加载主界面
                }
            })
            .setNegativeButton("拒绝并退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish(); // 退出应用
                }
            })
            .show();
    }

    // 激活状态检测
    private boolean activate() {
        return false;
    }

    // 更新信息容器类
    private static class UpdateInfo {
        String versionName;
        String updateLog;
        String apkUrl;
    }
}