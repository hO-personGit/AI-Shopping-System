<template>
  <div class="header-bar">
    <div class="header-left">
      <BreadCrumbs />
    </div>
    <div class="header-right">
      <div class="current-time">
        <i class="el-icon-time"></i>
        <span>{{ currentTime }}</span>
      </div>
      <div class="header-actions">
        <el-tooltip content="返回前台" placement="bottom">
          <div class="action-btn" @click="goToFront">
            <i class="el-icon-s-home"></i>
            <span>商城首页</span>
          </div>
        </el-tooltip>
        
        <div class="action-group">
          <el-tooltip content="刷新页面" placement="bottom">
            <div class="action-icon-btn" @click="refreshPage">
              <i class="el-icon-refresh-right"></i>
            </div>
          </el-tooltip>
          
          <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏显示'" placement="bottom">
            <div class="action-icon-btn" @click="toggleFullScreen">
              <i :class="isFullscreen ? 'el-icon-close' : 'el-icon-full-screen'"></i>
            </div>
          </el-tooltip>
        </div>

        <div class="divider"></div>
        <UserAvatar />
      </div>
    </div>
  </div>
</template>

<script>
import BreadCrumbs from '../components/BreadCrumbs/index.vue'
import UserAvatar from '../components/UserAvatar/index.vue'

export default {
  name: 'HeaderBar',
  components: {
    BreadCrumbs,
    UserAvatar
  },
  data() {
    return {
      isFullscreen: false,
      currentTime: '',
      timer: null
    }
  },
  methods: {
    refreshPage() {
      window.location.reload()
    },
    toggleFullScreen() {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen()
        this.isFullscreen = true
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen()
          this.isFullscreen = false
        }
      }
    },
    goToFront() {
      this.$router.push('/');
    },
    updateTime() {
      const now = new Date();
      const options = { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric', 
        weekday: 'long',
        hour: '2-digit', 
        minute: '2-digit', 
        second: '2-digit' 
      };
      this.currentTime = now.toLocaleDateString('zh-CN', options).replace(/\//g, '-');
    }
  },
  mounted() {
    document.addEventListener('fullscreenchange', () => {
      this.isFullscreen = !!document.fullscreenElement
    })
    this.updateTime();
    this.timer = setInterval(this.updateTime, 1000);
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }
}
</script>

<style lang="less" scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0;
  background: #ffffff;
  border-bottom: none;
  width: 100%;
  box-sizing: border-box;
  min-height: 64px; /* 确保最小高度 */
  
  .header-left {
    display: flex;
    align-items: center;
    height: 100%;
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    height: 100%;
    
    .current-time {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #606266;
      font-size: 14px;
      background: #f5f7fa;
      padding: 6px 12px;
      border-radius: 20px;
      white-space: nowrap;
      
      i {
        color: #67C23A;
        font-size: 16px;
      }
    }
    
    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .action-btn {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 8px 16px;
        border-radius: 20px;
        cursor: pointer;
        transition: all 0.3s ease;
        background: #67C23A;
        color: white;
        font-size: 14px;
        box-shadow: 0 2px 6px rgba(103, 194, 58, 0.2);
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
        }
        
        i {
          font-size: 16px;
        }
      }

      .action-group {
        display: flex;
        align-items: center;
        gap: 8px;
        background: #f5f7fa;
        padding: 4px;
        border-radius: 16px;
      }
      
      .action-icon-btn {
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        cursor: pointer;
        transition: all 0.3s ease;
        color: #606266;
        
        i {
          font-size: 18px;
          transition: all 0.3s ease;
        }
        
        &:hover {
          background-color: white;
          color: #67C23A;
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
          
          i.el-icon-refresh-right {
            transform: rotate(180deg);
          }
        }
      }
      
      .divider {
        width: 1px;
        height: 24px;
        background: #e4e7ed;
        margin: 0 8px;
      }
    }
  }
}

:deep(.el-badge__content.is-fixed) {
  top: 8px;
  right: 8px;
  border: 2px solid white;
}

:deep(.el-badge__content) {
  background-color: #67C23A;
  box-shadow: 0 2px 4px rgba(103, 194, 58, 0.2);
}

:deep(.el-tooltip__popper) {
  font-size: 13px;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .current-time span {
    display: none;
  }
  
  .current-time {
    padding: 6px !important;
  }
}
</style>