# encoding: utf-8

import inspect
import random
import threading
import time

import helper
from api_test import Task, Client, ResponseResult, name_protocol_id_dict, log
# protobuf 协议文件！
from client_pb2 import *
from helper import BalootGameOb

# 配置文件
config_dict = helper.parse_config()


"""
压测任务
"""
class ForceTask(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.force_time = config_dict["force_time"]
        self.is_record = config_dict["is_record"]
        self.task_player_limit = config_dict["task_player_limit"]
        self.startTime = time.time()
        self.endTime = self.startTime + int(config_dict["force_time"])
        # TODO 记录信息
        # self.recordInfo = helper.RecordInfo(self.startTime, self.endTime)
        self.interface_info_dict = {}
        # 停止标识
        self.stop_flag = False
        self.finish_stop_num = 0


        self.lock = threading.Lock()
        self.forceModules = self.__init_and_start_force_modules()

        # self.lock = threading.Lock()

    # 初始化所有压测模块
    def __init_and_start_force_modules(self):
        forceModules = []

        # 指定数量
        for moduleName, num in config_dict["force_modules"].items():
            # 分配任务数
            task_count = int(num / self.task_player_limit + 1)
            # 多余分配玩家数
            final_num = int(num % self.task_player_limit)

            # self.__record_dict(moduleName, num)
            for i in range(task_count):
                num = self.task_player_limit
                if i + 1 == task_count:
                    num = final_num

                if num <= 0:
                    continue

                # t = threading.Thread(target=self.__create_thread, args=(num, moduleName))
                # t.start()

                obj = self.__create_obj(num, moduleName)
                obj.task.t_name = f"task_{i}"
                # 开始任务
                obj.start()
                forceModules.append(obj)
                log.info(f"{obj.task.t_name} - __create_thread success~")

        return forceModules



    # 记录模块分配情况
    def __record_dict(self, moduleName, num):
        pass
        # total = 0
        #
        # if moduleName in self.recordInfo.force_modules_record_dict:
        #     total = self.recordInfo.force_modules_record_dict[moduleName]
        #
        # self.recordInfo.force_modules_record_dict[moduleName] = total + num

    # 获取所有测试模块
    def __get_test_modules(self):
        # 获取当前模块对象
        current_module = inspect.currentframe().f_globals['__name__']
        module = __import__(current_module)

        # 获取当前模块中的所有成员
        all_members = inspect.getmembers(module)
        # 筛选出类对象
        classes = [member[1] for member in all_members if inspect.isclass(member[1])]

        test_modules = []
        # 打印所有类的名称
        for cls in classes:
            class_name = cls.__name__
            if not class_name.endswith("Module"):
                continue
            if class_name == "ForceModule":
                continue
            test_modules.append(class_name)

        return test_modules


    def __create_thread(self, num, moduleName):
        obj = self.__create_obj(num, moduleName)
        # 开始任务
        obj.start()
        try:
            log.info(f"{obj.task.t_name} - __create_thread success~")
            self.lock.acquire()
            self.forceModules.append(obj)
        finally:
            self.lock.release()

    # 创建对象
    def __create_obj(self, num, class_name):
        # 根据类名获取类对象
        class_obj = globals()[class_name]

        # 使用类对象创建实例
        return class_obj(num, self.endTime, self.force_task_callback)


    def run(self) -> None:
        # self.__start()

        while True:
            cur = time.time()
            # 时间到 && 未结束过 && baloot游戏结束
            if cur >= self.endTime and not self.stop_flag and self.__check_bloot_match_game_force_module_over():
                self.__stop()
                self.stop_flag = True
                break

            time.sleep(1)

        log.info("ForceTask over~")

    # 校验所有baloot匹配是否都结束
    def __check_bloot_match_game_force_module_over(self) -> bool:
        for module in self.forceModules:
            if isinstance(module, BalootMatchGameForceModule) and module.state != 3:
                return False
        return True

    """
    压测开始
    """
    # def __start(self):
    #     for module_obj in self.forceModules:
    #         module_obj.start()

    """
    压测结束
    """
    def __stop(self):
        for module_obj in self.forceModules:
            module_obj.stopForceModule()

        # TODO
        # t_stop = threading.Thread(target=self.__run_stop())
        # t_stop.start()

    # def __run_stop(self):
    #     while True:
    #         if self.finish_stop_num >= len(self.forceModules):
    #             self.recordInfo.update_record(self.interface_info_dict)
    #             break
    #         else:
    #             time.sleep(1)

    # TODO 结束回调
    def force_task_callback(self, name_alltimes_dict: dict):
        log.info("force_task_callback .....")
        # try:
        #     self.lock.acquire()
        #
        #     for name in name_alltimes_dict.keys():
        #         allStartTime = name_alltimes_dict[name]
        #         if not name.startswith("Req"):
        #             continue
        #
        #         if name in self.interface_info_dict:
        #             interface_info = self.interface_info_dict[name]
        #         else:
        #             interface_info = helper.InterfaceInfo(name)
        #             self.interface_info_dict[name] = interface_info
        #
        #
        #         resName = name.replace("q", "s", 1)
        #         allResStartTime = name_alltimes_dict[resName]
        #
        #         for i in range(len(allStartTime)):
        #             start = allStartTime[i - 1]
        #             end = allResStartTime[i - 1]
        #             if end == -1:
        #                 interface_info.fail += 1
        #             else:
        #                 interface_info.success += 1
        #
        #             interface_info.update_record(end != -1, (end - start))
        #
        # except RuntimeError as e:
        #     pass
        # finally:
        #     self.finish_stop_num += 1
        #     self.lock.release()


"""
压测抽象模块
"""
class ForceModule(threading.Thread):
    def __init__(self, playerCount=1, endTime=None, force_task_callback=None):
        threading.Thread.__init__(self)
        # 状态: 0: 未初始化; 1: 首次; 2: 开始; 3: 结束
        self.state = 0
        # 玩家数量
        self.playerCount = playerCount
        # 结束时间
        self.endTime = endTime

        # Task
        self.task = Task(True, playerCount, self.handle_receive, self.init_finish_callback, self.close_finish_callback)
        self.loop = self.task.loop
        # 客户端(deviceId:Client)
        self.client_dict = self.task.client_dict

        # ForceTask回调
        self.force_task_callback = force_task_callback
        # 休眠时间 单位秒
        self.ticket = 1


    def run(self):
        first = True
        while True:
            # 结束
            if self.state == 3:
                break

            # 首次 and first
            if self.state == 1 and first:
                first = False
                # 开始
                self.state = 2

                self.__before_test()


            # 保持心跳
            self.add_command("CEPingMessage", {})

            # test
            self.__test()

            self.ticket = self.get_random_ticket()
            time.sleep(self.ticket)
            log.info(threading.current_thread().getName() + f" force休眠{self.ticket}s~")

        log.info(threading.current_thread().getName() + f" ForceModule over~")


    # 终止压测
    def stopForceModule(self):
        self.state = 3
        # 开始标记关闭
        self.task.close = True


    """
    模块名称
    """
    def get_force_module_name(self):
        return ""

    """
    添加命令
    """
    def add_command(self, protocol_name: str, params: {}, callback=None):
        self.task.add_command(protocol_name, params, callback)


    """
    测试前操作
    """
    def __before_test(self):
        self.do_before_test()


    """
    开始压测
    """
    def __test(self):
        if self.state == 2:
            self.do_test()


    """
    获取随机ticket
    """
    def get_random_ticket(self):
        return random.randint(1, 1)

    """
    do 测试前操作
    """
    def do_before_test(self):
        pass

    """
    do 开始压测
    """
    def do_test(self):
        pass

    """
    处理回调
    """
    def handle_receive(self, client: Client, responseResult: ResponseResult):
        pass

    """
    处理初始化(来自Task回调)
    """
    def init_finish_callback(self):
        log.info("handle_module_init....")
        self.state = 1

    """
    TODO 处理结束(来自Task回调)
    """
    def close_finish_callback(self, name_alltimes_dict: dict):
        log.info("handle_module_end....")
        # 回调 ForceTask
        self.force_task_callback(name_alltimes_dict)
        pass


# ===================================================================================>


'''
baloot 匹配房压测
'''
class BalootMatchGameForceModule(ForceModule):
    """
    构造函数
    """
    def __init__(self, playerCount=1, endTime=None, force_task_callback=None):
        super().__init__(playerCount, endTime, force_task_callback)

        # 房间信息dict
        self.baloot_game_ob_dict = {}
        # 正在游戏中玩家id集合
        self.game_player_id_set = set()

        self.__init()

    def __init(self):
        for client in self.client_dict.values():
            self.baloot_game_ob_dict[client.deviceId] = BalootGameOb()


    """
    模块名称
    """
    def get_force_module_name(self):
        return "baloot 匹配房压测"

    def do_before_test(self):
        # 定义增加金币回调
        def add_gold_callback(client: Client, responseResult: ResponseResult):
            balootGameOb = self.baloot_game_ob_dict[client.deviceId]
            # 2. 开始匹配
            client.add_callback_command("CWMatchGameMessage", {"gameId":1, "roomId": balootGameOb.roomId})


        # 定义 GCBalootRoomInfosMessage 回调(baloot游戏房间信息)
        def room_info_callback(client: Client, responseResult: ResponseResult):
            rooms_arr = responseResult.resMessage.rooms
            # 选择房间
            i = random.randint(0, len(rooms_arr) - 1)
            room = rooms_arr[i]
            # room = rooms_arr[1]
            balootGameOb = self.baloot_game_ob_dict[client.deviceId]
            balootGameOb.roomId = room.roomId
            balootGameOb.goldLowerLimit = room.goldLowerLimit
            balootGameOb.goldUpperLimit = room.goldUpperLimit

            # 金币条件判断
            gold = client.player.gold
            addGold = 0
            if gold < balootGameOb.goldLowerLimit:
                addGold = balootGameOb.goldLowerLimit
            if balootGameOb.goldUpperLimit != -1 and addGold > balootGameOb.goldUpperLimit:
                addGold = addGold - balootGameOb.goldUpperLimit

            # 1. 增加道具
            log.info("chooseRoom: ", room, " curGold: ",  gold, " needAddGold: ", addGold)
            if addGold != 0:
                client.add_callback_command("CWExecuteGmCmdMessage", {"key":"addItem", "args": [f"1000,{addGold}"]}, add_gold_callback)

            # 2. 开始匹配
            else:
                client.add_callback_command("CWMatchGameMessage", {"gameId":1, "roomId": balootGameOb.roomId})


        # 请求获取房间信息，回调进行匹配
        for client in self.client_dict.values():
            client.add_callback_command("CGBalootRoomInfosMessage", {"mode":"COMMON"}, room_info_callback)


    def do_test(self):
        now = time.time()

        for client in self.client_dict.values():
            balootGameOb = self.baloot_game_ob_dict[client.deviceId]
            # 匹配
            if not balootGameOb.isMatch:
                client.add_callback_command("CWMatchGameMessage", {"gameId": 1, "roomId": balootGameOb.roomId})


        # 校验游戏是否结束
        if len(self.game_player_id_set) <= 0 and self.endTime != None and now >= self.endTime:
            # TODO
            self.stopForceModule()


    def handle_receive(self, client: Client, responseResult: ResponseResult):
        # 定义 同步游戏数据(GCSyncGameMessage) 回调
        def sync_game_callback(client: Client, responseResult: ResponseResult):
            if responseResult.errorCodeValue == 'SUCCESS':
                resMessage = responseResult.resMessage
                # 同步玩家座位号
                for gamePlayerBean in resMessage.baseInfo.player:
                    if gamePlayerBean.seatInfo.playerId == client.player.playerId:
                        balootGameOb.seatId = gamePlayerBean.seatInfo.seatId
                        log.info(f"设置玩家座位号 playerId: {gamePlayerBean.seatInfo.playerId} seatId: {balootGameOb.seatId}")


                # 协议信息
                # 同步seq
                balootGameOb.seq = resMessage.balootInfo.seq
                balootGameOb.balootInfo = resMessage.balootInfo
                balootGameOb.baseInfo = resMessage.baseInfo
                log.info("sync_game_callback update seq: %d", balootGameOb.seq)
                log.info("sync_game_callback update seq: %d", balootGameOb.seq)

                execute_command()
            else:
                log.info("同步游戏失败~ Errorcode:  %s", responseResult.errorCodeValue)

        # 执行命令
        def execute_command():
            # 后续逻辑处理:
            for biderBean in balootGameOb.balootInfo.biderBeans:
                # 自己叫话
                if biderBean.bidSeatId == balootGameOb.seatId:
                    cmdBeansRes = []
                    # 过滤掉 PROJECT 命令
                    for cmdBean in biderBean.cmdBeans:
                        # PROJECT == 18
                        if cmdBean.command == 18:
                            continue
                        cmdBeansRes.append(cmdBean)

                    if len(cmdBeansRes) <= 0:
                        return

                    # 执行cmd
                    random.shuffle(cmdBeansRes)
                    biderBeanRes = cmdBeansRes[0]
                    # 追加参数
                    args = []
                    if len(biderBeanRes.args) > 0:
                        random.shuffle(biderBeanRes.args)
                        args.append(biderBeanRes.args[0])
                    # 执行命令
                    command = CommandEnum.Name(biderBeanRes.command)
                    log.info(f"玩家: {client.player.playerId} 执行命令, command: {command}, args: {args} seq: {balootGameOb.seq}")
                    client.add_callback_command("CGBalootExecuteCommandMessage", {"command": command, "args": args, "seq": balootGameOb.seq})


        cmd = responseResult.cmd
        resMessage = responseResult.resMessage

        balootGameOb = self.baloot_game_ob_dict[client.deviceId]
        # 1. 监听匹配推送
        if cmd == name_protocol_id_dict["OnGCMatchGameMessage"]:
            log.info("OnGCMatchGameMessage error ok: %s", responseResult.errorCodeValue)
            # 1. 匹配成功 开始准备
            if responseResult.errorCode.find("SUCCESS") != -1:
                balootGameOb.isMatch = True
                log.info("匹配成功 deviceId %s", client.deviceId)
                # 同步游戏数据
                client.add_callback_command("CGSyncGameMessage", {}, sync_game_callback)


            # 1.2. 匹配失败
            elif responseResult.errorCode.find("FAIL_MATCH_ERROR") != -1:
                log.info("匹配失败, 重新匹配, deviceId  %s", client.deviceId)
                # 重新匹配
                client.add_callback_command("CWMatchGameMessage", {"gameId": 1, "roomId": balootGameOb.roomId})

            # 1.3. 匹配超时
            elif responseResult.errorCode.find("FAIL_MATCH_TIMEOUT") != -1:
                log.info("匹配超时, 重新匹配, deviceId %s", client.deviceId)
                # 重新匹配
                client.add_callback_command("CWMatchGameMessage", {"gameId": 1, "roomId": balootGameOb.roomId})

            # 1.4. 玩家正在游戏中
            elif responseResult.errorCodeValue == 'FAIL_PLAYER_IN_GAME':
                log.info("玩家正在游戏中, 准备加入到游戏, deviceId %s", client.deviceId)
                balootGameOb.isMatch = True
                # 进入游戏
                client.add_callback_command("CWEnterGameMessage", {"gameId": 1, "roomId": balootGameOb.roomId})
                # 同步游戏数据
                client.add_callback_command("CGSyncGameMessage", {}, sync_game_callback)

        elif cmd == name_protocol_id_dict["WCMatchGameMessage"]:
            log.info("匹配推送 WCMatchGameMessage... errorCode: %s", responseResult.errorCodeValue)
            if responseResult.errorCodeValue == 'FAIL_PLAYER_IN_GAME':
                log.info("玩家正在游戏中, 准备加入到游戏, deviceId %s", client.deviceId)
                balootGameOb.isMatch = True
                # 进入游戏
                client.add_callback_command("CWEnterGameMessage", {"gameId": 1, "roomId": balootGameOb.roomId})
            # 同步游戏数据
            client.add_callback_command("CGSyncGameMessage", {}, sync_game_callback)

        # 2. 同步游戏
        elif cmd == name_protocol_id_dict["GCSyncGameMessage"]:
            pass

        # 3. 推送玩家游戏中变动消息（针对每次游戏操作推送）
        elif cmd == name_protocol_id_dict["OnGCPushBalootGameMessage"]:
            # SETTLE_FINAL == 17 游戏最终结算。添加退出游戏标记
            if resMessage.cmdBean.command == 17:
                self.game_player_id_set.remove(client.player.playerId)

            else:
                balootInfoBean =  resMessage.balootInfo
                balootGameOb.seq = balootInfoBean.seq
                balootGameOb.balootInfo = balootInfoBean

                self.game_player_id_set.add(client.player.playerId)
                # 执行命令
                execute_command()

        # 4. 执行游戏命令回调
        elif cmd == name_protocol_id_dict["GCBalootExecuteCommandMessage"]:
            # FAIL_GAME_NO_SYNC_SEQ: 游戏seq不同步 or 指令不存在 or 玩家等待操作
            # if responseResult.errorCodeValue == 74 or responseResult.errorCodeValue == 20 or responseResult.errorCodeValue == 19 or :
            # 不是成功：则同步最新数据
            if responseResult.errorCodeValue != 'SUCCESS':
                # 同步游戏数据
                client.add_callback_command("CGSyncGameMessage", {}, sync_game_callback)


# ===================================================================================>

'''
 登录压测(任务-商城-(公告邮件背包))
'''
class LoginForceModule(ForceModule):
    """
    构造函数
    """
    def __init__(self, playerCount=1, endTime=None, force_task_callback=None):
        super().__init__(playerCount, endTime, force_task_callback)

    """
    模块名称
    """
    def get_force_module_name(self):
        return "登录压测(任务-商城-(公告邮件背包))"


    def get_random_ticket(self):
        return random.randint(1, 5)

    """
    do 测试前操作
    """
    def do_before_test(self):
        log.info("_do_before_test... 登录压测")
        pass

    """
    do 开始压测
    """
    def do_test(self):
        log.info(f"{self.task.t_name} - {len(self.client_dict.values())} - do_test => add_callback_command")
        for client in self.client_dict.values():
            type = random.randint(0, 2)

            # 任务
            if type == 0:
                # 请求日常任务列表（定制化开发接口）
                client.add_callback_command("CWTaskTableMessage", {})
                # 请求领取任务奖励（定制化开发接口）
                client.add_callback_command("CWGetTaskRewardMessage", {})

            # 商城
            elif type == 1:
                # 礼物 金币 钻石 礼包 vip 新手礼包 头像框 背景 卡面 桌面 气泡框...
                functionIdList = [25, 501, 502, 503, 1102, 3103, 50401, 50402, 50403, 50404, 504015]
                functionId = functionIdList[random.randint(0, len(functionIdList) - 1)]

                # 创建订单回调
                def create_order_callback(client: Client, responseResult: ResponseResult):
                    if responseResult.resMessage != None:
                        client.add_callback_command("CWChargeByFakeMessage", {"orderId": responseResult.resMessage.orderId})

                # 获取商城信息回调
                def get_shop_goods_callback(client: Client, responseResult: ResponseResult):
                    resMessage = responseResult.resMessage
                    if resMessage != None and len(resMessage.goodsList) > 0:
                        goodsList = resMessage.goodsList
                        goodsId = goodsList[random.randint(0, len(goodsList) - 1)].goodsId
                        # 创建订单
                        client.add_callback_command("CWCreateOrderMessage", {"goodsId": goodsId, "payType":"", "extraInfo":""}, create_order_callback)


                # 请求商品列表信息
                client.add_callback_command("CWGetShopGoodsMessage", {"functionId": functionId}, get_shop_goods_callback)

            # 公告 - 邮件 - 背包道具
            elif type == 2:
                # 请求获取游戏公告列表消息
                client.add_callback_command("CWGetNoticeListMessage", {})
                # 操作邮件：获取所有邮件
                client.add_callback_command("CWOperateMailMessage", {"operate": "MAIL_GET"})
                # 请求获取玩家所有道具信息列表
                client.add_callback_command("CWGetPlayerItemListMessage", {})

    """
    处理回调
    """
    def handle_receive(self, client: Client, responseResult: ResponseResult):
        pass

"""
获取所有压测模块信息
"""
def get_all_force_module_info():
    # 获取当前模块对象
    current_module = inspect.currentframe().f_globals['__name__']
    module = __import__(current_module)

    # 获取当前模块中的所有成员
    all_members = inspect.getmembers(module)
    # 筛选出类对象
    classes = [member[1] for member in all_members if inspect.isclass(member[1])]

    res = ""
    # 打印所有类的名称
    for cls in classes:
        class_name = cls.__name__
        if not class_name.endswith("Module"):
            continue
        if class_name == "ForceModule":
            continue

        # 根据类名获取类对象
        class_obj = globals()[class_name]
        forceModuleName = class_obj.get_force_module_name()
        res += f"{forceModuleName}:{class_name};"

    return res


if __name__ == '__main__':
    print("\nDESCRIPTOR: ", DESCRIPTOR, " 注：本行打印调用, 避免idea自动导包将 (from client_pb2 import *) 移除!!!")
    helper.handle_dirty_players_config_data()
    # 开始压测
    task = ForceTask()
    task.start()
