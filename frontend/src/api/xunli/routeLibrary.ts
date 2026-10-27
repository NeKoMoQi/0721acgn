import request from '@/utils/request'

function result(body: any) {
  if (body?.code !== 200) throw new Error(body?.msg || '行程操作失败')
  return body.data
}
export async function listSavedRoutes() { return result(await request.get('/xunli/agent/routes')) }
export async function saveRoutePlan(title: string, plan: any) { return result(await request.post('/xunli/agent/routes', { title, plan })) }
export async function loadRoutePlan(id: string) { return result(await request.get(`/xunli/agent/routes/${encodeURIComponent(id)}`)) }
export async function deleteRoutePlan(id: string) { return result(await request.delete(`/xunli/agent/routes/${encodeURIComponent(id)}`)) }
