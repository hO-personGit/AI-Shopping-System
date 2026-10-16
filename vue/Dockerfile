# ===== Vue 前端构建 + Nginx 部署 =====
# Stage 1: Node 构建
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
# 生产构建（跳过 lint）
RUN npm run build -- --no-lint

# Stage 2: Nginx 运行
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# 反向代理 /api → SpringBoot 后端（docker-compose 中服务名为 backend）
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
