<template>
  <div class="dashboard-wrapper">
    <!-- 数据统计卡片 -->
    <div class="stat-cards">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-header">
          <i class="el-icon-shopping-cart-full stat-icon"></i>
          <div class="stat-title">本月订单</div>
        </div>
        <div class="stat-value">
          <count-to :startVal="0" :endVal="orderStats.currentMonthOrders" :duration="2000">
          </count-to>
        </div>
        <div class="stat-footer">
          较上月
          <span :class="orderTrend >= 0 ? 'up' : 'down'">
            {{ orderStats.growthRate }}
            <i :class="orderTrend >= 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
          </span>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-header">
          <i class="el-icon-money stat-icon"></i>
          <div class="stat-title">本月销售额</div>
        </div>
        <div class="stat-value">
          <count-to :startVal="0" :endVal="salesStats.currentMonthSales" :duration="2000" :decimals="2">
          </count-to>
        </div>
        <div class="stat-footer">
          较上月
          <span :class="saleTrend >= 0 ? 'up' : 'down'">
            {{ salesStats.growthRate }}
            <i :class="saleTrend >= 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
          </span>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-header">
          <i class="el-icon-user stat-icon"></i>
          <div class="stat-title">今年用户数</div>
        </div>
        <div class="stat-value">
          <count-to :startVal="0" :endVal="userStats.currentYearUsers" :duration="2000">
          </count-to>
        </div>
        <div class="stat-footer">
          较去年
          <span :class="userTrend >= 0 ? 'up' : 'down'">
            {{ userStats.growthRate }}
            <i :class="userTrend >= 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
          </span>
        </div>
      </el-card>
    </div>

    <div class="content-wrapper">
      <!-- 热销商品TOP5图表 -->
      <el-card class="chart-card" shadow="hover">
        <div slot="header" class="chart-header">
          <span>热销商品 TOP5</span>
          <el-button type="text" @click="fetchTopProducts">刷新</el-button>
        </div>
        <div class="chart-content">
          <div ref="topProductsChart" class="chart"></div>
        </div>
      </el-card>

      <!-- 品类销售占比图表 -->
      <el-card class="chart-card" shadow="hover">
        <div slot="header" class="chart-header">
          <span>品类销售占比</span>
          <el-button type="text" @click="fetchCategoryStats">刷新</el-button>
        </div>
        <div class="chart-content">
          <div ref="categoryChart" class="chart"></div>
        </div>
      </el-card>
    </div>

    <!-- AI智能销售分析 -->
    <el-card class="ai-analysis-card" shadow="hover">
      <div slot="header" class="ai-analysis-header">
        <span><i class="el-icon-data-analysis"></i> AI智能销售分析</span>
        <el-button type="success" size="small" icon="el-icon-magic-stick" :loading="aiAnalysisLoading" @click="handleAiSalesAnalysis">AI智能分析</el-button>
      </div>
      <div v-if="aiAnalysis" class="ai-analysis-content">
        <div class="analysis-item">
          <h4>热销商品分析</h4>
          <p>{{ aiAnalysis.hotProductsAnalysis }}</p>
        </div>
        <div class="analysis-item">
          <h4>库存预警建议</h4>
          <p>{{ aiAnalysis.stockWarning }}</p>
        </div>
        <div class="analysis-item">
          <h4>补货建议</h4>
          <p>{{ aiAnalysis.replenishmentAdvice }}</p>
        </div>
        <div class="analysis-item">
          <h4>销售趋势总结</h4>
          <p>{{ aiAnalysis.salesTrendSummary }}</p>
        </div>
        <div class="analysis-summary">{{ aiAnalysis.summary }}</div>
      </div>
      <el-empty v-else description="点击 AI智能分析 生成经营建议"></el-empty>
    </el-card>

    <div class="content-wrapper">
      <el-card class="notice-card" shadow="hover">
        <div slot="header" class="notice-header">
          <span>通知公告</span>
          <el-button type="text" @click="fetchData">刷新</el-button>
        </div>
        <div class="notice-content">
          <el-timeline>
            <el-timeline-item v-for="(notice, index) in announcements" :key="index" :timestamp="notice.time" :type="getNoticeType(notice.type)">
              <el-card class="notice-item" shadow="never">
                <h4>{{ notice.title }}</h4>
                <p class="notice-text">{{ notice.content }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { Timeline, TimelineItem, Card, Button } from 'element-ui'
import CountTo from 'vue-count-to'
import Request from '../utils/request.js'
import * as echarts from 'echarts'

export default {
  name: 'ShowView',
  components: {
    [Card.name]: Card,
    [Timeline.name]: Timeline,
    [TimelineItem.name]: TimelineItem,
    [Button.name]: Button,
    CountTo
  },
  data() {
    return {
      noticeLimit: 10,
      announcements: [],
      // 统计数据
      orderStats: {
        currentMonthOrders: 0,
        lastMonthOrders: 0,
        growthRate: '0.00%'
      },
      salesStats: {
        currentMonthSales: 0,
        lastMonthSales: 0,
        growthRate: '0.00%'
      },
      userStats: {
        currentYearUsers: 0,
        lastYearUsers: 0,
        growthRate: '0.00%'
      },
      // 热销商品图表
      topProductsChart: null,
      topProducts: [],
      // 品类统计
      categoryChart: null,
      categoryStats: [],
      aiAnalysisLoading: false,
      aiAnalysis: null
    }
  },
  computed: {
    // 订单增长率数值
    orderTrend() {
      const str = this.orderStats.growthRate || '0%'
      return parseFloat(str.replace('%', ''))
    },
    // 销售额增长率数值
    saleTrend() {
      const str = this.salesStats.growthRate || '0%'
      return parseFloat(str.replace('%', ''))
    },
    // 用户增长率数值
    userTrend() {
      const str = this.userStats.growthRate || '0%'
      return parseFloat(str.replace('%', ''))
    }
  },
  created() {
    this.fetchData()
    this.fetchStatistics()
    this.fetchTopProducts()
    this.fetchCategoryStats()
  },
  mounted() {
    this.$nextTick(() => {
      this.initTopProductsChart()
      this.initCategoryChart()
    })
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy() {
    if (this.topProductsChart) {
      this.topProductsChart.dispose()
      this.topProductsChart = null
    }
    if (this.categoryChart) {
      this.categoryChart.dispose()
      this.categoryChart = null
    }
    window.removeEventListener('resize', this.resizeCharts)
  },
  methods: {
    // 获取通知公告
    fetchData() {
      Request.get("/notice/limit", {
        params: {
          count: this.noticeLimit
        }
      }).then(response => {
        if (response.code === '0') {
          this.announcements = response.data
        }
      }).catch(err => {
        console.error('获取公告失败', err)
      })
    },

    // 获取首页统计数据
    fetchStatistics() {
      // 订单统计
      Request.get("/statistics/orders/monthly")
        .then(response => {
          if (response.code === '0') {
            this.orderStats = response.data
          }
        }).catch(err => console.error('订单统计接口异常', err))

      // 销售额统计
      Request.get("/statistics/sales/monthly")
        .then(response => {
          if (response.code === '0') {
            this.salesStats = response.data
          }
        }).catch(err => console.error('销售统计接口异常', err))

      // 用户统计
      Request.get("/statistics/users/yearly")
        .then(response => {
          if (response.code === '0') {
            this.userStats = response.data
          }
        }).catch(err => console.error('用户统计接口异常', err))
    },

    getNoticeType(type) {
      const types = {
        1: 'primary',   // 普通通知
        2: 'success',   // 活动通知
        3: 'warning',   // 重要通知
        4: 'danger'     // 紧急通知
      }
      return types[type] || 'primary'
    },

    // 热销商品柱状图初始化
    initTopProductsChart() {
      if (!this.$refs.topProductsChart) return
      this.topProductsChart = echarts.init(this.$refs.topProductsChart)
      this.updateTopProductsChart()
    },

    updateTopProductsChart() {
      if (!this.topProductsChart) return
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: '{b}: {c}件'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          axisLabel: {
            formatter: (value) => value + '件'
          }
        },
        yAxis: {
          type: 'category',
          data: this.topProducts.map(item => item.name).reverse(),
          axisLabel: {
            formatter: (value) => {
              if (value.length > 10) {
                return value.substring(0, 10) + '...'
              }
              return value
            }
          }
        },
        series: [{
          name: '销售数量',
          type: 'bar',
          data: this.topProducts.map(item => item.salesCount).reverse(),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#83bff6' },
              { offset: 0.5, color: '#188df0' },
              { offset: 1, color: '#188df0' }
            ])
          },
          emphasis: {
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#2378f7' },
                { offset: 0.7, color: '#2378f7' },
                { offset: 1, color: '#83bff6' }
              ])
            }
          }
        }]
      }
      this.topProductsChart.setOption(option)
    },

    // 获取TOP5商品
    async fetchTopProducts() {
      try {
        const response = await Request.get('/statistics/products/top5')
        if (response.code === '0' && response.data.topProducts) {
          this.topProducts = response.data.topProducts
          this.$nextTick(() => {
            this.updateTopProductsChart()
          })
        }
      } catch (error) {
        console.error('获取热销商品数据失败:', error)
      }
    },

    // 品类饼图初始化
    initCategoryChart() {
      if (!this.$refs.categoryChart) return
      this.categoryChart = echarts.init(this.$refs.categoryChart)
      this.updateCategoryChart()
    },

    updateCategoryChart() {
      if (!this.categoryChart) return
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center'
        },
        series: [
          {
            name: '品类销售',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              formatter: '{b}: {d}%'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '14',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: true
            },
            data: this.categoryStats.map(item => ({
              name: item.name,
              value: item.salesCount
            }))
          }
        ],
        color: [
          '#409EFF', '#67C23A', '#E6A23C', '#F56C6C',
          '#909399', '#36CBCB', '#FFA2D3', '#9A60B4'
        ]
      }
      this.categoryChart.setOption(option)
    },

    // 获取品类销售数据
    async fetchCategoryStats() {
      try {
        const response = await Request.get('/statistics/category/sales')
        if (response.code === '0' && response.data.categoryStats) {
          this.categoryStats = response.data.categoryStats
          this.$nextTick(() => {
            this.updateCategoryChart()
          })
        }
      } catch (error) {
        console.error('获取品类统计数据失败:', error)
      }
    },

    // AI销售分析
    async handleAiSalesAnalysis() {
      this.aiAnalysisLoading = true
      try {
        const response = await Request.post('/ai/sales-analysis', {})
        if (response.code === '0') {
          this.aiAnalysis = response.data
          this.$message.success('AI销售分析已生成')
        } else {
          this.$message.error(response.msg || 'AI销售分析失败')
        }
      } catch (error) {
        this.$message.error('AI销售分析服务暂不可用，请确认后端和AI服务已启动')
      } finally {
        this.aiAnalysisLoading = false
      }
    },

    // 图表自适应
    resizeCharts() {
      this.topProductsChart?.resize()
      this.categoryChart?.resize()
    }
  }
}
</script>

