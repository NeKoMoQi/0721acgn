import request from '@/utils/request'

enum API {
    GET_GRID_URL = "/warflag/getgrid",
}

export const getWarGrid = () => request.get<any, any>(API.GET_GRID_URL);