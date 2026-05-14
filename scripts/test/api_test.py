# encoding: utf-8

import asyncio
import threading
from time import sleep

import requests
import websockets
from google.protobuf import json_format

import helper
# protobuf 协议文件！注：一定得保留如下 import ，否则创建协议对象会报错！！！ => class_obj = globals()[protocol_name]
from client_pb2 import *
from helper import Player, ResponseResult, get_log

# 日志对象
log = get_log()


# 配置文件
config_dict = helper.parse_config()
# 协议
protocal = helper.read_protocal(config_dict["client_ts_path"])
# 协议名 与 协议id 字典
name_protocol_id_dict = protocal[0]
#  协议id  与协议名 字典
protocol_id_name_dict = protocal[1]

# 不需要打印的协议
FILTER_PRINT_MESSAGE_SET = {}
# FILTER_PRINT_MESSAGE_SET = {"OnPushFunctionStateListMessage"}

"""
任务
"""
class Task():
  def __init__(self, force_test=False, playerCount=1, receiveCallback=None, initCallback=None, closeCallback=None):
    # 主线程name
    self.t_name = threading.currentThread().getName()
    self.ws = config_dict["ws"]
    self.env = config_dict["env"]
    self.prefix_name = config_dict["prefix_name"]
    # 是否创角
    self.is_create_player = config_dict["is_create_player"]
    # 是否是压测环境
    self.force_test = force_test
    self.playerCount = playerCount
    self.receiveCallback = receiveCallback
    self.initCallback = initCallback
    # TODO 调用
    self.closeCallback = closeCallback
    # 有新请求
    self.has_new_command = False

    # 是否关闭
    self.close = False
    # 状态 0: 默认; 1: 可close; 2: 正在close; 3: close 完成
    self.taskClose = False
    # loop
    self.loop = asyncio.new_event_loop()

    # 客户端(deviceId:Client)
    self.client_dict = {}
    # 初始化客户端
    self.__init_clients()

    # 接受者
    self.worker = Worker(self, receiveCallback)
    self.worker.start()


  # 初始化客户端
  def __init_clients(self):
    self.loop.run_until_complete(self.__init_clients_async())
    self.has_new_command = True

    if self.initCallback is not None:
      self.initCallback()

  async def __init_clients_async(self):
    tasks = []
    for i in range(self.playerCount):
      try:
        # 将任务添加到列表
        tasks.append(self.__init_client_async())
      except RuntimeError as e:
        # print("new Client fail i: ", i, e)
        log.error(f"new Client fail i: {i}, {e}")

    # 设置return_exceptions=True来实现。这将使得 asyncio.gather 在遇到异常时不会取消其他任务
    await asyncio.gather(*tasks, return_exceptions=True)

  # do初始化客户端
  async def __init_client_async(self):
    deviceId = None
    try:
      deviceId = helper.find_device_id(self.env, self.is_create_player, self.prefix_name)
      # print("find deviceId: ", deviceId)

      client = Client(self, self.ws, self.env, deviceId)
      # print("\nclient: ", client)
      # 建立连接
      self.client_dict[deviceId] = client

      await client.connect()
    except RuntimeError as e:
      # print("new Client fail deviceId: ", deviceId, e)
      log.error(f"new Client fail deviceId: {deviceId} {e}")
    finally:
      pass
      # print("start release")



  """
  添加指令
  """
  def add_command(self, protocol_name: str, params: dict, callback=None):
    has_new_command = False
    try:
      # 发送请求
      for client in self.client_dict.values():
        client.add_command(protocol_name, params, callback)

    except RuntimeError as e:
      # print(e)
      log.error("add_command error", e)
    finally:
      if has_new_command:
        self.has_new_command = True


  """
  购买商品
  """
  def buy_goods(self, goodsId: int):
    # 创建订单回调
    def create_order_callback(client: Client, responseResult: ResponseResult):
      self.add_command("CWChargeByFakeMessage", {"orderId": responseResult.resMessage.orderId})

    self.add_command("CWCreateOrderMessage", {"goodsId": goodsId, "payType":"", "extraInfo":""}, create_order_callback)

  """
  绑定账户
  """
  def bind_account(self):
    has_new_command = False
    try:
      # 发送请求
      for client in self.client_dict.values():
        client.bind_account()

    except RuntimeError as e:
      # print(e)
      log.error("add_command error", e)
    finally:
      if has_new_command:
        self.has_new_command = True

