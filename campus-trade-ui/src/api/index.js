const BASE = '/api'

export const request = async (url, options = {}) => {
  const token = localStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = token

  const res = await fetch(BASE + url, { ...options, headers })
  const data = await res.json()
  return data
}

export const api = {
  // 用户
  login: (data) => request('/user/login', { method: 'POST', body: JSON.stringify(data) }),
  register: (data) => request('/user/register', { method: 'POST', body: JSON.stringify(data) }),
  getUserInfo: (userId) => request(`/user/info?userId=${userId}`),
  updateUser: (data) => request('/user/update', { method: 'PUT', body: JSON.stringify(data) }),

  // 商品
  getProducts: (data) => request('/product/list', { method: 'POST', body: JSON.stringify(data) }),
  getProductDetail: (id) => request(`/product/detail?id=${id}`),
  publishProduct: (data) => request('/product/publish', { method: 'POST', body: JSON.stringify(data) }),
  getMyProducts: (userId) => request(`/product/my?userId=${userId}`),
  updateProduct: (data) => request('/product/update', { method: 'PUT', body: JSON.stringify(data) }),
  offlineProduct: (id) => request(`/product/offline?id=${id}`, { method: 'PUT' }),
  getCategories: () => request('/product/categories'),

  // 订单
  createOrder: (productId, buyerId) => request(`/order/create?productId=${productId}&buyerId=${buyerId}`, { method: 'POST' }),
  payOrder: (orderId, userId) => request(`/order/pay?orderId=${orderId}&userId=${userId}`, { method: 'POST' }),
  getMyOrders: (userId, type = 'buyer') => request(`/order/my?userId=${userId}&type=${type}`),
  updateOrderStatus: (id, status) => request(`/order/status?id=${id}&status=${status}`, { method: 'PUT' }),
  checkPendingOrder: (productId, buyerId) => request(`/order/check?productId=${productId}&buyerId=${buyerId}`),

  // 消息
  sendMessage: (fromUserId, toUserId, productId, content) => request(`/message/send?fromUserId=${fromUserId}&toUserId=${toUserId}&productId=${productId}&content=${encodeURIComponent(content)}`, { method: 'POST' }),
  getConversation: (userId, otherUserId, productId) => request(`/message/conversation?userId=${userId}&otherUserId=${otherUserId}&productId=${productId}`),
  getConversationList: (userId) => request(`/message/list?userId=${userId}`),
  getUnreadCount: (userId) => request(`/message/unread?userId=${userId}`),
  markAsRead: (fromUserId, toUserId, productId) => request(`/message/read?fromUserId=${fromUserId}&toUserId=${toUserId}&productId=${productId}`, { method: 'PUT' }),

  // 收藏
  toggleFavorite: (userId, productId) => request(`/favorite/toggle?userId=${userId}&productId=${productId}`, { method: 'POST' }),
  getMyFavorites: (userId) => request(`/favorite/my?userId=${userId}`),
  checkFavorite: (userId, productId) => request(`/favorite/check?userId=${userId}&productId=${productId}`),

  // 消息
  sendMessage: (fromUserId, toUserId, productId, content) => request('/message/send', { method: 'POST', body: JSON.stringify({ fromUserId, toUserId, productId, content }) }),
  getConversation: (userId, otherUserId, productId) => request(`/message/conversation?userId=${userId}&otherUserId=${otherUserId}&productId=${productId}`),
  getConversationList: (userId) => request(`/message/conversations?userId=${userId}`),
  getUnreadCount: (userId) => request(`/message/unread?userId=${userId}`),
  markAsRead: (userId, otherUserId) => request(`/message/read?userId=${userId}&otherUserId=${otherUserId}`, { method: 'PUT' }),

  // 支付
  payOrder: (orderId, userId) => request(`/order/pay?orderId=${orderId}&userId=${userId}`, { method: 'POST' }),
  checkPendingOrder: (productId, buyerId) => request(`/order/check?productId=${productId}&buyerId=${buyerId}`),
}
