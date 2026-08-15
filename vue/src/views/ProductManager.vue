<template>
  <div class="product-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>商品管理</h2>
      <el-tag type="info">共{{ total }} 件商品</el-tag>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="operation-area" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.name" placeholder="请输入商品名称" clearable></el-input>
        </el-form-item>
        <el-form-item label="商品分类">
          <el-select v-model="searchForm.categoryId" placeholder="请选择分类" clearable>
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="商品状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="上架" :value="1"></el-option>
            <el-option label="下架" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="medium" plain type="primary" @click="handleSearch">查询</el-button>
          <el-button size="medium" plain @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="operation-buttons">
        <el-button v-if="userInfo.role==='MERCHANT'" plain size="medium" type="primary" icon="el-icon-plus" class="add-button" @click="handleAdd">新增商品</el-button>
        <el-button plain size="medium" icon="el-icon-refresh" @click="getList">刷新</el-button>
      </div>
    </el-card>

    <!-- 数据统计卡片 -->
    <div class="statistics-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-goods"></i></div>
                <div class="stat-title">在售商品</div>
              </div>
              <div class="stat-value">{{ getOnSaleCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-sold-out"></i></div>
                <div class="stat-title">已下架</div>
              </div>
              <div class="stat-value">{{ getOffSaleCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-price-tag"></i></div>
                <div class="stat-title">促销商品</div>
              </div>
              <div class="stat-value">{{ getDiscountCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-left">
                <div class="stat-icon"><i class="el-icon-trophy"></i></div>
                <div class="stat-title">销量最高</div>
              </div>
              <div class="stat-value">{{ topSalesProduct?.name || '-' }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 商品列表 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="products" border style="width: 100%" :default-sort="{prop: 'id', order: 'ascending'}">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="商品图片" width="120">
          <template slot-scope="scope">
            <el-image :src="'api'+scope.row.imageUrl" :preview-src-list="['api'+scope.row.imageUrl]" fit="cover" style="width: 80px; height: 80px"></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称"></el-table-column>
        <el-table-column prop="price" label="价格" width="120">
          <template slot-scope="scope">
            ¥{{ scope.row.price.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100"></el-table-column>
        <el-table-column prop="salesCount" label="销量" width="100"></el-table-column>
        <el-table-column prop="category.name" label="分类" width="120"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="() => handleStatusChange(scope.row)" active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </template>
        </el-table-column>
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

    <!-- 商品表单弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="1200px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px" class="product-form">
        <!-- 左侧基础信息 -->
        <div class="form-left">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入商品名称"></el-input>
          </el-form-item>
          <el-form-item label="商品分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%;">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="产地" prop="placeOfOrigin">
            <el-input v-model="form.placeOfOrigin" placeholder="请输入商品产地"></el-input>
          </el-form-item>
          <el-form-item label="商品价格" prop="price">
            <el-input-number v-model="form.price" :precision="2" :step="0.1" :min="0" style="width: 100%;"></el-input-number>
          </el-form-item>
          <el-form-item label="商品库存" prop="stock">
            <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%;"></el-input-number>
          </el-form-item>
          <el-form-item label="商品图片" prop="imageUrl">
            <el-upload class="avatar-uploader" action="#" :show-file-list="false" :auto-upload="false" :on-change="handleUpload">
              <img v-if="form.imageUrl" :src="'api/'+form.imageUrl" class="avatar">
              <i v-else class="el-icon-plus avatar-uploader-icon"></i>
            </el-upload>
          </el-form-item>
          <el-form-item label="开启折扣" class="discount-switch">
            <el-switch v-model="form.isDiscount" :active-value="1" :inactive-value="0"></el-switch>
          </el-form-item>
          <el-form-item label="折扣价格" v-if="form.isDiscount === 1" prop="discountPrice">
            <el-input-number v-model="form.discountPrice" :precision="2" :step="0.1" :min="0" :max="form.price" style="width: 100%;"></el-input-number>
            <span class="discount-tip">原价: ¥{{ form.price }}</span>
          </el-form-item>
        </div>

        <!-- 右侧商品描述 -->
        <div class="form-right">
          <div class="ai-copywriting-toolbar">
            <el-button type="success" size="small" icon="el-icon-magic-stick" :loading="aiCopywritingLoading" @click="handleAiCopywriting">AI生成文案</el-button>
            <span>根据当前商品信息生成标题、简介、详情和营销推荐语</span>
          </div>
          <el-form-item label="商品描述" prop="description" class="description-item">
            <div class="editor-wrapper">
              <Toolbar class="editor-toolbar" :editor="editor" :defaultConfig="toolbarConfig" :mode="mode" />
              <Editor class="editor-content" v-model="form.description" :defaultConfig="editorConfig" :mode="mode" @onCreated="handleCreated"
                @onChange="handleChange" />
            </div>
          </el-form-item>
        </div>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <!-- 图片裁剪弹窗 -->
    <el-dialog title="图片裁剪" :visible.sync="cropperVisible" width="600px" append-to-body :close-on-click-modal="false" :close-on-press-escape="false">
      <div class="cropper-content">
        <vue-cropper ref="cropper" :img="cropImage" :info="true" :autoCrop="true" :autoCropWidth="300" :autoCropHeight="300" :fixedBox="true" :fixed="true"
          :fixedNumber="[1, 1]" :centerBox="true" outputType="png" :full="true" :canMove="true" :canMoveBox="true" :original="false" :enlarge="1"></vue-cropper>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cropperVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCropUpload">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Request from '@/utils/request'
import { generateCopywriting } from '@/api/ai'
import Pagination from '@/components/Pagination/index.vue'
import { VueCropper } from 'vue-cropper'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

export default {
  name: 'ProductManager',
  inject: ['userInfo'],
  components: {
    Pagination,
    VueCropper,
    Editor,
    Toolbar
  },
  data() {
    // 折扣价格验证规则
    const validateDiscountPrice = (rule, value, callback) => {
      if (this.form.isDiscount === 1) {
        if (!value) {
          callback(new Error('请输入折扣价格'))
        } else if (value >= this.form.price) {
          callback(new Error('折扣价必须低于原价'))
        } else {
          callback()
        }
      } else {
        callback()
      }
    }

    return {
      // 搜索表单
      searchForm: {
        name: '',
        categoryId: '',
        status: ''
      },
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      // 商品列表
      products: [],
      // 分类列表
      categories: [],
      // 总数
      total: 0,
      // 弹窗显示
      dialogVisible: false,
      // 弹窗标题
      dialogTitle: '',
      // 表单数据
      form: {
        id: undefined,
        name: '',
        categoryId: '',
        price: 0,
        stock: 0,
        imageUrl: '',
        description: '',
        isDiscount: 0,
        discountPrice: 0,
        placeOfOrigin: ''
      },
      // 表单验证规则
      rules: {
        name: [
          { required: true, message: '请输入商品名称', trigger: 'blur' }
        ],
        categoryId: [
          { required: true, message: '请选择商品分类', trigger: 'change' }
        ],
        price: [
          { required: true, message: '请输入商品价格', trigger: 'blur' }
        ],
        stock: [
          { required: true, message: '请输入商品库存', trigger: 'blur' }
        ],
        discountPrice: [
          { validator: validateDiscountPrice, trigger: 'blur' }
        ]
      },
      // 图片裁剪相关
      cropperVisible: false,
      cropImage: '',
      uploadFile: null,
      topSalesProduct: null,
      aiCopywritingLoading: false,
      editor: null,
      mode: 'default',
      toolbarConfig: {
        excludeKeys: [
          'group-video'
        ]
      },
      editorConfig: {
        placeholder: '请输入商品描述...',
        MENU_CONF: {
          uploadImage: {
            base64LimitSize: 5 * 1024 * 1024,
            customUpload: async (file, insertFn) => {
              try {
                const isImage = file.type.startsWith('image/')
                if (!isImage) {
                  this.$message.error('上传文件只能是图片格式')
                  return
                }
                const fileSize = file.size / 1024 / 1024
                if (fileSize > 10) {
                  this.$message.error('图片大小不能超过 10MB!')
                  return
                }
                const formData = new FormData()
                formData.append('file', file)
                const res = await Request.post('/file/upload/img', formData, {
                  headers: {
                    'Content-Type': 'multipart/form-data',
                    'token': this.userInfo.token
                  }
                })
                if (res.code === '0') {
                  insertFn(`/api${res.data}`)
                  this.$message.success('图片上传成功')
                } else {
                  this.$message.error(res.msg || '图片上传失败')
                }
              } catch (error) {
                console.error('上传图片失败:', error)
                this.$message.error('图片上传失败')
              }
            }
          }
        }
      }
    }
  },
  created() {
    this.getCategories()
    this.getList()
  },
  methods: {
    // 获取分类列表
    async getCategories() {
      try {
        const res = await Request.get('/category/all')
        if (res.code === '0') {
          this.categories = res.data
        }
      } catch (error) {
        console.error('获取分类列表失败:', error)
      }
    },
    // 获取商品列表
    async getList() {
      if(this.userInfo.role === 'MERCHANT'){
        this.queryParams.merchantId = this.userInfo.id
      }
      try {
        const params = {
          ...this.queryParams,
          name: this.searchForm.name,
          categoryId: this.searchForm.categoryId,
          status: this.searchForm.status
        }
        const res = await Request.get('/product/page', { params })
        if (res.code === '0') {
          this.products = res.data.records || []
          this.total = res.data.total
          this.topSalesProduct = this.products.length > 0
            ? this.products.reduce((max, current) =>
                (current.salesCount > (max?.salesCount || 0)) ? current : max, null)
            : null
        }
      } catch (error) {
        console.error('获取商品列表失败:', error)
        this.products = []
        this.total = 0
        this.topSalesProduct = null
      }
    },
    // 查询
    handleSearch() {
      this.queryParams.currentPage = 1
      this.getList()
    },
    // 重置查询
    resetSearch() {
      this.searchForm = {
        name: '',
        categoryId: '',
        status: ''
      }
      this.handleSearch()
    },
    // 新增商品
    handleAdd() {
      this.dialogTitle = '新增商品'
      this.form = {
        name: '',
        categoryId: '',
        price: 0,
        stock: 0,
        imageUrl: '',
        description: '',
        isDiscount: 0,
        discountPrice: 0,
        placeOfOrigin: ''
      }
      this.dialogVisible = true
    },
    // 编辑商品
    handleEdit(row) {
      this.dialogTitle = '编辑商品'
      this.form = { ...row }
      this.dialogVisible = true
    },
    // 删除商品
    handleDelete(row) {
      this.$confirm('确认删除该商品？', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await Request.delete(`/product/${row.id}`)
          if (res.code === '0') {
            this.$message.success('删除成功')
            this.getList()
          }else{
            this.$message({
              message: res.msg,
              type: 'error'
            })
          }
        } catch (error) {
          console.error('删除商品失败:', error)
        }
      }).catch(() => { })
    },
    // 更改商品上下架状态
    async handleStatusChange(row) {
      try {
        const res = await Request.put(`/product/${row.id}/status?status=${row.status}`)
        if (res.code === '0') {
          this.$message.success('状态更新成功')
        } else {
          row.status = row.status === 1 ? 0 : 1
          this.$message.error('状态更新失败')
        }
      } catch (error) {
        row.status = row.status === 1 ? 0 : 1
        console.error('更新商品状态失败', error)
        this.$message.error('更新失败')
      }
    },
    // 图片选择回调
    handleUpload(file) {
      const isImage = file.raw.type.startsWith('image/')
      const isLt10M = file.raw.size / 1024 / 1024 < 10
      if (!isImage) {
        this.$message.error('上传文件只能是图片格式')
        return false
      }
      if (!isLt10M) {
        this.$message.error('上传图片大小不能超过 10MB!')
        return false
      }
      this.uploadFile = file.raw
      const reader = new FileReader()
      reader.readAsDataURL(file.raw)
      reader.onload = () => {
        this.cropImage = reader.result
        this.cropperVisible = true
      }
      return false
    },
    // 裁剪后上传
    async handleCropUpload() {
      try {
        const blob = await new Promise((resolve) => {
          this.$refs.cropper.getCropBlob((data) => resolve(data))
        })
        const fileName = `cropped-${Date.now()}.png`
        const file = new File([blob], fileName, { type: 'image/png' })
        const formData = new FormData()
        formData.append('file', file)
        const res = await Request.post('/file/upload/img', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
            'token': this.userInfo.token
          }
        })
        if (res.code === '0') {
          this.form.imageUrl = res.data
          this.$message.success('图片上传成功')
          this.cropperVisible = false
        } else {
          throw new Error(res.msg || '上传失败')
        }
      } catch (error) {
        console.error('图片上传失败:', error)
        this.$message.error(error.message || '图片上传失败')
      }
    },
    getSelectedCategoryName() {
      const category = this.categories.find(item => item.id === this.form.categoryId)
      return category ? category.name : ''
    },
    formatAiDescription(data) {
      const detail = (data.detail || '').replace(/\n/g, '<br/>')
      return `<p><strong>商品简介：</strong>${data.summary || ''}</p><p><strong>商品详情：</strong></p><p>${detail}</p><p><strong>营销推荐语：</strong>${data.slogan || ''}</p>`
    },
    // AI生成文案
    async handleAiCopywriting() {
      this.aiCopywritingLoading = true
      try {
        const res = await generateCopywriting({
          name: this.form.name,
          category: this.getSelectedCategoryName(),
          price: this.form.price,
          stock: this.form.stock,
          placeOfOrigin: this.form.placeOfOrigin,
          description: this.form.description
        })
        if (res.code === '0') {
          const data = res.data || {}
          this.form.name = data.title || this.form.name
          this.form.description = this.formatAiDescription(data)
          this.$message.success('AI文案已生成，可继续编辑后保存')
        } else {
          this.$message.error(res.msg || 'AI文案生成失败')
        }
      } catch (error) {
        this.$message.error('AI文案服务暂不可用，请确认后端和AI服务已启动')
      } finally {
        this.aiCopywritingLoading = false
      }
    },
    // 提交表单
    submitForm() {
      if(this.userInfo.role==='MERCHANT'){
        this.form.merchantId=this.userInfo.id
      }
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          try {
            const method = this.form.id ? 'put' : 'post'
            const url = this.form.id ? `/product/${this.form.id}` : '/product'
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
    handleCreated(editor) {
      this.editor = Object.seal(editor)
    },
    handleChange(editor) {
      this.form.description = editor.getHtml()
    }
  },
  computed: {
    getOnSaleCount() {
      return this.products ? this.products.filter(p => p.status === 1).length : 0
    },
    getOffSaleCount() {
      return this.products ? this.products.filter(p => p.status === 0).length : 0
    },
    getDiscountCount() {
      return this.products ? this.products.filter(p => p.isDiscount === 1).length : 0
    }
  },
  beforeDestroy() {
    if (this.editor) {
      this.editor.destroy()
    }
  },
  mounted() {
  }
}
</script>

<style scoped>
.product-manager {
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
  text-align: center;
  width: 100px;
  gap: 4px;
  align-items: flex-start;
}

.stat-icon {
  font-size: 24px;
  color: #409eff;
  opacity: 0.8;
  width: 100%;
  text-align: center;
}

.stat-title {
  font-size: 13px;
  color: #909399;
  width: 100%;
  text-align: center;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1f2f3d;
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

.discount-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}

:deep(.el-switch.is-checked .el-switch__core) {
  border-color: #67c23a;
  background-color: #67c23a;
}

.product-form {
  display: flex;
  gap: 24px;
}

.form-left {
  width: 380px;
  flex-shrink: 0;
}

.form-right {
  flex: 1;
  min-width: 0;
}

.description-item {
  height: 100%;
  margin-bottom: 0;
}

.ai-copywriting-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #67c23a;
  font-size: 13px;
}

.editor-wrapper {
  height: 600px;
  display: flex;
  flex-direction: column;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.editor-toolbar {
  border-bottom: 1px solid #dcdfe6;
  background-color: #fff;
  flex-shrink: 0;
}

.editor-content {
  flex: 1;
  overflow-y: auto;
}

:deep(.w-e-text-container) {
  height: calc(100% - 40px) !important;
  overflow-y: auto !important;
}

:deep(.w-e-scroll) {
  height: 100% !important;
  min-height: unset !important;
}

.form-left :deep(.el-form-item) {
  margin-bottom: 22px;
}

.form-left :deep(.el-input-number) {
  width: 100%;
}

.form-left :deep(.el-select) {
  width: 100%;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

:deep(.w-e-text-container) {
  height: 100% !important;
}

:deep(.w-e-toolbar) {
  padding: 8px !important;
}

:deep(.w-e-bar-item) {
  padding: 4px 6px !important;
}

:deep(.w-e-bar-item button) {
  padding: 4px 10px !important;
}

:deep(.w-e-text-container) {
  background-color: #fff !important;
}

:deep(.w-e-panel-content-image) {
  max-height: 400px !important;
}

:deep(.w-e-panel-content-image .w-e-image-container) {
  min-height: 100px !important;
}

:deep(.w-e-progress-bar) {
  background-color: #409eff !important;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

.discount-switch {
  margin-bottom: 16px;
}

:deep(.w-e-text-container img) {
  max-width: 100%;
  height: auto;
  margin: 8px 0;
  border-radius: 4px;
}

:deep(.w-e-panel-content-image) {
  max-height: 400px !important;
  padding: 16px !important;
}

:deep(.w-e-progress-bar) {
  background-color: #409eff !important;
}

:deep(.w-e-panel-content-image .w-e-image-container) {
  min-height: 100px !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.w-e-text-placeholder) {
  color: #909399 !important;
}

.cropper-content {
  height: 400px;
  width: 100%;
}

:deep(.cropper-view-box) {
  border-radius: 4px;
  border: 1px solid #409eff;
}

:deep(.cropper-container) {
  width: 100% !important;
  height: 100% !important;
}

:deep(.vue-cropper) {
  width: 100% !important;
  height: 100% !important;
}

:deep(.el-dialog__body) {
  padding: 10px 20px;
}

.cropper-content {
  background-color: #f5f7fa;
  border-radius: 4px;
}

:deep(.cropper-point) {
  background-color: #409eff;
  width: 8px;
  height: 8px;
}

:deep(.cropper-line) {
  background-color: #409eff;
}

:deep(.cropper-face) {
  background-color: inherit;
}
</style>
