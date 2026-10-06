import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import Icon from './Icon'

export default function AppHeader() {
  const navigate = useNavigate()
  const { logout, user } = useAuth()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <>
    <a className="skip-link" href="#main-content">Bỏ qua điều hướng</a>
    <header className="topbar">
      <NavLink className="brand" to="/chat"><span className="brand-mark"><Icon name="chat" size={19}/></span><strong>ChatApp</strong></NavLink>
      <nav className="main-nav" aria-label="Điều hướng chính">
        <NavLink to="/chat"><Icon name="chat"/><span>Trò chuyện</span></NavLink>
        <NavLink to="/users"><Icon name="search"/><span>Tìm người</span></NavLink>
        <NavLink to="/profile"><Icon name="user"/><span>Hồ sơ</span></NavLink>
        {user?.role === 'ADMIN' && <NavLink to="/admin"><Icon name="shield"/><span>Quản trị</span></NavLink>}
      </nav>
      <div className="topbar-account"><span className="mini-avatar">{(user?.displayName || user?.username || '?').charAt(0).toUpperCase()}</span><span className="topbar-user"><strong>{user?.displayName}</strong><small>@{user?.username}</small></span><button className="icon-button" aria-label="Đăng xuất" title="Đăng xuất" onClick={handleLogout}><Icon name="logout"/></button></div>
    </header>
    </>
  )
}
