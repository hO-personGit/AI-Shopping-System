<template>
    <div class="register-container">
        <h2 class="register-title" @click="goToHome">农产品销售系统</h2>

        <el-form ref="registerForm" :model="registerForm" :rules="rules" class="register-form">
            <div class="form-content">
                <el-form-item prop="username">
                    <div class="custom-input">
                        <i class="el-icon-user"></i>
                        <el-input v-model="registerForm.username" placeholder="请输入用户名"></el-input>
                    </div>
                </el-form-item>

                <el-form-item prop="password">
                    <div class="custom-input">
                        <i class="el-icon-lock"></i>
                        <el-input type="password" v-model="registerForm.password" placeholder="请设置密码"></el-input>
                    </div>
                </el-form-item>

                <el-form-item prop="name">
                    <div class="custom-input">
                        <i class="el-icon-user"></i>
                        <el-input v-model="registerForm.name" placeholder="请输入真实姓名"></el-input>
                    </div>
                </el-form-item>

                <el-form-item prop="email">
                    <div class="custom-input">
                        <i class="el-icon-message"></i>
                        <el-input v-model="registerForm.email" placeholder="请输入邮箱"></el-input>
                    </div>
                </el-form-item>

                <el-form-item prop="code">
                    <div class="validate-container">
                        <div class="custom-input">
                            <i class="el-icon-chat-line-round"></i>
                            <el-input v-model="registerForm.code" placeholder="请输入验证码"></el-input>
                        </div>
                        <el-button 
                            type="success" 
                            :disabled="disabled"
                            class="validate-btn"
                            @click="sendVerificationCode">
                            {{ buttonContent }}
                        </el-button>
                    </div>
                </el-form-item>

                <el-form-item prop="role">
                    <el-select 
                        v-model="registerForm.role" 
                        placeholder="请选择用户角色"
                        class="role-select">
                        <el-option label="普通用户" value="USER"></el-option>
                        <el-option label="商户" value="MERCHANT"></el-option>
                        <el-option label="管理员" value="ADMIN"></el-option>
                    </el-select>
                </el-form-item>

                <el-form-item prop="invitationCode" v-if="registerForm.role === 'ADMIN'">
                    <div class="custom-input">
                        <i class="el-icon-key"></i>
                        <el-input v-model="registerForm.invitationCode" placeholder="请输入管理员邀请码"></el-input>
                    </div>
                </el-form-item>

                <div class="button-group">
                    <el-button 
                        type="primary" 
                        class="register-button"
                        @click="onRegister">
                        <i class="el-icon-check"></i>
                        注册
                    </el-button>
                    <el-button 
                        type="success" 
                        class="login-button"
                        @click="$router.push('/login')">
                        <i class="el-icon-arrow-left"></i>
                        返回登录
                    </el-button>
                </div>
            </div>
        </el-form>
    </div>
</template>

<script>
import request from "@/utils/request";

