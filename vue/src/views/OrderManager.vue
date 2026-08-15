<template>
  <div class="order-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>订单管理</h2>
      <el-tag type="info">共 {{ total }} 个订单</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.id" placeholder="请输入订单号" clearable></el-input>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option v-for="(label, value) in orderStatusMap" :key="value" :label="label" :value="Number(value)">
            </el-option>
          </el-select>
        </el-form-item>
        <!-- 管理员可以查看所有用户订单 -->
        <el-form-item label="用户账号" v-if="isAdmin">
          <el-input v-model="searchForm.userId" placeholder="请输入用户账号" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button size="medium" plain type="primary" @click="handleSearch">查询</el-button>
          <el-button size="medium" plain @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单统计卡片 -->
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-time"></i></div>
                <div class="stat-title">待付款</div>
              </div>
              <div class="stat-value">{{ getOrderCountByStatus(0) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-box"></i></div>
                <div class="stat-title">待发货</div>
              </div>
              <div class="stat-value">{{ getOrderCountByStatus(1) }}</div>
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
              <div class="stat-value">{{ getOrderCountByStatus(2) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-circle-check"></i></div>
                <div class="stat-title">已完成</div>
              </div>
              <div class="stat-value">{{ getOrderCountByStatus(3) }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 订单列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="orders" border style="width: 100%" :default-sort="{prop: 'createdAt', order: 'descending'}">
        <el-table-column prop="id" label="订单号" width="120" sortable></el-table-column>
        <el-table-column label="商品信息" min-width="300">
          <template slot-scope="scope">
            <div class="product-info">
              <el-image :src="'api'+scope.row.product.imageUrl" :preview-src-list="['api'+scope.row.product.imageUrl]" fit="cover"
                style="width: 50px; height: 50px">
              </el-image>
              <div class="product-detail">
                <div class="product-name">{{ scope.row.product.name }}</div>
                <div class="product-price">
                  ¥{{ scope.row.price.toFixed(2) }} × {{ scope.row.quantity }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="订单金额" width="120">
          <template slot-scope="scope">
            ¥{{ scope.row.totalPrice.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="收货信息" width="200">
          <template slot-scope="scope">
            <div>{{ scope.row.recvAddress }}</div>
            <div>{{ scope.row.recvPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="订单状态" width="120">
          <template slot-scope="scope">
            <el-tag :type="getOrderStatusType(scope.row.status)">
              {{ orderStatusMap[scope.row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180" :sortable="true">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <!-- 根据订单状态和用户角色显示不同的操作按钮 -->
            <template v-if="isMerchant">
              <!-- 商户只能发货 -->
              <el-button 
                v-if="scope.row.status === 1" 
                type="text" 
                size="small" 
                @click="handleShip(scope.row)">发货</el-button>
              <!-- 添加查看物流按钮 -->
              <el-button 
                v-if="scope.row.status >= 2 " 
                type="text" 
                size="small" 
                @click="handleViewLogistics(scope.row)">查看物流</el-button>
              <el-button 
                v-if="scope.row.status === 5" 
                type="text" 
                size="small" 
                @click="handleRefund(scope.row)">处理退款</el-button>
            </template>
            
            <template v-else-if="isAdmin">
              <!-- 管理员只能删除订单 -->
              <el-button 
                type="text" 
                size="small" 
                @click="handleDelete(scope.row)">删除</el-button>
            </template>
            
            <el-button type="text" size="small" @click="handleDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.currentPage" :limit.sync="queryParams.size" @pagination="getList" />
    </el-card>

    <!-- 添加退款处理对话框 -->
    <el-dialog title="退款处理" :visible.sync="refundDialogVisible" width="500px">
      <div class="refund-info">
        <p>订单号：{{ currentOrder?.id }}</p>
        <p>退款金额：¥{{ currentOrder?.totalPrice }}</p>
        <p>退款原因：{{ currentOrder?.refundReason }}</p>
      </div>
      <div class="refund-operation">
        <el-form :model="refundForm" label-width="80px">
          <el-form-item label="处理结果">
            <el-radio-group v-model="refundForm.status">
              <el-radio :label="6">同意退款</el-radio>
              <el-radio :label="7">拒绝退款</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理备注">
            <el-input type="textarea" v-model="refundForm.remark" :rows="3"></el-input>
          </el-form-item>
        </el-form>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="refundDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitRefund">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog title="订单详情" :visible.sync="detailDialogVisible" width="600px">
      <div class="order-detail">
        <!-- 订单基本信息 -->
        <div class="detail-section">
          <h4>订单信息</h4>
          <div class="detail-item">
            <span class="label">订单编号：</span>
            <span>{{ currentOrder?.id }}</span>
          </div>
          <div class="detail-item">
            <span class="label">下单时间：</span>
            <span>{{ formatTime(currentOrder?.createdAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">订单状态：</span>
            <el-tag :type="getOrderStatusType(currentOrder?.status)">
              {{ orderStatusMap[currentOrder?.status] }}
            </el-tag>
          </div>
        </div>

        <!-- 商品信息 -->
        <div class="detail-section">
          <h4>商品信息</h4>
          <div class="product-detail-info">
            <el-image 
              :src="'api'+currentOrder?.product?.imageUrl" 
              :preview-src-list="['api'+currentOrder?.product?.imageUrl]"
              fit="cover"
              style="width: 80px; height: 80px; border-radius: 4px;">
            </el-image>
            <div class="product-info-right">
              <div class="product-name">{{ currentOrder?.product?.name }}</div>
              <div class="product-price">
                <span>单价：¥{{ currentOrder?.price }}</span>
                <span class="quantity">数量：{{ currentOrder?.quantity }}</span>
              </div>
              <div class="total-price">
                总价：¥{{ currentOrder?.totalPrice }}
              </div>
            </div>
          </div>
        </div>

        <!-- 收货信息 -->
        <div class="detail-section">
          <h4>收货信息</h4>
          <div class="detail-item">
            <span class="label">收货人：</span>
            <span>{{ currentOrder?.recvName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">联系电话：</span>
            <span>{{ currentOrder?.recvPhone }}</span>
          </div>
          <div class="detail-item">
            <span class="label">收货地址：</span>
            <span>{{ currentOrder?.recvAddress }}</span>
          </div>
        </div>

        <!-- 添加商户信息部分 -->
        <div class="detail-section">
          <h4>商户信息</h4>
          <div class="detail-item">
            <span class="label">商户姓名：</span>
            <span>{{ currentOrder?.merchant?.name }}</span>
          </div>
        </div>

        <!-- 退款信息(如果有) -->
        <div class="detail-section" v-if="currentOrder?.refundStatus > 0">
          <h4>退款信息</h4>
          <div class="detail-item">
            <span class="label">退款状态：</span>
            <el-tag :type="getRefundStatusType(currentOrder?.refundStatus)">
              {{ refundStatusMap[currentOrder?.refundStatus] }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">退款原因：</span>
            <span>{{ currentOrder?.refundReason }}</span>
          </div>
          <div class="detail-item" v-if="currentOrder?.refundTime">
            <span class="label">退款时间：</span>
            <span>{{ formatTime(currentOrder?.refundTime) }}</span>
          </div>
          <div class="detail-item" v-if="currentOrder?.remark">
            <span class="label">处理备注：</span>
            <span>{{ currentOrder?.remark }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 修改发货对话框 -->
    <el-dialog title="发货处理" :visible.sync="shipDialogVisible" width="500px">
      <el-form :model="shipForm" :rules="shipRules" ref="shipForm" label-width="100px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="shipForm.receiverName" disabled></el-input>
        </el-form-item>
        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="shipForm.receiverPhone" disabled></el-input>
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddress">
          <el-input type="textarea" v-model="shipForm.receiverAddress" disabled :rows="2"></el-input>
        </el-form-item>
        <el-form-item label="预计到达" prop="expectedArrivalTime">
          <el-date-picker
            v-model="shipForm.expectedArrivalTime"
            type="datetime"
            placeholder="选择预计到达时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="快递公司" prop="companyName">
          <el-select v-model="shipForm.companyName" placeholder="请选择快递公司">
            <el-option label="顺丰快递" value="顺丰快递"></el-option>
            <el-option label="中通快递" value="中通快递"></el-option>
            <el-option label="圆通快递" value="圆通快递"></el-option>
            <el-option label="韵达快递" value="韵达快递"></el-option>
            <el-option label="申通快递" value="申通快递"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号" prop="trackingNumber">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入快递单号"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="shipDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitShip">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 添加物流信息对话框 -->
    <el-dialog title="物流信息" :visible.sync="logisticsDialogVisible" width="600px">
      <div class="logistics-info" v-if="logisticsInfo">
        <div class="logistics-header">
          <div class="company-info">
            <span class="label">快递公司：</span>
            <span>{{ logisticsInfo.companyName }}</span>
          </div>
          <div class="tracking-info">
            <span class="label">快递单号：</span>
            <span>{{ logisticsInfo.trackingNumber }}</span>
          </div>
          <div class="status-info">
            <span class="label">物流状态：</span>
            <el-tag :type="getLogisticsStatusType(logisticsInfo.status)">
              {{ logisticsStatusMap[logisticsInfo.status] }}
            </el-tag>
          </div>
          <div class="time-info">
            <div>
              <span class="label">创建时间：</span>
              <span>{{ formatTime(logisticsInfo.createdAt) }}</span>
            </div>
            <div>
              <span class="label">更新时间：</span>
              <span>{{ formatTime(logisticsInfo.updatedAt) }}</span>
            </div>
          </div>
        </div>
        <div class="logistics-address">
          <h4>收货信息</h4>
          <div class="address-content">
            <p><span class="label">收货人：</span>{{ logisticsInfo.receiverName }}</p>
            <p><span class="label">联系电话：</span>{{ logisticsInfo.receiverPhone }}</p>
            <p><span class="label">收货地址：</span>{{ logisticsInfo.receiverAddress }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import Pagination from '@/components/Pagination/index.vue'
import { formatTime } from '@/utils/time'  // 导入时间格式化函数

export default {
  name: 'OrderManager',
  components: {
    Pagination
  },
  inject: ['userInfo'],
  data() {
    return {
      // 搜索表单
      searchForm: {
        id: undefined,
        status: undefined,
        userId: undefined
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 订单列表
      orders: [],
      // 总数
      total: 0,
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
      // 添加退款相关数据
      refundDialogVisible: false,
      currentOrder: null,
      refundForm: {
        status: 6,
        remark: ''
      },
      detailDialogVisible: false,
      refundStatusMap: {
        0: '无退款',
        1: '申请退款',
        2: '退款中',
        3: '已退款',
        4: '退款失败'
      },
      // 修改发货对话框
      shipDialogVisible: false,
      logisticsDialogVisible: false,
      shipForm: {
        orderId: null,
        receiverName: '',
        receiverPhone: '',
        receiverAddress: '',
        companyName: '',
        trackingNumber: '',
        expectedArrivalTime: '',
      },
      shipRules: {
        companyName: [
          { required: true, message: '请选择快递公司', trigger: 'change' }
        ],
        trackingNumber: [
          { required: true, message: '请输入快递单号', trigger: 'blur' }
        ],
        expectedArrivalTime: [
          { required: true, message: '请选择预计到达时间', trigger: 'change' }
        ],
      },
      logisticsInfo: null,
      logisticsStatusMap: {
        0: '待发货',
        1: '已发货',
        2: '已签收',
        3: '已取消'
      }
    }
  },
  computed: {
    // 判断是否为管理员
    isAdmin() {
      return ['SUPER_ADMIN', 'ADMIN'].includes(this.userInfo.role)
    },
    // 判断是否为商户
    isMerchant() {
      return this.userInfo.role === 'MERCHANT'
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取订单列表
    async getList() {
      try {
        const params = {
          ...this.queryParams,
          ...this.searchForm
        }
        // 如果不是管理员，只能查看自己的订单
        if (!this.isAdmin) {
          params.merchantId = this.userInfo.id
        }
        const res = await Request.get('/order/page', { params })
        if (res.code === '0') {
          if (res.data.total === 0) {
            this.orders = []
            this.total = 0
          } else {
            this.orders = res.data.records
            this.total = res.data.total
          }
        }
      } catch (error) {
        console.error('获取订单列表失败:', error)
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
        id: '',
        status: '',
        userId: ''
      }
      this.handleSearch()
    },
    // 获取订单状态类型
    getOrderStatusType(status) {
      const typeMap = {
        0: 'warning',   // 待支付
        1: 'primary',   // 已支付
        2: 'success',   // 已发货
        3: 'info',      // 已完成
        4: 'danger',    // 已取消
        5: 'warning',   // 退款中
        6: 'info',      // 已退款
        7: 'danger'     // 退款失败
      }
      return typeMap[status] || 'info'
    },
    // 获取指定状态的订单数量
    getOrderCountByStatus(status) {
      return this.orders.filter(order => order.status === status).length
    },
    // 取消订单
    async handleCancel(order) {
      try {
        await this.$confirm('确认取消该订单?', '提示', {
          type: 'warning'
        })
        const res = await Request.put(`/order/${order.id}/status?status=4`)
        if (res.code === '0') {
          this.$message.success('订单已取消')
          this.getList()
        }
      } catch (error) {
        console.error('取消订单失败:', error)
      }
    },
    // 发货
    handleShip(order) {
      this.currentOrder = order
      this.shipForm = {
        orderId: order.id,
        receiverName: order.recvName,
        receiverPhone: order.recvPhone,
        receiverAddress: order.recvAddress,
        companyName: '',
        trackingNumber: '',
        expectedArrivalTime: '',
      }
      this.shipDialogVisible = true
    },
    // 确认收货
    async handleConfirm(order) {
      try {
        await this.$confirm('确认收到商品?', '提示', {
          type: 'warning'
        })
        const res = await Request.put(`/order/${order.id}/status?status=3`)
        if (res.code === '0') {
          this.$message.success('确认收货成功')
          this.getList()
        }
      } catch (error) {
        console.error('确认收货失败:', error)
      }
    },
    // 评价
    handleReview(order) {
      this.$router.push(`/review/${order.id}`)
    },
    // 查看详情
    handleDetail(order) {
      this.currentOrder = order
      this.detailDialogVisible = true
    },
    // 处理退款
    handleRefund(order) {
      this.currentOrder = order
      this.refundForm = {
        status: 6,
        remark: ''
      }
      this.refundDialogVisible = true
    },
    // 提交退款处理
    async submitRefund() {
      try {
        const res = await Request.put(`/order/${this.currentOrder.id}/handle-refund`, null, {
          params: {
            status: this.refundForm.status,    // 6-已退款 或 7-退款失败
            remark: this.refundForm.remark     // 处理备注
          }
        })
        
        if (res.code === '0') {
          this.$message.success(this.refundForm.status === 6 ? '退款已通过' : '退款已拒绝')
          this.refundDialogVisible = false
          this.getList()
        } else {
          this.$message.error(res.msg || '退款处理失败')
        }
      } catch (error) {
        console.error('退款处理失败:', error)
        this.$message.error('退款处理失败')
      }
    },
    // 获取退款状态对应的类型
    getRefundStatusType(status) {
      const typeMap = {
        0: 'info',
        1: 'warning',
        2: 'warning',
        3: 'success',
        4: 'danger'
      }
      return typeMap[status] || 'info'
    },
    // 添加格式化时间方法
    formatTime,
    // 添加删除订单方法
    async handleDelete(order) {
      try {
        await this.$confirm('确认删除该订单?', '提示', {
          type: 'warning'
        })
        const res = await Request.delete(`/order/${order.id}`)
        if (res.code === '0') {
          this.$message.success('订单已删除')
          this.getList()
        }
      } catch (error) {
        console.error('删除订单失败:', error)
      }
    },
    // 提交发货信息
    async submitShip() {
      try {
        // 表单验证
        await this.$refs.shipForm.validate()
        
        // 显示加载中
        const loading = this.$loading({
          lock: true,
          text: '正在处理发货请求...',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        });
        
        try {
          // 创建物流信息
          const logisticsRes = await Request.post('/logistics', {
            orderId: this.shipForm.orderId,
            receiverName: this.shipForm.receiverName,
            receiverPhone: this.shipForm.receiverPhone,
            receiverAddress: this.shipForm.receiverAddress,
            companyName: this.shipForm.companyName,
            trackingNumber: this.shipForm.trackingNumber,
            expectedArrivalTime: this.shipForm.expectedArrivalTime,
            status: 1  // 直接设置为已发货状态
          })
  
          if (logisticsRes.code === '0') {
            // 更新订单状态为已发货
            const orderRes = await Request.put(`/order/${this.shipForm.orderId}/status?status=2`)
            
            if (orderRes.code === '0') {
              this.$message({
                type: 'success',
                message: '发货成功',
                duration: 3000
              });
              this.shipDialogVisible = false
              this.shipForm = {  // 重置表单
                orderId: null,
                receiverName: '',
                receiverPhone: '',
                receiverAddress: '',
                companyName: '',
                trackingNumber: '',
                expectedArrivalTime: '',
              }
              this.getList()
            } else {
              this.$message.error(orderRes.msg || '更新订单状态失败，但物流信息已创建')
            }
          } else {
            this.$message.error(logisticsRes.msg || '创建物流信息失败')
          }
        } catch (error) {
          console.error('发货处理过程中出错:', error)
          this.$message.error(error.response?.data?.msg || '发货处理失败，请稍后重试')
        } finally {
          // 关闭加载提示
          loading.close();
        }
      } catch (validationError) {
        // 表单验证失败
        console.error('表单验证失败:', validationError)
        this.$message.warning('请完善发货信息')
      }
    },
    // 查看物流信息
    async handleViewLogistics(order) {
      try {
        // 显示加载中
        const loading = this.$loading({
          lock: true,
          text: '正在获取物流信息...',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        });
        
        try {
          const res = await Request.get(`/logistics/order/${order.id}`)
          
          if (res.code === '0' && res.data) {
            this.logisticsInfo = res.data
            // 添加物流时间显示
            this.logisticsInfo.createdTime = formatTime(res.data.createdAt)
            this.logisticsInfo.updatedTime = formatTime(res.data.updatedAt)
            this.logisticsDialogVisible = true
          } else {
            this.$message({
              type: 'warning',
              message: res.msg || '暂无物流信息',
              duration: 3000
            })
          }
        } finally {
          // 关闭加载提示
          loading.close();
        }
      } catch (error) {
        console.error('获取物流信息失败:', error)
        this.$message.error('获取物流信息失败，请稍后重试')
      }
    },
    // 获取物流状态对应的类型
    getLogisticsStatusType(status) {
      const typeMap = {
        0: 'info',
        1: 'primary',
        2: 'success',
        3: 'danger'
      }
      return typeMap[status] || 'info'
    }
  }
}
</script>

<style scoped>
.order-manager {
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

.refund-info {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.refund-info p {
  margin: 8px 0;
  color: #606266;
}

.refund-operation {
  padding: 0 20px;
}

.order-detail {
  padding: 20px;
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

.product-detail-info {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.product-info-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-name {
  font-size: 14px;
  color: #1f2f3d;
  font-weight: 500;
}

.product-price {
  color: #606266;
  font-size: 13px;
  display: flex;
  gap: 16px;
}

.total-price {
  color: #f56c6c;
  font-size: 14px;
  font-weight: 500;
}

/* 添加物流信息相关样式 */
.logistics-info {
  padding: 20px;
}

.logistics-header {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.logistics-header > div {
  margin-bottom: 12px;
}

.logistics-header > div:last-child {
  margin-bottom: 0;
}

.logistics-header .label {
  color: #909399;
  margin-right: 8px;
}

.time-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
}

.time-info > div {
  margin-bottom: 8px;
}

.logistics-address {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.logistics-address h4 {
  font-size: 16px;
  color: #1f2f3d;
  margin: 0 0 16px 0;
}

.address-content p {
  margin: 8px 0;
  color: #606266;
}
</style> 