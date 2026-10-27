<template>
  <Headpage></Headpage>
  <div class="live2d-role-console" :class="{ 'live2d-role-console--chat': agentMode === 'chat' }">
    <div class="live2d-mode-switch" role="tablist" aria-label="Live2D AI 功能模式">
      <button
        type="button"
        :class="{ active: agentMode === 'route' }"
        :disabled="agentRoleSending"
        @click="setAgentMode('route')"
      >
        路线查询
      </button>
      <button
        type="button"
        :class="{ active: agentMode === 'chat' }"
        :disabled="agentRoleSending"
        @click="setAgentMode('chat')"
      >
        日常闲聊
      </button>
    </div>
    <div v-if="agentMode === 'chat'" class="live2d-chat-stage">
      <div class="live2d-chat-stage-title">
        <strong>小窝日常闲聊</strong>
        <span>主人，想聊什么都可以告诉我喵~</span>
      </div>
    </div>
    <div v-if="agentMode === 'chat'" ref="agentChatScrollRef" class="live2d-chat-messages">
      <div
        v-for="message in agentChatMessages"
        :key="message.id"
        class="live2d-chat-message"
        :class="'live2d-chat-message--' + message.role"
      >
        {{ message.content }}
      </div>
    </div>
    <div v-if="agentRoleSending || agentStreamError" class="agent-progress" role="status" aria-live="polite">
      <div class="agent-progress-heading">
        <span>{{ agentStreamError || agentProgress }}</span>
        <button v-if="agentRoleSending" type="button" @click="cancelAgentRequest">停止</button>
      </div>
      <div v-if="agentPartialReply" class="agent-partial-reply">{{ agentPartialReply }}</div>
    </div>
    <div class="live2d-input-row">
      <input
        v-model="agentRoleInput"
        class="live2d-role-input"
        maxlength="2000"
        :disabled="agentRoleSending"
        :placeholder="agentMode === 'route' ? '想巡礼哪部番或哪座城市？' : '和小窝随便聊一句...'"
        @focus="forceLive2DVisible"
        @keydown.enter.prevent="sendAgentRoleMessage"
      />
      <button class="live2d-role-send" :disabled="agentRoleSending || !agentRoleInput.trim()" @click="sendAgentRoleMessage">
        {{ agentRoleSending ? '思考中' : '发送' }}
      </button>
      <button type="button" class="agent-new-session" :disabled="agentRoleSending" @click="newAgentConversation">新对话</button>
      <button type="button" class="agent-new-session" :disabled="agentRoleSending" @click="openAgentLibrary">我的行程</button>
    </div>
  </div>
  <el-dialog v-model="agentLibraryVisible" title="我的巡礼行程" width="min(92vw, 560px)" :z-index="20100">
    <p>保存到当前登录账号，读取时重新核对地点状态。</p>
    <p v-if="agentLibraryLoading">正在加载…</p>
    <p v-else-if="!agentSavedRoutes.length">还没有保存的行程。</p>
    <div v-for="saved in agentSavedRoutes" :key="saved.id" class="agent-saved-row">
      <div><strong>{{ saved.title }}</strong><small>{{ saved.days }} 天 · {{ saved.updatedAt }}</small></div>
      <el-button size="small" :disabled="agentLibraryLoading || agentRoleSending" @click="restoreAgentRoute(saved.id)">打开</el-button>
      <el-button size="small" type="danger" plain :disabled="agentLibraryLoading" @click="removeAgentRoute(saved.id)">删除</el-button>
    </div>
  </el-dialog>
  <section v-if="agentMode === 'route' && (agentGuideText || agentGuideSpots.length)" class="agent-guide-panel">
    <div class="agent-guide-head">
      <div>
        <strong>小窝巡礼讲解</strong>
        <span>按天预览，确认后导入地图</span>
      </div>
      <button class="agent-guide-close" @click="clearAgentGuide">×</button>
    </div>
    <div v-if="agentRoutePlan?.days?.length" class="agent-day-controls">
      <div class="agent-day-tabs" role="tablist" aria-label="巡礼天数">
        <button v-for="(day, index) in agentRoutePlan.days" :key="day.day" type="button"
          role="tab" :aria-selected="agentSelectedDay === index" :class="{ active: agentSelectedDay === index }"
          @click="selectAgentDay(index)">第 {{ day.day }} 天</button>
      </div>
      <p v-if="activeAgentDay">{{ activeAgentDay.spots.length }} 个地点 · 直线合计约 {{ activeAgentDay.straightLineKm }} km（非实际路程）</p>
      <p v-for="warning in activeAgentDay?.warnings || []" :key="warning" class="agent-day-warning">{{ warning }}</p>
      <button type="button" class="agent-import-day" :disabled="agentImporting || !map || agentRoleSending" @click="importAgentDay">
        {{ agentImporting ? '正在导入…' : agentGuideSpots.length === 1 ? '在地图查看当天地点' : '将这一天导入地图' }}
      </button>
      <div class="agent-save-row">
        <input v-model="agentRouteTitle" aria-label="行程名称" maxlength="100" placeholder="行程名称" />
        <button type="button" :disabled="agentSaving || agentRoleSending" @click="saveAgentRoute">{{ agentSaving ? '保存中…' : '保存行程' }}</button>
      </div>
    </div>
    <div v-if="agentGuideSpots.length" class="agent-guide-spots">
      <button
        v-for="(spot, index) in agentGuideSpots"
        :key="spot.dbPointId || spot.id || spot.name || index"
        class="agent-guide-spot"
        :class="{ active: agentGuideActiveSpotKey === agentGuideSpotKey(spot, index) }"
        @click="focusAgentGuideSpot(spot, index)"
      >
        <img v-if="spot.imageUrl" :src="spot.imageUrl" alt="" />
        <div v-else class="agent-guide-spot-placeholder"></div>
        <div>
          <b>{{ index + 1 }}. {{ spot.name }}</b>
          <span>{{ spot.workTitle || spot.address || '站内巡礼点' }}</span>
          <small>{{ spot.accessStatus === 'public' ? '曾核实可达，现场为准' : '开放状态待核实' }}</small>
          <small v-if="spot.verifiedAt">最近核验：{{ spot.verifiedAt }}</small>
          <small v-if="spot.photoNote">{{ spot.photoNote }}</small>
          <small v-if="spot.sourceUrl">资料来源：{{ spot.sourceUrl }}</small>
        </div>
      </button>
    </div>
    <div v-if="agentGuideText" class="agent-guide-text" v-html="agentGuideHtml"></div>
    <div v-if="agentKnowledgeSources.length" class="agent-knowledge-sources">
      <strong>本次检索参考 · 站内编辑资料</strong>
      <p v-for="source in agentKnowledgeSources" :key="source.id">{{ source.title }} · {{ source.updatedAt }}<br /><small>{{ source.source }}</small></p>
    </div>
  </section>
  <div class="navigation-controls" v-if="navigation && !agentRouteFocused">
        <div class="nav-field">
          <el-input
            v-model="startPoint"
            placeholder="输入起点"
            class="nav-input"
            @focus="clearRoute"
            @input="handleInput('start', $event)"
          >
            <template #prefix>
              <el-icon><Location /></el-icon>
            </template>
            <template #suffix>
              <el-icon v-if="loadingStart"><Loading /></el-icon>
            </template>
          </el-input>
          <div v-if="startSuggestions.length > 0" class="suggestion-dropdown">
            <div
              v-for="(suggestion, index) in startSuggestions"
              :key="index"
              class="suggestion-item"
              @click="selectSuggestion('start', suggestion)"
            >
              <div class="suggestion-name">{{ suggestion.name }}</div>
              <div class="suggestion-address">{{ suggestion.address }}</div>
            </div>
          </div>
        </div>

        <div class="wp-add-row">
          <el-tooltip content="添加一个途经点" placement="bottom">
            <el-button circle type="primary" plain size="small" class="wp-add-circle" @click="addWaypointStop">
              <el-icon><Plus /></el-icon>
            </el-button>
          </el-tooltip>
        </div>

        <div
          v-for="(stop, widx) in waypointStops"
          :key="stop.id"
          class="nav-field"
          :class="{ 'wp-nav-field-expanded': stop.suggestions.length > 0 }"
        >
          <div class="wp-row">
            <el-input
              :model-value="stop.query"
              :placeholder="'搜索或选择 · 途经点 ' + (widx + 1)"
              class="nav-input wp-input-flex"
              clearable
              @focus="onWaypointInputFocus(widx)"
              @input="onWaypointRowInput(widx, $event)"
              @clear="onWaypointClear(widx)"
            >
              <template #prefix>
                <el-icon><Guide /></el-icon>
              </template>
              <template #suffix>
                <el-icon v-if="stop.loading"><Loading /></el-icon>
              </template>
            </el-input>
            <el-popover
              placement="bottom-start"
              :width="328"
              trigger="click"
              :teleported="true"
              persistent
              :hide-after="0"
              :z-index="30000"
              popper-class="shengdi-wp-import-popper"
              @show="loadWaypointCatalog"
            >
              <template #reference>
                <el-button
                  circle
                  plain
                  type="success"
                  size="small"
                  class="wp-import-ref"
                  :title="'从地图巡礼数据中导入坐标到途经点 ' + (widx + 1) + '（含可视范围内收录点与当前打开的详情）'"
                >
                  <el-icon><MapLocation /></el-icon>
                </el-button>
              </template>
              <div class="wp-import-pane">
                <div class="wp-import-header">
                  正在为 <strong>途经点 {{ widx + 1 }}</strong> 选择地点。
                  <span class="wp-import-header-hint">全站收录，不限当前缩放范围。</span>
                </div>
                <p v-if="waypointCatalogLoading">正在加载站内地点…</p>
                <el-input
                  :model-value="stop.importFilter"
                  size="small"
                  clearable
                  placeholder="筛选名称 / 作品…"
                  class="wp-import-filter"
                  @update:model-value="(v) => (stop.importFilter = v)"
                />
                <el-scrollbar max-height="260px" class="wp-import-scroll">
                  <div
                    v-for="(pick, pi) in filteredWaypointImports(stop)"
                    :key="'xk' + stop.id + '-' + pi"
                    class="wp-import-item"
                    @mousedown.prevent
                    @click.prevent="applyXunliPickToWaypoint(widx, pick)"
                  >
                    <div class="wp-import-thumb-wrap">
                      <img v-if="pick.imageUrl" :src="pick.imageUrl" class="wp-import-thumb" alt="" />
                      <div v-else class="wp-import-thumb wp-import-thumb-ph" />
                    </div>
                    <div class="wp-import-item-text">
                      <span class="wp-import-name">{{ pick.name }}</span>
                      <span v-if="pick.workTitle" class="wp-import-work">番组：{{ pick.workTitle }}</span>
                      <span class="wp-import-sub">{{ pick.address }}</span>
                    </div>
                  </div>
                  <div v-if="filteredWaypointImports(stop).length === 0" class="wp-import-empty">
                    暂无匹配的巡礼点，或当前图幅内尚无数
                  </div>
                </el-scrollbar>
              </div>
            </el-popover>
            <el-button type="danger" plain circle size="small" class="wp-remove-btn" title="移除此途经点" @click="removeWaypointStop(widx)">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div v-if="stop.suggestions.length > 0" class="suggestion-dropdown">
            <div
              v-for="(suggestion, idx) in stop.suggestions"
              :key="'sw' + stop.id + idx"
              class="suggestion-item"
              @click="selectWaypointSuggestion(widx, suggestion)"
            >
              <div class="suggestion-name">{{ suggestion.name }}</div>
              <div class="suggestion-address">{{ suggestion.address }}</div>
            </div>
          </div>
        </div>

        <div class="nav-field">
          <el-input v-model="endPoint" placeholder="输入终点" class="nav-input" @input="handleInput('end', $event)">
            <template #prefix>
              <el-icon><Flag /></el-icon>
            </template>
            <template #suffix>
              <el-icon v-if="loadingEnd"><Loading /></el-icon>
            </template>
          </el-input>
          <div v-if="endSuggestions.length > 0" class="suggestion-dropdown">
            <div
              v-for="(suggestion, index) in endSuggestions"
              :key="index"
              class="suggestion-item"
              @click="selectSuggestion('end', suggestion)"
            >
              <div class="suggestion-name">{{ suggestion.name }}</div>
              <div class="suggestion-address">{{ suggestion.address }}</div>
            </div>
          </div>
        </div>

        <div class="travel-mode-selector">
          <div class="mode-button" :class="{ active: travelMode === 'driving' }" @click="travelMode = 'driving'">
            <el-icon><Van /></el-icon>
            <span>驾车</span>
          </div>
          <div class="mode-button" :class="{ active: travelMode === 'walking' }" @click="travelMode = 'walking'">
            <el-icon><Promotion /></el-icon>
            <span>步行</span>
          </div>
          <div class="mode-button" :class="{ active: travelMode === 'cycling' }" @click="travelMode = 'cycling'">
            <el-icon><Bicycle /></el-icon><span>骑行</span>
          </div>
        </div>
        <div class="nav-actions">
          <el-button type="primary" @click="calculateRoute" :disabled="!startPoint || !endPoint">
            开始导航
          </el-button>
          <el-button plain type="danger" @click="clearRoute({ closePanel: true, resetFields: true })">
            清除路线
          </el-button>
        </div>
  </div>
  <div class="map-container" >
    <el-drawer
        :data="xunlibyidlist"
        v-model="clickdrawer"
        direction="ltr"
        :modal="false"
        modal-class="mask-layer"
        :before-close="handleClose"
        :mask-closable="false"
        size="100%"
        class="custom-drawers"
      >
        <div class="jianjie" v-if="xunlibyidlist" :style="{ backgroundColor: xunlibyidlist.xunlicolor }">
          <div style="height: 100%; width: 25%">
            <img :src="xunlibyidlist.bcimg" alt="" style="height: 100%; width: 100%" />
          </div>
          <div class="jianjie-content" v-if="xunlibyidlist">
            <div class="jianjie-title-div">
              <span class="jianjie-title">{{ xunlibyidlist.xunliname }}</span>
              <div class="jianjie-circle" :style="{ backgroundColor: xunlibyidlist.circlecolor }"></div>
            </div>
            <div>
              <span class="jianjie-small">{{ xunlibyidlist.city }}</span>
              <span class="dot-divider">·</span>
              <span class="jianjie-small">{{ xunlibyidlist.count }}地标</span>
            </div>
          </div>
        </div>
        <div class="point">
          <div v-for="(item, index) in xunlibyidlist.xunlipoint" :key="item" class="pointmain" @click="navigateTo(item.geopoint, item)" :id="String(index)">
            <div class="point-picturer" v-if="item.xunliname !== null">
              <img class="point-picture" :src="item.gaoqingpic" />
              <div class="ep">EP{{ item.ep }}</div>
              <div class="timestamp">{{ changetime(item.time) }}</div>
            </div>
            <div style="padding: 6px 10px; margin-left: 0" v-if="item.xunliname !== null">
              <h4>{{ item.xunliname }}</h4>
            </div>
          </div>
        </div>
      </el-drawer>
    <div class="custom-search-container" v-if="searching">
      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="您有什么想去巡礼的地方吗"
          class="map-search-input"
          size="large"
          clearable
          @input="inputing(searchQuery)"
          @focus="opensearch(searchQuery)"
          @blur="closesearch"
          @clear="clearing"
        >
          <template #suffix>
            <el-icon class="search-icon"><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="search-result-container" v-if="searchaera">
        <div class="search_city">
          <span v-for="item in searchcitys" :key="item" @click="inputsome(item)">{{ item }}</span>
        </div>
        <div class="search_leibie">
          <span @click="searchnew" :class="{ active: activeIndex === 1 }">最近收录</span>
          <span @click="searchhot" :class="{ active: activeIndex === 2 }">热门番剧</span>
          <span @click="searchrange" :class="{ active: activeIndex === 3 }">随机宝藏</span>
        </div>
        <div class="search_leibie_result">
          <div v-for="(item, six) in xunlisearchlist" :key="'s' + six + item.xunliname" class="searchitem" @click="searchtos(item)">
            <div style="display: flex">
              <div class="searchimg" :style="{ backgroundImage: `url(${item.bcimg})` }"></div>
              <div class="searchdata">
                <div class="searchname">{{ item.xunliname }}</div>
                <div class="searchinfo">
                  <span>{{ item.city }}</span><span>{{ item.count }}地标</span><span>{{ item.count }}截图</span>
                </div>
              </div>
              <div style="align-items: center; justify-items: center; display: flex">
                <div class="search-circle" :style="{ backgroundColor: item.xunlicolor }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="search-result-out" v-if="isinputing">
      <div class="mainresult">
        <div
          class="jianjie"
          :style="{ backgroundColor: item.xunlicolor }"
          v-for="(item, ridx) in searchlist"
          :key="'r' + ridx + item.xunliname"
          @click="searchto(item)"
        >
          <div style="height: 100%; width: 25%">
            <img :src="item.bcimg" alt="" style="height: 100%; width: 100%" />
          </div>
          <div class="jianjie-content" v-if="item">
            <div class="jianjie-title-div">
              <span class="jianjie-title">{{ item.xunliname }}</span>
              <div class="jianjie-circle" :style="{ backgroundColor: item.circlecolor }"></div>
            </div>
            <div>
              <span class="jianjie-small">{{ item.city }}</span>
              <span class="dot-divider">·</span>
              <span class="jianjie-small">{{ item.count }}地标</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  

      <div class="back" v-if="clickdrawer" @click="back">
        <el-icon style="width: 14px; height: 14px; display: flex; align-items: center">
          <ArrowLeftBold />
        </el-icon>
        返回地图
      </div>

      <div ref="mapContainer" id="map"></div>
      <div id="custom-geocoder-container"></div>
  </div>
