"""
收藏列表页面
显示用户收藏的商品
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QLabel,
                             QScrollArea, QPushButton, QFrame)
from PyQt5.QtCore import Qt
from PyQt5.QtGui import QFont

from ..api.client import api
from ..components.product_card import ProductCard
from .product_detail import ProductDetailWindow


class FavoritesPage(QWidget):
    """收藏商品列表页面"""

    def __init__(self, user_id: int):
        super().__init__()
        self.user_id = user_id
        self._setup_ui()
        self._load_favorites()

    def _setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        # 标题栏
        header = QFrame()
        header.setFixedHeight(44)
        header.setStyleSheet("background: white; border-bottom: 1px solid #eee;")
        h_layout = QVBoxLayout(header)
        h_layout.setContentsMargins(16, 0, 16, 0)
        title = QLabel("❤️ 我的收藏")
        title.setFont(QFont("微软雅黑", 16, QFont.Bold))
        h_layout.addWidget(title)
        layout.addWidget(header)

        # 商品列表
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setStyleSheet("border: none; background: transparent;")

        self.container = QWidget()
        self.container.setStyleSheet("background: transparent;")
        self.grid = QVBoxLayout(self.container)
        self.grid.setContentsMargins(16, 12, 16, 12)
        self.grid.setSpacing(10)
        self.grid.addStretch()

        scroll.setWidget(self.container)
        layout.addWidget(scroll, 1)

    def _load_favorites(self):
        """加载收藏列表"""
        # 清空旧卡片
        while self.grid.count() > 0:
            item = self.grid.takeAt(0)
            if item.widget():
                item.widget().deleteLater()

        try:
            result = api.get_favorites(self.user_id)
            if result.get("code") != 200:
                self._show_empty("加载失败")
                return

            products = result.get("data", [])
            if not products:
                self._show_empty("还没有收藏的商品\n去首页逛逛吧～")
                return

            for data in products:
                card = ProductCard(data)
                card.clicked.connect(self._open_detail)
                self.grid.addWidget(card)

            self.grid.addStretch()
        except Exception:
            self._show_empty("加载失败，请检查网络")

    def _show_empty(self, text: str):
        label = QLabel(text)
        label.setAlignment(Qt.AlignCenter)
        label.setStyleSheet("color: #999; font-size: 16px; padding: 60px;")
        self.grid.addWidget(label)
        self.grid.addStretch()

    def _open_detail(self, product_id: int):
        self.detail_win = ProductDetailWindow(product_id, self.user_id)
        self.detail_win.show()