"""
接收者
"""
class Worker(threading.Thread):
  def __init__(self, task: Task, receiveCallback=None):
    threading.Thread.__init__(self)
    self.task = task
    self.receiveCallback = receiveCallback


  def run(self):
    no_command_ticket = 0
    ticket = 0

    while not self.task.close:
      # 开始标记关闭
      if self.task.close or (no_command_ticket >= 3 and not self.task.force_test):
        self.task.close = True
        log.info(f"{self.task.t_name} - task 开始标记关闭")
        break


      # 没有新请求，休眠1s
      if not self.task.has_new_command:
        no_command_ticket = no_command_ticket + 1
        # print(f"{self.task.t_name} - task 没有新请求，休眠1s")
        log.info(f"{self.task.t_name} - task 没有新请求，休眠1s")
        sleep(1)
        continue

      self.task.has_new_command = False
      no_command_ticket = 0


      try:
        log.info(f"{self.task.t_name} - do_test => __send_message_async start")
        self.task.loop.run_until_complete(self.__send_message_async())
        log.info(f"{self.task.t_name} - do_test => __send_message_async end")

        log.info(f"{self.task.t_name} - do_test => __receive_messages_async start")
        self.task.loop.run_until_complete(self.__receive_messages_async())
        log.info(f"{self.task.t_name} - do_test => __receive_messages_async end")
      except RuntimeError as e:
        # print(f"{self.task.t_name} - Receiver run exception, e: ", e)
        log.error(f"{self.task.t_name} - Receiver run exception, e: {e}")
      finally:
        pass
        # print(f"{self.task.t_name} - ticket: {ticket}")
        log.info(f"{self.task.t_name} - ticket: {ticket}")
        ticket = ticket + 1
        # 休眠1s
        # sleep(random.uniform(1.5,3))

    else:
      # close标志
      self.__task_close()
      log.info(f"{self.task.t_name} - Receiver close")
      # print(f"{self.task.t_name} - Receiver close")


  # 异步发送请求消息
  async def __send_message_async(self):
    tasks = []
    log.info(f"{self.task.t_name} - do_test => send_message_async")
    for client in self.task.client_dict.values():
      try:
        # 将任务添加到列表
        tasks.append(client.send_messages())
      except RuntimeError as e:
        # print(f"{self.task.t_name} - send_message_async fail", e)
        log.error(f"{self.task.t_name} - send_message_async fail e: {e}")

    # 等待所有任务完成
    await asyncio.gather(*tasks)

  # 异步接收消息
  async def __receive_messages_async(self):
    tasks = []
    for client in self.task.client_dict.values():
      try:
        # 将任务添加到列表
          tasks.append(client.receive_messages(self.receiveCallback))
      except RuntimeError as e:
        # print(f"{self.task.t_name} - __receive_messages_async fail", e)
        log.error(f"{self.task.t_name} - __receive_messages_async fail e: {e}")

    # 等待所有任务完成
    await asyncio.gather(*tasks)


  """
  关闭连接
  """
  def __task_close(self):
    if self.task.taskClose:
      # print("has task_close, return!")
      return

    # print(f"{self.task.t_name} - start task_close")
    log.info(f"{self.task.t_name} - start task_close")
    self.task.taskClose = True
    try:
      # 关闭客户端连接
      self.task.loop.run_until_complete(self.__close_client_async())

    except RuntimeError as e:
      # print(f"Task close client error! {e}")
      log.error(f"Task close client error! {e}")
    finally:
      # print(f"{self.task.t_name} - close all clients success~")
      log.info(f"{self.task.t_name} - close all clients success~")

  # 异步关闭所有客户端
  async def __close_client_async(self):
    tasks = []
    for client in self.task.client_dict.values():
      try:
        # 将任务添加到列表
        tasks.append(client.client_close())
      except RuntimeError as e:
        # print(f"{self.task.t_name} - __close_client_async fail", e)
        log.error(f"{self.task.t_name} - __close_client_async fail e: {e}")

    # 等待所有任务完成
    await asyncio.gather(*tasks)

