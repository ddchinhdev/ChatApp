import { useEffect, useState } from 'react'
import api from '../api'
import { useAuth } from '../auth/AuthContext'
import AppHeader from '../components/AppHeader'
import { useToast } from '../components/Toast'

const emptyForm = { displayName: '', bio: '', avatarUrl: '' }

export default function Profile() {
  const { user, updateUser } = useAuth()
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const toast = useToast()

  useEffect(() => {
    api.get('/users/me')
      .then(({ data }) => {
        updateUser(data)
        setForm({
          displayName: data.displayName || '',
          bio: data.bio || '',
          avatarUrl: data.avatarUrl || '',
        })
      })
      .catch(err => setError(err.response?.data?.message || 'Không thể tải hồ sơ.'))
      .finally(() => setLoading(false))
  }, [])

  const change = (event) => {
    setForm(current => ({ ...current, [event.target.name]: event.target.value }))
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSaving(true)

    try {
      const { data } = await api.patch('/users/me', form)
      updateUser(data)
      setForm({
        displayName: data.displayName || '',
        bio: data.bio || '',
        avatarUrl: data.avatarUrl || '',
      })
      setSuccess('Đã cập nhật hồ sơ.')
      toast('Hồ sơ đã được cập nhật.')
    } catch (err) {
      const fields = err.response?.data?.fieldErrors
      const message = fields ? Object.values(fields).join('. ') : err.response?.data?.message || 'Cập nhật thất bại.'
      setError(message); toast(message, 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="home-page">
      <AppHeader />
      <main id="main-content" className="page-content" tabIndex="-1">
        <section className="profile-card">
          <h1>Hồ sơ của tôi</h1>
          {loading && <div className="state-card" role="status" aria-busy="true">Đang tải hồ sơ...</div>}
          {!loading && (
            <form onSubmit={submit}>
              <div className="profile-summary">
                <div className="avatar avatar-large">
                  {form.avatarUrl
                    ? <img src={form.avatarUrl} alt="Ảnh đại diện" />
                    : (form.displayName || user?.username || '?').charAt(0).toUpperCase()}
                </div>
                <div>
                  <strong>@{user?.username}</strong>
                  <div className="muted">{user?.email}</div>
                </div>
              </div>

              <label htmlFor="displayName">Tên hiển thị</label>
              <input id="displayName" name="displayName" value={form.displayName} onChange={change}
                     required maxLength={100} />

              <label htmlFor="bio">Giới thiệu</label>
              <textarea id="bio" name="bio" value={form.bio} onChange={change} maxLength={500}
                        rows={5} placeholder="Một vài điều về bạn" />
              <div className="character-count">{form.bio.length}/500</div>

              <label htmlFor="avatarUrl">Avatar URL</label>
              <input id="avatarUrl" name="avatarUrl" type="url" value={form.avatarUrl} onChange={change}
                     maxLength={500} placeholder="https://example.com/avatar.png" />

              {error && <div className="error" role="alert">{error}</div>}
              {success && <div className="success" role="status">{success}</div>}
              <button disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
            </form>
          )}
        </section>
      </main>
    </div>
  )
}
