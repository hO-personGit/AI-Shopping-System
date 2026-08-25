<template>
  <div class="ai-guide-assistant">
    <el-button class="ai-float-button" type="success" icon="el-icon-chat-dot-round" circle @click="openPanel"></el-button>

    <el-dialog
      title="AI 智能导购"
      :visible.sync="visible"
      width="780px"
      :close-on-click-modal="false"
      append-to-body
      @closed="onClosed"
    >
      <div class="guide-panel">
        <!-- 会话消息区 -->
        <div ref="chatBox" class="chat-box">
          <div v-if="!messages.length" class="chat-empty">
            <i class="el-icon-chat-line-square"></i>
            <p>我是 AI 导购，支持连续追问。<br />描述你的购物需求，例如：想买适合学生党的高性价比商品。</p>
          </div>
          <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-row', msg.role]">
            <div class="bubble">
              <template v-if="msg.role === 'assistant'">
                <div class="assistant-head">
                  <i class="el-icon-magic-stick"></i>
                  <span v-if="msg.toolCalls && msg.toolCalls.length" class="tool-tip">
                    已调用工具：{{ msg.toolCalls.join('、') }}
                  </span>
                  <span v-if="msg.cached" class="tool-tip">缓存命中</span>
                </div>
                <p class="bubble-text">{{ msg.text }}</p>
                <div v-if="msg.recommendations && msg.recommendations.length" class="rec-list">
                  <div
                    v-for="item in msg.recommendations"
                    :key="item.id"
                    class="rec-item"
                    @click="goProduct(item.id)"
                  >
                    <div class="rec-name">{{ item.name }}</div>
                    <div class="rec-meta">
                      <el-tag size="mini" type="success">{{ item.category || '商品' }}</el-tag>
                      <span>￥{{ Number(item.price || 0).toFixed(2) }}</span>
                      <span>库存 {{ item.stock || 0 }}</span>
                      <span>销量 {{ item.salesCount || 0 }}</span>
                    </div>
                    <div class="rec-reason">{{ item.reason }}</div>
                  </div>
                </div>
              </template>
              <template v-else>
                {{ msg.text }}
              </template>
            </div>
          </div>
          <div v-if="streaming" class="chat-row assistant">
            <div class="bubble streaming-bubble"><span class="cursor">▍</span></div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="guide-input-row">
          <el-input
            v-model="query"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="输入购物需求后按 Enter 发送（Shift+Enter 换行）"
            @keydown.enter.native.prevent="submitGuide"
          ></el-input>
          <div class="input-actions">
            <el-button type="success" :loading="loading" icon="el-icon-search" @click="submitGuide">发送</el-button>
            <el-button
              v-if="messages.length"
              type="text"
              icon="el-icon-refresh-left"
              @click="clearChat"
            >清空会话</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { smartGuide, streamGuide } from '@/api/ai'

