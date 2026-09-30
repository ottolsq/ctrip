import request from '@/utils/request'

/**
 * 评论相关接口
 */

/**
 * 获取评论列表（树形结构）
 */
export function getComments(guideId) {
  return request.get(`/v1/guides/${guideId}/comments`)
}

/**
 * 发表评论
 */
export function createComment(guideId, data) {
  return request.post(`/v1/guides/${guideId}/comments`, data)
}

/**
 * 删除评论
 */
export function deleteComment(guideId, commentId) {
  return request.delete(`/v1/guides/${guideId}/comments/${commentId}`)
}