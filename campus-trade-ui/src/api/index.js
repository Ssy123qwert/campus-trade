const BASE = import.meta.env.VITE_API_BASE || "/api"

export const request = async (url, options = {}) => {
  const token = localStorage.getItem("token")
  const headers = { ...options.headers }
  if (!(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json"
  }
  if (token) headers["Authorization"] = token
  const res = await fetch(BASE + url, { ...options, headers })
  const data = await res.json()
  return data
}

export const api = {
  login: (data) => request("/user/login", { method: "POST", body: JSON.stringify(data) }),
  register: (data) => request("/user/register", { method: "POST", body: JSON.stringify(data) }),
  getUserInfo: (userId) => request("/user/info?userId=" + userId),
  updateUser: (data) => request("/user/update", { method: "PUT", body: JSON.stringify(data) }),
  getProducts: (data) => request("/product/list", { method: "POST", body: JSON.stringify(data) }),
  getProductDetail: (id) => request("/product/detail?id=" + id),
  publishProduct: (data) => request("/product/publish", { method: "POST", body: JSON.stringify(data) }),
  getMyProducts: (userId) => request("/product/my?userId=" + userId),
  updateProduct: (data) => request("/product/update", { method: "PUT", body: JSON.stringify(data) }),
  offlineProduct: (id) => request("/product/offline?id=" + id, { method: "PUT" }),
  relistProduct: (id) => request("/product/relist?id=" + id, { method: "PUT" }),
  getCategories: () => request("/product/categories"),
  createOrder: (productId, buyerId) => request("/order/create?productId=" + productId + "&buyerId=" + buyerId, { method: "POST" }),
  payOrder: (orderId, userId) => request("/order/pay?orderId=" + orderId + "&userId=" + userId, { method: "POST" }),
  getMyOrders: (userId, type = "buyer") => request("/order/my?userId=" + userId + "&type=" + type),
  updateOrderStatus: (id, status, userId) => request("/order/status?id=" + id + "&status=" + status + "&userId=" + userId, { method: "PUT" }),
  checkPendingOrder: (productId, buyerId) => request("/order/check?productId=" + productId + "&buyerId=" + buyerId),
  sendMessage: (fromUserId, toUserId, productId, content) => request("/message/send?fromUserId=" + fromUserId + "&toUserId=" + toUserId + "&productId=" + productId + "&content=" + encodeURIComponent(content), { method: "POST" }),
  getConversation: (userId, otherUserId, productId) => request("/message/conversation?userId=" + userId + "&otherUserId=" + otherUserId + "&productId=" + productId),
  getConversationList: (userId) => request("/message/list?userId=" + userId),
  getUnreadCount: (userId) => request("/message/unread?userId=" + userId),
  markAsRead: (fromUserId, toUserId, productId) => request("/message/read?fromUserId=" + fromUserId + "&toUserId=" + toUserId + "&productId=" + productId, { method: "PUT" }),
  uploadImages: (formData) => request("/file/upload", { method: "POST", body: formData }),
  getAnnouncement: () => request("/announcement/latest"),
  getAnnouncements: () => request("/announcement/list"),
  saveAnnouncement: (data) => request("/announcement/save?content=" + encodeURIComponent(data.content), { method: "POST" }),
  updateAnnouncement: (data) => request("/announcement/update?id=" + data.id + "&content=" + encodeURIComponent(data.content), { method: "PUT" }),
  deleteAnnouncement: (id) => request("/announcement/delete?id=" + id, { method: "DELETE" }),
  checkAdmin: () => request("/admin/check"),
  getAdminUsers: (params) => request("/admin/users?page=" + params.page + "&size=" + params.size + (params.keyword ? "&keyword=" + encodeURIComponent(params.keyword) : "")),
  deleteAdminUser: (userId) => request("/admin/user?userId=" + userId, { method: "DELETE" }),
  getAdminProducts: (params) => request("/admin/products?page=" + params.page + "&size=" + params.size + (params.keyword ? "&keyword=" + encodeURIComponent(params.keyword) : "")),
  deleteAdminProduct: (productId) => request("/admin/product?productId=" + productId, { method: "DELETE" }),
  updateProductStatus: (productId, status) => request("/admin/product/status?productId=" + productId + "&status=" + status, { method: "PUT" }),
  toggleFavorite: (userId, productId) => request("/favorite/toggle?userId=" + userId + "&productId=" + productId, { method: "POST" }),
  getMyFavorites: (userId) => request("/favorite/my?userId=" + userId),
  checkFavorite: (userId, productId) => request("/favorite/check?userId=" + userId + "&productId=" + productId),
  createReview: (data) => request("/review/create", { method: "POST", body: JSON.stringify(data) }),
  getUserReviews: (userId, page = 1) => request("/review/user/" + userId + "?page=" + page + "&size=10"),
  getReviewRate: (userId) => request("/review/rate/" + userId),
}
