import request from '@/utils/request'
enum API {
    PEOPLE_COUNT_URL = '/peoplecount'
}
export const getPeopleCount = () => request.get<any, any>(API.PEOPLE_COUNT_URL)

