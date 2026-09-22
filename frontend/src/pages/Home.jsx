import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'

export default function Home() {
  const navigate = useNavigate()
  const [user, setUser] = useState(null)
  const [displayName, setDisplayName] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    api.get('/users/me')
      .then(({ data }) => {
        setUser(data)
        setDisplayName(data.displayName)
      })
      .catch(() => {
        localStorage.removeItem('chatapp_token')
        navigate('/login')
      })
  }, [navigate])

  const updateProfile = async (e) => {
    e.preventDefault()
    setMessage('')
    try {
      const { data } = await api.put('/users/me', { displayName })
      setUser(data)
      localStorage.setItem('chatapp_user', JSON.stringify(data))
      setMessage('Cập nhật thành công.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'Cập nhật thất bại.')
    }
  }

  const logout = () => {
    localStorage.removeItem('chatapp_token')
    localStorage.removeItem('chatapp_user')
    navigate('/login')
  }

  if (!user) return <div className="loading">Đang tải...</div>

  return (
    <div className="home-page">
      <header className="topbar">
        <strong>ChatApp</strong>
        <button className="secondary" onClick={logout}>Đăng xuất</button>
      </header>

      <main className="home-content">
        <section className="welcome-card">
          <h2>Xin chào, {user.displayName}</h2>
          <p>Tuần 2 đã hoàn thành: Authentication + User Profile.</p>
          <p className="muted">
            WebSocket và chức năng chat realtime sẽ được triển khai từ tuần 4.
          </p>
        </section>

        <form className="profile-card" onSubmit={updateProfile}>
          <h3>Thông tin tài khoản</h3>
          <p><b>Username:</b> {user.username}</p>
          <p><b>Email:</b> {user.email}</p>
          <p><b>Role:</b> {user.role}</p>

          <label>Display name</label>
          <input
            value={displayName}
            onChange={e => setDisplayName(e.target.value)}
          />

          {message && <div className="success">{message}</div>}

          <button>Cập nhật</button>
        </form>
      </main>
    </div>
  )
}
