# encoding=utf8
# import md5
import hashlib
import sys

"""
python3移除了 md5，取而代之 hashlib
使用：
cat logs/action/WorldAction.log | python exception-unique.py
"""

# d = datetime.datetime.strptime("2020-01-06 13:35:20", "%Y-%m-%d %H:%M:%S")
# after = "2020-01-06 13:35:20"

EXCEPTION_PREFIX = "	at "
EXCEPTION_PREFIX_BEFORE = "Caused by: "
exception_lines = []

def except_md5():
  m = hashlib.md5()
  # m = md5.new()
  for line in exception_lines[1:]:
    m.update(line.encode(encoding='utf-8'))
    # m.update(line)
  return m.hexdigest()


exceptions = {}

def update():
  key = except_md5()
  if key in exceptions:
    attrs = exceptions[key]
    attrs["count"] += 1
    attrs["last"] = exception_lines[:]
  else:
    exceptions[key] = {"count":1, "last":exception_lines[:]}



def find_begin(line1, line2, line3):
  if line3.startswith(EXCEPTION_PREFIX) or line3.startswith(EXCEPTION_PREFIX_BEFORE):
    exception_lines.append(line1)
    exception_lines.append(line2)
    exception_lines.append(line3)
    return find_end
  else:
    return find_begin


def find_end(line1, line2, line3):
  if line3.startswith(EXCEPTION_PREFIX) or line3.startswith(EXCEPTION_PREFIX_BEFORE):
    exception_lines.append(line3)
    return find_end
  else:
    # 结束
    # for l in exception_lines:
    #       print l,
    # print '-'*80, except_md5()
    update()
    del exception_lines[:]
    return find_begin





pre_line1 = sys.stdin.readline()
pre_line2 = sys.stdin.readline()
process = find_begin
for line in sys.stdin:
  process = process(pre_line1, pre_line2, line)
  pre_line1 = pre_line2
  pre_line2 = line

# 结束最后一个
if len(exception_lines) > 0:
  # print '结束最后一个'
  # for l in exception_lines:
  #       print l,
  # print '-'*80, except_md5()
  update()


# print '='*80

# for key, attrs in exceptions.iteritems():
for key, attrs in exceptions.items():
  print('-'*40, key, 'X', attrs["count"], '-'*40)
  for line in attrs["last"]:
    print(line, end='')
