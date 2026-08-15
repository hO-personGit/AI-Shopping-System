<template>
  <el-dropdown class="user-avatar-wrapper" @command="handleCommand">
    <div class="avatar-box">
 
      <span class="username">{{userInfo.username}}</span>
      <i class="el-icon-caret-bottom" />

    </div>

    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item command="userCenter">个人中心</el-dropdown-item>
      <el-dropdown-item command="loginOut">退出登录</el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>

export default {
  inject: ['userInfo'],
  created() {

            console.log(this.userInfo)
    },
  name: 'UserAvatar',
  data() {
    return {
      // userInfo: {},
      imgBaseUrl: this.HOST,
    }
  },
  methods: {
    handleCommand(command) {
      if (command === 'userCenter') {
          this.$router.push({ path: '/personInfo' })
      }
      if (command === 'loginOut') {
          this.loginOut()
      }
    },
    loginOut() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem("backUser")
  
        this.$router.push({ path: '/login'});
        //   removeToken()
        //   location.reload()
      })

    }
  }
}
</script>

<style lang="less">
.user-avatar-wrapper {
  float: left;
  // width: 120px;
  padding: 3px 0 3px 20px;
  margin-left: 20px;
  border-left: solid 1px #ddd;
  cursor: pointer;

  .avatar-box {
    outline: none;
    display: flex;
  align-items: center; /* 垂直居中对齐 */
  justify-content: space-between; /* 头像和下拉箭头之间留有空间 */
  }

  .username {
    font-size: 14px;
    /* 根据需要调整字体大小 */
    color: #333;
    /* 字体颜色 */
    margin-left: 5px;
    /* 与头像之间的间距 */
  }

  .el-avatar--small {
    display: inline-block;
    vertical-align: middle;
    width: 32px;
    height: 32px;
    line-height: 32px;
  }

  i {
    display: inline-block;
    vertical-align: middle;
    margin-left: 2px;
  }
}
</style>