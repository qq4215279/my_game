#!/bin/sh

#服务名
servers=("RegisterServer" "AccountServer" "WorldServer" "GameServer" "GateServer")

#使用说明，用来提示输入参数
usage() {
    echo "Usage: sh run.sh [start|stop|restart|status]"
    exit 1
}

#启动方法
start(){
#  for server in "${servers[@]}"; do
#    echo "$server"
#    sh run.sh start $server
#  done
  for i in "${!servers[@]}"; do
    server="${servers[$i]}"
    port=$((i + 8900))
    echo "run: ${server}, debug port: ${port}"
    sh run.sh start $server $port
  done
}

#停止方法
stop(){
  for server in "${servers[@]}"; do
    echo "$server"
    sh run.sh stop $server
  done
}

#输出运行状态
status(){
  for server in "${servers[@]}"; do
    echo "$server"
    sh run.sh status $server
  done
}

#重启
restart(){
  stop
  start
}

# sh batchRun.sh start
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
  "restart")
    restart
    ;;
  *)
    usage
    ;;
esac
exit 0