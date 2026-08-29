import Request from '@/utils/request'

/**
 * 非流式智能导购（普通模式 / 兜底）。
 */
export function smartGuide(data) {
  return Request.post('/ai/guide', data, { timeout: 60000 })
}

/**
 * SSE 流式智能导购：使用 fetch 手动解析 text/event-stream，实现打字机效果。
 *
 * @param {Object} data 请求体（query / userId / sessionId / topK）
 * @param {Object} handlers { onDelta(text), onDone(meta), onError(err) }
 * @returns {Promise<void>}
 */
export function streamGuide(data, { onDelta, onDone, onError } = {}) {
  return new Promise((resolve, reject) => {
    const user = localStorage.getItem('backUser')
    const headers = { 'Content-Type': 'application/json' }
    if (user) {
      try {
        headers.token = JSON.parse(user).token
      } catch (e) {
        // ignore
      }
    }

    fetch('/api/ai/guide/stream', {
      method: 'POST',
      headers,
      body: JSON.stringify(data)
    })
      .then(async (response) => {
        if (!response.ok || !response.body) {
          throw new Error(`AI 流式接口异常：HTTP ${response.status}`)
        }
        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        const processEvent = (event) => {
          const payloadStr = event.replace(/^data:\s*/, '').trim()
          if (!payloadStr || payloadStr === '[DONE]') {
            return
          }
          try {
            const payload = JSON.parse(payloadStr)
            if (payload.error) {
              onError && onError(new Error(payload.error))
              return
            }
            if (payload.delta) {
              onDelta && onDelta(payload.delta)
            } else if (payload.done) {
              onDone && onDone(payload)
            }
          } catch (e) {
            // 忽略无法解析的中间事件
          }
        }

        const pump = () => {
          return reader.read().then(({ done, value }) => {
            if (done) {
              onDone && onDone(null)
              resolve()
              return
            }
            buffer += decoder.decode(value, { stream: true })
            // SSE 事件以空行分隔
            const parts = buffer.split('\n\n')
            buffer = parts.pop()
            parts.forEach((part) => {
              part.split('\n').filter((l) => l.startsWith('data:')).forEach(processEvent)
            })
            return pump()
          })
        }
        return pump()
      })
      .catch((err) => {
        onError && onError(err)
        reject(err)
      })
  })
}

export function generateCopywriting(data) {
  return Request.post('/ai/copywriting', data, { timeout: 60000 })
}

export function analyzeSales(data = {}) {
  return Request.post('/ai/sales-analysis', data, { timeout: 60000 })
}
