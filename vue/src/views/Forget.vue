<template>
    <div class="forget-container">
        <h2 class="forget-title">重设安全密钥</h2>
        <p class="forget-subtitle">请输入您的身份凭证接收验证</p>

        <el-form ref="resetForm" :model="resetForm" :rules="rules" class="forget-form">
            <div class="form-content">
                <el-form-item prop="email">
                    <el-input 
                        v-model="resetForm.email" 
                        placeholder="请输入邮箱"
                        prefix-icon="el-icon-message">
                    </el-input>
                </el-form-item>

                <el-form-item prop="code">
                    <div class="validate-container">
                        <el-input 
                            v-model="resetForm.code" 
                            placeholder="请输入验证码"
                            prefix-icon="el-icon-chat-line-round">
                        </el-input>
                        <el-button 
                            type="success" 
                            :disabled="disabled"
                            class="validate-btn"
                            @click="sendVerificationCode">
                            {{ buttonContent }}
                        </el-button>
                    </div>
                </el-form-item>

                <el-form-item prop="newPassword">
                    <el-input 
                        type="password" 
                        v-model="resetForm.newPassword" 
                        placeholder="请设置新密码"
                        prefix-icon="el-icon-lock">
                    </el-input>
                </el-form-item>

                <el-form-item>
                    <el-button 
                        type="success" 
                        class="submit-button"
                        @click="onResetPassword">
                        重置密码
                    </el-button>
                </el-form-item>

                <div class="form-footer">
                    <el-link type="success" @click="$router.push('/login')">
                        <i class="el-icon-arrow-left"></i> 返回登录
                    </el-link>
                </div>
            </div>
        </el-form>
    </div>
</template>

<script>
import request from "@/utils/request";

export default {
    name: 'ResetPassword',
    data() {
        return {
            countdown: 0,
            disabled: false,
            timer: null,
            emailCode: '',
            buttonContent: '发送验证码',
            resetForm: {
                email: '',
                code: '',
                newPassword: '',
            },
            rules: {
                email: [
                    { required: true, message: '请输入邮箱', trigger: 'blur' },
                    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
                ],
                code: [
                    { required: true, message: '请输入验证码', trigger: 'blur' }
                ],
                newPassword: [
                    { required: true, message: '请输入新密码', trigger: 'blur' },
                    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
                ]
            }
        };
    },
    methods: {
        sendVerificationCode() {
            // 发送重置密码请求到后端
            if (this.disabled) return; // 如果按钮已禁用，则不执行发送逻辑

            // 发送验证码的逻辑...
            console.log('验证码发送逻辑');
      request.get(`/email/findByEmail/${this.resetForm.email}`)
            .then(res => {
                if (res.code == '0') {
                    this.$message({
                        type: 'success',
                        message: "验证码已发送到您的邮箱,请查收"
                    })
                    this.emailCode = res.data;
                } else {
                    this.$message({
                        type: 'error',
                        message: res.msg
                    })
                    return
                }
            })
            this.countdown = 60; // 设置倒计时为60秒
            this.disabled = true; // 禁用按钮
            this.buttonContent = `${this.countdown}秒后可重发`; // 更新按钮文本为倒计时

            this.timer = setInterval(() => {
                if (this.countdown > 0) {
                    this.countdown--; // 每秒减少1秒
                    this.buttonContent = `${this.countdown}秒后可重发`; // 更新按钮文本
                } else {
                    clearInterval(this.timer); // 清除定时器
                    this.countdown = 0; // 重置倒计时
                    this.disabled = false; // 启用按钮
                    this.buttonContent = '发送验证码'; // 重置按钮文本
                }
            }, 1000);

        
    },
    onResetPassword() {
        this.$refs.resetForm.validate((valid) => {
            if (valid) {
                if (this.resetForm.code != this.emailCode) {
                    this.$message({
                        type: 'error',
                        message: '验证码不正确'
                    });
                    return;
                }
                // 发送重置密码请求到后端
                request.get("/user/forget", {
                    params: {
                        email: this.resetForm.email,
                        newPassword: this.resetForm.newPassword,

                    },
                }).then(res => {
                    if (res.code == '0') {
                        this.$message({
                            type: 'success',
                            message: "密码重置成功"
                        });
                        this.$router.push("/login");
                    } else {
                        this.$message({
                            type: 'error',
                            message: res.msg
                        });
                    }
                });
            } else {
                console.error('重置密码失败: 表单校验失败');
                return false;
            }
        });
    }
},
beforeDestroy() {
    clearInterval(this.timer);
},
  };
</script>

<style scoped>
.forget-container {
    width: 100%;
    max-width: 380px;
    animation: fadeIn 0.6s ease-out;
}

.forget-title {
    font-size: 32px;
    color: #2c3e50;
    margin-bottom: 12px;
    text-align: center;
    font-weight: 600;
}

.forget-subtitle {
    font-size: 16px;
    color: #7f8c8d;
    margin-bottom: 40px;
    text-align: center;
}

.forget-form {
    margin-top: 20px;
}

.form-content {
    width: 85%;
    margin: 0 auto;
}

.validate-container {
    display: flex;
    gap: 16px;
}

.validate-btn {
    flex-shrink: 0;
    width: 120px;
    height: 42px;
    font-size: 14px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 15px;
}

.submit-button {
    width: 100%;
    height: 42px;
    font-size: 16px;
    font-weight: 500;
    letter-spacing: 1px;
    margin-top: 20px;
    border-radius: 8px;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
}

.submit-button:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.form-footer {
    display: flex;
    justify-content: center;
    margin-top: 24px;
    font-size: 14px;
}

.form-footer .el-link {
    display: flex;
    align-items: center;
    gap: 4px;
}

/* Element UI 组件样式覆盖 */
:deep(.el-input__inner) {
    height: 42px;
    line-height: 42px;
    border-radius: 8px;
    border: 1px solid #dcdfe6;
    transition: all 0.3s ease;
}

:deep(.el-input__inner:focus) {
    border-color: #67C23A;
    box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2);
}

:deep(.el-form-item) {
    margin-bottom: 24px;
}

:deep(.el-input__prefix) {
    left: 12px;
    top: 0;
    height: 100%;
    display: flex;
    align-items: center;
}

:deep(.el-input__prefix i) {
    font-size: 16px;
    line-height: 1;
    display: flex;
    align-items: center;
}

:deep(.el-input__inner) {
    padding-left: 38px;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* 响应式调整 */
@media (max-width: 480px) {
    .forget-container {
        padding: 0 20px;
    }
    
    .form-content {
        width: 100%;
    }
    
    .validate-container {
        gap: 8px;
    }
    
    .validate-btn {
        width: 110px;
        font-size: 13px;
    }
}
</style>