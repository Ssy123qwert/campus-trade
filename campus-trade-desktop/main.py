"""
校园二手交易平台 - 桌面客户端入口（一键启动）

功能：自动启动后端 → 登录 → 主界面
技术：PyQt5 + requests，后端 Spring Boot @ localhost:8080

使用方式：
    双击 校园二手交易平台.exe 即可
"""
import sys
import os
import subprocess
import urllib.request
import json
import time

from PyQt5.QtWidgets import QApplication, QSystemTrayIcon, QMenu, QMessageBox, QStyleFactory
from PyQt5.QtGui import QIcon
from PyQt5.QtCore import Qt

# 确保可以找到 app 包
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.windows.login_window import LoginWindow
from app.windows.main_window import MainWindow
from app.components.toast import Toast
from app.theme import STYLESHEET

# ─────────── 后端自动启动 ───────────

BACKEND_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
JAVA_PATH = r"C:\Program Files\Java\jdk-22\bin\java.exe"
JAR_PATH = os.path.join(BACKEND_DIR, "campus-trade-server-1.0.0.jar")
BACKEND_URL = "http://localhost:8080"


def _is_backend_running() -> bool:
    """检查后端是否已在运行"""
    try:
        proxy_handler = urllib.request.ProxyHandler({})
        opener = urllib.request.build_opener(proxy_handler)
        resp = opener.open(BACKEND_URL + "/api/announcement/list", timeout=3)
        return resp.status == 200
    except Exception:
        return False


def _start_backend():
    """启动后端 Java 进程（隐藏窗口）"""
    try:
        proc = subprocess.Popen(
            [JAVA_PATH, "-jar", JAR_PATH],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=subprocess.CREATE_NO_WINDOW,
            cwd=BACKEND_DIR
        )
        return proc
    except Exception:
        return None


def _wait_for_backend(timeout: int = 30) -> bool:
    """等待后端就绪，最多等 timeout 秒"""
    for i in range(timeout):
        if _is_backend_running():
            return True
        time.sleep(1)
    return False


class App:
    """应用主控，管理窗口生命周期和系统托盘"""

    def __init__(self):
        self.app = QApplication(sys.argv)
        self.app.setApplicationName("校园二手交易平台")
        self.app.setQuitOnLastWindowClosed(False)  # 关闭窗口不退出，保留托盘

        # 启用 Fusion 风格（比 Windows 原生更现代）
        self.app.setStyle(QStyleFactory.create("Fusion"))

        # 全局样式（统一美化）
        self.app.setStyleSheet(STYLESHEET)

        self.tray = None
        self.login_win = None
        self.main_win = None

    def run(self):
        """启动应用（自动检测并启动后端）"""
        self._setup_tray()

        # 检查并启动后端
        if not _is_backend_running():
            print("正在启动后端服务...")
            proc = _start_backend()
            if proc:
                print("后端进程已启动，等待就绪...")
                if _wait_for_backend(30):
                    print("后端就绪！")
                else:
                    print("后端启动超时，请稍后手动检查")
            else:
                print("后端启动失败，你可能需要手动启动")
        else:
            print("后端服务已在运行")

        self._show_login()
        sys.exit(self.app.exec_())

    # ─────────── 登录流程 ───────────

    def _show_login(self):
        """打开登录窗口"""
        self.login_win = LoginWindow()
        self.login_win.login_success.connect(self._on_login_success)
        self.login_win.show()

    def _on_login_success(self, user_id: int, token: str):
        """登录成功，打开主窗口"""
        self.login_win.close()
        self.login_win = None

        self.main_win = MainWindow(user_id, token)

        # 系统托盘双击恢复窗口
        if self.tray:
            self.tray.showMessage("登录成功", "欢迎回来！",
                                  QSystemTrayIcon.Information, 2000)

        self.main_win.show()

    # ─────────── 系统托盘 ───────────

    def _setup_tray(self):
        """创建系统托盘图标"""
        # 如果没有托盘图标就用空图标
        icon = QIcon()

        self.tray = QSystemTrayIcon(icon)
        self.tray.setToolTip("校园二手交易平台")

        menu = QMenu()
        show_action = menu.addAction("显示窗口")
        show_action.triggered.connect(self._show_window)

        menu.addSeparator()
        quit_action = menu.addAction("退出")
        quit_action.triggered.connect(self._quit_app)

        self.tray.setContextMenu(menu)
        self.tray.activated.connect(self._on_tray_activated)
        self.tray.show()

    def _on_tray_activated(self, reason: int):
        """点击托盘图标恢复窗口"""
        if reason == QSystemTrayIcon.DoubleClick:
            self._show_window()

    def _show_window(self):
        """显示主窗口"""
        if self.main_win:
            self.main_win.showNormal()
            self.main_win.activateWindow()
            self.main_win.raise_()

    def _quit_app(self):
        """退出应用"""
        if self.tray:
            self.tray.hide()
        self.app.quit()


if __name__ == "__main__":
    App().run()
