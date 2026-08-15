<template>
  <div class="login-container">
    <div class="login-header">
      <h2 class="login-title" @click="goToHome">农 产 品 销 售 系 统</h2>
    </div>

    <el-form ref="loginForm" :model="loginForm" :rules="rules" class="login-form">
      <el-form-item prop="username">
        <div class="custom-input">
          <i class="el-icon-user"></i>
          <el-input v-model="loginForm.username" placeholder="用户名"></el-input>
        </div>
      </el-form-item>

      <el-form-item prop="password">
        <div class="custom-input">
          <i class="el-icon-lock"></i>
          <el-input type="password" v-model="loginForm.password" placeholder="密码"></el-input>
        </div>
      </el-form-item>

      <el-form-item>
        <div class="validate-container">
          <div class="custom-input">
            <i class="el-icon-key"></i>
            <el-input v-model="loginForm.validCode" placeholder="验证码"></el-input>
          </div>
          <ValidCode @input="createValidCode" class="validate-code" />
        </div>
      </el-form-item>

      <div class="login-options">
        <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
        <el-link type="success" @click="$router.push('/forget')" class="forget-link">
          忘记密码？
        </el-link>
      </div>

      <div class="button-group">
        <el-button type="success" class="login-button" @click="onLogin">
          <i class="el-icon-right"></i>
          登录
        </el-button>
        <el-button type="primary" class="register-button" @click="toRegister">
          <i class="el-icon-plus"></i>
          注册
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script>
import ValidCode from "../components/Validate";
import request from "@/utils/request";
import { setRoutes } from "@/router";

export default {
  name: 'Login',
  components: {
    ValidCode
  },
  data() {
    return {
      validCode: '', //通过valicode获取的验证码
      loginForm: {
        username: '',
        password: '',
        validCode: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ]
      },
      rememberMe: false
    };
  },
  mounted() {
    this.loadRememberedCredentials();
  },
  methods: {
    goToHome() {
      this.$router.push('/');
    },
    toRegister() {
      this.$router.push("/register");
    },
    createValidCode(data) {
      this.validCode = data
    },
    loadRememberedCredentials() {
      const remember = localStorage.getItem('rememberMe') === 'true';
      if (remember) {
        this.rememberMe = true;
        this.loginForm.username = localStorage.getItem('username') || '';
        this.loginForm.password = localStorage.getItem('password') || '';
      }
    },
    onLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          // 验证码比较时转换为小写，实现不区分大小写
          if (this.loginForm.validCode.toLowerCase() !== this.validCode.toLowerCase()) {
            this.$message.error("验证码错误");
            return;
          }
          request.post("/user/login", this.loginForm)
            .then(res => {
              if (res.code === "0") {
                this.$message({
                  message: "登录成功",
                  type: "success",
                  duration: 1000,
                  showClose: true
                })
                if (res.data.token) {
                  localStorage.setItem("token", res.data.token);
                }
                if (res.data) {
                  if (res.data.role === 'USER') {
                    localStorage.setItem("frontUser", JSON.stringify(res.data));
                  } else {
                    localStorage.setItem("backUser", JSON.stringify(res.data));
                  }
                }

                // 根据用户角色决定跳转路径
                if (res.data.role !== 'USER') {
                  if (res.data.menuList) {
                    localStorage.setItem("userMenuList", JSON.stringify(res.data.menuList));
                  }
                  setRoutes();
                  this.$router.push('/showView');
                } else {
                  this.$router.push('/');
                }

                if (this.rememberMe) {
                  localStorage.setItem('rememberMe', 'true');
                  localStorage.setItem('username', this.loginForm.username);
                  localStorage.setItem('password', this.loginForm.password);
                } else {
                  localStorage.removeItem('rememberMe');
                  localStorage.removeItem('username');
                  localStorage.removeItem('password');
                }
              } else {
                this.$message.error(res.msg);
              }
            })
            .catch(error => {
              console.error("登录失败:", error);
            });
        } else {
          return false;
        }
      });
    }
  }
};
</script>

<style scoped>
.login-container {
  width: 100%;
  max-width: 380px;
  margin: 0 auto;
  padding-top: 5px;
}

.login-header {
  text-align: center;
  margin-bottom: 5px;
}

.login-title {
  font-size: 32px;
  color: #2c3e50;
  margin-top: 20px;
  margin-bottom: 20px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.3s;
}

.login-title:hover {
  color: #67C23A;
}

.login-form {
  margin-top: 40px;
}

.validate-container {
  display: flex;
  gap: 16px;
}

.validate-code {
  flex-shrink: 0;
  width: 130px;
  height: 50px;
  border-radius: 8px;
  overflow: hidden;
}

.button-group {
  display: flex;
  gap: 15px;
  margin-top: 25px;
}

.login-button, .register-button {
  flex: 1;
  height: 50px;
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 1px;
  border-radius: 8px;
  transition: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.login-button {
  background: linear-gradient(135deg, #67C23A, #85ce61);
  border-color: #67C23A;
}

.register-button {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  border-color: #409EFF;
}

.login-button:hover, .register-button:hover {
  transform: none;
  box-shadow: none;
}

.login-button i, .register-button i {
  margin-right: 8px;
  font-size: 18px;
}

.custom-input {
  position: relative;
  width: 100%;
}

.custom-input i {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  color: #67C23A;
  font-size: 18px;
}

.custom-input .el-input {
  width: 100%;
}

:deep(.el-input__inner) {
  height: 50px;
  line-height: 50px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  transition: none;
  font-size: 16px;
  background-color: #f9f9f9;
  padding-left: 45px;
}

:deep(.el-input__inner:focus) {
  border-color: #67C23A;
  box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2);
  background-color: #fff;
}

:deep(.el-form-item) {
  margin-bottom: 25px;
}

@keyframes fadeIn {
  from {
    opacity: 1;
    transform: none;
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* 响应式调整 */
@media (max-width: 480px) {
  .login-container {
    padding: 0 20px;
  }
  
  .login-title {
    font-size: 28px;
  }
  
  :deep(.el-input__inner) {
    height: 48px;
    line-height: 48px;
    font-size: 15px;
  }
  
  .button-group {
    flex-direction: column;
    gap: 12px;
  }
  
  .login-button, .register-button {
    height: 48px;
    font-size: 17px;
  }
  
  .validate-container {
    gap: 12px;
  }
  
  .validate-code {
    width: 120px;
    height: 48px;
  }
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 18px 0 25px;
}

:deep(.el-checkbox__label) {
  color: #606266;
  font-size: 15px;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #67C23A;
  border-color: #67C23A;
}

:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #67C23A;
}

.forget-link, .register-link {
  font-size: 15px;
}

/* 移除之前的登录按钮和注册链接样式 */
.login-actions {
  display: none;
}
</style>