import DOMPurify from 'dompurify'

/**
 * 净化用户生成 HTML（攻略正文、预览、后台审核详情）。
 */
export function sanitizeHtml(html) {
  return DOMPurify.sanitize(html || '', {
    USE_PROFILES: { html: true }
  })
}
