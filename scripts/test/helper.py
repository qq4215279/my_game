# encoding:utf-8

import configparser
import datetime
import json
import logging
import os
import threading
import time

# 获取 api.ini 配置文件所在位置
API_INI_PATH = f"{os.path.dirname(__file__)}/config/api.ini"
# 获取 players.ini 配置文件所在位置
PLAYERS_INI_PATH = f"{os.path.dirname(__file__)}/config/players.ini"

"""
帮助类
"""
lock = threading.Lock()


"""
返回结果
"""
class ResponseResult:
    def __init__(self, cmd=0, seq=0, errorCodeValue=None, errorCode=None, resMessage=None, protocol_name=""):
        self.cmd = cmd
        self.seq = seq
        self.errorCodeValue = errorCodeValue
        self.errorCode = errorCode
        self.resMessage = resMessage
        self.protocol_name = protocol_name

"""
玩家信息
"""
class Player:
    def __init__(self):
        # 玩家id
        self.playerId = 0
        self.token = ""
        # 玩家金币
        self.gold = 0
        # 玩家钻石
        self.diamond = 0

"""
baloot 房间信息 - 内部类
"""
class BalootGameOb:
    def __init__(self):
        # 是否匹配中
        self.isMatch = False
        # 房间id
        self.roomId = 11
        # 金币下限
        self.goldLowerLimit = 100
        # 金币上限
        self.goldUpperLimit = -1
        # seq
        self.seq = -1
        # seatId
        self.seatId = 0

        # 桌子基本信息（含玩家信息） - GameTableBaseBean
        self.baseInfo = None
        # 当前叫牌玩家列表 - BalootTableBean
        self.balootInfo = None

"""
解析配置文件
"""
def parse_config():
    # 配置文件信息
    config_dict = {}

    # 创建一个 ConfigParser 对象
    config = configparser.ConfigParser()
    # 读取配置文件
    config.read(API_INI_PATH, encoding="utf-8")

    # 遍历所有节
    for section in config.sections():
        # 遍历当前节中的所有配置项
        for option in config.options(section):
            value = config.get(section, option)

            if option.find("is_create_player") != -1 or option.find("is_record") != -1 or option.find("task_player_limit") != -1:
                config_dict[option] = int(value)
            elif option.find("force_modules") != -1:
                force_modules_dict = {}
                arr = value.split(";")
                for i in range(len(arr)):
                    arr2 = arr[i].split(":")
                    force_modules_dict[arr2[0]] = int(arr2[1])
                config_dict[option] = force_modules_dict
            else:
                config_dict[option] = value

    __set_choose_server_config(config_dict)

    return config_dict

# 设置服务器地址信息
def __set_choose_server_config(config_dict):
    choose_server_config_prefix = config_dict["choose_server_config"]
    # xuhai
    if choose_server_config_prefix == 'xuhai':
        config_dict["ws"] = config_dict["xuhai_ws"]
        config_dict["login"] = config_dict["xuhai_login"]
        config_dict["env"] = config_dict["xuhai_env"]
    # liuzhen
    elif choose_server_config_prefix == 'liuzhen':
        config_dict["ws"] = config_dict["liuzhen_ws"]
        config_dict["login"] = config_dict["liuzhen_login"]
        config_dict["env"] = config_dict["liuzhen_env"]
    # dev
    elif choose_server_config_prefix == 'dev':
        config_dict["ws"] = config_dict["dev_ws"]
        config_dict["login"] = config_dict["dev_login"]
        config_dict["env"] = config_dict["dev_env"]

    # test
    elif choose_server_config_prefix == 'test':
        config_dict["ws"] = config_dict["test_ws"]
        config_dict["login"] = config_dict["test_login"]
        config_dict["env"] = config_dict["test_env"]

    # stress
    elif choose_server_config_prefix == 'stress':
        config_dict["ws"] = config_dict["stress_ws"]
        config_dict["login"] = config_dict["stress_login"]
        config_dict["env"] = config_dict["stress_env"]

    # review
    elif choose_server_config_prefix == 'review':
        config_dict["ws"] = config_dict["review_ws"]
        config_dict["login"] = config_dict["review_login"]
        config_dict["env"] = config_dict["review_env"]

    else:
        config_dict["ws"] = config_dict["liuzhen_ws"]
        config_dict["login"] = config_dict["liuzhen_login"]
        config_dict["env"] = config_dict["liuzhen_env"]


"""
读取协议
"""
def read_protocal(client_ts_path):
    if client_ts_path == "":
        client_ts_path = r"./../cmd/client/client.ts"

    with open(client_ts_path, "r", encoding='utf-8') as file:
        name_protocol_id_dict = {}
        protocol_id_name_dict = {}

        for line in  file.readlines():
            if line.find("export const CommandIds = {") != -1 or  line.find("}") != -1:
                continue
            arr = line.split(":")
            proto_name = arr[0].strip()
            proto_id = int(arr[1].split(",")[0].strip())

            name_protocol_id_dict[proto_name] = proto_id
            protocol_id_name_dict[proto_id] = proto_name

    return name_protocol_id_dict, protocol_id_name_dict

