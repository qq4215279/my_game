# encoding=utf8
import hashlib
import os


# import md5

class LogUtil:
  EXCEPTION_PREFIX = "	at "
  __log_path = ""
  __exception_lines = []
  __exceptions = {}

  def __init__(self, path):
    self.__log_path = path

  def __check_file(self):
    """
    检查LOG文件是否存在
    :return: true-存在
    """
    if not os.path.exists(self.__log_path):
      print("文件不存在!", self.__log_path)
      return False
    return True

  def __except_md5(self):
    """
    生成每个异常堆栈的 md5码，用于合并相同的异常堆栈
    :return: md5码
    """
    m = hashlib.md5()
    # m = md5.new()
    for line in self.__exception_lines[1:]:
      m.update(line.encode(encoding='utf-8'))
      # m.update(line)
    return m.hexdigest()

  def __update(self):
    """
    记录异常堆栈数据次数
    :return: None
    """
    key = self.__except_md5()
    if key in self.__exceptions:
      attrs = self.__exceptions[key]
      attrs["count"] += 1
      attrs["last"] = self.__exception_lines[:]
    else:
      self.__exceptions[key] = {"count": 1, "last": self.__exception_lines[:]}

  def __find_begin(self, line1, line2, line3):
    if line3.startswith(self.EXCEPTION_PREFIX) or line3.startswith(
        "Caused by: "):
      self.__exception_lines.append(line1)
      self.__exception_lines.append(line2)
      self.__exception_lines.append(line3)
      return self.__find_end
    else:
      return self.__find_begin

  def __find_end(self, line1, line2, line3):
    if line3.startswith(self.EXCEPTION_PREFIX) or line3.startswith(
        "Caused by: "):
      self.__exception_lines.append(line3)
      return self.__find_end
    else:
      # 结束
      # for l in exception_lines:
      #       print l,
      # print '-'*80, except_md5()
      self.__update()
      del self.__exception_lines[:]
      return self.__find_begin

  def find_exception(self):
    if not self.__check_file(): return None

    log_file = open(self.__log_path, mode='r', encoding='utf8')
    log_list = log_file.readlines()
    print("日志总行数：", len(log_list))

    pre_line1 = log_list[0]
    pre_line2 = log_list[1]
    process = self.__find_begin
    for line in log_list[2:]:
      process = process(pre_line1, pre_line2, line)
      pre_line1 = pre_line2
      pre_line2 = line

    # 结束最后一个
    if len(self.__exception_lines) > 0:
      self.__update()

    for key, attrs in self.__exceptions.items():
      print('-' * 40, key, 'X', attrs["count"], '-' * 40)
      for line in attrs["last"]:
        print(line, end='')


"""
python log_exception_find.py
"""
# D:\\TestFiles\\test.log
LogUtil(input("请输入log路径：")).find_exception()
