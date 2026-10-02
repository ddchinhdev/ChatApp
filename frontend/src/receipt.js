export function promoteReceipt(current = 'SENT', next = 'SENT') {
  const rank = { SENT: 0, DELIVERED: 1, READ: 2 }
  return rank[next] > rank[current] ? next : current
}

export function sumUnread(conversations) {
  return conversations.reduce((total, conversation) => total + (conversation.unreadCount || 0), 0)
}
