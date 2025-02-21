/*
 * Copyright 2020-2023, 木木996.
 * All Right Reserved.
 */
package com.mumu.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

/**
 * 高频方法集合类
 */
public class ToolUtil {

    /**
     * 获取随机位数的字符串
     *
     * @author gobestsoft
     * @Date 2017/8/24 14:09
     */
    public static String getRandomString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
    
    /**
     * 字符串前补0
     * @param str		字符串
     * @param length	总长度
     * @return
     */
    public static String getStrWithZeroStart(String str,int length) {
    	String zero = "";
    	for (int i = 0; i < length; i++) {
			zero += "0";
		}
    	zero += str;
    	return zero.substring(zero.length()-length, zero.length());
    }
    
    /**
     * 判断一个对象是否是时间类型
     * 
     * @author gobestsoft
     * @Date 2017/4/18 12:55
     */
    public static String dateType(Object o){
        if(o instanceof Date){
            return DateUtil.getDay((Date) o);
        }else{
            return o.toString();
        }
    }

    /**
     * 获取异常的具体信息
     *
     * @author gobestsoft
     * @Date 2017/3/30 9:21
     * @version 2.0
     */
    public static String getExceptionMsg(Exception e) {
        StringWriter sw = new StringWriter();
        try{
            e.printStackTrace(new PrintWriter(sw));
        }finally {
            try {
                sw.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return sw.getBuffer().toString().replaceAll("\\$","T");
    }

    /**
     * 比较两个对象是否相等。<br>
     * 相同的条件有两个，满足其一即可：<br>
     * 1. obj1 == null && obj2 == null; 2. obj1.equals(obj2)
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @return 是否相等
     */
    public static boolean equals(Object obj1, Object obj2) {
        return (obj1 != null) ? (obj1.equals(obj2)) : (obj2 == null);
    }

    /**
     * 计算对象长度，如果是字符串调用其length函数，集合类调用其size函数，数组调用其length属性，其他可遍历对象遍历计算长度
     *
     * @param obj 被计算长度的对象
     * @return 长度
     */
    public static int length(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length();
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).size();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).size();
        }

