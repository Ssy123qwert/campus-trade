"""
校园二手交易平台 - 桌面客户端（Web 套壳版）
自动启动后端 → 打开原生窗口显示网页前端
"""
import webview
import subprocess
import urllib.request
import os
import sys
import time

# ────────── 配置 ──────────
BACKEND_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
JAVA_PATH = r"C:\Program Files\Java\jdk-22\bin\java.exe"
JAR_PATH = os.path.join(BACKEND_DIR, "campus-trade-server-1.0.0.jar")
BACKEND_URL = "http://localhost:8080"


def is_backend_running() -> bool:
    """检查后端是否已启动"""
    try:
        resp = urllib.request.urlopen(BACKEND_URL + "/api/announcement/list", timeout=3)
        return resp.status == 200
    except Exception:
        return False


def start_backend():
    """后台启动 Java 后端"""
    try:
        startupinfo = subprocess.STARTUPINFO()
        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        proc = subprocess.Popen(
            [JAVA_PATH, "-jar", JAR_PATH],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=subprocess.CREATE_NO_WINDOW,
            cwd=BACKEND_DIR
        )
        return proc
    except Exception as e:
        return None


# ────────── 主流程 ──────────

def main():
    print("校园二手交易平台 v2.0")
    print("=" * 30)

    # 1. 启动后端
    if not is_backend_running():
        print("正在启动后端服务...")
        proc = start_backend()
        if proc:
            print("等待后端就绪...")
            for i in range(30):
                time.sleep(1)
                if is_backend_running():
                    print(f"后端就绪 ({i+1}s)")
                    break
            else:
                print("后端启动超时，请检查")
        else:
            print("后端启动失败")
    else:
        print("后端已在运行")

    # 2. 打开原生窗口
    window = webview.create_window(
        title="校园二手交易平台",
        url=BACKEND_URL,
        width=1200,
        height=800,
        resizable=True,
        min_size=(800, 600),
        confirm_close=True,
    )
    webview.start(debug=False)


if __name__ == "__main__":
    main()