</template>

<script setup>
import { ElMessage,ElNotification } from 'element-plus'
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { getXunli,getXunliPoint,getXunliUpdatePoint,getXunliUpdate,getXunliById,getXunliPointById,getXunliSearch,getXunliSearchNew,getXunliSearchHot,getXunLiSearchRan,getXunliClickPoint,getXunliPickerWorks,getXunliPickerPoints} from '@/api/xunli';
import { decode as decodePolyline } from '@mapbox/polyline';
import axios from 'axios';
import * as turf from '@turf/turf';
import mapboxgl, { NavigationControl} from 'mapbox-gl';
import MapboxDirections from '@mapbox/mapbox-gl-directions/dist/mapbox-gl-directions.js';
import '@mapbox/mapbox-gl-directions/dist/mapbox-gl-directions.css';
import MapboxLanguage from '@mapbox/mapbox-gl-language';
import MapboxGeocoder from '@mapbox/mapbox-gl-geocoder';
import '@mapbox/mapbox-gl-geocoder/dist/mapbox-gl-geocoder.css';
import 'mapbox-gl/dist/mapbox-gl.css';
import { ArrowLeftBold, Search } from '@element-plus/icons-vue'
import { add, debounce, last } from 'lodash';
import { da, fa, fr, tr } from 'element-plus/es/locales.mjs';
import { nextTick } from 'vue';
import { useRouter } from 'vue-router';
import useUserStore from '@/stores/modules/users';
import { cleanupLive2D, mountShengdiLive2D } from '@/utils/live2d-loader';
import { streamAgentChat } from '@/api/xunli/agentStream';
import { listSavedRoutes, saveRoutePlan, loadRoutePlan, deleteRoutePlan } from '@/api/xunli/routeLibrary';
import { getOrCreateAcgnClientId } from '@/utils/acgnLocal';
const FOURSQUARE_API_KEY = import.meta.env.VITE_FOURSQUARE_API_KEY || '';
const FOURSQUARE_CATEGORIES = '16000,19001,19002,19003,19004'; 
mapboxgl.accessToken = 'pk.eyJ1IjoiNHd4bm51dXYiLCJhIjoiY204N25oaGJ3MDk0NzJpcXU1YmtvcGM5OCJ9.05C4KGMpkW1QolRvxwSt_g';
const $router = useRouter()
const userStore = useUserStore()
const searching = ref(true)
const xunlipointbyidlist = ref([])
const xunlibyidlist = ref([])
const searchText = ref('');
const mapContainer = ref(null);
const map = ref(null);
const clickdrawer = ref(false);
const drawer = ref(false);
const datalist1 = ref([]);
const datalist2 = ref([]);
const array = ref([])
const direction = ref('rtl')
const language = new MapboxLanguage();
const zoom = ref(5.7)
const xunlilist = ref([])
const xunlipointlist = ref([])
/** 途经点导入：全站数据集（不按视口裁剪） */
const pickerWorksAll = ref([])
const pickerPointsAll = ref([])
const waypointCatalogLoading = ref(false)
let waypointCatalogLoaded = false
const pointcolor = ref('#333')
const markers = ref([])
const agentRouteFocused = ref(false)
const agentMode = ref('route')
const agentRoleInput = ref('')
const agentRoleSending = ref(false)
const agentConversationId = ref('')
const agentConversationIds = { route: '', chat: '' }
const agentProgress = ref('')
const agentPartialReply = ref('')
const agentStreamError = ref('')
const agentRoutePlan = ref(null)
const agentKnowledgeSources = ref([])
const agentRouteTitle = ref('')
const agentSaving = ref(false)
const agentLibraryVisible = ref(false)
const agentLibraryLoading = ref(false)
const agentSavedRoutes = ref([])
const agentSelectedDay = ref(0)
const activeAgentDay = computed(() => agentRoutePlan.value?.days?.[agentSelectedDay.value] || null)
const agentImporting = ref(false)
let agentAbortController = null
const agentChatScrollRef = ref(null)
const agentChatMessages = ref([
  {
    id: 'chat-welcome',
    role: 'assistant',
    content: '主人，日常闲聊时我会在这里陪你喵~ 想排路线时再点回路线查询就好。',
  },
])
const agentGuideText = ref('')
const agentGuideSpots = ref([])
const agentGuideActiveSpotKey = ref('')
const agentGuideHtml = computed(() => formatAgentGuideHtml(agentGuideText.value))
const agentWaitingTimers = []
let agentWaitingLoopTimer = null
let agentGuideHighlightMarker = null
/** 拖拽/缩放末尾会高频触发刷新：异步世代号丢弃过期结果 */
let mapViewportUpdateGen = 0
/** 已成功拉取的视口 key，完全相同则跳过重复请求与标点重绘 */
let lastViewportFetchKey = ''
/** init 赋值，onBeforeUnmount 时 cancel */
let debouncedMapViewportUpdate = null
const clickpoint = ref(false)
//控制具体点
const tanchuang = ref(false)
const searchQuery = ref('');
const searchaera = ref(false)
const isinputing = ref(false)
const searchlist = ref([])
const ismask = ref(false)
const truesearch = ref(false)
const searchlei = ref(false)
const searchcitys = ref(['东京都','神奈川县','京都府','北海道','爱知县','镰仓县','埼玉县','欧洲','京都市','岐阜县','千叶县','名古屋市'])
const activeIndex = ref(1)
const xunlisearchlist = ref([])
const startPoint = ref('')
const endPoint = ref('')
const directions = ref(null);
const startSuggestions = ref([]);
const endSuggestions = ref([]);
const loadingStart = ref(false);
const loadingEnd = ref(false);
const waypointIdCounter = ref(0);
const waypointStops = ref([]);
const selectedStart = ref(null);
const selectedEnd = ref(null);
const travelMode = ref('driving')//防止推荐搜索触发搜索框白屏bug
const navigation = ref(false)
const endll = ref()
const isnavigation = ref(false)
const lastid =  ref('0')
const doubleclick = ref(false)
const profileMap = {
  driving: 'mapbox/driving',
  walking: 'mapbox/walking',
  cycling: 'mapbox/cycling'
};
/** Directions / 标点统一用 [lng, lat]，与 Foursquare、巡礼 geo 对齐 */
function navCoordsToLngLat(v) {
  if (!Array.isArray(v) || v.length < 2) return null;
  const lng = Number(v[0]);
  const lat = Number(v[1]);
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null;
  return [lng, lat];
}

function isAgentSpot(v) {
  return !!v && typeof v === 'object' && (
    v.type === 'work' ||
    v.type === 'point' ||
    (v.name != null && (v.lng != null || Array.isArray(v.geopoint)))
  )
}

function agentSpotToPick(spot) {
  if (!isAgentSpot(spot)) return null
  const coords =
    navCoordsToLngLat(spot.geopoint) ||
    (spot.lng != null && spot.lat != null ? navCoordsToLngLat([spot.lng, spot.lat]) : null)
  if (!coords) return null
  return {
    dbPointId: spot.id || spot.dbPointId || '',
    xunliid: spot.xunliid || '',
    name: String(spot.name || spot.workTitle || '站内巡礼点').trim(),
    address: String(spot.address || spot.city || '站内巡礼点').trim(),
    coordinates: coords,
    category: spot.type === 'work' ? '巡礼主视点' : '巡礼点',
    imageUrl: spot.imageUrl || '',
    workTitle: spot.workTitle || '',
    sourceType: spot.type || 'point',
  }
}
const searchtos = (item) => {
  searchto(item)
  truesearch.value = false
}
const inputsome = (some) =>{
  searchlei.value = true
  searchQuery.value = some
  inputing(some)
}
const searchnew = async() =>{
  const result = await getXunliSearchNew()
  xunlisearchlist.value = result.data
  activeIndex.value = 1
  searchlei.value = true
  searchaera.value = true
}
const searchhot = async() =>{
  const result = await getXunliSearchHot()
  xunlisearchlist.value = result.data
  activeIndex.value = 2
  searchlei.value = true
  searchaera.value = true
}
const searchrange = async() =>{
  const result = await getXunLiSearchRan()
  xunlisearchlist.value = result.data
  activeIndex.value = 3
  searchlei.value = true
  searchaera.value = true
}
const searchto = async(item) =>{
  truesearch.value = true
  searchaera.value = false
  searching.value = false
  isinputing.value = false
  if(ismask.value == true){
    map.value.removeLayer('mask')
  map.value.removeLayer('maskline')
  map.value.removeSource('mask')
  map.value.removeSource('maskline')
  ismask.value = false
  }
  const LngLat = [item.maingeo[0], item.maingeo[1]]
  searching.value = false
      map.value.flyTo({
        center:LngLat,
        zoom:16,
        essential: true,
        duration: 6000 // 动画持续时间（毫秒）
})
      const result = await getXunliById(item.xunliid)
      const result2 = await getXunliPointById(item.xunliid)
      xunlibyidlist.value = result.data
      xunlibyidlist.value.circlecolor = xunlibyidlist.value.xunlicolor
      xunlibyidlist.value.xunlicolor = xunlibyidlist.value.xunlicolor + '12'
      xunlipointbyidlist.value = result2.data
      xunlipointbyidlist.value.xunlicolor = xunlibyidlist.value.circlecolor
      xunlibyidlist.value.xunlipoint = xunlipointbyidlist.value
      xunlipointbyidlist.value.xunlimainname = xunlibyidlist.value.xunliname
      updatepointbyid(xunlipointbyidlist)
      mask(xunlipointbyidlist)
      if(xunlipointbyidlist.value.length > 0){
        const bounds = new mapboxgl.LngLatBounds();
        xunlipointbyidlist.value.forEach(item => {
        const lng = item.geopoint[0];  // 经度
        const lat = item.geopoint[1];  // 纬度
        bounds.extend([lng, lat]);
      })
      const mainLng = xunlibyidlist.value.maingeo[0];
    const mainLat = xunlibyidlist.value.maingeo[1];
    bounds.extend([mainLng, mainLat]);
    console.log(bounds)
    // 调整地图视野
    map.value.fitBounds(bounds, {
      padding: { top: 100, bottom: 100, left: 500, right: 100 }, // 四周留白
      maxZoom: 16, // 最大缩放级别
      duration: 2000
    });
    }
      clickdrawer.value = true


}
const inputing = async(search) =>{
  searchaera.value = false
  if(search !== ''){
    const res = await getXunliSearch(search)
    isinputing.value = true
    searchlist.value = res.data
    for(let i = 0; i < searchlist.value.length; i++){
      searchlist.value[i].circlecolor = searchlist.value[i].xunlicolor
      searchlist.value[i].xunlicolor = searchlist.value[i].xunlicolor + '20'
    }
    console.log(searchlist.value)
  }
  if(search == ''){
    isinputing.value = false
  }
}
const clearing = () =>{
  searchQuery.value = ''
  isinputing.value = false
  searchaera.value = true
  opensearch()
}
const opensearch = async(searchQuery) =>{
  if(searchQuery == ''){
    searchaera.value = true
    const result = await getXunliSearchNew()
    xunlisearchlist.value = result.data
  }
}
const closesearch = () =>{
  if(searchlei.value){
    searchaera.value = false
    searchlei.value = false
  }
  if(searchQuery == ''){
    isinputing.value = false
    searchlei.value = false
  }
}
const open = () => {
  drawer.value = !drawer.value;
};
const back = () => {
  navigation.value = false
  waypointStops.value = []
  clearRoute();
  ismask.value = false
  clickdrawer.value = false
  clickpoint.value = false
  map.value.removeLayer('mask')
  map.value.removeLayer('maskline')
  map.value.removeSource('mask')
  map.value.removeSource('maskline')
  if(truesearch.value){
    isinputing.value = true
    truesearch.value = false
  }
  searching.value = true
  lastViewportFetchKey = ''
  updateData()
}
const handlescroll = () => {
  // 处理滚动逻辑
}
const navigateTo = (geopoint,item) => {
  const LngLat = [geopoint[0],geopoint[1]]
  map.value.flyTo({
        center:LngLat,
        zoom:14,
        essential: true,
        duration: 2000 // 动画持续时间（毫秒）
})
showjianjie(item)
}
const handleClose = (done) => {
  done();
};

const handleRecommend = (item) => {
  // 处理推荐项点击逻辑
};

const geocoder = new MapboxGeocoder({
  accessToken: mapboxgl.accessToken,
  mapboxgl: mapboxgl,
  language: 'zh-CN'
})

function roundKey(n, d) {
  const p = 10 ** d
  return Math.round(n * p) / p
}

function makeViewportKey(bounds, z) {
  return [
    roundKey(bounds.getWest(), 2),
    roundKey(bounds.getSouth(), 2),
    roundKey(bounds.getEast(), 2),
    roundKey(bounds.getNorth(), 2),
    roundKey(z, 1),
  ].join('|')
}

