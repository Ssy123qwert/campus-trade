"""
商品卡片组件
在商品列表中展示单个商品的缩略信息
"""
from PyQt5.QtWidgets import (QFrame, QHBoxLayout, QVBoxLayout, QLabel,
                             QPushButton, QWidget)
from PyQt5.QtCore import Qt, pyqtSignal
from PyQt5.QtGui import QPixmap, QFont
import requests
from io import BytesIO

from ..api.client import api


class ProductCard(QFrame):
    """商品卡片，显示缩略图、标题、价格、状态"""

    # 点击卡片信号，传递商品ID
    clicked = pyqtSignal(int)

    def __init__(self, product_data: dict, parent=None):
        super().__init__(parent)
        self.product_id = product_data.get("id")
        self._setup_ui(product_data)

    def _setup_ui(self, data: dict):
        """构建卡片界面"""
        self.setObjectName("productCard")
        self.setFixedHeight(100)
        self.setCursor(Qt.PointingHandCursor)
        self.setStyleSheet("""
            #productCard {
                background: white;
                border-radius: 10px;
                border: 1px solid #eee;
            }
            #productCard:hover {
                border-color: #07c160;
                background: #fafff7;
            }
        """)

        layout = QHBoxLayout(self)
        layout.setContentsMargins(10, 8, 12, 8)
        layout.setSpacing(12)

        # ── 缩略图 ──
        self.thumb = QLabel()
        self.thumb.setFixedSize(80, 80)
        self.thumb.setAlignment(Qt.AlignCenter)
        self.thumb.setStyleSheet("background: #f0f0f0; border-radius: 6px;")
        self.thumb.setText("📷")
        layout.addWidget(self.thumb)

        # 异步加载图片
        images_str = data.get("images", "[]")
        import json
        try:
            images = json.loads(images_str) if isinstance(images_str, str) else images_str
            if images:
                self._load_thumb(images[0])
        except Exception:
            pass

        # ── 信息 ──
        info = QVBoxLayout()
        info.setSpacing(4)

        # 标题
        title = QLabel(data.get("title", "未知商品"))
        title.setFont(QFont("微软雅黑", 14, QFont.Bold))
        title.setWordWrap(True)
        title.setMaximumHeight(40)
        info.addWidget(title)

        # 价格 + 分类
        meta = QHBoxLayout()
        price = QLabel(f"¥{data.get('price', 0):.2f}")
        price.setFont(QFont("微软雅黑", 16, QFont.Bold))
        price.setStyleSheet("color: #f44336;")
        meta.addWidget(price)

        category = QLabel(data.get("category", ""))
        category.setStyleSheet("color: #999; font-size: 12px; "
                               "background: #f0f0f0; padding: 2px 8px; "
                               "border-radius: 4px;")
        category.setAlignment(Qt.AlignCenter)
        meta.addWidget(category)
        meta.addStretch()
        info.addLayout(meta)

        layout.addLayout(info, 1)

        # 状态标签
        status = data.get("status", 1)
        status_label = QLabel({
            1: "在售", 2: "已售", 3: "已下架"
        }.get(status, "未知"))
        status_label.setStyleSheet({
            1: "color: #07c160; font-size: 12px; font-weight: bold;",
            2: "color: #999; font-size: 12px;",
            3: "color: #f44336; font-size: 12px;",
        }.get(status, "color: #999; font-size: 12px;"))
        layout.addWidget(status_label, alignment=Qt.AlignTop)

    def _load_thumb(self, image_path: str):
        """从后端加载缩略图"""
        try:
            url = api.BASE_URL + image_path
            resp = requests.get(url, timeout=5)
            if resp.status_code == 200:
                pixmap = QPixmap()
                pixmap.loadFromData(resp.content)
                scaled = pixmap.scaled(80, 80, Qt.KeepAspectRatio,
                                       Qt.SmoothTransformation)
                self.thumb.setPixmap(scaled)
        except Exception:
            pass  # 加载失败就用默认图标

    def mousePressEvent(self, event):
        """点击卡片发射信号"""
        self.clicked.emit(self.product_id)
        super().mousePressEvent(event)
