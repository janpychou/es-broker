/*
 * Copyright (C) 2012 GZ-ISCAS Inc., All Rights Reserved.
 */
package io.sugo.es.broker;

import io.sugo.utils.LogUtil;
import io.sugo.utils.StringUtil;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.Properties;

/**
 * 
 * @Description: 全局配置参数  
 * @Author janpychou@qq.com
 * @CreateDate:   [May 26, 2015 8:04:08 PM]   
 *
 */
public class SystemConfig {

    private static Properties configs = new Properties();

    private static final String UTF_8 = "UTF-8";

    public static final String PREFIX_ARG = "-";

    private static final String SYSTEM_CONFIG_FILE = "config.properties";
    private static final String LOG4J_CONFIG_FILE = "log4j.properties";

    public static final String PROJECT_NAME = "project.name";

    public static final String LISTEN_IP = "listen.ip";
    public static final String LISTEN_PORT = "listen.port";
    public static final String LOCK_FILE = "lockfile";

    public static final String ES_IP = "es.ip";
    public static final String ES_PORT = "es.port";
    public static final String ES_INDEX_NAME = "es.index.name";
    public static final String ES_TYPE_NAME = "es.index.type";
    public static final String ES_ID_COLUMN = "es.id.column";
    public static final String ES_SEARCH_COLUMNS = "es.search.columns";
    public static final String ES_INCLUDE_FIELDS = "es.include.fields";

    public static final java.lang.String MYSQL_URL = "mysql.url";
    public static final java.lang.String MYSQL_USERNAME = "mysql.username";
    public static final java.lang.String MYSQL_PASSWORD = "mysql.password";
    public static final java.lang.String MYSQL_TABLE = "mysql.table";
    public static final java.lang.String MYSQL_COLUMNS = "mysql.columns";
    public static final java.lang.String MYSQL_TAG_COLUMNS = "mysql.tag.columns";


//    public static String lockFile;
    public static String appPath = ".";
//    public static String listenIp = "0.0.0.0";
//    public static int listenPort = 2626;

    static {
        appPath = getAppPath();
        String lockFile = appPath + "file.lock";
        Properties logProperties = readConfigs(LOG4J_CONFIG_FILE);
        PropertyConfigurator.configure(logProperties);
        
        configs.putAll(readConfigs(SYSTEM_CONFIG_FILE));
        checkConfigs();
        if (!configs.containsKey(LOCK_FILE)) {
            configs.setProperty(LOCK_FILE, lockFile);
        }
        if (!configs.containsKey(LISTEN_IP)) {
            configs.setProperty(LISTEN_IP, "0.0.0.0");
        }
        if (!configs.containsKey(LISTEN_PORT)) {
            configs.setProperty(LISTEN_PORT, "2626");
        }
        LogUtil.info("lockfile：" + lockFile);
    }

    private static Properties readConfigs(String fileName) {

        Properties props = new Properties();

        InputStream inputStream = null;
        try {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("load config.properties failed", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return props;
    }

    public static String getAppPath() {
        URL url = SystemConfig.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        try {
            filePath = URLDecoder.decode(url.getPath(), "utf-8");// 转化为utf-8编码  
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (filePath.endsWith(".jar")) {// 可执行jar包运行的结果里包含".jar"  
            // 截取路径中的jar包名  
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        }

        File file = new File(filePath);

        // /If this abstract pathname is already absolute, then the pathname  
        // string is simply returned as if by the getPath method. If this  
        // abstract pathname is the empty abstract pathname then the pathname  
        // string of the current user directory, which is named by the system  
        // property user.dir, is returned.  
        filePath = file.getAbsolutePath();//得到windows下的正确路径  
        return filePath + "/";
    }

    private static void checkConfigs() {
        //检查配置参数是否正确
    }

    public static String getString(String key) {
        return configs.getProperty(key);
    }

    public static String getString(String key, String defaultValue) {
        String value = configs.getProperty(key);
        if (StringUtil.isNullOrEmpty(value)) {
            value = defaultValue;
        }
        return value;
    }

    public static String[] getStrings(String key) {
        String value = getString(key);
        return value.split(",");
    }

    public static boolean getBoolean(String key) {

        String stringValue = getString(key);
        if (stringValue == null) {
            return true;
        }

        stringValue = stringValue.trim();
        if (stringValue.equals("")) {
            return false;
        }

        return Boolean.parseBoolean(stringValue);

    }

    public static long getLong(String key) {
        return Long.parseLong(getString(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public static int getInt(String esPort, int defaultValue) {
        String tmp = getString(esPort);
        if (StringUtil.isNullOrEmpty(tmp)) {
            return defaultValue;
        }
        return Integer.parseInt(tmp);
    }

    /**
     * @since 5.0
     */
    public static Integer getInteger(String key) {
        String value = getString(key);
        return value == null || value.equals("") ? null : Integer.parseInt(value);
    }

    /**
     * @since 5.0
     */
    public static int[] getIntArray(String key) {
        String[] values = getStrings(key);
        return StringUtil.toInts(values);
    }

    public static void tryLock(String filePath) throws Exception {
        File file = new File(filePath);
        file.deleteOnExit();
        @SuppressWarnings("resource")
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel channel = raf.getChannel();
        FileLock lock = channel.tryLock();
        if (lock != null && lock.isValid()) {
        } else {
            throw new RuntimeException("program is already running:" + filePath + " is already locked");
        }
    }

    public static String getLockFile() {
        return getString(LOCK_FILE);
    }

    public static void parseArgs(String[] args) {
        Map<String, String> argmaps = StringUtil.parseArgs(PREFIX_ARG, args);
        if (argmaps.containsKey("bindip")) {
            configs.setProperty(LISTEN_IP, argmaps.get("bindip"));
        }
        if (argmaps.containsKey("port")) {
            configs.setProperty(LISTEN_PORT, argmaps.get("port"));
        }
        LogUtil.info("--------- configs -----------------------");
        LogUtil.info(configs);
    }
}
