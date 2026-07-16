const BASE = import.meta.env.VITE_API_BASE || "/api"

/**
 * 统一请求封装
 * - 自动附带 JWT Token（Authorization: Bearer xxx）
 * - 401 时自动用 Refresh Token 刷新
 * - 刷新失败则跳转登录页
 */
export const request = async (url, options = {}) => {
  const accessToken = localStorage.getItem("accessToken")
  const headers = { ...options.headers }
  if (!(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json"
  }
  if (accessToken) headers["Authorization"] = "Bearer " + accessToken

  const res = await fetch(BASE + url, { ...options, headers })

  // Token 过期 → 尝试刷新
  if (res.status === 401) {
    const refreshToken = localStorage.getItem("refreshToken")
    if (refreshToken) {
      const refreshRes = await fetch(BASE + "/user/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken })
      })
      const refreshData = await refreshRes.json()
      if (refreshData.code === 200 && refreshData.data) {
        // 刷新成功，重试原请求
        localStorage.setItem("accessToken", refreshData.data.accessToken)
        localStorage.setItem("refreshToken", refreshData.data.refreshToken)
        headers["Authorization"] = "Bearer " + refreshData.data.accessToken
        const retryRes = await fetch(BASE + url, { ...options, headers })
        const retryData = await retryRes.json()
        return retryData
      }
    }
    // 刷新失败 → 跳转登录
    localStorage.removeItem("accessToken")
    localStorage.removeItem("refreshToken")
    localStorage.removeItem("user")
    window.location.hash = "#/login"
    return { code: 401, msg: "登录已过期，请重新登录" }
  }

  const data = await res.json()
  return data
}

/**
 * 获取当前登录用户信息（从 localStorage 缓存）
 */
export const getCurrentUser = () => {
  const userStr = localStorage.getItem("user")
  return userStr ? JSON.parse(userStr) : null
}

