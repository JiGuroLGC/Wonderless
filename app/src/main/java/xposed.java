import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.widget.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.*;
import java.util.*;

public class xposed implements IXposedHookLoadPackage {
    public static Activity updown;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
        // com.magicalstory.AppStore
        if (param.packageName.equals("com.magicalstory.AppStore")) {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam attachParam) throws Throwable {
                        Context context = (Context) attachParam.args[0];
                        ClassLoader classLoader = context.getClassLoader(); // 对付360加固
                        if (updown == null) {
                            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam Parameter114) throws Throwable {
                                        super.afterHookedMethod(Parameter114);
                                        if (updown == null) {
                                            updown = (Activity) Parameter114.thisObject;
                                            jiguroMessage("領域展開，りょういきてんかい !");
                                            XposedBridge.log("获取到奇妙应用上下文");
                                        }
                                    }
                                });
                        }

                        // y9.d0.W()返回true
                        try {
                            XposedHelpers.findAndHookMethod("y9.d0", classLoader, "W", new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        param.setResult(true);
                                    }
                                });
                        } catch (Throwable t) {
                            XposedBridge.log("破解奇妙应用会员失败: " + t);
                        }

                        // com.tencent.mmkv.MMKV.d()返回2099年时间戳
                        try {
                            XposedHelpers.findAndHookMethod("com.tencent.mmkv.MMKV", classLoader, "d", new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        long timestamp2099 = 4070908800L; // 2099-01-01 00:00:00 UTC
                                        param.setResult(timestamp2099);
                                    }
                                });
                        } catch (Throwable t) {
                            XposedBridge.log("破解奇妙应用时间失败: " + t);
                        }

                        // editor.getCoin()返回0
                        try {
                            XposedHelpers.findAndHookMethod("com.magicalstory.AppStore.entity.section.editor",
                                classLoader, "getCoin", new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        param.setResult(0);
                                    }
                                });
                        } catch (Throwable t) {
                            XposedBridge.log("破解奇妙应用硬币失败: " + t);
                        }

                        // item_post.getCoin()返回0
                        try {
                            XposedHelpers.findAndHookMethod("com.magicalstory.AppStore.entity.section.item_post",
                                classLoader, "getCoin", new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        param.setResult(0);
                                    }
                                });
                        } catch (Throwable t) {
                            XposedBridge.log("破解奇妙应用硬币失败: " + t);
                        }

                        // com.magicalstory.AppStore.entity.user.user_from_net.getCoinnum()返回 114514
                        try {
                            XposedHelpers.findAndHookMethod(
                                "com.magicalstory.AppStore.entity.user.user_from_net",
                                classLoader,
                                "getCoinnum",
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        param.setResult(114514);
                                    }
                                });
                        } catch (Throwable t) {
                            XposedBridge.log("破解奇妙应用硬币总数失败: " + t);
                        }
                    }
                });
        } else if (param.packageName.equals("com.magicalstory.cleaner")) {
            if (updown == null) {
                XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            if (updown == null) {
                                updown = (Activity) param.thisObject;
                                jiguroMessage("領域展開，りょういきてんかい !");
                                XposedBridge.log("获取到安卓清理君上下文");
                            }
                        }
                    });
            }

            // MMKV.e()返回特定时间戳
            try {
                XposedHelpers.findAndHookMethod("com.tencent.mmkv.MMKV", param.classLoader, 
                    "e", String.class, long.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            long targetTimestamp = 0x3bb2b0c6018L; // 十六进制时间戳
                            param.setResult(targetTimestamp);
                        }
                    });
            } catch (Throwable t) {
                XposedBridge.log("破解安卓清理君永久会员失败: " + t);
            }

            // gb.p0.d()返回true
            try {
                XposedHelpers.findAndHookMethod("gb.p0", param.classLoader, "d", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(true);
                        }
                    });
            } catch (Throwable t) {
                XposedBridge.log("破解安卓清理君免登录失败: " + t);
            }
        } else if (param.packageName.equals("com.magicalstory.days")) {
            if (updown == null) {
                XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            if (updown == null) {
                                updown = (Activity) param.thisObject;
                                jiguroMessage("領域展開，りょういきてんかい !");
                                XposedBridge.log("获取到朝花夕拾上下文");
                            }
                        }
                    });
            }

            // MMKV.e()返回特定时间戳
            try {
                XposedHelpers.findAndHookMethod("com.tencent.mmkv.MMKV", param.classLoader, 
                    "e", String.class, long.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            long targetTimestamp = 0x5af3107a3fffL; // 十六进制时间戳
                            param.setResult(targetTimestamp);
                        }
                    });
            } catch (Throwable t) {
                XposedBridge.log("朝花夕拾永久会员破解失败: " + t);
            }

            // bb.h.i()返回true
            try {
                XposedHelpers.findAndHookMethod("bb.h", param.classLoader, "i", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(true);
                        }
                    });
            } catch (Throwable t) {
                XposedBridge.log("朝花夕拾免登录破解失败: " + t);
            }
        } else if (param.packageName.equals("com.magicalstory.scanner")) {
            if (updown == null) {
                XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            if (updown == null) {
                                updown = (Activity) param.thisObject;
                                jiguroMessage("領域展開，りょういきてんかい !");
                                XposedBridge.log("获取到奇妙扫描上下文");
                            }
                        }
                    });
            }

            // MMKV.OooO0oO()返回特定时间戳
            try {
                XposedHelpers.findAndHookMethod("com.tencent.mmkv.MMKV", param.classLoader, 
                    "OooO0oO", String.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            long targetTimestamp = 0x5af3107a3fffL; // 十六进制时间戳
                            param.setResult(targetTimestamp);
                        }
                    });
            } catch (Throwable t) {
                XposedBridge.log("奇妙扫描永久会员破解失败: " + t);
            }

            // MMKV.OooO0Oo()返回1
            try {
                XposedHelpers.findAndHookMethod("com.tencent.mmkv.MMKV", param.classLoader, 
                    "OooO0Oo", int.class, String.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(1);
                        }
                    });
            } catch (Throwable t) {
                XposedBridge.log("奇妙扫描免登录破解失败: " + t);
            }
        } else if (param.packageName.equals("com.magicalstory.toolbox")) {
            if (updown == null) {
                XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            if (updown == null) {
                                updown = (Activity) param.thisObject;
                                jiguroMessage("領域展開，りょういきてんかい !");
                                XposedBridge.log("获取到奇妙工具箱上下文");
                            }
                        }
                    });
            }

            // uf.l.s()返回特定时间戳
            try {
                XposedHelpers.findAndHookMethod("uf.l", param.classLoader, 
                    "s", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            long targetTimestamp = 0x9184e729fffL; // 十六进制时间戳
                            param.setResult(targetTimestamp);
                        }
                    });
            } catch (Throwable t) {
                XposedBridge.log("奇妙工具箱永久会员破解失败: " + t);
            }

            // uf.l.t()返回true
            try {
                XposedHelpers.findAndHookMethod("uf.l", param.classLoader, 
                    "t", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(true);
                        }
                    });
            } catch (Throwable t) {
                XposedBridge.log("奇妙工具箱免登录破解失败: " + t);
            }
        }
    }

    private void jiguroMessage(String information) {
        try {
            Toast.makeText(updown, information, 1000).show();
        } catch (Exception e) {
            XposedBridge.log("提示异常:" + e);
        }
        XposedBridge.log("提示内容：" + information);
    }
}
