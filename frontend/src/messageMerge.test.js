import test from 'node:test'
import assert from 'node:assert/strict'
import { mergeMessages } from './messageMerge.js'

test('realtime and sync overlap does not duplicate a message', () => {
  const message = { id: 42, clientMessageId: 'client-42', content: 'hello' }
  assert.deepEqual(mergeMessages([message], [message]), [message])
})

test('server acknowledgement replaces the optimistic message', () => {
  const pending = { id: 'pending-1', clientMessageId: 'client-1', deliveryStatus: 'SENDING' }
  const saved = { id: 1, clientMessageId: 'client-1', content: 'saved' }
  assert.deepEqual(mergeMessages([pending], [saved]), [saved])
})
