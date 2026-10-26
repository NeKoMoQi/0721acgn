<template>
  <div class="root-container">
    <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>

    <div class="background-layer"></div>

    <div class="acgn_container" ref="maintain" @scroll="handscroll">
      <div class="block text-center">
        <span class="demonstration"></span>
        <el-carousel height="535px" motion-blur>
          <el-carousel-item
            v-for="item in novellisthot"
            :key="'h' + item.novelid"
            style="display: flex; border-radius: 30px; overflow: hidden"
            @click="gotonovel(item.novelid)"
          >
            <div style="position: relative; height: 100%; width: 100%">
              <img :src="item.hengpic" style="width: 100%; height: 100%; object-fit: cover" />
              <div
                style="
                  position: absolute;
                  bottom: 40px;
                  left: 50%;
                  transform: translateX(-50%);
                  min-width: 320px;
                  max-width: 60%;
                  padding: 16px 24px;
                  background: linear-gradient(135deg, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.7) 100%);
                  border-radius: 12px;
                  color: #fff;
                  display: flex;
                  gap: 20px;
                  align-items: center;
                  font-size: 18px;
                  font-weight: 600;
                  backdrop-filter: blur(8px);
                  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
                  z-index: 100;
                  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
                  cursor: pointer;
                "
              >
                <div style="border-right: 2px solid rgba(255, 255, 255, 0.15); padding-right: 20px">
                  {{ item.name }}
                </div>
                <div style="font-weight: 400; font-size: 16px; line-height: 1.4; opacity: 0.9">
                  {{ item.shortcomment }}
                </div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
      <div style="margin-top: 40px">
        <h1>猜你喜欢</h1>
        <AcgnFilterBar @change="onFilterChange" />
      </div>
      <div class="acgn_show">
        <div v-for="item in novellist" :key="'g' + item.novelid" class="card-container" @click="gotonovel(item.novelid)">
          <div class="card-inner">
            <div class="image-wrapper">
              <img :src="item.picurl" alt="封面图片" class="cover-image" />
            </div>
            <div class="content-wrapper">
              <h4 class="title">{{ item.name }}</h4>
              <p class="description">{{ item.shortcomment }}</p>
            </div>
          </div>
        </div>
      </div>
      <div v-if="isloading" class="tishi">加载中...请稍等哦^_^</div>
      <div v-if="!nomore" class="tishi">已经到底啦~亲(๑•̀ㅂ•́)و✧</div>
    </div>

    <Behind class="bottom"></Behind>
  </div>
</template>

<script setup>
import { getnovel, getnovelhot } from '@/api/novel/index'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/stores/modules/users'
import AcgnFilterBar from '@/components/AcgnFilterBar.vue'
import { decadeRange } from '@/utils/acgnConstants'

const usestore = useUserStore()
const novellist = ref([])
const $router = useRouter()
const novellisthot = ref([])
const maintain = ref(null)
const isloading = ref(false)
const nomore = ref(true)
const currentpage = ref(2)
const listParams = ref({})

async function reloadList() {
  nomore.value = true
  currentpage.value = 2
  const result = await getnovel(1, listParams.value)
  const rows = result.data || []
  novellist.value = rows
  if (rows.length < 20) nomore.value = false
}

function onFilterChange(f) {
  const yr = decadeRange(f.decade || '')
  listParams.value = {
    genre: f.genre || undefined,
    status: f.status || undefined,
    sortBy: f.sortBy || 'default',
    ...yr,
  }
  reloadList()
}

onMounted(async () => {
  const [hotRes] = await Promise.all([getnovelhot(), reloadList()])
  novellisthot.value = hotRes.data || []
})

function gotonovel(id) {
  usestore.novelid = id
  $router.push({
    name: 'NovelIntroduce',
    query: { novelid: id },
  })
}

const getMore = async () => {
  try {
    const result = await getnovel(currentpage.value, listParams.value)
    const rows = result.data || []
    if (rows.length < 20) nomore.value = false
    if (rows.length) novellist.value.push(...rows)
    currentpage.value += 1
  } finally {
    isloading.value = false
  }
}

function load() {
  if (isloading.value || !nomore.value) return
  isloading.value = true
  getMore()
}

function handscroll() {
  const el = maintain.value
  if (!el) return
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distanceToBottom <= 150) load()
}
</script>
<style scoped>
.card-container {
  margin: 5px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
  height: 280px;
}

.card-container:hover {
  transform: translateY(-4px);
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.image-wrapper:hover .cover-image {
  transform: scale(1.05);
}

.content-wrapper {
  padding: 16px;
  background: white;
}

.title {
  margin: 0 0 8px 0;
  font-size: 1rem;
  color: #333;
  text-align: center;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.description {
  margin: 0;
  font-size: 0.8rem;
  color: #666;
  text-align: center;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 768px) {
  .card-container {
    margin: 8px;
  }

  .image-wrapper {
    height: 150px;
  }

  .title {
    font-size: 1rem;
  }

  .description {
    font-size: 0.8rem;
  }
}

.tishi {
  text-align: center;
  padding: 1rem;
  color: #666;
  font-size: 0.9rem;
  background: #f8f9fa;
  border-radius: 4px;
  margin: 10px 0;
  border: 1px solid #e9ecef;
}

.acgn_show {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  width: auto;
  margin-top: 20px;
  padding: 20px;
}

.demonstration {
  color: var(--el-text-color-secondary);
}

.el-carousel__item h3 {
  color: #475669;
  opacity: 0.75;
  line-height: 200px;
  margin: 0;
  text-align: center;
}

.el-carousel__item:nth-child(2n) {
  background-color: #99a9bf;
}

.el-carousel__item:nth-child(2n + 1) {
  background-color: #d3dce6;
}

.root-container {
  position: relative;
  min-height: 100vh;
}

.background-layer {
  position: fixed;
  left: 0;
  top: 50%;
  transform: translateY(-45%);
  width: 100vw;
  height: 0;
  padding-top: 56.25%;
  background: url('/BCPHOTO/acgnbc4.png') no-repeat center/contain;
  overflow: hidden;
}

.acgn_container {
  top: 100px;
  bottom: 50px;
  width: 62%;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(211, 211, 211, 0.8);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 40px;
  min-height: 60vh;
  max-height: calc(100vh - 100px);
  position: relative;
  margin-bottom: 40px;
  overflow-y: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.div::-webkit-scrollbar {
  display: none;
}

.bottom {
  position: relative;
  top: 200px;
}
</style>
