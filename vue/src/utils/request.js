import axios from 'axios'
import router from '../router'
import { Message } from 'element-ui'

const request = axios.create({
  baseURL: '/api',
  timeout: 25000
})

const errorFlags = {
  401: false,
  403: false,
  500: false,
  408: false,
  network: false
}

let messageShown = false

const resetErrorFlags = (specificError = null) => {
  if (specificError) {
    errorFlags[specificError] = false
  } else {
    Object.keys(errorFlags).forEach(key => {
      errorFlags[key] = false
    })
  }
  messageShown = false
}

const setErrorFlag = (status) => {
  if (Object.prototype.hasOwnProperty.call(errorFlags, status)) {
    errorFlags[status] = true
    setTimeout(() => {
      resetErrorFlags(status)
    }, 5000)
  }
}

window.onerror = function(message, url, lineNumber) {
  console.error('Uncaught error:', message, url, lineNumber)
}

request.interceptors.request.use(
  config => {
    const user = localStorage.getItem('backUser')
    if (user) {
      try {
        config.headers.token = JSON.parse(user).token
      } catch (e) {
        console.error('Token parsing error:', e)
      }
    }
    return config
  },
  error => {
    console.error('request error:', error)
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response
    }

    let res = response.data
    if (typeof res === 'string') {
      try {
        res = JSON.parse(res)
      } catch (e) {
        console.error('Response parsing error:', e)
      }
    }
    return res
  },
  error => {
    if (messageShown) {
      return Promise.reject(error)
    }

    let status = null

    if (error.response) {
      status = error.response.status
      if (Object.prototype.hasOwnProperty.call(errorFlags, status) && !errorFlags[status]) {
        setErrorFlag(status)
        messageShown = true

        if (status === 401) {
          try {
            localStorage.removeItem('backUser')
            localStorage.removeItem('userMenuList')
            localStorage.removeItem('frontUser')
            router.push('/login')
          } catch (e) {
            console.error('Error during logout:', e)
          }
        }
        handleErrorResponse(status)
      }
    } else if (!messageShown) {
      setErrorFlag('network')
      messageShown = true
      handleErrorResponse('network')
    }

    return Promise.reject(error)
  }
)

function handleErrorResponse(status) {
  let message = ''
  switch (status) {
    case 500:
      message = '服务器内部错误，请稍后再试'
      break
    case 403:
      message = '没有权限访问该资源'
      break
    case 408:
      message = '请求超时，请检查网络连接'
      break
    case 401:
      message = '登录失效，请重新登录'
      break
    case 'network':
      message = '网络连接错误，请检查后端服务是否启动'
      break
    default:
      message = '请求发生错误，请稍后再试'
      break
  }

  if (message) {
    try {
      Message.error({
        message,
        duration: 3000,
        showClose: true,
        onClose: () => {
          messageShown = false
        }
      })
    } catch (e) {
      console.error('Error showing message:', e)
      messageShown = false
    }
  }
}

request.resetAuth = () => {
  resetErrorFlags()
  messageShown = false
}

export default request