"""
客户端
"""
class Client:
  def __init__(self, task: Task, ws=None, env=None, deviceId=None):
    self.task = task
    self.ws = ws
    self.env = env
    # 登录deviceId
    self.deviceId = deviceId
    # 玩家信息
    self.player = Player()
    # 状态: 0: 等待连接; 1: 已连接; 2: 已登录; 3: 已关闭
    self.state = 0

    self.websocket = None
    self.seq = 0
    # 锁
    self.lock = threading.Lock()
    # 命令
    self.commands = []
    # seq 与 callback回调
    self.seq_callback_dict = {}
    # 正在接收
    self.receiveing = False

  """
  建立连接
  """
  async def connect(self, connect_again=False):
    start = helper.getMilliseconds()
    """连接到 WebSocket 服务器"""
    self.websocket = await websockets.connect(self.ws, open_timeout=60, max_size=2**20)
    end = helper.getMilliseconds()


    # 重连
    if connect_again:
      # 标记已登录
      # print(f"{self.task.t_name} - {self.deviceId} 重连到 WebSocket 服务器#连接耗时: ", (end - start))
      log.info(f"{self.task.t_name} - {self.deviceId} 重连到 WebSocket 服务器#连接耗时: {end - start}")
      await self.__reconect()

      # 初始化
    else:
      self.state = 1
      # print(f"{self.task.t_name} - {self.deviceId} 已连接到 WebSocket 服务器#连接耗时: ", (end - start))
      log.info(f"{self.task.t_name} - {self.deviceId} 已连接到 WebSocket 服务器#连接耗时: {end - start}")
      await self.__init()


  # 初始化
  async def __init(self):
    # 1. 账号服进行游客登录
    token = self.__account_login()
    self.player.token = token
    # print(f"{self.task.t_name} - deviceId: {self.deviceId}, token: {token}")
    log.info(f"{self.task.t_name} - deviceId: {self.deviceId}, token: {token}")

    # 2. 游戏登录
    # 2.1. 定义callback
    def login_callback(client: Client, responseResult: ResponseResult):
      # print(f"{self.task.t_name} - 登录成功~ deviceId: ", client.deviceId)
      log.info(f"{self.task.t_name} - 登录成功~ deviceId: {client.deviceId}")
      clientPlayerInfo = responseResult.resMessage.clientPlayerInfo
      # print("login_callback~")
      write_dict = {"isUsed": 1, "deviceId": client.deviceId, "playerId": clientPlayerInfo.id, "nick": clientPlayerInfo.nick}
      # 标记已登录
      client.state = 2
      client.player.playerId = clientPlayerInfo.id
      client.player.gold = clientPlayerInfo.gold
      client.player.diamond = clientPlayerInfo.diamond
      helper.add_player_account(client.env, client.deviceId, write_dict)

    # 2.2. 游戏登录
    await self.__send_msg_and_receive("ReqLoginMessage", {"token": token, "languageCode": "zhcn", "channel": "dev"}, login_callback)

  async def __reconect(self):
    # 1. 账号服进行游客登录
    token = self.player.token
    # print(f"{self.task.t_name} - deviceId: {self.deviceId}, token: {token}")
    log.info(f"{self.task.t_name} - deviceId: {self.deviceId}, token: {token}")


    # 2.2. 游戏登录
    await self.__send_msg_and_receive("ReqLoginMessage", {"token": token, "languageCode": "zhcn", "channel": "dev"})
    log.info(f"{self.task.t_name} - {self.deviceId} - __reconect ReqLoginMessage")


  # account服登录
  def __account_login(self):
    data = {'loginType': "GUEST", 'deviceId': self.deviceId, 'channel': 'dev'}
    response = requests.post(config_dict["login"] + "/account/login", json=data)
    res = response.json()
    if res["code"] != 0:
      raise RuntimeError("登录失败.... response: ", response)

    return res["data"]["token"]

  # 是否连接成功
  def is_connect(self):
    if self.websocket is None:
      # print(f"{self.task.t_name} - {self.deviceId} 未连接上服务器！已断开连接...")
      log.error(f"{self.task.t_name} - {self.deviceId} 未连接上服务器！已断开连接...")
      raise RuntimeError("未连接上服务器！")


  """
  绑定玩家账号
  """
  def bind_account(self):
    data = {'token': self.player.token, 'facebookId': self.deviceId, 'nickName': self.player.playerId, 'head': 'fbHead'}
    response = requests.post(config_dict["login"] + "/account/bindFacebook", json=data)
    res = response.json()
    if res["code"] == 0:
      facebook_token = res["data"]["token"]
      self.add_command("CWBindAccountMessage", {"token": facebook_token})


  """
   添加命令
  """
  def add_command(self, protocol_name: str, params: dict, callback=None):
    try:
      self.lock.acquire()
      self.commands.append((protocol_name, params, callback))
    except RuntimeError as e:
      # print(e)
      log.error("add_command error", e)
    finally:
      self.lock.release()

  """
  添加命令，并加入到消息任务
  """
  def add_callback_command(self, protocol_name, params, callback=None):
    self.add_command(protocol_name, params, callback)
    self.task.has_new_command = True

  """
  发送消息 并接收
  """
  async def __send_msg_and_receive(self, protocol_name, params, callback=None) -> []:
    # 发送请求到 WebSocket 服务器
    self.is_connect()

    # 1. 加入到队列中
    self.add_command(protocol_name, params, callback)
    # 2. 发送
    await self.send_messages()
    # 3. 接收
    return await self.receive_messages()


  """ 
  持续向 WebSocket 服务器发送消息
  """
  async def send_messages(self):
    # 等待连接
    while self.state == 0:
      await asyncio.sleep(1)
      # print("send_messages - 等待连接...")


    all_commands = []
    try:
      self.lock.acquire()
      all_commands = self.commands[:]
      self.commands.clear()
    except RuntimeError as e:
      # print(f"{self.task.t_name} - send_messages fail e: {e}")
      log.error(f"{self.task.t_name} - send_messages fail e: {e}")
    finally:
      self.lock.release()

    for command in all_commands:
      protocol_name = command[0]
      params = command[1]
      callback = command[2]

      # 发送请求
      await self.__send_message(protocol_name, params, callback)
    else:
      pass


  """
  发送消息
  """
  async def __send_message(self, protocol_name, params, callback=None):
    # 发送请求到 WebSocket 服务器
    self.is_connect()

    # 发送
    encode_send_data = self.__encode_send_param(protocol_name, params, callback)
    try:
      await self.websocket.send(encode_send_data)
    except RuntimeError as e:
      log.error(f"{self.task.t_name} __send_message error, {e}")

  """
  持续接收来自 WebSocket 服务器的消息
  """
  async def receive_messages(self, receiveCallback=None) -> list:
    self.is_connect()

    self.receiveing = True
    response_result_array = []
    while True:
      try:
        # 设置接收消息的超时时间为 3 秒
        response = await asyncio.wait_for(self.websocket.recv(), timeout=1)
        responseResult = self.__decode_receive_msg(response)

        response_result_array.append(responseResult)

        # 1. 回调监听
        if not receiveCallback is None :
          receiveCallback(self, responseResult)

        # 2. callback回调
        seq = responseResult.seq
        if seq in self.seq_callback_dict:
          callback = self.seq_callback_dict[seq]
          # do回调
          callback(self, responseResult)
          del(self.seq_callback_dict[seq])

      # 结束本次接收消息
      except asyncio.TimeoutError:
        # # print("接收消息超时！")
        break
      except ConnectionResetError as e:
        # print(f"{self.task.t_name} - {self.deviceId} - receive_messages - Connection reset error: {e}")
        log.error(f"{self.task.t_name} - {self.deviceId} - receive_messages - Connection reset error: {e}")
        break
      except websockets.ConnectionClosed as e:
        try:
          # print(f"{self.task.t_name} - {self.deviceId} - 连接已关闭,再次建立连接")
          log.info(f"{self.task.t_name} - {self.deviceId} - 连接已关闭,再次建立连接")
          await self.connect(True)
        except RuntimeError as e:
          # print(f"{self.task.t_name} - {self.deviceId} - 再次建立连接失败 e: {e}")
          log.info(f"{self.task.t_name} - {self.deviceId} - 再次建立连接失败 e: {e}")
          break
        finally:
          pass

      except Exception as e:
        # print(f"{self.task.t_name} - 其他接收异常: {e}")
        log.error(f"{self.task.t_name} - 其他接收异常: {e}")
        break

    self.receiveing = False
    return response_result_array

  """
  断开连接
  """
  async def client_close(self):
    try:
      if self.state == 1 or self.state == 2:
        # 停止接收消息
        self.state = 3

        # 关闭 WebSocket 连接
        await self.websocket.close()
      # print("已关闭 WebSocket 连接")

    except RuntimeError as e:
      # print(f"close client error! e: {e}")
      log.error(f"close client error! e: {e}")
    finally:
      helper.reback_player_account(self.env, self.deviceId)
      # print(f"{self.task.t_name} - {self.deviceId} - client end close~")
      # log.info(f"{self.task.t_name} - {self.deviceId} - client end close~")

  # 获取发送字节数组
  def __encode_send_param(self, protocol_name="", paramsJ={}, callback=None):
    messageOb = self.__create_ob(protocol_name, paramsJ)
    # 使用 getattr() 调用指定名称的方法
    messageSerializedData = None
    if messageOb is not None:
      messageObSerializeApi = getattr(messageOb, "SerializeToString")
      messageSerializedData = messageObSerializeApi()
    # print("messageSerializedData: ", messageSerializedData)

    # seq++
    self.__increment_seq()
    # self.seq = self.seq + 1

    # 记录callback信息
    if callback is not None:
      self.seq_callback_dict[self.seq] = callback

    cmd = int(name_protocol_id_dict[protocol_name])
    messageProxyDict = {"cmd": cmd, "seq": self.seq, "data": messageSerializedData}
    messageProxyOb = self.__create_ob("MessageProxy", messageProxyDict)
    # 使用 getattr() 调用指定名称的方法
    messageProxySerializeApi = getattr(messageProxyOb, "SerializeToString")
    messageProxySerializeData = messageProxySerializeApi()

    # print("messageProxySerializeData: ", messageProxySerializeData)
    # log.info(f"{self.task.t_name} - deviceId: {self.deviceId} seq: {self.seq}, cmd: {cmd} protocol_name: {protocol_name}")

    return messageProxySerializeData

  """
  自增seq
  """
  def __increment_seq(self):
    try:
      self.lock.acquire()
      self.seq = self.seq + 1
    except RuntimeError as e:
      # print(e)
      log.error("__increment_seq error", e)
    finally:
      self.lock.release()

  # 解码返回消息
  def __decode_receive_msg(self, serialized_data) -> ResponseResult:
    # 反序列化
    messageProxy = self.__create_ob("MessageProxy", {})
    messageProxy.ParseFromString(serialized_data)

    cmd = messageProxy.cmd
    seq = messageProxy.seq
    errorCodeValue = messageProxy.errorCode
    errorCode = ErrorCode.Name(errorCodeValue)

    data = messageProxy.data
    resMessage_json = ""
    if messageProxy.cmd is None or messageProxy.cmd == 0 or data is None:
      # print(f"{self.task.t_name} - deviceId: {self.deviceId} seq: {seq}, cmd: {cmd} errorCode: {errorCode}")
      log.info(f"{self.task.t_name} - deviceId: {self.deviceId} seq: {seq}, cmd: {cmd} errorCode: {errorCode}")
      return ResponseResult(cmd, seq, errorCodeValue, errorCode, None, "")
    else:
      protocol_name = protocol_id_name_dict[messageProxy.cmd]
      resMessage = self.__create_ob(protocol_name, {})
      if resMessage is not None:
        resMessage.ParseFromString(data)
        # 将 protobuf 对象转换为 JSON 字符串
        resMessage_json = json_format.MessageToJson(resMessage).replace("\n", "").replace(" ", "")
        # resMessage_json = json.loads(resMessage_json)


    # 过滤打印
    if protocol_name not in FILTER_PRINT_MESSAGE_SET:
      print(f"{self.task.t_name} - deviceId: ", self.deviceId, "seq: ", seq, " cmd: ", cmd, " errorCode: ", errorCode, " protocolName: ", protocol_name, " resMessage: ", resMessage_json)
      log.info(f"{self.task.t_name} - deviceId: {self.deviceId} seq: {seq}, cmd: {cmd} errorCode: {errorCode} protocolName: {protocol_name}")

    return ResponseResult(cmd, seq, errorCode, errorCode, resMessage, protocol_name)

  # 创建对象
  def __create_ob(self, protocol_name: str, attbute_value_dict):
    try:
      class_obj = globals()[protocol_name]
    except RuntimeError as e:
      # print("打印异常：", e)
      return None
    except KeyError as e:
      return None

    class_obj = class_obj()
    descriptor = class_obj.DESCRIPTOR

    for field_name, value in attbute_value_dict.items():
      if value is None:
        continue

      field_descriptor = descriptor.fields_by_name.get(field_name)
      # 枚举赋值
      if field_descriptor and field_descriptor.type == 14:
        # print("枚举类型: ", field_name)
        # 动态获取枚举类型
        enum_type = field_descriptor.enum_type
        # 获取对应的枚举值
        enum_value = enum_type.values_by_name.get(value)
        if enum_value is not None:
          # 设置枚举字段
          setattr(class_obj, field_name, enum_value.number)
        else:
          raise ValueError(f"Invalid enum value: {value} for field: {field_name}")

      # 列表对象
      elif isinstance(value, list):
        key_list = getattr(class_obj, field_name)
        key_list.extend(value)

      # 普通字段
      else:
        setattr(class_obj, field_name, value)

    return class_obj



