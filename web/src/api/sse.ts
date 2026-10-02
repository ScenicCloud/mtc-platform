/**
 * SSE 客户端 —— 手动解析，不用 EventSource
 * 原因：EventSource 只能发 GET、不能带 Authorization 头
 *
 * 支持：
 * - 半帧（一个事件拆成多个 chunk）
 * - 粘帧（多个事件挤在一个 chunk）
 * - UTF-8 多字节字符跨分片
 * - CRLF / LF 行尾
 * - 空闲超时
 * - 用户取消（AbortController）
 */

export interface SseEvent {
  event: string
  data: string
}

export interface SseOptions {
  url: string
  method?: string
  body?: any
  token?: string
  onEvent?: (event: SseEvent) => void
  onError?: (error: string) => void
  onDone?: () => void
  idleTimeoutMs?: number // 空闲超时，默认 90 秒
}

const DEFAULT_IDLE_TIMEOUT = 90000

export function createSseRequest(options: SseOptions): { abort: () => void } {
  const {
    url,
    method = 'POST',
    body,
    token,
    onEvent,
    onError,
    onDone,
    idleTimeoutMs = DEFAULT_IDLE_TIMEOUT,
  } = options

  const controller = new AbortController()
  let idleTimer: ReturnType<typeof setTimeout> | null = null
  let finished = false

  const resetIdleTimer = () => {
    if (idleTimer) clearTimeout(idleTimer)
    idleTimer = setTimeout(() => {
      if (!finished) {
        finished = true
        controller.abort()
        onError?.('响应超时')
      }
    }, idleTimeoutMs)
  }

  const clearIdleTimer = () => {
    if (idleTimer) {
      clearTimeout(idleTimer)
      idleTimer = null
    }
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    signal: controller.signal,
  })
    .then(async (response) => {
      // 非 200 → 不是 SSE，解析 JSON 报错
      if (response.status !== 200) {
        try {
          const errData = await response.json()
          finished = true
          clearIdleTimer()
          onError?.(errData.message || `请求失败 (${response.status})`)
        } catch {
          finished = true
          clearIdleTimer()
          onError?.(`请求失败 (${response.status})`)
        }
        return
      }

      if (!response.body) {
        finished = true
        onError?.('响应体为空')
        return
      }

      resetIdleTimer()

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let eventName = ''
      let dataBuffer = ''
      let hasData = false

      const parseBuffer = () => {
        while (true) {
          // 找行尾（LF 或 CRLF）
          const lfIndex = buffer.indexOf('\n')
          if (lfIndex === -1) break // 不够一行，等下一个 chunk

          let line = buffer.substring(0, lfIndex)
          buffer = buffer.substring(lfIndex + 1)

          // 去掉 CR
          if (line.endsWith('\r')) {
            line = line.substring(0, line.length - 1)
          }

          if (line === '') {
            // 空行 = 事件结束
            if (hasData) {
              const evt: SseEvent = {
                event: eventName || 'message',
                data: dataBuffer,
              }
              resetIdleTimer()
              onEvent?.(evt)

              if (evt.event === 'done') {
                finished = true
                clearIdleTimer()
                onDone?.()
                reader.cancel()
                return
              }
              if (evt.event === 'error') {
                finished = true
                clearIdleTimer()
                try {
                  const errObj = JSON.parse(evt.data)
                  onError?.(errObj.message || '发生错误')
                } catch {
                  onError?.(evt.data || '发生错误')
                }
                reader.cancel()
                return
              }
            }
            // 重置
            eventName = ''
            dataBuffer = ''
            hasData = false
          } else if (line.startsWith('event:')) {
            eventName = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            if (hasData) {
              dataBuffer += '\n'
            }
            dataBuffer += line.substring(5).trim()
            hasData = true
          }
          // 其他行（注释 : ping 等）忽略
        }
      }

      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          // TextDecoder 自动处理 UTF-8 多字节分片
          buffer += decoder.decode(value, { stream: true })
          parseBuffer()

          if (finished) break
        }

        // 流结束但没收到 done/error
        if (!finished) {
          finished = true
          clearIdleTimer()
          onError?.('连接异常中断')
        }
      } catch (e: any) {
        if (e.name === 'AbortError') {
          // 用户主动取消
          if (!finished) {
            finished = true
            clearIdleTimer()
            onError?.('已取消')
          }
        } else if (!finished) {
          finished = true
          clearIdleTimer()
          onError?.(e.message || '连接异常')
        }
      }
    })
    .catch((e) => {
      if (e.name === 'AbortError') {
        if (!finished) {
          finished = true
          clearIdleTimer()
          onError?.('已取消')
        }
      } else if (!finished) {
        finished = true
        clearIdleTimer()
        onError?.('网络连接失败')
      }
    })

  return {
    abort: () => {
      if (!finished) {
        finished = true
        clearIdleTimer()
        controller.abort()
      }
    },
  }
}
