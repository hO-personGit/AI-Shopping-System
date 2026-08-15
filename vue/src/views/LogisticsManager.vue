<template>
  <div class="logistics-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>物流管理</h2>
      <el-tag type="info">共 {{ total }} 条物流记录</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderId" placeholder="请输入订单号" clearable></el-input>
        </el-form-item>
        <el-form-item label="物流状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option v-for="(label, value) in logisticsStatusMap" :key="value" :label="label" :value="Number(value)">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="medium" plain type="primary" @click="handleSearch">查询</el-button>
          <el-button size="medium" plain @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 物流统计卡片 -->
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-time"></i></div>
                <div class="stat-title">待发货</div>
              </div>
              <div class="stat-value">{{ getLogisticsCountByStatus(0) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-truck"></i></div>
                <div class="stat-title">已发货</div>
              </div>
              <div class="stat-value">{{ getLogisticsCountByStatus(1) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-circle-check"></i></div>
                <div class="stat-title">已签收</div>
              </div>
              <div class="stat-value">{{ getLogisticsCountByStatus(2) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-close"></i></div>
                <div class="stat-title">已取消</div>
              </div>
              <div class="stat-value">{{ getLogisticsCountByStatus(3) }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 物流列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="logisticsList" border style="width: 100%">
        <el-table-column prop="orderId" label="订单号" width="120"></el-table-column>
        <el-table-column prop="companyName" label="快递公司" width="120"></el-table-column>
        <el-table-column prop="trackingNumber" label="快递单号" width="150"></el-table-column>
        <el-table-column label="收货信息" width="200">
          <template slot-scope="scope">
            <div>{{ scope.row.order?.recvAddress }}</div>
            <div>{{ scope.row.order?.recvPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="物流状态" width="120">
          <template slot-scope="scope">
            <el-tag :type="getLogisticsStatusType(scope.row.status)">
              {{ logisticsStatusMap[scope.row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="expectedArrivalTime" label="预计到达时间" width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.expectedArrivalTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="handleDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.currentPage" :limit.sync="queryParams.size" @pagination="getList" />
    </el-card>

    <!-- 物流详情对话框 -->
    <el-dialog title="物流详情" :visible.sync="detailDialogVisible" width="600px">
      <div class="logistics-detail" v-if="currentLogistics">
        <div class="detail-section">
          <h4>物流信息</h4>
          <div class="detail-item">
            <span class="label">快递公司：</span>
            <span>{{ currentLogistics.companyName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">快递单号：</span>
            <span>{{ currentLogistics.trackingNumber }}</span>
          </div>
          <div class="detail-item">
            <span class="label">物流状态：</span>
            <el-tag :type="getLogisticsStatusType(currentLogistics.status)">
              {{ logisticsStatusMap[currentLogistics.status] }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">创建时间：</span>
            <span>{{ formatTime(currentLogistics.createdAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">更新时间：</span>
            <span>{{ formatTime(currentLogistics.updatedAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">预计到达：</span>
            <span>{{ formatTime(currentLogistics.expectedArrivalTime) }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>订单信息</h4>
          <div class="detail-item">
            <span class="label">订单编号：</span>
            <span>{{ currentLogistics.orderId }}</span>
          </div>
          <div class="detail-item">
            <span class="label">订单状态：</span>
            <el-tag :type="getOrderStatusType(currentLogistics.order?.status)">
              {{ orderStatusMap[currentLogistics.order?.status] }}
            </el-tag>
          </div>
        </div>

        <div class="detail-section">
          <h4>收货信息</h4>
          <div class="detail-item">
            <span class="label">收货人：</span>
            <span>{{ currentLogistics.order?.user?.name }}</span>
          </div>
          <div class="detail-item">
            <span class="label">联系电话：</span>
            <span>{{ currentLogistics.order?.recvPhone }}</span>
          </div>
          <div class="detail-item">
            <span class="label">收货地址：</span>
            <span>{{ currentLogistics.order?.recvAddress }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import Pagination from '@/components/Pagination/index.vue'
import { formatTime } from '@/utils/time'

export default {
  name: 'LogisticsManager',
  inject: ['userInfo'],
  components: {
    Pagination
  },
  data() {
    return {
      // 搜索表单
      searchForm: {
        orderId: undefined,
        status: undefined
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 物流列表
      logisticsList: [],
      // 总数
      total: 0,
      // 物流状态映射
      logisticsStatusMap: {
        0: '待发货',
        1: '已发货',
        2: '已签收',
        3: '已取消'
      },
      // 订单状态映射
      orderStatusMap: {
        0: '待支付',
        1: '已支付',
        2: '已发货',
        3: '已完成',
        4: '已取消',
        5: '退款中',
        6: '已退款',
        7: '退款失败'
      },
      // 详情对话框
      detailDialogVisible: false,
      currentLogistics: null
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取物流列表
    async getList() {
      try {
        const merchantId = this.userInfo.role === 'MERCHANT' ? this.userInfo.id : undefined
        const params = {
          ...this.queryParams,
          ...this.searchForm,
          merchantId
        }
        const res = await Request.get('/logistics/page', { params })
        if (res.code === '0') {
          this.logisticsList = res.data.records || []
          this.total = res.data.total || 0
        } else {
          this.logisticsList = []
          this.total = 0
          this.$message.error(res.msg || '获取物流列表失败')
        }
      } catch (error) {
        this.logisticsList = []
        this.total = 0
        console.error('获取物流列表失败:', error)
        this.$message.error('获取物流列表失败')
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
        orderId: undefined,
        status: undefined
      }
      this.handleSearch()
    },
    // 获取物流状态类型
    getLogisticsStatusType(status) {
      const typeMap = {
        0: 'info',
        1: 'primary',
        2: 'success',
        3: 'danger'
      }
      return typeMap[status] || 'info'
    },
    // 获取订单状态类型
    getOrderStatusType(status) {
      const typeMap = {
        0: 'warning',
        1: 'primary',
        2: 'success',
        3: 'info',
        4: 'danger',
        5: 'warning',
        6: 'info',
        7: 'danger'
      }
      return typeMap[status] || 'info'
    },
    // 获取指定状态的物流数量
    getLogisticsCountByStatus(status) {
      if (!this.logisticsList) return 0
      return this.logisticsList.filter(item => item.status === status).length
    },
    // 查看详情
    handleDetail(logistics) {
      this.currentLogistics = logistics
      this.detailDialogVisible = true
    },
    // 格式化时间
    formatTime
  }
}
</script>

<style scoped>
.logistics-manager {
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

.statistics-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s;
  border: none;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-content {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-icon {
  font-size: 24px;
  color: #409eff;
}

.stat-title {
  font-size: 14px;
  color: #606266;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2f3d;
}

.table-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.detail-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.detail-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.detail-section h4 {
  font-size: 16px;
  color: #1f2f3d;
  margin: 0 0 16px 0;
  font-weight: 500;
}

.detail-item {
  display: flex;
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.4;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-item .label {
  width: 100px;
  color: #909399;
}
</style> 