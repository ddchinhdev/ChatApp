import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../../api'
import { useAuth } from '../../auth/AuthContext'
import { useToast } from '../../components/Toast'
import Icon from '../../components/Icon'

export default function AdminUserDetail() {
  const { id } = useParams(), { user: admin } = useAuth()
  const [user, setUser] = useState(null), [loading, setLoading] = useState(true), [error, setError] = useState(''), [saving, setSaving] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const toast = useToast()
  useEffect(() => { api.get(`/admin/users/${id}`).then(({ data }) => setUser(data)).catch(err => setError(err.response?.data?.message || 'Không thể tải người dùng.')).finally(() => setLoading(false)) }, [id])
  const toggle = async () => {
    setSaving(true); setError('')
    try { const { data } = await api.patch(`/admin/users/${id}/status`, { active: !user.active }); setUser(data); setConfirming(false); toast(data.active ? 'Đã mở khóa tài khoản.' : 'Đã khóa tài khoản.') }
    catch (err) { const message = err.response?.data?.message || 'Không thể cập nhật tài khoản.'; setError(message); toast(message, 'error') }
    finally { setSaving(false) }
  }
  return <section><Link to="/admin/users">← Danh sách người dùng</Link><h1>Chi tiết người dùng</h1>{loading && <div className="state-card">Đang tải...</div>}{error && <div className="error">{error}</div>}{user && <article className="admin-user-detail"><div className="avatar">{user.avatarUrl ? <img src={user.avatarUrl} alt="" /> : user.displayName.charAt(0).toUpperCase()}</div><dl><dt>Tên</dt><dd>{user.displayName}</dd><dt>Username</dt><dd>@{user.username}</dd><dt>Email</dt><dd>{user.email}</dd><dt>Role</dt><dd>{user.role}</dd><dt>Trạng thái</dt><dd>{user.active ? 'Hoạt động' : 'Đã khóa'}</dd><dt>Ngày tạo</dt><dd>{new Date(user.createdAt).toLocaleString('vi-VN')}</dd></dl>{user.id !== admin.id && <button className={user.active ? 'danger' : ''} disabled={saving} onClick={() => setConfirming(true)}>{user.active ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}</button>}</article>}{confirming && <div className="modal-backdrop"><div className="group-modal" role="alertdialog" aria-modal="true"><span className="empty-icon"><Icon name="lock"/></span><h2 style={{textAlign:'center'}}>Xác nhận {user.active ? 'khóa' : 'mở khóa'}</h2><p className="muted" style={{textAlign:'center'}}>Bạn chắc chắn muốn {user.active ? 'khóa' : 'mở khóa'} tài khoản <strong>@{user.username}</strong>?</p><div className="modal-actions"><button className="secondary" onClick={() => setConfirming(false)}>Hủy</button><button className={user.active ? 'danger' : ''} disabled={saving} onClick={toggle}>{saving ? 'Đang lưu...' : 'Xác nhận'}</button></div></div></div>}</section>
}
