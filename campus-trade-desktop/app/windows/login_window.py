"""
登录/注册窗口
支持登录和注册切换，登录成功后自动跳转到主界面
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel,
                             QLineEdit, QPushButton, QFrame, QMessageBox,
                             QStackedWidget)
from PyQt5.QtCore import Qt, QTimer, pyqtSignal
from PyQt5.QtGui import QFont

from ..api.client import api


class LoginWindow(QWidget):
    """登录窗口，带登录/注册切换功能"""

    # 登录成功信号，携带用户ID
    login_success = pyqtSignal(int, str)

    def __init__(self):
        super().__init__()
        self.setWindowTitle("校园二手交易平台")
        self.setFixedSize(420, 560)

        # 主布局
        layout = QVBoxLayout(self)
        layout.setContentsMargins(40, 30, 40, 30)

        # ── Logo / 标题 ──
        title = QLabel("校园二手交易")
        title.setObjectName("loginTitle")
        title.setAlignment(Qt.AlignCenter)
        title.setFont(QFont("微软雅黑", 26, QFont.Bold))
        layout.addWidget(title)

        subtitle = QLabel("Campus Trade Platform")
        subtitle.setObjectName("loginSubtitle")
        subtitle.setAlignment(Qt.AlignCenter)
        layout.addWidget(subtitle)

        layout.addSpacing(30)

        # ── 登录/注册切换 ──
        self.stack = QStackedWidget()
        self.stack.addWidget(self._build_login_panel())   # index 0
        self.stack.addWidget(self._build_register_panel())  # index 1
        layout.addWidget(self.stack)

        layout.addStretch()

    # ─────────── 登录面板 ───────────

    def _build_login_panel(self) -> QWidget:
        """构建登录表单"""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(16)

        # 账号
        self.login_account = QLineEdit()
        self.login_account.setPlaceholderText("请输入学号/手机号/邮箱")
        self.login_account.setObjectName("input")
        layout.addWidget(self.login_account)

        # 密码
        self.login_password = QLineEdit()
        self.login_password.setPlaceholderText("请输入密码")
        self.login_password.setEchoMode(QLineEdit.Password)
        self.login_password.setObjectName("input")
        layout.addWidget(self.login_password)

        layout.addSpacing(8)

        # 登录按钮
        btn_login = QPushButton("登  录")
        btn_login.setObjectName("btnPrimary")
        btn_login.clicked.connect(self._do_login)
        btn_login.setDefault(True)
        layout.addWidget(btn_login)

        layout.addSpacing(12)

        # 切换到注册
        btn_to_register = QPushButton("没有账号？点此注册")
        btn_to_register.setObjectName("btnLink")
        btn_to_register.clicked.connect(lambda: self.stack.setCurrentIndex(1))
        layout.addWidget(btn_to_register, alignment=Qt.AlignCenter)

        # 回车触发登录
        self.login_password.returnPressed.connect(btn_login.click)

        return panel

    # ─────────── 注册面板 ───────────

    def _build_register_panel(self) -> QWidget:
        """构建注册表单"""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(16)

        # 账号
        self.reg_account = QLineEdit()
        self.reg_account.setPlaceholderText("学号/手机号（用于登录）")
        self.reg_account.setObjectName("input")
        layout.addWidget(self.reg_account)

        # 昵称
        self.reg_nickname = QLineEdit()
        self.reg_nickname.setPlaceholderText("昵称（显示在平台上）")
        self.reg_nickname.setObjectName("input")
        layout.addWidget(self.reg_nickname)

        # 密码
        self.reg_password = QLineEdit()
        self.reg_password.setPlaceholderText("密码（至少6位）")
        self.reg_password.setEchoMode(QLineEdit.Password)
        self.reg_password.setObjectName("input")
        layout.addWidget(self.reg_password)

        # 确认密码
        self.reg_confirm = QLineEdit()
        self.reg_confirm.setPlaceholderText("再次输入密码")
        self.reg_confirm.setEchoMode(QLineEdit.Password)
        self.reg_confirm.setObjectName("input")
        layout.addWidget(self.reg_confirm)

        layout.addSpacing(8)

        # 注册按钮
        btn_register = QPushButton("注  册")
        btn_register.setObjectName("btnPrimary")
        btn_register.clicked.connect(self._do_register)
        layout.addWidget(btn_register)

        layout.addSpacing(12)

        # 切换回登录
        btn_to_login = QPushButton("已有账号？点此登录")
        btn_to_login.setObjectName("btnLink")
        btn_to_login.clicked.connect(lambda: self.stack.setCurrentIndex(0))
        layout.addWidget(btn_to_login, alignment=Qt.AlignCenter)

        self.reg_confirm.returnPressed.connect(btn_register.click)

        return panel

    # ─────────── 业务逻辑 ───────────

    def _do_login(self):
        """执行登录"""
        account = self.login_account.text().strip()
        password = self.login_password.text().strip()

        if not account or not password:
            QMessageBox.warning(self, "提示", "请输入账号和密码")
            return

        result = api.login(account, password)
        if result.get("code") == 200:
            # 从登录响应获取用户ID（后端返回 { token: "用户ID", user: {...} }）
            user_id = api.user_id or 1
            self.login_success.emit(user_id, api.token)
        else:
            QMessageBox.warning(self, "登录失败",
                                result.get("msg", "请检查账号和密码"))

    def _do_register(self):
        """执行注册"""
        account = self.reg_account.text().strip()
        nickname = self.reg_nickname.text().strip()
        password = self.reg_password.text().strip()
        confirm = self.reg_confirm.text().strip()

        if not account or not nickname or not password:
            QMessageBox.warning(self, "提示", "请填写完整信息")
            return
        if password != confirm:
            QMessageBox.warning(self, "提示", "两次密码不一致")
            return
        if len(password) < 6:
            QMessageBox.warning(self, "提示", "密码至少6位")
            return

        result = api.register(account, password, nickname)
        if result.get("code") == 200:
            QMessageBox.information(self, "成功", "注册成功，请登录！")
            self.stack.setCurrentIndex(0)
            # 把账号填回登录框
            self.login_account.setText(account)
        else:
            QMessageBox.warning(self, "注册失败",
                                result.get("msg", "请稍后重试"))