export const api = {
  // ===== 用户认证 =====
  login: (data) => request("/user/login", { method: "POST", body: JSON.stringify(data) }),
  register: (data) => request("/user/register", { method: "POST", body: JSON.stringify(data) }),
  refresh: (refreshToken) => request("/user/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken })
  }),
  logout: () => request("/user/logout", { method: "POST" }),
  getUserInfo: () => request("/user/info"),
  getPublicUserInfo: (userId) => request("/user/public/" + userId),
  updateUser: (data) => request("/user/update", { method: "PUT", body: JSON.stringify(data) }),

  // ===== 商品 =====
  getProducts: (data) => request("/product/list", { method: "POST", body: JSON.stringify(data) }),
  getProductDetail: (id) => request("/product/detail?id=" + id),
  publishProduct: (data) => request("/product/publish", { method: "POST", body: JSON.stringify(data) }),
  getMyProducts: () => request("/product/my"),
  updateProduct: (data) => request("/product/update", { method: "PUT", body: JSON.stringify(data) }),
  offlineProduct: (id) => request("/product/offline?id=" + id, { method: "PUT" }),
  relistProduct: (id) => request("/product/relist?id=" + id, { method: "PUT" }),
  getCategories: () => request("/product/categories"),

  // ===== 订单 =====
  createOrder: (productId) => request("/order/create?productId=" + productId, { method: "POST" }),
  payOrder: (orderId) => request("/order/pay?orderId=" + orderId, { method: "POST" }),
  getMyOrders: (type = "buyer") => request("/order/my?type=" + type),
  ship: (orderId) => request("/order/ship?orderId=" + orderId, { method: "POST" }),
  confirm: (orderId) => request("/order/confirm?orderId=" + orderId, { method: "POST" }),
  checkPendingOrder: (productId) => request("/order/check?productId=" + productId),

  // ===== 消息 =====
  sendMessage: (toUserId, productId, content) =>
    request("/message/send?toUserId=" + toUserId + "&productId=" + productId + "&content=" + encodeURIComponent(content), { method: "POST" }),
  getConversation: (otherUserId, productId) =>
    request("/message/conversation?otherUserId=" + otherUserId + "&productId=" + productId),
  getConversationList: () => request("/message/list"),
  getUnreadCount: () => request("/message/unread"),
  markAsRead: (fromUserId, toUserId, productId) =>
    request("/message/read?fromUserId=" + fromUserId + "&toUserId=" + toUserId + "&productId=" + productId, { method: "PUT" }),

  // ===== 文件 =====
  uploadImages: (formData) => request("/file/upload", { method: "POST", body: formData }),

  // ===== 通知 =====
  getUnreadNotif: () => request("/notification/unread"),
  getNotifList: () => request("/notification/list"),
  markNotifRead: (id) => request("/notification/read/" + id, { method: "PUT" }),
  markAllNotifRead: () => request("/notification/read-all", { method: "PUT" }),

  // ===== 公告 =====
  getAnnouncement: () => request("/announcement/latest"),
  getAnnouncements: () => request("/announcement/list"),
  saveAnnouncement: (data) => request("/announcement/save?content=" + encodeURIComponent(data.content), { method: "POST" }),
  updateAnnouncement: (data) => request("/announcement/update?id=" + data.id + "&content=" + encodeURIComponent(data.content), { method: "PUT" }),
  deleteAnnouncement: (id) => request("/announcement/delete?id=" + id, { method: "DELETE" }),

  // ===== 管理后台 =====
  checkAdmin: () => request("/admin/check"),
  getAdminUsers: (params) => request("/admin/users?page=" + params.page + "&size=" + params.size + (params.keyword ? "&keyword=" + encodeURIComponent(params.keyword) : "")),
  deleteAdminUser: (userId) => request("/admin/user?userId=" + userId, { method: "DELETE" }),
  getAdminProducts: (params) => request("/admin/products?page=" + params.page + "&size=" + params.size + (params.keyword ? "&keyword=" + encodeURIComponent(params.keyword) : "")),
  deleteAdminProduct: (productId) => request("/admin/product?productId=" + productId, { method: "DELETE" }),
  updateProductStatus: (productId, status) => request("/admin/product/status?productId=" + productId + "&status=" + status, { method: "PUT" }),

  // ===== 议价 =====
  createOffer: (productId, price) => request("/offer/create?productId=" + productId + "&price=" + price, { method: "POST" }),
  getMyOffers: () => request("/offer/my"),
  getReceivedOffers: () => request("/offer/received"),
  acceptOffer: (id) => request("/offer/accept/" + id, { method: "POST" }),
  rejectOffer: (id) => request("/offer/reject/" + id, { method: "POST" }),

  // ===== 相似推荐 =====
  getSimilar: (id) => request("/product/similar?id=" + id),

  // ===== 收藏 =====
  toggleFavorite: (productId) => request("/favorite/toggle?productId=" + productId, { method: "POST" }),
  getMyFavorites: () => request("/favorite/my"),
  checkFavorite: (productId) => request("/favorite/check?productId=" + productId),

  // ===== 评价 =====
  createReview: (data) => request("/review/create", { method: "POST", body: JSON.stringify(data) }),
  getUserReviews: (userId, page = 1) => request("/review/user/" + userId + "?page=" + page + "&size=10"),
  getReviewRate: (userId) => request("/review/rate/" + userId),

  // ===== 统计看板 =====
  getStatsOverview: () => request("/admin/statistics/overview"),
  getUserTrend: (days) => request("/admin/statistics/user-trend?days=" + days),
  getOrderTrend: (days) => request("/admin/statistics/order-trend?days=" + days),
  getCategoryDistribution: () => request("/admin/statistics/category-distribution"),
  getHotProducts: () => request("/admin/statistics/hot-products?limit=10"),
  getOperationLogs: (page = 1, size = 10) => request(`/admin/statistics/operation-logs?page=${page}&size=${size}`),

  // ===== 个人统计 =====
  getProfileStats: () => request("/user/profile-stats"),
}
