export function mergeMessages(current, incoming) {
  return incoming.reduce((messages, message) => {
    if (messages.some(item => item.id === message.id)) return messages
    const optimisticIndex = messages.findIndex(item =>
      item.clientMessageId && item.clientMessageId === message.clientMessageId)
    if (optimisticIndex >= 0) {
      const updated = [...messages]
      updated[optimisticIndex] = message
      return updated
    }
    return messages.concat(message)
  }, current)
}
