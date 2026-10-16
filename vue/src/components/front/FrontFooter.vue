<template>
  <footer class="front-footer">
    <div class="footer-content">
      <!-- 主要内容区域 -->
      <div class="footer-main">
        <div class="footer-section about-section">
          <div class="footer-logo">
            <i class="el-icon-shopping-cart-2"></i>
            <span class="footer-logo-text">农产品销售系统</span>
          </div>
          <div class="footer-info">
            <p class="footer-slogan">
              撷取自然馈赠 • 臻选品质生活
            </p>
            <div class="footer-divider">•</div>
            <div class="footer-contact">
              <div class="contact-item">
                <i class="el-icon-phone"></i>
                <span>客服专线 400-123-4567</span>
              </div>
              <div class="contact-divider">|</div>
              <div class="contact-item">
                <i class="el-icon-message"></i>
                <span>服务邮箱 service@greenfarm.com</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 版权信息 -->
      <div class="footer-bottom">
        <div class="copyright">
          <p>© 2024 农产品销售系统 版权所有 | 浙ICP备xxxxxxxx号-1</p>
        </div>
      </div>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'FrontFooter'
}
</script>

<style scoped>
.front-footer {
  background: linear-gradient(to bottom, #fff, #f8faf5);
  padding: 24px 0 16px;
  margin-top: 40px;
  position: relative;
  overflow: hidden;
}

.front-footer::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(circle at 0% 0%, rgba(103, 194, 58, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 100% 0%, rgba(103, 194, 58, 0.03) 0%, transparent 50%);
  opacity: 0.8;
  z-index: 0;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  position: relative;
  z-index: 1;
}

.footer-main {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

/* Logo 和关于我们部分 */
.about-section {
  max-width: 600px;
  text-align: center;
}

.footer-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.footer-logo i {
  font-size: 28px;
  color: #67C23A;
  margin-right: 8px;
}

.footer-logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #67C23A;
  letter-spacing: 1px;
}

.footer-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
  white-space: nowrap;
}

.footer-slogan {
  color: #606266;
  line-height: 1.5;
  font-size: 14px;
  letter-spacing: 0.5px;
  font-weight: 500;
  margin: 0;
  white-space: nowrap;
}

.footer-divider,
.contact-divider {
  color: #dcdfe6;
  font-weight: 300;
}

.footer-contact {
  color: #606266;
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  opacity: 0.9;
  white-space: nowrap;
}

.contact-item {
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.contact-item:hover {
  color: #67C23A;
}

.contact-item i {
  color: #67C23A;
  margin-right: 6px;
  font-size: 14px;
}

/* 版权信息部分 */
.footer-bottom {
  text-align: center;
  color: #909399;
  font-size: 12px;
  opacity: 0.8;
  padding-top: 16px;
  border-top: 1px solid rgba(103, 194, 58, 0.1);
}

.copyright p {
  margin: 4px 0;
  letter-spacing: 0.3px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .front-footer {
    padding: 20px 0 12px;
    margin-top: 30px;
  }

  .footer-info {
    flex-direction: row;
    gap: 8px;
    overflow-x: auto;
    width: 100%;
    justify-content: flex-start;
    padding-bottom: 8px;
  }

  .footer-divider {
    display: inline;
  }

  .footer-contact {
    gap: 8px;
  }

  /* 隐藏滚动条 */
  .footer-info::-webkit-scrollbar {
    display: none;
  }
}
</style> 