"""
消息/私信窗口
显示会话列表，点击进入聊天详情
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel,
                             QPushButton, QScrollArea, QFrame, QTextEdit,
                             QListWidget, QListWidgetItem, QSplitter,
                             QDialog, QInputDialog, QLineEdit)
from PyQt5.QtCore import Qt, QTimer, QThread, pyqtSignal
from PyQt5.QtGui import QFont

from ..api.client import api
from ..components.toast import Toast


class MessagePage(QWidget):
    """消息页面：左侧会话列表，右侧聊天窗口"""

    def __init__(self, user_id: int):
        super().__init__()
        self.user_id = user_id
        self.current_conversation = None
        self._setup_ui()
        self._load_conversations()

    def _setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        # 标题
        header = QFrame()
        header.setFixedHeight(44)
        header.setStyleSheet("background: white; border-bottom: 1px solid #eee;")
        h_layout = QHBoxLayout(header)
        h_layout.setContentsMargins(16, 0, 16, 0)
        title = QLabel("💬 消息")
        title.setFont(QFont("微软雅黑", 16, QFont.Bold))
        h_layout.addWidget(title)
        h_layout.addStretch()
        layout.addWidget(header)

        # 分割：左侧会话列表 | 右侧聊天
        splitter = QSplitter(Qt.Horizontal)

        # ── 左侧会话列表 ──
        left_panel = QWidget()
        left_layout = QVBoxLayout(left_panel)
        left_layout.setContentsMargins(0, 0, 0, 0)

        self.conv_list = QListWidget()
        self.conv_list.setStyleSheet("""
            QListWidget { border: none; background: white; }
            QListWidget::item { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; }
            QListWidget::item:hover { background: #f5fff5; }
            QListWidget::item:selected { background: #e8ffe8; }
        """)
        self.conv_list.currentRowChanged.connect(self._on_conversation_selected)
        left_layout.addWidget(self.conv_list)

        splitter.addWidget(left_panel)

        # ── 右侧聊天区 ──
        right_panel = QWidget()
        self.chat_layout = QVBoxLayout(right_panel)
        self.chat_layout.setContentsMargins(0, 0, 0, 0)

        # 聊天占位
        self.chat_placeholder = QLabel("选择一个会话开始聊天")
        self.chat_placeholder.setAlignment(Qt.AlignCenter)
        self.chat_placeholder.setStyleSheet("color: #999; font-size: 14px;")
        self.chat_layout.addWidget(self.chat_placeholder)

        # 聊天内容区域（初始隐藏）
        self.chat_content = QWidget()
        chat_vbox = QVBoxLayout(self.chat_content)
        chat_vbox.setContentsMargins(0, 0, 0, 0)

        # 聊天对方信息
        self.chat_header = QFrame()
        self.chat_header.setFixedHeight(44)
        self.chat_header.setStyleSheet("background: white; border-bottom: 1px solid #eee;")
        ch_layout = QHBoxLayout(self.chat_header)
        ch_layout.setContentsMargins(16, 0, 16, 0)
        self.chat_title = QLabel("")
        self.chat_title.setFont(QFont("微软雅黑", 14))
        ch_layout.addWidget(self.chat_title)
        ch_layout.addStretch()
        chat_vbox.addWidget(self.chat_header)

        # 消息列表
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setStyleSheet("border: none;")
        self.msg_container = QWidget()
        self.msg_container.setStyleSheet("background: #f5f5f5;")
        self.msg_vbox = QVBoxLayout(self.msg_container)
        self.msg_vbox.setContentsMargins(16, 12, 16, 12)
        self.msg_vbox.setSpacing(8)
        self.msg_vbox.addStretch()
        scroll.setWidget(self.msg_container)
        chat_vbox.addWidget(scroll, 1)

        # 输入栏
        input_bar = QFrame()
        input_bar.setFixedHeight(60)
        input_bar.setStyleSheet("background: white; border-top: 1px solid #eee;")
        input_layout = QHBoxLayout(input_bar)
        input_layout.setContentsMargins(12, 10, 12, 10)

        self.msg_input = QTextEdit()
        self.msg_input.setPlaceholderText("输入消息...")
        self.msg_input.setFixedHeight(36)
        self.msg_input.setStyleSheet("""
            border: 1px solid #ddd; border-radius: 18px;
            padding: 6px 14px; font-size: 13px;
        """)
        self.msg_input.setAcceptRichText(False)
        input_layout.addWidget(self.msg_input)

        btn_send = QPushButton("发送")
        btn_send.setFixedSize(56, 36)
        btn_send.setStyleSheet("""
            QPushButton { background: #07c160; color: white;
                          border: none; border-radius: 18px; font-weight: bold; }
            QPushButton:hover { background: #06ad56; }
        """)
        btn_send.clicked.connect(self._send_message)
        input_layout.addWidget(btn_send)

        chat_vbox.addWidget(input_bar)
        self.chat_content.setVisible(False)
        self.chat_layout.addWidget(self.chat_content)

        splitter.addWidget(right_panel)
        splitter.setSizes([250, 500])
        layout.addWidget(splitter, 1)

    def _load_conversations(self):
        """加载会话列表"""
        self.conv_list.clear()
        try:
            result = api.get_conversations(self.user_id)
            if result.get("code") != 200:
                return
            convs = result.get("data", [])
            for conv in convs:
                other_id = conv.get("otherId", 0)
                other_name = conv.get("otherName", f"用户{other_id}")
                last_msg = conv.get("lastMessage", "")[:30]
                product_title = conv.get("productTitle", "")
                text = f"{other_name}"
                if product_title:
                    text += f"\n关于: {product_title[:20]}"
                if last_msg:
                    text += f"\n{last_msg}"
                item = QListWidgetItem(text)
                item.setData(Qt.UserRole, {
                    "otherId": other_id,
                    "otherName": other_name,
                    "productId": conv.get("productId")
                })
                self.conv_list.addWidget(item)
        except Exception:
            empty = QListWidgetItem("暂无会话")
            self.conv_list.addWidget(empty)

    def _on_conversation_selected(self, index: int):
        """选择会话后加载聊天记录"""
        if index < 0:
            return
        item = self.conv_list.item(index)
        data = item.data(Qt.UserRole)
        if not data:
            return
        self.current_conversation = data
        self.chat_placeholder.setVisible(False)
        self.chat_content.setVisible(True)
        self.chat_title.setText(f"与 {data['otherName']} 聊天中")
        self._load_messages()

    def _load_messages(self):
        """加载聊天消息"""
        if not self.current_conversation:
            return
        data = self.current_conversation
        try:
            result = api.get_messages(
                self.user_id, data["otherId"], data.get("productId"))
            if result.get("code") != 200:
                return
            msgs = result.get("data", [])
            # 清空旧消息
            while self.msg_vbox.count() > 1:
                item = self.msg_vbox.takeAt(0)
                if item.widget():
                    item.widget().deleteLater()
            # 添加消息
            for msg in msgs:
                role = "me" if msg.get("fromId") == self.user_id else "other"
                content = msg.get("content", "")
                self._add_chat_bubble(role, content)
        except Exception:
            pass

    def _add_chat_bubble(self, role: str, content: str):
        """添加聊天气泡（微信风格：我右绿，他左白）"""
        bubble = QFrame()
        bubble.setMaximumWidth(280)
        if role == "me":
            bubble.setStyleSheet("""
                background: #95ec69; color: #333;
                border-radius: 10px 10px 4px 10px; padding: 8px 14px;
            """)
        else:
            bubble.setStyleSheet("""
                background: white; color: #333;
                border-radius: 10px 10px 10px 4px; padding: 8px 14px;
            """)
        label = QLabel(content)
        label.setWordWrap(True)
        label.setStyleSheet("background: transparent; font-size: 13px;")
        label.setFont(QFont("微软雅黑", 12))

        vbox = QVBoxLayout(bubble)
        vbox.setContentsMargins(0, 0, 0, 0)
        vbox.addWidget(label)

        # 自己的消息右对齐，对方的消息左对齐
        align = Qt.AlignRight if role == "me" else Qt.AlignLeft
        self.msg_vbox.insertWidget(self.msg_vbox.count() - 1, bubble, 0, align)

    def _send_message(self):
        """发送消息"""
        text = self.msg_input.toPlainText().strip()
        if not text or not self.current_conversation:
            return

        self.msg_input.clear()
        data = self.current_conversation
        try:
            result = api.send_message(
                self.user_id, data["otherId"],
                data.get("productId", 0), text)
            if result.get("code") == 200:
                self._add_chat_bubble("me", text)
            else:
                Toast.show(self, "发送失败")
        except Exception:
            Toast.show(self, "发送失败")
