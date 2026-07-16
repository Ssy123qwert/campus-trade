"""
轻提示组件
在屏幕底部显示短暂的消息提醒
"""
from PyQt5.QtWidgets import QWidget, QLabel, QHBoxLayout
from PyQt5.QtCore import Qt, QTimer, QPropertyAnimation, QRect, QEasingCurve


class Toast(QWidget):
    """仿 Android Toast 的轻提示，自动消失"""

    _instance = None

    @classmethod
    def show(cls, parent, message: str, duration: int = 2000):
        """显示一条提示，duration=显示时长(毫秒)"""
        if cls._instance:
            cls._instance.close()
        cls._instance = Toast(parent, message, duration)

    def __init__(self, parent, message: str, duration: int):
        super().__init__(parent)
        self.setStyleSheet("""
            background: rgba(0,0,0,0.8);
            border-radius: 20px;
            padding: 12px 24px;
        """)
        self.setAttribute(Qt.WA_TransparentForMouseEvents, True)

        layout = QHBoxLayout(self)
        layout.setContentsMargins(20, 10, 20, 10)
        label = QLabel(message)
        label.setStyleSheet("color: white; font-size: 14px;")
        label.setAlignment(Qt.AlignCenter)
        layout.addWidget(label)

        self.adjustSize()
        # 居中显示在父窗口底部
        parent_rect = parent.rect()
        x = (parent_rect.width() - self.width()) // 2
        y = parent_rect.height() - self.height() - 60
        self.move(x, y)

        # 淡入动画
        self.anim = QPropertyAnimation(self, b"windowOpacity")
        self.anim.setDuration(300)
        self.anim.setStartValue(0)
        self.anim.setEndValue(1)
        self.anim.start()

        # 定时关闭
        QTimer.singleShot(duration, self._fade_out)

    def _fade_out(self):
        self.anim = QPropertyAnimation(self, b"windowOpacity")
        self.anim.setDuration(300)
        self.anim.setStartValue(1)
        self.anim.setEndValue(0)
        self.anim.finished.connect(self.close)
        self.anim.start()
