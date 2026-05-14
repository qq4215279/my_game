# encoding: utf-8

import json
import random

# 协议生成目录。默认当前目录
CLIENT_OUT_DIRECTORY = "../cmd/client/"
SERVER_OUT_DIRECTORY = "../cmd/server/"

CMD_JSON_PATH = "../../GameCore/src/main/java/com/game/framework/core/cmd/consts/cmd.json"
CMD_JAVA_PATH = "../../GameCore/src/main/java/com/game/framework/core/cmd/consts/Cmd.java"

RPC_CMD_JSON_PATH = "../../GameCore/src/main/java/com/game/framework/core/cmd/consts/rpcCmd.json"
RPC_CMD_JAVA_PATH = "../../GameCore/src/main/java/com/game/framework/core/rpc/enums/RpcCmd.java"


"""
1. 生成client.ts 文件
"""
def general_client_ts_file():
  cmd_id_set = set()

  # 1. 读取cmd.json, 赋值proto_id_dict
  with open(CMD_JSON_PATH, "r", encoding='utf-8') as jsonFile:
    proto_id_dict = json.load(jsonFile)
    # 赋值 cmd_id_set
    for proto_name, cmd_id in proto_id_dict.items():
      cmd_id_set.add(cmd_id)


  # 2. 同步最新协议变更到 proto_id_dict
  __sync_latest_proto_2_dict(CMD_JAVA_PATH, proto_id_dict, cmd_id_set)

  # 3. 将协议文件写入到 client.ts
  with open(CLIENT_OUT_DIRECTORY + "client.ts", 'w', encoding="utf-8") as file:
    file.write("export const CommandIds = {\n")
    for proto_name, cmd_id in proto_id_dict.items():
      file.write("  " + proto_name + "Message: " + str(cmd_id) + ",\n")

    file.write("}")

  # 4. 生成cmd json 文件
  __general_cmd_json_file(CMD_JSON_PATH, proto_id_dict)



"""
2. 生成rpcCmd.json 文件
"""
def general_rpc_cmd_json_file():
  cmd_id_set = set()

  # 1.
  with open(RPC_CMD_JSON_PATH, "r", encoding='utf-8') as jsonFile:
    proto_id_dict = json.load(jsonFile)
    # 赋值 cmd_id_set
    for proto_name, cmd_id in proto_id_dict.items():
      cmd_id_set.add(cmd_id)


  # 2. 同步最新协议变更到 proto_id_dict
  __sync_latest_proto_2_dict(RPC_CMD_JAVA_PATH, proto_id_dict, cmd_id_set)


  # 3. 生成cmd json 文件
  __general_cmd_json_file(RPC_CMD_JSON_PATH, proto_id_dict)


# 同步最新协议变更到 proto_id_dict
def __sync_latest_proto_2_dict(cmd_java_path: str, proto_id_dict: dict, cmd_id_set: set):
  with open(cmd_java_path, 'r', encoding="utf-8") as file:
    for line in file.readlines():
      if line.find("//") != -1:
        continue
      if line.find("/**") != -1 or line.find("*/") != -1:
        continue

      # EIJoinChat(),
      if line.endswith(",\n"):
        proto_name = ""
        if line.find("(") != -1:
          proto_name = line.split("(")[0].strip()
        else:
          proto_name = line.split(",")[0].strip()

        if proto_name not in proto_id_dict:
          cmd_id = __general_cmd_id(cmd_id_set)
          proto_id_dict[proto_name] = cmd_id
          cmd_id_set.add(cmd_id)


# 生成cmd json 文件
def __general_cmd_json_file(json_path: str, proto_id_dict: dict):
  with open(json_path, "w", encoding="utf-8") as file:
    json.dump(proto_id_dict, file, indent=4, ensure_ascii=False)

# 生成不重复协议id
def __general_cmd_id(cmd_id_set: set):
  cmd_id = 0
  while(cmd_id == 0 or cmd_id in cmd_id_set):
    cmd_id = random.randint(100000, 999999)
  return cmd_id



if __name__ == '__main__':
  general_client_ts_file()
  general_rpc_cmd_json_file()