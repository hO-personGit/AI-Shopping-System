<template>
  <div class="category-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>分类管理</h2>
      <el-tag type="info">共 {{ total }} 个分类</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="分类名称">
          <el-input v-model="searchForm.name" placeholder="请输入分类名称" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button size="medium" plain type="primary" @click="handleSearch">查询</el-button>
          <el-button size="medium" plain @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="operation-buttons">

        <el-button plain size="medium" type="primary" icon="el-icon-plus" class="add-button" @click="handleAdd">新增分类</el-button>
        <el-button plain size="medium" icon="el-icon-refresh" @click="getList">刷新</el-button>
      </div>
    </el-card>


    <!-- 分类列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="categories" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="图标" width="100">
          <template slot-scope="scope">
            <i :class="scope.row.icon" style="font-size: 20px;"></i>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="分类名称"></el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="small" icon="el-icon-delete" class="delete-btn" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.currentPage" :limit.sync="queryParams.size" @pagination="getList" />
    </el-card>

    <!-- 分类表单对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        <el-form-item label="图标" :label-width="formLabelWidth" prop="icon">
          <el-select v-model="form.icon" filterable placeholder="请选择图标">
            <el-option v-for="dict in iconDict" :key="dict.itemKey" :label="dict.itemKey" :value="dict.itemValue">
              <i :class="dict.itemValue"></i>&nbsp;&nbsp;{{ dict.itemKey }}
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" :rows="4" placeholder="请输入分类描述"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import Pagination from '@/components/Pagination/index.vue'

export default {
  name: 'CategoryManager',
  components: {
    Pagination
  },
  data() {
    return {
      // 搜索表单
      searchForm: {
        name: ''
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 分类列表
      categories: [],
      // 总数
      total: 0,
      // 对话框显示
      dialogVisible: false,
      // 对话框标题
      dialogTitle: '',
      // 表单数据
      form: {
        id: undefined,
        name: '',
        icon: '',
        description: ''
      },
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: '请输入分类名称', trigger: 'blur' }
        ],
        icon: [
          { required: true, message: '请选择图标', trigger: 'change' }
        ]
      },
      // 表单标签宽度
      formLabelWidth: '100px',
      iconDict: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取分类列表
    async getList() {
      try {
        const params = {
          ...this.queryParams,
          name: this.searchForm.name
        }
        const res = await Request.get('/category/page', { params })
        if (res.code === '0') {
          this.categories = res.data.records.map(item => ({
            ...item,
            createdAt: new Date(item.createdAt).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
              hour12: false
            }).replace(/\//g, '-')
          }))
          this.total = res.data.total
        }
      } catch (error) {
        console.error('获取分类列表失败:', error)
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
        name: ''
      }
      this.handleSearch()
    },
    // 新增分类
    handleAdd() {
      this.dialogTitle = '新增分类'
      this.form = {
        name: '',
        icon: '',
        description: ''
      }
      this.loadIconList()
      this.dialogVisible = true
    },
    // 编辑分类
    handleEdit(row) {
      this.dialogTitle = '编辑分类'
      this.form = { ...row }
      this.form.createdAt = undefined
      this.form.updatedAt = undefined
      this.loadIconList()
      this.dialogVisible = true

    },
    // 删除分类
    handleDelete(row) {
      this.$confirm('确认删除该分类?', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await Request.delete(`/category/${row.id}`)
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.getList()
          }
        } catch (error) {
          console.error('删除分类失败:', error)
        }
      }).catch(() => {})
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            const method = this.form.id ? 'put' : 'post'
            const url = this.form.id ? `/category/${this.form.id}` : '/category'
            const res = await Request[method](url, this.form)
            if (res.code === '0') {
              this.$message.success(`${this.form.id ? '更新' : '添加'}成功`)
              this.dialogVisible = false
              this.getList()
            }
          } catch (error) {
            console.error('提交表单失败:', error)
          }
        }
      })
    },
    // 添加加载图标列表方法
    async loadIconList() {
      try {
        const res = await Request.get("/dictitem/findByType", {
          params: {
            code: "icon"
          }
        })
        if (res.code === '0') {
          this.iconDict = res.data
        } else {
          this.$message({
            type: 'error',
            message: '获取图标列表失败'
          })
        }
      } catch (error) {
        console.error('获取图标列表失败:', error)
        this.$message.error('获取图标列表失败')
      }
    }
  }
}
</script>

<style scoped>
.category-manager {
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

.operation-buttons {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

/* 美化表格 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: none;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 500;
  color: #1f2f3d;
  padding: 12px 0;
}

:deep(.el-table td) {
  padding: 12px 0;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #fafcff;
}

/* 美化对话框 */
:deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

:deep(.el-dialog__header) {
  padding: 24px;
  border-bottom: 1px solid #ebeef5;
  margin: 0;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 500;
  color: #1f2f3d;
}

:deep(.el-dialog__body) {
  padding: 32px 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
}

/* 美化表单 */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #1f2f3d;
}

:deep(.el-input__inner) {
  border-radius: 6px;
}

.delete-btn {
  color: #f56c6c;
  margin-left: 12px;
}

.delete-btn:hover {
  color: #ff4949;
}
</style> 