const updateData = async () => {
  if (!map.value || clickpoint.value || agentRouteFocused.value) return
  const bounds = map.value.getBounds()
  const curZoom = map.value.getZoom()
  zoom.value = curZoom

  const vkey = makeViewportKey(bounds, curZoom)
  if (vkey === lastViewportFetchKey) return

  const seq = ++mapViewportUpdateGen
  const data = [
    bounds.getWest(),
    bounds.getSouth(),
    bounds.getEast(),
    bounds.getNorth(),
    curZoom,
  ]

  try {
    const [result, result2] = await Promise.all([
      getXunliUpdate(data),
      getXunliUpdatePoint(data),
    ])
    if (seq !== mapViewportUpdateGen) return
    xunlilist.value = result.data
    xunlipointlist.value = result2.data
    lastViewportFetchKey = vkey
    getxunlipoint()
  } catch {
    if (seq === mapViewportUpdateGen) lastViewportFetchKey = ''
  }
};
//点击随机图片跳转相应番剧
const clickjumptoanimate = async(xunliid,id) =>{
  console.log(id)
  var idlist = []
  const result = await getXunliById(xunliid)
      const result2 = await getXunliPointById(xunliid)
      xunlibyidlist.value = result.data
      xunlibyidlist.value.circlecolor = xunlibyidlist.value.xunlicolor || 'red'
      xunlibyidlist.value.xunlicolor = xunlibyidlist.value.xunlicolor + '12'
      xunlipointbyidlist.value = result2.data
      xunlipointbyidlist.value.xunlicolor = xunlibyidlist.value.circlecolor
      xunlibyidlist.value.xunlipoint = xunlipointbyidlist.value
      xunlipointbyidlist.value.xunlimainname = xunlibyidlist.value.xunliname
      console.log(xunlipointbyidlist.value)
      for(let i = 0; i < xunlipointbyidlist.value.length; i++)
      if(xunlipointbyidlist.value[i].id == id){
        idlist = xunlipointbyidlist.value[i]
      }
      updatepointbyid(xunlipointbyidlist)
      mask(xunlipointbyidlist)
      if(xunlipointbyidlist.value.length > 0){
        const bounds = new mapboxgl.LngLatBounds();
        xunlipointbyidlist.value.forEach(item => {
        const lng = item.geopoint[0];  // 经度
        const lat = item.geopoint[1];  // 纬度
        bounds.extend([lng, lat]);
      })
      const mainLng = xunlibyidlist.value.maingeo[0];
    const mainLat = xunlibyidlist.value.maingeo[1];
    bounds.extend([mainLng, mainLat]);
    }
      clickdrawer.value = true
      map.value.flyTo({
        center:[idlist.geopoint[0],idlist.geopoint[1]],
        zoom:10,
        essential: true,
        duration: 4000 // 动画持续时间（毫秒）
})
      await nextTick()
      showjianjie(idlist)

}
const clearAllMarkers = () => {
  markers.value.forEach((marker) => {
    try {
      marker.remove()
    } catch (_) {
      /* */
    }
  })
  markers.value = []
  const root = map.value?.getContainer?.()
  if (!root) return
  const rmWrap = (el) => {
    const w = el.closest('.mapboxgl-marker')
    if (w) w.remove()
    else el.remove()
  }
  root.querySelectorAll('.clickjianjie').forEach(rmWrap)
  root.querySelectorAll('.marker-container').forEach(rmWrap)
  root.querySelectorAll('.piccontainer').forEach(rmWrap)
  root.querySelectorAll('.xunlijianjie').forEach(rmWrap)
  root.querySelectorAll('.marker-point').forEach(rmWrap)
}

const restoreMapMarkers = () => {
  if (!map.value || !agentRouteFocused.value) return
  agentRouteFocused.value = false
  lastViewportFetchKey = ''
  clickpoint.value = false
  getxunlipoint()
}

function forceLive2DVisible() {
  document.getElementById('live2dChatPanel')?.remove()
  document.getElementById('chat-history-panel')?.remove()
  document.getElementById('live2d-toolbox')?.remove()

  const canvas = document.getElementById('live2d')
  if (canvas) {
    canvas.style.position = 'fixed'
    canvas.style.right = '18px'
    canvas.style.bottom = '0'
    canvas.style.left = 'auto'
    canvas.style.top = 'auto'
    canvas.style.opacity = '1'
    canvas.style.display = ''
    canvas.style.visibility = 'visible'
    canvas.style.pointerEvents = 'auto'
    canvas.style.zIndex = '9999'
  }

  const messageBox = document.getElementById('live2dMessageBox')
  if (messageBox) {
    messageBox.style.display = ''
    messageBox.style.opacity = '1'
    messageBox.style.visibility = 'visible'
    messageBox.style.right = '18px'
    messageBox.style.bottom = '405px'
    messageBox.style.top = 'auto'
    messageBox.style.width = '320px'
    messageBox.style.height = 'auto'
    messageBox.style.justifyContent = 'flex-end'
  }

}

function keepLive2DVisible(reply, live2dCommand) {
  forceLive2DVisible()
  window.Live2dRender?.revealMessageBox?.()
  if (reply) {
    window.Live2dRender?.setMessageBox?.(
      reply,
      live2dCommand?.messageDurationMs || 9000,
    )
  }
}

function forceLive2DVisibleRepeatedly(duration = 3200) {
  const startedAt = Date.now()
  const tick = () => {
    forceLive2DVisible()
    if (Date.now() - startedAt < duration) {
      window.setTimeout(tick, 250)
    }
  }
  tick()
}

function handleLive2DChatActivity(event) {
  const target = event?.target
  if (!(target instanceof Element)) return
  if (
    target.closest('.live2d-role-console') ||
    target.closest('#live2dMessageBox')
  ) {
    forceLive2DVisible()
  }
}

function emitLive2DAgentResponse(payload) {
  window.dispatchEvent(new CustomEvent('live2d-agent-response', { detail: payload }))
}

function syncAgentModeBodyClass() {
  document.body.classList.toggle('shengdi-live2d-chat-mode', agentMode.value === 'chat')
}

