<template>
  <div class="cart-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>购物车管理</h2>
      <el-tag type="info">共 {{ total }} 个商品</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.productName" placeholder="请输入商品名称" clearable></el-input>
        </el-form-item>
        <!-- 管理员可以查看所有用户购物车 -->
        <el-form-item label="用户账号" >
          <el-input v-model="searchForm.userId" placeholder="请输入用户账号" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button size="medium" plain type="primary" @click="handleSearch">查询</el-button>
          <el-button size="medium" plain @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>


    </el-card>

    <!-- 购物车列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="cartItems" border style="width: 100%">
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column label="用户信息" width="180">
          <template slot-scope="scope">
            <div class="user-info">
              <div>用户名：{{ scope.row.user.username }}</div>
              <div>姓名：{{ scope.row.user.name }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品信息" min-width="300">
          <template slot-scope="scope">
            <div class="product-info">
              <el-image 
                :src="'api'+scope.row.product.imageUrl" 
                :preview-src-list="['api'+scope.row.product.imageUrl]"
                fit="cover"
                style="width: 50px; height: 50px">
              </el-image>
              <div class="product-detail">
                <div class="product-name">{{ scope.row.product.name }}</div>
                <div class="product-price">
                  ¥{{ scope.row.product.price.toFixed(2) }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template slot-scope="scope">
            ¥{{ (scope.row.quantity * scope.row.product.price).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="加入时间" width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination 
        v-show="total > 0" 
        :total="total" 
        :page.sync="queryParams.currentPage" 
        :limit.sync="queryParams.size" 
        @pagination="getList" />
    </el-card>
  </div>
</template>

<script>
import Request from '@/utils/request'
import Pagination from '@/components/Pagination/index.vue'
import { formatTime } from '@/utils/time'

export default {
  name: 'CartManager',
  components: {
    Pagination
  },
  inject: ['userInfo'],
  data() {
    return {
      // 搜索表单
      searchForm: {
        productName: '',
        userId: ''
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 购物车列表
      cartItems: [],
      // 总数
      total: 0
    }
  },
  computed: {
    // 判断是否为管理员
    isAdmin() {
      return ['SUPER_ADMIN', 'ADMIN'].includes(this.userInfo.role)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取购物车列表
    async getList() {
      try {
        const params = {
          ...this.queryParams,
          ...this.searchForm
        }
    
        const res = await Request.get('/cart/page', { params })
        if (res.code === '0') {
          this.cartItems = res.data.records
          this.total = res.data.total
        }
      } catch (error) {
        console.error('获取购物车列表失败:', error)
      }
    },
    // 搜索
    handleSearch() {
      this.queryParams.currentPage = 1
      this.getList()
    },
    // 重置搜索
    resetSearch() {
      this.searchForm = {
        productName: '',
        userId: ''
      }
      this.handleSearch()
    },
    // 删除购物车商品
    async handleDelete(item) {
      try {
        await this.$confirm('确认删除该商品?', '提示', {
          type: 'warning'
        })
        const res = await Request.delete(`/cart/${item.id}`)
        if (res.code === '0') {
          this.$message.success('删除成功')
          this.getList()
        }
      } catch (error) {
        console.error('删除失败:', error)
      }
    },
    // 格式化时间
    formatTime
  }
}
</script>

<style scoped>
.cart-manager {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 500;
  color: #1f2f3d;
  margin: 0;
  margin-right: 15px;
}

.operation-area {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.table-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 14px;
  color: #1f2f3d;
}

.product-price {
  font-size: 13px;
  color: #f56c6c;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  color: #606266;
}
</style> 