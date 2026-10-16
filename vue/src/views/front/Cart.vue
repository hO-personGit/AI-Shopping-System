<template>
  <div class="cart-page">
    <front-header></front-header>
    <div class="main-content">
      <!-- 购物车标题 -->
      <div class="page-header">
        <h2>我的购物车</h2>
        <div class="cart-count">共 {{ totalItems }} 件商品</div>
      </div>

      <!-- 购物车主体 -->
      <div class="cart-content" v-loading="loading">
        <!-- 空购物车提示 -->
        <el-empty v-if="!loading && cartItems.length === 0" description="购物车是空的">
          <el-button type="primary" @click="$router.push('/products')">去购物</el-button>
        </el-empty>

        <!-- 购物车列表 -->
        <template v-else>
          <!-- 表头 -->
          <div class="cart-header">
            <el-checkbox v-model="allSelected" @change="handleSelectAll">全选</el-checkbox>
            <span class="header-item">商品信息</span>
            <span class="header-item">单价</span>
            <span class="header-item">数量</span>
            <span class="header-item">小计</span>
            <span class="header-item">操作</span>
          </div>

          <!-- 购物车商品列表 -->
          <div class="cart-list">
            <div v-for="item in cartItems" :key="item.id" class="cart-item">
              <el-checkbox v-model="item.selected" @change="handleItemSelect"></el-checkbox>

              <div class="product-info">
                <el-image :src="'api'+item.product.imageUrl" fit="cover" class="product-image" @click="$router.push(`/product/${item.product.id}`)">
                  <div slot="error" class="image-slot">
                    <i class="el-icon-picture-outline"></i>
                  </div>
                </el-image>
                <div class="product-detail">
                  <div class="product-name">{{ item.product.name }}</div>
                  <div class="product-stock">库存: {{ item.product.stock }}</div>
                </div>
              </div>

              <div class="product-price">
                <span class="current-price">
                  ¥{{ item.product.isDiscount ? item.product.discountPrice : item.product.price }}
                </span>
                <span class="original-price" v-if="item.product.isDiscount">
                  ¥{{ item.product.price }}
                </span>
              </div>

              <div class="product-quantity">
                <el-input-number v-model="item.quantity" :min="1" :max="item.product.stock" size="small"
                  @change="(value) => handleQuantityChange(item, value)"></el-input-number>
              </div>

              <div class="product-subtotal">
                ¥{{ calculateSubtotal(item).toFixed(2) }}
              </div>

              <div class="product-actions">
                <el-button type="text" class="delete-btn" @click="handleDelete(item)">删除</el-button>
              </div>
            </div>
          </div>

          <!-- 收货地址选择区域 (新增) -->
          <div class="address-section" v-if="cartItems.some(item => item.selected)">
            <div class="section-title">收货地址</div>
            <div class="address-content">
              <el-select v-model="selectedAddressId" placeholder="请选择收货地址" class="address-select">
                <el-option v-for="addr in addresses" :key="addr.id" :label="formatAddress(addr)" :value="addr.id">
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

          <!-- 购物车底部 -->
          <div class="cart-footer">
            <div class="footer-left">
              <el-checkbox v-model="allSelected" @change="handleSelectAll">全选</el-checkbox>
              <el-button type="text" class="delete-btn" @click="handleBatchDelete">删除选中商品</el-button>
            </div>
            <div class="footer-right">
              <div class="total-info">
                <span>已选择 {{ selectedCount }} 件商品</span>
                <span class="total-price">
                  合计：<em>¥{{ totalPrice.toFixed(2) }}</em>
                </span>
              </div>
              <el-button type="primary" size="medium" :disabled="selectedCount === 0 || !selectedAddressId" @click="handleCheckout">
                结算
              </el-button>
            </div>
          </div>
        </template>
      </div>
    </div>
    <front-footer></front-footer>
  </div>
</template>

<script>
import FrontHeader from '@/components/front/FrontHeader.vue'
import FrontFooter from '@/components/front/FrontFooter.vue'
import Request from '@/utils/request'

