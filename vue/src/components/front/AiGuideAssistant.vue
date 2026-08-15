<template>
  <div class="ai-guide-assistant">
    <el-button class="ai-float-button" type="success" icon="el-icon-chat-dot-round" circle @click="visible = true"></el-button>

    <el-dialog title="AI智能导购" :visible.sync="visible" width="760px" :close-on-click-modal="false" append-to-body>
      <div class="guide-panel">
        <div class="guide-input-row">
          <el-input
            v-model="query"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入你的购物需求，例如：我想买适合学生党的高性价比商品"
          ></el-input>
          <el-button type="success" :loading="loading" icon="el-icon-search" @click="submitGuide">智能推荐</el-button>
        </div>

        <div v-if="answer" class="guide-answer">
          <div class="answer-title"><i class="el-icon-magic-stick"></i> 导购建议</div>
          <p>{{ answer }}</p>
        </div>

        <div v-if="recommendations.length" class="recommendation-list">
          <div v-for="item in recommendations" :key="item.id" class="recommendation-card" @click="goProduct(item.id)">
            <div class="card-main">
              <div class="product-name">{{ item.name }}</div>
              <div class="product-meta">
                <el-tag size="mini" type="success">{{ item.category || '商品' }}</el-tag>
                <span>￥{{ Number(item.price || 0).toFixed(2) }}</span>
                <span>库存 {{ item.stock || 0 }}</span>
                <span>销量 {{ item.salesCount || 0 }}</span>
              </div>
              <div class="reason">{{ item.reason }}</div>
            </div>
            <i class="el-icon-arrow-right"></i>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { smartGuide } from '@/api/ai'

export default {
  name: 'AiGuideAssistant',
  data() {
    return {
      visible: false,
      loading: false,
      query: '',
      answer: '',
      recommendations: []
    }
  },
  methods: {
    async submitGuide() {
      if (!this.query.trim()) {
        this.$message.warning('请输入购物需求')
        return
      }
      this.loading = true
      try {
        const userStr = localStorage.getItem('frontUser')
        const userId = userStr ? JSON.parse(userStr).id : undefined
        const res = await smartGuide({ query: this.query, userId, topK: 5 })
        if (res.code === '0') {
          this.answer = res.data.answer
          this.recommendations = res.data.recommendations || []
        } else {
          this.$message.error(res.msg || 'AI导购生成失败')
        }
      } catch (error) {
        this.$message.error('AI导购服务暂不可用，请确认后端和AI服务已启动')
      } finally {
        this.loading = false
      }
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
  gap: 18px;
}

.guide-input-row {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
  align-items: stretch;
}

.guide-answer {
  padding: 14px 16px;
  background: #f0f9eb;
  border: 1px solid #dff3d8;
  border-radius: 8px;
  color: #303133;
  line-height: 1.7;
}

.answer-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-weight: 600;
  color: #529b2e;
}

.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recommendation-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.recommendation-card:hover {
  border-color: #67c23a;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin: 8px 0;
  color: #606266;
  font-size: 13px;
}

.reason {
  color: #606266;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .guide-input-row {
    grid-template-columns: 1fr;
  }

  .ai-float-button {
    right: 18px;
    bottom: 72px;
  }
}
</style>
