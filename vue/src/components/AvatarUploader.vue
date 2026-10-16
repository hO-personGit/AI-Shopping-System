<template>
    <el-upload
      class="avatar-uploader"
      :action="uploadUrl"
      :show-file-list="false"
      :on-success="handleSuccess"
      :before-upload="beforeUpload"
    >
      <el-image v-if="avatar" :src="imgBaseUrl + '/' + avatar" class="avatar" />
      <i v-else class="el-icon-plus avatar-uploader-icon"></i>
    </el-upload>
  </template>
  
  <script>
  export default {
    name: 'AvatarUploader',
    props: {
      uploadUrl: {
        type: String,
        required: true
      },
      imgBaseUrl: {
        type: String,
        required: true
      },
      avatar: {
        type: String,
        default: ''
      }
    },
    data() {
      return {
        resData: ''
      }
    },
    methods: {
      handleSuccess(res,file) {
        // 把res传给父组件
        this.resData = res.data;
        this.$emit('upload-success', res.data);
      },
      beforeUpload(file) {
        const isImage = file.type.match('image/jpeg|image/png');
        const isLt2M = file.size / 1024 / 1024 < 2;
        if (!isImage) {
          this.$message.error('上传头像图片只能是 JPG 或 PNG 格式!');
        }
        if (!isLt2M) {
          this.$message.error('上传头像图片大小不能超过 2MB!');
        }
        return isImage && isLt2M;
      }
    }
  }
  </script>
  
  <style scoped>
  .avatar-uploader .el-upload {
    cursor: pointer;
    position: relative;
    overflow: hidden;
  }
  
  .avatar-uploader-icon {
    font-size: 28px;
    border: 1.5px dashed #d9d9d9;
    background-color: rgba(255, 255, 255, 0.479);
    border-radius: 6px;
    color: #006aff;
    width: 178px;
    height: 178px;
    line-height: 178px;
    transition: 0.5s;
    text-align: center;
    border-radius: 50%;
  }
  
  .avatar {
    width: 178px;
    height: 178px;
    display: block;
    border-radius: 50%;
  }
  </style>