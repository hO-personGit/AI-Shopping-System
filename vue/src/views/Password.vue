<template>
  <div class="password-page" style="padding: 24px; background-color: #f0f2f5; min-height: calc(100vh - 60px);">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h2>修改密码</h2>
      <el-tag type="info">{{ getRoleName(userInfo.role) }}</el-tag>
    </div>

    <!-- 密码修改卡片 -->
    <div class="card-container">
      <el-card class="password-card" shadow="hover">
        <el-form 
          :model="passwordForm" 
          :rules="passwordRules" 
          ref="passwordForm" 
          label-width="100px"
          class="password-form"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input 
              type="password" 
              v-model="passwordForm.oldPassword" 
              :show-password="true"
              autocomplete="off"
            >
              <template slot="prepend">
                <i class="el-icon-lock"></i>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input 
              type="password" 
              v-model="passwordForm.newPassword" 
              :show-password="true"
              autocomplete="off"
            >
              <template slot="prepend">
                <i class="el-icon-key"></i>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input 
              type="password" 
              v-model="passwordForm.confirmPassword" 
              :show-password="true"
              autocomplete="off"
            >
              <template slot="prepend">
                <i class="el-icon-check"></i>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item class="button-group">
            <el-button type="primary" @click="submitForm" icon="el-icon-check">
              确认修改
            </el-button>
            <el-button @click="resetForm" icon="el-icon-refresh" plain>
              重置
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
  inject: ['userInfo'],
  
  data() {
    const validatePassword = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('密码长度不能少于6位'));
      } else {
        callback();
      }
    };
    
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入密码不一致'));
      } else {
        callback();
      }
    };
    
    return {
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: '',
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入旧密码', trigger: 'blur' },
        ],
        newPassword: [
          { required: true, validator: validatePassword, trigger: 'blur' },
        ],
        confirmPassword: [
          { required: true, validator: validateConfirmPassword, trigger: 'blur' },
        ],
      },
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

    submitForm() {
      this.$refs.passwordForm.validate((valid) => {
        if (valid) {
          request.put('/user/password/' + this.userInfo.id, this.passwordForm).then(response => {
            if (response.code == '0') {
              this.$message({
                type: 'success',
                message: '密码修改成功!请重新登录！'
              });
              this.resetForm();
              localStorage.removeItem("backUser");
              this.$router.push({ path: '/login'});

            } else {
              this.$message({
                type: 'error',
                message: response.msg
              });
            }
          });
        } else {
          return false;
        }
      });
    },

    resetForm() {
      this.$refs.passwordForm.resetFields();
    },
  },
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

.password-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.password-form {
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

.button-group {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

:deep(.el-button) {
  flex: 1;
  padding: 12px 20px;
  font-size: 14px;
  border-radius: 4px;
}

:deep(.el-button--primary) {
  background-color: #409eff;
  border-color: #409eff;
}

:deep(.el-button--primary:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

:deep(.el-button--default) {
  border-color: #dcdfe6;
}

:deep(.el-button--default:hover) {
  border-color: #c6e2ff;
  color: #409eff;
}

:deep(.el-tag) {
  height: 28px;
  line-height: 26px;
  padding: 0 10px;
  font-size: 13px;
}
</style>