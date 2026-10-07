const TIME_ZONE_SUFFIX = /(Z|[+-]\d{2}:?\d{2})$/i

// The backend stores and returns LocalDateTime values in UTC. Java serializes
// those values without an offset, so make the UTC meaning explicit before the
// browser converts them to the user's local time.
export function parseApiDateTime(value) {
  if (value instanceof Date) return value
  if (typeof value !== 'string') return new Date(value)
  return new Date(TIME_ZONE_SUFFIX.test(value) ? value : `${value}Z`)
}

export function toDateTimeAttribute(value) {
  const date = parseApiDateTime(value)
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString()
}

export function dayKey(value) {
  return parseApiDateTime(value).toLocaleDateString('vi-VN')
}

export function formatDate(value) {
  return parseApiDateTime(value).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

export function formatTime(value) {
  return parseApiDateTime(value).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatListTime(value, now = new Date()) {
  const date = parseApiDateTime(value)
  return dayKey(date) === dayKey(now)
    ? formatTime(date)
    : date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}
