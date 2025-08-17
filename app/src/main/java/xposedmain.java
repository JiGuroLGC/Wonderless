import android.app.*;
import android.content.*;
import android.content.pm.*;
import com.jiguro.wonderless.*;
import dalvik.system.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.*;
import java.io.*;
import java.lang.reflect.*;

//这种方案建议只在开发调试的时候使用

public class xposedmain implements IXposedHookLoadPackage
{
    //按照实际使用情况修改下面几项的值
    private final String jiguroname = "com.jiguro.wonderless";
	private final String LogicalClass = xposed.class.getName();
	private final String LogicalClassinit = "handleLoadPackage";
	public static Context updown;
	
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable
	{
            //将loadPackageParam的classloader替换为宿主程序Application的classloader,解决宿主程序存在多个.dex文件时,有时候ClassNotFound的问题
		XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						 updown=(Context) param.args[0];
						if(BuildConfig.DEBUG){
						loadPackageParam.classLoader = updown.getClassLoader();
						invokeHandleHookMethod(updown, jiguroname, LogicalClass, LogicalClassinit, loadPackageParam);			}else{
							new xposed().handleLoadPackage(loadPackageParam);
						}			}
					});}

    /**
     * 安装app以后，系统会在/data/app/下备份了一份.apk文件，通过动态加载这个apk文件，调用相应的方法
     * 这样就可以实现，只需要第一次重启，以后修改hook代码就不用重启了
     * @param context context参数
     * @param modulePackageName 当前模块的packageName
     * @param handleHookClass   指定由哪一个类处理相关的hook逻辑
     * @param loadPackageParam  传入XC_LoadPackage.LoadPackageParam参数
     * @throws Throwable 抛出各种异常,包括具体hook逻辑的异常,寻找apk文件异常,反射加载Class异常等
     */
    private void invokeHandleHookMethod(Context context, String modulePackageName, String handleHookClass, String handleHookMethod, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable
	{ File apkFile=findApkFile(context, modulePackageName);
        if (apkFile == null)
		{
            throw new RuntimeException("寻找模块apk失败");
        }
        //加载指定的hook逻辑处理类，并调用它的handleHook方法
        PathClassLoader pathClassLoader = new PathClassLoader(apkFile.getAbsolutePath(), ClassLoader.getSystemClassLoader());
        Class<?> cls = Class.forName(handleHookClass, true, pathClassLoader);
        Object instance = cls.newInstance();
        Method method = cls.getDeclaredMethod(handleHookMethod, XC_LoadPackage.LoadPackageParam.class);
        method.invoke(instance, loadPackageParam);
    }

    /**
     * 根据包名构建目标Context,并调用getPackageCodePath()来定位apk
     * @param context context参数
     * @param modulePackageName 当前模块报名
     * @return return apk file
     */
    private File findApkFile(Context context, String modulePackageName)
	{
        if (context == null)
		{
            return null;
        }
        try
		{
            Context moudleContext = context.createPackageContext(modulePackageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            String apkPath=moudleContext.getPackageCodePath();
            return new File(apkPath);
        }
		catch (PackageManager.NameNotFoundException e)
		{
            e.printStackTrace();
        }
        return null;
    }
  

}







