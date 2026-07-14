import request from '@/utils/request'
enum API {
    ACGNUSER_URL = '/acgn/user',
    ACGNUSER_BIRTH_URL  ='/acgn/getbirth'
}
export const getAcgnUserRandom = () => request.get<any, any>(API.ACGNUSER_URL)
export const getAcgnUserBirth = () => request.get<any, any>(API.ACGNUSER_BIRTH_URL)


