<template>
  <div class="home" v-if="refresh" :key="refresh">
    <front-header></front-header>
    <div class="main-content">
      <div class="loading-container" v-if="loading">
        <div class="loading-spinner"><i class="el-icon-loading"></i></div>
        <div class="loading-text">加载中...</div>
      </div>

      <div v-else>
        <div class="carousel-notice-section">
          <div class="carousel-wrapper"><front-carousel></front-carousel></div>
          <div class="notice-wrapper"><front-notice></front-notice></div>
        </div>

        <front-category></front-category>

        <div class="section recommend-section">
          <div class="section-header">
            <div class="title-wrapper">
              <div class="title-icon-wrapper"><i class="el-icon-star-on"></i></div>
              <div class="title-content">
                <h2 class="section-title"><span>为您推荐</span></h2>
                <div class="subtitle">精选优质好物，专属于您的推荐</div>
              </div>
            </div>
            <div class="more-btn" @click="$router.push('/products')">
              <span>查看更多</span><i class="el-icon-arrow-right"></i>
            </div>
          </div>
          <div class="product-grid">
            <div class="product-item" v-for="product in recommendProducts" :key="product.id">
              <product-card :product="product" @add-to-cart="handleAddToCart" @toggle-favorite="handleToggleFavorite"></product-card>
            </div>
          </div>
        </div>

        <div class="section new-products-section">
          <div class="section-header">
            <div class="title-wrapper">
              <div class="title-icon-wrapper new-icon"><i class="el-icon-goods"></i></div>
              <div class="title-content">
                <h2 class="section-title"><span>新品尝鲜</span></h2>
                <div class="subtitle">甄选品质新品，快速送达您家</div>
              </div>
            </div>
            <div class="more-btn" @click="$router.push('/products?type=new')">
              <span>查看更多</span><i class="el-icon-arrow-right"></i>
            </div>
          </div>
          <div class="product-grid compact">
            <div class="product-item" v-for="product in newProducts" :key="product.id">
              <product-card :product="product" @add-to-cart="handleAddToCart" @toggle-favorite="handleToggleFavorite"></product-card>
            </div>
          </div>
        </div>
      </div>
    </div>
    <ai-guide-assistant></ai-guide-assistant>
    <front-footer></front-footer>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import FrontCarousel from '@/components/front/FrontCarousel.vue'
import FrontCategory from '@/components/front/FrontCategory.vue'
import FrontNotice from '@/components/front/FrontNotice.vue'
import ProductCard from '@/components/front/ProductCard.vue'
import AiGuideAssistant from '@/components/front/AiGuideAssistant.vue'
import Request from '@/utils/request'

export default {
  name: 'Home',
  components: {
    FrontHeader,
    FrontFooter,
    FrontCarousel,
    FrontCategory,
    FrontNotice,
    ProductCard,
    AiGuideAssistant
  },
  data() {
    return {
      recommendProducts: [],
      newProducts: [],
      isLoggedIn: false,
      refresh: true,
      loading: true
    }
  },
  created() {
    this.getRecommendProducts()
    this.getNewProducts()
  },
  methods: {
    checkLoginStatus() {
      const token = localStorage.getItem('token')
      const userStr = localStorage.getItem('frontUser')
      if (token && userStr) {
        try {
          const user = JSON.parse(userStr)
          if (user.role !== 'USER') {
            this.logout()
            this.$message.warning('请使用普通用户账号访问')
            return
          }
          this.isLoggedIn = true
        } catch (error) {
          this.logout()
        }
      } else {
        this.isLoggedIn = false
      }
    },
    logout() {
      localStorage.removeItem('token')
      localStorage.removeItem('frontUser')
      this.isLoggedIn = false
      window.location.reload()
    },
    async getRecommendProducts() {
      try {
        this.loading = true
        this.checkLoginStatus()
        let products = []
        if (!this.isLoggedIn) {
          const res = await Request.get('/product/page?status=1&size=8')
          if (res.code === '0') {
            products = res.data.records || res.data || []
            products = products.sort(() => Math.random() - 0.5)
          }
        } else {
          const user = JSON.parse(localStorage.getItem('frontUser'))
          const res = await Request.get(`/recommend/user/${user.id}`)
          if (res.code === '0') {
            products = res.data.records || res.data || []
          }
          products = await this.fillFavoriteStatus(products, user.id)
        }
        this.recommendProducts = products.map(product => ({ ...product, isFavorite: product.isFavorite || false })).slice(0, 8)
      } catch (error) {
        console.error('获取推荐商品失败:', error)
      } finally {
        setTimeout(() => {
          this.loading = false
        }, 400)
      }
    },
    async getNewProducts() {
      try {
        const res = await Request.get('/product/page?status=1&sort=updatedAt,desc&size=4')
        if (res.code === '0') {
          const records = res.data.records || res.data || []
          this.newProducts = records.slice(0, 4).map(product => ({ ...product, isNew: true }))
        }
      } catch (error) {
        console.error('获取新品失败:', error)
      }
    },
    async fillFavoriteStatus(products, userId) {
      try {
        const favRes = await Request.get(`/favorite/user/${userId}`)
        if (favRes.code === '0') {
          const favorites = favRes.data || []
          return products.map(product => ({
            ...product,
            isFavorite: favorites.some(f => f.productId === product.id && f.status === 1)
          }))
        }
      } catch (error) {
        console.error('获取收藏状态失败:', error)
      }
      return products.map(product => ({ ...product, isFavorite: false }))
    },
    handleAddToCart() {},
    handleToggleFavorite() {}
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: #f7f9f5;
}

.main-content {
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
  padding: 24px 0 48px;
}

.carousel-notice-section {
  display: flex;
  gap: 24px;
  height: 360px;
  margin-bottom: 32px;
}

.carousel-wrapper {
  flex: 2;
  min-width: 0;
  height: 100%;
}

.notice-wrapper {
  flex: 1;
  min-width: 320px;
  max-width: 380px;
  height: 100%;
}

.section {
  margin-top: 36px;
  padding: 28px 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 14px;
}

.title-icon-wrapper {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #fff;
  font-size: 22px;
  background: linear-gradient(135deg, #67c23a, #95d475);
}

.new-icon {
  background: linear-gradient(135deg, #ff6b81, #ff4757);
}

.section-title {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  margin-top: 4px;
  color: #909399;
  font-size: 14px;
}

.more-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #67c23a;
  cursor: pointer;
  font-weight: 600;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.loading-container {
  min-height: 500px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  font-size: 48px;
  color: #67c23a;
  animation: pulse 1.5s infinite;
}

.loading-text {
  margin-top: 16px;
  color: #909399;
}

@keyframes pulse {
  0%, 100% { opacity: 0.65; transform: scale(0.96); }
  50% { opacity: 1; transform: scale(1.05); }
}

@media (max-width: 1200px) {
  .carousel-notice-section {
    flex-direction: column;
    height: auto;
  }

  .carousel-wrapper,
  .notice-wrapper {
    width: 100%;
    max-width: none;
    height: 320px;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .main-content {
    width: calc(100% - 20px);
  }

  .section-header {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
