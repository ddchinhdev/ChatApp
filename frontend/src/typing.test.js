import test from 'node:test'
import assert from 'node:assert/strict'
import { createTypingController, friendlyLastSeen } from './typing.js'

test('typing emits one start for rapid keys and stops after idle timeout', () => {
  const events = []
  let timer
  const controller = createTypingController(value => events.push(value), {
    now: () => 1000,
    schedule: callback => { timer = callback; return 1 },
    cancel: () => {},
  })
  controller.input('x')
  controller.input('xy')
  controller.input('xyz')
  assert.deepEqual(events, [true])
  timer()
  assert.deepEqual(events, [true, false])
})

test('last seen parses the UTC instant and formats a friendly relative time', () => {
  assert.equal(friendlyLastSeen('2026-09-29T12:00:00Z', Date.parse('2026-09-29T12:05:00Z')),
    'Hoạt động 5 phút trước')
})
