import os
import subprocess

def generate_cmd_id_gradle_command():
  # 定义要执行的命令
  command = ["gradlew.bat", ":GameCore:run", "--args=../"]

  try:
    # 获取当前工作目录
    current_dir = os.getcwd()
    print(f"Current directory: {current_dir}")

    # 计算目标目录路径（上两级目录）
    target_dir = os.path.abspath(os.path.join(current_dir, '..', '..'))
    print(f"Changing directory to: {target_dir}")

    # 改变工作目录
    os.chdir(target_dir)

    # 使用 subprocess 运行命令
    result = subprocess.run(command, check=True, text=True, capture_output=True)

    # 打印标准输出
    print("Output:")
    print(result.stdout)

    # 打印标准错误
    if result.stderr:
      print("Errors:")
      print(result.stderr)

  except subprocess.CalledProcessError as e:
    print("An error occurred while executing the command:")
    print(e)

if __name__ == "__main__":
  generate_cmd_id_gradle_command()