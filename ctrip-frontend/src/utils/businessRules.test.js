import test from 'node:test'
import assert from 'node:assert/strict'
import {
  canBanUser,
  parseRuleConfig,
  ruleDescription,
  mergeRuleConfig
} from './businessRules.js'

test('canBanUser：仅允许封禁正常普通用户', () => {
  assert.equal(canBanUser({ id: 2, role: 'USER', status: 'ACTIVE' }, 1), true)
  assert.equal(canBanUser({ id: 2, role: 'USER', status: 'SUSPENDED' }, 1), false)
  assert.equal(canBanUser({ id: 2, role: 'ADMIN', status: 'ACTIVE' }, 1), false)
  assert.equal(canBanUser({ id: 2, role: 'USER', status: 'DELETED' }, 1), false)
  assert.equal(canBanUser({ id: 1, role: 'USER', status: 'ACTIVE' }, 1), false)
  assert.equal(canBanUser(null, 1), false)
})

test('parseRuleConfig：兼容 JSON 字符串、对象与非法值', () => {
  assert.deepEqual(parseRuleConfig('{"description":"x"}'), { description: 'x' })
  assert.deepEqual(parseRuleConfig({ description: 'y' }), { description: 'y' })
  assert.deepEqual(parseRuleConfig('{bad'), {})
  assert.deepEqual(parseRuleConfig(null), {})
  assert.deepEqual(parseRuleConfig('[1,2]'), {})
})

test('ruleDescription：仅返回合法 description', () => {
  assert.equal(ruleDescription('{"description":"x"}'), 'x')
  assert.equal(ruleDescription({ description: 'y' }), 'y')
  assert.equal(ruleDescription('{bad'), '')
  assert.equal(ruleDescription(null), '')
})

test('mergeRuleConfig：保留未知字段并处理清空', () => {
  const original = { foo: 'bar', description: '旧', discount: 8 }
  const merged = mergeRuleConfig(original, '2026-11-21T00:00:00', '2026-11-22T00:00:00', '新', null)
  assert.equal(merged.foo, 'bar')
  assert.equal(merged.description, '新')
  assert.equal('discount' in merged, false)

  const cleared = mergeRuleConfig(original, 'a', 'b', '', undefined)
  assert.equal('description' in cleared, false)
  assert.equal('discount' in cleared, false)
  assert.equal(cleared.activityStartTime, 'a')
  assert.equal(cleared.activityEndTime, 'b')
})
