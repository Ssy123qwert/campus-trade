"""
订单页面
显示我的订单（买的 + 卖的），支持状态切换
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel,
                             QPushButton, QScrollArea, QFrame, QTabWidget,
                             QMessageBox)
from PyQt5.QtCore import Qt
from PyQt5.QtGui import QFont

from ..api.client import api
from ..components.toast import Toast
from .review_page import ReviewDialog, ReviewsPage


class OrdersPage(QWidget):
    """订单列表页面，分"我买的"和"我卖的"两个标签"""

    STATUS_MAP = {
        0: "待支付", 1: "已支付",
        2: "已发货", 3: "已收货",
        4: "已完成", 5: "已取消"
    }

    def __init__(self, user_id: int):
        super().__init__()
        self.user_id = user_id
        self._setup_ui()

    def _setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        # 标题
        header = QFrame()
        header.setFixedHeight(44)
        header.setStyleSheet("background: white; border-bottom: 1px solid #eee;")
        h_layout = QVBoxLayout(header)
        h_layout.setContentsMargins(16, 0, 16, 0)
        title = QLabel("📦 我的订单")
        title.setFont(QFont("微软雅黑", 16, QFont.Bold))
        h_layout.addWidget(title)
        layout.addWidget(header)

        # Tab 切换：我买的 / 我卖的
        tabs = QFrame()
        tabs.setFixedHeight(44)
        tabs.setStyleSheet("background: white;")
        tab_layout = QHBoxLayout(tabs)
        tab_layout.setContentsMargins(0, 0, 0, 0)

        self.btn_buy = QPushButton("我买的")
        self.btn_buy.setStyleSheet(self._tab_style(True))
        self.btn_buy.clicked.connect(lambda: self._switch_tab("buy"))
        tab_layout.addWidget(self.btn_buy)

        self.btn_sell = QPushButton("我卖的")
        self.btn_sell.setStyleSheet(self._tab_style(False))
        self.btn_sell.clicked.connect(lambda: self._switch_tab("sell"))
        tab_layout.addWidget(self.btn_sell)

        layout.addWidget(tabs)

        # 订单列表区
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setStyleSheet("border: none; background: transparent;")

        self.container = QWidget()
        self.container.setStyleSheet("background: transparent;")
        self.order_grid = QVBoxLayout(self.container)
        self.order_grid.setContentsMargins(16, 12, 16, 12)
        self.order_grid.setSpacing(10)
        self.order_grid.addStretch()

        scroll.setWidget(self.container)
        layout.addWidget(scroll, 1)

        self.current_tab = "buy"
        self._load_orders()

    def _switch_tab(self, tab: str):
        """切换买/卖标签"""
        self.current_tab = tab
        self.btn_buy.setStyleSheet(self._tab_style(tab == "buy"))
        self.btn_sell.setStyleSheet(self._tab_style(tab == "sell"))
        self._load_orders()

    def _load_orders(self):
        """加载订单列表"""
        # 清空
        while self.order_grid.count() > 0:
            item = self.order_grid.takeAt(0)
            if item.widget():
                item.widget().deleteLater()

        try:
            result = api.get_my_orders(self.user_id, self.current_tab)
            if result.get("code") != 200:
                self._show_empty("加载失败")
                return

            orders = result.get("data", [])
            if not orders:
                self._show_empty(
                    "还没有订单哦～" if self.current_tab == "buy"
                    else "还没有人买你的商品～")
                return

            for order in orders:
                card = self._build_order_card(order)
                self.order_grid.addWidget(card)

            self.order_grid.addStretch()
        except Exception:
            self._show_empty("加载失败，请检查网络")

    def _build_order_card(self, order: dict) -> QFrame:
        """构建订单卡片"""
        card = QFrame()
        card.setStyleSheet("""
            QFrame { background: white; border-radius: 10px;
                     border: 1px solid #eee; }
            QFrame:hover { border-color: #07c160; }
        """)
        card.setFixedHeight(80)
        layout = QHBoxLayout(card)
        layout.setContentsMargins(16, 12, 16, 12)

        # 商品信息
        info = QVBoxLayout()
        info.setSpacing(4)

        title = QLabel(order.get("productTitle", "商品"))
        title.setFont(QFont("微软雅黑", 14, QFont.Bold))
        info.addWidget(title)

        price = QLabel(f"¥{order.get('totalPrice', 0):.2f}")
        price.setStyleSheet("color: #f44336; font-size: 16px; font-weight: bold;")
        info.addWidget(price)

        layout.addLayout(info, 1)

        # 状态标签
        status = order.get("status", 0)
        status_label = QLabel(self.STATUS_MAP.get(status, "未知"))
        status_label.setStyleSheet({
            0: "color: #ff9800; font-weight: bold;",
            1: "color: #2196f3; font-weight: bold;",
            2: "color: #9c27b0; font-weight: bold;",
            3: "color: #07c160; font-weight: bold;",
        }.get(status, "color: #999;"))
        layout.addWidget(status_label, alignment=Qt.AlignCenter)

        # 操作按钮
        order_id = order.get("id")
        seller_id = order.get("sellerId")
        if self.current_tab == "buy" and status == 1:
            btn = QPushButton("确认收货")
            btn.setStyleSheet("""
                QPushButton { padding: 6px 14px; background: #07c160;
                              color: white; border: none;
                              border-radius: 6px; font-size: 12px; }
                QPushButton:hover { background: #06ad56; }
            """)
            btn.clicked.connect(
                lambda checked, oid=order_id, sid=seller_id: self._confirm_and_review(oid, sid))
            layout.addWidget(btn)

        return card

    def _show_empty(self, text: str):
        label = QLabel(text)
        label.setAlignment(Qt.AlignCenter)
        label.setStyleSheet("color: #999; font-size: 16px; padding: 60px;")
        self.order_grid.addWidget(label)
        self.order_grid.addStretch()

    def _confirm_and_review(self, order_id: int, seller_id: int):
        """确认收货后弹出评价窗口"""
        result = api.update_order_status(order_id, 3)
        if result.get("code") == 200:
            Toast.show(self, "已确认收货！")
            self._load_orders()
            # 弹出评价窗口
            dialog = ReviewDialog(order_id, seller_id, self)
            dialog.exec_()
        else:
            QMessageBox.warning(self, "操作失败",
                                result.get("msg", "请稍后重试"))

    def _tab_style(self, active: bool) -> str:
        if active:
            return """
                QPushButton {
                    background: white; color: #07c160;
                    border: none; border-bottom: 2px solid #07c160;
                    font-weight: bold; font-size: 14px; padding: 10px;
                }
            """
        return """
            QPushButton {
                background: white; color: #999;
                border: none; border-bottom: 2px solid transparent;
                font-size: 14px; padding: 10px;
            }
            QPushButton:hover { color: #07c160; }
        """
