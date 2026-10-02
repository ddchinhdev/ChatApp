import test from 'node:test'
import assert from 'node:assert/strict'
import { promoteReceipt, sumUnread } from './receipt.js'

test('receipt status advances and never regresses across sessions', () => {
  assert.equal(promoteReceipt('SENT', 'DELIVERED'), 'DELIVERED')
  assert.equal(promoteReceipt('DELIVERED', 'READ'), 'READ')
  assert.equal(promoteReceipt('READ', 'DELIVERED'), 'READ')
})

test('total unread is the sum of conversation unread counts', () => {
  assert.equal(sumUnread([{ unreadCount: 2 }, { unreadCount: 3 }, {}]), 5)
})
