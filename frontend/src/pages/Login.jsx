import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'

export default function Login() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const { data } = await api.post('/auth/login', form)
      localStorage.setItem('chatapp_token', data.token)
      localStorage.setItem('chatapp_user', JSON.stringify(data.user))
      navigate('/chat')
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng nhập thất bại')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1>ChatApp</h1>
        <p className="subtitle">Đăng nhập</p>

        <label>Username</label>
        <input
          value={form.username}
          onChange={e => setForm({ ...form, username: e.target.value })}
          placeholder="Nhập username"
        />

        <label>Password</label>
        <input
          type="password"
          value={form.password}
          onChange={e => setForm({ ...form, password: e.target.value })}
          placeholder="Nhập password"
        />

        {error && <div className="error">{error}</div>}

        <button disabled={loading}>
          {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>

        <p className="switch">
          Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
        </p>
      </form>
    </div>
  )
}
