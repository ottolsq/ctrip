/**
 * 前端可测试的业务规则纯函数。
 */

export function canBanUser(row, currentUserId) {
  if (!row) return false
  return row.role !== 'ADMIN'
    && row.status !== 'SUSPENDED'
    && row.status !== 'DELETED'
    && row.id !== currentUserId
}

export function parseRuleConfig(value) {
  if (!value) return {}
  if (typeof value === 'object') {
    return Array.isArray(value) ? {} : value
  }
  if (typeof value !== 'string') return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

export function ruleDescription(value) {
  const rule = parseRuleConfig(value)
  return typeof rule.description === 'string' ? rule.description : ''
}

export function mergeRuleConfig(original, activityStartTime, activityEndTime, description, discount) {
  const merged = {
    ...(original && typeof original === 'object' && !Array.isArray(original) ? original : {})
  }
  merged.activityStartTime = activityStartTime
  merged.activityEndTime = activityEndTime

  if (description) {
    merged.description = description
  } else {
    delete merged.description
  }

  if (discount !== null && discount !== undefined && discount !== '') {
    merged.discount = discount
  } else {
    delete merged.discount
  }
  return merged
}
