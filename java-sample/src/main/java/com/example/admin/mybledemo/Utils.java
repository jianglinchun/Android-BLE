package com.example.admin.mybledemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.utils.UuidUtils;
import cn.com.superLei.aoparms.annotation.Async;

public class Utils {

    private static Toast mToast;

    public static void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(MyApplication.getInstance(), text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public static void showToast( int paramInt) {
        if (mToast == null) {
            mToast = Toast.makeText(MyApplication.getInstance(), paramInt, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(paramInt);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public static String getUuid(String uuid128){
        if (UuidUtils.isBaseUUID(uuid128)){
            return "UUID: 0x"+UuidUtils.uuid128To16(uuid128, true);
        }
        return uuid128;
    }

    public static int dp2px(float dpValue) {
        final float scale = MyApplication.getInstance().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static void shareAPK(Activity activity){
        PackageInfo packageInfo = getPackageInfo(activity);
        if (packageInfo != null){
            File apkFile = new File(packageInfo.applicationInfo.sourceDir);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider7.getUriForFile(activity, apkFile));
            activity.startActivity(intent);
        }
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            String packageName = getPackageName(context);
            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return packageInfo;
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return null;
    }

    private static String getPackageName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 拷贝OTA升级文件到SD卡
     */
    @Async
    public static void copyOtaFile(final Context context, final String path) {
        //判断是否存在ota文件
        if (SPUtils.get(context, Constant.SP.OTA_FILE_EXIST, false))return;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        File newFile = new File(path + Constant.Constance.OTA_FILE_PATH);
        copyFileToSD(context, Constant.Constance.OTA_FILE_PATH, newFile.getAbsolutePath());
        SPUtils.put(context, Constant.SP.OTA_FILE_EXIST, true);
    }

    private static void copyFileToSD(Context context, String assetPath, String strOutFileName) {
        try {
            InputStream myInput;
            OutputStream myOutput = new FileOutputStream(strOutFileName);
            myInput = context.getAssets().open(assetPath);
            byte[] buffer = new byte[1024];
            int length = myInput.read(buffer);
            while (length > 0) {
                myOutput.write(buffer, 0, length);
                length = myInput.read(buffer);
            }
            myOutput.flush();
            myInput.close();
            myOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class SPUtils {

        /**
         * 保存在手机里面的文件名
         */
        public static final String FILE_NAME = "share_data";

        /**
         * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
         *
         * @param context
         * @param key
         * @param object
         */
        public static void put(Context context, String key, Object object) {
            SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            if (object instanceof String) {
                editor.putString(key, (String) object);
            } else if (object instanceof Integer) {
                editor.putInt(key, (Integer) object);
            } else if (object instanceof Boolean) {
                editor.putBoolean(key, (Boolean) object);
            } else if (object instanceof Float) {
                editor.putFloat(key, (Float) object);
            } else if (object instanceof Long) {
                editor.putLong(key, (Long) object);
            } else {
                editor.putString(key, object == null ? null : String.valueOf(object));
            }
            SharedPreferencesCompat.apply(editor);
        }

        /**
         * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
         *
         * @param context
         * @param key
         * @param defaultObject
         * @return
         */
        public static <T> T get(Context context, String key, T defaultObject) {
            SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            if (defaultObject instanceof String) {
                return (T) sp.getString(key, (String) defaultObject);
            } else if (defaultObject instanceof Integer) {
                return (T) Integer.valueOf(sp.getInt(key, (Integer) defaultObject));
            } else if (defaultObject instanceof Boolean) {
                return (T) Boolean.valueOf(sp.getBoolean(key, (Boolean) defaultObject));
            } else if (defaultObject instanceof Float) {
                return (T) Float.valueOf(sp.getFloat(key, (Float) defaultObject));
            } else if (defaultObject instanceof Long) {
                return (T) Long.valueOf(sp.getLong(key, (Long) defaultObject));
            } else {
                return (T) sp.getString(key, (String) defaultObject);
            }
        }
        /**
         * 创建一个解决SharedPreferencesCompat.apply方法的一个兼容类
         *
         * @author zhy
         */
        private static class SharedPreferencesCompat {
            private static final Method sApplyMethod = findApplyMethod();

            /**
             * 反射查找apply的方法
             *
             * @return
             */
            @SuppressWarnings({"unchecked", "rawtypes"})
            private static Method findApplyMethod() {
                try {
                    Class clz = SharedPreferences.Editor.class;
                    return clz.getMethod("apply");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            }

            /**
             * 如果找到则使用apply执行，否则使用commit
             *
             * @param editor
             */
            public static void apply(SharedPreferences.Editor editor) {
                try {
                    if (sApplyMethod != null) {
                        sApplyMethod.invoke(editor);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                editor.commit();
            }
        }

    }

    public static class GetDeviceId {

        //保存文件的路径
        private static final String CACHE_IMAGE_DIR = "aray/cache/devices";
        //保存的文件 采用隐藏文件的形式进行保存
        private static final String DEVICES_FILE_NAME = ".DEVICES";

        /**
         * 获取设备唯一标识符
         *
         * @param context
         * @return
         */
        public static String getDeviceId(Context context) {
            //读取保存的在sd卡中的唯一标识符
            String deviceId = readDeviceID(context);
            //用于生成最终的唯一标识符
            StringBuffer s = new StringBuffer();
            //判断是否已经生成过,
            if (deviceId != null && !"".equals(deviceId)) {
                return deviceId;
            }
            try {
                //获取IMES(也就是常说的DeviceId)
                deviceId = getIMIEStatus(context);
                s.append(deviceId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                //获取设备的MACAddress地址 去掉中间相隔的冒号
                deviceId = getLocalMac(context).replace(":", "");
                s.append(deviceId);
            } catch (Exception e) {
                e.printStackTrace();
            }
//        }

            //如果以上搜没有获取相应的则自己生成相应的UUID作为相应设备唯一标识符
            if (s == null || s.length() <= 0) {
                UUID uuid = UUID.randomUUID();
                deviceId = uuid.toString().replace("-", "");
                s.append(deviceId);
            }
            //为了统一格式对设备的唯一标识进行md5加密 最终生成32位字符串
            String md5 = getMD5(s.toString(), false);
            if (s.length() > 0) {
                //持久化操作, 进行保存到SD卡中
                saveDeviceID(md5, context);
            }
            return md5;
        }


        /**
         * 读取固定的文件中的内容,这里就是读取sd卡中保存的设备唯一标识符
         *
         * @param context
         * @return
         */
        public static String readDeviceID(Context context) {
            File file = getDevicesDir(context);
            StringBuffer buffer = new StringBuffer();
            try {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                Reader in = new BufferedReader(isr);
                int i;
                while ((i = in.read()) > -1) {
                    buffer.append((char) i);
                }
                in.close();
                return buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 获取设备的DeviceId(IMES) 这里需要相应的权限<br/>
         * 需要 READ_PHONE_STATE 权限
         *
         * @param context
         * @return
         */
        private static String getIMIEStatus(Context context) {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = tm.getDeviceId();
            return deviceId;
        }


        /**
         * 获取设备MAC 地址 由于 6.0 以后 WifiManager 得到的 MacAddress得到都是 相同的没有意义的内容
         * 所以采用以下方法获取Mac地址
         *
         * @param context
         * @return
         */
        private static String getLocalMac(Context context) {
//        WifiManager wifi = (WifiManager) context
//                .getSystemService(Context.WIFI_SERVICE);
//        WifiInfo info = wifi.getConnectionInfo();
//        return info.getMacAddress();


            String macAddress = null;
            StringBuffer buf = new StringBuffer();
            NetworkInterface networkInterface = null;
            try {
                networkInterface = NetworkInterface.getByName("eth1");
                if (networkInterface == null) {
                    networkInterface = NetworkInterface.getByName("wlan0");
                }
                if (networkInterface == null) {
                    return "";
                }
                byte[] addr = networkInterface.getHardwareAddress();


                for (byte b : addr) {
                    buf.append(String.format("%02X:", b));
                }
                if (buf.length() > 0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                macAddress = buf.toString();
            } catch (SocketException e) {
                e.printStackTrace();
                return "";
            }
            return macAddress;


        }

        /**
         * 保存 内容到 SD卡中,  这里保存的就是 设备唯一标识符
         *
         * @param str
         * @param context
         */
        public static void saveDeviceID(String str, Context context) {
            File file = getDevicesDir(context);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                Writer out = new OutputStreamWriter(fos, "UTF-8");
                out.write(str);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 对挺特定的 内容进行 md5 加密
         *
         * @param message   加密明文
         * @param upperCase 加密以后的字符串是是大写还是小写  true 大写  false 小写
         * @return
         */
        public static String getMD5(String message, boolean upperCase) {
            String md5str = "";
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");

                byte[] input = message.getBytes();

                byte[] buff = md.digest(input);

                md5str = bytesToHex(buff, upperCase);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return md5str;
        }


        public static String bytesToHex(byte[] bytes, boolean upperCase) {
            StringBuffer md5str = new StringBuffer();
            int digital;
            for (int i = 0; i < bytes.length; i++) {
                digital = bytes[i];

                if (digital < 0) {
                    digital += 256;
                }
                if (digital < 16) {
                    md5str.append("0");
                }
                md5str.append(Integer.toHexString(digital));
            }
            if (upperCase) {
                return md5str.toString().toUpperCase();
            }
            return md5str.toString().toLowerCase();
        }

        /**
         * 统一处理设备唯一标识 保存的文件的地址
         *
         * @param context
         * @return
         */
        private static File getDevicesDir(Context context) {
            File mCropFile = null;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File cropdir = new File(Environment.getExternalStorageDirectory(), CACHE_IMAGE_DIR);
                if (!cropdir.exists()) {
                    cropdir.mkdirs();
                }
                mCropFile = new File(cropdir, DEVICES_FILE_NAME); // 用当前时间给取得的图片命名
            } else {
                File cropdir = new File(context.getFilesDir(), CACHE_IMAGE_DIR);
                if (!cropdir.exists()) {
                    cropdir.mkdirs();
                }
                mCropFile = new File(cropdir, DEVICES_FILE_NAME);
            }
            return mCropFile;
        }
    }
}
