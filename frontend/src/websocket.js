import { Client } from '@stomp/stompjs'

export const WebSocketStatus = Object.freeze({
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  DISCONNECTED: 'DISCONNECTED',
})

export function reconnectDelayForAttempt(attempt) {
  return Math.min(1000 * (2 ** Math.max(0, attempt - 1)), 30000)
}

export function resolveWebsocketUrl(configuredWsUrl, apiUrl, browserOrigin) {
  if (configuredWsUrl) return configuredWsUrl
  const base = apiUrl || 'http://localhost:8080/api'
  if (/^https?:\/\//i.test(base)) return base.replace(/^http/i, 'ws').replace(/\/api\/?$/, '') + '/ws'
  const origin = browserOrigin || 'http://localhost:8080'
  return origin.replace(/^http/i, 'ws').replace(/\/$/, '') + base.replace(/\/api\/?$/, '') + '/ws'
}

function websocketUrl() {
  return resolveWebsocketUrl(import.meta.env.VITE_WS_URL, import.meta.env.VITE_API_URL, window.location.origin)
}

class WebSocketService {
  client = null
  token = null
  status = WebSocketStatus.DISCONNECTED
  listeners = new Set()
  destinations = new Map()
  reconnectAttempts = 0

  connect(token) {
    if (!token) return
    if (this.client?.active && this.token === token) return
    this.disconnect()
    this.token = token
    this.setStatus(WebSocketStatus.CONNECTING)
    const client = new Client({
      brokerURL: websocketUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 1000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      onConnect: () => {
        this.reconnectAttempts = 0
        client.reconnectDelay = 1000
        this.setStatus(WebSocketStatus.CONNECTED)
        this.destinations.forEach(entry => this.bindDestination(entry))
      },
      onDisconnect: () => this.setStatus(WebSocketStatus.DISCONNECTED),
      onWebSocketClose: () => {
        if (!client.active) return this.setStatus(WebSocketStatus.DISCONNECTED)
        this.reconnectAttempts += 1
        if (this.reconnectAttempts >= 12) {
          client.reconnectDelay = 0
          client.deactivate()
          return this.setStatus(WebSocketStatus.DISCONNECTED)
        }
        client.reconnectDelay = reconnectDelayForAttempt(this.reconnectAttempts)
        this.setStatus(WebSocketStatus.CONNECTING)
      },
      onStompError: () => {
        this.setStatus(WebSocketStatus.DISCONNECTED)
        client.deactivate()
      },
    })
    this.client = client
    client.activate()
  }

  disconnect() {
    const client = this.client
    this.client = null
    this.token = null
    this.reconnectAttempts = 0
    this.destinations.forEach(entry => {
      entry.subscription?.unsubscribe()
      entry.subscription = null
    })
    this.setStatus(WebSocketStatus.DISCONNECTED)
    if (client?.active) client.deactivate()
  }

  subscribe(listener) {
    this.listeners.add(listener)
    listener(this.status)
    return () => this.listeners.delete(listener)
  }

  subscribeDestination(destination, callback) {
    const id = crypto.randomUUID()
    const entry = { destination, callback, subscription: null }
    this.destinations.set(id, entry)
    if (this.status === WebSocketStatus.CONNECTED) this.bindDestination(entry)
    return () => {
      entry.subscription?.unsubscribe()
      this.destinations.delete(id)
    }
  }

  bindDestination(entry) {
    entry.subscription?.unsubscribe()
    entry.subscription = this.client?.subscribe(entry.destination, frame => {
      try { entry.callback(JSON.parse(frame.body)) } catch { /* Ignore malformed server frames. */ }
    }) || null
  }

  publish(destination, body) {
    if (this.status !== WebSocketStatus.CONNECTED || !this.client?.connected) {
      throw new Error('WebSocket is not connected')
    }
    this.client.publish({ destination, body: JSON.stringify(body) })
  }

  isConnected() {
    return this.status === WebSocketStatus.CONNECTED && Boolean(this.client?.connected)
  }

  setStatus(status) {
    if (this.status === status) return
    this.status = status
    this.listeners.forEach(listener => listener(status))
  }
}

export const webSocketService = new WebSocketService()
