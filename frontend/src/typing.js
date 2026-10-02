export function createTypingController(send, options = {}) {
  const idleMs = options.idleMs ?? 2000
  const refreshMs = options.refreshMs ?? 3000
  const now = options.now ?? Date.now
  const schedule = options.schedule ?? setTimeout
  const cancel = options.cancel ?? clearTimeout
  let active = false
  let lastStartedAt = 0
  let idleTimer = null

  const stop = () => {
    if (idleTimer) cancel(idleTimer)
    idleTimer = null
    if (!active) return
    active = false
    send(false)
  }

  const input = value => {
    if (!value.trim()) return stop()
    const timestamp = now()
    if (!active || timestamp - lastStartedAt >= refreshMs) {
      active = true
      lastStartedAt = timestamp
      send(true)
    }
    if (idleTimer) cancel(idleTimer)
    idleTimer = schedule(stop, idleMs)
  }

  return { input, stop, dispose: stop }
}

export function friendlyLastSeen(value, now = Date.now()) {
  if (!value) return 'Offline'
  const seconds = Math.max(0, Math.floor((now - Date.parse(value)) / 1000))
  if (seconds < 60) return 'Hoạt động vừa xong'
  if (seconds < 3600) return `Hoạt động ${Math.floor(seconds / 60)} phút trước`
  if (seconds < 86400) return `Hoạt động ${Math.floor(seconds / 3600)} giờ trước`
  if (seconds < 604800) return `Hoạt động ${Math.floor(seconds / 86400)} ngày trước`
  return `Hoạt động ${new Date(value).toLocaleDateString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}`
}