export default {
    name: 'Register',
    data() {
        // 验证邀请码
        const validateInvitationCode = (rule, value, callback) => {
            if (this.registerForm.role === 'ADMIN') {
                if (!value) {
                    callback(new Error('请输入管理员邀请码'));
                } else if (value !== 'ADMIN666') { // 邀请码写死为ADMIN666
                    callback(new Error('邀请码不正确'));
                } else {
                    callback();
                }
            } else {
                callback();
            }
        };

        return {
            countdown: 0,
            disabled: false,
            timer: null,
            emailCode: '',
            buttonContent: '发送验证码',
            registerForm: {
                username: '',
                password: '',
                name: '',
                email: '',
                code: '',
                role: 'USER',
                status: 1,
                invitationCode: '' // 添加邀请码字段
            },
            rules: {
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' },
                    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' },
                    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
                ],
                name: [
                    { required: true, message: '请输入真实姓名', trigger: 'blur' }
                ],
                email: [
                    { required: true, message: '请输入邮箱', trigger: 'blur' },
                    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
                ],
                code: [
                    { required: true, message: '请输入验证码', trigger: 'blur' }
                ],
                role: [
                    { required: true, message: '请选择用户角色', trigger: 'change' }
                ],
                invitationCode: [
                    { validator: validateInvitationCode, trigger: 'blur' }
                ]
            }
        };
    },
    methods: {
        goToHome() {
            this.$router.push('/');
        },
        sendVerificationCode() {
            if (this.disabled) return;
            
            if (!this.registerForm.email) {
                this.$message.error('请先输入邮箱地址');
                return;
            }

            request.get(`/email/sendEmail/${this.registerForm.email}`).then(res => {
                if (res.code === '0') {
                    this.$message({
                        type: 'success',
                        message: "验证码已发送到您的邮箱,请查收"
                    });
                    console.log(res.data);
                    this.emailCode = res.data;
                    this.startCountdown();
                } else {
                    this.$message({
                        type: 'error',
                        message: res.msg
                    });
                }
            });
        },
        
        startCountdown() {
            this.countdown = 60;
            this.disabled = true;
            this.buttonContent = `${this.countdown}秒后重试`;

            this.timer = setInterval(() => {
                if (this.countdown > 0) {
                    this.countdown--;
                    this.buttonContent = `${this.countdown}秒后重试`;
                } else {
                    clearInterval(this.timer);
                    this.disabled = false;
                    this.buttonContent = '发送验证码';
                }
            }, 1000);
        },

        onRegister() {
            this.$refs.registerForm.validate((valid) => {
                if (valid) {
                    // 将验证码转换为字符串后再比较
                    if (String(this.registerForm.code) !== String(this.emailCode)) {
                        console.log('Input code:', this.registerForm.code, 'Expected code:', this.emailCode);
                        this.$message({
                            type: 'error',
                            message: '验证码不正确'
                        });
                        return;
                    }

                    // 如果是管理员注册，验证邀请码
                    if (this.registerForm.role === 'ADMIN' && 
                        this.registerForm.invitationCode !== 'ADMIN666') {
                        this.$message({
                            type: 'error',
                            message: '管理员邀请码不正确'
                        });
                        return;
                    }

                    request.post("/user/add", this.registerForm).then(res => {
                        if (res.code === '0') {
                            this.$message({
                                type: 'success',
                                message: "注册成功，请登录"
                            });
                            this.$router.push('/login');
                        } else {
                            this.$message({
                                type: 'error',
                                message: res.msg
                            });
                        }
                    });
                }
            });
        }
    },
    beforeDestroy() {
        clearInterval(this.timer);
    }
};
</script>

<style scoped>
.register-container {
    width: 100%;
    max-width: 380px;
    margin: 0 auto;
    padding-top: 5px;
}

.register-title {
    font-size: 32px;
    color: #2c3e50;
    margin-bottom: 20px;
    text-align: center;
    font-weight: 600;
    cursor: pointer;
    transition: color 0.3s;
}

.register-title:hover {
    color: #67C23A;
}

.register-form {
    margin-top: 0;
}

.form-content {
    width: 100%;
    margin: 0 auto;
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

.validate-container {
    display: flex;
    gap: 16px;
}

.validate-btn {
    flex-shrink: 0;
    width: 130px;
    height: 50px;
    font-size: 15px;
    border-radius: 8px;
    padding: 0 15px;
    background: linear-gradient(135deg, #67C23A, #85ce61);
    border-color: #67C23A;
}

.validate-btn:hover:not(:disabled) {
    background: linear-gradient(135deg, #85ce61, #67C23A);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.role-select {
    width: 100%;
}

.button-group {
    display: flex;
    gap: 15px;
    margin-top: 25px;
}

.register-button, .login-button {
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

.register-button {
    background: linear-gradient(135deg, #409EFF, #66b1ff);
    border-color: #409EFF;
}

.login-button {
    background: linear-gradient(135deg, #67C23A, #85ce61);
    border-color: #67C23A;
}

.register-button:hover, .login-button:hover {
    transform: none;
    box-shadow: none;
}

.register-button i, .login-button i {
    margin-right: 8px;
    font-size: 18px;
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
    margin-bottom: 15px;
}

:deep(.el-select .el-input__inner) {
    padding-left: 15px;
}

:deep(.el-select .el-input__suffix) {
    display: flex;
    align-items: center;
}

@media (max-width: 480px) {
    .register-container {
        padding: 0 20px;
    }
    
    .validate-container {
        gap: 12px;
    }
    
    .validate-btn {
        width: 120px;
        font-size: 14px;
        height: 48px;
    }

    .button-group {
        flex-direction: column;
        gap: 12px;
    }
    
    .register-button, .login-button {
        height: 48px;
        font-size: 17px;
    }
    
    :deep(.el-input__inner) {
        height: 48px;
        line-height: 48px;
        font-size: 15px;
    }
}
</style>