        int count;
        if (obj instanceof Iterator) {
            Iterator<?> iter = (Iterator<?>) obj;
            count = 0;
            while (iter.hasNext()) {
                count++;
                iter.next();
            }
            return count;
        }
        if (obj instanceof Enumeration) {
            Enumeration<?> enumeration = (Enumeration<?>) obj;
            count = 0;
            while (enumeration.hasMoreElements()) {
                count++;
                enumeration.nextElement();
            }
            return count;
        }
        if (obj.getClass().isArray() == true) {
            return Array.getLength(obj);
        }
        return -1;
    }

    /**
     * 对象中是否包含元素
     *
     * @param obj     对象
     * @param element 元素
     * @return 是否包含
     */
    public static boolean contains(Object obj, Object element) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof String) {
            if (element == null) {
                return false;
            }
            return ((String) obj).contains(element.toString());
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).contains(element);
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).values().contains(element);
        }

        if (obj instanceof Iterator) {
            Iterator<?> iter = (Iterator<?>) obj;
            while (iter.hasNext()) {
                Object o = iter.next();
                if (equals(o, element)) {
                    return true;
                }
            }
            return false;
        }
        if (obj instanceof Enumeration) {
            Enumeration<?> enumeration = (Enumeration<?>) obj;
            while (enumeration.hasMoreElements()) {
                Object o = enumeration.nextElement();
                if (equals(o, element)) {
                    return true;
                }
            }
            return false;
        }
        if (obj.getClass().isArray() == true) {
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                Object o = Array.get(obj, i);
                if (equals(o, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 对象是否不为空(新增)
     *
     * @param o String,List,Map,Object[],int[],long[]
     * @return
     */
    public static boolean isNotEmpty(Object o) {
        return !isEmpty(o);
    }

    /**
     * 对象是否为空
     *
     * @param o String,List,Map,Object[],int[],long[]
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof String) {
            if (o.toString().trim().equals("")) {
                return true;
            }
        } else if (o instanceof List) {
            if (((List) o).size() == 0) {
                return true;
            }
        } else if (o instanceof Map) {
            if (((Map) o).size() == 0) {
                return true;
            }
        } else if (o instanceof Set) {
            if (((Set) o).size() == 0) {
                return true;
            }
        } else if (o instanceof Object[]) {
            if (((Object[]) o).length == 0) {
                return true;
            }
        } else if (o instanceof int[]) {
            if (((int[]) o).length == 0) {
                return true;
            }
        } else if (o instanceof long[]) {
            if (((long[]) o).length == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对象组中是否存在 Empty Object
     *
     * @param os 对象组
     * @return
     */
    public static boolean isOneEmpty(Object... os) {
        for (Object o : os) {
            if (isEmpty(o)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 对象组中是否全是 Empty Object
     *
     * @param os
     * @return
     */
    public static boolean isAllEmpty(Object... os) {
        for (Object o : os) {
            if (!isEmpty(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否为数字
     *
     * @param obj
     * @return
     */
    public static boolean isNum(Object obj) {
        try {
            Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 如果为空, 则调用默认值
     *
     * @param str
     * @return
     */
    public static Object getValue(Object str, Object defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        return str;
    }

    /**
     * 强转->string,并去掉多余空格
     *
     * @param str
     * @return
     */
    public static String toStr(Object str) {
        return toStr(str, "");
    }

    /**
     * 强转->string,并去掉多余空格
     *
     * @param str
     * @param defaultValue
     * @return
     */
    public static String toStr(Object str, String defaultValue) {
        if (null == str) {
            return defaultValue;
        }
        return str.toString().trim();
    }

    /**
     * 强转->int
     *
     * @param obj
     * @return
     */
//	public static int toInt(Object value) {
//		return toInt(value, -1);
//	}

    /**
     * 强转->int
     *
     * @param obj
     * @param defaultValue
     * @return
     */
//	public static int toInt(Object value, int defaultValue) {
//		return Convert.toInt(value, defaultValue);
//	}

    /**
     * 强转->long
     *
     * @param obj
     * @return
     */
//	public static long toLong(Object value) {
//		return toLong(value, -1);
//	}

    /**
     * 强转->long
     *
     * @param obj
     * @param defaultValue
     * @return
     */
//	public static long toLong(Object value, long defaultValue) {
//		return Convert.toLong(value, defaultValue);
//	}
//
//	public static String encodeUrl(String url) {
//		return URLKit.encode(url, CharsetKit.UTF_8);
//	}
//
//	public static String decodeUrl(String url) {
//		return URLKit.decode(url, CharsetKit.UTF_8);
//	}

    /**
     * map的key转为小写
     *
     * @param map
     * @return Map<String,Object>
     */
    public static Map<String, Object> caseInsensitiveMap(Map<String, Object> map) {
        Map<String, Object> tempMap = new HashMap<>();
        for (String key : map.keySet()) {
            tempMap.put(key.toLowerCase(), map.get(key));
        }
        return tempMap;
    }

    /**
     * 获取map中第一个数据值
     *
     * @param <K> Key的类型
     * @param <V> Value的类型
     * @param map 数据源
     * @return 返回的值
     */
    public static <K, V> V getFirstOrNull(Map<K, V> map) {
        V obj = null;
        for (Entry<K, V> entry : map.entrySet()) {
            obj = entry.getValue();
            if (obj != null) {
                break;
            }
        }
        return obj;
    }

    /**
     * 创建StringBuilder对象
     *
     * @return StringBuilder对象
     */
    public static StringBuilder builder(String... strs) {
        final StringBuilder sb = new StringBuilder();
        for (String str : strs) {
            sb.append(str);
        }
        return sb;
    }

    /**
     * 创建StringBuilder对象
     *
     * @return StringBuilder对象
     */
    public static void builder(StringBuilder sb, String... strs) {
        for (String str : strs) {
            sb.append(str);
        }
    }

    /**
     * 去掉指定后缀
     *
     * @param str    字符串
     * @param suffix 后缀
     * @return 切掉后的字符串，若后缀不是 suffix， 返回原字符串
     */
    public static String removeSuffix(String str, String suffix) {
        if (isEmpty(str) || isEmpty(suffix)) {
            return str;
        }

        if (str.endsWith(suffix)) {
            return str.substring(0, str.length() - suffix.length());
        }
        return str;
    }

    /**
     * 当前时间
     *
     * @author gobestsoft
     * @Date 2017/5/7 21:56
     */
    public static String currentTime(){
        return DateUtil.getTime();
    }


    /**
     * 判断是否是windows操作系统
     *
     * @author gobestsoft
     * @Date 2017/5/24 22:34
     */
    public static Boolean isWinOs(){
        String os = System.getProperty("os.name");
        if(os.toLowerCase().startsWith("win")){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 获取临时目录
     *
     * @author gobestsoft
     * @Date 2017/5/24 22:35
     */
    public static String getTempPath(){
        return System.getProperty("java.io.tmpdir");
    }
    
    /**
     * 格式化菜单
     * @param val
     * @param ctx
     * @return 
     */
    public static String parseUrl(String val, String ctx) {
    	
    	if (StringUtils.startsWith(val, "http://") || StringUtils.startsWith(val, "https://")) {
    		return val;
    	}
    	
    	return ctx + val;
    }

    /**
     * web转义字符转换
     */
    public static String parseHtmlStr(String value) {
        String ono = value.replaceAll("& ", "&");
        String result = org.springframework.web.util.HtmlUtils.htmlUnescape(ono);
        return result;
    }

    /**
     * 用于返回逗号拼接的字符串
     * @author swt
     * @param arr
     * @return
     */
    public static String arrayJoin(String arr[] ){
        return Arrays.toString(arr).replace("[","").replace("]","");
    }

    /**
     * 用于格式化map中的值
     * @author swt
     * @param map
     * @param key 需要格式化的值
     * @param newPattern 新的格式
     */
    public static void formartMapDate(Map map,String key,String newPattern){
        Object o = map.get(key);
        if(o!=null){
            String date = DateUtil.parseAndFormat(o.toString(), "yyyyMMdd", newPattern);
            map.put(key,date);
        }

    }

    /**
     * 用于格式化map中的值
     * @author swt
     * @param map
     * @param key 需要格式化的值
     * @param oldPattern 旧的格式
     * @param newPattern 新的格式
     */
    public static void formartMapDate(Map map,String key,String oldPattern,String newPattern){
        Object o = map.get(key);
        if(o!=null){
            String date = DateUtil.parseAndFormat(o.toString(), oldPattern, newPattern);
            map.put(key,date);
        }

    }
    /**
     *转为驼峰
     * @param map
     */
    public static void transferKey(Map map){
        if(map==null||map.size()==0)return;
        Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
        Map temp = new HashMap();
        while (iterator.hasNext()){
            Entry<String, Object> next = iterator.next();
            String key = next.getKey();
            if(key.contains("_")){
                String[] s = key.split("_");
                key = "";
                for(int i=0;i<s.length;i++ ){
                    if(i!=0){
                        s[i] = (s[i].charAt(0)+"").toUpperCase()+(s[i].length()>1?s[i].substring(1,s[i].length()):"");
                    }
                    key+=s[i];
                }
                temp.put(key,next.getValue());
                iterator.remove();
            }
        }
        map.putAll(temp);
    }

    public static void transferKey(List<Map<String,Object>> list){

        if(list==null||list.size()==0)return;
        list.forEach((Map map )->{
            Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
            Map temp = new HashMap();
            while (iterator.hasNext()){
                Entry<String, Object> next = iterator.next();
                String key = next.getKey();
                if(key.contains("_")){
                    String[] s = key.split("_");
                    key = "";
                    for(int i=0;i<s.length;i++ ){
                        if(i!=0){
                            s[i] = (s[i].charAt(0)+"").toUpperCase()+(s[i].length()>1?s[i].substring(1,s[i].length()):"");
                        }
                        key+=s[i];
                    }
                    temp.put(key,next.getValue());
                    iterator.remove();
                }
            }
            map.putAll(temp);

        });

    }


}