export default {
  name: 'AiGuideAssistant',
  data() {
    return {
      visible: false,
      loading: false,
      streaming: false,
      query: '',
      // 会话 ID：同一用户多轮对话共享上下文
      sessionId: '',
      messages: []
    }
  },
  methods: {
    openPanel() {
      this.visible = true
      if (!this.sessionId) {
        const userStr = localStorage.getItem('frontUser')
        const userId = userStr ? JSON.parse(userStr).id : undefined
        this.sessionId = `guide-${userId || 'anon'}-${Date.now()}`
      }
      this.$nextTick(() => this.scrollToBottom())
    },
    onClosed() {
      this.loading = false
      this.streaming = false
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const box = this.$refs.chatBox
        if (box) box.scrollTop = box.scrollHeight
      })
    },
    async submitGuide() {
      const text = this.query.trim()
      if (!text) {
        this.$message.warning('请输入购物需求')
        return
      }
      if (this.loading || this.streaming) return

      const userStr = localStorage.getItem('frontUser')
      const userId = userStr ? JSON.parse(userStr).id : undefined
      this.messages.push({ role: 'user', text })
      this.query = ''

      // 追加一条空的助手消息用于流式填充
      const assistantMsg = { role: 'assistant', text: '', recommendations: [], toolCalls: [], cached: false }
      this.messages.push(assistantMsg)
      this.loading = true
      this.streaming = true
      this.scrollToBottom()

      const payload = { query: text, userId, sessionId: this.sessionId, topK: 5 }

      try {
        // 优先使用 SSE 流式输出（打字机效果）
        let streamed = false
        try {
          await streamGuide(payload, {
            onDelta: (delta) => {
              assistantMsg.text += delta
              this.scrollToBottom()
            },
            onDone: (meta) => {
              if (meta) {
                assistantMsg.toolCalls = meta.toolCalls || []
                assistantMsg.recommendations = meta.recommendations || []
                assistantMsg.cached = !!meta.cached
              }
              streamed = true
            },
            onError: (err) => {
              console.warn('AI 流式接口失败，回退到普通模式：', err)
            }
          })
        } catch (e) {
          streamed = false
        }

        // 流式失败时回退到非流式接口
        if (!streamed || !assistantMsg.text) {
          const res = await smartGuide(payload)
          if (res.code === '0' && res.data) {
            assistantMsg.text = res.data.answer || assistantMsg.text
            assistantMsg.recommendations = res.data.recommendations || []
            assistantMsg.toolCalls = res.data.toolCalls || []
            assistantMsg.cached = !!res.data.cached
          } else {
            assistantMsg.text = '抱歉，AI 导购暂时无法回答，请稍后重试。'
          }
        }
      } catch (error) {
        assistantMsg.text = 'AI 导购服务暂不可用，请确认后端和 AI 服务已启动。'
      } finally {
        this.loading = false
        this.streaming = false
        this.scrollToBottom()
      }
    },
    clearChat() {
      this.messages = []
      this.sessionId = `guide-${Date.now()}`
      this.$message.success('已清空会话，开启新的对话上下文')
    },
    goProduct(id) {
      if (!id) return
      this.visible = false
      this.$router.push(`/product/${id}`)
    }
  }
}
</script>

<style scoped>
.ai-float-button {
  position: fixed;
  right: 32px;
  bottom: 86px;
  z-index: 2000;
  width: 56px;
  height: 56px;
  font-size: 24px;
  box-shadow: 0 10px 24px rgba(103, 194, 58, 0.35);
}

.guide-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 520px;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-empty {
  margin: auto;
  text-align: center;
  color: #909399;
  font-size: 13px;
  line-height: 1.9;
}

.chat-empty i {
  font-size: 42px;
  color: #c0c4cc;
  display: block;
  margin-bottom: 8px;
}

.chat-row {
  display: flex;
}

.chat-row.user {
  justify-content: flex-end;
}

.chat-row.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.chat-row.user .bubble {
  background: #67c23a;
  color: #fff;
  border-top-right-radius: 2px;
}

.chat-row.assistant .bubble {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-top-left-radius: 2px;
}

.assistant-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  color: #529b2e;
  font-weight: 600;
  font-size: 13px;
}

.tool-tip {
  font-weight: 400;
  color: #909399;
  font-size: 12px;
  background: #f4f4f5;
  padding: 1px 8px;
  border-radius: 10px;
}

.bubble-text {
  white-space: pre-wrap;
}

.rec-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
  border-top: 1px dashed #ebeef5;
  padding-top: 10px;
}

.rec-item {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fcfcfc;
}

.rec-item:hover {
  border-color: #67c23a;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

.rec-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.rec-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin: 6px 0;
  color: #606266;
  font-size: 12px;
}

.rec-reason {
  color: #606266;
  font-size: 12px;
  line-height: 1.6;
}

.streaming-bubble {
  color: #909399;
}

.cursor {
  animation: blink 1s steps(2, start) infinite;
}

@keyframes blink {
  to {
    visibility: hidden;
  }
}

.guide-input-row {
  display: grid;
  grid-template-columns: 1fr 132px;
  gap: 12px;
  align-items: stretch;
}

.input-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: stretch;
}

@media (max-width: 768px) {
  .guide-input-row {
    grid-template-columns: 1fr;
  }

  .ai-float-button {
    right: 18px;
    bottom: 72px;
  }

  .guide-panel {
    height: 440px;
  }
}
</style>
