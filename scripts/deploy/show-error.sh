#使用说明，用来提示输入参数
usage() {
    echo "Usage: sh run.sh [world|game|gate|chat|center|account|register]"
    exit 1
}

case "$1" in
  "world")
    cat /data/logs/WorldServer/error/* | python3 exception-unique.py
    ;;
  "game")
    cat /data/logs/GameServer/error/* | python3 exception-unique.py
    ;;
  "gate")
    cat /data/logs/GateServer/error/* | python3 exception-unique.py
    ;;
  "chat")
    cat /data/logs/ChatServer/error/* | python3 exception-unique.py
    ;;
  "center")
    cat /data/logs/CenterServer/error/* | python3 exception-unique.py
    ;;
  "account")
    cat /data/logs/AccountServer/error/* | python3 exception-unique.py
    ;;
  "register")
    cat /data/logs/RegisterServer/error/* | python3 exception-unique.py
    ;;
  *)
    usage
    ;;
esac
exit 0