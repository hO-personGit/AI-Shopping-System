<template>
  <div class="category-page">
    <front-header></front-header>
    <div class="main-content">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>{{ categoryName }}</h2>
        <div class="total-count">共 {{ total }} 件商品</div>
      </div>

      <!-- 排序和搜索区域 -->
      <div class="toolbar">
        <div class="sort-options">
          <el-radio-group v-model="sortBy" size="small" @change="handleSort">
            <el-radio-button label="default">默认排序</el-radio-button>
            <el-radio-button label="sales,desc">销量优先</el-radio-button>
            <el-radio-button label="price,asc">价格从低到高</el-radio-button>
            <el-radio-button label="price,desc">价格从高到低</el-radio-button>
          </el-radio-group>
        </div>
        <div class="search-box">
          <el-input v-model="searchKey" placeholder="搜索商品" prefix-icon="el-icon-search" clearable></el-input>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="products-grid" v-loading="loading">
        <el-empty v-if="!loading && products.length === 0" description="暂无商品">
          <el-button type="primary" @click="$router.push('/products')">查看全部商品</el-button>
        </el-empty>

        <el-row :gutter="20" v-else>
          <el-col :span="6" v-for="product in products" :key="product.id">
            <product-card :product="product" />
          </el-col>
        </el-row>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination background :current-page.sync="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next, jumper"
          @current-change="getProducts">
        </el-pagination>
      </div>
    </div>
    <front-footer></front-footer>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import ProductCard from '@/components/front/ProductCard.vue'
import Request from '@/utils/request'
import { debounce } from 'lodash'

export default {
  name: 'Category',
  components: {
    FrontHeader,
    FrontFooter,
    ProductCard
  },
  data() {
    return {
      userInfo: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      loading: false,
      products: [],
      categoryName: '',
      currentPage: 1,
      pageSize: 12,
      total: 0,
      sortBy: 'default',
      searchKey: '',
      debouncedSearch: null
    }
  },
  created() {
    this.userInfo = JSON.parse(localStorage.getItem('frontUser') || '{}')
    this.categoryName = this.$route.query.name || '分类商品'
    this.debouncedSearch = debounce(() => {
      this.currentPage = 1
      this.getProducts()
    }, 300)
    this.getProducts()
  },
  watch: {
    searchKey() {
      this.debouncedSearch()
    }
  },
  methods: {
    async getProducts() {
      this.loading = true
      try {
        const params = {
          categoryId: this.$route.params.id,
          status: 1,
          currentPage: this.currentPage,
          size: this.pageSize,
          name: this.searchKey || undefined
        }

        if (this.sortBy && this.sortBy !== 'default') {
          const [field, order] = this.sortBy.split(',')
          params.sortField = field
          params.sortOrder = order
        }

        const res = await Request.get('/product/page', { params })
        if (res.code === '0') {
          if (res.data && res.data.records) {
            this.products = res.data.records.map(product => ({
              ...product,
              isFavorite: false,
              imageUrl: product.imageUrl?.startsWith('http') ? product.imageUrl : `${product.imageUrl}`
            }))
            this.total = res.data.total
           
          } else {
            this.products = []
            
            this.total = 0
          }
        } else {
          this.products = []
          this.total = 0
          this.$message.error(res.msg || '获取商品列表失败')
        }
        this.loading = false
      } catch (error) {
        console.error('获取商品列表失败:', error)
        this.$message.error('获取商品列表失败')
        this.products = []
        this.total = 0
      }
    },

    handleSort() {
      this.currentPage = 1
      this.getProducts()
    },
    isLogin() {
      if (!this.userInfo?.id) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return false
      }
      return true
    },
 
  }
}
</script>

<style scoped>
.category-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
}

.main-content {
  flex: 1;
  padding: 40px;
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
  margin-bottom: 30px;
  background: white;
  padding: 24px 30px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.page-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  position: relative;
  display: inline-block;
}

.page-header h2::after {
  content: "";
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 100%;
  height: 4px;
  background: #67c23a;
  border-radius: 2px;
  opacity: 0.8;
}

.total-count {
  color: #606266;
  font-size: 15px;
  font-weight: 500;
  background: #f0f2f5;
  padding: 8px 16px;
  border-radius: 20px;
}

/* 工具栏样式 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  background: white;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

/* 商品网格样式 */
.products-grid {
  min-height: 400px;
  margin: 0 -10px;
}

.el-col {
  padding: 10px;
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

/* 排序选项样式 */
.sort-options :deep(.el-radio-group) {
  display: flex;
  gap: 8px;
}

.sort-options :deep(.el-radio-button__inner) {
  border-radius: 20px;
  padding: 0 20px;
  height: 32px;
  line-height: 32px;
  transition: all 0.3s ease;
}

.sort-options :deep(.el-radio-button:not(:last-child) .el-radio-button__inner) {
  border-right: 1px solid #dcdfe6;
}

.sort-options
  :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: #67c23a;
  border-color: #67c23a;
  box-shadow: -1px 0 0 0 #67c23a;
}

/* 搜索框样式 */
.search-box {
  width: 320px;
  position: relative;
}

.search-box :deep(.el-input__inner) {
  border-radius: 24px;
  padding-left: 45px;
  height: 40px;
  line-height: 40px;
  font-size: 14px;
  border: 2px solid transparent;
  background: #f5f7fa;
  transition: all 0.3s ease;
}

.search-box :deep(.el-input__inner:hover) {
  background: #ebeef5;
}

.search-box :deep(.el-input__inner:focus) {
  background: #fff;
  border-color: #67c23a;
  box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.1);
}

.search-box :deep(.el-input__prefix) {
  left: 16px;
  line-height: 40px;
}

.search-box :deep(.el-input__icon) {
  font-size: 18px;
  color: #909399;
  transition: all 0.3s ease;
}

.search-box :deep(.el-input__inner:focus + .el-input__prefix .el-input__icon) {
  color: #67c23a;
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

/* 加载状态样式 */
:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
}

/* 响应式布局 */
@media (max-width: 1440px) {
  .main-content {
    max-width: 1200px;
  }
}

@media (max-width: 1200px) {
  .main-content {
    max-width: 960px;
  }
}
</style> 