import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import AppHeader from '../components/AppHeader'
import api from '../api'
import { webSocketService } from '../websocket'
import { mergeMessages } from '../messageMerge'
import { promoteReceipt, sumUnread } from '../receipt'
import { createTypingController, friendlyLastSeen } from '../typing'
import Icon from '../components/Icon'
import { useToast } from '../components/Toast'
import { dayKey, formatDate, formatListTime, formatTime, parseApiDateTime, toDateTimeAttribute } from '../dateTime'

export default function Home() {
  const { user, webSocketStatus } = useAuth()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const [conversations, setConversations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [messageEvents, setMessageEvents] = useState([])
  const [receiptEvents, setReceiptEvents] = useState([])
  const [typingEvents, setTypingEvents] = useState([])
  const [showCreateGroup, setShowCreateGroup] = useState(false)
  const selectedId = searchParams.get('conversation')
  const selectedIdRef = useRef(selectedId)
  const seenIncomingRef = useRef(new Set())
  selectedIdRef.current = selectedId

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    api.get('/conversations', { signal: controller.signal })
      .then(({ data }) => { setConversations(data); setError('') })
      .catch(err => {
        if (err.code !== 'ERR_CANCELED') setError(err.response?.data?.message || 'Không thể tải danh sách cuộc trò chuyện.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [])

  const selected = conversations.find(item => String(item.id) === selectedId)
  useEffect(() => {
    if (!loading && selectedId && !selected) {
      setSearchParams({}, { replace: true })
    }
  }, [loading, selected, selectedId, setSearchParams])

  const removeLeftConversation = conversationId => {
    const remaining = conversations.filter(item => item.id !== conversationId)
    setConversations(remaining)
    const nextConversation = remaining[0]
    setSearchParams(nextConversation ? { conversation: nextConversation.id } : {}, { replace: true })
  }
  const recordLastMessage = (message, countUnread = false) => setConversations(current => current
    .map(item => item.id === message.conversationId ? {
      ...item,
      lastMessage: message,
      lastActivityAt: message.createdAt,
      unreadCount: countUnread ? (item.unreadCount || 0) + 1 : item.unreadCount,
    } : item)
    .sort((a, b) => parseApiDateTime(b.lastActivityAt) - parseApiDateTime(a.lastActivityAt)))

  useEffect(() => webSocketService.subscribeDestination('/user/queue/messages', message => {
    const incoming = message.senderId !== user?.id
    const activeConversation = String(message.conversationId) === selectedIdRef.current
      && document.visibilityState === 'visible' && document.hasFocus()
    const shouldCount = incoming && !activeConversation && !seenIncomingRef.current.has(message.id)
    if (incoming) seenIncomingRef.current.add(message.id)
    recordLastMessage(message, shouldCount)
    setMessageEvents(current => current.concat(message).slice(-500))
  }), [user?.id])

  useEffect(() => webSocketService.subscribeDestination('/user/queue/receipts', receipt => {
    setReceiptEvents(current => current.concat(receipt).slice(-500))
  }), [])

  useEffect(() => webSocketService.subscribeDestination('/user/queue/typing', event => {
    setTypingEvents(current => current.concat(event).slice(-200))
  }), [])

  useEffect(() => {
    if (webSocketStatus !== 'CONNECTED' || !user?.id) return
    let cancelled = false
    const synchronize = async () => {
      let syncCursor = readMessageId(user.id)
      let more = true
      while (more && !cancelled) {
        const { data } = await api.get('/sync/messages', { params: { afterMessageId: syncCursor, limit: 100 } })
        if (data.messages.length) {
          data.messages.forEach(recordLastMessage)
          setMessageEvents(current => current.concat(data.messages).slice(-500))
          syncCursor = data.nextAfterMessageId
          rememberMessageId(user.id, syncCursor)
        }
        more = data.hasMore
      }
      const { data: refreshed } = await api.get('/conversations')
      if (!cancelled) setConversations(refreshed)
    }
    synchronize().catch(() => { if (!cancelled) setError('Không thể đồng bộ tin nhắn đã bỏ lỡ.') })
    return () => { cancelled = true }
  }, [webSocketStatus, user?.id])

  const totalUnread = sumUnread(conversations)
  const clearUnread = conversationId => setConversations(current => current.map(conversation =>
    conversation.id === conversationId ? { ...conversation, unreadCount: 0 } : conversation))

  return <div className="home-page">
    <AppHeader />
    <main id="main-content" className={`chat-layout ${selected ? 'has-selection' : ''}`} tabIndex="-1">
      <aside className="conversation-sidebar" aria-label="Danh sách cuộc trò chuyện">
        <div className="conversation-heading"><div><span className="eyebrow">Tin nhắn</span><h1>Trò chuyện {totalUnread > 0 && <span className="total-unread">{totalUnread}</span>}</h1></div><div className="heading-actions"><button className="icon-button" onClick={() => setShowCreateGroup(true)} aria-label="Tạo nhóm" title="Tạo nhóm"><Icon name="users"/></button><Link className="icon-button primary" to="/users" state={{ backgroundLocation: location }} aria-label="Tìm người để trò chuyện" title="Tìm người"><Icon name="plus"/></Link></div></div>
        {loading && <div className="conversation-skeleton" aria-label="Đang tải cuộc trò chuyện" aria-busy="true" role="status">{[1,2,3,4,5].map(item => <div className="skeleton-row" key={item} aria-hidden="true"><i/><span><b/><b/></span></div>)}</div>}
        {error && <div className="error" role="alert">{error}</div>}
        {!loading && !error && conversations.length === 0 && <div className="sidebar-state"><span className="empty-icon"><Icon name="chat" size={28}/></span><strong>Chưa có cuộc trò chuyện</strong><p>Tìm một người bạn và gửi lời chào đầu tiên.</p><Link className="text-link" to="/users" state={{ backgroundLocation: location }}>Tìm người dùng</Link></div>}
        {!loading && !error && conversations.map(conversation => <button key={conversation.id}
          className={`conversation-item ${String(conversation.id) === selectedId ? 'selected' : ''}`}
          aria-pressed={String(conversation.id) === selectedId}
          onClick={() => setSearchParams({ conversation: conversation.id })}>
          <UserAvatar person={conversationPerson(conversation)} />
          <span className="conversation-summary"><span className="conversation-meta"><strong>{conversationTitle(conversation)}</strong>{conversation.lastActivityAt && <time>{formatListTime(conversation.lastActivityAt)}</time>}</span><small>{conversation.lastMessage?.content || (conversation.type === 'GROUP' ? `${conversation.memberCount} thành viên` : `@${conversation.otherUser.username}`)}</small></span>
          {conversation.unreadCount > 0 && <span className="unread-badge">{conversation.unreadCount > 99 ? '99+' : conversation.unreadCount}</span>}
        </button>)}
      </aside>
      <section className="conversation-panel">
        {selected
          ? <ChatRoom key={selected.id} conversation={selected} currentUser={user}
              webSocketStatus={webSocketStatus} messageEvents={messageEvents}
              receiptEvents={receiptEvents} onMessageSent={recordLastMessage}
              typingEvents={typingEvents} onRead={() => clearUnread(selected.id)}
              onConversationUpdated={updated => setConversations(current => current.map(item => item.id === updated.id ? updated : item))}
              onBack={() => setSearchParams({})}
              onLeft={removeLeftConversation} />
          : <div className="conversation-placeholder"><span className="hero-chat-icon"><Icon name="sparkles" size={34}/></span><h2>Xin chào, {user?.displayName || user?.username}</h2><p>Chọn một cuộc trò chuyện ở bên trái hoặc tìm người mới để bắt đầu kết nối.</p><Link className="action-link" to="/users" state={{ backgroundLocation: location }}><Icon name="search" size={18}/> Tìm người dùng</Link></div>}
      </section>
    </main>
    {showCreateGroup && <CreateGroupModal onClose={() => setShowCreateGroup(false)} onCreated={group => {
      setConversations(current => [group, ...current])
      setSearchParams({ conversation: group.id })
      setShowCreateGroup(false)
    }} />}
  </div>
}

function ChatRoom({ conversation, currentUser, webSocketStatus, messageEvents, receiptEvents, typingEvents, onMessageSent, onRead, onConversationUpdated, onBack, onLeft }) {
  const toast = useToast()
  const [messages, setMessages] = useState([])
  const [cursor, setCursor] = useState(null)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingOlder, setLoadingOlder] = useState(false)
  const [error, setError] = useState('')
  const [content, setContent] = useState('')
  const [sending, setSending] = useState(false)
  const [typingName, setTypingName] = useState('')
  const [showGroupInfo, setShowGroupInfo] = useState(false)
  const listRef = useRef(null)
  const preserveScrollRef = useRef(null)
  const shouldAutoScrollRef = useRef(true)
  const initialScrollRef = useRef(true)
  const deliveredCursorRef = useRef(0)
  const readCursorRef = useRef(0)
  const typingControllerRef = useRef(null)

  useEffect(() => {
    const controller = createTypingController(typing => {
      if (webSocketService.isConnected()) {
        webSocketService.publish('/app/chat.typing', { conversationId: conversation.id, typing })
      }
    })
    typingControllerRef.current = controller
    return () => {
      controller.dispose()
      typingControllerRef.current = null
    }
  }, [conversation.id])

  useEffect(() => {
    const latest = [...typingEvents].reverse().find(event => event.conversationId === conversation.id
      && event.userId !== currentUser?.id)
    if (!latest || !latest.typing) {
      setTypingName('')
      return
    }
    setTypingName(latest.displayName)
    const delay = Math.max(0, Date.parse(latest.expiresAt) - Date.now())
    const timer = setTimeout(() => setTypingName(''), delay)
    return () => clearTimeout(timer)
  }, [typingEvents, conversation.id, currentUser?.id])

  const nearBottom = () => {
    const list = listRef.current
    return !list || list.scrollHeight - list.scrollTop - list.clientHeight < 100
  }

  const acceptServerMessage = message => {
    if (message.conversationId !== conversation.id) return
    shouldAutoScrollRef.current = nearBottom()
    setMessages(current => mergeMessages(current, [message]))
    onMessageSent(message)
    if (message.senderId !== currentUser?.id) acknowledge('delivered', message.id)
  }

  const acknowledge = (type, messageId) => {
    const numericId = Number(messageId)
    const cursorRef = type === 'read' ? readCursorRef : deliveredCursorRef
    if (!numericId || numericId <= cursorRef.current) return
    cursorRef.current = numericId
    const body = { conversationId: conversation.id, messageId: numericId }
    if (webSocketService.isConnected()) {
      webSocketService.publish(`/app/chat.${type}`, body)
    } else {
      api.post(`/conversations/${conversation.id}/receipts/${type}`, { messageId: numericId })
        .catch(() => { cursorRef.current = 0 })
    }
    if (type === 'read') onRead()
  }

  useEffect(() => {
    messageEvents.filter(message => message.conversationId === conversation.id).forEach(acceptServerMessage)
  }, [messageEvents, conversation.id])

  useEffect(() => {
    receiptEvents.filter(receipt => receipt.conversationId === conversation.id).forEach(receipt => {
      setMessages(current => current.map(message => message.senderId === currentUser?.id && Number(message.id) <= receipt.messageId
        ? { ...message, receiptStatus: promoteReceipt(message.receiptStatus, receipt.status) }
        : message))
    })
  }, [receiptEvents, conversation.id, currentUser?.id])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    api.get(`/conversations/${conversation.id}/messages`, { params: { size: 30 }, signal: controller.signal })
      .then(({ data }) => {
        setMessages([...data.messages].reverse())
        setCursor(data.nextCursor)
        setHasMore(data.hasMore)
        initialScrollRef.current = true
        const newestIncoming = data.messages.find(message => message.senderId !== currentUser?.id)
        if (newestIncoming) acknowledge('delivered', newestIncoming.id)
      })
      .catch(err => {
        if (err.code !== 'ERR_CANCELED') setError(err.response?.data?.message || 'Không thể tải lịch sử chat.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [conversation.id])

  useEffect(() => {
    const markVisibleAsRead = () => {
      if (document.visibilityState !== 'visible' || !document.hasFocus()) return
      const latestIncoming = [...messages].reverse().find(message => message.senderId !== currentUser?.id)
      if (latestIncoming) acknowledge('read', latestIncoming.id)
    }
    markVisibleAsRead()
    document.addEventListener('visibilitychange', markVisibleAsRead)
    window.addEventListener('focus', markVisibleAsRead)
    return () => {
      document.removeEventListener('visibilitychange', markVisibleAsRead)
      window.removeEventListener('focus', markVisibleAsRead)
    }
  }, [messages, conversation.id, currentUser?.id])

  useLayoutEffect(() => {
    const list = listRef.current
    if (!list || loading) return
    if (preserveScrollRef.current) {
      const { height, top } = preserveScrollRef.current
      list.scrollTop = list.scrollHeight - height + top
      preserveScrollRef.current = null
    } else if (initialScrollRef.current || shouldAutoScrollRef.current) {
      list.scrollTop = list.scrollHeight
    }
    initialScrollRef.current = false
  }, [messages, loading])

  const loadOlder = async () => {
    const list = listRef.current
    if (!cursor || !hasMore || loadingOlder || !list) return
    setLoadingOlder(true)
    preserveScrollRef.current = { height: list.scrollHeight, top: list.scrollTop }
    try {
      const { data } = await api.get(`/conversations/${conversation.id}/messages`, { params: { cursor, size: 30 } })
      setMessages(current => [...data.messages].reverse().concat(current))
      setCursor(data.nextCursor)
      setHasMore(data.hasMore)
    } catch (err) {
      preserveScrollRef.current = null
      setError(err.response?.data?.message || 'Không thể tải thêm tin nhắn cũ.')
    } finally {
      setLoadingOlder(false)
    }
  }

  const send = async event => {
    event.preventDefault()
    const trimmed = content.trim()
    if (!trimmed || sending) return
    typingControllerRef.current?.stop()
    const clientMessageId = crypto.randomUUID()
    const optimistic = {
      id: `pending-${clientMessageId}`,
      conversationId: conversation.id,
      senderId: currentUser.id,
      senderUsername: currentUser.username,
      senderDisplayName: currentUser.displayName,
      content: trimmed,
      clientMessageId,
      createdAt: new Date().toISOString(),
      deliveryStatus: 'SENDING',
      receiptStatus: 'SENT',
    }
    shouldAutoScrollRef.current = nearBottom()
    setMessages(current => current.concat(optimistic))
    setSending(true)
    setError('')
    try {
      setContent('')
      if (webSocketService.isConnected()) {
        webSocketService.publish('/app/chat.send', { conversationId: conversation.id, content: trimmed, clientMessageId })
      } else {
        const { data } = await api.post(`/conversations/${conversation.id}/messages`, { content: trimmed, clientMessageId })
        acceptServerMessage(data)
      }
    } catch (err) {
      setMessages(current => current.map(message => message.clientMessageId === clientMessageId
        ? { ...message, deliveryStatus: 'FAILED' } : message))
      const message = err.response?.data?.message || 'Không thể gửi tin nhắn qua REST fallback.'
      setError(message)
      toast(message, 'error')
    } finally {
      setSending(false)
    }
  }

  return <div className="chat-room">
    <div className="selected-conversation"><button type="button" className="icon-button mobile-back" aria-label="Quay lại danh sách" onClick={onBack}><Icon name="back"/></button><div className="presence-avatar"><UserAvatar person={conversationPerson(conversation)} />{conversation.type === 'DIRECT' && conversation.otherUser.online && <span className="online-dot" aria-label="Online" />}</div><div className="conversation-header-text"><h2>{conversationTitle(conversation)}</h2><div className="muted">{conversation.type === 'GROUP' ? `${conversation.memberCount} thành viên` : presenceLabel(conversation.otherUser)} <span className={`connection-dot ${webSocketStatus.toLowerCase()}`} aria-hidden="true"/> {connectionLabel(webSocketStatus)}</div>{typingName && <div className="typing-indicator" role="status"><span/><span/><span/> {typingName} đang nhập</div>}</div><button type="button" className="icon-button group-info-button" aria-label={showGroupInfo ? 'Đóng thông tin cuộc trò chuyện' : 'Mở thông tin cuộc trò chuyện'} title={showGroupInfo ? 'Đóng thông tin' : 'Thông tin'} aria-controls="conversation-info-panel" aria-expanded={showGroupInfo} onClick={() => setShowGroupInfo(value => !value)}><Icon name="info"/></button></div>
    {showGroupInfo && (conversation.type === 'GROUP'
      ? <GroupInfo conversation={conversation} currentUser={currentUser} onUpdated={onConversationUpdated} onLeft={onLeft} onClose={() => setShowGroupInfo(false)} />
      : <DirectConversationInfo conversation={conversation} onDeleted={onLeft} onClose={() => setShowGroupInfo(false)} />)}
    <div className="message-list" ref={listRef} onScroll={event => {
      shouldAutoScrollRef.current = nearBottom()
      if (event.currentTarget.scrollTop < 80) loadOlder()
    }}>
      {loading && <div className="message-loading" aria-label="Đang tải lịch sử" aria-busy="true" role="status">{[1,2,3,4,5].map((item) => <i aria-hidden="true" className={item % 2 ? 'left' : 'right'} key={item}/>)}</div>}
      {loadingOlder && <div className="older-loading">Đang tải tin nhắn cũ...</div>}
      {!loading && !error && messages.length === 0 && <div className="message-state"><span className="empty-icon"><Icon name="chat" size={28}/></span><strong>Bắt đầu cuộc trò chuyện</strong><p>Một lời chào thân thiện luôn là khởi đầu tuyệt vời.</p></div>}
      {!loading && messages.map((message, index) => {
        const previous = messages[index - 1]
        const showDate = !previous || dayKey(previous.createdAt) !== dayKey(message.createdAt)
        const own = message.senderId === currentUser?.id
        return <div key={message.id}>
          {showDate && <div className="date-separator"><span>{formatDate(message.createdAt)}</span></div>}
          {message.system ? <div className="system-message"><span>{message.content}</span></div> : <div className={`message-row ${own ? 'sent' : 'received'}`}><div className="message-bubble">{conversation.type === 'GROUP' && !own && <small className="message-sender">{message.senderDisplayName}</small>}<div>{message.content}</div><time dateTime={toDateTimeAttribute(message.createdAt)}>{formatTime(message.createdAt)}{own ? ` · ${message.deliveryStatus === 'SENDING' ? 'Đang gửi' : message.deliveryStatus === 'FAILED' ? 'Lỗi' : receiptLabel(message.receiptStatus)}` : ''}</time></div></div>}
        </div>
      })}
    </div>
    {error && <div className="chat-error error" role="alert">{error}</div>}
    <form className="message-composer" onSubmit={send}>
      <textarea value={content} maxLength={4000} rows={1} placeholder="Nhập tin nhắn..." onChange={event => { setContent(event.target.value); typingControllerRef.current?.input(event.target.value) }}
        onKeyDown={event => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); event.currentTarget.form.requestSubmit() } }} />
      <button className="send-button" disabled={sending || !content.trim()} aria-label="Gửi tin nhắn"><Icon name="send" size={19}/><span>{sending ? 'Đang gửi' : 'Gửi'}</span></button>
    </form>
  </div>
}

function CreateGroupModal({ onClose, onCreated }) {
  const [name, setName] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [selected, setSelected] = useState([])
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  useEffect(() => {
    const closeOnEscape = event => { if (event.key === 'Escape' && !saving) onClose() }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [onClose, saving])
  useEffect(() => {
    if (!query.trim()) return setResults([])
    const controller = new AbortController()
    const timer = setTimeout(() => api.get('/users/search', { params: { q: query.trim(), size: 10 }, signal: controller.signal })
      .then(({ data }) => setResults(data.content)).catch(err => { if (err.code !== 'ERR_CANCELED') setError('Không thể tìm thành viên.') }), 300)
    return () => { clearTimeout(timer); controller.abort() }
  }, [query])
  const create = async event => {
    event.preventDefault(); setSaving(true); setError('')
    try {
      const { data } = await api.post('/conversations/groups', { name: name.trim(), avatarUrl: avatarUrl.trim() || null, memberIds: selected.map(user => user.id) })
      onCreated(data)
    } catch (err) { setError(err.response?.data?.message || 'Không thể tạo nhóm.') }
    finally { setSaving(false) }
  }
  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !saving) onClose() }}><form className="group-modal" role="dialog" aria-modal="true" aria-labelledby="create-group-title" onSubmit={create}>
    <div className="modal-header"><div><span className="eyebrow">Cuộc trò chuyện mới</span><h2 id="create-group-title">Tạo nhóm mới</h2></div><button type="button" className="icon-button" aria-label="Đóng" disabled={saving} onClick={onClose}><Icon name="close"/></button></div><label>Tên nhóm<input autoFocus value={name} maxLength={100} required onChange={event => setName(event.target.value)} /></label><label>Ảnh nhóm (URL)<input type="url" value={avatarUrl} maxLength={500} onChange={event => setAvatarUrl(event.target.value)} /></label>
    <label>Chọn thành viên<input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Tìm username hoặc tên..." /></label>
    <div className="selected-members">{selected.map(user => <button type="button" key={user.id} onClick={() => setSelected(current => current.filter(item => item.id !== user.id))}>{user.displayName} ×</button>)}</div>
    <div className="member-search-results">{results.filter(user => !selected.some(item => item.id === user.id)).map(user => <button type="button" key={user.id} onClick={() => setSelected(current => current.concat(user))}><UserAvatar person={user} /> {user.displayName} <span>@{user.username}</span></button>)}</div>
    {error && <div className="error">{error}</div>}<div className="modal-actions"><button type="button" className="secondary" onClick={onClose}>Hủy</button><button disabled={saving || !name.trim()}>{saving ? 'Đang tạo...' : 'Tạo nhóm'}</button></div>
  </form></div>
}

function DirectConversationInfo({ conversation, onDeleted, onClose }) {
  const toast = useToast()
  const person = conversation.otherUser
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')
  const remove = async () => {
    if (deleting) return
    setDeleting(true)
    setError('')
    try {
      await api.delete(`/conversations/${conversation.id}`)
      onDeleted(conversation.id)
      toast('Đã xóa cuộc trò chuyện.')
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể xóa cuộc trò chuyện. Vui lòng thử lại.')
      setConfirmDelete(false)
    } finally {
      setDeleting(false)
    }
  }
  return <aside id="conversation-info-panel" className="group-info-panel contact-info-panel" aria-label="Thông tin người dùng">
    <button type="button" className="icon-button info-close" aria-label="Đóng thông tin" onClick={onClose}><Icon name="close"/></button>
    <div className="contact-profile"><UserAvatar person={person} /><h3>{person.displayName}</h3><p>{presenceLabel(person)}</p></div>
    <div className="contact-detail-list">
      <div><span className="contact-detail-icon"><Icon name="user" size={18}/></span><span><strong>@{person.username}</strong><small>Tên người dùng</small></span></div>
      <div><span className="contact-detail-icon"><Icon name="info" size={18}/></span><span><strong>{person.bio || 'Chưa có phần giới thiệu'}</strong><small>Giới thiệu</small></span></div>
      <div><span className="contact-detail-icon"><Icon name="users" size={18}/></span><span><strong>{person.commonGroupCount || 0} nhóm chung</strong><small>Nhóm có cả hai người tham gia</small></span></div>
    </div>
    {error && <div className="error" role="alert">{error}</div>}
    <button type="button" className="contact-danger-action" onClick={() => setConfirmDelete(true)}><Icon name="trash" size={19}/><span><strong>Xóa cuộc trò chuyện</strong><small>Xóa toàn bộ tin nhắn của cuộc trò chuyện này</small></span></button>
    {confirmDelete && <div className="modal-backdrop leave-group-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !deleting) setConfirmDelete(false) }}><div className="leave-group-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-chat-title" aria-describedby="delete-chat-description"><span className="eyebrow">Xác nhận xóa</span><h2 id="delete-chat-title">Bạn có chắc chắn muốn xóa cuộc trò chuyện?</h2><p id="delete-chat-description">Toàn bộ tin nhắn giữa bạn và <strong>{person.displayName}</strong> sẽ bị xóa cho cả hai người. Thao tác này không thể hoàn tác.</p><div className="modal-actions"><button autoFocus type="button" className="secondary" disabled={deleting} onClick={() => setConfirmDelete(false)}>Hủy</button><button type="button" className="danger" disabled={deleting} onClick={remove}>{deleting ? 'Đang xóa...' : 'Xóa cuộc trò chuyện'}</button></div></div></div>}
  </aside>
}

function GroupInfo({ conversation, currentUser, onUpdated, onLeft, onClose }) {
  const toast = useToast()
  const [members, setMembers] = useState([])
  const [name, setName] = useState(conversation.name)
  const [avatarUrl, setAvatarUrl] = useState(conversation.avatarUrl || '')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [error, setError] = useState('')
  const [addingUserId, setAddingUserId] = useState(null)
  const [leaving, setLeaving] = useState(false)
  const [confirmLeave, setConfirmLeave] = useState(false)
  const canManage = conversation.currentUserRole === 'OWNER' || conversation.currentUserRole === 'ADMIN'
  const load = () => api.get(`/conversations/${conversation.id}/members`).then(({ data }) => setMembers(data))
    .catch(err => setError(err.response?.data?.message || 'Không thể tải thành viên.'))
  useEffect(() => {
    let active = true
    api.get(`/conversations/${conversation.id}/members`)
      .then(({ data }) => { if (active) setMembers(data) })
      .catch(err => { if (active) setError(err.response?.data?.message || 'Không thể tải thành viên.') })
    return () => { active = false }
  }, [conversation.id])
  useEffect(() => {
    if (!query.trim()) return setResults([])
    const controller = new AbortController()
    const timer = setTimeout(() => api.get('/users/search', { params: { q: query.trim(), size: 8 }, signal: controller.signal })
      .then(({ data }) => setResults(data.content)).catch(() => {}), 300)
    return () => { clearTimeout(timer); controller.abort() }
  }, [query])
  const action = async callback => { try { setError(''); await callback(); await load() } catch (err) { setError(err.response?.data?.message || 'Thao tác nhóm thất bại.') } }
  const save = () => action(async () => { const { data } = await api.patch(`/conversations/${conversation.id}/group`, { name, avatarUrl: avatarUrl.trim() || null }); onUpdated(data) })
  const transfer = userId => action(async () => {
    await api.post(`/conversations/${conversation.id}/transfer-owner/${userId}`)
    const { data } = await api.get(`/conversations/${conversation.id}`)
    onUpdated(data)
  })
  const addMember = async user => {
    if (addingUserId !== null) return
    setAddingUserId(user.id)
    setError('')
    try {
      const { data: member } = await api.post(`/conversations/${conversation.id}/members/${user.id}`)
      setMembers(current => current.some(item => item.userId === member.userId) ? current : current.concat(member))
      setQuery('')
      setResults([])
      const { data: updatedConversation } = await api.get(`/conversations/${conversation.id}`)
      onUpdated(updatedConversation)
      toast(`Đã thêm ${member.displayName} vào nhóm.`)
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể thêm thành viên. Vui lòng thử lại.')
    } finally {
      setAddingUserId(null)
    }
  }
  const leave = async () => {
    if (leaving) return
    setLeaving(true)
    setError('')
    try {
      await api.post(`/conversations/${conversation.id}/leave`)
      onLeft(conversation.id)
      toast('Bạn đã rời nhóm và vẫn đang đăng nhập.')
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể rời nhóm. Vui lòng thử lại.')
      setConfirmLeave(false)
    } finally {
      setLeaving(false)
    }
  }
  return <aside id="conversation-info-panel" className="group-info-panel"><button type="button" className="icon-button info-close" aria-label="Đóng thông tin nhóm" onClick={onClose}><Icon name="close"/></button><h3>Thông tin nhóm</h3>{canManage && <div className="group-name-fields"><div className="group-name-edit"><input value={name} maxLength={100} onChange={event => setName(event.target.value)} /><button onClick={save}>Lưu</button></div><input type="url" placeholder="URL ảnh nhóm" value={avatarUrl} maxLength={500} onChange={event => setAvatarUrl(event.target.value)} /></div>}
    {canManage && <><label className="group-member-search">Thêm thành viên<input type="search" placeholder="Tìm username hoặc tên..." value={query} onChange={event => setQuery(event.target.value)} /></label><div className="member-search-results">{results.filter(user => !members.some(member => member.userId === user.id)).map(user => <button type="button" key={user.id} disabled={addingUserId !== null} onClick={() => addMember(user)}><UserAvatar person={user} /><span><strong>{user.displayName}</strong><small>@{user.username}</small></span><span className="member-add-label">{addingUserId === user.id ? 'Đang thêm...' : 'Thêm'}</span></button>)}</div></>}
    <div className="group-member-list">{members.map(member => <div key={member.userId}><UserAvatar person={member} /><span><strong>{member.displayName}</strong><small>{member.role}</small></span>{conversation.currentUserRole === 'OWNER' && member.userId !== currentUser.id && member.role !== 'OWNER' && <button className="secondary" onClick={() => action(() => api.patch(`/conversations/${conversation.id}/members/${member.userId}/role`, { role: member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN' }))}>{member.role === 'ADMIN' ? 'Hạ quyền' : 'Admin'}</button>}{canManage && member.userId !== currentUser.id && member.role !== 'OWNER' && <button className="danger" onClick={() => action(() => api.delete(`/conversations/${conversation.id}/members/${member.userId}`))}>Xóa</button>}{conversation.currentUserRole === 'OWNER' && member.userId !== currentUser.id && <button className="secondary" onClick={() => transfer(member.userId)}>Chuyển owner</button>}</div>)}</div>
    {error && <div className="error" role="alert">{error}</div>}<button type="button" className="danger leave-group" onClick={() => setConfirmLeave(true)}>Rời nhóm</button>
    {confirmLeave && <div className="modal-backdrop leave-group-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !leaving) setConfirmLeave(false) }}><div className="leave-group-dialog" role="alertdialog" aria-modal="true" aria-labelledby="leave-group-title" aria-describedby="leave-group-description"><span className="eyebrow">Xác nhận thao tác</span><h2 id="leave-group-title">Bạn có chắc chắn muốn rời nhóm không?</h2><p id="leave-group-description">Bạn sẽ không còn xem hoặc gửi tin nhắn trong nhóm <strong>{conversation.name}</strong>. Tài khoản của bạn vẫn được đăng nhập.</p><div className="modal-actions"><button autoFocus type="button" className="secondary" disabled={leaving} onClick={() => setConfirmLeave(false)}>Không, ở lại</button><button type="button" className="danger" disabled={leaving} onClick={leave}>{leaving ? 'Đang rời nhóm...' : 'Có, rời nhóm'}</button></div></div></div>}
  </aside>
}

function UserAvatar({ person }) {
  return <div className="avatar">{person.avatarUrl ? <img src={person.avatarUrl} alt="" /> : (person.displayName || person.username).charAt(0).toUpperCase()}</div>
}
function conversationTitle(conversation) { return conversation.type === 'GROUP' ? conversation.name : conversation.otherUser.displayName }
function conversationPerson(conversation) { return conversation.type === 'GROUP' ? { displayName: conversation.name, avatarUrl: conversation.avatarUrl } : conversation.otherUser }

function connectionLabel(status) { return status === 'CONNECTED' ? 'Đã kết nối' : status === 'CONNECTING' ? 'Đang kết nối lại…' : 'Mất kết nối' }
function presenceLabel(person) {
  if (person.online) return 'Đang online'
  return friendlyLastSeen(person.lastSeenAt)
}
function receiptLabel(status) { return status === 'READ' ? 'Đã xem' : status === 'DELIVERED' ? 'Đã nhận' : 'Đã gửi' }
function watermarkKey(userId) { return `chatapp_last_message_id_${userId}` }
function readMessageId(userId) { return Number(localStorage.getItem(watermarkKey(userId)) || 0) }
function rememberMessageId(userId, messageId) {
  if (!userId || !messageId) return
  const current = readMessageId(userId)
  if (messageId > current) localStorage.setItem(watermarkKey(userId), String(messageId))
}
