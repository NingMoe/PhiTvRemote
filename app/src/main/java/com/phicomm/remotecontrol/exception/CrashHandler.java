package com.phicomm.remotecontrol.exception;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;


import com.phicomm.remotecontrol.activities.WelcomeActivity;
import com.phicomm.remotecontrol.base.BaseApplication;
import com.phicomm.remotecontrol.util.DateUtil;
import com.phicomm.remotecontrol.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局捕获异常
 * 当程序发生Uncaught异常的时候,有该类来接管程序,并记录错误日志
 *
 * @author user
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    public static String TAG = "MyCrash";
    // 系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private static CrashHandler mInstance = new CrashHandler();
    private Context mContext;

    // 用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<String, String>();

    /**
     * 保证只有一个CrashHandler实例
     */
    private CrashHandler() {
    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        return mInstance;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
        //autoClear(20);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            //延时操作，有风险，最好handle处理下，等子线程处理完log后，再结束当前进程，或者放在主线程处理
            SystemClock.sleep(300);
            restartApp();
//            restartAppAnotherWay();
           // 退出当前程序，结束当前进程，进程中的所有线程停止运行
            android.os.Process.killProcess(android.os.Process.myPid());
            // 退出当前程序
            //System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息; 否则返回false.
     */
    private boolean handleException(final Throwable ex) {
        if (ex == null)
            return false;

        try {
            // 使用Toast来显示异常信息
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    //关闭所有activity
                    BaseApplication.getApplication().destoryAllActivity();
                    Toast.makeText(mContext,"对不起，程序出现异常！",Toast.LENGTH_SHORT).show();
                    // 收集设备参数信息
                    collectDeviceInfo(mContext);
                   // 保存日志文件
                    try {
                        saveCrashInfoFile(ex);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Looper.loop();
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName + "";
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return 返回文件名称, 便于将文件传送到服务器
     * @throws Exception
     */
    private String saveCrashInfoFile(Throwable ex) throws Exception {
        StringBuffer sb = new StringBuffer();
        try {
            SimpleDateFormat sDateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss");
            String date = sDateFormat.format(new Date());
            sb.append("\r\n" + date + "\n");
            for (Map.Entry<String, String> entry : infos.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                sb.append(key + "=" + value + "\n");
            }

            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            ex.printStackTrace(printWriter);
            Throwable cause = ex.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            printWriter.flush();
            printWriter.close();
            String result = writer.toString();
            sb.append(result);

            String fileName = writeFile(sb.toString());
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
            sb.append("an error occured while writing file...\r\n");
            writeFile(sb.toString());
        }
        return null;
    }

    private String writeFile(String sb) throws Exception {
        String time = DateUtil.getDateTimeFormat(new Date());
        String fileName = "crash-" + time + ".log";
        if (FileUtil.existSdcard()) {
            String path = getGlobalpath();
            File dir = new File(path);
            if (!dir.exists())
                dir.mkdirs();

            FileOutputStream fos = new FileOutputStream(path + fileName, true);
            fos.write(sb.getBytes());
            fos.flush();
            fos.close();
        }
        return fileName;
    }

    public static String getGlobalpath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "crash" + File.separator;
    }

    public static void setTag(String tag) {
        TAG = tag;
    }

    /**
     * 文件删除
     *
     * @param autoClearDay 文件保存天数
     */
    public void autoClear(final int autoClearDay) {
        FileUtil.delete(getGlobalpath(), new FilenameFilter() {
            @Override
            public boolean accept(File file, String filename) {
                String s = FileUtil.getFileNameWithoutExtension(filename);
                int day = autoClearDay < 0 ? autoClearDay : -1 * autoClearDay;
                String date = "crash-" + DateUtil.getOtherDay(day);
                return date.compareTo(s) >= 0;
            }
        });
    }

    private void restartApp() {
        Intent intent = new Intent();
        intent.setClass(mContext, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

    }
//
//    private void restartAppAnotherWay() {
//        Intent intent = new Intent();
//        intent.setClass(mContext, WelcomeActivity.class);
//        PendingIntent restartIntent = PendingIntent.getActivity(mContext, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
//        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
//        mgr.set(AlarmManager.RTC, System.currentTimeMillis(),
//                restartIntent);
////        mgr.set(AlarmManager.RTC, System.currentTimeMillis()+1000,
////                restartIntent);//跳转到页面后，延时1秒打开app
//    }
}