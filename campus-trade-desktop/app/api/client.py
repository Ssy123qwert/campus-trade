"""
API 客户端封装
所有后端接口调用统一走这里，避免重复写 HTTP 请求
"""
import requests


class ApiClient:
    """封装所有后端 API 调用，提供统一的超时/错误处理"""

    BASE_URL = "http://localhost:8080"
    TIMEOUT = 10  # 普通接口超时（秒）
    AI_TIMEOUT = 40  # AI 接口超时（秒，DeepSeek 有时较慢）

    def __init__(self):
        self.session = requests.Session()
        self.token = None  # 登录后保存 token
        self.user_id = None  # 登录后保存用户ID
        self.user_info = {}   # 登录后保存用户信息

    # ─────────── 通用请求方法 ───────────

    def _headers(self) -> dict:
        """构建请求头，带 token 则自动添加"""
        h = {"Content-Type": "application/json"}
        if self.token:
            h["Authorization"] = self.token
        return h

    def get(self, path: str, params: dict = None, timeout: int = None):
        """GET 请求"""
        url = self.BASE_URL + path
        resp = self.session.get(url, headers=self._headers(),
                                params=params, timeout=timeout or self.TIMEOUT)
        return resp.json()

    def post(self, path: str, data: dict = None, timeout: int = None):
        """POST 请求"""
        url = self.BASE_URL + path
        resp = self.session.post(url, headers=self._headers(),
                                 json=data, timeout=timeout or self.TIMEOUT)
        return resp.json()

    def put(self, path: str, data: dict = None, timeout: int = None):
        """PUT 请求"""
        url = self.BASE_URL + path
        resp = self.session.put(url, headers=self._headers(),
                                json=data, timeout=timeout or self.TIMEOUT)
        return resp.json()

    def delete(self, path: str, timeout: int = None):
        """DELETE 请求"""
        url = self.BASE_URL + path
        resp = self.session.delete(url, headers=self._headers(),
                                   timeout=timeout or self.TIMEOUT)
        return resp.json()

    # ─────────── 用户模块 ───────────

    def login(self, account: str, password: str) -> dict:
        """登录，成功后保存 token 和用户信息"""
        result = self.post("/api/user/login", {
            "username": account,
            "password": password
        })
        if result.get("code") == 200 and result.get("data"):
            data = result["data"]
            self.token = data.get("token", "")
            self.user_id = int(self.token) if self.token.isdigit() else None
            self.user_info = data.get("user", {})
        return result

    def register(self, account: str, password: str, nickname: str) -> dict:
        """注册"""
        return self.post("/api/user/register", {
            "account": account,
            "password": password,
            "nickname": nickname
        })

    def get_user_info(self, user_id: int) -> dict:
        """获取用户信息"""
        return self.get(f"/api/user/info", {"id": user_id})

    def update_user(self, data: dict) -> dict:
        """更新个人信息"""
        return self.put("/api/user/update", data)

    # ─────────── 商品模块 ───────────

    def get_products(self, page: int = 1, size: int = 20,
                     category: str = None, keyword: str = None) -> dict:
        """获取商品列表，支持分页/分类/搜索"""
        params = {"page": page, "size": size}
        if category:
            params["category"] = category
        if keyword:
            params["keyword"] = keyword
        return self.post("/api/product/list", params)

    def get_product_detail(self, product_id: int) -> dict:
        """获取商品详情"""
        return self.get("/api/product/detail", {"id": product_id})

    def get_my_products(self, user_id: int) -> dict:
        """获取我的发布"""
        return self.get("/api/product/my", {"userId": user_id})

    def publish_product(self, data: dict) -> dict:
        """发布商品"""
        return self.post("/api/product/publish", data)

    def update_product(self, data: dict) -> dict:
        """更新商品"""
        return self.put("/api/product/update", data)

    def offline_product(self, product_id: int) -> dict:
        """下架商品"""
        return self.put("/api/product/offline", {"id": product_id})

    def get_categories(self) -> dict:
        """获取分类列表"""
        return self.get("/api/product/categories")

    # ─────────── 订单模块 ───────────

    def create_order(self, product_id: int, buyer_id: int) -> dict:
        """创建订单"""
        return self.post("/api/order/create", {
            "productId": product_id,
            "buyerId": buyer_id
        })

    def get_my_orders(self, user_id: int, role: str = "buy") -> dict:
        """获取我的订单（buy=买的，sell=卖的）"""
        return self.get("/api/order/my", {"userId": user_id, "role": role})

    def update_order_status(self, order_id: int, status: int) -> dict:
        """更新订单状态（支付/发货/收货）"""
        return self.put("/api/order/status", {
            "id": order_id,
            "status": status
        })

    # ─────────── 收藏模块 ───────────

    def toggle_favorite(self, user_id: int, product_id: int) -> dict:
        """收藏/取消收藏"""
        return self.post("/api/favorite/toggle", {
            "userId": user_id,
            "productId": product_id
        })

    def get_favorites(self, user_id: int) -> dict:
        """获取收藏列表"""
        return self.get("/api/favorite/list", {"userId": user_id})

    # ─────────── 消息模块 ───────────

    def send_message(self, from_id: int, to_id: int,
                     product_id: int, content: str) -> dict:
        """发送私信"""
        return self.post("/api/message/send", {
            "fromId": from_id,
            "toId": to_id,
            "productId": product_id,
            "content": content
        })

    def get_messages(self, user_id: int, other_id: int,
                     product_id: int = None) -> dict:
        """获取与某人的聊天记录"""
        params = {"userId": user_id, "otherId": other_id}
        if product_id:
            params["productId"] = product_id
        return self.get("/api/message/list", params)

    def get_conversations(self, user_id: int) -> dict:
        """获取会话列表"""
        return self.get("/api/message/conversations", {"userId": user_id})

    # ─────────── AI 模块 ───────────

    def ai_chat(self, question: str) -> dict:
        """AI 对话"""
        return self.post("/api/ai/chat", {"question": question},
                         timeout=self.AI_TIMEOUT)

    # ─────────── 公告模块 ───────────

    def get_announcements(self) -> dict:
        """获取公告列表"""
        return self.get("/api/announcement/list")


# 全局单例，所有窗口共用
api = ApiClient()
