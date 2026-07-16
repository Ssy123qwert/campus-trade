"""
评价系统
提交评价 + 查看用户评价 + 好评率
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel,
                             QPushButton, QScrollArea, QFrame, QTextEdit,
                             QDialog, QMessageBox, QSlider, QDialogButtonBox)
from PyQt5.QtCore import Qt
from PyQt5.QtGui import QFont

from ..api.client import api
from ..components.toast import Toast


class ReviewDialog(QDialog):
    """提交评价弹窗（收货后评价卖家）"""

    def __init__(self, order_id: int, seller_id: int, parent=None):
        super().__init__(parent)
        self.order_id = order_id
        self.seller_id = seller_id
        self.setWindowTitle("提交评价")
        self.setFixedSize(400, 350)
        self.setStyleSheet("background: white;")
        self.rating = 5
        self._setup_ui()

    def _setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(24, 20, 24, 20)
        layout.setSpacing(16)

        title = QLabel("⭐ 给卖家评分")
        title.setFont(QFont("微软雅黑", 18, QFont.Bold))
        layout.addWidget(title)

        # 星级选择
        star_layout = QHBoxLayout()
        star_layout.addStretch()
        self.star_btns = []
        for i in range(5, 0, -1):
            btn = QPushButton("⭐" * i)
            btn.setStyleSheet("""
                QPushButton { background: transparent; border: none;
                              font-size: 24px; padding: 4px 8px; }
                QPushButton:hover { background: #fff3e0; border-radius: 8px; }
            """)
            btn.clicked.connect(lambda checked, s=i: self._set_rating(s))
            self.star_btns.append(btn)
            star_layout.addWidget(btn)
        star_layout.addStretch()
        layout.addLayout(star_layout)

        self.rating_label = QLabel("5 分（非常好）")
        self.rating_label.setAlignment(Qt.AlignCenter)
        self.rating_label.setStyleSheet("color: #ff9800; font-size: 16px; font-weight: bold;")
        layout.addWidget(self.rating_label)

        # 评价内容
        self.input_content = QTextEdit()
        self.input_content.setPlaceholderText("写写你的评价感受...（可选）")
        self.input_content.setFixedHeight(100)
        self.input_content.setStyleSheet("""
            border: 1px solid #ddd; border-radius: 8px;
            padding: 10px; font-size: 14px;
        """)
        layout.addWidget(self.input_content)

        layout.addStretch()

        # 按钮
        btn_layout = QHBoxLayout()
        btn_cancel = QPushButton("取消")
        btn_cancel.setStyleSheet(self._btn_secondary())
        btn_cancel.clicked.connect(self.reject)
        btn_layout.addWidget(btn_cancel)

        btn_submit = QPushButton("提交评价")
        btn_submit.setStyleSheet(self._btn_primary())
        btn_submit.clicked.connect(self._do_submit)
        btn_layout.addWidget(btn_submit)

        layout.addLayout(btn_layout)

    def _set_rating(self, score: int):
        """设置评分"""
        self.rating = score
        descs = {1: "很差", 2: "较差", 3: "一般", 4: "满意", 5: "非常好"}
        self.rating_label.setText(f"{score} 分（{descs[score]}）")

    def _do_submit(self):
        """提交评价"""
        content = self.input_content.toPlainText().strip()
        result = api.post("/api/review/create", {
            "orderId": self.order_id,
            "rating": self.rating,
            "content": content or "用户未填写评价内容"
        })
        if result.get("code") == 200:
            Toast.show(self, "评价成功！感谢你的反馈 🎉")
            self.accept()
        else:
            QMessageBox.warning(self, "提交失败",
                                result.get("msg", "请稍后重试"))

    def _btn_primary(self) -> str:
        return """QPushButton { padding: 10px; background: #07c160; color: white;
            border: none; border-radius: 8px; font-size: 14px; font-weight: bold; min-width: 100px; }
            QPushButton:hover { background: #06ad56; }"""

    def _btn_secondary(self) -> str:
        return """QPushButton { padding: 10px; background: #f5f5f5; color: #666;
            border: 1px solid #ddd; border-radius: 8px; font-size: 14px; min-width: 100px; }
            QPushButton:hover { background: #eee; }"""


class ReviewsPage(QWidget):
    """用户评价页面（查看某个用户的评价列表）"""

    def __init__(self, user_id: int, user_name: str = ""):
        super().__init__()
        self.target_user_id = user_id
        self.user_name = user_name
        self.setWindowTitle(f"{user_name or f'用户{user_id}'}的评价")
        self.setMinimumSize(500, 500)
        self.setStyleSheet("background: #f5f5f5;")
        self._setup_ui()
        self._load_reviews()

    def _setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        # 标题 + 好评率
        header = QFrame()
        header.setStyleSheet("background: white; border-bottom: 1px solid #eee;")
        h_layout = QVBoxLayout(header)
        h_layout.setContentsMargins(20, 16, 20, 16)

        name = self.user_name or f"用户{self.target_user_id}"
        title = QLabel(f"📊 {name} 的评价")
        title.setFont(QFont("微软雅黑", 18, QFont.Bold))
        h_layout.addWidget(title)

        self.rate_label = QLabel("好评率加载中...")
        self.rate_label.setStyleSheet("color: #ff9800; font-size: 14px; margin-top: 4px;")
        h_layout.addWidget(self.rate_label)

        layout.addWidget(header)

        # 评价列表
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setStyleSheet("border: none;")

        self.container = QWidget()
        self.review_layout = QVBoxLayout(self.container)
        self.review_layout.setContentsMargins(16, 12, 16, 12)
        self.review_layout.setSpacing(10)
        self.review_layout.addStretch()

        scroll.setWidget(self.container)
        layout.addWidget(scroll, 1)

    def _load_reviews(self):
        """加载评价列表"""
        # 加载好评率
        try:
            rate_result = api.get(f"/api/review/rate/{self.target_user_id}")
            if rate_result.get("code") == 200:
                rate = rate_result.get("data", 0)
                stars = "⭐" * int(rate // 20) if rate > 0 else "暂无评分"
                self.rate_label.setText(f"好评率 {rate:.0f}%  {stars}")
        except Exception:
            self.rate_label.setText("好评率加载失败")

        # 加载评价
        try:
            result = api.get(f"/api/review/user/{self.target_user_id}")
            if result.get("code") != 200:
                self._show_empty("加载失败")
                return
            records = result.get("data", {}).get("records", [])
            if not records:
                self._show_empty("暂无评价")
                return
            for r in records:
                self._add_review_card(r)
        except Exception:
            self._show_empty("加载失败")

    def _add_review_card(self, data: dict):
        """添加评价卡片"""
        card = QFrame()
        card.setStyleSheet("background: white; border-radius: 10px; border: 1px solid #eee;")
        layout = QVBoxLayout(card)
        layout.setContentsMargins(16, 12, 16, 12)
        layout.setSpacing(8)

        # 头部：评分 + 时间
        top = QHBoxLayout()
        rating = data.get("rating", 5)
        stars = QLabel("⭐" * rating)
        stars.setStyleSheet("font-size: 16px;")
        top.addWidget(stars)
        top.addStretch()
        time_label = QLabel(str(data.get("createTime", ""))[:10])
        time_label.setStyleSheet("color: #999; font-size: 12px;")
        top.addWidget(time_label)
        layout.addLayout(top)

        # 内容
        content = data.get("content", "")
        if content:
            content_label = QLabel(content)
            content_label.setWordWrap(True)
            content_label.setStyleSheet("color: #333; font-size: 14px;")
            layout.addWidget(content_label)

        self.review_layout.insertWidget(self.review_layout.count() - 1, card)

    def _show_empty(self, text: str):
        label = QLabel(text)
        label.setAlignment(Qt.AlignCenter)
        label.setStyleSheet("color: #999; font-size: 16px; padding: 60px;")
        self.review_layout.addWidget(label)
        self.review_layout.addStretch()
