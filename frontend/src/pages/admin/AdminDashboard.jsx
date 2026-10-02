import { useEffect, useState } from 'react'
import api from '../../api'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api.get('/admin/stats').then(({ data }) => setStats(data)).catch(err => setError(err.response?.data?.message || 'Không thể tải thống kê.')) }, [])
  return <section><h1>Tổng quan hệ thống</h1>{error && <div className="error">{error}</div>}{!stats && !error && <div className="state-card">Đang tải thống kê...</div>}{stats && <><div className="admin-stat-grid">
    <Stat label="Tổng người dùng" value={stats.users} /><Stat label="Đang hoạt động" value={stats.activeUsers} /><Stat label="Conversation" value={stats.conversations} /><Stat label="Message" value={stats.messages} />
  </div><div className="simple-chart"><h2>Tương quan dữ liệu</h2>{[['User', stats.users], ['Conversation', stats.conversations], ['Message', stats.messages]].map(([label, value]) => <div key={label}><span>{label}</span><div><i style={{ width: `${Math.max(2, value / Math.max(stats.users, stats.conversations, stats.messages, 1) * 100)}%` }} /></div><strong>{value}</strong></div>)}</div></>}
  </section>
}
function Stat({ label, value }) { return <article className="admin-stat"><span>{label}</span><strong>{value}</strong></article> }
