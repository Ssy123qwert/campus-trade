"""
校园二手交易平台 - 桌面客户端
只负责打开原生窗口，后端由启动器 bat 管理
"""
import webview
import sys

# 等待后端就绪
import urllib.request, time
for i in range(60):
    try:
        urllib.request.urlopen("http://localhost:8080/", timeout=2)
        print(f"后端就绪 ({i+1}s)")
        break
    except:
        time.sleep(1)

# 创建原生桌面窗口（无浏览器工具栏）
window = webview.create_window(
    title="校园二手交易平台",
    url="http://localhost:8080",
    width=1200,
    height=800,
    resizable=True,
    min_size=(800, 600),
    confirm_close=True,
)

webview.start(debug=False)
