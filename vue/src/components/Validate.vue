<template>
    <div
    class="validate-code disabled-select" 
        :style="`width:${width}; height:${height}`"
        @click="refreshCode"
    >
    <canvas ref="canvas" :width="canvasWidth" :height="canvasHeight"></canvas>
    </div>
  </template>
  
  <script>
  export default {
    name: 'ValidCode',
    model: {
      prop: 'value',
      event: 'input'
    },
    props: {
      width: {
        type: String,
      default: '120px'
      },
      height: {
        type: String,
      default: '42px'
      },
      length: {
        type: Number,
        default: 4
      },
      refresh: {
        type: Number
      }
    },
  data() {
      return {
      canvasWidth: 120,
      canvasHeight: 42,
      chars: 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz23456789',
      code: ''
      }
    },
    watch: {
    refresh() {
      this.drawCode()
    },
    width: {
      handler(val) {
        this.canvasWidth = parseInt(val)
        this.$nextTick(() => {
          this.drawCode()
        })
      },
      immediate: true
    },
    height: {
      handler(val) {
        this.canvasHeight = parseInt(val)
        this.$nextTick(() => {
          this.drawCode()
        })
      },
      immediate: true
    }
  },
  mounted() {
    this.drawCode()
  },
  methods: {
    // 生成随机颜色
    randomColor(min, max) {
      const r = this.randomNum(min, max)
      const g = this.randomNum(min, max)
      const b = this.randomNum(min, max)
      return `rgb(${r},${g},${b})`
    },
    // 生成随机数
    randomNum(min, max) {
      return Math.floor(Math.random() * (max - min) + min)
    },
    // 刷新验证码
    refreshCode() {
      this.drawCode()
    },
    // 绘制验证码
    drawCode() {
      const canvas = this.$refs.canvas
      const ctx = canvas.getContext('2d')
      ctx.textBaseline = 'middle'
      ctx.fillStyle = '#f5f7fa'
      ctx.fillRect(0, 0, this.canvasWidth, this.canvasHeight)

      // 生成验证码
      let code = ''
      for (let i = 0; i < this.length; i++) {
        const char = this.chars.charAt(Math.floor(Math.random() * this.chars.length))
        code += char
        
        // 随机字体大小
        const fontSize = this.randomNum(16, 20)
        ctx.font = `${fontSize}px Arial`
        
        // 随机旋转角度
        const rotate = this.randomNum(-30, 30) * Math.PI / 180
        
        // 计算位置
        const x = this.canvasWidth / (this.length + 1) * (i + 1)
        const y = this.canvasHeight / 2
        
        // 随机颜色
        ctx.fillStyle = this.randomColor(50, 160)
        
        // 绘制文字
        ctx.save()
        ctx.translate(x, y)
        ctx.rotate(rotate)
        ctx.fillText(char, -fontSize/2, 0)
        ctx.restore()
      }

      // 绘制干扰线
      for (let i = 0; i < 3; i++) {
        ctx.beginPath()
        ctx.moveTo(this.randomNum(0, this.canvasWidth), this.randomNum(0, this.canvasHeight))
        ctx.lineTo(this.randomNum(0, this.canvasWidth), this.randomNum(0, this.canvasHeight))
        ctx.strokeStyle = this.randomColor(150, 230)
        ctx.lineWidth = 1
        ctx.stroke()
      }

      // 绘制干扰点
      for (let i = 0; i < 30; i++) {
        ctx.beginPath()
        ctx.arc(this.randomNum(0, this.canvasWidth), this.randomNum(0, this.canvasHeight), 1, 0, 2 * Math.PI)
        ctx.fillStyle = this.randomColor(150, 230)
        ctx.fill()
      }

      this.code = code
      this.$emit('input', code)
      }
    }
  }
  </script>
  
  <style scoped>
.validate-code {
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  transition: all 0.3s ease;
}

.validate-code:hover {
  opacity: 0.8;
}

.disabled-select {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

canvas {
  display: block;
  width: 100%;
  height: 100%;
  }
  </style>
  