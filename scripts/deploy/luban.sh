#!/bin/sh
#获取脚本所在目录
SOURCE="$0"
while [ -h "$SOURCE"  ]; do # resolve $SOURCE until the file is no longer a symlink
    DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"
    SOURCE="$(readlink "$SOURCE")"
    [[ $SOURCE != /*  ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"

#服务名
AIP_NAME="luban_manage"
#jar包
JAVA_JAR="$DIR/luban_manage_java-0.0.1-SNAPSHOT.jar"
#项目启动参数
JAVA_OPTION="--spring.config.location=$DIR/application.properties"
#PID  代表是PID文件
PID="$DIR/$AIP_NAME.pid"

#使用说明，用来提示输入参数
usage() {
    echo "Usage: sh 执行脚本.sh [start|stop|restart|status|log]"
    exit 1
}

#检查程序是否在运行
is_exist(){
  pid=`ps -ef|grep "$JAVA_JAR"|grep -v grep|awk '{print $2}' `
  #如果不存在返回1，存在返回0
  if [ -z "${pid}" ]; then
   return 1
  else
    return 0
  fi
}

#启动方法
start(){
  #检查是否已启动
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> ${AIP_NAME} is already running PID=${pid} <<<"
    return
  fi
  #检查创建日志目录
  if [ ! -d $DIR/logs ]; then
      mkdir -p $DIR/logs
  fi
  nohup java -jar ${JAVA_JAR} ${JAVA_OPTION} > $DIR/logs/${AIP_NAME}.log 2>&1 &
  echo $! > $PID
  echo ">>> start $AIP_NAME successed PID=$! <<<"
}

#停止方法
stop(){
  if [ ! -f $PID ]; then
    echo ">>> $PID is not exist <<<"
  else
    pidf=$(cat $PID)
    echo ">>> api PID = $pidf begin kill $pidf <<<"
    kill $pidf
    rm -rf $PID
    sleep 2
  fi
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> api 2 PID = $pid begin kill -9 $pid  <<<"
    kill -9  $pid
    sleep 2
    echo ">>> $AIP_NAME process stopped <<<"
  else
    echo ">>> ${AIP_NAME} is not running <<<"
  fi
}

#输出运行状态
status(){
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> ${AIP_NAME} is running PID is ${pid} <<<"
  else
    echo ">>> ${AIP_NAME} is not running <<<"
  fi
}

log(){
  if [ -f $DIR/logs/$AIP_NAME.log ]; then
    tail -500f $DIR/logs/$AIP_NAME.log
  else
    echo ">>> Can not find File > $DIR/logs/${AIP_NAME}.log <<<"
  fi
}

#重启
restart(){
  stop
  start
}

#根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
  "start")
    start
    ;;
  "stop")
    stop
    ;;
  "status")
    status
    ;;
  "log")
    log
    ;;
  "restart")
    restart
    ;;
  *)
    usage
    ;;
esac
exit 0