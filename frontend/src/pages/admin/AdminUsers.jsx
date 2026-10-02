import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api'

export default function AdminUsers() {
  const [query, setQuery] = useState(''), [debounced, setDebounced] = useState(''), [page, setPage] = useState(0)
  const [result, setResult] = useState(null), [loading, setLoading] = useState(true), [error, setError] = useState('')
  useEffect(() => { const timer = setTimeout(() => { setDebounced(query.trim()); setPage(0) }, 300); return () => clearTimeout(timer) }, [query])
  useEffect(() => { const controller = new AbortController(); setLoading(true); setError(''); api.get('/admin/users', { params: { q: debounced, page, size: 15 }, signal: controller.signal }).then(({ data }) => setResult(data)).catch(err => { if (err.code !== 'ERR_CANCELED') setError(err.response?.data?.message || 'Không thể tải người dùng.') }).finally(() => { if (!controller.signal.aborted) setLoading(false) }); return () => controller.abort() }, [debounced, page])
  return <section><h1>Quản lý người dùng</h1><input type="search" placeholder="Tìm username, email hoặc tên..." value={query} onChange={event => setQuery(event.target.value)} />
    {loading && <div className="state-card">Đang tải...</div>}{error && <div className="error">{error}</div>}{!loading && !error && result?.content.length === 0 && <div className="state-card">Không có người dùng.</div>}
    {!loading && result?.content.length > 0 && <><div className="admin-table"><div className="admin-table-head"><span>Người dùng</span><span>Role</span><span>Trạng thái</span><span></span></div>{result.content.map(user => <div key={user.id}><span><strong>{user.displayName}</strong><small>@{user.username} · {user.email}</small></span><span>{user.role}</span><span className={user.active ? 'status-active' : 'status-banned'}>{user.active ? 'Hoạt động' : 'Đã khóa'}</span><Link to={`/admin/users/${user.id}`}>Chi tiết</Link></div>)}</div><div className="pagination"><button className="secondary" disabled={result.first} onClick={() => setPage(value => value - 1)}>Trước</button><span>Trang {result.page + 1}/{Math.max(result.totalPages, 1)}</span><button className="secondary" disabled={result.last} onClick={() => setPage(value => value + 1)}>Sau</button></div></>}
  </section>
}
