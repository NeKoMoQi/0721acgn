import request from '@/utils/request'
enum API {
    Activity_Get_Url = '/activity',
    AllActivity_Get_Url = '/activity/all',
    Activity_Hot_Url= '/activity/hot',
    Activity_Leibie_Url = '/activity/leibie'
}
export const getpageActivity = (data: any) => request.get<any, any>(API.Activity_Get_Url+`?page=${data}`)
export const getAllActivity = () => request.get<any, any>(API.AllActivity_Get_Url)
export const getActivityHot = () => request.get<any, any>(API.Activity_Hot_Url)
export const getActivityLeibie = () => request.get<any, any>(API.Activity_Leibie_Url)

