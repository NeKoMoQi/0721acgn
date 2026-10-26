import request from '@/utils/request'
enum API { 
    UPDATE_PIC_URL = '/updatepicbyoss'
}
export const updatePic = (file:File) => {
    const data = new FormData()
    data.append("file", file)
    return request.post<any, any>(API.UPDATE_PIC_URL, data,{
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })

}