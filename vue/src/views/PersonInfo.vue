<template>
  <div class="person-info" style="padding: 24px; background-color: #f0f2f5; min-height: calc(100vh - 60px);">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>个人信息</h2>
      <el-tag type="info">{{ getRoleName(userInfo.role) }}</el-tag>
    </div>

    <!-- 信息卡片 -->
    <div class="card-container">
      <el-card class="info-card" shadow="hover" v-loading="loading">
        <el-form 
          :model="userInfo" 
          :rules="rules" 
          ref="userInfoForm" 
          label-width="80px"
          class="info-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userInfo.username" disabled>
              <template slot="prepend">
                <i class="el-icon-user"></i>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="姓名" prop="name">
            <el-input v-model="userInfo.name">
              <template slot="prepend">
                <i class="el-icon-edit"></i>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userInfo.email">
              <template slot="prepend">
                <i class="el-icon-message"></i>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="营业执照" v-if="userInfo.role === 'MERCHANT'" prop="businessLicense">
            <div class="license-upload">
              <input 
                type="file" 
                ref="fileInput" 
                style="display: none" 
                accept="image/jpeg,image/png"
                @change="handleFileChange"
              >
              <div v-if="userInfo.businessLicense" class="preview-container">
                <img :src="'/api' + userInfo.businessLicense" class="license-image">
                <div class="image-overlay">
                  <el-button type="text" @click="triggerUpload">重新上传</el-button>
                  <el-button type="text" class="delete-btn" @click="removeLicense">删除</el-button>
                </div>
              </div>
              <el-button v-else type="primary" @click="triggerUpload" size="small">
                <i class="el-icon-upload2"></i> 点击上传
              </el-button>
              <div class="el-upload__tip">只能上传jpg/png文件，且不超过2MB</div>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="update" icon="el-icon-check">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'PersonInfo',
  inject: ['userInfo'],
  
  data() {
    return {
      loading: false,
      rules: {
        name: [
          { required: true, message: '姓名不能为空', trigger: 'blur' },
          { min: 2, max: 10, message: '姓名长度必须在2到10个字符之间', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '邮箱不能为空', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
        ]
      }
    };
  },

  methods: {
    getRoleName(role) {
      const roleMap = {
        'SUPER_ADMIN': '超级管理员',
        'ADMIN': '管理员',
        'MERCHANT': '商户',
        'USER': '普通用户'
      }
      return roleMap[role]
    },

    update() {
      this.$refs.userInfoForm.validate((valid) => {
        if (valid) {
          this.loading = true;
          request.put("/user/" + this.userInfo.id, this.userInfo).then(response => {
            this.loading = false;
            if (response.code == '0') {
              this.$message({
                type: 'success',
                message: '信息保存成功!'
              })
              localStorage.setItem("frontUser", JSON.stringify(this.userInfo))
              this.$emit("update:user", this.userInfo)
            } else {
              this.$message({
                type: 'error',
                message: response.msg
              })
            }
          }).catch(() => {
            this.loading = false;
          })
        } else {
          return false;
        }
      });
    },

    triggerUpload() {
      this.$refs.fileInput.click();
    },

    async handleFileChange(e) {
      const file = e.target.files[0];
      if (!file) return;

      // 验证文件类型和大小
      const isJPGOrPNG = file.type === 'image/jpeg' || file.type === 'image/png';
      const isLt2M = file.size / 1024 / 1024 < 2;

      if (!isJPGOrPNG) {
        this.$message.error('只能上传 JPG/PNG 格式的图片!');
        return;
      }
      if (!isLt2M) {
        this.$message.error('图片大小不能超过 2MB!');
        return;
      }

      // 创建 FormData
      const formData = new FormData();
      formData.append('file', file);

      this.loading = true;
      try {
        const res = await request.post('/file/upload/img', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });

        if (res.code === '0') {
          this.userInfo.businessLicense = res.data;
          // 更新用户信息到本地存储
          const storedUser = JSON.parse(localStorage.getItem('frontUser'));
          if (storedUser) {
            storedUser.businessLicense = res.data;
            localStorage.setItem('frontUser', JSON.stringify(storedUser));
          }
          this.$message.success('上传成功');
        } else {
          this.$message.error(res.msg || '上传失败');
        }
      } catch (error) {
        console.error('上传失败:', error);
        this.$message.error('上传失败');
      } finally {
        this.loading = false;
      }

      // 清空 input 的值，允许重复上传同一个文件
      this.$refs.fileInput.value = '';
    },

    removeLicense() {
      this.$confirm('确定要删除营业执照吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.userInfo.businessLicense = '';
        // 更新用户信息到本地存储
        const storedUser = JSON.parse(localStorage.getItem('frontUser'));
        if (storedUser) {
          storedUser.businessLicense = '';
          localStorage.setItem('frontUser', JSON.stringify(storedUser));
        }
        this.$message.success('删除成功');
      }).catch(() => {});
    }
  }
};
</script>

<style scoped>
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

.card-container {
  max-width: 800px;
  margin: 0 auto;
}

.info-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.info-form {
  max-width: 600px;
  margin: 0 auto;
  padding: 30px 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #1f2f3d;
}

:deep(.el-input__inner) {
  border-radius: 4px;
}

:deep(.el-input-group__prepend) {
  background-color: #f5f7fa;
  border-color: #dcdfe6;
  color: #909399;
  padding: 0 15px;
}

:deep(.el-button) {
  padding: 12px 20px;
  font-size: 14px;
  border-radius: 4px;
  width: 100%;
}

:deep(.el-button--primary) {
  background-color: #409eff;
  border-color: #409eff;
}

:deep(.el-button--primary:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

:deep(.el-tag) {
  height: 28px;
  line-height: 26px;
  padding: 0 10px;
  font-size: 13px;
}

.license-upload {
  text-align: center;
}

.preview-container {
  position: relative;
  display: inline-block;
  width: 280px;
  height: 396px;
}

.license-image {
  width: 280px;
  height: 396px;
  object-fit: contain;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.3s;
  background-color: #f5f7fa;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: all 0.3s;
  border-radius: 4px;
  flex-direction: column;
  gap: 10px;
}

.preview-container:hover .image-overlay {
  opacity: 1;
}

.image-overlay .el-button {
  color: #fff;
  margin: 0 10px;
}

.image-overlay .delete-btn {
  color: #f56c6c;
}

.el-upload__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>