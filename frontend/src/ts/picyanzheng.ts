import { ElNotification } from 'element-plus'
export const processFile = (file:File) => {
    if (!file) return;
    // 验证文件类型和大小
    const validTypes = ['image/jpeg', 'image/png', 'image/gif'];
    const maxSize = 5 * 1024 * 1024; // 5MB
    if (!validTypes.includes(file.type)) {
        ElNotification({
            title: '错误',
            message: '错误：只支持 JPG, PNG, GIF 格式的图片',
            type: 'error',
          })
      return;
    }
    if (file.size > maxSize) {
        ElNotification({
            title: '错误',
            message: '错误：图片大小不能超过 5MB',
            type: 'error',
          })  
      return;
    }
    ElNotification({
        title: 'bingo',
        message: '上传成功',
        type: 'success',
      })
      
    return file;
  };