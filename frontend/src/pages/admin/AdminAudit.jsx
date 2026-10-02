import { useEffect, useState } from 'react'
import api from '../../api'

export default function AdminAudit() {
  const [page, setPage] = useState(0), [result, setResult] = useState(null), [error, setError] = useState('')
  useEffect(() => { api.get('/admin/audit-logs', { params: { page, size: 20 } }).then(({ data }) => setResult(data)).catch(err => setError(err.response?.data?.message || 'Không thể tải audit log.')) }, [page])
  return <section><h1>Audit log</h1>{error && <div className="error">{error}</div>}{!result && !error && <div className="state-card">Đang tải...</div>}{result?.content.length === 0 && <div className="state-card">Chưa có hành động quản trị.</div>}{result?.content.length > 0 && <><div className="audit-list">{result.content.map(log => <article key={log.id}><strong>{log.action}</strong><span>{log.details}</span><small>{log.adminUsername} · {new Date(`${log.createdAt}Z`).toLocaleString('vi-VN')}</small></article>)}</div><div className="pagination"><button className="secondary" disabled={result.first} onClick={() => setPage(value => value - 1)}>Trước</button><button className="secondary" disabled={result.last} onClick={() => setPage(value => value + 1)}>Sau</button></div></>}
  </section>
}
