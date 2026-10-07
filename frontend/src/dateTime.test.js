import test from 'node:test'
import assert from 'node:assert/strict'
import { parseApiDateTime, toDateTimeAttribute } from './dateTime.js'

test('treats an offset-less backend timestamp as UTC', () => {
  assert.equal(parseApiDateTime('2026-10-07T01:30:00').getTime(), Date.parse('2026-10-07T01:30:00Z'))
})

test('preserves timestamps that already contain a timezone', () => {
  assert.equal(parseApiDateTime('2026-10-07T08:30:00+07:00').getTime(), Date.parse('2026-10-07T01:30:00Z'))
  assert.equal(parseApiDateTime('2026-10-07T01:30:00Z').getTime(), Date.parse('2026-10-07T01:30:00Z'))
})

test('uses an absolute ISO timestamp in the time element', () => {
  assert.equal(toDateTimeAttribute('2026-10-07T01:30:00'), '2026-10-07T01:30:00.000Z')
})
