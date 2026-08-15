import Request from '@/utils/request'

export function smartGuide(data) {
  return Request.post('/ai/guide', data, { timeout: 60000 })
}

export function generateCopywriting(data) {
  return Request.post('/ai/copywriting', data, { timeout: 60000 })
}

export function analyzeSales(data = {}) {
  return Request.post('/ai/sales-analysis', data, { timeout: 60000 })
}