function scrollAgentChatToBottom() {
  nextTick(() => {
    const el = agentChatScrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function appendAgentChatMessage(role, content) {
  const text = String(content || '').trim()
  if (!text) return
  agentChatMessages.value.push({
    id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    role,
    content: text,
  })
  scrollAgentChatToBottom()
}

function setAgentMode(mode) {
  agentConversationIds[agentMode.value] = agentConversationId.value
  agentConversationId.value = agentConversationIds[mode] || ''
  agentStreamError.value = ''
  agentPartialReply.value = ''
  agentMode.value = mode
  syncAgentModeBodyClass()
  forceLive2DVisible()
  if (mode === 'chat') {
    window.Live2dRender?.setMessageBox?.('日常闲聊模式打开啦，路线面板先不动，你可以直接和我聊天。', 4200)
  } else {
    window.Live2dRender?.setMessageBox?.('路线查询模式打开啦，告诉我番组、城市或想去的巡礼点就好。', 4200)
  }
}

function clearAgentGuide() {
  agentKnowledgeSources.value = []
  agentRouteTitle.value = ''
  agentRoutePlan.value = null
  agentSelectedDay.value = 0
  agentGuideText.value = ''
  agentGuideSpots.value = []
  agentGuideActiveSpotKey.value = ''
  if (agentGuideHighlightMarker) {
    agentGuideHighlightMarker.remove()
    agentGuideHighlightMarker = null
  }
}

function resetLive2DAgentSession() {
  agentAbortController?.abort()
  agentConversationIds.route = ''
  agentConversationIds.chat = ''
  agentConversationId.value = ''
  agentRoleInput.value = ''
  agentRoleSending.value = false
  clearAgentWaitingLines()
  clearAgentGuide()
  document.getElementById('agent-route-preview')?.remove()
  document.getElementById('live2dChatPanel')?.remove()
  document.getElementById('chat-history-panel')?.remove()
  scrollAgentChatToBottom()
}

function cleanupAgentGuideText(text) {
  return String(text || '')
    .replace(/!\[[^\]]*]\([^)]+\)/g, '')
    .replace(/^!\[?.*?]\([^)]+\)\s*$/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function formatAgentGuideHtml(text) {
  const lines = String(text || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  return lines
    .map((line) => {
      const safe = escapeHtml(line)
      if (/^Day\s*\d+/i.test(line)) {
        return `<h3>${safe}</h3>`
      }
      if (/^路线[:：]/.test(line)) {
        return `<div class="agent-guide-route">${safe.replace(/^路线[:：]/, '<b>路线</b>：')}</div>`
      }
      if (/^\d+[.、]\s*/.test(line)) {
        return `<div class="agent-guide-stop-title">${safe}</div>`
      }
      if (/^看点[:：]/.test(line)) {
        return `<p class="agent-guide-look">${safe.replace(/^看点[:：]/, '<b>看点</b>：')}</p>`
      }
      if (/当天小结[:：]/.test(line)) {
        return `<p class="agent-guide-summary">${safe.replace(/^当天小结[:：]/, '<b>当天小结</b>：')}</p>`
      }
      return `<p>${safe}</p>`
    })
    .join('')
}

function agentGuideSpotKey(spot, index = 0) {
  const coords = navCoordsToLngLat(spot?.coordinates)
  if (coords) return coords[0].toFixed(6) + ',' + coords[1].toFixed(6)
  return String(spot?.dbPointId || spot?.id || spot?.name || index)
}

function focusAgentGuideSpot(spot, index = 0) {
  const coords = navCoordsToLngLat(spot?.coordinates)
  if (!coords || !map.value) return
  agentGuideActiveSpotKey.value = agentGuideSpotKey(spot, index)
  map.value.flyTo({
    center: coords,
    zoom: Math.max(map.value.getZoom?.() || 0, 16),
    essential: true,
    duration: 1200,
  })

  if (agentGuideHighlightMarker) agentGuideHighlightMarker.remove()
  const el = document.createElement('div')
  el.className = 'agent-guide-map-highlight'
  el.innerHTML = '<span></span>'
  agentGuideHighlightMarker = new mapboxgl.Marker({ element: el, offset: [0, -18] })
    .setLngLat(coords)
    .setPopup(
      new mapboxgl.Popup({ offset: 24, closeButton: false, closeOnClick: true }).setHTML(
        '<strong>' + escapeHtml(spot?.name || '巡礼点') + '</strong><br><span>' + escapeHtml(spot?.workTitle || spot?.address || '') + '</span>',
      ),
    )
    .addTo(map.value)
  agentGuideHighlightMarker.togglePopup()
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function clearAgentWaitingLines() {
  while (agentWaitingTimers.length) {
    window.clearTimeout(agentWaitingTimers.pop())
  }
  if (agentWaitingLoopTimer) {
    window.clearInterval(agentWaitingLoopTimer)
    agentWaitingLoopTimer = null
  }
}

function cancelAgentRequest() {
  agentAbortController?.abort()
}

function newAgentConversation() {
  if (agentRoleSending.value) return
  agentConversationId.value = ''
  agentConversationIds[agentMode.value] = ''
  agentStreamError.value = ''
  agentPartialReply.value = ''
  agentRoleInput.value = ''
  if (agentMode.value === 'route') {
    clearAgentGuide()
    clearRoute({ closePanel: true, resetFields: true })
  } else {
    agentChatMessages.value = []
  }
}

async function sendAgentRoleMessage() {
  const query = agentRoleInput.value.trim()
  if (!query || agentRoleSending.value) return
  const requestMode = agentMode.value
  const requestToken = userStore.token
  const controller = new AbortController()
  agentAbortController = controller
  let timedOut = false
  const timeout = window.setTimeout(() => { timedOut = true; controller.abort() }, 100000)
  agentRoleInput.value = ''
  agentRoleSending.value = true
  agentStreamError.value = ''
  agentPartialReply.value = ''
  agentProgress.value = '正在连接巡礼助手…'
  if (requestMode === 'chat') appendAgentChatMessage('user', query)
  forceLive2DVisible()
  try {
    const contextPlan = requestMode === 'route' && !agentConversationId.value && agentRoutePlan.value
      ? { travelMode: agentRoutePlan.value.travelMode, days: agentRoutePlan.value.days.map(day => ({ title: day.title, note: day.note, pointIds: day.pointIds })) }
      : undefined
    const result = await streamAgentChat({ query, mode: requestMode, contextPlan,
      conversationId: agentConversationId.value || null,
    }, ({ event, data }) => {
      if (controller.signal.aborted || userStore.token !== requestToken) return
      if (event === 'meta' && data.conversationId) {
        agentConversationId.value = data.conversationId
        agentConversationIds[requestMode] = data.conversationId
      }
      if (event === 'status') agentProgress.value = data.text || '正在处理…'
      if (event === 'delta') agentPartialReply.value += data.text || ''
    }, controller.signal, { token: requestToken || undefined, clientId: getOrCreateAcgnClientId() })
    if (controller.signal.aborted || userStore.token !== requestToken) return
    emitLive2DAgentResponse({ ...result, mode: requestMode })
  } catch (error) {
    if (userStore.token !== requestToken) return
    const message = controller.signal.aborted
      ? timedOut ? '等待超时，请缩小范围后重试。' : '已停止生成，原路线保留。'
      : error?.message || '巡礼服务暂时不可用，请稍后重试。'
    agentStreamError.value = message
    agentRoleInput.value = query
    // Incomplete text must never replace the last validated itinerary.
    agentPartialReply.value = ''
  } finally {
    window.clearTimeout(timeout)
    clearAgentWaitingLines()
    if (agentAbortController === controller) {
      agentAbortController = null
      agentRoleSending.value = false
      agentPartialReply.value = ''
    }
  }
}

function requireAgentAccount() {
  if (userStore.token) return true
  userStore.triggerLoginWall('', '登录后即可保存和读取自己的巡礼行程')
  return false
}

async function saveAgentRoute() {
  if (!agentRoutePlan.value || agentSaving.value || !requireAgentAccount()) return
  const token = userStore.token
  agentSaving.value = true
  try {
    const title = agentRouteTitle.value.trim() || `${agentRoutePlan.value.days[0].title} · ${agentRoutePlan.value.days.length}天`
    await saveRoutePlan(title, agentRoutePlan.value)
    if (token === userStore.token) ElMessage.success('主人，行程已经保存到账号了喵~')
  } catch (error) {
    if (token === userStore.token) ElMessage.error(error?.message || '保存失败，请重试')
  } finally { agentSaving.value = false }
}

async function openAgentLibrary() {
  if (!requireAgentAccount()) return
  agentLibraryVisible.value = true
  agentLibraryLoading.value = true
  const token = userStore.token
  try {
    const rows = await listSavedRoutes()
    if (token === userStore.token) agentSavedRoutes.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    if (token === userStore.token) ElMessage.error(error?.message || '读取失败')
  } finally { agentLibraryLoading.value = false }
}

async function restoreAgentRoute(id) {
  if (!requireAgentAccount() || agentRoleSending.value) return
  agentLibraryLoading.value = true
  const token = userStore.token
  try {
    const saved = await loadRoutePlan(id)
    if (token !== userStore.token) return
    setAgentMode('route')
    agentConversationId.value = ''
    agentConversationIds.route = ''
    agentRoutePlan.value = saved.plan
    agentRouteTitle.value = saved.title
    agentKnowledgeSources.value = saved.plan.knowledgeSources || []
    selectAgentDay(0)
    agentLibraryVisible.value = false
    ElMessage.success('行程已读取并重新核对，可以继续修改或导入地图')
  } catch (error) {
    if (token === userStore.token) ElMessage.error(error?.message || '读取失败')
  } finally { agentLibraryLoading.value = false }
}

async function removeAgentRoute(id) {
  if (!requireAgentAccount()) return
  agentLibraryLoading.value = true
  const token = userStore.token
  try {
    await deleteRoutePlan(id)
    if (token === userStore.token) agentSavedRoutes.value = agentSavedRoutes.value.filter(item => item.id !== id)
  } catch (error) {
    if (token === userStore.token) ElMessage.error(error?.message || '删除失败')
  } finally { agentLibraryLoading.value = false }
}

function selectAgentDay(index) {
  if (agentImporting.value) return
  const day = agentRoutePlan.value?.days?.[index]
  if (!day) return
  agentSelectedDay.value = index
  agentGuideSpots.value = (day.spots || []).map(spot => ({ ...spot, sourceType: 'point' }))
  agentGuideText.value = `Day ${day.day} · ${day.title}\n看点：${day.note || ''}`
  agentGuideActiveSpotKey.value = ''
  agentGuideHighlightMarker?.remove()
  agentGuideHighlightMarker = null
}

async function importAgentDay() {
  if (agentImporting.value || agentRoleSending.value) return
  if (!map.value || !map.value.isStyleLoaded()) {
    ElMessage.warning('地图仍在加载，请稍后导入')
    return
  }
  const picks = agentGuideSpots.value
  if (!picks.length) return
  if (picks.length === 1) {
    clearRoute({ closePanel: true, resetFields: true })
    focusAgentGuideSpot(picks[0])
    return
  }
  agentImporting.value = true
  try {
    travelMode.value = agentRoutePlan.value.travelMode
    await applyRoutePicksToNavigation(picks)
  } catch (error) {
    ElMessage.error('导入地图失败，请重试')
  } finally {
    agentImporting.value = false
  }
}

function createWaypointStopEntry() {
  waypointIdCounter.value += 1
  return {
    id: waypointIdCounter.value,
    query: '',
    suggestions: [],
    loading: false,
    selected: null,
    importFilter: '',
  }
}

function addWaypointStop() {
  waypointStops.value.push(createWaypointStopEntry())
}

function removeWaypointStop(index) {
  waypointStops.value.splice(index, 1)
}

const waypointXunliPickList = computed(() => {
  const picks = []
  const seen = new Set()

  const pickStr = (v) => (v == null ? '' : String(v).trim())

  const japanOrCity = (obj) =>
    pickStr(obj?.japanname) || pickStr(obj?.Japanname) || pickStr(obj?.city)

  const add = (placeTitle, addressLine, coords, { imageUrl = '', workTitle = '', dbPointId = '', xunliid = '', sourceType = 'point' } = {}) => {
    if (!coords || coords.length < 2) return
    const lng = Number(coords[0])
    const lat = Number(coords[1])
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return
    const key = `${lng.toFixed(5)},${lat.toFixed(5)}`
    if (seen.has(key)) return
    seen.add(key)
    const name = pickStr(placeTitle) || '站点'
    const wt = pickStr(workTitle)
    picks.push({
      name,
      dbPointId: dbPointId == null ? '' : String(dbPointId),
      xunliid: xunliid == null ? '' : String(xunliid),
      sourceType,
      address: addressLine ? pickStr(addressLine) : '站内巡礼点',
      coordinates: [lng, lat],
      category: '巡礼点',
      imageUrl,
      workTitle: wt,
    })
  }

  const worksSrc =
    Array.isArray(pickerWorksAll.value) && pickerWorksAll.value.length
      ? pickerWorksAll.value
      : xunlilist.value || []
  const pointsSrc =
    Array.isArray(pickerPointsAll.value) && pickerPointsAll.value.length
      ? pickerPointsAll.value
      : xunlipointlist.value || []

  ;[...(worksSrc || []), ...(pointsSrc || [])].forEach((spot) => {
    const pick = agentSpotToPick(spot)
    if (!pick) return
    add(pick.name, pick.address, pick.coordinates, {
      imageUrl: pick.imageUrl,
      workTitle: pick.workTitle,
      dbPointId: pick.dbPointId,
      xunliid: pick.xunliid,
      sourceType: pick.sourceType,
    })
  })

  ;(worksSrc || []).filter((w) => !isAgentSpot(w)).forEach((w) => {
    const workTitle = pickStr(w?.xunliname)
    const city = pickStr(w?.city)
    const jp = japanOrCity(w)
    const primary =
      jp || (city ? `${city} · 主视点` : '') || (workTitle ? `主视点 · ${workTitle}` : '可视主地标')
    const sub = [
      workTitle ? `《${workTitle}》` : '',
      city && !primary.includes(city) ? city : '',
      w.count != null ? `${w.count} 地标` : '',
    ]
      .filter(Boolean)
      .join(' · ')
    add(primary, sub || undefined, w.maingeo, {
      imageUrl: pickStr(w.bcimg) || pickStr(w.hengpic) || '',
      workTitle,
      dbPointId: w.xunliid || w.id,
      xunliid: w.xunliid || w.id,
      sourceType: 'work',
    })
  })

  ;(pointsSrc || []).filter((p) => !isAgentSpot(p)).forEach((p) => {
    const workTitle = pickStr(p.xunliname) || pickStr(p.xunlimainname)
    const landmark =
      pickStr(p.pointXunliname) ||
      pickStr(p.point_xunliname) ||
      pickStr(p.xunlipointname) ||
      pickStr(p.pointname) ||
      ''
    const primary =
      landmark ||
      (pickStr(p.city) ? `${pickStr(p.city)} · 取景点` : '') ||
      workTitle ||
      '巡礼地标'

    const subParts = []
    if (landmark && workTitle) subParts.push(`《${workTitle}》`)
    else if (!landmark && workTitle) subParts.push(`${workTitle}`)
    if (p.ep != null && String(p.ep).trim()) subParts.push(`EP${p.ep}`)
    if (pickStr(p.city)) subParts.push(p.city)
    add(primary, subParts.filter(Boolean).join(' · '), p.geopoint, {
      imageUrl: pickStr(p.xunlipic) || pickStr(p.gaoqingpic) || '',
      workTitle,
      dbPointId: p.id,
      xunliid: p.xunliid,
      sourceType: 'point',
    })
  })

  const cur = xunlibyidlist.value
  if (cur && typeof cur === 'object' && !Array.isArray(cur)) {
    if (Array.isArray(cur.maingeo) && cur.maingeo.length >= 2) {
      const workTitle = pickStr(cur.xunliname)
      const loc =
        japanOrCity(cur) || (pickStr(cur.city) ? `${pickStr(cur.city)} · 主视点` : '')
      const primary =
        loc || (`主视点${workTitle ? ` · ${workTitle}` : ''}`)
      add(
        primary,
        [workTitle ? `《${workTitle}》` : '', pickStr(cur.city)].filter(Boolean).join(' · '),
        cur.maingeo,
        { imageUrl: pickStr(cur.bcimg), workTitle, dbPointId: cur.xunliid || cur.id, xunliid: cur.xunliid || cur.id, sourceType: 'work' },
      )
    }
    const pts = Array.isArray(cur.xunlipoint) ? cur.xunlipoint : []
    pts.forEach((p) => {
      if (!p?.geopoint) return
      const wt = pickStr(cur.xunliname)
      const landmark =
        pickStr(p.pointXunliname) ||
        pickStr(p.point_xunliname) ||
        pickStr(p.xunlipointname) ||
        pickStr(p.xunliname) ||
        '集内地标'
      const sub = [
        wt ? `《${wt}》` : '',
        p.ep != null && String(p.ep).trim() ? `EP${p.ep}` : '',
      ]
        .filter(Boolean)
        .join(' · ')
      add(
        landmark,
        sub || (wt ? `《${wt}》` : undefined),
        p.geopoint,
        {
          imageUrl: pickStr(p.xunlipic) || pickStr(p.gaoqingpic) || '',
          workTitle: wt,
          dbPointId: p.id,
          xunliid: p.xunliid || cur.xunliid,
          sourceType: 'point',
        },
      )
    })
  }

  return picks
})

function filteredWaypointImports(stop) {
  const q = (stop?.importFilter || '').trim().toLowerCase()
  const list = waypointXunliPickList.value
  if (!q) return list
  return list.filter((p) => {
    const blob = `${p.name}\n${p.address}\n${p.workTitle || ''}`.toLowerCase()
    return blob.includes(q)
  })
}

function applyXunliPickToWaypoint(widx, pick) {
  const stop = waypointStops.value[widx]
  if (!stop || !pick?.coordinates) return
  stop.query = pick.name
  stop.selected = { ...pick }
  stop.suggestions = []
}

async function loadWaypointCatalog() {
  if (waypointCatalogLoaded || waypointCatalogLoading.value) return
  waypointCatalogLoading.value = true
  try {
    const [works, points] = await Promise.allSettled([getXunliPickerWorks(), getXunliPickerPoints()])
    if (!map.value) return
    if (works.status === 'fulfilled' && Array.isArray(works.value?.data)) pickerWorksAll.value = works.value.data
    if (points.status === 'fulfilled' && Array.isArray(points.value?.data)) pickerPointsAll.value = points.value.data
    waypointCatalogLoaded = works.status === 'fulfilled' && points.status === 'fulfilled'
  } finally {
    waypointCatalogLoading.value = false
  }
}

async function applyRoutePicksToNavigation(picks) {
  if (!Array.isArray(picks) || picks.length < 2) return

  const cleanPicks = []
  const seenCoords = new Set()
  picks.forEach((pick) => {
    if (!pick?.coordinates) return
    const coords = navCoordsToLngLat(pick.coordinates)
    if (!coords) return
    const key = coords[0].toFixed(5) + ',' + coords[1].toFixed(5)
    if (seenCoords.has(key)) return
    seenCoords.add(key)
    cleanPicks.push({ ...pick, coordinates: coords })
  })

  if (cleanPicks.length < 2) {
    ElMessage.warning('路线已收到，但站内暂时没匹配到足够的可导航坐标')
    return
  }

  if (cleanPicks.length > 25) throw new Error('单次导航地点过多，请按天导入')
  const limitedPicks = cleanPicks
  const start = limitedPicks[0]
  const end = limitedPicks[limitedPicks.length - 1]
  const waypoints = limitedPicks.slice(1, -1)

  clearRoute({ closePanel: false, resetFields: false, restoreMarkers: false })
  agentRouteFocused.value = true
  clearAllMarkers()
  navigation.value = true
  startPoint.value = start.name
  endPoint.value = end.name
  selectedStart.value = start
  selectedEnd.value = end
  endll.value = end.coordinates
  waypointStops.value = waypoints.map((pick) => {
    const stop = createWaypointStopEntry()
    stop.query = pick.name
    stop.selected = pick
    return stop
  })

  await nextTick()
  await calculateRoute({ agentRoute: true, routePicks: limitedPicks })
  ElMessage.success('已导入当天 ' + limitedPicks.length + ' 个地点，正在请求地图路径')
}

function handleLive2DAgentResponse(event) {
  const data = event?.detail || {}
  const reply = cleanupAgentGuideText(data.reply)
  if (data.mode === 'chat') {
    appendAgentChatMessage('assistant', reply || '我在哦。')
    keepLive2DVisible(reply || '我在哦。', { messageDurationMs: 8000 })
    return
  }
  agentKnowledgeSources.value = data.knowledgeSources || data.routePlan?.knowledgeSources || []
  if (data.routePlan?.version === 2 && Array.isArray(data.routePlan.days) && data.routePlan.days.length) {
    agentRoutePlan.value = data.routePlan
    agentRouteTitle.value = `${data.routePlan.days[0].title} · ${data.routePlan.days.length}天`
    selectAgentDay(0)
    keepLive2DVisible('主人，路线核对好了喵~ 可以按天预览，确认后导入地图。', { messageDurationMs: 6000 })
  } else {
    // Clarifying questions remain conversational and cannot move the map.
    agentGuideText.value = reply || '请告诉我想巡礼的作品或城市。'
    keepLive2DVisible(agentGuideText.value, { messageDurationMs: 8000 })
  }
}

function onWaypointInputFocus() {
  clearRoute()
}

async function handleWaypointSearch(index, value) {
  const stop = waypointStops.value[index]
  if (!stop) return
  if (!value) {
    stop.suggestions = []
    stop.selected = null
    return
  }
  stop.loading = true
  try {
    stop.suggestions = await searchPlaces(value)
  } catch (e) {
    console.error(e)
    stop.suggestions = []
  } finally {
    stop.loading = false
  }
}

function onWaypointRowInput(index, value) {
  const stop = waypointStops.value[index]
  if (!stop) return
  stop.query = value
  handleWaypointSearch(index, value)
}

function onWaypointClear(index) {
  const stop = waypointStops.value[index]
  if (!stop) return
  stop.query = ''
  stop.suggestions = []
  stop.selected = null
  stop.importFilter = ''
}

function selectWaypointSuggestion(index, suggestion) {
  const stop = waypointStops.value[index]
  if (!stop) return
  stop.query = suggestion.name
  stop.selected = suggestion
  stop.suggestions = []
}

const handleInput = async (type, value) => {
  if (!value) {
    if (type === 'start') {
      startSuggestions.value = [];
      selectedStart.value = null;
    } else if (type === 'end') {
      endSuggestions.value = [];
      selectedEnd.value = null;
      endll.value = undefined;
    }
    return;
  }

  try {
    if (type === 'start') loadingStart.value = true;
    else loadingEnd.value = true;

    const results = await searchPlaces(value);

    if (type === 'start') {
      startSuggestions.value = results;
      loadingStart.value = false;
    } else {
      endSuggestions.value = results;
      loadingEnd.value = false;
    }
  } catch (error) {
    console.error('搜索地点失败:', error);
    loadingStart.value = false;
    loadingEnd.value = false;
  }
};

const selectSuggestion = (type, suggestion) => {
  if (type === 'start') {
    startPoint.value = suggestion.name;
    selectedStart.value = suggestion;
    startSuggestions.value = [];
  } else {
    endPoint.value = suggestion.name;
    selectedEnd.value = suggestion;
    endll.value = suggestion.coordinates;
    endSuggestions.value = [];
  }
};

// 修改地理编码方法，返回多个结果
const searchPlaces = async (query) => {
  const center = map.value.getCenter();
  const ll = `${center.lat},${center.lng}`;
  
  try {
    const response = await axios.get('https://api.foursquare.com/v3/places/search', {
      params: {
        query: query,
        ll: ll,
        limit: 5,
        v: '20240530',
      },
      headers: {
        Authorization: FOURSQUARE_API_KEY,
        'Accept-Language': 'zh-CN'
      }
    });
    
    return response.data.results.map(result => ({
      name: result.name,
      address: result.location.formatted_address,
      coordinates: [result.geocodes.main.longitude, result.geocodes.main.latitude],
      category: result.categories[0]?.name || '地点'
    }));
  } catch (error) {
    console.error('地点搜索错误:', error);
    return [];
  }
};

function fitDirectionsRouteToMap(directionsCtl, routesPayload) {
  if (!map.value || !Array.isArray(routesPayload) || !routesPayload.length) return;
  let idx = 0;
  try {
    idx = directionsCtl._store?.getState?.()?.routeIndex ?? 0;
  } catch (_) {
    idx = 0;
  }
  const routeFeat = routesPayload[idx] ?? routesPayload[0];
  const geom = routeFeat?.geometry;
  if (!geom) return;

  let path = [];
  if (typeof geom === 'string') {
    try {
      path = decodePolyline(geom, 5).map((pair) => [pair[1], pair[0]]);
    } catch (_) {
      return;
    }
  } else if (geom.type === 'LineString' && Array.isArray(geom.coordinates) && geom.coordinates.length) {
    path = geom.coordinates;
  } else return;

  let b = new mapboxgl.LngLatBounds(path[0], path[0]);
  for (let i = 1; i < path.length; i += 1) b = b.extend(path[i]);

  map.value.fitBounds(b, {
    padding: { top: 88, bottom: 230, left: 72, right: 72 },
    maxZoom: 11.75,
    duration: 1100,
    essential: true,
  });
}

const scheduleFitDirectionsRoute = debounce((directionsCtl, routesPayload) => {
  fitDirectionsRouteToMap(directionsCtl, routesPayload);
}, 320);

// 修改路径计算方法
const calculateRoute = async (options = {}) => {
  const { agentRoute = false, routePicks = [] } = options || {}
  const startCoord = navCoordsToLngLat(selectedStart.value?.coordinates);
  const endCoord =
    navCoordsToLngLat(selectedEnd.value?.coordinates) ??
    navCoordsToLngLat(endll.value);

  if (!startCoord || !endCoord) {
    ElMessage.error('无法解析起终点坐标，请在输入框下拉中点选候选地点');
    return;
  }

  const orphanedWp = waypointStops.value.some(
    (s) => !!(s.query && String(s.query).trim()) && !s.selected?.coordinates,
  );
  if (orphanedWp) {
    ElMessage.warning('途经点必须从下拉候选中选择具体地点后再开始导航');
    return;
  }

  clearRoute({ restoreMarkers: !agentRoute });
  if (agentRoute) {
    agentRouteFocused.value = true
    clearAllMarkers()
  }
  const profile = profileMap[travelMode.value];
  isnavigation.value = true;
  document.body.classList.add('shengdi-navigation-active');
  directions.value = new MapboxDirections({
    accessToken: mapboxgl.accessToken,
    unit: 'metric',
    profile,
    language: 'zh',
    controls: {
      inputs: false,
      instructions: true,
    },
    interactive: false,
  });
  map.value.addControl(directions.value, 'top-right');
  directions.value.on('error', () => {
    ElMessage.warning('地图暂时无法计算这段路径，地点已保留，请调整出行方式或分段查询')
  });
  directions.value.on('route', (evt) => {
    scheduleFitDirectionsRoute(directions.value, evt?.route);
  });

  /** idle + 兜底定时：控件刚挂载时立刻 setOrigin 常会失败 */
  let routed = false;
  const applyOnce = () => {
    if (routed || !directions.value || !map.value) return;
    const ctrl = directions.value;

    const wpCoords = waypointStops.value
      .map((s) => navCoordsToLngLat(s.selected?.coordinates))
      .filter((c) => !!c);

    ctrl.setOrigin(startCoord);
    wpCoords.forEach((lnglat, i) => {
      ctrl.addWaypoint(i, lnglat);
    });
    ctrl.setDestination(endCoord);
    const markerPlaces = routePicks.length
      ? routePicks
      : [
          selectedStart.value,
          ...waypointStops.value.map((s) => s.selected).filter(Boolean),
          selectedEnd.value,
        ]
    addCustomMarkers(startCoord, endCoord, wpCoords, markerPlaces);
    routed = true;
  };
  const fallbackMs = window.setTimeout(applyOnce, 900);
  window.requestAnimationFrame(() => window.setTimeout(applyOnce, 180));
  map.value.once('idle', () => {
    window.clearTimeout(fallbackMs);
    applyOnce();
  });
};
// 新增标记管理方法
const customMarkers = ref([]);

function getRoutePlaceImage(place) {
  return (
    place?.imageUrl ||
    place?.bcimg ||
    place?.xunlipic ||
    place?.gaoqingpic ||
    place?.hengpic ||
    ''
  )
}

function createRouteMarkerElement(place, index, total) {
  const marker = document.createElement('div')
  marker.className = 'piccontainer route-xunli-marker'
  marker.style.cursor = 'pointer'
  marker.style.alignItems = 'center'
  marker.style.display = 'flex'
  marker.style.width = '40px'
  marker.style.height = '50px'
  marker.style.flexDirection = 'column'
  marker.title = place?.name || place?.address || `Point ${index + 1}`

  const point = document.createElement('div')
  point.className = 'marker-point'
  point.style.width = '15px'
  point.style.height = '15px'
  point.style.backgroundColor = index === 0 ? '#4CAF50' : index === total - 1 ? '#F44336' : '#FF9800'
  point.style.borderRadius = '50%'
  point.style.border = '2px solid white'
  point.style.justifyContent = 'center'
  point.style.zIndex = '998'
  point.style.marginTop = '25px'

  const image = document.createElement('div')
  image.className = 'xunlipic route-xunli-pic'
  const imageUrl = getRoutePlaceImage(place)
  if (imageUrl) image.style.backgroundImage = `url("${imageUrl}")`
  image.style.width = '40px'
  image.style.height = '35px'
  image.style.backgroundSize = 'contain'
  image.style.backgroundRepeat = 'no-repeat'
  image.style.backgroundPosition = 'center'
  image.style.justifyContent = 'center'
  image.style.clipPath = 'polygon(0% 0%, 100% 0%, 100% 80%, 60% 80%,50% 100%,40% 80%,0% 80%)'
  image.style.zIndex = '1000'
  image.style.position = 'absolute'

  const badge = document.createElement('div')
  badge.className = 'route-xunli-badge'
  badge.textContent = index === 0 ? 'S' : index === total - 1 ? 'E' : String(index)

  marker.appendChild(image)
  marker.appendChild(point)
  marker.appendChild(badge)
  return marker
}

const addCustomMarkers = (start, end, waypointLngLatList, routePlaces = []) => {
  const coords = [
    start,
    ...(waypointLngLatList || []).filter((c) => Array.isArray(c) && c.length >= 2),
    end,
  ]
  coords.forEach((lnglat, index) => {
    const element = createRouteMarkerElement(routePlaces[index], index, coords.length)
    customMarkers.value.push(
      new mapboxgl.Marker(element).setLngLat(lnglat).addTo(map.value),
    )
  })
};

const removeMarkers = () => {
  customMarkers.value.forEach(marker => marker.remove());
  customMarkers.value = [];
};

// 修改模板中的clearRoute调用
const clearRoute = (options = {}) => {
  const { closePanel = false, resetFields = false, restoreMarkers = true } = options || {};
  scheduleFitDirectionsRoute.cancel?.();
  try {
    directions.value?.removeRoutes?.();
  } catch (_) {
    /* 控件未挂载完成时忽略 */
  }
  if (directions.value && map.value) {
    try {
      map.value.removeControl(directions.value);
    } catch (_) {
      /* 已经被 Mapbox 移除时忽略 */
    }
  }
  directions.value = null;
  isnavigation.value = false;
  removeMarkers();
  if (restoreMarkers) restoreMapMarkers();
  document.body.classList.remove('shengdi-navigation-active');
  if (closePanel) navigation.value = false;
  if (resetFields) {
    startPoint.value = '';
    endPoint.value = '';
    selectedStart.value = null;
    selectedEnd.value = null;
    endll.value = null;
    waypointStops.value = [];
    startSuggestions.value = [];
    endSuggestions.value = [];
  }
};
const init = () => {
  map.value = new mapboxgl.Map({
    container: mapContainer.value,
    style: 'mapbox://styles/mapbox/streets-v11',
    center: [139.7584,35.8104],
    zoom: zoom.value,
    pitch: 0,
    bearing: 0,
    antialias: false,
    essential: true,
    fadeDuration: 0,
    renderWorldCopies: false,
    skipOnboarding: true,
  });
  // map.value.addControl(new NavigationControl(), 'bottom-right');
  // map.value.addControl(new mapboxgl.GeolocateControl(),'bottom-right');
  map.value.addControl(new MapboxLanguage({ defaultLanguage: 'zh-Hans' }));

//当鼠标滚动时，触发事件，获取当前地图缩放级别

  map.value.on('click',async(e) => {
    if(clickdrawer.value === false){
      // clearAllMarkers()
      document.querySelectorAll('.clickjianjie').forEach(marker => marker.remove());
      const data = [e.lngLat.lng, e.lngLat.lat]
      e.lngLat.lng = e.lngLat.lng.toFixed(6)
      e.lngLat.lat = e.lngLat.lat.toFixed(6)
      const result = await getXunliClickPoint(data)
      //逆向地理编码
      const addressResponse = await fetch(
      `https://api.mapbox.com/geocoding/v5/mapbox.places/${e.lngLat.lng},${e.lngLat.lat}.json?` +
      `access_token=${mapboxgl.accessToken}&` +
      `types=country,region,place&` +
      `language=zh`
    );
    const addressData = await addressResponse.json();
    let address
    if (addressData.features && addressData.features.length > 0) { 
       address = addressData.features[0].place_name;
      console.log(address);
    }
      //点击产生的查找最近巡礼点框
      const clickdiv = document.createElement('div')
      clickdiv.className = 'clickjianjie'
      clickdiv.style.width = '240px'
      clickdiv.style.height = '140px'
      clickdiv.style.clipPath = 'polygon(0% 0%, 100% 0%, 100% 90%, 55% 90%,50% 100%,45% 90%,0% 90%)'
      clickdiv.style.backgroundColor = 'rgba(255,255,255)'
      //经纬度
      const clicklatlng = document.createElement('div')
      clicklatlng.style.width = '100%'
      clicklatlng.style.height = '10%'
      clicklatlng.style.padding = '5px'
      clicklatlng.style.fontSize = '13px'
      clicklatlng.style.color = 'rgb(0, 11, 55,0.6)'
      clicklatlng.style.fontWeight = '700'
      clicklatlng.textContent  = `${e.lngLat.lng},${e.lngLat.lat}`
      //第二个盒子
      const clickaddressaddbox = document.createElement('div')
      clickaddressaddbox.style.width = '100%'
      clickaddressaddbox.style.position = 'relative'
      clickaddressaddbox.style.display =  'flex'
      //当前位置
      const clickaddress = document.createElement('div');
      clickaddress.style.position = 'absolute';
      clickaddress.style.width = '60%';
      clickaddress.style.fontSize = '12px';
      clickaddress.style.color = 'rgb(0, 11, 55)';
      clickaddress.style.marginTop = '5px';
      clickaddress.style.padding = '5px'
      clickaddress.style.fontWeight = '700'
      clickaddress.style.left = '0'
      clickaddress.textContent = address;
      //添加地标选项
      const clickadd = document.createElement('div');
      clickadd.style.position = 'absolute';
      clickadd.style.width = '40%';
      clickadd.style.fontSize = '12px';
      clickadd.style.color = 'rgb(0, 125, 203)';
      clickadd.style.marginTop = '5px';
      clickadd.style.padding = '5px'
      clickadd.style.fontWeight = '700'
      clickadd.style.right = '-30px'
      clickadd.textContent = '添加地标';
      clickadd.addEventListener('click',async(event)=>{ 
        event.stopPropagation()
        if(doubleclick.value === false){
          ElNotification({
        title: '提示',
        message: '添加地标前需要把缩放等级放到16级，选择精确坐标后再次点击添加地标',
        type: 'warning',
    })
        map.value.flyTo({
          center:data,
          zoom:16,
          essential: true,
          duration: 6000 // 动画持续时间（毫秒）
    })
      doubleclick.value = true
        }else {
          $router.push({path:'/shengdi/addnew',query:{lng:data[0],lat:data[1]}})
        }
       
      })
      //装图片盒子
      const clickpicbox = document.createElement('div')
      clickpicbox.style.width = '100%'
      clickpicbox.style.height = '55%'
      clickpicbox.style.display = 'flex'
      //三个巡礼点图片及距离
      for(let i=0;i<result.data.length;i++){
        const item = result.data[i]
        console.log(item)
        let distance = 'km'
        if (item.distancemeters < 1000){
           distance = `${item.distancemeters.toFixed(1)}m`
        }
        else{
           distance = `${(item.distancemeters / 1000).toFixed(2)}km`
        }
        const clickclide  = document.createElement('div')
        clickclide.style.minWidth = '40%'
        clickclide.style.height = '100%'
        clickclide.style.bordeeRight= i < result.data.length - 1 
        ? '1px solid #eee' 
        : 'none';
        clickclide.style.position = 'relative'
        clickclide.style.backgroundImage = `url(${item.xunlipic})`
        clickclide.style.backgroundSize = 'cover'
        clickclide.addEventListener('click',async(event)=>{ 
        event.stopPropagation()
        clickjumptoanimate(item.xunliid,item.id)
        navigation.value  = false
        searching.value = false
        })
        //距离
        const clickdistance = document.createElement('div')
        clickdistance.style.position = 'absolute'
        clickdistance.style.bottom = '0'          // 贴紧底部
        clickdistance.style.left = '0'            // 贴紧左侧
        clickdistance.style.padding = '2px 2px'   // 增加内边距
        clickdistance.style.color = 'white'
        clickdistance.style.fontSize = '12px'
        clickdistance.textContent = distance
        clickclide.appendChild(clickdistance)
        clickpicbox.appendChild(clickclide)
      }
      clickaddressaddbox.appendChild(clickadd)
      clickaddressaddbox.appendChild(clickaddress)
      clickdiv.appendChild(clickpicbox)
      clickdiv.appendChild(clicklatlng)
      clickdiv.appendChild(clickaddressaddbox)
      new mapboxgl.Marker({
        element:clickdiv,
        offset: [0, -72]})
      .setLngLat(data)
      .addTo(map.value)
      
    }
    if (tanchuang.value) {
    console.log(tanchuang.value)
    document.querySelectorAll('.xunlijianjie').forEach(marker => marker.remove());
  }
  tanchuang.value = false
})


    debouncedMapViewportUpdate?.cancel?.()
    debouncedMapViewportUpdate = debounce(() => updateData(), 720)
    map.value.on('moveend', debouncedMapViewportUpdate)
  map.value.on('load', () => {
    getxunlipoint()
    // 设置中国世界观过滤器
    const setChinaWorldview = (layerId) => {
      if (map.value.getLayer(layerId)) {
        map.value.setFilter(layerId, [
          "all",
          ["==", ["get", "admin_level"], 0],
          ["==", ["get", "disputed"], "false"],
          ["==", ["get", "maritime"], "false"],
          ["match", ["get", "worldview"], ["all", "CN"], true, false]
        ]);
      }
    };

    // 应用过滤器到所有行政边界图层
    ['admin-0-boundary', 'admin-1-boundary', 'place-label', 'road-label'].forEach(layerId => {
      setChinaWorldview(layerId);
    });

    // 处理争议地区边界
    map.value.setFilter('admin-0-boundary-disputed', [
      "all",
      ["==", ["get", "disputed"], "true"],
      ["match", ["get", "worldview"], ["all", "CN"], true, false]
    ]);
    const geocoderEl = geocoder.onAdd(map.value);
    const geocoderHost = document.getElementById('custom-geocoder-container');
    if (geocoderHost) {
      geocoderHost.appendChild(geocoder.container || geocoderEl);
    }
  });
};
const color = ref('#333')
const getxunlipoint = () => {
  if (!map.value || clickpoint.value) return
  clearAllMarkers()

  const zlvl =
    typeof map.value.getZoom === 'function'
      ? map.value.getZoom()
      : zoom.value ?? 5.7

  let pointRows = Array.isArray(xunlipointlist.value) ? xunlipointlist.value : []
  if (zlvl < 8) {
    pointRows = []
  } else if (zlvl < 10) {
    pointRows = pointRows.slice(0, 90)
  } else if (zlvl < 12) {
    pointRows = pointRows.slice(0, 220)
  } else if (zlvl < 14) {
    pointRows = pointRows.slice(0, 400)
  } else {
    pointRows = pointRows.slice(0, 700)
  }

  let works = Array.isArray(xunlilist.value) ? xunlilist.value : []
  const wcap = zlvl < 6 ? 28 : zlvl < 8 ? 56 : Infinity
  if (works.length > wcap) works = works.slice(0, wcap)

  for (let i = 0; i < pointRows.length; i++) {
    const row = pointRows[i]
    if (!row?.geopoint?.length) continue
    const LngLat1 = [row.geopoint[0], row.geopoint[1]]
    const dotColor = row.xunlicolor || '#333'
    const point = document.createElement('div')
    point.className = 'marker-point'
    point.style.width = '10px'
    point.style.height = '10px'
    point.style.backgroundColor = dotColor
    point.style.borderRadius = '50%'
    point.style.border = '2px solid white'
    const mk = new mapboxgl.Marker(point).setLngLat(LngLat1).addTo(map.value)
    markers.value.push(mk)
  }

  for (let i = 0; i < works.length; i++) {
    const LngLat = [works[i].maingeo[0], works[i].maingeo[1]]
    const markColor = works[i].xunlicolor
    const xunliid = works[i].xunliid
    const container = document.createElement('div')
    container.className = 'marker-container'
    container.style.display = 'flex'
    container.style.flexDirection = 'column'
    container.style.alignItems = 'center'
    container.style.transform = 'translateY(-10%)'
    const el = document.createElement('div')
    el.style.width = '30px'
    el.style.height = '30px'
    el.style.borderRadius = '50%'
    el.style.backgroundImage = `url(${works[i].bcimg})`
    el.style.backgroundSize = 'cover'
    el.style.backgroundPosition = 'center'
    el.style.display = 'flex'
    el.style.justifyContent = 'center'
    el.style.alignItems = 'center'

    const textDiv = document.createElement('div')
    textDiv.textContent = works[i].xunliname
    textDiv.style.marginTop = '30px'
    textDiv.style.whiteSpace = 'normal'
    textDiv.style.width = '100px'
    textDiv.style.wordBreak = 'break-word'
    textDiv.style.textAlign = 'center'
    textDiv.style.fontSize = '12px'
    textDiv.style.color = markColor
    textDiv.style.padding = '2px 6px'
    textDiv.style.borderRadius = '4px'
    textDiv.style.marginTop = '-4px'
    textDiv.style.fontWeight = 'bold'
    textDiv.style.textShadow = '0 1px 2px rgba(0,0,0,0.3)'
    container.addEventListener('click', async (event) => {
      event.stopPropagation()
      searching.value = false
      map.value.flyTo({
        center: LngLat,
        zoom: 16,
        essential: true,
        duration: 6000,
      })
      const result = await getXunliById(xunliid)
      const result2 = await getXunliPointById(xunliid)
      xunlibyidlist.value = result.data
      xunlibyidlist.value.circlecolor = xunlibyidlist.value.xunlicolor
      xunlibyidlist.value.xunlicolor = xunlibyidlist.value.xunlicolor + '12'
      xunlipointbyidlist.value = result2.data
      xunlipointbyidlist.value.xunlicolor = xunlibyidlist.value.circlecolor
      xunlibyidlist.value.xunlipoint = xunlipointbyidlist.value
      xunlipointbyidlist.value.xunlimainname = xunlibyidlist.value.xunliname
      updatepointbyid(xunlipointbyidlist)
      mask(xunlipointbyidlist)
      if (xunlipointbyidlist.value.length > 0) {
        const bounds = new mapboxgl.LngLatBounds()
        xunlipointbyidlist.value.forEach((item) => {
          const lng = item.geopoint[0]
          const lat = item.geopoint[1]
          bounds.extend([lng, lat])
        })
        const mainLng = xunlibyidlist.value.maingeo[0]
        const mainLat = xunlibyidlist.value.maingeo[1]
        bounds.extend([mainLng, mainLat])
        map.value.fitBounds(bounds, {
          padding: { top: 100, bottom: 100, left: 500, right: 100 },
          maxZoom: 16,
          duration: 2000,
        })
      }
      clickdrawer.value = true
    })
    container.appendChild(el)
    container.appendChild(textDiv)

    const mainMk = new mapboxgl.Marker(container).setLngLat(LngLat).addTo(map.value)
    markers.value.push(mainMk)
  }
}
const mask = (xunlipointlist) => {
  const points = []
  var maskempty
  ismask.value = true
  let color = 'rgba(51,51,51,0.7)'
  console.log(xunlipointlist.value)
  if(xunlipointlist.value.xunlicolor){
     color = hexToRgba(xunlipointlist.value.xunlicolor, 0.7)
     console.log(color)
    }

 
  const bianjie = turf.featureCollection([])
  let buffered;
  var mask = turf.polygon([[
  [-180, -90], // 左下角
  [-180, 90],  // 左上角
  [180, 90],   // 右上角
  [180, -90],  // 右下角
  [-180, -90]  // 闭合多边形
  ]]);
  for(let i = 0; i < xunlipointlist.value.length; i++){
    const lng = xunlipointlist.value[i].geopoint[0];  // 经度
    const lat = xunlipointlist.value[i].geopoint[1];  // 纬度
    const point = turf.point([lng, lat]);
    points.push(point)
    bianjie.features.push(point);
  }
  console.log(points)
  if (xunlipointlist.value.length >= 3) {
    // 正常生成凸包并缓冲
    const hull = turf.convex(bianjie);
     maskempty= turf.buffer(hull, 10, { units: 'kilometers' });
  } else {
    for  (let i = 0; i < 3; i++) { 
      const lng = xunlipointlist.value[0].geopoint[0]+i*0.0001;
      const lat = xunlipointlist.value[0].geopoint[1]+i*0.0001;
      const point = turf.point([lng, lat]);
      points.push(point)
      console.log(points)
      bianjie.features.push(point);

    }
    const hull = turf.convex(bianjie);
    maskempty= turf.buffer(hull, 1, { units: 'kilometers' });
  }
  var zhegai = turf.mask(maskempty, mask)
  map.value.addLayer({
    id: 'mask',
    type: 'fill',
    source: {
      type: 'geojson',
      data: zhegai
    },
    paint: {
      'fill-color': color,
      'fill-outline-color': color
    }
  })
  map.value.addLayer({
    id: 'maskline',
    type: 'line',
    source: {
      type: 'geojson',
      data: maskempty,
    },
    paint: {
      'line-color': color,
      'line-width': 4
    }

  })
}
const hexToRgba = (hex, alpha) => {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
};
const updatepointbyid = async (xunlipointlist) => { 
  clickpoint.value = true
  clearAllMarkers()
  for(let i = 0; i < xunlipointlist.value.length; i++){
    xunlipointlist.value[i].xunlimainname = xunlipointlist.value.xunlimainname
    xunlipointlist.value[i].jumpid = i
    const LngLat1 = [xunlipointlist.value[i].geopoint[0], xunlipointlist.value[i].geopoint[1]]
    const color = xunlipointlist.value.xunlicolor || '#333'
    const container = document.createElement('div')
    container.style.cursor = 'pointer'
    container.style.alignItems = 'center';
    container.className = 'piccontainer';
    container.style.display = 'flex';
    container.style.width = '40px';
    container.style.height = '50px';
    container.style.flexDirection = 'column';
    const point = document.createElement('div')
    point.className = 'marker-point';
    point.style.width = '15px';
    point.style.height = '15px';
    point.style.backgroundColor = color;
    point.style.borderRadius = '50%';
    point.style.border = '2px solid white';
    point.style.justifyContent = 'center';  // 修正属性名
    point.style.zIndex = '998'
    point.style.marginTop  = '25px';
    const xunlipic = document.createElement('div')
    xunlipic.className = 'xunlipic';
    xunlipic.style.width = '40px';
    xunlipic.style.height = '35px';
    xunlipic.style.backgroundImage = `url(${xunlipointlist.value[i].xunlipic})`
    xunlipic.style.backgroundSize = 'contain';
    xunlipic.style.justifyContent = 'center';  // 修正属性名
    xunlipic.style.clipPath = 'polygon(0% 0%, 100% 0%, 100% 80%, 60% 80%,50% 100%,40% 80%,0% 80%)';
    xunlipic.style.zIndex = '1000'
    xunlipic.style.position = 'absolute'
    // xunlipic.style.marginBottom = '45px';
    // xunlipic.style.Top = '5px';
    container.appendChild(xunlipic)
    container.appendChild(point)
    container.addEventListener('click', async (event) => {
      event.stopPropagation();
      showjianjie(xunlipointlist.value[i])
    })
    const detailMk = new mapboxgl.Marker(container).setLngLat(LngLat1).addTo(map.value)
    markers.value.push(detailMk)
  }
};

const showjianjie = (xunlipointlist) =>{
  console.log(xunlipointlist)
  lastid.value = xunlipointlist.jumpid
  const lastjianjie = document.getElementById(lastid.value);
  console.log(lastjianjie)
  const LngLat1 = [xunlipointlist.geopoint[0], xunlipointlist.geopoint[1]]
  if(xunlipointlist.ep == null){
    xunlipointlist.ep = '暂未收录'
  }
      document.querySelectorAll('.xunlijianjie').forEach(marker => marker.remove());
    const targetId = xunlipointlist.jumpid;
    const targetElement = document.getElementById(targetId);
    if (targetElement) {
      // 滚动到目标元素
      targetElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      });
      // 添加高亮效果
      targetElement.style.backgroundColor = '#8ca9d440';
      lastid.value = targetId;
    }
      const jianjie = document.createElement('div')
      jianjie.className = 'xunlijianjie';
      jianjie.style.width = '320px'
      jianjie.style.height = '243px'
      jianjie.style.backgroundColor = 'white'
      jianjie.style.zIndex = '1001'
      jianjie.style.display = 'flex';
      jianjie.style.flexDirection = 'column';
      jianjie.style.clipPath = 'polygon(0% 0%, 100% 0%, 100% 97%, 55% 97%,50% 100%,45% 97%,0% 97%)';
      const jianjiepic = document.createElement('div')
      jianjiepic.className = 'xunlijianjiepic';
      jianjiepic.style.width = '320px'
      jianjiepic.style.height = '180px'
      jianjiepic.style.backgroundImage = `url(${xunlipointlist.xunlipic})`
      jianjiepic.style.backgroundSize = 'contain';
      
      const ep = document.createElement('div')
      ep.className = 'xunliep'
      ep.style.position = 'absolute'
      ep.style.bottom = '63px'
      ep.style.padding = '2px 8px'
      ep.style.color = 'white'
      ep.style.fontSize = '12px'
      ep.style.borderRadius = '4px'
      ep.style.zIndex = '9999'
      ep.style.left = '2px'
      ep.textContent =  `EP${xunlipointlist.ep}`
      const timestamp = document.createElement('div')
      timestamp.className = 'xunlitimestamp'
      timestamp.style.position = 'absolute'
      timestamp.style.bottom = '63px'
      timestamp.style.padding = '2px 8px'
      timestamp.style.color = 'white'
      timestamp.style.fontSize = '12px'
      timestamp.style.borderRadius = '4px'
      timestamp.style.zIndex = '9999'
      timestamp.style.right = '2px'
      timestamp.textContent =  changetime(xunlipointlist.time)
      const daohang = document.createElement('div')
      daohang.style.position = 'absolute'; // 添加绝对定位
      daohang.style.bottom = '10px'; // 调整位置
      daohang.style.right = '10px';
      daohang.style.cursor = 'pointer'; // 手型光标
      daohang.style.color = 'rgb(0, 125, 203)'
      daohang.textContent = '导航前往'
      daohang.addEventListener('click', () => {
        navigation.value = true;
        waypointStops.value = [];
        startPoint.value = null;
        endPoint.value = xunlipointlist.xunliname;
        const coords = [...LngLat1];
        endll.value = coords;
        selectedEnd.value = {
          name: xunlipointlist.xunliname,
          address: xunlipointlist.address || '',
          coordinates: coords,
        };
      });
      const jianjietext = document.createElement('div')
      jianjietext.className = 'xunlijianjietext';
      jianjietext.style.width = '320px'
      jianjietext.style.height = '63px'
      jianjietext.style.display = 'flex';
      jianjietext.style.flexDirection = 'column';
      jianjietext.style.padding = '4px 7px 4px 7px'
      const jianjietext1 = document.createElement('div')
      jianjietext1.className = 'xunlijianjietext1';
      jianjietext1.style.width  = '260px'
      jianjietext1.style.height = '20px'
      jianjietext1.style.fontSize = '14px'
      jianjietext1.style.fontWeight = '700'
      jianjietext1.style.fontFamily = 'font-family "Helvetica Neue", Arial, Helvetica, sans-serif'
      jianjietext1.style.marginTop = '3px'
      jianjietext1.style.marginBottom = '5px'
      jianjietext1.textContent = xunlipointlist.xunliname
      const duizhao = document.createElement('div')
      duizhao.className = 'xunlijianjietext3';
      duizhao.style.position = 'absolute'; 
      duizhao.style.height = '20px'
      duizhao.style.bottom = '37px'; // 调整位置
      duizhao.style.right = '10px';
      duizhao.style.cursor = 'pointer'; // 手型光标
      duizhao.style.color = 'rgb(0, 125, 203)'
      duizhao.textContent = '拍摄对比'
      duizhao.addEventListener('click', () => { 
        if (map.value) {
          map.value.remove();
          map.value = null;
        }
        $router.push({ path:'/shengdi/duizhao'})
        userStore.xunlilist = xunlipointlist
      });
      const jianjietext2 = document.createElement('div')
      jianjietext2.className = 'xunlijianjietext2';
      jianjietext2.style.width  = '320px'
      jianjietext2.style.height = '20px'
      jianjietext2.style.fontWeight = '400'
      jianjietext2.style.fontFamily = 'Helvetica Neue", Arial, Helvetica, sans-serif'
      jianjietext2.style.fontSize = '12px'
      jianjietext2.style.color = 'rgb(0, 125, 203)'
      jianjietext2.textContent = xunlipointlist.xunlimainname
      
      jianjiepic.appendChild(ep)
      jianjiepic.appendChild(timestamp)
      jianjietext.appendChild(jianjietext1)
      jianjietext.appendChild(duizhao)
      jianjietext.appendChild(jianjietext2)
      jianjietext.appendChild(daohang)
      jianjie.appendChild(jianjiepic)
      jianjie.appendChild(jianjietext)
      const jjMk = new mapboxgl.Marker({
        element: jianjie,
        offset: [0, -110],
      })
        .setLngLat(LngLat1)
        .addTo(map.value)
      markers.value.push(jjMk)
      tanchuang.value = true
}
const changetime = (time) =>{
  if (isNaN(time)) return '00:00:00';
  
  const hours = Math.floor(time / 3600);
  const minutes = Math.floor((time % 3600) / 60);
  const secs = time % 60;
  
  return [hours, minutes, secs]
    .map(v => v.toString().padStart(2, '0'))
    .join(':');
}
watch(() => userStore.token, () => {
  resetLive2DAgentSession()
  agentStreamError.value = ''
  agentPartialReply.value = ''
  agentChatMessages.value = []
  agentSavedRoutes.value = []
  agentLibraryVisible.value = false
  clearRoute({ closePanel: true, resetFields: true })
})

onMounted(async () => {
  await nextTick();
  resetLive2DAgentSession()
  syncAgentModeBodyClass()
  window.addEventListener('beforeunload', resetLive2DAgentSession)
  window.addEventListener('live2d-agent-response', handleLive2DAgentResponse);
  document.addEventListener('click', handleLive2DChatActivity, true);
  document.addEventListener('focusin', handleLive2DChatActivity, true);
  mountShengdiLive2D().catch((error) => {
    console.warn('Live2D 加载失败:', error);
  }).finally(() => {
    window.setTimeout(() => {
      forceLive2DVisibleRepeatedly()
      window.Live2dRender?.setMessageBox?.('主人，想巡礼哪部番组都可以告诉我喵~', 5000)
    }, 600)
  });

  // AI returns enriched points; full manual-import data is fetched only when its popover opens.
  init();
  const initialViewportGen = mapViewportUpdateGen;
  const [works, points] = await Promise.allSettled([getXunli(zoom.value), getXunliPoint(zoom.value)]);
  if (!map.value || initialViewportGen !== mapViewportUpdateGen) return;
  xunlilist.value = works.status === 'fulfilled' && Array.isArray(works.value?.data) ? works.value.data : [];
  xunlipointlist.value = points.status === 'fulfilled' && Array.isArray(points.value?.data) ? points.value.data : [];
  if (map.value.isStyleLoaded()) getxunlipoint();
});

onBeforeUnmount(() => {
  agentAbortController?.abort()
  window.removeEventListener('beforeunload', resetLive2DAgentSession)
  window.removeEventListener('live2d-agent-response', handleLive2DAgentResponse)
  document.removeEventListener('click', handleLive2DChatActivity, true)
  document.removeEventListener('focusin', handleLive2DChatActivity, true)
  clearAgentWaitingLines()
  if (agentGuideHighlightMarker) {
    agentGuideHighlightMarker.remove()
    agentGuideHighlightMarker = null
  }
  cleanupLive2D()
  debouncedMapViewportUpdate?.cancel?.()
  debouncedMapViewportUpdate = null
  scheduleFitDirectionsRoute.cancel?.()
  document.body.classList.remove('shengdi-navigation-active')
  document.body.classList.remove('shengdi-live2d-chat-mode')
  if (map.value) map.value.remove()
  map.value = null
});
</script>

<style scoped>
.live2d-role-console {
  position: fixed;
  right: 24px;
  bottom: 18px;
  z-index: 10030;
  display: flex;
  align-items: center;
  gap: 8px;
  width: min(520px, calc(100vw - 32px));
  padding: 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 24px rgba(47, 57, 84, 0.18);
  backdrop-filter: blur(12px);
}

.live2d-role-console--chat {
  width: min(390px, calc(100vw - 32px));
  min-height: 600px;
  align-items: stretch;
  flex-direction: column;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.92)),
    linear-gradient(135deg, rgba(255, 127, 176, 0.12), rgba(122, 167, 255, 0.16));
  overflow: hidden;
}

