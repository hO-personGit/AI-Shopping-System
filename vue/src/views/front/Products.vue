<template>
  <div class="products-page">
    <front-header></front-header>
    <div class="main-content">
      <!-- 页面标题 -->
      <div class="page-header">
        <div class="title-container">
          <h2 class="page-title">
            <i class="el-icon-shopping-bag-1"></i>
            <span>全部商品</span>
          </h2>
          <div class="page-subtitle">精选优质农产品，品质保障</div>
        </div>
        <div class="search-box">
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索商品" 
            prefix-icon="el-icon-search"
            clearable
            @clear="handleSearch"
            @keyup.enter.native="handleSearch"
          >
          </el-input>
        </div>
      </div>

      <!-- 顶部过滤器 -->
      <div class="filter-bar">
        <div class="filter-group">
          <div class="filter-item">
            <el-dropdown trigger="click" @command="handleCategoryChange">
              <span class="filter-label" :class="{'active': selectedCategory}">
                <i class="el-icon-menu"></i>
                分类
                <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="">全部商品</el-dropdown-item>
                <el-dropdown-item 
                  v-for="category in categories" 
                  :key="category.id"
                  :command="category.id"
                >{{ category.name }}</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>

          <div class="filter-item">
            <el-dropdown trigger="click" @command="handlePriceRangeChange">
              <span class="filter-label" :class="{'active': priceRange}">
                <i class="el-icon-price-tag"></i>
                价格
                <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="">全部价格</el-dropdown-item>
                <el-dropdown-item 
                  v-for="(range, index) in priceRanges" 
                  :key="index"
                  :command="range.value"
                >{{ range.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>

          <div class="filter-item">
            <el-dropdown trigger="click" @command="handleSortChange">
              <span class="filter-label" :class="{'active': sortBy !== 'default'}">
                <i class="el-icon-sort"></i>
                排序
                <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item 
                  v-for="option in sortOptions" 
                  :key="option.value"
                  :command="option.value"
                >{{ option.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>

        <div class="filter-actions">
          <el-button 
            type="text" 
            class="reset-btn" 
            @click="resetFilters" 
            v-if="hasFilters"
          >
            <i class="el-icon-refresh-right"></i> 重置筛选
          </el-button>
        </div>
      </div>

      <!-- 已选择的过滤条件 -->
      <div class="selected-filters" v-if="hasFilters">
        <div class="selected-filters-title">已选条件：</div>
        <div class="selected-filters-content">
          <el-tag 
            v-if="selectedCategory" 
            closable
            @close="handleCategoryChange('')"
            type="success"
            effect="light"
          >
            分类: {{ getCategoryName(selectedCategory) }}
          </el-tag>
          <el-tag 
            v-if="priceRange" 
            closable
            @close="handlePriceRangeChange('')"
            type="success"
            effect="light"
          >
            价格: {{ getPriceRangeLabel(priceRange) }}
          </el-tag>
          <el-tag 
            v-if="sortBy !== 'default'" 
            closable
            @close="handleSortChange('default')"
            type="success"
            effect="light"
          >
            {{ getSortLabel(sortBy) }}
          </el-tag>
          <el-tag 
            v-if="searchKeyword" 
            closable
            @close="clearSearch"
            type="success"
            effect="light"
          >
            关键词: {{ searchKeyword }}
          </el-tag>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="products-section" v-loading="loading">
        <transition-group name="fade-list" tag="div" class="grid-container">
          <div 
            v-for="product in products" 
            :key="product.id"
            class="product-item"
          >
            <product-card :product="product" />
          </div>
        </transition-group>
        
        <div v-if="!loading && products.length === 0" class="empty-state">
          <i class="el-icon-goods"></i>
          <p>暂无相关商品</p>
          <el-button type="primary" plain round @click="resetFilters">清除筛选条件</el-button>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          background
          :current-page.sync="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 36, 48]"
          layout="sizes, prev, pager, next, jumper, total"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        >
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
  name: 'Products',
  components: {
    FrontHeader,
    FrontFooter,
    ProductCard
  },
  data() {
    return {
      loading: false,
      products: [],
      categories: [],
      selectedCategory: '',
      priceRange: '',
      priceRanges: [
        { label: '0-50元', value: '0-50' },
        { label: '50-100元', value: '50-100' },
        { label: '100-200元', value: '100-200' },
        { label: '200元以上', value: '200-' }
      ],
      sortOptions: [
        { label: '默认排序', value: 'default' },
        { label: '销量优先', value: 'sales,desc' },
        { label: '价格从低到高', value: 'price,asc' },
        { label: '价格从高到低', value: 'price,desc' }
      ],
      sortBy: 'default',
      searchKeyword: '',
      currentPage: 1,
      pageSize: 12,
      total: 0,
      debouncedSearch: null
    }
  },
  computed: {
    hasFilters() {
      return this.selectedCategory || this.priceRange || this.sortBy !== 'default' || this.searchKeyword
    }
  },
  methods: {
    // 获取商品分类
    async getCategories() {
      try {
        const res = await Request.get('/category/all')
        if (res.code === '0') {
          this.categories = res.data
        }
      } catch (error) {
        console.error('获取分类失败:', error)
      }
    },
    // 获取商品列表
    async getProducts() {
      this.loading = true
      try {
        const params = {
          status: 1,
          currentPage: this.currentPage,
          size: this.pageSize
        }

        // 添加分类筛选
        if (this.selectedCategory) {
          params.categoryId = this.selectedCategory
        }

        // 添加价格区间筛选
        if (this.priceRange) {
          const [min, max] = this.priceRange.split('-')
          if (min) params.minPrice = min
          if (max) params.maxPrice = max
        }

        // 添加排序
        if (this.sortBy !== 'default') {
          const [field, order] = this.sortBy.split(',')
          params.sortField = field
          params.sortOrder = order
        }

        // 添加搜索关键词
        if (this.searchKeyword) {
          params.name = this.searchKeyword
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
        }
      } catch (error) {
        console.error('获取商品列表失败:', error)
        this.$message.error('获取商品列表失败')
        this.products = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleCategoryChange(categoryId) {
      this.selectedCategory = categoryId
      this.currentPage = 1
      this.getProducts()
    },
    handlePriceRangeChange(range) {
      this.priceRange = range
      this.currentPage = 1
      this.getProducts()
    },
    handleSortChange(value) {
      this.sortBy = value
      this.currentPage = 1
      this.getProducts()
    },
    handleSearch() {
      this.debouncedSearch()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.getProducts()
    },
    handleRouteChange() {
      const query = {}
      if (this.selectedCategory) {
        query.category = this.selectedCategory
      }
      if (this.searchKeyword) {
        query.keyword = this.searchKeyword
      }
      // 更新URL，但不触发路由变化
      this.$router.replace({ query }).catch(() => {})
    },
    getCategoryName(id) {
      const category = this.categories.find(c => c.id === id)
      return category ? category.name : '全部'
    },
    getPriceRangeLabel(value) {
      const range = this.priceRanges.find(r => r.value === value)
      return range ? range.label : '全部'
    },
    getSortLabel(value) {
      const option = this.sortOptions.find(o => o.value === value)
      return option ? option.label : '默认排序'
    },
    clearSearch() {
      this.searchKeyword = ''
      this.handleSearch()
    },
    resetFilters() {
      this.selectedCategory = ''
      this.priceRange = ''
      this.sortBy = 'default'
      this.searchKeyword = ''
      this.currentPage = 1
      this.getProducts()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.getProducts()
    }
  },
  watch: {
    searchKeyword() {
      this.handleSearch()
      this.handleRouteChange()
    },
    selectedCategory() {
      this.handleRouteChange()
    },
    sortBy() {
      this.currentPage = 1
      this.getProducts()
    }
  },
  created() {
    this.debouncedSearch = debounce(() => {
      this.currentPage = 1
      this.getProducts()
    }, 300)

    this.getCategories()
    this.getProducts()
    
    const { category, keyword } = this.$route.query
    if (category) this.selectedCategory = category
    if (keyword) {
      this.searchKeyword = keyword
      this.handleSearch()
    }
  },
  beforeDestroy() {
    if (this.debouncedSearch) {
      this.debouncedSearch.cancel()
    }
  }
}
</script>

<style scoped>
.products-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(to bottom, #fff, #f8faf5);
}

.main-content {
  flex: 1;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
  box-sizing: border-box;
}

/* 页面标题样式 */
.page-header {
  margin-bottom: 24px;
  background: white;
  padding: 24px 30px;
  border-radius: 16px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 6px;
  height: 100%;
  background: linear-gradient(to bottom, #67C23A, #85ce61);
}

.title-container {
  display: flex;
  flex-direction: column;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 0;
  font-size: 26px;
  font-weight: 600;
  color: #2c3e50;
}

.page-title i {
  font-size: 30px;
  color: #67C23A;
  background: rgba(103, 194, 58, 0.1);
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-subtitle {
  margin-top: 8px;
  font-size: 15px;
  color: #909399;
  padding-left: 64px;
}

.search-box {
  width: 320px;
}

.search-box :deep(.el-input__inner) {
  height: 44px;
  border-radius: 22px;
  padding-left: 20px;
  border: 1px solid #ebeef5;
  transition: all 0.3s ease;
  font-size: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.search-box :deep(.el-input__prefix) {
  left: 15px;
}

.search-box :deep(.el-input__inner:hover) {
  border-color: #c0c4cc;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.search-box :deep(.el-input__inner:focus) {
  border-color: #67C23A;
  box-shadow: 0 2px 12px rgba(103, 194, 58, 0.1);
}

/* 过滤器样式 */
.filter-bar {
  background: white;
  border-radius: 16px;
  padding: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.filter-group {
  display: flex;
  position: relative;
}

.filter-item {
  position: relative;
}

.filter-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 20px;
  background: #ebeef5;
}

.filter-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 16px 24px;
  transition: all 0.3s ease;
  color: #606266;
  font-size: 15px;
  position: relative;
  font-weight: 500;
}

.filter-label:hover, .filter-label.active {
  background: rgba(103, 194, 58, 0.08);
  color: #67C23A;
}

.filter-label.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: #67C23A;
  border-radius: 3px;
}

.filter-actions {
  padding-right: 24px;
}

.reset-btn {
  color: #67C23A;
  font-size: 14px;
  font-weight: 500;
}

.reset-btn i {
  margin-right: 4px;
}

/* 已选择的过滤条件 */
.selected-filters {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: rgba(103, 194, 58, 0.05);
  border-radius: 16px;
  border: 1px dashed rgba(103, 194, 58, 0.3);
}

.selected-filters-title {
  font-size: 14px;
  color: #67C23A;
  font-weight: 500;
  margin-right: 16px;
}

.selected-filters-content {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.selected-filters :deep(.el-tag) {
  padding: 0 12px;
  height: 32px;
  line-height: 30px;
  border-radius: 16px;
  font-size: 13px;
}

.selected-filters :deep(.el-tag .el-tag__close) {
  background-color: transparent;
  color: #67C23A;
  font-weight: bold;
  right: 0;
}

.selected-filters :deep(.el-tag .el-tag__close:hover) {
  background-color: #67C23A;
  color: white;
}

/* 下拉菜单样式优化 */
:deep(.el-dropdown-menu) {
  padding: 8px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

:deep(.el-dropdown-menu__item) {
  padding: 10px 20px;
  font-size: 14px;
  border-radius: 8px;
  margin: 4px 0;
  color: #606266;
  transition: all 0.2s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

:deep(.el-dropdown-menu__item.is-disabled) {
  color: #c0c4cc;
  cursor: not-allowed;
  background-color: transparent;
}

/* 商品网格 */
.products-section {
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  min-height: 500px;
  position: relative;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
}

.product-item {
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  animation: fadeIn 0.6s ease forwards;
  opacity: 0;
}

.product-item:nth-child(1) { animation-delay: 0.05s; }
.product-item:nth-child(2) { animation-delay: 0.1s; }
.product-item:nth-child(3) { animation-delay: 0.15s; }
.product-item:nth-child(4) { animation-delay: 0.2s; }
.product-item:nth-child(5) { animation-delay: 0.25s; }
.product-item:nth-child(6) { animation-delay: 0.3s; }
.product-item:nth-child(7) { animation-delay: 0.35s; }
.product-item:nth-child(8) { animation-delay: 0.4s; }
.product-item:nth-child(9) { animation-delay: 0.45s; }
.product-item:nth-child(10) { animation-delay: 0.5s; }
.product-item:nth-child(11) { animation-delay: 0.55s; }
.product-item:nth-child(12) { animation-delay: 0.6s; }

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 动画效果 */
.fade-list-enter-active, .fade-list-leave-active {
  transition: all 0.3s ease;
}

.fade-list-enter, .fade-list-leave-to {
  opacity: 0;
  transform: translateY(30px);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #909399;
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 20px;
  color: #dcdfe6;
}

.empty-state p {
  font-size: 16px;
  margin: 0 0 24px;
  color: #606266;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

:deep(.el-pagination) {
  padding: 16px 24px;
  background: white;
  border-radius: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

:deep(.el-pagination.is-background .el-pager li) {
  margin: 0 4px;
  border-radius: 50%;
  min-width: 32px;
  transition: all 0.3s ease;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background-color: #67C23A;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled):hover) {
  color: #67C23A;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  border-radius: 50%;
  margin: 0 4px;
}

:deep(.el-select .el-input) {
  margin: 0 8px;
}

:deep(.el-pagination__total) {
  margin-right: 16px;
}

:deep(.el-pagination__jump) {
  margin-left: 16px;
}

/* 响应式布局优化 */
@media (max-width: 1200px) {
  .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 24px;
  }
  
  .products-section {
    padding: 24px;
  }
}

@media (max-width: 992px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .search-box {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px;
  }
  
  .page-header {
    padding: 20px;
    margin-bottom: 20px;
  }
  
  .page-title {
    font-size: 22px;
  }
  
  .page-title i {
    font-size: 24px;
    width: 42px;
    height: 42px;
  }
  
  .page-subtitle {
    padding-left: 56px;
    font-size: 14px;
  }

  .filter-bar {
    flex-direction: column;
  }
  
  .filter-group {
    width: 100%;
    overflow-x: auto;
    padding: 0;
  }
  
  .filter-label {
    padding: 14px 16px;
    white-space: nowrap;
    font-size: 14px;
  }
  
  .filter-actions {
    width: 100%;
    padding: 12px 16px;
    border-top: 1px solid #ebeef5;
    display: flex;
    justify-content: center;
  }

  .selected-filters {
    padding: 12px 16px;
    flex-direction: column;
    align-items: flex-start;
  }
  
  .selected-filters-title {
    margin-bottom: 8px;
    margin-right: 0;
  }
  
  .selected-filters-content {
    width: 100%;
  }

  .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }
  
  .products-section {
    padding: 16px;
    border-radius: 12px;
  }
  
  .pagination-wrapper {
    margin-top: 24px;
  }
  
  :deep(.el-pagination) {
    padding: 12px;
    border-radius: 24px;
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 12px;
  }
  
  .empty-state {
    padding: 60px 0;
  }
  
  .empty-state i {
    font-size: 48px;
  }
}
</style> 