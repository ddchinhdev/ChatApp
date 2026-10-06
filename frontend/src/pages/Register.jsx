import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../auth/AuthContext'
import PasswordField from '../components/PasswordField'

export default function Register() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const { data } = await api.post('/auth/register', form)
      login(data)
      navigate('/chat', { replace: true })
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

        <label htmlFor="register-username">Username</label>
        <input
          id="register-username"
          name="username"
          autoComplete="username"
          required
          minLength={3}
          maxLength={50}
          pattern="[A-Za-z0-9_]+"
          value={form.username}
          onChange={e => setForm({ ...form, username: e.target.value })}
          placeholder="Ít nhất 3 ký tự"
        />

        <label htmlFor="register-email">Email</label>
        <input
          id="register-email"
          name="email"
          type="email"
          autoComplete="email"
          required
          maxLength={120}
          value={form.email}
          onChange={e => setForm({ ...form, email: e.target.value })}
          placeholder="you@example.com"
        />

        <PasswordField
          id="register-password"
          label="Mật khẩu"
          name="password"
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
          value={form.password}
          onChange={e => setForm({ ...form, password: e.target.value })}
          placeholder="Từ 8 đến 72 ký tự"
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
