import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { webSocketService, WebSocketStatus } from '../websocket'

const AuthContext = createContext(null)

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('chatapp_user'))
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem('chatapp_token'))
  const [user, setUser] = useState(readStoredUser)
  const [webSocketStatus, setWebSocketStatus] = useState(WebSocketStatus.DISCONNECTED)

  const login = ({ accessToken: token, user: authenticatedUser }) => {
    localStorage.setItem('chatapp_token', token)
    localStorage.setItem('chatapp_user', JSON.stringify(authenticatedUser))
    setAccessToken(token)
    setUser(authenticatedUser)
  }

  const logout = () => {
    webSocketService.disconnect()
    localStorage.removeItem('chatapp_token')
    localStorage.removeItem('chatapp_user')
    setAccessToken(null)
    setUser(null)
  }

  const updateUser = (updatedUser) => {
    localStorage.setItem('chatapp_user', JSON.stringify(updatedUser))
    setUser(updatedUser)
  }

  useEffect(() => {
    window.addEventListener('auth:unauthorized', logout)
    return () => window.removeEventListener('auth:unauthorized', logout)
  }, [])

  useEffect(() => webSocketService.subscribe(setWebSocketStatus), [])

  useEffect(() => {
    if (accessToken) webSocketService.connect(accessToken)
    else webSocketService.disconnect()
    return () => webSocketService.disconnect()
  }, [accessToken])

  const value = useMemo(() => ({
    accessToken,
    user,
    isAuthenticated: Boolean(accessToken),
    login,
    logout,
    updateUser,
    webSocketStatus,
  }), [accessToken, user, webSocketStatus])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