# 查找玩家账号
def find_device_id(env: str, is_create_player: bool, prefix_name: str) -> str:
    if len(prefix_name) <= 0:
        prefix_name = 0

    # 前缀
    prefix = env + "_" + prefix_name + "_"
    resDeviceId = ""
    try:
        lock.acquire()

        config_players_dict = __read_players_config(env)

        maxIndex = 0

        for deviceId, value_dict in config_players_dict.items():
            arr = deviceId.split("_")
            curIndex = int(arr[len(arr) - 1])
            maxIndex = max(maxIndex, curIndex)

            # 创角 or 当前账号被使用
            if is_create_player or value_dict["isUsed"] == 1:
                continue
            # 找相同前缀
            elif deviceId.find(prefix) == -1:
                continue
            else:
                value_dict["isUsed"] = 1
                value_str = json.dumps(value_dict)
                __write_players_config(env, deviceId, value_str)

                resDeviceId = deviceId
                break

        # 拼接 deviceId: env_prefix_name_index
        if resDeviceId == "":
            resDeviceId = prefix + str(maxIndex + 1)
            write_dict = {"isUsed": 1, "deviceId": resDeviceId, "playerId": 0, "nick": ""}
            __write_players_config(env, resDeviceId, json.dumps(write_dict))

    except BaseException as e:
        print(e)
    finally:
        lock.release()
        return resDeviceId

# 记录创角账号
def add_player_account(env: str, devieId: str, account_dict: {}):
    try:
        lock.acquire()
        __write_players_config(env, devieId, json.dumps(account_dict))
    except BaseException as e:
        print(e)
    finally:
        lock.release()

# 返还玩家账号
def reback_player_account(env: str, devieId: str):
    try:
        lock.acquire()
        config_players_dict = __read_players_config(env)

        value_dict = config_players_dict[devieId]
        value_dict["isUsed"] = 0

        __write_players_config(env, devieId, json.dumps(value_dict))

    except BaseException as e:
        print(e)
    finally:
        lock.release()


def __read_players_config(env: str):
    # 配置文件信息
    config_players_dict = {}

    # 创建一个 ConfigParser 对象
    config = configparser.ConfigParser()

    # 读取配置文件
    config.read(PLAYERS_INI_PATH, encoding='utf-8')

    # 遍历当前节中的所有配置项
    for option in config.options(env):
        value = config.get(env, option)
        # print(f" {option} = {value}")
        config_players_dict[option] = json.loads(value)

    return config_players_dict

def __write_players_config(env: str, devieId: str, value: str):
    # 创建一个 ConfigParser 对象
    config = configparser.ConfigParser()

    # 读取配置文件
    config.read(PLAYERS_INI_PATH, encoding='utf-8')

    if not config.has_section(env):
        config.add_section(env)
    config.set(env, devieId, value)

    # 将修改后的配置写回文件
    with open(PLAYERS_INI_PATH, 'w', encoding='utf-8') as configfile:
        config.write(configfile)

"""
处理玩家账号脏数据
"""
def handle_dirty_players_config_data():
    # 创建一个 ConfigParser 对象
    config = configparser.ConfigParser()

    # 读取配置文件
    config.read(PLAYERS_INI_PATH, encoding='utf-8')

    # 遍历所有节
    for section in config.sections():
        # 遍历当前节中的所有配置项
        for option in config.options(section):
            value = config.get(section, option)
            # print(f" {option} = {value}")
            value_dict = json.loads(value)
            # 处理脏数据
            if value_dict["isUsed"] == 1:
                value_dict["isUsed"] = 0
                value = json.dumps(value_dict)
                config.set(section, option, value)

    # 将修改后的配置写回文件
    with open(PLAYERS_INI_PATH, 'w', encoding='utf-8') as configfile:
        config.write(configfile)


class InterfaceInfo:
    def __init__(self, name: str):
        # 协议名称
        self.name = name
        # 请求成功次数
        self.success = 0
        # 请求失败次数
        self.fail = 0
        # 最小请求时间
        self.minTime = 0
        # 最大请求时间
        self.maxTime = 0
        # 平均请求时间
        self.avgTime = 0

        self.lock = threading.Lock()

    """
    更新记录
    """
    def update_record(self, is_success: bool, access_time: int) -> None:
        try:
            # self.lock.acquire()
            if is_success:
                self.minTime = min(self.minTime, access_time)
                self.maxTime = max(self.maxTime, access_time)
                self.avgTime = ((self.avgTime * self.success) + access_time) / (self.success + 1)
                self.success += 1
            else:
                self.fail += 1
        except RuntimeError as e:
            print("update_record error...")
        finally:
            pass
            # self.lock.release()

