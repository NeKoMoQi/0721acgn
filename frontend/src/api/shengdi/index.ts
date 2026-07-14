import request from '@/utils/request'
enum API {
   ShengDi_Hot_URL = '/shengdi/hot',
}

export const getShengDiHot = () => request.get<any, any>(API.ShengDi_Hot_URL)