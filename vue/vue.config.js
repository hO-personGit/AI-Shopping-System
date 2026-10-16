const { defineConfig } = require("@vue/cli-service");

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    client: {
      overlay: false
    },
    port: 8080,
    proxy: {
      "/api": {
        target: "http://localhost:1234",
        changeOrigin: true
      }
    }
  }
});