class RecordInfo:
    def __init__(self, startTime: float, endTime: float):
        # 压测开始时间
        self.startTime = startTime
        # 压测结束时间
        self.endTime = endTime
        # 压测模块: 数量
        self.force_modules_record_dict = {}
        # 总请求次数
        self.total = 0
        # 请求失败总次数
        self.fail = 0
        # 失败协议: 总次数
        self.fail_dict = {}
        # 总平均请求时间
        self.avgTime = 0
        # 平均请求时间最长协议
        self.maxAvgTimeName = ""
        # 平均请求最长时间
        self.maxAvgTime = 0
        # 平均请求时间最长协议 的总访问次数
        self.maxAvgAccessTimes = 0
        # 平均访问时间超过100毫秒的协议
        self.maxAvg100Mills = []

    def update_record(self, interface_info_dict: dict):
        # 总成功次数
        success = 0
        # 总请求消耗时间
        total_time = 0

        for key in interface_info_dict:
            obj = interface_info_dict[key]
            if obj is None:
                continue

            success += obj.success
            total_time += obj.success * obj.avgTime

            self.fail += obj.fail

            fail = 0
            if self.fail in self.fail_dict:
                fail = self.fail_dict[self.fail]
            self.fail_dict[self.fail] = fail + obj.fail

            if obj.avgTime > self.maxAvgTime:
                self.maxAvgTimeName = obj.name
                self.maxAvgTime = obj.avgTime
                self.maxAvgAccessTimes = obj.success + obj.fail

            # 超过100毫秒
            if obj.avgTime > 100:
                value = [obj.name, obj.avgTime, obj.success + obj.fail]
                self.maxAvg100Mills.append(value)



        self.total = success + self.fail

        if success != 0:
            self.avgTime = total_time / success

        # 写日志
        self.__write_log()

    # 写日志
    def __write_log(self):
        file_path = __file__
        # 获取当前目录所在绝对了路径
        dir_path = os.path.dirname(file_path)
        # with open(r"./record.log", "a+", encoding='utf-8') as file:
        with open(rf"{dir_path}/record.log", "a+", encoding='utf-8') as file:
            file.writelines("本次压测结果汇总如下: \n")
            t = self.endTime - self.startTime
            start = datetime.datetime.fromtimestamp(self.startTime)
            end = datetime.datetime.fromtimestamp(self.endTime)
            file.writelines(f"1. 压测开始时间: {start}, 压测结束时间: {end}, 总耗时: {t} 秒\n")
            file.writelines(f"2. 压测模块有如下:  \n")
            str = ""
            for protocal_name in self.force_modules_record_dict:
                value = self.force_modules_record_dict[protocal_name]
                str += f"    {protocal_name}, 玩家数: {value}; \n"
            file.writelines(f"{str}\n")
            file.writelines(f"3. 总请求次数: {self.total}, 请求失败总次数: {self.fail} , 失败协议汇总如下: \n")
            str = ""
            for protocal_name in self.fail_dict:
                value = self.fail_dict[protocal_name]
                if not value == 0:
                    str += f"     {protocal_name}, 失败次数: {value}; "
            file.writelines(f"{str}\n")

            avgTime = int(self.avgTime)
            file.writelines(f"4. 总平均请求时间: {avgTime} 毫秒 \n")
            maxAvgTime = int(self.maxAvgTime)
            file.writelines(f"5. 平均请求时间最大协议: {self.maxAvgTimeName} , 平均请求最长时间为: {maxAvgTime} 毫秒 \n")
            file.writelines(f"6. 平均访问时间超过100毫秒的协议如下： \n")
            for i in range(len(self.maxAvg100Mills)):
                value = self.maxAvg100Mills[i - 1]
                file.writelines(f"    协议名称: {value[0]} , 平均访问时间: {int(value[1])} 毫秒, 总访问次数: {value[2]} \n")
            file.writelines("汇总结束 ------------------------------------------------------> \n\n\n")


# ==================================================================================>

"""
获取当前时间的毫秒值
"""
def getMilliseconds():
    return int(time.time() * 1000)


"""
获取日志对象
"""
def get_log():
    # 创建一个文件处理器，指定编码为 UTF-8
    file_handler = logging.FileHandler('test.log', mode='w', encoding='utf-8')
    # 创建一个控制台处理器，用于将日志输出到控制台
    console_handler = logging.StreamHandler()
    # 配置日志记录
    logging.basicConfig(
        level=logging.INFO,  # 设置日志级别
        format='%(asctime)s - %(levelname)s - %(message)s',  # 设置日志格式
        handlers=[file_handler, console_handler],
        # filemode='a'  # 设置文件模式为附加模式（'w'为覆盖模式）
    )
    # 创建一个日志记录器
    return logging.getLogger(__name__)