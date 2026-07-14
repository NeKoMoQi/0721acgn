import request from '@/utils/request'
enum API {
   XUNLI_URL = '/xunli',
   XUNLI_POINT_URL = '/xunli/point',
   XUNLI_UPDATE_POINT_URL = '/xunli/point/update',
   XUNLI_UPDATE_URL = '/xunli/update',
   XUNLI_BYID_URL = '/xunli/byid',
   XUNLI_POINT_BYID_URL = '/xunli/point/byid',
   XUNLI_SEARCH_URL = '/xunli/search',
   XUNLI_SEARCH_NEW_URL= '/xunli/searchnew',
   XUNLI_SEARCH_HOT_URL= '/xunli/searchhot',
   XUNLI_SEARCH_RAN_URL= '/xunli/searchran',
   XUNLI_CLICK_POINT_URL = 'xunli/point/clicksearch',
   XUNLI_ADD_NEW = '/xunli/addnew',
   /** 全站作品列表，供途经点导入（不限地图视口） */
   XUNLI_PICKER_WORKS_URL = '/xunli/picker/works',
   XUNLI_PICKER_POINTS_URL = '/xunli/picker/points',
   XUNLI_AGENT_PICKER_WORKS_URL = '/xunli/agent/picker/works',
   XUNLI_AGENT_PICKER_POINTS_URL = '/xunli/agent/picker/points',
}

export const getXunli = (data) => request.get<any, any>(API.XUNLI_URL+`?Zoom=${data}`)
export const getXunliPoint = (data) => request.get<any, any>(API.XUNLI_POINT_URL+`?Zoom=${data}`)
export const getXunliUpdatePoint = (data) => request.get<any, any>(API.XUNLI_UPDATE_POINT_URL+`?data=${data}`)
export const getXunliUpdate = (data) => request.get<any, any>(API.XUNLI_UPDATE_URL+`?data=${data}`)
export const getXunliById = (data) => request.get<any, any>(API.XUNLI_BYID_URL+`?Xunliid=${data}`)
export const getXunliPointById = (data) => request.get<any, any>(API.XUNLI_POINT_BYID_URL+`?Xunliid=${data}`)
export const getXunliSearch = (data) => request.get<any, any>(API.XUNLI_SEARCH_URL+`?Search=${data}`)
export const getXunliSearchNew = () => request.get<any, any>(API.XUNLI_SEARCH_NEW_URL)
export const getXunliSearchHot = () => request.get<any, any>(API.XUNLI_SEARCH_HOT_URL)
export const getXunLiSearchRan = () => request.get<any, any>(API.XUNLI_SEARCH_RAN_URL)
export const getXunliClickPoint = (data) => request.get<any, any>(API.XUNLI_CLICK_POINT_URL+`?data=${data}`)
export const addXunliNew = (data) => request.post<any, any>(API.XUNLI_ADD_NEW,data)

/** 单次数据量较大，单独放宽超时（仍受后端 LIMIT） */
export const getXunliPickerWorks = () =>
  request.get<any, any>(API.XUNLI_AGENT_PICKER_WORKS_URL, { timeout: 120000 })

export const getXunliPickerPoints = () =>
  request.get<any, any>(API.XUNLI_AGENT_PICKER_POINTS_URL, { timeout: 180000 })
