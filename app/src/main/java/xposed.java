import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;


public class xposed implements IXposedHookLoadPackage {

    private static final String KEY_SUPPRESS_TOAST = "suppress_toast";
    private static WeakReference<AlertDialog> activeDialogRef = new WeakReference<AlertDialog>(null);

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam param) throws Throwable {

        // 通过自己Hook自己进行激活校验
        if (param.packageName.equals("com.jiguro.wonderless")) {
            try {
                Class<?> clazz = param.classLoader.loadClass("com.jiguro.wonderless.ModuleStatus"); // 加载校验类
                java.lang.reflect.Field field = clazz.getDeclaredField("activated");
                field.setAccessible(true);
                field.setBoolean(null, true);
            } catch (Throwable ignored) {}
            return;
        }

        // 重构的包名判断
        if (param.packageName.equals("com.magicalstory.AppStore")) {
            hookAppStore(param);
        } else if (param.packageName.equals("com.magicalstory.cleaner")) {
            hookCleaner(param);
        } else if (param.packageName.equals("com.magicalstory.days")) {
            hookDays(param);
        } else if (param.packageName.equals("com.magicalstory.scanner")) {
            hookScanner(param);
        } else if (param.packageName.equals("com.magicalstory.toolbox")) {
            hookToolbox(param);
        }
    }

    /* ===================== 奇妙应用 ===================== */
    private void hookAppStore(final XC_LoadPackage.LoadPackageParam param) {
        XposedHelpers.findAndHookMethod(Application.class, "attach",
            Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam attachParam) throws Throwable {
                    final Context ctx = (Context) attachParam.args[0];
                    final ClassLoader cl = ctx.getClassLoader();

                    jiguroMessage(ctx, "領域展開，りょういきてんかい !");

                    // 设置界面弹窗
                    XposedHelpers.findAndHookMethod(
                        "com.magicalstory.AppStore.setting.settingActivity",
                        cl, "onCreate", Bundle.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        public void run() {
                                            if (!isDialogShowing()) {
                                                showModuleSettingsDialog((Activity) param.thisObject);
                                            }
                                        }
                                    }, 500);
                            }
                        });

                    /* Hook 1：会员 */
                    try {
                        XposedHelpers.findAndHookMethod("y9.d0", cl, "W", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_vip", true))
                                        param.setResult(true);
                                }
                            });
                    } catch (Throwable t) {
                        XposedBridge.log("破解奇妙应用会员失败: " + t);
                    }

                    /* Hook 2：时间 */
                    try {
                        XposedHelpers.findAndHookMethod("com.tencent.mmkv.MMKV", cl, "d", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_time", true))
                                        param.setResult(4070908800L);
                                }
                            });
                    } catch (Throwable t) {
                        XposedBridge.log("破解奇妙应用时间失败: " + t);
                    }

                    /* Hook 3/4 附件硬币 = 0 */
                    hookZeroCoin("com.magicalstory.AppStore.entity.section.editor", "getCoin", cl, ctx);
                    hookZeroCoin("com.magicalstory.AppStore.entity.section.item_post", "getCoin", cl, ctx);

                    /* Hook 5：硬币总数自定义 */
                    try {
                        XposedHelpers.findAndHookMethod(
                            "com.magicalstory.AppStore.entity.user.user_from_net",
                            cl, "getCoinnum", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_custom_coin", true)) {
                                        param.setResult(com.jiguro.wonderless.ConfigManager.getInt(ctx, "custom_coin", 114514));
                                    }
                                }
                            });
                    } catch (Throwable t) {
                        XposedBridge.log("破解奇妙应用硬币总数失败: " + t);
                    }

                    /* Hook 6：粉丝数自定义 */
                    try {
                        XposedHelpers.findAndHookMethod(
                            "com.magicalstory.AppStore.entity.user.user_from_net",
                            cl, "getFans", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_custom_fans", true)) {
                                        param.setResult(com.jiguro.wonderless.ConfigManager.getInt(ctx, "custom_fans", 520));
                                    }
                                }
                            });
                    } catch (Throwable t) {
                        XposedBridge.log("破解奇妙应用粉丝数失败: " + t);
                    }

                    /* Hook 7：付费数自定义 */
                    try {
                        XposedHelpers.findAndHookMethod(
                            "com.magicalstory.AppStore.entity.user.user_from_net",
                            cl, "getPayNum", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_custom_paynum", true)) {
                                        param.setResult(com.jiguro.wonderless.ConfigManager.getInt(ctx, "custom_paynum", 1314));
                                    }
                                }
                            });
                        XposedHelpers.findAndHookMethod(
                            "com.magicalstory.AppStore.entity.user.user",
                            cl, "getPayNum", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_custom_paynum", true)) {
                                        param.setResult(com.jiguro.wonderless.ConfigManager.getInt(ctx, "custom_paynum", 1314));
                                    }
                                }
                            });
                    } catch (Throwable t) {
                        XposedBridge.log("破解奇妙应用付费数失败: " + t);
                    }

                    /* Hook 8：自定义关注数 */
                    try {
                        XposedHelpers.findAndHookMethod(
                            "com.magicalstory.AppStore.entity.user.user_from_net",
                            cl, "getConcern", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_custom_concern", true)) {
                                        param.setResult(com.jiguro.wonderless.ConfigManager.getInt(ctx, "custom_concern", 666));
                                    }
                                }
                            });
                    } catch (Throwable t) {
                        XposedBridge.log("破解奇妙应用付费数失败: " + t);
                    }

                    /* Hook 9：等级经验值自定义 */
                    try {
                        XposedHelpers.findAndHookMethod(
                            "com.magicalstory.AppStore.entity.user.user_from_net",
                            cl, "getExperience", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_custom_level", true)) {
                                        int level = com.jiguro.wonderless.ConfigManager.getInt(ctx, "custom_level", 99);
                                        double experience = Math.ceil(Math.pow(level - 1, 1.5) * 1000);
                                        param.setResult((int) experience);
                                    }
                                }
                            });
                    } catch (Throwable t) {
                        XposedBridge.log("破解奇妙应用等级经验值失败: " + t);
                    }

                    // 移除广告
                    hookActivity(cl);

                    /* 自动签到功能 */
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_auto_sign", false)) {
                                    performAutoSign(ctx, cl);
                                }
                            }
                        }, 1000); // 延迟1秒执行，确保初始化完成

                }
            });
    }


    /* ===================== 清理君 ===================== */
    private void hookCleaner(final XC_LoadPackage.LoadPackageParam param) {
        XposedHelpers.findAndHookMethod(Application.class, "attach",
            Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam attachParam) throws Throwable {
                    final Context ctx = (Context) attachParam.args[0];
                    jiguroMessage(ctx, "領域展開，りょういきてんかい !");

                    hookMMKVLong("com.tencent.mmkv.MMKV", "e", param, 0x3bb2b0c6018L);
                    hookReturnTrue("gb.p0", "d", param);
                }
            });
    }

    /* ===================== 朝花夕拾 ===================== */
    private void hookDays(final XC_LoadPackage.LoadPackageParam param) {
        XposedHelpers.findAndHookMethod(Application.class, "attach",
            Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam attachParam) throws Throwable {
                    final Context ctx = (Context) attachParam.args[0];
                    jiguroMessage(ctx, "領域展開，りょういきてんかい !");

                    hookMMKVLong("com.tencent.mmkv.MMKV", "e", param, 0x5af3107a3fffL);
                    hookReturnTrue("bb.h", "i", param);
                }
            });
    }

    /* ===================== 扫描 ===================== */
    private void hookScanner(final XC_LoadPackage.LoadPackageParam param) {
        XposedHelpers.findAndHookMethod(Application.class, "attach",
            Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam attachParam) throws Throwable {
                    final Context ctx = (Context) attachParam.args[0];
                    jiguroMessage(ctx, "領域展開，りょういきてんかい !");

                    hookMMKVLong("com.tencent.mmkv.MMKV", "OooO0oO", param, 0x5af3107a3fffL);
                    hookReturnOne("com.tencent.mmkv.MMKV", "OooO0Oo", param);
                }
            });
    }

    /* ===================== 工具箱 ===================== */
    private void hookToolbox(final XC_LoadPackage.LoadPackageParam param) {
        XposedHelpers.findAndHookMethod(Application.class, "attach",
            Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam attachParam) throws Throwable {
                    final Context ctx = (Context) attachParam.args[0];
                    jiguroMessage(ctx, "領域展開，りょういきてんかい !");

                    hookReturnLong("uf.l", "s", param, 0x9184e729fffL);
                    hookReturnTrue("uf.l", "t", param);
                }
            });
    }


    /* ===================== 通用 Hook 工具 ===================== */
    private void hookZeroCoin(String cls, String mtd, ClassLoader cl, final Context ctx) {
        try {
            XposedHelpers.findAndHookMethod(cls, cl, mtd, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "enable_coin_unlock", true))
                            param.setResult(0);
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("Hook " + cls + "." + mtd + " 失败: " + t);
        }
    }

    private void hookMMKVLong(String cls, String mtd, XC_LoadPackage.LoadPackageParam param, final long val) {
        try {
            XposedHelpers.findAndHookMethod(cls, param.classLoader, mtd,
                String.class, long.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(val);
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("Hook " + cls + "." + mtd + " 失败: " + t);
        }
    }

    private void hookReturnLong(String cls, String mtd, XC_LoadPackage.LoadPackageParam param, final long val) {
        try {
            XposedHelpers.findAndHookMethod(cls, param.classLoader, mtd,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(val);
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("Hook " + cls + "." + mtd + " 失败: " + t);
        }
    }

    private void hookReturnTrue(String cls, String mtd, XC_LoadPackage.LoadPackageParam param) {
        hookReturnBoolean(cls, mtd, param, true);
    }

    private void hookReturnOne(String cls, String mtd, XC_LoadPackage.LoadPackageParam param) {
        try {
            XposedHelpers.findAndHookMethod(cls, param.classLoader, mtd,
                int.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(1);
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("Hook " + cls + "." + mtd + " 失败: " + t);
        }
    }

    private void hookReturnBoolean(String cls, String mtd, XC_LoadPackage.LoadPackageParam param, final boolean val) {
        try {
            XposedHelpers.findAndHookMethod(cls, param.classLoader, mtd,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(val);
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("Hook " + cls + "." + mtd + " 失败: " + t);
        }
    }

    /* ===================== 工具方法 ===================== */
    // 妈的去个弹窗写了我三天

    private void jiguroMessage(Context context, String msg) {
        // 先看私有目录
        File privateConfig = new File(context.getExternalFilesDir(null),
                                      ".wonderless/toast_config");
        if (privateConfig.exists()) {
            if (readToastFlag(privateConfig) == 1) return;
        }

        // 再看公共目录
        File publicConfig = new File("/storage/emulated/0/.wonderless/toast_config");
        if (readToastFlag(publicConfig) == 1) return;

        // 都没屏蔽才弹 Toast
        try {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            XposedBridge.log("提示异常: " + e);
        }
    }

    // 读 1/0 的小工具
    private int readToastFlag(File f) {
        if (!f.exists()) return 0;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            byte[] buf = new byte[1];
            if (fis.read(buf) == 1) {
                String s = new String(buf);
                return "1".equals(s) ? 1 : 0;
            }
        } catch (Exception ignored) {
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
        }
        return 0;
    }

    // 获取今天的 yyyyMMdd 字符串
    private static String todayString() {
        return new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.CHINA)
            .format(new java.util.Date());
    }

    // 判断今天是否已经签到过
    private static boolean hasSignedToday(Context ctx) {
        String lastDay = com.jiguro.wonderless.ConfigManager.getString(ctx, "last_sign_date", "");
        return todayString().equals(lastDay);
    }

    // 把今天标记为已签到
    private static void markSignedToday(Context ctx) {
        com.jiguro.wonderless.ConfigManager.putString(ctx, "last_sign_date", todayString());
    }

    private void hookActivity(ClassLoader cl) {
        XposedHelpers.findAndHookMethod(
            "com.magicalstory.AppStore.main.MainActivity",
            cl,
            "onCreate",
            Bundle.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Activity act = (Activity) param.thisObject;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                removeById(act);
                            }
                        }, 300);
                }
            });
    }

    // 根据开关状态移除控件
    private static void removeById(Activity act) {
        if (act == null) return;
        final Context ctx = act;

        // 移动广告
        if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "block_move_ad", false)) {
            View v = act.findViewById(0x7f09028c);
            if (v != null) ((ViewGroup) v.getParent()).removeView(v);
        }

        // 主页 Banner
        if (com.jiguro.wonderless.ConfigManager.getBoolean(ctx, "block_home_banner", false)) {
            View v = act.findViewById(0x7f09008c);
            if (v != null) ((ViewGroup) v.getParent()).removeView(v);
        }
    }

    /* ===================== 奇妙应用内部设置弹窗 ===================== */
    private void showModuleSettingsDialog(final Activity activity) {
        /* ------------------ 可滚动根容器 ------------------ */
        ScrollView scrollRoot = new ScrollView(activity);
        scrollRoot.setFillViewport(true);   // 确保子布局能撑满
        scrollRoot.setPadding(0, 0, 0, 0);  // 滚动区域本身不额外留白

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad24 = dp(activity, 24);
        root.setPadding(pad24, pad24, pad24, pad24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF5F5F5);
        bg.setCornerRadius(dp(activity, 12));
        root.setBackground(bg);
        
        // 标题
        TextView title = new TextView(activity);
        title.setText("模块设置");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(0x7F040134);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                         ViewGroup.LayoutParams.MATCH_PARENT,
                         ViewGroup.LayoutParams.WRAP_CONTENT));

        // 开关组
        addSwitch(root, activity, "会员破解", "解锁奇妙应用Premium权益，去除所有广告", "enable_vip", true);
        addSwitch(root, activity, "时间破解", "会员破解附属品，但好像没啥用(／_＼)", "enable_time", true);
        addSwitch(root, activity, "附件透明", "免硬币直接查看帖子附件", "enable_coin_unlock", true);
        addSwitch(root, activity, "自动签到", "启动时自动完成每日签到", "enable_auto_sign", false);
        addSwitch(root, activity, "屏蔽签到提示", "仅在自动签到开启时生效，屏蔽签到状态提示", "enable_sign_toast_block", false);
        addSwitch(root, activity, "Fuck移动", "去除主页的流量卡广告，建议与会员破解共同使用，重启生效", "block_move_ad", false);
        addSwitch(root, activity, "屏蔽主页Banner", "去除主页没卵用的顶部轮播Banner，重启生效", "block_home_banner", false);
        addSwitch(root, activity, "硬币总数修改", "自定义硬币总数，刷新生效", "enable_custom_coin", true);
        addSwitch(root, activity, "关注数修改", "自定义关注数，刷新生效", "enable_custom_concern", true);
        addSwitch(root, activity, "粉丝数修改", "自定义粉丝数，刷新生效", "enable_custom_fans", true);
        addSwitch(root, activity, "付费数修改", "自定义付费数，刷新生效", "enable_custom_paynum", true);
        addSwitch(root, activity, "等级修改", "自定义等级，刷新生效", "enable_custom_level", true);

        // 分割线
        View divider = new View(activity);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 1));
        divLp.setMargins(0, dp(activity, 12), 0, dp(activity, 12));
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(0xFFBBDEFB);
        root.addView(divider);

        // 数值输入
        addNumberInput(root, activity, "硬币总数", "上限为1000000000，保存后立即生效", "custom_coin", 114514);
        addNumberInput(root, activity, "关注数量", "上限为1000000000，保存后立即生效", "custom_concern", 666);
        addNumberInput(root, activity, "粉丝数量", "上限为1000000000，保存后立即生效", "custom_fans", 520);
        addNumberInput(root, activity, "付费数量", "上限为1000000000，保存后立即生效", "custom_paynum", 1314);
        addNumberInput(root, activity, "等级", "上限为10000，保存后立即生效", "custom_level", 99);

        // 确定按钮
        Button ok = new Button(activity);
        ok.setText("确定");
        ok.setTextColor(Color.WHITE);
        ok.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        ok.setTypeface(null, Typeface.BOLD);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(0x7F040134);
        btnBg.setCornerRadius(dp(activity, 8));
        ok.setBackground(btnBg);
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        okLp.topMargin = dp(activity, 16);
        root.addView(ok, okLp);

        /* ------------------ 把 root 放进 ScrollView ------------------ */
        scrollRoot.addView(root, new ScrollView.LayoutParams(
                               ScrollView.LayoutParams.MATCH_PARENT,
                               ScrollView.LayoutParams.WRAP_CONTENT));

        /* ------------------ 创建并显示对话框 ------------------ */
        final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(scrollRoot)     // 注意这里传入 scrollRoot
            .create();
        activeDialogRef = new WeakReference<>(dialog);

        ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dialog.isShowing()) dialog.dismiss();
                }
            });

        dialog.show();
        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawableResource(android.R.color.transparent);
            win.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            win.setElevation(dp(activity, 6));
        }
    }


    // 美化开关项
    private void addSwitch(LinearLayout parent, final Context ctx, 
                           String label, final String hint, 
                           final String key, final boolean def) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(ctx, 8), 0, dp(ctx, 8));

        // 水平布局
        LinearLayout hor = new LinearLayout(ctx);
        hor.setOrientation(LinearLayout.HORIZONTAL);
        hor.setGravity(Gravity.CENTER_VERTICAL);

        // 标签
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTextColor(0xFF444444);  // 深灰色
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                               0, 
                               ViewGroup.LayoutParams.WRAP_CONTENT, 
                               1)); // 权重布局
        hor.addView(tv);

        // 开关
        final Switch sw = new Switch(ctx);
        sw.setChecked(com.jiguro.wonderless.ConfigManager.getBoolean(ctx, key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    com.jiguro.wonderless.ConfigManager.putBoolean(ctx, key, isChecked);
                }
            });
        hor.addView(sw);
        container.addView(hor);

        // 提示文本
        TextView hintTv = new TextView(ctx);
        hintTv.setText(hint);
        hintTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hintTv.setTextColor(0xFF888888);
        hintTv.setPadding(0, dp(ctx, 4), 0, 0);
        container.addView(hintTv);

        parent.addView(container);
    }

    // 美化数值输入
    private void addNumberInput(LinearLayout parent, final Context ctx,
                                String label, String hint,
                                final String prefKey, final int defValue) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(ctx, 8), 0, 0);

        // 水平布局
        LinearLayout hor = new LinearLayout(ctx);
        hor.setOrientation(LinearLayout.HORIZONTAL);
        hor.setGravity(Gravity.CENTER_VERTICAL);
        hor.setPadding(0, dp(ctx, 8), 0, 0);

        // 标签
        TextView labelTv = new TextView(ctx);
        labelTv.setText(label + ":");
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        labelTv.setTextColor(0xFF555555);
        hor.addView(labelTv);

        // 输入框
        final EditText et = new EditText(ctx);
        // 边框样式
        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0xFFFFFFFF);  // 白色背景
        etBg.setCornerRadius(dp(ctx, 8));
        etBg.setStroke(dp(ctx, 1), 0x7F040134);  // 紫色边框
        et.setBackground(etBg);
        // 内边距
        et.setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8));
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        et.setTextColor(0xFF333333);  // 深灰色
        et.setHint("输入数量");
        et.setHintTextColor(0xFF888888);  // 浅灰色
        et.setText(String.valueOf(com.jiguro.wonderless.ConfigManager.getInt(ctx, prefKey, defValue)));
        // 布局参数
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            dp(ctx, 140), 
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(ctx, 8), 0, 0, 0);
        hor.addView(et, lp);
        container.addView(hor);

        // 提示文本
        TextView hintTv = new TextView(ctx);
        hintTv.setText(hint);
        hintTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hintTv.setTextColor(0xFF888888);
        hintTv.setPadding(0, dp(ctx, 4), 0, 0);
        container.addView(hintTv);

        // 文本监听
        et.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int val = Integer.parseInt(s.toString());
                        com.jiguro.wonderless.ConfigManager.putInt(ctx, prefKey, val);
                    } catch (Exception ignored) {}
                }
            });

        parent.addView(container);
    }
    
    // 取dp的小工具
    private int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            dp, 
            ctx.getResources().getDisplayMetrics());
    }

    // 检查窗口是否正在弹出，不检查打开设置会弹两次
    private boolean isDialogShowing() {
        AlertDialog d = activeDialogRef.get();
        return d != null && d.isShowing();
    }

    private void performAutoSign(final Context ctx, final ClassLoader cl) {
        // 每天只签到一次
        if (hasSignedToday(ctx)) {
            XposedBridge.log("今日已签到，跳过");
            return;
        }

        final boolean blockToast = com.jiguro.wonderless.ConfigManager
            .getBoolean(ctx, "enable_sign_toast_block", false);

        new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 获取 token
                        Class<?> y9d0Class = XposedHelpers.findClass("y9.d0", cl);
                        String token = (String) XposedHelpers.callStaticMethod(y9d0Class, "I");
                        if (token == null || token.isEmpty()) {
                            if (!blockToast) {
                                showToastOnMainThread(ctx, "自动签到失败: 无法获取 token");
                            }
                            XposedBridge.log("自动签到失败: 无法获取 token");
                            return;
                        }

                        // 发 GET 请求
                        java.net.URL url = new java.net.URL("http://www.magicalapp.cn/user/api/signDays");
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("token", token);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);

                        int code = conn.getResponseCode();
                        String resp = readStream(conn.getInputStream());

                        XposedBridge.log("签到响应码: " + code + "  响应体: " + resp);

                        if (code == 200) {
                            markSignedToday(ctx);          // 记录今天已签到
                            if (!blockToast) {
                                showToastOnMainThread(ctx, "自动签到成功！");
                            }
                        }

                    } catch (Exception e) {
                        XposedBridge.log("自动签到异常: " + e.getMessage());
                        if (!blockToast) {
                            showToastOnMainThread(ctx, "自动签到失败: 网络异常");
                        }
                    }
                }
            }).start();
    }

    private String readStream(java.io.InputStream is) throws java.io.IOException {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private void showToastOnMainThread(final Context ctx, final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                }
            });
    }

}
