import { ElNotification } from 'element-plus'
export const contrastpic = async (url1:string, url2:string) => {
    try {
        // 发送请求到后端
        const response = await fetch('http://localhost:5005/compare_images', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                image1_url: url1,
                image2_url: url2
            })
        });
        
        if (!response.ok) {
            throw new Error(`请求失败: ${response.status}`);
        }
        
        const data = await response.json();
        ElNotification({
            title: 'Success',
            message: '成功',
            type: 'success',
          })
        // 处理响应数据
            return data;
        
    } catch (error) {
        ElNotification({
            title: 'Error',
            message: '错误',
            type: 'error',
          })
          return;
}
}