import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

export default function AdminRoute({ children }) {
  const { user } = useAuth()
  return user?.role === 'ADMIN' ? children : <Navigate to="/chat" replace />
}
