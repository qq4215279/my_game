/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.util2;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jctools.maps.NonBlockingHashSet;

import com.mumu.framework.core.log.LogTopic;

/** class 扫描 @Date: 2024/7/22 上午11:33 @Author: xu.hai */
public class ClassScanner {

  /** 需要扫描的包名 */
  final String packagePath;

  /** 存放扫描过的 clazz */
  final Set<Class<?>> clazzSet = new NonBlockingHashSet<>();

  /** true 保留符合条件的class */
  final Predicate<Class<?>> predicateFilter;

  ClassLoader classLoader;

  /**
   * 扫描
   *
   * @param packagePath 扫描路径
   * @param predicateFilter 过滤条件
   */
  public ClassScanner(String packagePath, Predicate<Class<?>> predicateFilter) {
    this.predicateFilter = predicateFilter;

    var path = packagePath.replace('.', '/');
    path = path.endsWith("/") ? path : path + '/';

    this.packagePath = path;
  }

  public List<Class<?>> listScan() {
    try {
      this.initClassLoad();

      Enumeration<URL> urlEnumeration = classLoader.getResources(packagePath);

      while (urlEnumeration.hasMoreElements()) {
        URL url = urlEnumeration.nextElement();
        String protocol = url.getProtocol();

        if ("jar".equals(protocol)) {
          scanJar(url);
        } else if ("file".equals(protocol)) {
          scanFile(url);
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new ArrayList<>(clazzSet);
  }

  private void initClassLoad() {
    if (Objects.nonNull(this.classLoader)) {
      return;
    }

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    this.classLoader = classLoader != null ? classLoader : ClassScanner.class.getClassLoader();
  }

  public List<URL> listResource() throws IOException {
    this.initClassLoad();

    Enumeration<URL> urlEnumeration = classLoader.getResources(packagePath);
    Set<URL> set = new HashSet<>();
    while (urlEnumeration.hasMoreElements()) {
      URL url = urlEnumeration.nextElement();
      set.add(url);
    }

    return new ArrayList<>(set);
  }

  private void scanJar(URL url) throws IOException {
    URLConnection urlConn = url.openConnection();

    if (urlConn instanceof JarURLConnection jarUrlConn) {
      JarFile jarFile = jarUrlConn.getJarFile();

      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        // jarEntryName
        String jarEntryName = entry.getName();

        if (jarEntryName.charAt(0) == '/') {
          jarEntryName = jarEntryName.substring(1);
        }

        if (entry.isDirectory() || !jarEntryName.startsWith(packagePath)) {
          continue;
        }

        // 扫描 packagePath 下的类
        if (jarEntryName.endsWith(".class") && jarEntryName.startsWith(packagePath)) {
          jarEntryName = jarEntryName.substring(0, jarEntryName.length() - 6).replace('/', '.');
          loadClass(jarEntryName);
        }
      }
    }
  }

  private void scanFile(URL url) {
    String name = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
    File file = new File(name);

    String classPath = getClassPath(file);
    scanFile(file, classPath);
  }

  private void scanFile(File file, String classPath) {
    if (file.isDirectory()) {

      File[] files = file.listFiles();

      if (Objects.isNull(files)) {
        return;
      }

      for (File value : files) {
        scanFile(value, classPath);
      }

    } else if (file.isFile()) {

      String absolutePath = file.getAbsolutePath();

      if (absolutePath.endsWith(".class")) {

        String className =
            absolutePath
                .substring(classPath.length(), absolutePath.length() - 6)
                .replace(File.separatorChar, '.');

        loadClass(className);
      }
    }
  }

  private String getClassPath(File file) {
    String absolutePath = file.getAbsolutePath();

    if (!absolutePath.endsWith(File.separator)) {
      absolutePath = absolutePath + File.separator;
    }

    String ret = packagePath.replace('/', File.separatorChar);

    int index = absolutePath.lastIndexOf(ret);

    if (index != -1) {
      absolutePath = absolutePath.substring(0, index);
    }

    return absolutePath;
  }

  private void loadClass(String className) {
    Class<?> clazz = null;

    try {
      clazz = classLoader.loadClass(className);
    } catch (Throwable e) {
      LogTopic.ACTION.error(e.getMessage(), e);
    }

    if (clazz != null && !clazzSet.contains(clazz)) {
      if (predicateFilter.test(clazz)) {
        clazzSet.add(clazz);
      }
    }
  }
}
