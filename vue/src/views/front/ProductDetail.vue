<template>
  <div class="product-detail">
    <front-header></front-header>
    <div class="main-content">
      <!-- 面包屑导航 -->
      <div class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/products' }">全部商品</el-breadcrumb-item>
          <el-breadcrumb-item>{{ product.name || '加载中...' }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 加载状态显示 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton style="width: 100%" animated>
          <template slot="template">
            <div style="display: flex; gap: 48px; padding: 24px;">
              <el-skeleton-item variant="image" style="width: 400px; height: 400px; border-radius: 8px;" />
              <div style="flex: 1">
                <el-skeleton-item variant="p" style="width: 50%; height: 24px; margin-bottom: 20px" />
                <el-skeleton-item variant="h3" style="width: 100%; height: 36px; margin-bottom: 24px" />
                <el-skeleton-item variant="text" style="width: 40%; height: 40px; margin-bottom: 24px" />
                <el-skeleton-item variant="p" style="width: 70%; height: 24px; margin-bottom: 16px" />
                <el-skeleton-item variant="p" style="width: 60%; height: 24px; margin-bottom: 32px" />
                <el-skeleton-item variant="text" style="width: 100%; height: 48px; margin-top: 32px" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>

      <!-- 商品基本信息区域 -->
      <div v-else class="product-info-section">
        <div class="product-main">
          <div class="image-container">
            <el-image 
              :src="product.imageUrl?.startsWith('http') ? product.imageUrl : `/api${product.imageUrl}`"
              fit="cover"
              :preview-src-list="[product.imageUrl?.startsWith('http') ? product.imageUrl : `/api${product.imageUrl}`]"
            >
              <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline"></i>
              </div>
            </el-image>
            <div class="product-badges">
              <div class="badge new" v-if="product.isNew">新品上市</div>
              <div class="badge discount" v-if="product.isDiscount">特惠</div>
            </div>
          </div>
          
          <div class="product-info">
            <div class="category-tag">{{ product.category.name || '商品' }}</div>
            <h1 class="product-name">{{ product.name }}</h1>
            
            <div class="price-section">
              <template v-if="product.isDiscount">
                <span class="discount-price">¥{{ product.discountPrice }}</span>
                <span class="original-price">¥{{ product.price }}</span>
                <span class="discount-tag">{{ ((1 - product.discountPrice/product.price) * 100).toFixed(0) }}% OFF</span>
              </template>
              <span v-else class="current-price">¥{{ product.price }}</span>
            </div>
            
            <div class="sales-info">
              <div class="info-item">
                <i class="el-icon-sold-out"></i>
                <span>已售 {{ product.salesCount }} 件</span>
              </div>
              <div class="info-item">
                <i class="el-icon-box"></i>
                <span>库存 {{ product.stock }} 件</span>
              </div>
              <div class="info-item" v-if="product.placeOfOrigin">
                <i class="el-icon-location-outline"></i>
                <span>产地: {{ product.placeOfOrigin }}</span>
              </div>
              <div class="stock-status" :class="{ 'low': product.stock < 10 }">
                {{ product.stock > 0 ? (product.stock < 10 ? '库存紧张' : '库存充足') : '暂时缺货' }}
              </div>
            </div>
            
            <div class="divider">
              <span class="divider-text">选择数量</span>
            </div>
            
            <div class="quantity-section">
              <el-input-number 
                v-model="quantity" 
                :min="1" 
                :max="product.stock"
                size="small"
                :disabled="!product.stock"
              >
              </el-input-number>
            </div>
            
            <div class="action-buttons">
              <el-button 
                type="primary" 
                size="large" 
                @click="handleAddToCart"
                :disabled="!product.stock"
                class="cart-btn"
              >
                <i class="el-icon-shopping-cart-2"></i>
                加入购物车
              </el-button>
              
              <el-button 
                type="danger" 
                size="large" 
                @click="handleBuyNow"
                :disabled="!product.stock"
                class="buy-btn"
              >
                立即购买
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品详情和评价区域 -->
      <div class="detail-tabs">
        <el-tabs v-model="activeTab" type="card">
          <el-tab-pane label="商品详情" name="detail">
            <div v-if="loading" class="loading-detail">
              <el-skeleton :rows="10" animated />
            </div>
            <div v-else class="detail-content rich-text-content" v-html="sanitizedDescription"></div>
          </el-tab-pane>
          <el-tab-pane label="商品评价" name="reviews">
            <div v-if="reviewsLoading" class="loading-reviews">
              <el-skeleton :rows="5" animated />
            </div>
            <div v-else class="reviews-list">
              <div v-if="reviews.length" class="review-items">
                <div v-for="review in reviews" :key="review.id" class="review-item">
                  <div class="review-header">
                    <div class="reviewer-info">
                      <el-avatar :size="40" :src="review.user.avatar">
                        {{ review.user.username.charAt(0).toUpperCase() }}
                      </el-avatar>
                      <span class="reviewer-name">{{ review.user.username }}</span>
                    </div>
                    <el-rate
                      v-model="review.rating"
                      disabled
                      show-score
                      text-color="#ff9900">
                    </el-rate>
                  </div>
                  <div class="review-content">{{ review.content }}</div>
                  <div class="review-time">{{ formatTime(review.createdAt) }}</div>
                </div>
              </div>
              <el-empty v-else description="暂无评价"></el-empty>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
    <front-footer></front-footer>

    <!-- 添加确认订单对话框 -->
    <el-dialog title="确认订单" :visible.sync="orderDialogVisible" width="500px" custom-class="order-dialog">
      <!-- 商品信息 -->
      <div class="confirm-product-info">
        <el-image :src="product.imageUrl?.startsWith('http') ? product.imageUrl : `/api${product.imageUrl}`" 
          fit="cover" class="confirm-product-image">
        </el-image>
        <div class="confirm-product-detail">
          <div class="confirm-product-name">{{ product.name }}</div>
          <div class="confirm-product-price">
            <span class="price">¥{{ product.isDiscount ? product.discountPrice : product.price }}</span>
            <span class="quantity">x {{ quantity }}</span>
          </div>
        </div>
      </div>

      <!-- 收货地址选择 -->
      <div class="address-section">
        <div class="section-title">收货地址</div>
        <div v-if="addressesLoading" class="loading-addresses">
          <el-skeleton :rows="2" animated />
        </div>
        <div v-else class="address-content">
          <el-select v-model="selectedAddressId" placeholder="请选择收货地址" class="address-select">
            <el-option 
              v-for="addr in addresses" 
              :key="addr.id" 
              :label="formatAddress(addr)" 
              :value="addr.id"
            >
              <div class="address-option">
                <div class="receiver-info">
                  <span class="phone">{{ formatAddress(addr) }}</span>
                </div>
                <div class="address-detail">{{ addr.address }}</div>
              </div>
            </el-option>
          </el-select>
          <el-button type="text" icon="el-icon-plus" @click="$router.push('/user-center')">
            添加新地址
          </el-button>
        </div>
      </div>

      <!-- 订单总价 -->
      <div class="order-total">
        <span>总计：</span>
        <span class="total-price">
          ¥{{ ((product.isDiscount ? product.discountPrice : product.price) * quantity).toFixed(2) }}
        </span>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmOrder" :disabled="!selectedAddressId">
          提交订单
        </el-button>
      </div>
    </el-dialog>

    <!-- 全局加载遮罩 -->
    <el-loading 
      :visible.sync="submitLoading" 
      fullscreen 
      element-loading-text="处理中..." 
      element-loading-spinner="el-icon-loading"
      element-loading-background="rgba(0, 0, 0, 0.7)">
    </el-loading>
  </div>
</template>

<script>
import Request from '@/utils/request'
import { formatTime } from '@/utils/time'
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import DOMPurify from 'dompurify'

export default {
  name: 'ProductDetail',
  components: {
    FrontHeader,
    FrontFooter
  },
  data() {
    return {
      userInfo: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      product: {},
      quantity: 1,
      reviews: [],
      orderDialogVisible: false,
      selectedAddressId: null,
      addresses: [],
      activeTab: 'detail',
      loading: true,
      reviewsLoading: true,
      addressesLoading: false,
      submitLoading: false
    }
  },
  created() {
    this.getProductDetail()
    this.getProductReviews()
  },
  mounted() {
    this.userInfo = JSON.parse(localStorage.getItem('frontUser') || '{}')
  },
  methods: {
    formatTime,
    async getProductDetail() {
      try {
        this.loading = true
        const res = await Request.get(`/product/${this.$route.params.id}`)
        if (res.code === '0') {
          this.product = res.data
        }
      } catch (error) {
        console.error('获取商品详情失败:', error)
        this.$message.error('获取商品详情失败')
      } finally {
        this.loading = false
      }
    },
    async getProductReviews() {
      try {
        this.reviewsLoading = true
        const res = await Request.get(`/review/product/${this.$route.params.id}`, {
          params: {
            status: 1  // 只获取已通过的评价
          }
        })
        if (res.code === '0') {
          this.reviews = res.data
        }
      } catch (error) {
        console.error('获取商品评价失败:', error)
      } finally {
        this.reviewsLoading = false
      }
    },
    isLogin() {
      const userStr = localStorage.getItem('frontUser')
      if (!userStr) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
    },
    async handleAddToCart() {
      try {
        this.isLogin()

        this.submitLoading = true
        const userId = this.userInfo.id

        const data = {
          userId,
          productId: this.product.id,
          quantity: this.quantity
        }

        const res = await Request.post('/cart', data)
        if (res.code === '0') {
          this.$message.success('已添加到购物车')
        }
      } catch (error) {
        console.error('添加到购物车失败:', error)
        this.$message.error('添加失败')
      } finally {
        this.submitLoading = false
      }
    },
    async handleBuyNow() {
      try {
        this.isLogin()

        // 获取用户地址列表
        this.addressesLoading = true
        await this.getAddresses()
        // 显示确认订单对话框
        this.orderDialogVisible = true

      } catch (error) {
        console.error('操作失败:', error)
        this.$message.error('操作失败')
      } finally {
        this.addressesLoading = false
      }
    },
    async getAddresses() {
      try {
        const userId = this.userInfo.id
        const res = await Request.get(`/address/user/${userId}`)
        if (res.code === '0') {
          this.addresses = res.data
          // 如果只有一个地址，自动选中
          if (this.addresses.length === 1) {
            this.selectedAddressId = this.addresses[0].id
          }
        }
      } catch (error) {
        console.error('获取地址列表失败:', error)
        this.$message.error('获取地址列表失败')
      }
    },
    formatAddress(addr) {
      return `${addr.phone} ${addr.address}`
    },
    async confirmOrder() {
      try {
        if (!this.selectedAddressId) {
          this.$message.warning('请选择收货地址')
          return
        }

        this.submitLoading = true
        const selectedAddress = this.addresses.find(addr => addr.id === this.selectedAddressId)
        const orderData = {
          userId: this.userInfo.id,
          productId: this.product.id,
          quantity: this.quantity,
          price: this.product.isDiscount ? this.product.discountPrice : this.product.price,
          totalPrice: (this.product.isDiscount ? this.product.discountPrice : this.product.price) * this.quantity,
          recvAddress: selectedAddress.address,
          recvPhone: selectedAddress.phone,
          recvName: selectedAddress.receiver
        }

        const res = await Request.post('/order', orderData)
        if (res.code === '0') {
          this.$message.success('下单成功')
          this.orderDialogVisible = false
          this.$router.push('/order')
        }
      } catch (error) {
        console.error('创建订单失败:', error)
        this.$message.error('下单失败')
      } finally {
        this.submitLoading = false
      }
    }
  },
  computed: {
    sanitizedDescription() {
      return this.product.description ? DOMPurify.sanitize(this.product.description) : ''
    }
  }
}
</script>

<style scoped>
.product-detail {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.main-content {
  flex: 1;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.breadcrumb {
  margin-bottom: 24px;
  padding: 0 4px;
}

.product-info-section {
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.product-main {
  display: flex;
  gap: 48px;
  padding: 24px;
}

.image-container {
  position: relative;
  width: 400px;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-image) {
  width: 100%;
  height: 100%;
}

:deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 1;
}

.badge {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: white;
}

.badge.new {
  background: linear-gradient(135deg, #67C23A, #85ce61);
}

.badge.discount {
  background: linear-gradient(135deg, #ff4757, #ff6b81);
}

.discount-tag {
  background: #ff4757;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 12px;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.category-tag {
  display: inline-block;
  padding: 4px 12px;
  background: rgba(103, 194, 58, 0.1);
  color: #67C23A;
  border-radius: 20px;
  font-size: 14px;
  margin-bottom: 16px;
}

.product-name {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  line-height: 1.4;
}

.price-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.current-price,
.discount-price {
  font-size: 32px;
  color: #ff4757;
  font-weight: 600;
}

.original-price {
  font-size: 18px;
  color: #909399;
  text-decoration: line-through;
}

.sales-info {
  display: flex;
  gap: 32px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.info-item i {
  font-size: 16px;
  color: #909399;
}

.divider {
  position: relative;
  height: 1px;
  background: #ebeef5;
  margin: 24px 0;
}

.divider-text {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  background: white;
  padding-right: 12px;
  color: #909399;
  font-size: 14px;
}

.quantity-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

.cart-btn,
.buy-btn {
  flex: 1;
  height: 48px;
  font-size: 16px;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.cart-btn {
  background: linear-gradient(135deg, #67C23A, #5aad32);
  border: none;
}

.cart-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.2);
}

.buy-btn {
  background: linear-gradient(135deg, #f56c6c, #e64242);
  border: none;
}

.buy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.2);
}

.detail-tabs {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 24px;
}

:deep(.el-tabs__item.is-active) {
  color: #67C23A;
}

:deep(.el-tabs__active-bar) {
  background-color: #67C23A;
}

.review-items {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px;
}

.review-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reviewer-name {
  font-weight: 500;
  color: #303133;
}

.review-content {
  color: #606266;
  line-height: 1.6;
  margin: 12px 0;
}

.review-time {
  color: #909399;
  font-size: 12px;
}

/* 确认订单对话框样式 */
.confirm-product-info {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 24px;
}

.confirm-product-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
}

.confirm-product-detail {
  flex: 1;
}

.confirm-product-name {
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
}

.confirm-product-price {
  display: flex;
  align-items: center;
  gap: 8px;
}

.confirm-product-price .price {
  color: #ff4757;
  font-weight: 600;
}

.confirm-product-price .quantity {
  color: #909399;
}

.address-section {
  margin: 24px 0;
}

.section-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 16px;
}

.address-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.address-select {
  width: 100%;
}

.address-option {
  padding: 8px 0;
}

.receiver-info {
  margin-bottom: 4px;
}

.receiver-info .phone {
  color: #606266;
  font-size: 14px;
}

.address-detail {
  color: #909399;
  font-size: 13px;
}

.order-total {
  text-align: right;
  padding: 16px;
  border-top: 1px solid #ebeef5;
  font-size: 14px;
  color: #606266;
}

.total-price {
  color: #ff4757;
  font-size: 20px;
  font-weight: 600;
  margin-left: 8px;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

.rich-text-content {
  padding: 24px;
  color: #606266;
  line-height: 1.8;
}

/* 段落样式 */
:deep(.rich-text-content p) {
  margin: 1em 0;
  line-height: 1.8;
}

/* 标题样式 */
:deep(.rich-text-content h1),
:deep(.rich-text-content h2),
:deep(.rich-text-content h3),
:deep(.rich-text-content h4),
:deep(.rich-text-content h5) {
  margin: 1.5em 0 0.8em;
  font-weight: 600;
  line-height: 1.4;
}

/* 列表样式 */
:deep(.rich-text-content ul),
:deep(.rich-text-content ol) {
  margin: 1em 0;
  padding-left: 2em;
}

:deep(.rich-text-content li) {
  margin: 0.5em 0;
}

/* 引用样式 */
:deep(.rich-text-content blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #409eff;
  background-color: #ecf5ff;
  color: #606266;
}

/* 代码样式 */
:deep(.rich-text-content pre) {
  margin: 1em 0;
  padding: 1em;
  background-color: #f6f8fa;
  border-radius: 4px;
  overflow-x: auto;
}

:deep(.rich-text-content code) {
  background-color: #f6f8fa;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
}

/* 表格样式 */
:deep(.rich-text-content table) {
  width: 100%;
  margin: 1em 0;
  border-collapse: collapse;
  border: 1px solid #dcdfe6;
}

:deep(.rich-text-content th),
:deep(.rich-text-content td) {
  padding: 12px;
  border: 1px solid #dcdfe6;
  text-align: left;
}

:deep(.rich-text-content th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

:deep(.rich-text-content tr:nth-child(even)) {
  background-color: #fafafa;
}

:deep(.rich-text-content tr:hover) {
  background-color: #f5f7fa;
}

/* 图片样式 */
:deep(.rich-text-content img) {
  max-width: 100%;
  height: auto;
  margin: 1em 0;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 链接样式 */
:deep(.rich-text-content a) {
  color: #409eff;
  text-decoration: none;
  transition: color 0.3s;
}

:deep(.rich-text-content a:hover) {
  color: #66b1ff;
  text-decoration: underline;
}

/* 分割线样式 */
:deep(.rich-text-content hr) {
  margin: 1.5em 0;
  border: none;
  border-top: 1px solid #dcdfe6;
}

/* 文本对齐 */
:deep(.rich-text-content .w-e-text-align-left) {
  text-align: left;
}

:deep(.rich-text-content .w-e-text-align-center) {
  text-align: center;
}

:deep(.rich-text-content .w-e-text-align-right) {
  text-align: right;
}

:deep(.rich-text-content .w-e-text-align-justify) {
  text-align: justify;
}

/* 字体大小 */
:deep(.rich-text-content .w-e-text-small) {
  font-size: 0.875em;
}

:deep(.rich-text-content .w-e-text-large) {
  font-size: 1.25em;
}

/* 响应式布局优化 */
@media (max-width: 768px) {
  .product-main {
    flex-direction: column;
    gap: 24px;
  }

  .image-container {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
    max-width: 500px;
    margin: 0 auto;
  }

  .product-info {
    padding: 0;
  }
}

/* 加载动画容器样式 */
.loading-container {
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.loading-detail,
.loading-reviews,
.loading-addresses {
  padding: 20px;
}
</style> 