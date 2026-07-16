"""
主窗口
包含底部导航栏，切换首页/发布/消息/我的/AI 五个页面
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout,
                             QPushButton, QStackedWidget, QLabel,
                             QScrollArea, QFrame, QGridLayout, QLineEdit,
                             QComboBox)
from PyQt5.QtCore import Qt, QSize
from PyQt5.QtGui import QFont

from ..api.client import api
from ..components.product_card import ProductCard
from ..components.toast import Toast
from .product_detail import ProductDetailWindow
from .publish_dialog import PublishDialog
from .ai_chat import AiChatDialog
from .message_page import MessagePage
from .favorites_page import FavoritesPage
from .orders_page import OrdersPage
from .review_page import ReviewsPage
from .admin_page import AdminPage


class MainWindow(QWidget):
    """主窗口，包含底部5个tab页面"""

    TAB_ICONS = {
        0: "🏠", 1: "➕", 2: "💬", 3: "👤", 4: "🤖"
    }
    TAB_LABELS = {
        0: "首页", 1: "发布", 2: "消息", 3: "我的", 4: "AI"
    }

    def __init__(self, user_id: int, token: str):
        super().__init__()
        self.user_id = user_id
        api.token = token
        self.current_page = 0

        self.setWindowTitle("校园二手交易平台")
        self.setMinimumSize(900, 680)
        self._setup_ui()
        self._load_home()

    def _setup_ui(self):
        """构建界面"""
        self.setStyleSheet("background: #f5f5f5;")

        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        # ── 标题栏 ──
        header = QFrame()
        header.setFixedHeight(50)
        header.setStyleSheet("background: white; border-bottom: 1px solid #eee;")
        h_layout = QHBoxLayout(header)
        h_layout.setContentsMargins(16, 0, 16, 0)

        title = QLabel("校园二手交易")
        title.setFont(QFont("微软雅黑", 18, QFont.Bold))
        title.setStyleSheet("color: #07c160;")
        h_layout.addWidget(title)
        h_layout.addStretch()

        # 最小化/关闭按钮
        btn_min = QPushButton("─")
        btn_min.setFixedSize(32, 28)
        btn_min.setStyleSheet(self._btn_style())
        btn_min.clicked.connect(self.showMinimized)
        h_layout.addWidget(btn_min)

        btn_close = QPushButton("✕")
        btn_close.setFixedSize(32, 28)
        btn_close.setStyleSheet(self._btn_style() + "color: #f44336;")
        btn_close.clicked.connect(self.close)
        h_layout.addWidget(btn_close)

        layout.addWidget(header)

        # ── 内容区（堆叠页面） ──
        self.stack = QStackedWidget()

        # 页面0: 首页
        self.home_page = self._build_home_page()
        self.stack.addWidget(self.home_page)

        # 页面1: 发布（占位，点击tab时打开对话框）
        self.stack.addWidget(QWidget())

        # 页面2: 消息
        self.msg_page = self._build_msg_page()
        self.stack.addWidget(self.msg_page)

        # 页面3: 我的
        self.profile_page = self._build_profile_page()
        self.stack.addWidget(self.profile_page)

        # 页面4: AI
        self.ai_page = self._build_ai_page()
        self.stack.addWidget(self.ai_page)

        layout.addWidget(self.stack, 1)

        # ── 底部导航栏 ──
        nav = QFrame()
        nav.setFixedHeight(60)
        nav.setStyleSheet("background: white; border-top: 1px solid #eee;")
        nav_layout = QHBoxLayout(nav)
        nav_layout.setContentsMargins(0, 0, 0, 0)
        nav_layout.setSpacing(0)

        self.nav_btns = []
        for i in range(5):
            btn = QPushButton(f"{self.TAB_ICONS[i]}\n{self.TAB_LABELS[i]}")
            btn.setFixedSize(180, 60)
            btn.setFont(QFont("微软雅黑", 10))
            btn.setStyleSheet(self._nav_style(False))
            btn.clicked.connect(lambda checked, idx=i: self._switch_tab(idx))
            self.nav_btns.append(btn)
            nav_layout.addWidget(btn)

        layout.addWidget(nav)
        self._update_nav(0)

    # ─────────── 首页 ───────────

    def _build_home_page(self) -> QWidget:
        """构建首页（商品列表 + 搜索）"""
        page = QWidget()
        layout = QVBoxLayout(page)
        layout.setContentsMargins(16, 12, 16, 12)

        # ── 公告栏 ──
        self.announcement_bar = QFrame()
        self.announcement_bar.setStyleSheet("""
            background: #fff8e1; border-radius: 8px;
            border: 1px solid #ffe082;
        """)
        ann_layout = QHBoxLayout(self.announcement_bar)
        ann_layout.setContentsMargins(12, 8, 12, 8)
        self.ann_label = QLabel("📢 加载公告中...")
        self.ann_label.setStyleSheet("color: #e65100; font-size: 13px;")
        ann_layout.addWidget(self.ann_label)
        layout.addWidget(self.announcement_bar)

        # 搜索栏
        search_bar = QHBoxLayout()
        self.search_input = QLineEdit()
        self.search_input.setPlaceholderText("搜索商品...")
        self.search_input.setStyleSheet("""
            padding: 10px 16px; border: 1px solid #ddd;
            border-radius: 20px; font-size: 14px; background: white;
        """)
        self.search_input.returnPressed.connect(self._do_search)
        search_bar.addWidget(self.search_input)

        self.category_combo = QComboBox()
        self.category_combo.addItem("全部分类", "")
        self.category_combo.addItems([
            "数码电子", "书籍教材", "生活用品",
            "服饰鞋包", "运动户外", "其他"
        ])
        self.category_combo.setStyleSheet("""
            padding: 8px; border: 1px solid #ddd;
            border-radius: 8px; font-size: 13px; background: white;
            min-width: 100px;
        """)
        self.category_combo.currentIndexChanged.connect(self._do_search)
        search_bar.addWidget(self.category_combo)
        layout.addLayout(search_bar)

        # 商品列表（滚动区域）
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setStyleSheet("border: none; background: transparent;")

        self.product_container = QWidget()
        self.product_container.setStyleSheet("background: transparent;")
        self.product_grid = QVBoxLayout(self.product_container)
        self.product_grid.setSpacing(10)
        self.product_grid.addStretch()

        scroll.setWidget(self.product_container)
        layout.addWidget(scroll, 1)

        return page

    def _load_home(self, keyword: str = None, category: str = None):
        """加载商品列表 + 公告"""
        # 加载公告
        try:
            ann_result = api.get_announcements()
            if ann_result.get("code") == 200:
                anns = ann_result.get("data", [])
                if anns:
                    texts = [a.get("content", "")[:50] for a in anns[:3]]
                    self.ann_label.setText("📢 " + " | ".join(texts))
                else:
                    self.ann_label.setText("📢 暂无公告")
        except Exception:
            self.ann_label.setText("📢 公告加载失败")

        # 加载商品
        result = api.get_products(keyword=keyword, category=category)
        if result.get("code") != 200:
            return

        # 清除旧卡片（保留最后的stretch）
        while self.product_grid.count() > 0:
            item = self.product_grid.takeAt(0)
            if item.widget():
                item.widget().deleteLater()

        records = result.get("data", {}).get("records", [])
        if not records:
            empty = QLabel("暂无商品\n快去发布第一个商品吧！")
            empty.setAlignment(Qt.AlignCenter)
            empty.setStyleSheet("color: #999; font-size: 16px; padding: 60px;")
            self.product_grid.addWidget(empty)
            self.product_grid.addStretch()
            return

        for data in records:
            card = ProductCard(data)
            card.clicked.connect(self._open_detail)
            self.product_grid.addWidget(card)

        self.product_grid.addStretch()

    def _do_search(self):
        """执行搜索/筛选"""
        keyword = self.search_input.text().strip() or None
        category = self.category_combo.currentData() or None
        self._load_home(keyword=keyword, category=category)

    # ─────────── 消息页面 ───────────

    def _build_msg_page(self) -> QWidget:
        """构建消息列表页面（会话列表 + 聊天）"""
        return MessagePage(self.user_id)

    # ─────────── 我的页面 ───────────

    def _build_profile_page(self) -> QWidget:
        """构建个人中心页面"""
        page = QWidget()
        layout = QVBoxLayout(page)
        layout.setContentsMargins(20, 20, 20, 20)

        # 头像+昵称
        avatar = QLabel("👤")
        avatar.setAlignment(Qt.AlignCenter)
        avatar.setFont(QFont("微软雅黑", 48))
        layout.addWidget(avatar)

        nickname = api.user_info.get("nickname", "") if hasattr(api, 'user_info') else ""
        self.profile_name = QLabel(nickname or f"用户 {self.user_id}")
        self.profile_name.setAlignment(Qt.AlignCenter)
        self.profile_name.setFont(QFont("微软雅黑", 16, QFont.Bold))
        layout.addWidget(self.profile_name)

        # 学号/手机号
        account = api.user_info.get("account", "") if hasattr(api, 'user_info') else ""
        if account:
            account_label = QLabel(account)
            account_label.setAlignment(Qt.AlignCenter)
            account_label.setStyleSheet("color: #999; font-size: 13px;")
            layout.addWidget(account_label)

        layout.addSpacing(30)

        # 功能菜单
        menu_items = [
            ("📋 我的发布", self._show_my_products),
            ("❤️ 我的收藏", self._open_favorites),
            ("📦 我的订单", self._open_orders),
            ("⭐ 我的评价", self._open_my_reviews),
        ]

        # 如果是管理员，显示管理后台入口
        is_admin = api.user_info.get("role") == 1 if hasattr(api, 'user_info') and api.user_info else False
        if is_admin:
            menu_items.append(("🔧 管理后台", self._open_admin))

        for text, callback in menu_items:
            btn = QPushButton(text)
            btn.setStyleSheet("""
                QPushButton {
                    text-align: left; padding: 14px 20px;
                    background: white; border: none;
                    border-radius: 8px; font-size: 15px;
                }
                QPushButton:hover { background: #f0f0f0; }
            """)
            btn.clicked.connect(callback)
            layout.addWidget(btn)

        layout.addStretch()

        return page

    def _show_my_products(self):
        """显示我的发布"""
        result = api.get_my_products(self.user_id)
        if result.get("code") == 200:
            products = result.get("data", [])
            Toast.show(self, f"你有 {len(products)} 件商品在售")
            # 切换到首页并搜索自己的商品
            self.search_input.setText("")
            self._switch_tab(0)

    def _open_favorites(self):
        """打开收藏页面"""
        self.fav_page = FavoritesPage(self.user_id)
        self.fav_page.setWindowTitle("我的收藏")
        self.fav_page.setGeometry(self.geometry())
        self.fav_page.show()

    def _open_orders(self):
        """打开订单页面"""
        self.orders_win = OrdersPage(self.user_id)
        self.orders_win.setWindowTitle("我的订单")
        self.orders_win.setGeometry(self.geometry())
        self.orders_win.show()

    def _open_my_reviews(self):
        """打开我的评价"""
        name = api.user_info.get("nickname", "") if hasattr(api, 'user_info') and api.user_info else ""
        self.reviews_win = ReviewsPage(self.user_id, name or f"用户{self.user_id}")
        self.reviews_win.show()

    def _open_admin(self):
        """打开管理后台"""
        self.admin_win = AdminPage(self.user_id)
        self.admin_win.show()

    def _open_detail(self, product_id: int):
        """打开商品详情窗口"""
        self.detail_win = ProductDetailWindow(product_id, self.user_id)
        self.detail_win.show()

    # ─────────── AI 页面 ───────────

    def _build_ai_page(self) -> QWidget:
        """构建AI助手页面"""
        page = QWidget()
        layout = QVBoxLayout(page)

        btn_open_ai = QPushButton("🤖 打开 AI 助手")
        btn_open_ai.setStyleSheet("""
            QPushButton {
                padding: 20px; font-size: 18px;
                background: white; border-radius: 12px;
                border: 2px solid #07c160; color: #07c160;
            }
            QPushButton:hover {
                background: #07c160; color: white;
            }
        """)
        btn_open_ai.clicked.connect(self._open_ai)
        layout.addWidget(btn_open_ai, alignment=Qt.AlignCenter)

        # AI对话记录简洁预览
        self.ai_preview = QLabel("点击上方按钮，向 AI 咨询二手交易问题")
        self.ai_preview.setAlignment(Qt.AlignCenter)
        self.ai_preview.setStyleSheet("color: #999; font-size: 14px; padding: 20px;")
        layout.addWidget(self.ai_preview)

        layout.addStretch()
        return page

    def _open_ai(self):
        """打开AI对话窗口"""
        self.ai_dialog = AiChatDialog(self)
        self.ai_dialog.show()

    # ─────────── 导航 ───────────

    def _switch_tab(self, index: int):
        """切换底部tab"""
        if index == 1:  # 发布按钮 -> 打开对话框
            dialog = PublishDialog(self.user_id, self)
            dialog.product_published.connect(
                lambda: self._switch_tab(0))
            dialog.exec_()
            return

        self.current_page = index
        self.stack.setCurrentIndex(index)
        self._update_nav(index)

        # 切到首页时刷新
        if index == 0:
            self._load_home()

    def _update_nav(self, active: int):
        """更新导航按钮样式"""
        for i, btn in enumerate(self.nav_btns):
            btn.setStyleSheet(self._nav_style(i == active))

    def _nav_style(self, active: bool) -> str:
        if active:
            return """
                QPushButton {
                    background: #f0fff5; color: #07c160;
                    border: none; border-top: 2px solid #07c160;
                    font-weight: bold;
                }
            """
        return """
            QPushButton {
                background: white; color: #999;
                border: none; border-top: 2px solid transparent;
            }
            QPushButton:hover { color: #07c160; }
        """

    def _btn_style(self) -> str:
        return """
            QPushButton {
                background: transparent; border: none;
                font-size: 16px; border-radius: 4px;
            }
            QPushButton:hover { background: #f0f0f0; }
        """