.live2d-mode-switch {
  flex: 0 0 auto;
  display: inline-grid;
  grid-template-columns: 1fr 1fr;
  width: 164px;
  height: 34px;
  padding: 2px;
  border-radius: 8px;
  background: rgba(244, 247, 255, 0.96);
  border: 1px solid rgba(122, 167, 255, 0.24);
}

.live2d-mode-switch button {
  min-width: 0;
  border: 0;
  border-radius: 6px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
  background: transparent;
  cursor: pointer;
}

.live2d-mode-switch button.active {
  color: #fff;
  background: linear-gradient(135deg, #ff7fb0, #7aa7ff);
  box-shadow: 0 5px 14px rgba(122, 167, 255, 0.26);
}

.live2d-mode-switch button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.live2d-chat-stage {
  position: relative;
  height: 250px;
  margin-top: 2px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background:
    linear-gradient(180deg, rgba(244, 248, 255, 0.8), rgba(255, 239, 248, 0.68)),
    linear-gradient(90deg, rgba(112, 130, 170, 0.08) 1px, transparent 1px),
    linear-gradient(180deg, rgba(112, 130, 170, 0.08) 1px, transparent 1px);
  background-size: auto, 22px 22px, 22px 22px;
}

.live2d-chat-stage-title {
  position: absolute;
  left: 12px;
  top: 10px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #334155;
  pointer-events: none;
}

.live2d-chat-stage-title strong {
  font-size: 14px;
  line-height: 1.2;
}

.live2d-chat-stage-title span {
  font-size: 12px;
  color: #748095;
}

.live2d-chat-messages {
  flex: 1;
  min-height: 0;
  max-height: 220px;
  padding: 2px 2px 8px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.live2d-chat-message {
  width: fit-content;
  max-width: 82%;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.45;
  color: #334155;
  word-break: break-word;
  box-shadow: 0 8px 20px rgba(47, 57, 84, 0.08);
}

.live2d-chat-message--assistant {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(122, 167, 255, 0.16);
}

.live2d-chat-message--user {
  align-self: flex-end;
  color: #fff;
  background: linear-gradient(135deg, #ff7fb0, #7aa7ff);
}

.live2d-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.live2d-role-console:not(.live2d-role-console--chat) .live2d-input-row {
  flex: 1;
}

.live2d-role-input {
  flex: 1;
  min-width: 0;
  height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(255, 149, 188, 0.45);
  border-radius: 999px;
  outline: none;
  color: #3f4154;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.96);
}

.live2d-role-input:focus {
  border-color: #ff7fb0;
  box-shadow: 0 0 0 3px rgba(255, 149, 188, 0.18);
}

.live2d-role-send {
  flex: 0 0 auto;
  height: 34px;
  min-width: 64px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff7fb0, #7aa7ff);
  cursor: pointer;
}

.live2d-role-send:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

:global(body.shengdi-live2d-chat-mode #live2d) {
  right: 96px !important;
  bottom: 320px !important;
  width: 220px !important;
  height: 330px !important;
  z-index: 10031 !important;
  pointer-events: auto !important;
}

:global(body.shengdi-live2d-chat-mode #live2dMessageBox) {
  right: 48px !important;
  bottom: 298px !important;
  width: min(320px, calc(100vw - 64px)) !important;
  z-index: 10032 !important;
}

:global(body.shengdi-live2d-chat-mode #live2d-toolbox) {
  display: none !important;
}

.agent-guide-panel {
  position: fixed;
  left: 24px;
  top: 92px;
  z-index: 10020;
  width: min(540px, calc(100vw - 48px));
  max-height: calc(100vh - 132px);
  padding: 0;
  border-radius: 8px;
  background-color: rgba(248, 251, 255, 0.88);
  background-image:
    linear-gradient(90deg, rgba(255,255,255,0.45), rgba(255,255,255,0.12)),
    url('/svg/agent-guide-bg.svg'),
    linear-gradient(135deg, rgba(255, 126, 170, 0.16), rgba(122, 167, 255, 0.13)),
    radial-gradient(circle at 12% 18%, rgba(255, 191, 216, 0.42), transparent 30%),
    radial-gradient(circle at 92% 4%, rgba(125, 166, 255, 0.25), transparent 28%),
    linear-gradient(rgba(93, 106, 136, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(93, 106, 136, 0.055) 1px, transparent 1px);
  background-size: auto, cover, auto, auto, auto, 22px 22px, 22px 22px;
  background-position: center;
  box-shadow: 0 18px 48px rgba(38, 49, 72, 0.22);
  backdrop-filter: blur(14px);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.78);
}

.agent-guide-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, rgba(255,255,255,0.24), rgba(255,255,255,0.08)),
    repeating-linear-gradient(135deg, rgba(255,255,255,0.16) 0 8px, transparent 8px 18px);
}

.agent-guide-panel > * {
  position: relative;
  z-index: 1;
}

.agent-guide-head {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 14px 10px;
  background: linear-gradient(135deg, rgba(255, 127, 176, 0.9), rgba(122, 167, 255, 0.86));
  border-bottom: 1px solid rgba(255, 255, 255, 0.46);
}

.agent-guide-head strong {
  display: block;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0;
}

.agent-guide-head span {
  display: block;
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 12px;
}

.agent-guide-close {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  color: #ff7fb0;
  background: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 127, 176, 0.28);
}