if __name__ == '__main__':
  print("\nDESCRIPTOR: ", DESCRIPTOR, " 注：本行打印调用 , 避免idea自动导包将 (from client_pb2 import *) 移除!!!")
  helper.handle_dirty_players_config_data()
  task = Task()

  # 绑定账户
  # task.bind_account()
  # 请求玩家个人信息
  # task.add_command("CWGetPlayerInfoMessage", {})
  # 增加道具
  # task.add_command("CWExecuteGmCmdMessage", {"key":"道具", "args": ["1000,2000"]})
  # 请求标记完成新手引导
  # task.add_command("CWMarkFinishGuideMessage", {})

  # 请求获取功能状态信息列表
  # task.add_command("CWGetFunctionStateListMessage", {"functionId": 0})
  # 请求获取子功能id列表消息
  # task.add_command("CWGetSubFunctionIdListMessage", {"functionId": 0})



  # 限时挑战 ===============================================================>
  # 请求获取玩家挑战信息
  # task.add_command("CWGetPlayerChallengeInfoMessage", {})
  # 请求领取限时挑战任务奖励
  # task.add_command("CWGetChallengeRewardMessage", {"id": 1})

  # 冲级挑战 ===============================================================>
  # 请求获取冲级活动信息
  # task.add_command("CWGetUpgradeActivityInfoMessage", {})
  # 请求领取冲级活动奖励
  # task.add_command("CWGetUpgradeActivityRewardMessage", {"id": 1})

  # 请求发送聊天消息
  # task.add_command("CISendChatMessage", {"chatType": 5, "msgType": "CONTEXT", "msg": "111", "to": -2, "param": None, "param2": None})


  # 请求执行水果机游戏命令
  # task.add_command("CZFruitExecuteCommandMessage", {"command": "FRUIT_SYNC"})

  # 请求获取语音房礼物图鉴列表信息
  # task.add_command("CZGetVoiceGiftHandBookListMessage", {})