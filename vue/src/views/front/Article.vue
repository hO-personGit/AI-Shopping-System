<template>
  <div class="article-page">
    <front-header></front-header>
    <div class="main-content">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>农业资讯</h2>
        <div class="article-count">共 {{ total }} 篇文章</div>
      </div>

      <!-- 文章列表 -->
      <div class="article-content" v-loading="loading">
        <!-- 空状态提示 -->
        <el-empty v-if="!loading && articles.length === 0" :image-size="200" class="empty-article">
          <template slot="description">
            <div class="empty-text">
              <p class="main-text">暂无相关资讯</p>
              <p class="sub-text">敬请期待更多精彩内容</p>
            </div>
          </template>
        </el-empty>

        <!-- 文章列表 -->
        <div v-else class="article-list">
          <div v-for="article in articles" :key="article.id" class="article-item" @click="viewArticle(article)">
            <el-image 
              :src="article.coverImage?.startsWith('http') ? article.coverImage : `/api${article.coverImage}`" 
              fit="cover"
              class="article-image">
              <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline"></i>
              </div>
            </el-image>
            <div class="article-details">
              <h3 class="article-title">{{ article.title }}</h3>
              <p class="article-summary">{{ article.summary }}</p>
              <div class="article-meta">
                <span class="view-count">
                  <i class="el-icon-view"></i>
                  {{ article.viewCount }} 阅读
                </span>
                <span class="publish-time">
                  <i class="el-icon-time"></i>
                  {{ formatTime(article.createdAt) }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            background
            :current-page.sync="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next, jumper"
            @current-change="getArticles">
          </el-pagination>
        </div>
      </div>
    </div>
    <front-footer></front-footer>

    <!-- 文章详情对话框 -->
    <el-dialog
      :title="currentArticle?.title"
      :visible.sync="dialogVisible"
      width="70%"
      class="article-dialog">
      <div class="article-content-wrapper">
        <div class="article-info">
          <span class="publish-time">发布时间：{{ formatTime(currentArticle?.createdAt) }}</span>
          <span class="view-count">
            <i class="el-icon-view"></i>
            {{ currentArticle?.viewCount }} 阅读
          </span>
        </div>
        <div class="article-full-content" v-html="currentArticle?.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import Request from '@/utils/request'
import { formatTime } from '@/utils/time'

export default {
  name: 'Article',
  components: {
    FrontHeader,
    FrontFooter
  },
  data() {
    return {
      loading: false,
      articles: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      currentArticle: null
    }
  },
  created() {
    this.getArticles()
  },
  methods: {
    formatTime,
    async getArticles() {
      this.loading = true
      try {
        const res = await Request.get('/article/page', {
          params: {
            currentPage: this.currentPage,
            size: this.pageSize,
            status: 1, // 只获取上架的文章
            title: '' // 可选的标题搜索参数
          }
        })
        if (res.code === '0') {
          this.articles = res.data.records.map(article => ({
            id: article.id,
            title: article.title,
            content: article.content,
            summary: article.summary || this.generateSummary(article.content), // 如果没有摘要，从内容生成
            coverImage: article.coverImage,
            viewCount: article.viewCount,
            createdAt: article.createdAt,
            updatedAt: article.updatedAt
          }))
          this.total = res.data.total
        }
      } catch (error) {
    
      } finally {
        this.loading = false
      }
    },
    generateSummary(content) {
      // 移除HTML标签
      const plainText = content.replace(/<[^>]+>/g, '')
      // 取前100个字符作为摘要
      return plainText.substring(0, 100) + (plainText.length > 100 ? '...' : '')
    },
    async viewArticle(article) {
      this.$router.push(`/article/${article.id}`)
    }
  }
}
</script>

<style scoped>
.article-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f8faf5 0%, #f5f7fa 100%);
}

.main-content {
  flex: 1;
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

/* 页面标题样式 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  background: white;
  padding: 20px 24px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(to bottom, #67C23A, #85ce61);
}

.page-header h2 {
  font-size: 24px;
  font-weight: 500;
  color: #2c3e50;
  margin: 0;
}

.article-count {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  background: rgba(103, 194, 58, 0.1);
  padding: 6px 16px;
  border-radius: 20px;
}

/* 文章列表样式 */
.article-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.article-item {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  transition: all 0.3s;
  cursor: pointer;
  position: relative;
}

.article-item:hover {
  transform: translateY(-2px);
  background: linear-gradient(to right, rgba(103, 194, 58, 0.02), transparent);
}

.article-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(to bottom, #67C23A, #85ce61);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.article-item:hover::before {
  opacity: 1;
}

.article-image {
  width: 100%;
  height: 200px;
  transition: all 0.3s ease;
}

.article-image:hover {
  transform: scale(1.02);
}

.article-details {
  padding: 20px;
}

.article-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #303133;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  transition: color 0.3s ease;
}

.article-item:hover .article-title {
  color: #67C23A;
}

.article-summary {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  opacity: 0.9;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.view-count, .publish-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin: 40px 0;
  padding: 20px 0;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background-color: #67c23a;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled):hover) {
  color: #67c23a;
}

/* 文章详情对话框样式 */
.article-dialog :deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 20px;
  border-bottom: 1px solid #f0f2f5;
  margin: 0;
  background: linear-gradient(135deg, #f8faf5, #fff);
}

:deep(.el-dialog__body) {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

/* 自定义滚动条样式 */
:deep(.el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:deep(.el-dialog__body::-webkit-scrollbar-thumb) {
  background: rgba(144, 147, 153, 0.3);
  border-radius: 3px;
}

:deep(.el-dialog__body::-webkit-scrollbar-track) {
  background: transparent;
}

.article-content-wrapper {
  position: relative;
}

.article-info {
  display: flex;
  justify-content: space-between;
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  color: #909399;
  font-size: 14px;
  background: #f8faf5;
  padding: 16px 20px;
  border-radius: 8px;
}

.article-full-content {
  line-height: 1.8;
  color: #303133;
  font-size: 16px;
  padding: 0 20px;
}

/* 空状态样式 */
.empty-article {
  padding: 60px 0;
  background: white;
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.empty-text {
  margin: 16px 0;
}

.empty-text .main-text {
  font-size: 16px;
  color: #606266;
  margin-bottom: 8px;
}

.empty-text .sub-text {
  font-size: 14px;
  color: #909399;
}

:deep(.el-empty__image) {
  opacity: 0.8;
}
</style> 