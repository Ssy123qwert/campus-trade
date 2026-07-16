"""
商品详情窗口
展示商品完整信息：图片、标题、价格、描述、联系卖家
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout,
                             QLabel, QPushButton, QScrollArea,
                             QFrame, QTextEdit, QMessageBox)
from PyQt5.QtCore import Qt, QTimer
from PyQt5.QtGui import QFont, QPixmap
import requests
from io import BytesIO
import json

from ..api.client import api
from ..components.toast import Toast


class ProductDetailWindow(QWidget):
    """商品详情弹窗"""

    def __init__(self, product_id: int, user_id: int):
        super().__init__()
        self.product_id = product_id
        self.user_id = user_id
        self.setWindowTitle("商品详情")
        self.setFixedSize(500, 700)
        self.setStyleSheet("background: white;")

        self._setup_ui()
        self._load_data()

    def _setup_ui(self):
        """构建界面"""
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        # 滚动区域
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setStyleSheet("border: none;")

        content = QWidget()
        content_layout = QVBoxLayout(content)
        content_layout.setContentsMargins(20, 0, 20, 20)

        # 图片占位
        self.image_label = QLabel()
        self.image_label.setFixedHeight(250)
        self.image_label.setAlignment(Qt.AlignCenter)
        self.image_label.setStyleSheet("background: #f5f5f5; border-radius: 12px;")
        self.image_label.setText("加载中...")
        content_layout.addWidget(self.image_label)

        content_layout.addSpacing(16)

        # 标题
        self.title_label = QLabel()
        self.title_label.setFont(QFont("微软雅黑", 20, QFont.Bold))
        self.title_label.setWordWrap(True)
        content_layout.addWidget(self.title_label)

        # 价格
        self.price_label = QLabel()
        self.price_label.setFont(QFont("微软雅黑", 24, QFont.Bold))
        self.price_label.setStyleSheet("color: #f44336;")
        content_layout.addWidget(self.price_label)

        content_layout.addSpacing(12)

        # 信息行
        info = QHBoxLayout()
        self.category_label = QLabel()
        self.category_label.setStyleSheet(
            "color: #999; background: #f5f5f5; padding: 4px 12px; "
            "border-radius: 4px; font-size: 13px;")
        info.addWidget(self.category_label)

        self.condition_label = QLabel()
        self.condition_label.setStyleSheet(
            "color: #999; background: #f5f5f5; padding: 4px 12px; "
            "border-radius: 4px; font-size: 13px;")
        info.addWidget(self.condition_label)
        info.addStretch()
        content_layout.addLayout(info)

        content_layout.addSpacing(12)

        # 分割线
        line = QFrame()
        line.setFrameShape(QFrame.HLine)
        line.setStyleSheet("color: #eee;")
        content_layout.addWidget(line)

        # 描述
        self.desc_label = QLabel()
        self.desc_label.setWordWrap(True)
        self.desc_label.setStyleSheet("color: #333; font-size: 14px; line-height: 1.6;")
        content_layout.addWidget(self.desc_label)

        content_layout.addStretch()

        scroll.setWidget(content)
        layout.addWidget(scroll, 1)

        # 底部操作栏
        bottom = QFrame()
        bottom.setFixedHeight(60)
        bottom.setStyleSheet("background: white; border-top: 1px solid #eee;")
        bottom_layout = QHBoxLayout(bottom)
        bottom_layout.setContentsMargins(20, 8, 20, 8)

        self.btn_contact = QPushButton("💬 联系卖家")
        self.btn_contact.setStyleSheet(self._btn_primary())
        self.btn_contact.clicked.connect(self._contact_seller)
        bottom_layout.addWidget(self.btn_contact)

        self.btn_buy = QPushButton("🛒 立即购买")
        self.btn_buy.setStyleSheet(self._btn_primary())
        self.btn_buy.clicked.connect(self._buy_product)
        bottom_layout.addWidget(self.btn_buy)

        layout.addWidget(bottom)

    def _load_data(self):
        """加载商品数据"""
        result = api.get_product_detail(self.product_id)
        if result.get("code") != 200:
            self.title_label.setText("加载失败")
            return

        data = result.get("data", {})

        self.title_label.setText(data.get("title", ""))
        price = data.get("price", 0)
        orig = data.get("originalPrice", 0)
        if orig:
            self.price_label.setText(f"¥{price:.2f}  <span style='color:#ccc; font-size:16px; text-decoration:line-through;'>¥{orig:.2f}</span>")
        else:
            self.price_label.setText(f"¥{price:.2f}")
        self.price_label.setTextFormat(Qt.RichText)

        self.category_label.setText(data.get("category", ""))
        cond_map = {1: "全新", 2: "九成新", 3: "七成新", 4: "有瑕疵"}
        self.condition_label.setText(cond_map.get(data.get("condition"), "未知"))
        self.desc_label.setText(data.get("description", "暂无描述"))

        # 加载图片
        images_str = data.get("images", "[]")
        try:
            images = json.loads(images_str) if isinstance(images_str, str) else images_str
            if images:
                self._load_image(images[0])
        except Exception:
            self.image_label.setText("📷 暂无图片")

    def _load_image(self, path: str):
        """加载商品图片"""
        try:
            url = api.BASE_URL + path
            resp = requests.get(url, timeout=10)
            if resp.status_code == 200:
                pixmap = QPixmap()
                pixmap.loadFromData(resp.content)
                scaled = pixmap.scaled(
                    460, 250, Qt.KeepAspectRatio, Qt.SmoothTransformation)
                self.image_label.setPixmap(scaled)
        except Exception:
            self.image_label.setText("📷 图片加载失败")

    def _contact_seller(self):
        """联系卖家（显示提示）"""
        Toast.show(self, "私信功能开发中...")

    def _buy_product(self):
        """购买商品"""
        result = api.create_order(self.product_id, self.user_id)
        if result.get("code") == 200:
            QMessageBox.information(self, "成功", "订单已创建！")
        else:
            QMessageBox.warning(self, "失败",
                                result.get("msg", "创建订单失败"))

    def _btn_primary(self) -> str:
        return """
            QPushButton {
                padding: 12px; background: #07c160; color: white;
                border: none; border-radius: 8px; font-size: 15px;
                font-weight: bold;
            }
            QPushButton:hover { background: #06ad56; }
        """
