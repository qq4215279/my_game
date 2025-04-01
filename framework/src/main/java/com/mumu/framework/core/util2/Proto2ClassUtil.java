/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.util2;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.baidu.bjf.remoting.protobuf.ProtobufIDLProxy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Proto2ClassUtil proto协议生成java代码工具类
 *
 * @author liuzhen
 * @version 1.0.0 2024/8/15 17:11
 */
public class Proto2ClassUtil {
  /** 配置项目所在目录 */
  private static String projectAbPath = FileUtil.getAbsolutePath(new File(""));

  /**
   * 协议生成
   *
   * @param args args
   * @return void
   * @date 2024/8/15 19:45
   */
  public static void main(String[] args) {
    if (args.length > 0) {
      projectAbPath = args[0];
    }
    System.out.println("projectAbPath: " + projectAbPath);
    // 协议所在目录
    String protoAbPath = projectAbPath + "/AutoGen/src/main/resources/proto";
    // 输出目录文件
    File outPutFile = new File(projectAbPath + "/AutoGen/src/main/java");
    ;

    List<File> txtFiles =
        FileUtil.loopFiles(protoAbPath, file -> file.getName().endsWith(".proto"));
    for (File file : txtFiles) {
      String absolutePath = file.getAbsolutePath();
      try {
        doProtoToJava(absolutePath, outPutFile);

        System.out.println(file.getName() + "general java succ");

      } catch (IOException e) {
        throw new RuntimeException("生成java文件异常！protoFile: " + absolutePath);
      }
    }
  }

  /**
   * proto 文件生成 java 文件
   *
   * @param protoPath protoPath
   * @param outPutFile outPutFile
   * @date 2024/8/15 19:27
   */
  private static void doProtoToJava(String protoPath, File outPutFile) throws IOException {
    InputStream resource = ResourceUtil.getResourceObj(protoPath).getStream();

    ProtobufIDLProxy.generateSource(resource, outPutFile);
  }
}
