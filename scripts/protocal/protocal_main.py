# encoding: utf-8
import subprocess

from general_client_proto import general_client_proto
from generate_java_protobuf import general_proto_buff

"""
3. 生成python proto协议
"""
def general_python_proto():
  command = [
    "protoc",
    "--proto_path=../cmd/client",
    "client.proto",
    "--python_out=../test"
  ]
  # protoc --proto_path=../cmd/client client.proto  --python_out=../test
  try:
    result = subprocess.run(command, capture_output=True, text=True, check=True)
  except subprocess.CalledProcessError as e:
    print("命令执行失败:")
    raise RuntimeError("生成python proto失败，错误信息: ", e.stderr)

  # os.system("protoc --proto_path=../cmd/client client.proto  --python_out=../test")



if __name__ == '__main__':
  # 1. 协议生成
  # 指定协议名，则只处理指定协议文件，不指定默认选择所有proto文件
  proto_files = []
  # proto_files = ["server.proto"]
  # proto_files = ["baloot.proto"]
  general_proto_buff(proto_files)
  print("一、协议生成 finish! ===========================================================>\n")

  # 2. 生成客户端协议
  general_client_proto()
  print("二、客户端协议生成 finish! ======================================================>\n")

  # 3. 生成python proto协议
  general_python_proto()
  print("三、python proto协议生成 finish! ================================================>\n")

  # # 4. 生成client.ts 文件 + cmd.json 文件
  # general_client_ts_file()
  # print("四、生成client.ts 文件 + cmd.json 文件 finish! ================================================>\n")

  # # 5. 生成rpcCmd.json 文件
  # general_rpc_cmd_json_file()
  # print("五、生成rpcCmd.json 文件 finish! ================================================>\n")

  # 4. 更新 CmdId 文件 TODO 定义完协议后，手动执行 Cmd.main 生成 ts 文件
  # generate_cmd_id_gradle_command()
  # print("四、更新 CmdId 文件 finish! ================================================>\n")
