import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import Icon from './Icon'

const ToastContext = createContext(() => {})

export function ToastProvider({ children }) {
  const [items, setItems] = useState([])
  const notify = useCallback((message, type = 'success') => {
    const id = crypto.randomUUID()
    setItems(current => [...current, { id, message, type }])
    window.setTimeout(() => setItems(current => current.filter(item => item.id !== id)), 3800)
  }, [])
  const value = useMemo(() => notify, [notify])
  return <ToastContext.Provider value={value}>{children}<div className="toast-region" aria-live="polite">{items.map(item => <div className={`toast toast-${item.type}`} key={item.id}><span className="toast-icon"><Icon name={item.type === 'error' ? 'close' : 'check'} size={16}/></span><span>{item.message}</span><button className="icon-button" aria-label="Đóng thông báo" onClick={() => setItems(current => current.filter(entry => entry.id !== item.id))}><Icon name="close" size={16}/></button></div>)}</div></ToastContext.Provider>
}

export const useToast = () => useContext(ToastContext)
