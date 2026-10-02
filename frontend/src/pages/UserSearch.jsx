import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import AppHeader from '../components/AppHeader'
import { useToast } from '../components/Toast'

const pageSize = 6

export default function UserSearch() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [openingUserId, setOpeningUserId] = useState(null)
  const toast = useToast()

  const openConversation = async (userId) => {
    setOpeningUserId(userId)
    setError('')
    try {
      const { data } = await api.post(`/conversations/direct/${userId}`)
      navigate(`/chat?conversation=${data.id}`)
    } catch (err) {
      const message = err.response?.data?.message || 'Không thể mở cuộc trò chuyện.'
      setError(message); toast(message, 'error')
    } finally {
      setOpeningUserId(null)
    }
  }

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setDebouncedQuery(query.trim())
      setPage(0)
    }, 350)
    return () => window.clearTimeout(timeout)
  }, [query])

  useEffect(() => {
    if (!debouncedQuery) {
      setResult(null)
      setError('')
      return
    }

    const controller = new AbortController()
    setLoading(true)
    setError('')

    api.get('/users/search', {
      params: { q: debouncedQuery, page, size: pageSize },
      signal: controller.signal,
    })
      .then(({ data }) => setResult(data))
      .catch(err => {
        if (err.code !== 'ERR_CANCELED') {
          setError(err.response?.data?.message || 'Không thể tìm kiếm người dùng.')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => controller.abort()
  }, [debouncedQuery, page])

  return (
    <div className="home-page">
      <AppHeader />
      <main className="page-content">
        <section className="search-card">
          <h1>Tìm người dùng</h1>
          <label htmlFor="user-search">Username hoặc tên hiển thị</label>
          <input id="user-search" type="search" value={query}
                 onChange={event => setQuery(event.target.value)}
                 maxLength={100} placeholder="Nhập từ khóa tìm kiếm..." autoFocus />

          {!debouncedQuery && !loading && (
            <div className="state-card">Nhập username hoặc tên hiển thị để bắt đầu.</div>
          )}
          {loading && <div className="state-card">Đang tìm kiếm...</div>}
          {error && <div className="error" role="alert">{error}</div>}
          {!loading && !error && result?.content.length === 0 && (
            <div className="state-card">Không tìm thấy người dùng phù hợp.</div>
          )}

          {!loading && !error && result?.content.length > 0 && (
            <>
              <div className="search-meta">Tìm thấy {result.totalElements} người dùng</div>
              <div className="user-grid">
                {result.content.map(user => (
                  <article className="user-card" key={user.id}>
                    <div className="avatar">
                      {user.avatarUrl
                        ? <img src={user.avatarUrl} alt="" />
                        : (user.displayName || user.username).charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <strong>{user.displayName}</strong>
                      <div className="muted">@{user.username}</div>
                      {user.bio && <p>{user.bio}</p>}
                      <button className="start-chat" disabled={openingUserId === user.id}
                              onClick={() => openConversation(user.id)}>
                        {openingUserId === user.id ? 'Đang mở...' : 'Nhắn tin'}
                      </button>
                    </div>
                  </article>
                ))}
              </div>

              <div className="pagination">
                <button className="secondary" disabled={result.first}
                        onClick={() => setPage(current => current - 1)}>Trang trước</button>
                <span>Trang {result.page + 1}/{result.totalPages}</span>
                <button className="secondary" disabled={result.last}
                        onClick={() => setPage(current => current + 1)}>Trang sau</button>
              </div>
            </>
          )}
        </section>
      </main>
    </div>
  )
}