<style lang="less" scoped>
.dashboard-wrapper {
  padding: 20px;
}

.stat-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  min-width: 240px;

  .stat-header {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  .stat-icon {
    font-size: 24px;
    margin-right: 12px;
    color: #409eff;
  }

  .stat-title {
    font-size: 16px;
    color: #606266;
  }

  .stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 16px;
  }

  .stat-footer {
    font-size: 14px;
    color: #909399;

    .up {
      color: #67c23a;
    }

    .down {
      color: #f56c6c;
    }
  }
}

.content-wrapper {
  display: flex;
  gap: 20px;
  margin-top: 20px;

  .chart-card {
    flex: 1;
    min-width: 400px;
  }

  .notice-card {
    flex: 1;
    min-width: 400px;
  }
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.chart-content {
  height: 400px;
  .chart {
    width: 100%;
    height: 100%;
  }
}

.ai-analysis-card {
  margin-top: 20px;
}

.ai-analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.ai-analysis-content {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.analysis-item {
  padding: 14px 16px;
  background: #f8fbff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.analysis-item h4 {
  margin: 0 0 8px;
  color: #303133;
}

.analysis-item p {
  margin: 0;
  color: #606266;
  line-height: 1.7;
}

.analysis-summary {
  grid-column: 1 / -1;
  padding: 14px 16px;
  color: #529b2e;
  background: #f0f9eb;
  border-radius: 8px;
  line-height: 1.7;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.notice-content {
  max-height: 600px;
  overflow-y: auto;
}

.notice-item {
  margin-bottom: 8px;

  h4 {
    margin: 0 0 8px;
    color: #303133;
  }

  .notice-text {
    color: #606266;
    margin: 0;
    line-height: 1.6;
  }
}

:deep(.el-timeline-item__node--primary) {
  background-color: #409eff;
}

:deep(.el-timeline-item__node--success) {
  background-color: #67c23a;
}

:deep(.el-timeline-item__node--warning) {
  background-color: #e6a23c;
}

:deep(.el-timeline-item__node--danger) {
  background-color: #f56c6c;
}

.chart {
  min-height: 300px;
}

/* 小屏幕适配 */
@media screen and (max-width: 1200px) {
  .stat-cards {
    flex-direction: column;
  }
  .content-wrapper {
    flex-direction: column;
  }
}
</style>