.agent-guide-spots {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  max-height: 230px;
  padding: 12px 14px;
  overflow: auto;
  border-bottom: 1px solid rgba(228, 232, 242, 0.76);
  background: rgba(255, 255, 255, 0.28);
}

.agent-guide-spot {
  display: grid;
  grid-template-columns: 58px 1fr;
  gap: 9px;
  align-items: center;
  min-height: 58px;
  padding: 6px;
  border: 1px solid rgba(226, 231, 244, 0.9);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 2px 8px rgba(39, 49, 75, 0.06);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.agent-guide-spot:hover,
.agent-guide-spot.active {
  transform: translateY(-1px);
  border-color: rgba(255, 127, 176, 0.72);
  box-shadow: 0 8px 18px rgba(255, 127, 176, 0.16);
}

.agent-guide-spot img,
.agent-guide-spot-placeholder {
  width: 58px;
  height: 58px;
  border-radius: 6px;
  object-fit: cover;
  background: linear-gradient(135deg, #eef1f6, #dde3ee);
}

.agent-guide-spot b {
  display: block;
  color: #303446;
  font-size: 13px;
  line-height: 1.35;
}

.agent-guide-spot span {
  display: block;
  margin-top: 2px;
  color: #7f8496;
  font-size: 12px;
  line-height: 1.35;
}

.agent-guide-text {
  max-height: 390px;
  margin: 0;
  padding: 14px;
  overflow: auto;
  color: #34394f;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  background: rgba(255, 255, 255, 0.18);
}

.agent-guide-text :deep(h3) {
  margin: 14px 0 8px;
  padding: 8px 10px;
  border-radius: 7px;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  background: linear-gradient(135deg, rgba(48, 58, 92, 0.9), rgba(122, 167, 255, 0.72));
  box-shadow: 0 8px 18px rgba(48, 58, 92, 0.14);
}

.agent-guide-text :deep(h3:first-child) {
  margin-top: 0;
}

.agent-guide-text :deep(p) {
  margin: 7px 0;
}

.agent-guide-text :deep(.agent-guide-route),
.agent-guide-text :deep(.agent-guide-look),
.agent-guide-text :deep(.agent-guide-summary) {
  margin: 8px 0;
  padding: 9px 10px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(226, 231, 244, 0.72);
}

.agent-guide-text :deep(.agent-guide-route) {
  color: #56607a;
  background: rgba(255, 255, 255, 0.8);
}

.agent-guide-text :deep(.agent-guide-stop-title) {
  margin: 10px 0 5px;
  padding-left: 10px;
  color: #2f3449;
  font-weight: 800;
  border-left: 3px solid #ff7fb0;
}

.agent-guide-text :deep(.agent-guide-look b),
.agent-guide-text :deep(.agent-guide-route b),
.agent-guide-text :deep(.agent-guide-summary b) {
  color: #ff669c;
}

.agent-guide-text :deep(.agent-guide-summary) {
  background: linear-gradient(135deg, rgba(255, 236, 245, 0.82), rgba(237, 244, 255, 0.82));
}

:global(#live2dMessageBox) {
  right: 18px !important;
  left: auto !important;
  top: auto !important;
  bottom: 405px !important;
  width: 320px !important;
  height: auto !important;
  justify-content: flex-end !important;
  pointer-events: none;
}

:global(#live2dMessageBox-content) {
  max-width: 300px;
  padding: 10px 12px;
  border-radius: 10px;
  line-height: 1.5;
  box-shadow: 0 8px 24px rgba(42, 50, 80, 0.18);
}

:global(#live2d) {
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  pointer-events: auto !important;
  z-index: 9999 !important;
}

:global(.agent-guide-map-highlight) {
  position: relative;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 127, 176, 0.24);
  border: 2px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 6px rgba(255, 127, 176, 0.18), 0 10px 22px rgba(42, 48, 72, 0.22);
}

:global(.agent-guide-map-highlight span) {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff7fb0, #7aa7ff);
  transform: translate(-50%, -50%);
}

.navigation-controls {
  position: fixed;
  top: 88px;
  right: 22px;
  z-index: 12020;
  background: rgba(255, 255, 255, 0.94);
  padding: 14px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(14px);
  width: 340px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.nav-field {
  position: relative;
}

.nav-input:not(.wp-input-flex) {
  margin-bottom: 12px;
}

.wp-row .wp-input-flex {
  margin-bottom: 0;
}

.wp-add-row {
  display: flex;
  justify-content: center;
  margin: 2px 0 6px;
}

.wp-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.wp-input-flex {
  flex: 1;
  min-width: 0;
}

.wp-remove-btn {
  flex-shrink: 0;
  margin-top: 4px;
}

.nav-input {
  width: 100%;
}

.nav-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

:deep(.mapbox-directions-component) {
  width: 100%;
  font-family: 'Helvetica Neue', Arial, sans-serif;
}

:deep(.mapbox-directions-route-summary) {
  padding: 12px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  margin-top: 10px;
}

:deep(.mapbox-directions-instructions) {
  max-height: 60vh;
  overflow-y: auto;
}

:global(body.shengdi-navigation-active #live2d) {
  right: 18px !important;
  bottom: 0 !important;
  left: auto !important;
  top: auto !important;
}

:global(body.shengdi-navigation-active #live2d-toolbox) {
  right: 18px !important;
}

:global(body.shengdi-navigation-active #live2dMessageBox) {
  right: 18px !important;
  bottom: 405px !important;
  left: auto !important;
  top: auto !important;
}

:global(body.shengdi-navigation-active #live2dChatPanel) {
  right: 744px !important;
  left: auto !important;
}

:global(.mapboxgl-ctrl-top-right .mapbox-directions-component) {
  margin-top: 78px;
  margin-right: 18px;
  width: min(380px, calc(100vw - 36px));
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 16px 42px rgba(15, 23, 42, 0.16);
  border: 1px solid rgba(148, 163, 184, 0.28);
}

:global(.mapboxgl-ctrl-top-right .mapbox-directions-instructions) {
  max-height: min(46vh, 420px);
  margin-top: 0;
}
.point-picturer{
  position: relative; /* 添加相对定位 */
  width: 45%;
  height: 110px;
  flex: 0 0 45%;
}
.ep, .timestamp {
  position: absolute;
  bottom: 8px; /* 定位到底部 */
  padding: 2px 8px;
  color: white;
  font-size: 12px;
  border-radius: 4px;
  z-index: 9999;
}
.xunlipic{
  clip-path: polygon(
    0 0,          /* 左上角 */
    100% 0,       /* 右上角 */
    100% 85%,     /* 右下角 */
    50% 100%,     /* 箭头顶点 */
    0 85%         /* 左下角 */
  );
}
.ep {
  left: 2px;
}
.timestamp {
  right: 2px;
}
.point-picture{
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pointmain:hover {
  background: #8ca9d440;
}
.point{
  width: 100%;
  height: 75%;
  overflow-y: scroll;
  -ms-overflow-style: none; /* 适用于 Internet Explorer 和旧版 Edge */
  scrollbar-width: none; /* 适用于 Firefox */
}
.pointmain{
  width: 100%;
  display: flex;
  cursor: pointer
}

:deep(.el-drawer__body) {
  padding: 0 !important; /* 移除默认 padding */
  padding-top: 10px !important;
}
.dot-divider {
  margin: 0 6px;
  color: #999;
  font-weight: bold;
}
.jianjie-content{
  margin-left: 20px;
  width: 70%;
}
.jianjie-small{
  font-size: 11.6667px;
  color: rgb(0, 11, 55);
  font-family: 'sans-serif';
}
.jianjie-circle{
  margin-left: 15%;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 3px white solid;
}
.jianjie-title-div{
  display: flex;
  width: 100%;
  align-items: center;
}
.jianjie-title{
  font-size: 18px;
  width: 70%;
  height: 15%;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  color: rgb(0, 11, 55);
  font-family: 'sans-serif';
  font-weight: 700;
}
.jianjie{
  display: flex;
  height: 20%;
  width: 100%;
}
.searchjianjie{
  display: flex;
  height: 20%;
  width: 100%;
}
.custom-search-container{
  position: fixed;
  top: 88px;
  left: 22px;
  z-index: 9999;
  width: 360px;
  max-width: calc(100vw - 44px);
  transition: all 0.3s ease;
  transform: translateZ(0);
}
.search-result{
  position: relative;
  background-color: white;
  width: 115%;
  height: 660%;
  z-index: 998;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(4px);
  left: -10%;
  top: -60%;
}
.search_city {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  font-size: 13px;
}
.search_city span {
  font-family: 'Noto Sans SC';
  cursor: pointer;
  transition: all 0.2s;
  color: black
}

.search_city span:hover {
  background:rgb(187, 251, 255,0.5);
  color: #333;
  border-color: #ddd;
  transform: translateY(-1px);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .search_city {
    gap: 6px;
    padding: 8px;
  }
  
  .search_city span {
    padding: 3px 10px;
    font-size: 12px;
  }
}
.search_leibie_result{
  max-height: 500px;
  background: rgba(255, 255, 255, 0.96);
  overflow-y: auto;
}
.searchitem{
  min-height: 64px;
  padding: 8px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  transition: all 0.2s ease;
  cursor: pointer;
}
.searchitem:hover {
  background: rgba(219, 234, 254, 0.75);
  transform: translateX(2px);
}
.searchitem:last-child {
  border-bottom: none;
}
.searchdata {
  margin-left: 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: calc(100% - 62px);
}
.searchname {
  max-width: 222px;
  font-size: 14px;
  font-weight: 800;
  color: #1a1a1a;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: "Noto Sans SC";
}
.searchinfo {
  font-size: 12px;
  color: #666;
  display: flex;
  gap: 6px;
}
.searchinfo span {
  display: flex;
  align-items: center;
  font-weight: 500;
}

.searchinfo span:not(:last-child)::after {
  content: "·";
  margin-left: 6px;
  color: #999;
}
.search-circle{
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 3px white solid;
}
.searchimg{
  width: 52px;
  height: 52px;
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  border-radius: 8px;
}
.search_leibie {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.search_leibie span {
  cursor: pointer;
  padding: 6px 16px;
  color: #666;
  font-size: 13px;
  transition: all 0.3s ease;
  border-radius: 999px;
}

.search_leibie span:hover {
  background: #e0f0ff;
  color: #007bff;
  transform: translateY(-1px);
}

.search_leibie span.active {
  background: #007bff;
  color: white;
  border-color: #0056b3;
  box-shadow: 0 2px 6px rgba(0,123,255,0.3);
}
.search_leibie span.active2 {
  background: #007bff;
  color: white;
  border-color: #0056b3;
  box-shadow: 0 2px 6px rgba(0,123,255,0.3);
}
.search_leibie span.active3 {
  background: #007bff;
  color: white;
  border-color: #0056b3;
  box-shadow: 0 2px 6px rgba(0,123,255,0.3);
}
/* 移动端适配 */
@media (max-width: 768px) {
  .search_leibie {
    gap: 8px;
    padding: 8px 12px;
  }
  
  .search_leibie span {
    padding: 4px 12px;
    font-size: 12px;
  }
}
.search-result-container{
  margin-top: 12px;
  background-color: rgba(255, 255, 255, 0.94);
  width: 100%;
  max-height: min(550px, calc(100vh - 180px));
  z-index: 9999;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 10px;
  overflow: hidden;
}
.search-box {
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(14px);
  z-index: 9999;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 10px;
  overflow: hidden;
}
.search-icon {
  color: #666;
  font-size: 16px;
  margin-left: 8px;
  transition: color 0.3s ease;
}
.map-search-input{
  border: 0;
}
:deep(.map-search-input:hover .search-icon) {
  color: var(--el-color-primary);
}
:deep(.map-search-input .el-input__inner:hover),
:deep(.map-search-input .el-input__inner:focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.search-result-out{
  position: fixed;
  width: 24%;
  height: 90%;
  background-color: white;
  top: 10%;
  z-index: 9998;
}
.mainresult{
  width: 100%;
  margin-top: 22%;
  height: 90%;
  overflow: auto;
}
.back {
  color: white;
  font-size: 14px;
  display: flex;
  position: fixed;
  top: 13%;
  left: 26%;
  width: 104px;
  height: 36px;
  background-color: rgba(0, 119, 255);
  z-index: 9999;
  border-radius: 50%;
  padding: 8px 14px;
  justify-content: center;
  font-family: sans-serif;
  align-items: center;
}
.marker-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  transform: translateY(-50%); /* 保持标记中心点正确 */
}

:global(.route-xunli-marker) {
  pointer-events: auto;
}

:global(.route-xunli-badge) {
  position: absolute;
  right: -8px;
  top: -8px;
  z-index: 1001;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 999px;
  background: #111827;
  border: 2px solid #fff;
  color: #fff;
  font-size: 12px;
  line-height: 16px;
  font-weight: 700;
  text-align: center;
}

:global(.route-photo-marker) {
  position: relative;
  width: 96px;
  height: 76px;
  cursor: pointer;
  filter: drop-shadow(0 10px 18px rgba(15, 23, 42, 0.24));
  contain: layout paint;
}

:global(.route-photo-marker-pin) {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 64px;
  height: 74px;
  transform: translateX(-50%);
}

:global(.route-photo-marker-pin::after) {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 14px;
  height: 14px;
  background: #fff;
  border-right: 3px solid #fff;
  border-bottom: 3px solid #fff;
  transform: translateX(-50%) rotate(45deg);
  box-shadow: 6px 6px 14px rgba(15, 23, 42, 0.16);
}

:global(.route-photo-marker-image) {
  position: absolute;
  left: 50%;
  top: 0;
  z-index: 1;
  width: 64px;
  height: 64px;
  border: 3px solid #fff;
  border-radius: 12px;
  background: #eef2ff center / cover no-repeat;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.22);
  transform: translateX(-50%);
  overflow: hidden;
}

:global(.route-photo-marker-image-empty::after) {
  content: '';
  display: block;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f43f5e, #22c55e);
}

:global(.route-photo-marker-badge) {
  position: absolute;
  top: -9px;
  right: -7px;
  z-index: 2;
  min-width: 22px;
  height: 22px;
  padding: 0 5px;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  border: 2px solid #fff;
  font-size: 12px;
  line-height: 18px;
  font-weight: 700;
  text-align: center;
}

:global(.route-photo-marker-label) {
  position: absolute;
  left: 50%;
  top: 78px;
  width: 94px;
  transform: translateX(-50%);
  padding: 3px 7px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.94);
  color: #1f2937;
  font-size: 12px;
  line-height: 1.25;
  font-weight: 700;
  text-align: center;
  word-break: break-word;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.16);
}

.marker-image {
  width: 35px;
  height: 35px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  border: 2px solid white;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  border: 2px solid red !important;
}

.marker-text {
  white-space: nowrap;
  font-size: 12px;
  color: #333;
  background: rgba(255,255,255,0.9);
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  position: relative;
  z-index: 1;
}

/* 添加小箭头 */
.marker-text::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 8px solid rgba(255,255,255,0.9);
}
.custom-drawers {
  margin-top: 13%;
}

.drawer-container {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sub-title {
  color: #666;
  font-size: 14px;
  margin: 0;
  line-height: 1.5;
}

.search-input {
  :deep(.el-input__inner) {
    border-radius: 20px;
    padding-left: 40px;
    height: 40px;
  }
  
  :deep(.el-input__prefix) {
    left: 12px;
    display: flex;
    align-items: center;
  }
}

.recommend-title {
  color: #333;
  font-size: 15px;
  margin: 0 0 12px 0;
  font-weight: 500;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommend-item {
  width: 100%;
  height: 35%;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  color: #444;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #eee;

  &:hover {
    background-color: #f1f3f5;
    transform: translateX(4px);
    border-color: #e0e0e0;
  }

  &:active {
    background: #e9ecef;
  }
}
.circle {
  width: 25px;
  height: 50px;
  border-radius: 25px 0 0 25px;
  position: absolute;
  top: 50%;
  left: 96.8%;
  transform: translateY(-50%) translateX(100%);
  background-color: white;
  display: flex;
  align-items: center;
  cursor: pointer;
  z-index: 1000; /* 确保高于地图层级 */
}

.map-container {
  position: fixed;
  width: 100%;
  height: 100vh;
}
#custom-geocoder-container {
display: none;
}
#map {
  width: 100%;
  height: 100vh;
}
:deep(.mask-layer){
  width: 25%;
}
.inputDeep>>>.el-input__inner {
    border: 0;
  }
/* 添加自定义样式，确保搜索框在右上角 */
.mapboxgl-ctrl-top-right{
  position: absolute !important;
  top: 10px !important;
  right: 10px !important;
  z-index: 1000;
}
.suggestion-dropdown {
  position: absolute;
  width: 100%;
  max-height: 200px;
  overflow-y: auto;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 5000;
  top: 100%;
  left: 0;
  margin-top: 4px;
}

.wp-nav-field-expanded {
  z-index: 20;
}

.wp-import-ref {
  flex-shrink: 0;
  margin-top: 4px;
}

.wp-import-pane {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wp-import-header {
  font-size: 13px;
  line-height: 1.45;
  color: #606266;
}

.wp-import-header strong {
  color: #303133;
}

.wp-import-header-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.wp-import-filter {
  width: 100%;
}

.wp-import-scroll {
  min-height: 0;
}

.wp-import-item {
  padding: 6px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.wp-import-thumb-wrap {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
}

.wp-import-thumb {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.wp-import-thumb-ph {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ebeef5 0%, #dcdfe6 100%);
}

.wp-import-item-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.wp-import-item:hover {
  background: #f5f7fa;
}

.wp-import-name {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  line-height: 1.35;
  word-break: break-word;
}

.wp-import-work {
  font-size: 11px;
  color: #c45656;
  line-height: 1.35;
}

.wp-import-sub {
  font-size: 12px;
  color: #909399;
  line-height: 1.3;
  word-break: break-word;
}

.wp-import-empty {
  padding: 12px 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.suggestion-item {
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.3s;
}

.suggestion-item:hover {
  background: #f5f7fa;
}

.suggestion-name {
  font-weight: 600;
  font-size: 14px;
}

.suggestion-address {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.mapbox-directions-instructions{
  margin-top: 0;
}
.travel-mode-selector {
  height: 36px;
  display: flex;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
}

.mode-button {
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 0px 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-right: 1px solid #dcdfe6;
  gap: 4px;
}

.mode-button:last-child {
  border-right: none;
}

.mode-button:hover {
  background-color: #e1f0ff;
}

.mode-button.active {
  background-color: var(--el-color-primary);
  color: white;
}

.mode-button.active .el-icon {
  color: white;
}

.mode-button .el-icon {
  font-size: 16px;
  margin-bottom: 5px;
  color: #606266;
}

.mode-button span {
  font-size: 12px;
  font-weight: 500;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .mode-button {
    padding: 8px 3px;
  }
  
.mode-button .el-icon {
  font-size: 16px;
}

@media (max-width: 1100px) {
  .navigation-controls {
    right: 12px;
    width: min(340px, calc(100vw - 24px));
  }

  .live2d-role-console {
    right: 12px;
    bottom: 12px;
    width: min(340px, calc(100vw - 24px));
    flex-wrap: wrap;
    border-radius: 10px;
  }

  .live2d-role-console--chat {
    min-height: min(600px, calc(100vh - 24px));
    flex-wrap: nowrap;
  }

  .live2d-mode-switch {
    width: 100%;
  }

  .live2d-chat-stage {
    height: 220px;
  }

  :global(body.shengdi-live2d-chat-mode #live2d) {
    right: 68px !important;
    bottom: 330px !important;
    width: 200px !important;
    height: 300px !important;
  }

  :global(body.shengdi-live2d-chat-mode #live2dMessageBox) {
    right: 22px !important;
    bottom: 312px !important;
    width: min(296px, calc(100vw - 44px)) !important;
  }

  .agent-guide-panel {
    left: 12px;
    right: 12px;
    top: 80px;
    width: auto;
    max-height: 48vh;
  }

  .agent-guide-spots {
    grid-template-columns: 1fr;
    max-height: 150px;
  }

  .agent-guide-text {
    max-height: 190px;
  }

  :global(#live2dMessageBox) {
    right: 12px !important;
    bottom: 300px !important;
    width: min(300px, calc(100vw - 24px)) !important;
  }

  :global(body.shengdi-navigation-active #live2d) {
    right: 12px !important;
    bottom: 0 !important;
    left: auto !important;
    top: auto !important;
    opacity: 1 !important;
    pointer-events: auto !important;
  }

  :global(body.shengdi-navigation-active #live2dMessageBox) {
    right: 12px !important;
    bottom: 300px !important;
    left: auto !important;
    top: auto !important;
    opacity: 1 !important;
    pointer-events: none !important;
  }

  :global(body.shengdi-navigation-active #live2dChatPanel) {
    right: 12px !important;
    left: auto !important;
    top: auto !important;
    bottom: 12px !important;
    width: min(400px, calc(100vw - 24px)) !important;
  }

  :global(body.shengdi-navigation-active #live2d-toolbox) {
    display: none !important;
  }

  :global(.mapboxgl-ctrl-top-right .mapbox-directions-component) {
    margin-top: 68px;
    margin-right: 12px;
    width: min(360px, calc(100vw - 24px));
  }
}
  
  .mode-button span {
    font-size: 11px;
  }
}

/* 导航控件容器需要相对定位 */
.agent-save-row { display: flex; gap: 8px; margin-top: 10px; }
.agent-save-row input { flex: 1; min-width: 0; border: 1px solid #dce2ed; border-radius: 6px; padding: 7px 8px; }
.agent-save-row button { flex-shrink: 0; border: 1px solid #dce2ed; background: white; border-radius: 6px; cursor: pointer; }
.agent-saved-row { display: flex; gap: 8px; align-items: center; margin: 12px 0; padding: 10px; background: #f8fafc; border-radius: 8px; }
.agent-saved-row > div { flex: 1; min-width: 0; word-break: break-word; }
.agent-saved-row small { display: block; color: #64748b; margin-top: 4px; }
.agent-knowledge-sources { padding: 12px 14px; background: #f8fafce8; font-size: 12px; color: #64748b; line-height: 1.6; }
.agent-knowledge-sources p { margin: 8px 0 0; }
.agent-guide-spot small { display: block; font-size: 11px; color: #64748b; line-height: 1.5; overflow-wrap: anywhere; }
</style>

<!-- Popover teleport 到 body，必须非 scoped；层级需盖住 Mapbox -->
<style>
/* 页面内大量 9999；默认 Popper ~2000，会被盖住 — 与 :z-index 双保险 */
.shengdi-wp-import-popper.el-popper {
  z-index: 30000 !important;
}

.agent-new-session { border: 0; background: transparent; color: #667085; cursor: pointer; white-space: nowrap; font-size: 12px; }
.agent-progress { position: absolute; bottom: calc(100% + 10px); left: 0; width: 100%; box-sizing: border-box; padding: 12px; border-radius: 10px; background: #fff; color: #475569; box-shadow: 0 6px 24px #33415522; font-size: 13px; }
.live2d-role-console--chat .agent-progress { position: static; }
.agent-progress-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.agent-progress-heading button { flex-shrink: 0; border: 1px solid #ddd; border-radius: 6px; padding: 4px 10px; background: white; cursor: pointer; }
.agent-partial-reply { white-space: pre-wrap; overflow: auto; max-height: 150px; margin-top: 8px; line-height: 1.6; }
.agent-day-controls { padding: 12px 14px; background: #ffffffd9; font-size: 12px; color: #475569; }
.agent-day-tabs { display: flex; gap: 6px; overflow-x: auto; }
.agent-day-tabs button { white-space: nowrap; border: 1px solid #dce2ed; padding: 6px 12px; background: white; border-radius: 6px; cursor: pointer; }
.agent-day-tabs button.active { background: #edf5ff; border-color: #7aa7ff; color: #315b9c; }
.agent-day-warning { color: #856135; line-height: 1.5; margin: 6px 0; }
.agent-import-day { padding: 8px 12px; margin-top: 6px; border: 0; border-radius: 6px; background: #517fe0; color: white; cursor: pointer; }
.agent-import-day:disabled, .agent-new-session:disabled { opacity: .55; cursor: not-allowed; }
.agent-guide-panel { overflow-y: auto; }
@media (max-width: 600px) {
  .live2d-role-console:not(.live2d-role-console--chat) { flex-wrap: wrap; }
  .live2d-role-console:not(.live2d-role-console--chat) .live2d-input-row { flex-basis: 100%; }
  /* 窄屏把输入框放到单独一行，发送/新对话/我的行程另起一行，避免「我的行程」被裁掉 */
  .live2d-role-console:not(.live2d-role-console--chat) .live2d-input-row { flex-wrap: wrap; }
  .live2d-role-console:not(.live2d-role-console--chat) .live2d-role-input { flex: 1 1 100%; }
  .live2d-role-console:not(.live2d-role-console--chat) .live2d-role-send { flex: 1 1 auto; }
  .live2d-role-console:not(.live2d-role-console--chat) .agent-new-session { flex: 0 0 auto; }
  .agent-guide-panel { max-height: calc(100dvh - 240px); }
}
</style>
