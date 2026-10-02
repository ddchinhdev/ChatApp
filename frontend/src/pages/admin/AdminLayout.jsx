import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import Icon from '../../components/Icon'

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  return <div className="admin-shell">
    <aside className="admin-sidebar"><h1><span className="brand-mark"><Icon name="shield" size={18}/></span>ChatApp Admin</h1><div className="admin-identity">{user.displayName}<small>@{user.username}</small></div><nav>
      <NavLink end to="/admin"><Icon name="dashboard" size={18}/>Tổng quan</NavLink><NavLink to="/admin/users"><Icon name="users" size={18}/>Người dùng</NavLink><NavLink to="/admin/audit"><Icon name="activity" size={18}/>Audit log</NavLink><NavLink to="/chat"><Icon name="chat" size={18}/>Về ChatApp</NavLink>
    </nav><button className="secondary" onClick={() => { logout(); navigate('/login', { replace: true }) }}><Icon name="logout" size={18}/>Đăng xuất</button></aside>
    <main className="admin-content"><Outlet /></main>
  </div>
}
