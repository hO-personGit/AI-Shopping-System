<template>
  <div class="carousel-manager">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>轮播图管理</h2>
      <el-tag type="info">共 {{ total }} 个轮播图</el-tag>
    </div>

    <div class="content-box">
      <!-- 操作栏 -->
      <el-card class="operation-area" shadow="hover">
        <div class="control-btns">
          <div class="right-btns">
            <el-button type="primary" plain size="medium" @click="handleAdd">
              <i class="el-icon-plus"></i> 新增轮播图
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 表格区域 -->
      <el-card class="table-card" shadow="hover">
        <el-table :data="carouselList" 
                 border 
                 v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column label="图片" width="220" align="center">
            <template slot-scope="scope">
              <img :src="'api'+scope.row.imageUrl" class="preview-image">
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="120" />
          <el-table-column prop="tag" label="标签" width="120" />
          <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status ? 'success' : 'info'">
                {{ scope.row.status ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="primary" plain @click="handleEdit(scope.row)">
                <i class="el-icon-edit"></i> 编辑
              </el-button>
              <el-button size="mini" type="danger" plain @click="handleDelete(scope.row)">
                <i class="el-icon-delete"></i> 删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <pagination v-show="total > 0"
                   :total="total"
                   :page.sync="queryParams.currentPage"
                   :limit.sync="queryParams.size"
                   @pagination="getList" />
      </el-card>

      <!-- 新增/编辑弹窗 -->
      <el-dialog :title="dialogTitle" 
                :visible.sync="dialogVisible" 
                width="700px"
                :close-on-click-modal="false">
        <el-form :model="form" :rules="rules" ref="form" label-width="100px">
          <el-form-item label="图片" prop="imageUrl">
            <el-upload class="carousel-uploader"
                      action="#"
                      :show-file-list="false"
                      :auto-upload="false"
                      :on-change="handleUpload">
              <img v-if="form.imageUrl" :src="'api'+form.imageUrl" class="carousel-image">
              <div v-else class="carousel-uploader-placeholder">
                <i class="el-icon-plus"></i>
                <div class="placeholder-text">点击上传图片</div>
              </div>
            </el-upload>
            <div class="upload-tip">建议尺寸：900×300px，支持 jpg、png 格式，大小不超过 10MB</div>
          </el-form-item>
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="标签" prop="tag">
            <el-input v-model="form.tag" placeholder="请输入标签" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" 
                      v-model="form.description" 
                      :autosize="{ minRows: 3, maxRows: 5 }"
                      placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="关联商品" prop="productId">
            <el-select v-model="form.productId" 
                      filterable 
                      clearable
                      placeholder="请选择商品"
                      style="width: 100%;">
              <el-option label="不关联商品" :value="null"></el-option>
              <el-option v-for="item in productList"
                        :key="item.id"
                        :label="item.name"
                        :value="item.id">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="form.sortOrder" 
                           :min="0"
                           controls-position="right"
                           style="width: 120px;" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-switch v-model="form.status"
                      :active-value="1"
                      :inactive-value="0">
            </el-switch>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button @click="dialogVisible = false" plain>取 消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading" plain>确 定</el-button>
        </div>
      </el-dialog>

      <!-- 图片裁剪弹窗 -->
      <el-dialog title="图片裁剪" 
                :visible.sync="cropperVisible" 
                width="800px" 
                append-to-body
                :close-on-click-modal="false">
        <div class="cropper-content">
          <vue-cropper ref="cropper"
                      :img="cropImage"
                      :info="true"
                      :autoCrop="true"
                      :autoCropWidth="900"
                      :autoCropHeight="300"
                      :fixedBox="true"
                      :fixed="true"
                      :fixedNumber="[3, 1]"
                      :centerBox="true"
                      outputType="png">
          </vue-cropper>
        </div>
        <div slot="footer" class="dialog-footer">
          <el-button @click="cropperVisible = false" plain>取 消</el-button>
          <el-button type="primary" @click="handleCropUpload" :loading="uploadLoading" plain>确 定</el-button>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import Request from '../utils/request.js'
import Pagination from '../components/Pagination/index.vue'
import { VueCropper } from 'vue-cropper'

export default {
  name: 'CarouselManager',
  components: { Pagination, VueCropper },
  inject: ['userInfo'],
  data() {
    return {
      // 查询参数
      queryParams: {
        currentPage: 1,
        size: 10
      },
      total: 0,
      carouselList: [],
      productList: [],
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: undefined,
        imageUrl: '',
        title: '',
        tag: '',
        description: '',
        productId: null,
        sortOrder: 0,
        status: 1
      },
      cropperVisible: false,
      cropImage: '',
      uploadFile: null,
      rules: {
        imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }],
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
      },
      loading: false,
      submitLoading: false,
      uploadLoading: false
    }
  },
  created() {
    this.getList()
    this.getProducts()
  },
  methods: {
    // 获取轮播图列表
    async getList() {
      try {
        this.loading = true
        const res = await Request.get('/carousel/page', {
          params: this.queryParams
        })
        if (res.code === '0') {
          this.carouselList = res.data.records
          this.total = res.data.total
        }
      } catch (error) {
        console.error('获取轮播图列表失败:', error)
        this.$message.error('获取轮播图列表失败')
      } finally {
        this.loading = false
      }
    },
    // 获取商品列表
    async getProducts() {
      try {
        const res = await Request.get('/product/all')
        if (res.code === '0') {
          this.productList = res.data
        }
      } catch (error) {
        console.error('获取商品列表失败:', error)
      }
    },
    // 处理新增
    handleAdd() {
      this.dialogTitle = '新增轮播图'
      this.form = {
        imageUrl: '',
        title: '',
        tag: '',
        description: '',
        productId: null,
        sortOrder: 0,
        status: 1
      }
      this.dialogVisible = true
    },
    // 处理编辑
    handleEdit(row) {
      this.dialogTitle = '编辑轮播图'
      this.form = { ...row }
      this.dialogVisible = true
    },
    // 处理删除
    async handleDelete(row) {
      try {
        await this.$confirm('确认删除该轮播图?', '提示', {
          type: 'warning'
        })
        const res = await Request.delete(`/carousel/${row.id}`)
        if (res.code === '0') {
          this.$message.success('删除成功')
          this.getList()
        }
      } catch (error) {
        console.error('删除轮播图失败:', error)
      }
    },
    // 处理图片上传
    handleUpload(file) {
      // 检查文件类型和大小
      const isImage = file.raw.type.startsWith('image/')
      const isLt10M = file.raw.size / 1024 / 1024 < 10

      if (!isImage) {
        this.$message.error('上传文件只能是图片格式!')
        return
      }
      if (!isLt10M) {
        this.$message.error('上传图片大小不能超过 10MB!')
        return
      }

      // 保存原始文件
      this.uploadFile = file.raw
      // 读取文件并显示裁剪框
      const reader = new FileReader()
      reader.onload = (e) => {
        this.cropImage = e.target.result
        this.cropperVisible = true
      }
      reader.readAsDataURL(file.raw)
    },
    // 处理裁剪上传
    handleCropUpload() {
      this.$refs.cropper.getCropBlob(async (blob) => {
        try {
          this.uploadLoading = true
          // 创建文件名
          const fileName = this.uploadFile.name || 'carousel-image.png'
          
          // 创建 File 对象
          const file = new File([blob], fileName, { type: 'image/png' })
          
          // 创建 FormData
          const formData = new FormData()
          formData.append('file', file)
          
          // 发送上传请求
          const res = await Request.post('/file/upload/img', formData, {
            headers: {
              'Content-Type': 'multipart/form-data',
              'token': this.userInfo.token
            }
          })
          
          if (res.code === '0') {
            this.form.imageUrl = res.data
            this.$message.success('上传成功')
            this.cropperVisible = false
          } else {
            this.$message.error(res.msg || '上传失败')
          }
        } catch (error) {
          console.error('上传失败:', error)
          this.$message.error('上传失败')
        } finally {
          this.uploadLoading = false
        }
      })
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          try {
            this.submitLoading = true
            const method = this.form.id ? 'put' : 'post'
            const url = this.form.id ? `/carousel/${this.form.id}` : '/carousel'
            const res = await Request[method](url, this.form)
            if (res.code === '0') {
              this.$message.success(`${this.form.id ? '更新' : '新增'}成功`)
              this.dialogVisible = false
              this.getList()
            }
          } catch (error) {
            console.error('保存轮播图失败:', error)
            this.$message.error('操作失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.carousel-manager {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;

  h2 {
    font-size: 24px;
    font-weight: 500;
    color: #1f2f3d;
    margin: 0;
    margin-right: 15px;
  }
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

.control-btns {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;

  .right-btns {
    display: flex;
    gap: 10px;
  }
}

.preview-image {
  height: 60px;
  width: 180px;
  border-radius: 4px;
  object-fit: contain;
  background-color: #f5f7fa;
}

.carousel-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 450px;
  height: 150px;
  display: flex;
  justify-content: center;
  align-items: center;

  &:hover {
    border-color: #409EFF;
  }

  .carousel-image {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
    background-color: #f5f7fa;
  }

  .carousel-uploader-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: #8c939d;
    background-color: #f5f7fa;

    .el-icon-plus {
      font-size: 28px;
      margin-bottom: 8px;
      color: #c0c4cc;
    }

    .placeholder-text {
      font-size: 14px;
      color: #909399;
    }
  }
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.cropper-content {
  height: 500px;
}

:deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;

  .el-dialog__header {
    padding: 24px;
    border-bottom: 1px solid #ebeef5;
    margin: 0;
  }

  .el-dialog__body {
    padding: 32px 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px;
    border-top: 1px solid #ebeef5;
  }
}
</style> 