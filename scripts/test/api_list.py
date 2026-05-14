"""
接口定义
"""

# 常用 ------------------------------------------------------------------------------>
# 绑定账户
# task.bind_account()
# 增加道具
# task.add_command("CWExecuteGmCmdMessage", {"key":"道具", "args": ["1000,2000"]})
# 购买钻石
# task.buy_goods(101)
# 请求获取功能状态信息列表
# task.add_command("CWGetFunctionStateListMessage", {"functionId": 0})
# 请求获取子功能id列表消息
# task.add_command("CWGetSubFunctionIdListMessage", {"functionId": 0})

# 基本信息  ------------------------------------------------------------------------------>
# 请求标记完成新手引导
# task.add_command("CWMarkFinishGuideMessage", {})
# 请求玩家个人信息  id: 玩家id
# task.add_command("CWGetPlayerInfoMessage", {})
# 请求获取自己赠送礼物记录列表  position: 槽位(从0开始)   count: 总数
# task.add_command("CWGetPlayerSendGiftListMessage", {"position": 2, "count": 200})
# 请求获取通用表情列表
# task.add_command("CWGetCommonEmoteListMessage", {})
# 请求获取游戏公告列表消息
# task.add_command("CWGetNoticeListMessage", {})
# 请求消除功能红点消息
# task.add_command("CWClearFuncRedPointMessage", {"functionId":202, "arg0":1})
# 请求获取新手累计签到信息
# task.add_command("CWGetSignInfoMessage", {})
# 请求获取新手累计签到信息
# task.add_command("CWGetSignRewardMessage", {"day": 1})


# baloot游戏 ------------------------------------------------------------------------------>
# 请求baloot游戏房间信息
# task.add_command("CGBalootRoomInfosMessage", {"mode":"COMMON"})