export default {
  name: 'Cart',
  components: {
    FrontHeader,
    FrontFooter
  },
  data() {
    return {
      userInfo: JSON.parse(localStorage.getItem('frontUser') || '{}'),
      loading: false,
      cartItems: [],
      allSelected: false,
      selectedAddressId: null,
      addresses: []
    }
  },
  computed: {
    totalItems() {
      return this.cartItems.length
    },
    selectedCount() {
      return this.cartItems.filter(item => item.selected).length
    },
    totalPrice() {
      return this.cartItems
        .filter(item => item.selected)
        .reduce((total, item) => {
          const price = item.product.isDiscount === 1 ? item.product.discountPrice : item.product.price
          return total + price * item.quantity
        }, 0)
    }
  },
 
  methods: {
    // 获取购物车数据
    async getCartItems() {
      this.loading = true
      try {
        const userId = this.userInfo.id
        const res = await Request.get(`/cart/user/${userId}`)
        if (res.code === '0') {
          this.cartItems = res.data ? res.data.map(item => ({
            ...item,
            selected: false
          })) : []
          // 如果购物车为空，确保显示空状态
          if (!res.data || res.data.length === 0) {
            this.cartItems = []
          }
        }
      } catch (error) {
        console.error('获取购物车失败:', error)
        this.$message.error('获取购物车失败')
      } finally {
        this.loading = false
      }
    },
    // 更新商品数量
    async handleQuantityChange(item, value) {
      const originalQuantity = item.quantity
      try {
        await Request.put(`/cart/${item.id}?quantity=${value}`)
        this.$message.success('更新成功')
      } catch (error) {
        console.error('更新数量失败:', error)
        this.$message.error('更新数量失败')
        // 恢复原数量
        item.quantity = originalQuantity
      }
    },
    // 删除
    async handleDelete(item) {
      try {
        await this.$confirm('确定要删除该商品吗？', '提示', {
          type: 'warning'
        })
         Request.delete(`/cart/${item.id}`).then(res=>{
          if(res.code === '0'){
            this.$message.success('删除成功')
            // 更新本地数据以立即反映删除操作
            const index = this.cartItems.findIndex(i => i.id === item.id)
            if (index !== -1) {
              this.cartItems.splice(index, 1)
            }
            // 如果已经没有商品，确保更新全选状态
            if (this.cartItems.length === 0) {
              this.allSelected = false
            }
          }
         })
       
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },
    // 批量删除
    async handleBatchDelete(isCheckout = false) {
      try {
        const selectedItems = this.cartItems.filter(item => item.selected)
        if (selectedItems.length === 0 && !isCheckout) {
          this.$message.warning('请选择要删除的商品')
          return
        }

        if (!isCheckout) {
          await this.$confirm('确定要删除选中的商品吗？', '提示', {
            type: 'warning'
          })
        }

        const ids = selectedItems.map(item => item.id).join(',')
        const res = await Request.delete(`/cart/batch?ids=${ids}`)
        if (res.code === '0') {
          if (!isCheckout) {
            this.$message.success('删除成功')
          }
          // 直接从本地列表中移除被删除的商品
          for (const item of selectedItems) {
            const index = this.cartItems.findIndex(i => i.id === item.id)
            if (index !== -1) {
              this.cartItems.splice(index, 1)
            }
          }
          // 如果没有商品了，重置全选状态
          if (this.cartItems.length === 0) {
            this.allSelected = false
          }
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },
    // 全选/取消全选
    handleSelectAll(val) {
      this.cartItems.forEach(item => {
        item.selected = val
      })
    },
    // 单个商品选择
    handleItemSelect() {
      this.allSelected = this.cartItems.every(item => item.selected)
    },
    // 结算
    async handleCheckout() {
      if (this.addresses.length === 0) {
        this.$message.warning('您还没有添加收货地址')
        this.$router.push('/user-center')
        return
      }
      
      if (!this.selectedAddressId) {
        this.$message.warning('请选择收货地址')
        return
      }

      try {
        const selectedItems = this.cartItems.filter(item => item.selected)
        const selectedAddress = this.addresses.find(addr => addr.id === this.selectedAddressId)
        
        // 为每个选中的商品创建单独的订单
        const orderPromises = selectedItems.map(item => {
          const orderData = {
            userId: this.userInfo.id,
            productId: item.product.id,
            quantity: item.quantity,
            price: item.product.isDiscount === 1 
              ? item.product.discountPrice 
              : item.product.price,
            totalPrice: this.calculateSubtotal(item),
            recvAddress: selectedAddress.address,
            recvPhone: selectedAddress.phone,
            recvName: selectedAddress.receiver
          }
          return Request.post('/order', orderData)
        })

        const results = await Promise.all(orderPromises)
        const hasError = results.some(res => res.code !== '0')

        if (!hasError) {
          this.$message.success('下单成功')
          await this.handleBatchDelete(true)
          this.$router.push('/order')
        } else {
          throw new Error('部分订单创建失败')
        }
      } catch (error) {
        console.error('下单失败:', error)
        this.$message.error('下单失败')
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
    // 添加小计计算方法
    calculateSubtotal(item) {
      const price = item.product.isDiscount === 1 ? item.product.discountPrice : item.product.price
      return price * item.quantity
    }
  },
  created() {
    const userInfo = localStorage.getItem('frontUser')
    if(!userInfo){
      this.$message.warning('请先登录')
      this.$router.push('/login')
    }
    else{
      this.userInfo = JSON.parse(userInfo)
      this.getCartItems()
      this.getAddresses()
    }
  },
  mounted() {
 
  }
}
</script>

<style scoped>
/* 整体布局样式 */
.cart-page {
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

.cart-count {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  background: rgba(103, 194, 58, 0.1);
  padding: 6px 16px;
  border-radius: 20px;
}

/* 购物车内容样式 */
.cart-content {
  background: white;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  min-height: 400px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

/* 表头样式 */
.cart-header {
  display: grid;
  grid-template-columns: 80px 3fr 1fr 1fr 1fr 1fr;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(to right, #f8faf5, #fff);
  border-radius: 12px 12px 0 0;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 商品项样式 */
.cart-item {
  display: grid;
  grid-template-columns: 80px 3fr 1fr 1fr 1fr 1fr;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
  transition: all 0.3s ease;
  position: relative;
}

.cart-item:hover {
  background: linear-gradient(to right, rgba(103, 194, 58, 0.02), transparent);
}

/* 商品信息样式 */
.product-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.product-image:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.product-detail {
  flex: 1;
}

.product-name {
  font-size: 15px;
  color: #2c3e50;
  margin-bottom: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.3s ease;
  line-height: 1.4;
}

.product-name:hover {
  color: #67c23a;
}

.product-stock {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  padding: 2px 8px;
  background: #f8f9fa;
  border-radius: 4px;
  display: inline-block;
}

/* 价格样式 */
.product-price {
  color: #ff6b6b;
  font-weight: 600;
  font-size: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.original-price {
  font-size: 12px;
  color: #909399;
  text-decoration: line-through;
}

.product-subtotal {
  color: #ff6b6b;
  font-weight: 600;
  font-size: 18px;
}

/* 操作按钮样式 */
.delete-btn {
  color: #909399;
  transition: all 0.3s ease;
  font-size: 14px;
}

.delete-btn:hover {
  color: #ff6b6b;
  transform: translateY(-1px);
}

/* 底部样式 */
.cart-footer {
  padding: 20px 24px;
  background: linear-gradient(to right, #f8faf5, #fff);
  border-radius: 0 0 12px 12px;
  border-top: 1px solid #ebeef5;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total-info {
  text-align: right;
  font-size: 15px;
  color: #606266;
}

.total-price {
  margin-left: 24px;
  font-size: 15px;
}

.total-price em {
  font-style: normal;
  color: #ff6b6b;
  font-size: 24px;
  font-weight: 600;
}

/* Checkbox样式优化 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #67c23a;
  border-color: #67c23a;
  transition: all 0.3s ease;
}

:deep(.el-checkbox__inner:hover) {
  border-color: #67c23a;
}

/* 数量输入框样式 */
:deep(.el-input-number) {
  width: 120px;
  border-radius: 4px;
  overflow: hidden;
}

:deep(.el-input-number .el-input__inner) {
  text-align: center;
  border: 1px solid #dcdfe6;
  transition: all 0.3s ease;
}

:deep(.el-input-number .el-input__inner:hover) {
  border-color: #67c23a;
}

/* 结算按钮样式 */
.footer-right .el-button--primary {
  padding: 10px 24px;
  font-size: 14px;
  border-radius: 6px;
  background: linear-gradient(135deg, #67c23a, #85ce61);
  border: none;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.footer-right .el-button--primary::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #85ce61, #67c23a);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.footer-right .el-button--primary:hover {
  transform: translateY(-1px);
}

.footer-right .el-button--primary:hover::before {
  opacity: 1;
}

.footer-right .el-button--primary span {
  position: relative;
  z-index: 1;
}

/* 空购物车样式 */
:deep(.el-empty) {
  padding: 60px 0;
}

:deep(.el-empty .el-button) {
  margin-top: 20px;
  padding: 12px 30px;
  font-size: 15px;
  border-radius: 24px;
  background: linear-gradient(135deg, #67c23a, #85ce61);
  border: none;
  transition: all 0.3s ease;
}

:deep(.el-empty .el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

/* 加载状态样式 */
:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

/* 收货地址区域样式 */
.address-section {
  padding: 20px 30px;
  border-bottom: 1px solid #f0f2f5;
  background: #fafafa;
}

.section-title {
  font-size: 16px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 16px;
}

.address-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.address-select {
  width: 400px;
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

.current-price {
  color: #ff4757;
  font-weight: 600;
}

.original-price {
  color: #999;
  font-size: 12px;
  text-decoration: line-through;
  margin-left: 8px;
}
</style> 