import test from 'node:test'
import assert from 'node:assert/strict'
import { reconnectDelayForAttempt, resolveWebsocketUrl } from './websocket.js'

test('reconnect uses exponential backoff with a safe cap', () => {
  assert.equal(reconnectDelayForAttempt(1), 1000)
  assert.equal(reconnectDelayForAttempt(2), 2000)
  assert.equal(reconnectDelayForAttempt(6), 30000)
  assert.equal(reconnectDelayForAttempt(20), 30000)
})

test('relative API URL resolves WebSocket against the browser origin', () => {
  assert.equal(resolveWebsocketUrl('', '/api', 'http://localhost:8088'), 'ws://localhost:8088/ws')
  assert.equal(resolveWebsocketUrl('', '/api', 'https://chat.example.com'), 'wss://chat.example.com/ws')
})

test('absolute API URL keeps its backend host for WebSocket', () => {
  assert.equal(resolveWebsocketUrl('', 'http://localhost:8080/api', 'http://localhost:5173'), 'ws://localhost:8080/ws')
})
