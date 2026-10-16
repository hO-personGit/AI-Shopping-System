<template>
  <div class="review-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>评价管理</h2>
      <el-tag type="info">共 {{ total }} 条评价</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.productName" placeholder="请输入商品名称" clearable></el-input>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable></el-input>
        </el-form-item>
        <el-form-item label="评价状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="不通过" :value="0"></el-option>
            <el-option label="已通过" :value="1"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="medium" plain type="primary" @click="handleSearch">查询</el-button>
          <el-button size="medium" plain @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>


    </el-card>

    <!-- 评价列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="reviews" border style="width: 100%">
        <!-- <el-table-column type="selection" width="55"></el-table-column> -->
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
        <el-table-column label="评分" width="150">
          <template slot-scope="scope">
            <el-rate
              v-model="scope.row.rating"
              disabled
              show-score
              text-color="#ff9900">
            </el-rate>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200">
          <template slot-scope="scope">
            <div class="review-content">{{ scope.row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="评价时间" width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="userInfo.role==='ADMIN'||userInfo.role==='SUPER_ADMIN'" prop="status" label="审核状态" width="120">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
      
              @change="handleStatusChange(scope.row)"
              active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </template>
        </el-table-column>
        <el-table-column v-if="userInfo.role==='ADMIN'||userInfo.role==='SUPER_ADMIN'" label="操作" width="100" fixed="right">
          <template slot-scope="scope">
            <el-button 
              type="text"
              class="danger-text"
              size="small" 
              @click="handleDelete(scope.row)">删除</el-button>
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
import { NULL } from 'sass';

export default {
  name: 'ReviewManager',
  inject: ['userInfo'],
  components: {
    Pagination
  },
  data() {
    return {
      // 搜索表单
      searchForm: {
        productName: '',
        username: '',
        status: ''
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 评价列表
      reviews: [],
      // 总数
      total: 0
    }
  },
  created() {
    this.getList()
  },
  methods: {
    formatTime,
    // 获取评价列表
    async getList() {
      try {
        const merchantId = this.userInfo.role==='MERCHANT' ? this.userInfo.id : null
        console.log("merchantId",this.userInfo.id)
        const params = {
          ...this.queryParams,
          ...this.searchForm,
          merchantId
        }
        const res = await Request.get('/review/page', { params })
        if (res.code === '0') {
          this.reviews = res.data.records
          this.total = res.data.total
        }
      } catch (error) {
        console.error('获取评价列表失败:', error)
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
        username: '',
        status: ''
      }
      this.handleSearch()
    },
    // 处理状态变更
    async handleStatusChange(row) {
      try {
        const res = await Request.put(`/review/${row.id}/status?status=${row.status}`)
        if (res.code === '0') {
          this.$message.success(`审核${row.status === 1 ? '通过' : '不通过'}成功`)
          this.getList()
        } else {
          // 如果失败，恢复原状态
          row.status = row.status === 1 ? 0 : 1
          this.$message.error('操作失败')
        }
      } catch (error) {
        // 发生错误时恢复原状态
        row.status = row.status === 1 ? 0 : 1
        console.error('更新评价状态失败:', error)
        this.$message.error('操作失败')
      }
    },
    // 删除评价
    async handleDelete(row) {
      try {
        await this.$confirm('确认删除该评价?', '提示', {
          type: 'warning'
        })
        const res = await Request.delete(`/review/${row.id}`)
        if (res.code === '0') {
          this.$message.success('删除成功')
          this.getList()
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.review-manager {
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

.review-content {
  color: #606266;
  line-height: 1.5;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 文本按钮样式 */
.success-text {
  color: #67C23A;
}

.success-text:hover {
  color: #85ce61;
}

.danger-text {
  color: #F56C6C;
}

.danger-text:hover {
  color: #f78989;
}

:deep(.el-button--text) {
  padding: 0 8px;
}

:deep(.el-switch__label) {
  color: #606266;
}

:deep(.el-switch.is-checked .el-switch__label--right) {
  color: #13ce66;
}

:deep(.el-switch:not(.is-checked) .el-switch__label--left) {
  color: #ff4949;
}

:deep(.el-switch__label--right) {
  margin-left: 6px;
}

:deep(.el-switch__label--left) {
  margin-right: 6px;
}
</style> 