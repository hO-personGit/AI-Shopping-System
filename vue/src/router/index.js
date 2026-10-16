import Vue from 'vue'
import VueRouter, { RouteConfig } from 'vue-router'
import home from '../layout/index.vue';


import ShowView from '@/views/showView.vue';
import Notices from '../views/NoticeList.vue';

import Login from '@/views/Login.vue';
import LRLayout from '@/layout/LRLayout.vue';
import RegisterVue from '@/views/RegisterVue.vue';
import Forget from '@/views/Forget.vue';
import Menu from '@/views/Menu.vue';
import Home from '@/views/front/Home.vue';

Vue.use(VueRouter)

const routes = [

  {
    path: "/login",
    name: "LRLayout",
    redirect: '/login',  
    component:LRLayout,
    children: [
      {
        path: "/login",
        name: "Login",
        component: Login,
      },
      {
        path: "/register",
        name: "Register",
        component: RegisterVue
      },

      {
        path:'/forget',
        name: 'Forget',
        component: Forget
      },
 
    ]
  },
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../views/404.vue')
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('@/views/front/Products.vue')
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/front/Cart.vue')
  },
  {
    path: '/favorite',
    name: 'Favorite',
    component: () => import('@/views/front/Favorite.vue'),
    meta: {
      requiresAuth: true
    }
  },
  {
    path: '/category/:id',
    name: 'category',
    component: () => import('@/views/front/Category.vue')
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('@/views/front/Order.vue')
  },
  {
    path: '/user-center',
    name: 'UserCenter',
    component: () => import('@/views/front/UserCenter.vue'),
    meta: {
      requiresAuth: true
    }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/front/ProductDetail.vue')
  },
  {
    path: '/articles',
    name: 'Articles',
    component: () => import('@/views/front/Article.vue')
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/front/ArticleDetail.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/front/Search.vue'),
    meta: {
      title: '搜索结果'
    }
  }
  
  
];



const router = new VueRouter({
  mode: "history",
  base: process.env.BASE_URL,
  routes,
});
export const setRoutes = () => {
  const userMenuListStr = localStorage.getItem("userMenuList")
  if (userMenuListStr && userMenuListStr !== 'undefined') {
    const currentRoutes = router.getRoutes().map(v => v.name)
    if (!currentRoutes.includes('Layout')) {
      const currRouter = {
        path: '/',
        name: 'home',
        component: home,
        redirect: '/showView',
        children: [
          {
            path: "/showView",
            name: "homeView",
            component: ShowView,
            meta: {
              title: "首页"
            },
          },
          {
            path: '/menu',
            name: 'Menu',
            component: Menu,
            meta: {
              title: "菜单管理"
            },
          },
        ]
      }

      try {
      const menus = JSON.parse(userMenuListStr);
      menus.forEach((item) => {
        if (item.path) {
          const itemMenu = {
            path: item.path.replace("/", ""),
            name: item.name,
            component: () => import(`../views/${item.pagePath}.vue`),
              meta: {
                title: item.name
            }
          };
          currRouter.children.push(itemMenu);
        } else if (item.children?.length) {
          item.children.forEach((item) => {
            if (item.path) {
              const itemMenu = {
                path: item.path.replace("/", ""),
                name: item.name,
                component: () => import(`../views/${item.pagePath}.vue`),
                  meta: {
                    title: item.name
                }
              };
              currRouter.children.push(itemMenu);
            }
          });
        }
      });
      router.addRoute(currRouter)
      } catch (err) {
        console.error('Failed to parse userMenuList:', err);
    }
  }
  }
}
setRoutes()
router.onError((error) => {
  if (error.message.includes('Cannot find module')) {
    // 如果错误是因为找不到模块，跳转到404页面
    router.replace('/404');
  }
});
router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresAuth)) {
    const userInfo = localStorage.getItem('frontUser')
    if (!userInfo) {
      ElementUI.Message({
        message: '请先登录',
        type: 'warning'
      })
      next('/login')
      return
    }
  }

  if (to.name) {
    localStorage.setItem('currentPathName', to.name);
  }

  const localMenus = localStorage.getItem("userMenuList");
  if (!to.matched.length) {
    if (to.path !== '/404') {
      if (localMenus) {
        // 记录来源路径
        next({
          path: '/404',
          replace: true,
          query: { redirect: from.fullPath }
        })
      } else {
        ElementUI.Message({
          message: '请先登录',
          type: 'warning'
        });
        next('/login')
      }
    } else {
      next()
    }
  } else {
    next()
  }
})
const originalPush = VueRouter.prototype.push
   VueRouter.prototype.push = function push(location) {
   return originalPush.call(this, location).catch(err => err)
}

export default router;
