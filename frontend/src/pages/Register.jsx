import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'

export default function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const { data } = await api.post('/auth/register', form)
      localStorage.setItem('chatapp_token', data.token)
      localStorage.setItem('chatapp_user', JSON.stringify(data.user))
      navigate('/chat')
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng ký thất bại')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1>ChatApp</h1>
        <p className="subtitle">Tạo tài khoản</p>

        <label>Username</label>
        <input
          value={form.username}
          onChange={e => setForm({ ...form, username: e.target.value })}
          placeholder="Ít nhất 3 ký tự"
        />

        <label>Email</label>
        <input
          type="email"
          value={form.email}
          onChange={e => setForm({ ...form, email: e.target.value })}
          placeholder="you@example.com"
        />

        <label>Password</label>
        <input
          type="password"
          value={form.password}
          onChange={e => setForm({ ...form, password: e.target.value })}
          placeholder="Ít nhất 6 ký tự"
        />

        {error && <div className="error">{error}</div>}

        <button disabled={loading}>
          {loading ? 'Đang tạo...' : 'Đăng ký'}
        </button>

        <p className="switch">
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </p>
      </form>
    </div>
  )
}
