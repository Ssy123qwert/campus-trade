"""
发布商品弹窗
表单：标题、描述、价格、原价、分类、成色、图片路径
"""
from PyQt5.QtWidgets import (QDialog, QVBoxLayout, QHBoxLayout,
                             QLabel, QLineEdit, QTextEdit, QPushButton,
                             QComboBox, QMessageBox, QFrame)
from PyQt5.QtCore import Qt, pyqtSignal
from PyQt5.QtGui import QFont
import json

from ..api.client import api
from ..components.toast import Toast


class PublishDialog(QDialog):
    """发布商品弹窗"""

    product_published = pyqtSignal()

    CONDITION_MAP = {"全新": 1, "九成新": 2, "七成新": 3, "有瑕疵": 4}

    def __init__(self, user_id: int, parent=None):
        super().__init__(parent)
        self.user_id = user_id
        self.setWindowTitle("发布商品")
        self.setFixedSize(500, 600)
        self.setStyleSheet("background: white;")
        self._setup_ui()

    def _setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(24, 20, 24, 20)
        layout.setSpacing(14)

        # 标题
        title = QLabel("发布新商品")
        title.setFont(QFont("微软雅黑", 20, QFont.Bold))
        title.setStyleSheet("color: #333;")
        layout.addWidget(title)

        # 分割线
        line = QFrame()
        line.setFrameShape(QFrame.HLine)
        line.setStyleSheet("color: #eee;")
        layout.addWidget(line)

        layout.addSpacing(8)

        # ── 表单 ──
        self.input_title = QLineEdit()
        self.input_title.setPlaceholderText("商品标题 *")
        self._style_input(self.input_title)
        layout.addWidget(self.input_title)

        self.input_price = QLineEdit()
        self.input_price.setPlaceholderText("售价 *（如: 25.00）")
        self._style_input(self.input_price)
        layout.addWidget(self.input_price)

        self.input_original = QLineEdit()
        self.input_original.setPlaceholderText("原价（可选）")
        self._style_input(self.input_original)
        layout.addWidget(self.input_original)

        # 分类
        self.combo_category = QComboBox()
        self.combo_category.addItems([
            "数码电子", "书籍教材", "生活用品",
            "服饰鞋包", "运动户外", "其他"
        ])
        self.combo_category.setStyleSheet(self._combo_style())
        layout.addWidget(self.combo_category)

        # 成色
        self.combo_condition = QComboBox()
        self.combo_condition.addItems(list(self.CONDITION_MAP.keys()))
        self.combo_condition.setStyleSheet(self._combo_style())
        layout.addWidget(self.combo_condition)

        self.input_images = QLineEdit()
        self.input_images.setPlaceholderText("图片路径（可选，如: /images/xxx.jpg）")
        self._style_input(self.input_images)
        layout.addWidget(self.input_images)

        self.input_video = QLineEdit()
        self.input_video.setPlaceholderText("视频链接（可选，支持 mp4 格式）")
        self._style_input(self.input_video)
        layout.addWidget(self.input_video)

        # 描述
        self.input_desc = QTextEdit()
        self.input_desc.setPlaceholderText("商品描述 *")
        self.input_desc.setFixedHeight(100)
        self.input_desc.setStyleSheet("""
            padding: 10px; border: 1px solid #ddd;
            border-radius: 8px; font-size: 14px;
        """)
        layout.addWidget(self.input_desc)

        layout.addStretch()

        # ── 按钮 ──
        btn_layout = QHBoxLayout()
        btn_cancel = QPushButton("取消")
        btn_cancel.setStyleSheet(self._btn_secondary())
        btn_cancel.clicked.connect(self.reject)
        btn_layout.addWidget(btn_cancel)

        btn_submit = QPushButton("发布")
        btn_submit.setStyleSheet(self._btn_primary())
        btn_submit.clicked.connect(self._do_publish)
        btn_layout.addWidget(btn_submit)

        layout.addLayout(btn_layout)

    def _do_publish(self):
        """提交发布"""
        title = self.input_title.text().strip()
        price_text = self.input_price.text().strip()
        desc = self.input_desc.toPlainText().strip()

        if not title or not price_text or not desc:
            QMessageBox.warning(self, "提示", "请填写标题、售价和描述")
            return

        try:
            price = float(price_text)
        except ValueError:
            QMessageBox.warning(self, "提示", "价格格式不正确")
            return

        orig_text = self.input_original.text().strip()
        original_price = float(orig_text) if orig_text else None

        video_url = self.input_video.text().strip()
        data = {
            "userId": self.user_id,
            "title": title,
            "description": desc,
            "price": price,
            "category": self.combo_category.currentText(),
            "condition": self.CONDITION_MAP[self.combo_condition.currentText()],
            "images": json.dumps([self.input_images.text().strip()]) if self.input_images.text().strip() else "[]",
        }
        if video_url:
            data["videoUrl"] = video_url
        if original_price:
            data["originalPrice"] = original_price

        result = api.publish_product(data)
        if result.get("code") == 200:
            Toast.show(self, "发布成功！🎉", 2000)
            self.product_published.emit()
            self.accept()
        else:
            QMessageBox.warning(self, "发布失败",
                                result.get("msg", "请稍后重试"))

    def _style_input(self, widget):
        widget.setStyleSheet("""
            padding: 10px 14px; border: 1px solid #ddd;
            border-radius: 8px; font-size: 14px;
        """)

    def _combo_style(self) -> str:
        return """
            padding: 10px; border: 1px solid #ddd;
            border-radius: 8px; font-size: 14px;
        """

    def _btn_primary(self) -> str:
        return """
            QPushButton {
                padding: 12px; background: #07c160; color: white;
                border: none; border-radius: 8px; font-size: 15px;
                font-weight: bold; min-width: 120px;
            }
            QPushButton:hover { background: #06ad56; }
        """

    def _btn_secondary(self) -> str:
        return """
            QPushButton {
                padding: 12px; background: #f5f5f5; color: #666;
                border: 1px solid #ddd; border-radius: 8px;
                font-size: 15px; min-width: 120px;
            }
            QPushButton:hover { background: #eee; }
        """
