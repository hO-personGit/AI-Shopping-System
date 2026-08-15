<template>
  <div class="category-container">
    <div class="category-wrapper">
      <div 
        v-for="(category, index) in visibleCategories" 
        :key="index" 
        class="category-item"
        :style="{animationDelay: index * 0.1 + 's'}"
        @click="handleCategoryClick(category)"
      >
        <div class="icon-wrapper">
          <i :class="['icon', category.icon]"></i>
          <div class="icon-bg"></div>
        </div>
        <span class="category-name">{{ category.name }}</span>
        <div class="hover-indicator"></div>
      </div>
      <div 
        v-if="showMore" 
        class="category-item more-item"
        :style="{animationDelay: visibleCategories.length * 0.1 + 's'}"
        @click="handleMoreClick"
      >
        <div class="icon-wrapper">
          <i class="el-icon-more"></i>
          <div class="icon-bg"></div>
        </div>
        <span class="category-name">更多分类</span>
        <div class="hover-indicator"></div>
      </div>
    </div>
  </div>
</template>

<script>
import Request from '@/utils/request.js'

export default {
  name: 'FrontCategory',
  data() {
    return {
      categories: [],
      maxVisible: 5,
      iconMap: {
        '新鲜水果': 'el-icon-apple',
        '时令蔬菜': 'el-icon-food',
        '粮油作物': 'el-icon-dish',
        '特色农产': 'el-icon-sugar',
        '有机农产': 'el-icon-dish-1',
        '水产': 'el-icon-cherry',
        '肉禽蛋': 'el-icon-chicken',
        '干货坚果': 'el-icon-dessert',
        '其他': 'el-icon-more'
      }
    }
  },
  computed: {
    visibleCategories() {
      return this.categories.slice(0, this.maxVisible)
    },
    showMore() {
      return this.categories.length > this.maxVisible
    }
  },
  created() {
    this.fetchCategories()
  },
  methods: {
    async fetchCategories() {
      try {
        const res = await Request.get('/category/all')
        if (res.code === '0') {
          this.categories = res.data.map(item => ({
            ...item,
            icon: this.getIconByName(item.name)
          }))
        }
      } catch (error) {
        console.error('获取分类数据失败:', error)
      }
    },
    getIconByName(name) {
      return this.iconMap[name] || 'el-icon-more'
    },
    handleCategoryClick(category) {
      this.$router.push({
        name: 'category',
        params: { id: category.id },
        query: { name: category.name }
      })
    },
    handleMoreClick() {
      this.$router.push('/products')
    }
  }
}
</script>

<style scoped>
.category-container {
  background: linear-gradient(to right, #ffffff, #f8faf5);
  padding: 24px 0;
  margin: 24px 0;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.category-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(circle at 0% 0%, rgba(103, 194, 58, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 100% 0%, rgba(103, 194, 58, 0.05) 0%, transparent 50%);
  opacity: 0.9;
  z-index: 0;
}

.category-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.category-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 10px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  position: relative;
  overflow: hidden;
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
  transform: translateY(20px);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.category-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(103, 194, 58, 0.1) 0%, transparent 70%);
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.5s cubic-bezier(0.25, 1, 0.5, 1);
}

.category-item:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(103, 194, 58, 0.1);
}

.category-item:hover::before {
  opacity: 1;
  transform: scale(1.5);
}

.icon-wrapper {
  width: 64px;
  height: 64px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  z-index: 2;
}

.icon {
  font-size: 28px;
  color: #67C23A;
  position: relative;
  z-index: 2;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

.category-item:hover .icon {
  animation: none;
  transform: scale(1.15);
  color: #5ab82f;
}

.icon-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, 
    rgba(103, 194, 58, 0.12),
    rgba(103, 194, 58, 0.18)
  );
  border-radius: 50%;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
}

.category-item:hover .icon-bg {
  transform: scale(1.15);
  background: linear-gradient(135deg, 
    rgba(103, 194, 58, 0.18),
    rgba(103, 194, 58, 0.25)
  );
  box-shadow: 0 6px 15px rgba(103, 194, 58, 0.15);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(103, 194, 58, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}

.category-name {
  font-size: 15px;
  color: #2c3e50;
  font-weight: 600;
  transition: all 0.3s ease;
  position: relative;
  margin-top: 4px;
  z-index: 2;
}

.category-item:hover .category-name {
  color: #5ab82f;
  transform: scale(1.05);
}

.hover-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 3px;
  background: linear-gradient(to right, #67C23A, #85ce61);
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  transform: translateX(-50%);
  border-radius: 3px;
  z-index: 2;
}

.category-item:hover .hover-indicator {
  width: 30px;
}

.more-item .icon-bg {
  background: linear-gradient(135deg, 
    rgba(144, 147, 153, 0.12),
    rgba(144, 147, 153, 0.18)
  );
}

.more-item .icon {
  color: #909399;
}

.more-item:hover .icon-bg {
  background: linear-gradient(135deg, 
    rgba(144, 147, 153, 0.18),
    rgba(144, 147, 153, 0.25)
  );
  box-shadow: 0 6px 15px rgba(144, 147, 153, 0.15);
}

.more-item:hover::before {
  background: radial-gradient(circle at center, rgba(144, 147, 153, 0.1) 0%, transparent 70%);
}

.more-item:hover .icon {
  color: #606266;
}

.more-item:hover .category-name {
  color: #606266;
}

.more-item:hover .hover-indicator {
  background: linear-gradient(to right, #909399, #c0c4cc);
}

@media (max-width: 768px) {
  .category-wrapper {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .category-item {
    flex: 0 0 calc(25% - 9px);
    padding: 12px 8px;
  }
  
  .icon-wrapper {
    width: 50px;
    height: 50px;
    margin-bottom: 8px;
  }
  
  .icon {
    font-size: 24px;
  }
  
  .category-name {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .category-container {
    padding: 16px 0;
  }
  
  .category-item {
    flex: 0 0 calc(33.33% - 8px);
    padding: 10px 6px;
  }
  
  .icon-wrapper {
    width: 44px;
    height: 44px;
  }
  
  .icon {
    font-size: 20px;
  }
  
  .category-name {
    font-size: 13px;
  }
}
</style> 