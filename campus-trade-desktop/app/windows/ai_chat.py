"""
AI 智能助手对话窗口
调用后端 DeepSeek 接口，支持消息历史记录
"""
from PyQt5.QtWidgets import (QDialog, QVBoxLayout, QHBoxLayout,
                             QLabel, QPushButton, QScrollArea,
                             QTextEdit, QFrame, QWidget)
from PyQt5.QtCore import Qt, QThread, pyqtSignal
from PyQt5.QtGui import QFont

from ..api.client import api
from ..components.toast import Toast


class AiChatDialog(QDialog):
    """AI 对话窗口，发送问题并显示回复"""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("AI 智能助手")
        self.setFixedSize(450, 600)
        self.setStyleSheet("background: #f5f5f5;")
        self.messages = []  # 聊天记录 [{"role":"user/ai", "content":"..."}]
        self._setup_ui()

    def _setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        # ── 标题栏 ──
        header = QFrame()
        header.setFixedHeight(50)
        header.setStyleSheet("background: #07c160;")
        h_layout = QHBoxLayout(header)
        h_layout.setContentsMargins(16, 0, 16, 0)

        title = QLabel("🤖 AI 智能助手")
        title.setFont(QFont("微软雅黑", 16, QFont.Bold))
        title.setStyleSheet("color: white;")
        h_layout.addWidget(title)
        h_layout.addStretch()

        btn_close = QPushButton("✕")
        btn_close.setFixedSize(28, 28)
        btn_close.setStyleSheet("""
            QPushButton { background: transparent; color: white;
                          border: none; font-size: 16px; }
            QPushButton:hover { background: rgba(255,255,255,0.2);
                                border-radius: 14px; }
        """)
        btn_close.clicked.connect(self.close)
        h_layout.addWidget(btn_close)

        layout.addWidget(header)

        # ── 消息列表 ──
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setStyleSheet("border: none; background: transparent;")

        self.msg_container = QWidget()
        self.msg_container.setStyleSheet("background: transparent;")
        self.msg_layout = QVBoxLayout(self.msg_container)
        self.msg_layout.setContentsMargins(16, 16, 16, 16)
        self.msg_layout.setSpacing(12)

        # 欢迎消息
        self._add_welcome()

        self.msg_layout.addStretch()
        scroll.setWidget(self.msg_container)
        layout.addWidget(scroll, 1)

        # ── 输入栏 ──
        input_bar = QFrame()
        input_bar.setFixedHeight(80)
        input_bar.setStyleSheet("background: white; border-top: 1px solid #eee;")
        input_layout = QHBoxLayout(input_bar)
        input_layout.setContentsMargins(12, 10, 12, 10)

        self.input_edit = QTextEdit()
        self.input_edit.setPlaceholderText("输入你想问的问题...")
        self.input_edit.setFixedHeight(40)
        self.input_edit.setStyleSheet("""
            border: 1px solid #ddd; border-radius: 20px;
            padding: 8px 16px; font-size: 14px;
        """)
        self.input_edit.setAcceptRichText(False)
        self.input_edit.installEventFilter(self)
        input_layout.addWidget(self.input_edit)

        self.btn_send = QPushButton("发送")
        self.btn_send.setFixedSize(60, 40)
        self.btn_send.setStyleSheet("""
            QPushButton {
                background: #07c160; color: white;
                border: none; border-radius: 20px;
                font-size: 14px; font-weight: bold;
            }
            QPushButton:hover { background: #06ad56; }
            QPushButton:disabled { background: #ccc; }
        """)
        self.btn_send.clicked.connect(self._send_message)
        input_layout.addWidget(self.btn_send)

        layout.addWidget(input_bar)

        # 回车发送
        self.input_edit.textChanged.connect(self._check_enter)

    def _check_enter(self):
        """检测用户是否按了回车（通过文本变化）"""
        text = self.input_edit.toPlainText()
        if text.endswith("\n"):
            self.input_edit.setPlainText(text.rstrip("\n"))
            self._send_message()

    # ─────────── 消息气泡 ───────────

    def _add_welcome(self):
        """添加欢迎消息"""
        msg = "你好！我是校园二手交易平台的 AI 助手 👋\n\n我可以帮你：\n• 选购二手商品建议\n• 评估商品合理价格\n• 二手交易注意事项\n• 回答平台使用问题\n\n有什么想问的吗？"
        self._add_bubble("ai", msg)

    def _add_bubble(self, role: str, content: str):
        """添加对话气泡"""
        bubble = QFrame()
        bubble.setMaximumWidth(340)
        if role == "user":
            bubble.setStyleSheet("""
                background: #07c160; color: white;
                border-radius: 12px 12px 4px 12px;
                padding: 12px 16px;
            """)
        else:
            bubble.setStyleSheet("""
                background: white; color: #333;
                border-radius: 12px 12px 12px 4px;
                padding: 12px 16px;
            """)

        label = QLabel(content)
        label.setWordWrap(True)
        label.setFont(QFont("微软雅黑", 13))
        label.setStyleSheet("background: transparent; " +
                            ("color: white;" if role == "user" else "color: #333;"))
        label.setOpenExternalLinks(True)

        layout = QVBoxLayout(bubble)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.addWidget(label)

        # 插入到 stretch 之前
        self.msg_layout.insertWidget(self.msg_layout.count() - 1, bubble)

        self.messages.append({"role": role, "content": content})

    def _add_thinking(self) -> QLabel:
        """添加"思考中..."占位"""
        bubble = QFrame()
        bubble.setMaximumWidth(340)
        bubble.setStyleSheet("""
            background: white; color: #999;
            border-radius: 12px 12px 12px 4px;
            padding: 12px 16px;
        """)
        label = QLabel("思考中... 🤔")
        label.setFont(QFont("微软雅黑", 13))
        layout = QVBoxLayout(bubble)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.addWidget(label)
        self.msg_layout.insertWidget(self.msg_layout.count() - 1, bubble)
        self.thinking_label = label
        self.thinking_widget = bubble
        return label

    def _remove_thinking(self):
        """移除"思考中..."占位气泡"""
        if hasattr(self, "thinking_widget") and self.thinking_widget:
            self.thinking_widget.deleteLater()
            self.thinking_widget = None

    # ─────────── 发送消息 ───────────

    def _send_message(self):
        """发送消息到 AI"""
        text = self.input_edit.toPlainText().strip()
        if not text:
            return

        self.input_edit.clear()
        self.btn_send.setEnabled(False)

        # 用户消息
        self._add_bubble("user", text)

        # 思考中占位
        self._add_thinking()

        # 后台线程调用 API
        self.worker = AiWorker(text)
        self.worker.finished.connect(self._on_response)
        self.worker.start()

    def _on_response(self, result: dict):
        """接收 AI 回复"""
        self._remove_thinking()
        self.btn_send.setEnabled(True)

        if result.get("code") == 200:
            content = result.get("data", "AI暂时无法回复，请稍后再试")
            self._add_bubble("ai", content)
        else:
            self._add_bubble("ai",
                             f"😅 {result.get('msg', 'AI服务暂时不可用')}")


class AiWorker(QThread):
    """后台线程调用 AI API，避免界面卡顿"""

    finished = pyqtSignal(dict)

    def __init__(self, question: str):
        super().__init__()
        self.question = question

    def run(self):
        try:
            result = api.ai_chat(self.question)
        except Exception as e:
            result = {"code": 500, "msg": f"连接失败: {str(e)[:50]}"}
        self.finished.emit(result)
