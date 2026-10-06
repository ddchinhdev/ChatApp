import { useState } from 'react'
import Icon from './Icon'

export default function PasswordField({ id, label = 'Password', ...props }) {
  const [visible, setVisible] = useState(false)

  return <div className="field-group">
    <label htmlFor={id}>{label}</label>
    <div className="password-field">
      <input id={id} type={visible ? 'text' : 'password'} {...props} />
      <button
        type="button"
        className="password-toggle"
        aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
        aria-pressed={visible}
        onClick={() => setVisible(value => !value)}
      >
        <Icon name={visible ? 'eyeOff' : 'eye'} size={19} />
      </button>
    </div>
  </div>
}
