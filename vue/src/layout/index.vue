<template>
  <div class="app-wrapper">
    <div class="side-container">
      <div class="logo-container">
        <i class="el-icon-shopping-cart-2 logo-icon"></i>
        <div class="logo-text-container">
          <h1 class="logo-text">农产品销售系统</h1>
        </div>
      </div>
      <SideMenu ref="sideMenu" />
    </div>
    <div class="main-container">
      <div class="main-header">
        <HeaderBar />
      </div>
      <div class="main-content">
        <el-scrollbar wrap-class="scrollbar" :native="false" :noresize="false">
          <router-view @update:user="updateUser" />
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<script>
import HeaderBar from '../components/Header.vue'
import SideMenu from '../components/Aside.vue'

export default {
  name: 'Layout',
  data() {
    return {
      userInfo: JSON.parse(localStorage.getItem("backUser") || {}),
    };
  },
  created() {
// 判断是否登录
    if(!this.userInfo){
      this.$message.warning('请先登录')
      this.$router.push('/login')
    }

  },
  provide() {
    return {
      userInfo: this.userInfo,
    
    };
  },
  components: { HeaderBar, SideMenu },
  computed: {

  },
  methods: {
    updateUser(user) {
      this.userInfo = user;
      this.$refs.sideMenu.refreshMenu();
    }

  }
}
</script>

<style lang="less">
@import "../assets/less/scroller-bar";

.app-wrapper {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #f5f7fa;
  display: flex; /* 使用flex布局 */

  .side-container {
    box-shadow: 2px 0 8px rgba(0,0,0,0.08);
    background-color: #fff;
    width: 260px; /* 增加侧边栏宽度，从220px到260px */
    height: 100vh;
    overflow: hidden;
    border-right: 1px solid #f0f0f0;
    position: relative;
    z-index: 2;
    flex-shrink: 0; /* 防止侧边栏被压缩 */
    display: flex;
    flex-direction: column;

    .logo-container {
      height: 96px;
      background: linear-gradient(to right, #3AB777, #4FC08D);
      display: flex;
      align-items: center;
      padding: 0 16px;
      position: relative;
      overflow: hidden;
      box-sizing: border-box;
      width: 100%;
      
      &:before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 1px;
        background: linear-gradient(to right, 
          rgba(255,255,255,0.1),
          rgba(255,255,255,0.3),
          rgba(255,255,255,0.1)
        );
      }
      
      .logo-icon {
        font-size: 32px;
        color: #fff;
        position: relative;
        z-index: 1;
        margin-right: 12px;
        flex-shrink: 0; /* 防止图标被压缩 */
      }
      
      .logo-text-container {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        width: calc(100% - 44px); /* 确保文本容器占满剩余空间，减去图标宽度和间距 */
        overflow: hidden; /* 防止文本溢出 */
      }
      
      .logo-text {
        margin: 0;
        font-size: 22px;
        color: #fff;
        font-weight: 600;
        letter-spacing: 1px;
        white-space: nowrap;
        position: relative;
        z-index: 1;
        text-shadow: 0 2px 4px rgba(0,0,0,0.1);
        line-height: 1.2;
        width: 100%; /* 确保文本宽度占满容器 */
        overflow: hidden; /* 防止文本溢出 */
        text-overflow: ellipsis; /* 文本溢出时显示省略号 */
        display: block; /* 确保作为块级元素 */
      }
      
      .logo-subtitle {
        font-size: 14px;
        color: rgba(255, 255, 255, 0.85);
        font-weight: 400;
        letter-spacing: 0.5px;
        white-space: nowrap;
        margin-top: 4px;
        width: 100%; /* 确保文本宽度占满容器 */
        overflow: hidden; /* 防止文本溢出 */
        text-overflow: ellipsis; /* 文本溢出时显示省略号 */
        display: block; /* 确保作为块级元素 */
      }
    }
  }

  .main-container {
    flex: 1; /* 占用剩余空间 */
    min-height: 100vh;
    background-color: #f5f7fa;
    padding: 16px;
    position: relative;
    overflow: auto; /* 修改为auto，允许滚动 */
    display: flex;
    flex-direction: column;
    box-sizing: border-box; /* 确保内边距不会增加元素宽度 */
    max-width: calc(100% - 260px); /* 调整为与侧边栏宽度对应 */

    .main-header {
      background: #fff;
      border-radius: 8px;
      margin-bottom: 16px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.04);
      padding: 16px;
      width: 100%; /* 确保宽度占满容器 */
      box-sizing: border-box;
      min-height: 96px; /* 与侧边栏logo容器高度一致 */
      display: flex;
      align-items: center;
      
      .el-header {
        padding: 0;
        height: auto;
        width: 100%;
      }
    }

    .main-content {
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.04);
      flex: 1; /* 占用剩余空间 */
      display: flex;
      flex-direction: column;
      width: 100%; /* 确保宽度占满容器 */
      box-sizing: border-box;
      overflow: visible; /* 允许内容溢出 */
      position: relative; /* 添加相对定位 */
      
      .el-scrollbar {
        height: auto; /* 改为自动高度 */
        min-height: calc(100vh - 160px); /* 调整最小高度 */
        flex: 1; /* 占用剩余空间 */
        position: relative; /* 添加相对定位 */
        
        .scrollbar {
          padding: 20px;
        }
      }
    }
  }
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .logo-subtitle {
    display: none; /* 在小屏幕上隐藏副标题 */
  }
}
</style>