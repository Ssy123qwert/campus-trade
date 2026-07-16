"""
管理后台
管理员管理用户和商品（需要用户角色为管理员）
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel,
                             QPushButton, QScrollArea, QFrame, QTableWidget,
                             QTableWidgetItem, QTabWidget, QHeaderView,
                             QMessageBox, QLineEdit, QAbstractItemView)
from PyQt5.QtCore import Qt, QThread, pyqtSignal
from PyQt5.QtGui import QFont

from ..api.client import api
from ..components.toast import Toast


class AdminPage(QWidget):
    """管理后台：用户管理 + 商品管理"""

    def __init__(self, user_id: int):
        super().__init__()
        self.user_id = user_id
        self.setWindowTitle("管理后台")
        self.setMinimumSize(800, 550)
        self.setStyleSheet("background: #f5f5f5;")
        self._setup_ui()

    def _setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        # 标题栏
        header = QFrame()
        header.setFixedHeight(50)
        header.setStyleSheet("background: #333;")
        h_layout = QHBoxLayout(header)
        h_layout.setContentsMargins(20, 0, 20, 0)
        title = QLabel("🔧 管理后台")
        title.setFont(QFont("微软雅黑", 18, QFont.Bold))
        title.setStyleSheet("color: white;")
        h_layout.addWidget(title)
        h_layout.addStretch()
        btn_close = QPushButton("✕")
        btn_close.setFixedSize(28, 28)
        btn_close.setStyleSheet("color: white; background: transparent; border: none; font-size: 16px;")
        btn_close.clicked.connect(self.close)
        h_layout.addWidget(btn_close)
        layout.addWidget(header)

        # Tab: 用户管理 | 商品管理
        tabs = QTabWidget()
        tabs.setStyleSheet("""
            QTabWidget::pane { border: none; background: transparent; }
            QTabBar::tab { padding: 10px 24px; font-size: 14px; }
            QTabBar::tab:selected { color: #07c160; font-weight: bold; border-bottom: 2px solid #07c160; }
        """)

        tabs.addTab(self._build_user_tab(), "👥 用户管理")
        tabs.addTab(self._build_product_tab(), "📦 商品管理")

        layout.addWidget(tabs, 1)

    # ─────────── 用户管理 ───────────

    def _build_user_tab(self) -> QWidget:
        """用户管理标签页"""
        page = QWidget()
        layout = QVBoxLayout(page)
        layout.setContentsMargins(16, 12, 16, 12)

        # 搜索栏
        search_bar = QHBoxLayout()
        self.user_search = QLineEdit()
        self.user_search.setPlaceholderText("搜索用户名/昵称...")
        self.user_search.setStyleSheet("""
            padding: 8px 14px; border: 1px solid #ddd;
            border-radius: 6px; font-size: 13px; background: white;
        """)
        self.user_search.returnPressed.connect(self._load_users)
        search_bar.addWidget(self.user_search)

        btn_search = QPushButton("搜索")
        btn_search.setStyleSheet(self._btn_small())
        btn_search.clicked.connect(self._load_users)
        search_bar.addWidget(btn_search)

        btn_refresh = QPushButton("刷新")
        btn_refresh.setStyleSheet(self._btn_small())
        btn_refresh.clicked.connect(self._load_users)
        search_bar.addWidget(btn_refresh)

        layout.addLayout(search_bar)

        # 用户表格
        self.user_table = QTableWidget()
        self.user_table.setColumnCount(5)
        self.user_table.setHorizontalHeaderLabels(["ID", "账号", "昵称", "角色", "操作"])
        self.user_table.horizontalHeader().setStretchLastSection(True)
        self.user_table.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        self.user_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.user_table.setEditTriggers(QAbstractItemView.NoEditTriggers)
        self.user_table.setStyleSheet("""
            QTableWidget { background: white; border: none; border-radius: 8px; }
            QTableWidget::item { padding: 8px; }
            QHeaderView::section { background: #f5f5f5; padding: 8px; border: none; font-weight: bold; }
        """)
        layout.addWidget(self.user_table, 1)

        self._load_users()
        return page

    def _load_users(self):
        """加载用户列表"""
        keyword = self.user_search.text().strip() or None
        try:
            result = api.get("/api/admin/users",
                           {"page": 1, "size": 100, "keyword": keyword})
            if result.get("code") != 200:
                Toast.show(self, "无管理权限" if "权限" in str(result.get("msg","")) else "加载失败")
                return
            records = result.get("data", {}).get("records", [])
            self.user_table.setRowCount(len(records))
            for row, user in enumerate(records):
                self.user_table.setItem(row, 0, QTableWidgetItem(str(user.get("id", ""))))
                self.user_table.setItem(row, 1, QTableWidgetItem(user.get("username", "")))
                self.user_table.setItem(row, 2, QTableWidgetItem(user.get("nickname", "")))
                role = "管理员" if user.get("role") == 1 else "用户"
                self.user_table.setItem(row, 3, QTableWidgetItem(role))

                # 删除按钮
                btn_del = QPushButton("删除")
                btn_del.setStyleSheet("""
                    QPushButton { padding: 4px 12px; background: #f44336; color: white;
                                  border: none; border-radius: 4px; font-size: 12px; }
                    QPushButton:hover { background: #d32f2f; }
                """)
                uid = user.get("id")
                btn_del.clicked.connect(lambda checked, uid=uid: self._delete_user(uid))
                self.user_table.setCellWidget(row, 4, btn_del)
        except Exception as e:
            Toast.show(self, f"加载失败: {str(e)[:30]}")

    def _delete_user(self, user_id: int):
        """删除用户"""
        reply = QMessageBox.question(self, "确认", f"确定删除用户 {user_id}？",
                                     QMessageBox.Yes | QMessageBox.No)
        if reply == QMessageBox.Yes:
            result = api.delete(f"/api/admin/user?userId={user_id}")
            if result.get("code") == 200:
                Toast.show(self, "已删除")
                self._load_users()
            else:
                QMessageBox.warning(self, "失败", result.get("msg", ""))

    # ─────────── 商品管理 ───────────

    def _build_product_tab(self) -> QWidget:
        """商品管理标签页"""
        page = QWidget()
        layout = QVBoxLayout(page)
        layout.setContentsMargins(16, 12, 16, 12)

        # 搜索
        search_bar = QHBoxLayout()
        self.prod_search = QLineEdit()
        self.prod_search.setPlaceholderText("搜索商品标题...")
        self.prod_search.setStyleSheet("""
            padding: 8px 14px; border: 1px solid #ddd;
            border-radius: 6px; font-size: 13px; background: white;
        """)
        self.prod_search.returnPressed.connect(self._load_products)
        search_bar.addWidget(self.prod_search)

        btn_search = QPushButton("搜索")
        btn_search.setStyleSheet(self._btn_small())
        btn_search.clicked.connect(self._load_products)
        search_bar.addWidget(btn_search)

        btn_refresh = QPushButton("刷新")
        btn_refresh.setStyleSheet(self._btn_small())
        btn_refresh.clicked.connect(self._load_products)
        search_bar.addWidget(btn_refresh)

        layout.addLayout(search_bar)

        # 商品表格
        self.prod_table = QTableWidget()
        self.prod_table.setColumnCount(6)
        self.prod_table.setHorizontalHeaderLabels(["ID", "标题", "价格", "分类", "状态", "操作"])
        self.prod_table.horizontalHeader().setStretchLastSection(True)
        self.prod_table.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        self.prod_table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.prod_table.setEditTriggers(QAbstractItemView.NoEditTriggers)
        self.prod_table.setStyleSheet("""
            QTableWidget { background: white; border: none; border-radius: 8px; }
            QTableWidget::item { padding: 8px; }
            QHeaderView::section { background: #f5f5f5; padding: 8px; border: none; font-weight: bold; }
        """)
        layout.addWidget(self.prod_table, 1)

        self._load_products()
        return page

    def _load_products(self):
        """加载商品列表"""
        keyword = self.prod_search.text().strip() or None
        try:
            result = api.get("/api/admin/products",
                           {"page": 1, "size": 100, "keyword": keyword})
            if result.get("code") != 200:
                Toast.show(self, "加载失败")
                return
            records = result.get("data", {}).get("records", [])
            self.prod_table.setRowCount(len(records))
            status_map = {1: "在售", 2: "已售", 3: "已下架"}
            for row, prod in enumerate(records):
                self.prod_table.setItem(row, 0, QTableWidgetItem(str(prod.get("id", ""))))
                self.prod_table.setItem(row, 1, QTableWidgetItem(prod.get("title", "")[:30]))
                self.prod_table.setItem(row, 2, QTableWidgetItem(f"¥{prod.get('price', 0):.2f}"))
                self.prod_table.setItem(row, 3, QTableWidgetItem(prod.get("category", "")))
                status = prod.get("status", 1)
                self.prod_table.setItem(row, 4, QTableWidgetItem(status_map.get(status, "未知")))

                # 下架/删除按钮
                btn_frame = QFrame()
                btn_layout = QHBoxLayout(btn_frame)
                btn_layout.setContentsMargins(4, 2, 4, 2)
                btn_layout.setSpacing(4)

                pid = prod.get("id")
                if status == 1:
                    btn_offline = QPushButton("下架")
                    btn_offline.setStyleSheet("padding: 4px 8px; background: #ff9800; color: white; border: none; border-radius: 4px; font-size: 11px;")
                    btn_offline.clicked.connect(lambda checked, pid=pid: self._offline_product(pid))
                    btn_layout.addWidget(btn_offline)

                btn_del = QPushButton("删除")
                btn_del.setStyleSheet("padding: 4px 8px; background: #f44336; color: white; border: none; border-radius: 4px; font-size: 11px;")
                btn_del.clicked.connect(lambda checked, pid=pid: self._delete_product(pid))
                btn_layout.addWidget(btn_del)

                self.prod_table.setCellWidget(row, 5, btn_frame)
        except Exception as e:
            Toast.show(self, f"加载失败: {str(e)[:30]}")

    def _offline_product(self, product_id: int):
        """下架商品"""
        result = api.put(f"/api/admin/product/status?productId={product_id}&status=3")
        if result.get("code") == 200:
            Toast.show(self, "已下架")
            self._load_products()
        else:
            QMessageBox.warning(self, "失败", result.get("msg", ""))

    def _delete_product(self, product_id: int):
        """删除商品"""
        reply = QMessageBox.question(self, "确认", f"确定删除商品 {product_id}？",
                                     QMessageBox.Yes | QMessageBox.No)
        if reply == QMessageBox.Yes:
            result = api.delete(f"/api/admin/product?productId={product_id}")
            if result.get("code") == 200:
                Toast.show(self, "已删除")
                self._load_products()
            else:
                QMessageBox.warning(self, "失败", result.get("msg", ""))

    def _btn_small(self) -> str:
        return """QPushButton { padding: 8px 16px; background: #07c160; color: white;
            border: none; border-radius: 6px; font-size: 13px; }
            QPushButton:hover { background: #06ad56; }"""
