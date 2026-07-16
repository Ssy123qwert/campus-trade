"""
全局样式表
Fusion 风格 + 绿色主题，让 PyQt 界面更现代
"""
STYLESHEET = """
/* ===== 全局 ===== */
QWidget {
    font-family: "Microsoft YaHei", "微软雅黑", "Segoe UI", sans-serif;
    font-size: 13px;
    color: #2c3e50;
}
QWidget#centralWidget { background: #f0f2f5; }

/* ===== 登录页 ===== */
#loginTitle {
    color: #07c160;
    font-size: 28px;
    font-weight: bold;
    padding: 20px 0 0 0;
}
#loginSubtitle {
    color: #94a3b8;
    font-size: 13px;
    padding-bottom: 10px;
}
#btnLink {
    background: transparent;
    border: none;
    color: #07c160;
    font-size: 13px;
}
#btnLink:hover { color: #06ad56; }

/* ===== 输入框 ===== */
QLineEdit, QTextEdit, QSpinBox, QDoubleSpinBox {
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 10px 14px;
    background: #ffffff;
    font-size: 14px;
    color: #1e293b;
    selection-background-color: #bbf7d0;
}
QLineEdit:focus, QTextEdit:focus {
    border-color: #07c160;
    border-width: 2px;
    padding: 9px 13px;
}
QLineEdit:hover, QTextEdit:hover {
    border-color: #cbd5e1;
}
QLineEdit:disabled, QTextEdit:disabled {
    background: #f8fafc;
    color: #94a3b8;
}
QLineEdit#input {
    padding: 12px 16px;
    font-size: 14px;
}

/* ===== 普通按钮 ===== */
QPushButton {
    background: #ffffff;
    color: #1e293b;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 8px 18px;
    font-size: 13px;
    min-height: 20px;
}
QPushButton:hover {
    background: #f8fafc;
    border-color: #cbd5e1;
}
QPushButton:pressed {
    background: #f1f5f9;
}

/* ===== 主要按钮（绿色） ===== */
QPushButton#btnPrimary {
    background: #07c160;
    color: white;
    border: none;
    border-radius: 8px;
    padding: 10px 24px;
    font-size: 15px;
    font-weight: bold;
    min-height: 20px;
}
QPushButton#btnPrimary:hover {
    background: #06ad56;
}
QPushButton#btnPrimary:pressed {
    background: #059a4c;
}
QPushButton#btnPrimary:disabled {
    background: #cbd5e1;
    color: #94a3b8;
}

/* ===== 危险按钮（红色） ===== */
QPushButton#btnDanger {
    background: #ef4444;
    color: white;
    border: none;
    border-radius: 6px;
    padding: 6px 14px;
    font-size: 12px;
}
QPushButton#btnDanger:hover { background: #dc2626; }

/* ===== 下拉框 ===== */
QComboBox {
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 8px 12px;
    background: white;
    font-size: 13px;
    min-width: 100px;
}
QComboBox:focus { border-color: #07c160; }
QComboBox::drop-down {
    border: none;
    width: 30px;
}
QComboBox QAbstractItemView {
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    padding: 4px;
    background: white;
    selection-background-color: #dcfce7;
    selection-color: #07c160;
    outline: none;
}

/* ===== 表格 ===== */
QTableWidget {
    background: white;
    border: none;
    border-radius: 10px;
    gridline-color: #f1f5f9;
    font-size: 13px;
}
QTableWidget::item {
    padding: 10px 8px;
    border-bottom: 1px solid #f1f5f9;
    color: #1e293b;
}
QTableWidget::item:selected {
    background: #dcfce7;
    color: #1e293b;
}
QHeaderView::section {
    background: #f8fafc;
    color: #64748b;
    padding: 10px 8px;
    border: none;
    border-bottom: 2px solid #e2e8f0;
    font-weight: bold;
    font-size: 13px;
}

/* ===== 列表 ===== */
QListWidget {
    border: none;
    background: white;
    font-size: 13px;
    outline: none;
}
QListWidget::item {
    padding: 12px 16px;
    border-bottom: 1px solid #f8fafc;
}
QListWidget::item:hover {
    background: #f0fdf4;
}
QListWidget::item:selected {
    background: #dcfce7;
    color: #1e293b;
}

/* ===== 滚动条 ===== */
QScrollBar:vertical {
    width: 6px;
    background: transparent;
    margin: 0;
}
QScrollBar::handle:vertical {
    background: #cbd5e1;
    border-radius: 3px;
    min-height: 30px;
}
QScrollBar::handle:vertical:hover { background: #94a3b8; }
QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical { height: 0; }

QScrollBar:horizontal {
    height: 6px;
    background: transparent;
}
QScrollBar::handle:horizontal {
    background: #cbd5e1;
    border-radius: 3px;
    min-width: 30px;
}
QScrollBar::handle:horizontal:hover { background: #94a3b8; }
QScrollBar::add-line:horizontal, QScrollBar::sub-line:horizontal { width: 0; }

/* ===== 标签 ===== */
QLabel#title {
    font-size: 20px;
    font-weight: bold;
    color: #0f172a;
}
QLabel#subtitle {
    font-size: 13px;
    color: #94a3b8;
}
QLabel#sectionTitle {
    font-size: 16px;
    font-weight: bold;
    color: #1e293b;
    padding: 12px 0 8px 0;
}

/* ===== 分割线 ===== */
QFrame#divider {
    max-height: 1px;
    background: #e2e8f0;
    border: none;
}

/* ===== Tab ===== */
QTabWidget::pane {
    border: none;
    background: transparent;
}
QTabBar::tab {
    padding: 10px 24px;
    font-size: 14px;
    min-width: 80px;
    border: none;
    color: #64748b;
}
QTabBar::tab:selected {
    color: #07c160;
    font-weight: bold;
    border-bottom: 2px solid #07c160;
}
QTabBar::tab:hover:!selected {
    color: #07c160;
    background: #f0fdf4;
}

/* ===== 对话框 ===== */
QDialog { background: #f8fafc; }

/* ===== 分组 ===== */
QGroupBox {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    margin-top: 12px;
    padding: 16px 12px 12px;
    font-weight: bold;
    font-size: 14px;
}
QGroupBox::title {
    subcontrol-origin: margin;
    left: 14px;
    padding: 0 6px;
    color: #07c160;
}

/* ===== Tooltip ===== */
QToolTip {
    background: #1e293b;
    color: white;
    border: none;
    padding: 8px 12px;
    border-radius: 6px;
    font-size: 12px;
}

/* ===== 消息弹窗 ===== */
QMessageBox {
    background: white;
}
QMessageBox QLabel {
    font-size: 14px;
    color: #1e293b;
    min-width: 200px;
}
QMessageBox QPushButton {
    min-width: 80px;
    padding: 8px 20px;
    background: #07c160;
    color: white;
    border: none;
    border-radius: 6px;
    font-size: 13px;
    font-weight: bold;
}
QMessageBox QPushButton:hover { background: #06ad56; }

/* ===== 进度条 ===== */
QProgressBar {
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    text-align: center;
    background: #f1f5f9;
    height: 20px;
}
QProgressBar::chunk {
    background: #07c160;
    border-radius: 5px;
}

/* ===== 卡片容器（白色背景圆角卡片） ===== */
QFrame#card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
}
QFrame#card:hover {
    border-color: #07c160;
}
"""
