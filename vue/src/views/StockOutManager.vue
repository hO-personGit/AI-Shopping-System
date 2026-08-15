<template>
  <div class="stock-out-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>出库管理</h2>
      <el-button type="primary" @click="showCreateDialog">新增出库</el-button>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商品">
          <el-select v-model="searchForm.productId" placeholder="请选择商品" clearable filterable>
            <el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="出库类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" clearable>
            <el-option v-for="(label, value) in outTypeMap" :key="value" :label="label" :value="Number(value)">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="正常" :value="1"></el-option>
            <el-option label="作废" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 出库列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="stockOutList" border style="width: 100%" :default-sort="{prop: 'id', order: 'ascending'}">
        <el-table-column prop="id" label="出库单号" width="120" sortable></el-table-column>
        <el-table-column label="商品信息" min-width="200">
          <template slot-scope="scope">
            <div class="product-info">
              <el-image :src="'api'+scope.row.product?.imageUrl" :preview-src-list="['api'+scope.row.product?.imageUrl]"
                fit="cover" style="width: 50px; height: 50px">
              </el-image>
              <div class="product-detail">
                <div class="product-name">{{ scope.row.product?.name }}</div>
                <div class="product-price">
                  单价：¥{{ scope.row.unitPrice?.toFixed(2) }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="出库数量" width="120"></el-table-column>
        <el-table-column prop="totalPrice" label="总金额" width="120">
          <template slot-scope="scope">
            ¥{{ scope.row.totalPrice?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="出库类型" width="120">
          <template slot-scope="scope">
            <el-tag :type="getOutTypeTagType(scope.row.type)">
              {{ outTypeMap[scope.row.type] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator.name" label="操作人" width="120"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '正常' : '作废' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="出库时间" width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户姓名" width="120"></el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="150"></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button v-if="scope.row.status === 1" type="text" size="small" @click="handleInvalidate(scope.row)">
              作废
            </el-button>
            <el-button type="text" size="small" @click="handleDetail(scope.row)">详情</el-button>
            <el-button type="text" size="small" class="delete-button" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.currentPage" :limit.sync="queryParams.size"
        @pagination="getList" />
    </el-card>

    <!-- 新增出库对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="createDialogVisible" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createForm" label-width="100px">
        <el-form-item label="商品" prop="productId">
          <el-select v-model="createForm.productId" placeholder="请选择商品" filterable style="width: 100%"
            @change="handleProductChange">
            <el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id">
              <span style="float: left">{{ item.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">库存: {{ item.stock }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="createForm.quantity" :min="1" :max="currentProductStock" style="width: 100%">
          </el-input-number>
        </el-form-item>
        <el-form-item label="单价" prop="unitPrice">
          <el-input v-model.number="createForm.unitPrice" type="number" placeholder="请输入单价">
            <template slot="append">元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="出库类型" prop="type">
          <el-select v-model="createForm.type" placeholder="请选择出库类型" style="width: 100%">
            <el-option v-for="(label, value) in outTypeMap" :key="value" :label="label" :value="Number(value)">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="客户姓名" prop="customerName">
          <el-input v-model="createForm.customerName" placeholder="请输入客户姓名"></el-input>
        </el-form-item>
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="createForm.orderNo" placeholder="请输入订单号"></el-input>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea" v-model="createForm.remark" :rows="3" placeholder="请输入备注信息"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="createDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitCreate">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="出库详情" :visible.sync="detailDialogVisible" width="600px">
      <div class="stock-detail" v-if="currentStock">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-item">
            <span class="label">出库单号：</span>
            <span>{{ currentStock.id }}</span>
          </div>
          <div class="detail-item">
            <span class="label">出库状态：</span>
            <el-tag :type="currentStock.status === 1 ? 'success' : 'info'">
              {{ currentStock.status === 1 ? '正常' : '作废' }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">出库时间：</span>
            <span>{{ formatTime(currentStock.createdAt) }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>商品信息</h4>
          <div class="detail-item">
            <span class="label">商品名称：</span>
            <span>{{ currentStock.product?.name }}</span>
          </div>
          <div class="detail-item">
            <span class="label">出库数量：</span>
            <span>{{ currentStock.quantity }}</span>
          </div>
          <div class="detail-item">
            <span class="label">单价：</span>
            <span>¥{{ currentStock.unitPrice?.toFixed(2) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">总金额：</span>
            <span class="total-price">¥{{ currentStock.totalPrice?.toFixed(2) }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>其他信息</h4>
          <div class="detail-item">
            <span class="label">出库类型：</span>
            <el-tag :type="getOutTypeTagType(currentStock.type)">
              {{ outTypeMap[currentStock.type] }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">操作人：</span>
            <span>{{ currentStock.operator?.name }}</span>
          </div>
          <div class="detail-item">
            <span class="label">备注：</span>
            <span>{{ currentStock.remark || '无' }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>客户信息</h4>
          <div class="detail-item">
            <span class="label">客户姓名：</span>
            <span>{{ currentStock.customerName || '无' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">订单号：</span>
            <span>{{ currentStock.orderNo || '无' }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 添加删除确认对话框 -->
    <el-dialog
      title="删除确认"
      :visible.sync="deleteDialogVisible"
      width="30%">
      <span>确定要删除该出库记录吗？删除后将无法恢复。</span>
      <span slot="footer" class="dialog-footer">
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import Pagination from '@/components/Pagination/index.vue'
import { formatTime } from '@/utils/time'

export default {
  name: 'StockOutManager',
  inject: ['userInfo'],
  components: {
    Pagination
  },
  data() {
    return {
      // 搜索表单
      searchForm: {
        productId: undefined,
        type: undefined,
        status: undefined
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 出库列表
      stockOutList: [],
      // 总数
      total: 0,
      // 商品列表
      products: [],
      // 出库类型映射
      outTypeMap: {
        1: '销售出库',
        2: '损耗出库',
        3: '其他出库'
      },
      // 新增对话框
      createDialogVisible: false,
      createForm: {
        productId: undefined,
        quantity: 1,
        operatorId: this.userInfo.id,
        unitPrice: undefined,
        type: 1,
        remark: '',
        customerName: '',
        orderNo: '',
      },
      createRules: {
        productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
        quantity: [{ required: true, message: '请输入出库数量', trigger: 'blur' }],
        unitPrice: [
          { required: true, message: '请输入单价', trigger: 'blur' },
          { type: 'number', min: 0.01, message: '单价必须大于0', trigger: 'blur' }
        ],
        type: [{ required: true, message: '请选择出库类型', trigger: 'change' }],
        customerName: [{ required: false, message: '请输入客户姓名', trigger: 'blur' }],
        orderNo: [{ required: false, message: '请输入订单号', trigger: 'blur' }],
      },
      // 详情对话框
      detailDialogVisible: false,
      currentStock: null,
      // 当前选中商品的库存
      currentProductStock: 99999,
      // 删除对话框
      deleteDialogVisible: false,
      currentDeleteStock: null,
    }
  },
  computed: {
    dialogTitle() {
      return '新增出库'
    }
  },
  created() {
    this.getProducts()
    this.getList()
  },
  methods: {
    // 获取商品列表
    async getProducts() {
      try {
        const merchantId = this.userInfo.role === 'MERCHANT' ? this.userInfo.id : undefined
        const res = await Request.get('/product/all', { params: { merchantId } })
        if (res.code === '0') {
          this.products = res.data
        }
      } catch (error) {
        console.error('获取商品列表失败:', error)
      }
    },
    // 获取出库列表
    async getList() {
      try {
        const operatorId = this.userInfo.role === 'MERCHANT' ? this.userInfo.id : undefined
        const params = {
          ...this.queryParams,
          ...this.searchForm,
          operatorId
        }
        const res = await Request.get('/stock/out/list', { params })
        if (res.code === '0') {
          this.stockOutList = res.data.records
          this.total = res.data.total
        }
      } catch (error) {
        console.error('获取出库列表失败:', error)
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
        productId: undefined,
        type: undefined,
        status: undefined
      }
      this.handleSearch()
    },
    // 获取出库类型标签样式
    getOutTypeTagType(type) {
      const typeMap = {
        1: 'success',
        2: 'danger',
        3: 'info'
      }
      return typeMap[type] || 'info'
    },
    // 显示新增对话框
    showCreateDialog() {
      this.createForm = {
        productId: undefined,
        quantity: 1,
        unitPrice: undefined,
        type: 1,
        remark: '',
        customerName: '',
        orderNo: '',
      }
      this.createDialogVisible = true
    },
    // 商品选择变更
    handleProductChange(productId) {
      const product = this.products.find(p => p.id === productId)
      if (product) {
        this.currentProductStock = product.stock
        this.createForm.unitPrice = product.price
      }
    },
    // 提交新增
    async submitCreate() {
      try {
        await this.$refs.createForm.validate()
        const res = await Request.post('/stock/out', {
          ...this.createForm,
          operatorId: this.userInfo.id
        })
        if (res.code === '0') {
          this.$message.success('出库成功')
          this.createDialogVisible = false
          this.getList()
          // 更新商品列表以刷新库存
          this.getProducts()
        }
      } catch (error) {
        console.error('出库失败:', error)
        this.$message.error(error.response?.data?.msg || '出库失败')
      }
    },
    // 作废
    async handleInvalidate(row) {
      try {
        await this.$confirm('确认要作废该出库记录吗？', '提示', {
          type: 'warning'
        })
        const res = await Request.put(`/stock/out/${row.id}/invalidate`)
        if (res.code === '0') {
          this.$message.success('作废成功')
          this.getList()
          // 更新商品列表以刷新库存
          this.getProducts()
        }
      } catch (error) {
        console.error('作废失败:', error)
        this.$message.error(error.response?.data?.msg || '作废失败')
      }
    },
    // 查看详情
    handleDetail(row) {
      this.currentStock = row
      this.detailDialogVisible = true
    },
    // 删除
    handleDelete(row) {
      this.currentDeleteStock = row
      this.deleteDialogVisible = true
    },
    // 确认删除
    async confirmDelete() {
      try {
        const res = await Request.delete(`/stock/out/${this.currentDeleteStock.id}`)
        if (res.code === '0') {
          this.$message.success('删除成功')
          this.deleteDialogVisible = false
          this.getList()
          // 更新商品列表以刷新库存
          this.getProducts()
        }
      } catch (error) {
        console.error('删除失败:', error)
        this.$message.error(error.response?.data?.msg || '删除失败')
      }
    },
    // 格式化时间
    formatTime
  }
}
</script>

<style scoped>
.stock-out-manager {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 500;
  color: #1f2f3d;
  margin: 0;
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
  color: #909399;
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

.total-price {
  color: #f56c6c;
  font-weight: 500;
}
</style> 