<template>
  <div class="form-container">
    <div class="form-header">
      <h2 class="form-title">地标信息编辑</h2>
      <div class="form-subtitle">添加新的圣地巡礼地点信息</div>
    </div>
    
    <div class="form-content">
      <!-- 搜索作品下拉框 -->
      <div class="field-group">
        <label class="field-label">对应作品</label>
        <div class="search-wrapper">
          <i class="fa fa-search input-icon"></i>
          <input 
            type="text" 
            class="form-input search-input" 
            placeholder="搜索作品名称..."
            @focus="showDropdown = true"
            @blur="setTimeout(() => showDropdown = false, 200)"
            @input="inputing(searchQuery)"
            v-model="searchQuery"
          >
          <div class="search-dropdown" v-if="showDropdown && searchQuery">
  <div 
    class="search-result-item" 
    v-for="item in searchResult" 
    @click="into(item.xunliname,item.xunliid)"
  >
    <img 
      :src="item.bcimg" 
      class="item-image" 
      :alt="item.xunliname"
      onerror="this.style.display='none'"
    >
    <div class="item-info">
      <div class="item-name">{{ item.xunliname }}</div>
      <div class="item-remark" v-if="item.remark">{{ item.remark }}</div>
    </div>
  </div>
</div>
        </div>
      </div>
      
      <!-- 修改为一列布局 -->
      <div class="field-group">
        <label class="field-label">地标位置</label>
        <div class="coordinates-wrapper">
          <div class="input-wrapper">
            <i class="fa fa-long-arrow-alt-east input-icon"></i>
            <p class="form-input">经度：{{ lng }}</p>
          </div>
          <div class="input-wrapper">
            <i class="fa fa-long-arrow-alt-north input-icon"></i>
            <p class="form-input">纬度：{{ lat }}</p>
          </div>
        </div>
      </div>
      
      <div class="field-group">
        <label class="field-label">地标原名</label>
        <div class="input-wrapper">
          <i class="fa fa-globe input-icon"></i>
          <input type="text" class="form-input" placeholder="优先使用官方语言名称" v-model="name">
        </div>
      </div>
      <div class="field-group">
        <label class="field-label">地标中文名</label>
        <div class="input-wrapper">
          <i class="fa fa-language input-icon"></i>
          <input type="text" class="form-input" placeholder="有中文名时优先显示" v-model="zhcn">
        </div>
      </div>
      <div class="field-group">
        <label class="field-label">番剧具体位置</label>
        <div class="coordinates-wrapper">
          <div class="input-wrapper">
            <i class="fa fa-film input-icon"></i>
            <input type="text" class="form-input" placeholder="请输入EP集数" v-model="episode">
          </div>
          <div class="input-wrapper">
            <i class="fa fa-long-arrow-alt-north input-icon"></i>
            <input type="text" class="form-input" placeholder="请输入具体时间" v-model="time">
          </div>
        </div>
        
      </div>
      <!-- 全宽字段 -->
      <div class="field-group full-width">
        <label class="field-label">备注</label>
        <div class="input-wrapper">
          <i class="fa fa-comment input-icon"></i>
          <textarea class="form-input textarea" placeholder="请输入备注信息" v-model="beizhu"></textarea>
        </div>
      </div>
      <!-- 上传区域 -->
      <div class="field-group full-width">
        <label class="field-label">对应作品截图</label>
        
        <div class="upload-area"
            @dragover.prevent="handleDragOver"
            @dragleave="handleDragLeave"
            @drop.prevent="handleDrop">
            <div class="upload-content" v-if="realImage !== null ">
          <img :src="realImage" alt="上传图片预览" class="preview-image">
        </div>
          <div class="upload-content"  v-if="realImage === null">
            <i class="fa fa-cloud-upload-alt upload-icon"></i>
            <p class="upload-text">选择图片或拖拽截图文件到框内即可上传</p>
            <button class="upload-button">
              <i class="fa fa-folder-open"></i>选择文件
            </button>
            <input type="file" class="hidden" @change="upload" multiple accept="image/*">
          </div>
        </div>
        <div class="upload-hints">
          <p>建议上传高清无字幕作品画面</p>
          <p>请勿上传带暂停播放按钮的手机截屏。</p>
          <p>请勿在此上传巡礼实景对比照片！</p>
        </div>
      </div>
      <div class="form-actions">
        <button class="cancel-button" @click="back">取消</button>
        <button class="save-button" @click="save" :disabled="!issearch">保存</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter,useRoute } from 'vue-router'
import { getXunliSearch,addXunliNew } from '@/api/xunli'
import { processFile } from '@/ts/picyanzheng'
import { updatePic } from '@/api/updatepic'
const route = useRoute()
const $router = useRouter()
const lnglat = route.query
const lng = ref(lnglat.lng)
const lat = ref(lnglat.lat)
const showDropdown = ref(false)
const searchQuery = ref('')
const  searchaera = ref(true)
const searchResult = ref([])
const uploadedFiles = ref(null); // 存储上传的文件
const isDragging = ref(false); // 拖拽状态
const url2 = ref('')
const realImage = ref(null);
const name = ref('')
const zhcn = ref('')
const beizhu = ref('')
const episode = ref('')
const time = ref('')
const issearch = ref(false)
const addnewshuzu = ref([])
const xunliid = ref(0)
const uploadProgress = ref(0); // 上传进度
const inputing = async(search) =>{
const result = await getXunliSearch(search)
searchResult.value = result.data
}
const back = () =>{
  $router.push('/shengdi')
}
const into = (name,id) =>{
  issearch.value = true
  searchQuery.value = name
  xunliid.value = id
  searchResult.value = []
  showDropdown.value = false
}
const handleFileSelect = async(event) => {
  const files = event.target.files[0];
  const processfile = processFile(files)
  if (!processfile) return
    const reader = new FileReader();
          reader.onload = (e) => {
            realImage.value = e.target.result;
          };
          reader.readAsDataURL(files);
    const result = await updatePic(processfile)
    url2.value = result.url
    
  event.target.value = "";
};

const handleDrop = async(event) => {
  event.preventDefault();
  isDragging.value = false;
  const files = event.dataTransfer.files[0];
  const processfile = processFile(files)
  if (!processfile) return
    const reader = new FileReader();
          reader.onload = (e) => {
            realImage.value = e.target.result;
          };
          reader.readAsDataURL(files);
    const result = await updatePic(processfile)
    url2.value = result.url
    console.log(url2)
};
const handleDragOver = (event) => {
  event.preventDefault();
  isDragging.value = true;
};
const handleDragLeave = () => {
  isDragging.value = false;
};
const save = () =>{
  addnewshuzu.value.push({searchQuery:searchQuery.value,lat:lat.value,lng:lng.value,xunliname:name.value,zhcn:zhcn.value,beizhu:beizhu.value,gaoqingpic:url2.value,ep:episode.value,time:time.value,time:time.value,xunliid:xunliid.value})
  console.log(addnewshuzu.value)
  xunliid.value = 0
  searchQuery.value = ''
  name.value = ''
  zhcn.value = ''
  beizhu.value = ''
  url2.value = ''
  issearch.value = false
  episode.value = ''
  time.value = ''
  addXunliNew(addnewshuzu.value)
  back()
}
</script>

<style scoped>
.coordinates-wrapper .input-wrapper {
  display: flex;
  align-items: center;
}

.coordinates-wrapper .form-input {
  flex: 1;
  padding-left: 42px;
}

.coordinates-wrapper .input-icon {
  left: 14px;
}
.preview-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  margin-bottom: 15px;
}
.form-container {
  max-width: 900px;
  margin: 40px auto;
  background: white;
  border-radius: 12px;
  box-shadow: 0 5px 25px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.form-header {
  padding: 25px 35px;
  background: linear-gradient(135deg, #6e45e2 0%, #88d3ce 100%);
  color: white;
}

.form-title {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.form-subtitle {
  opacity: 0.9;
  font-size: 14px;
  margin-top: 6px;
}

.form-content {
  padding: 35px;
}

/* 移除 form-grid 相关样式 */
.form-grid {
  display: block; /* 改为单列布局 */
}

.coordinates-wrapper {
  display: flex; /* 保持经纬度并排 */
  gap: 15px;
}

.field-group {
  margin-bottom: 20px;
}

.field-label {
  display: block;
  margin-bottom: 10px;
  font-weight: 500;
  color: #4a5568;
  font-size: 14px;
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #6e45e2;
  font-size: 15px;
}

.form-input {
  width: 100%;
  padding: 13px 15px 13px 42px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background-color: #f8fafc;
  transition: all 0.25s ease;
}

.form-input:focus {
  border-color: #6e45e2;
  box-shadow: 0 0 0 3px rgba(110, 69, 226, 0.15);
  background-color: white;
}

.textarea {
  min-height: 120px;
  resize: vertical;
}

.coordinates-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.upload-area {
  border: 2px dashed rgba(138, 43, 226, 0.3);
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  background-color: rgba(138, 43, 226, 0.05);
  transition: all 0.3s;
}

.upload-area:hover {
  border-color: rgba(138, 43, 226, 0.5);
  background-color: rgba(138, 43, 226, 0.08);
}

.upload-icon {
  font-size: 28px;
  color: #8a2be2;
  margin-bottom: 15px;
}

.upload-text {
  margin-bottom: 20px;
  color: #555;
  font-size: 15px;
}

.upload-button {
  background-color: #8a2be2;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.upload-button:hover {
  background-color: #7b1fa2;
}

.upload-hints p {
  font-size: 13px;
  color: #777;
  margin: 5px 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 40px;
  gap: 15px;
}

.cancel-button, .save-button {
  padding: 12px 28px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s;
}

.cancel-button {
  background-color: #f5f5f5;
  color: #555;
}

.save-button {
  background-color: #8a2be2;
  color: white;
}

.cancel-button:hover {
  background-color: #e0e0e0;
}

.save-button:hover {
  background-color: #7b1fa2;
}

.search-wrapper {
  position: relative;
}

/* 修改搜索框样式 */
.search-input {
  padding-left: 45px !important;
  height: 48px; /* 增加高度 */
  font-size: 16px; /* 增大字体 */
}

/* 调整下拉框位置 */
.search-dropdown {
  top: calc(100% + 5px); /* 下拉框与输入框间距 */
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  z-index: 10;
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.search-result-item:hover {
  background: #f8f0ff;
}

.hidden {
  display: none;
}
.search-result-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.search-result-item:hover {
  background: #f8f0ff;
}

.item-image {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  margin-right: 12px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-weight: 500;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-remark {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 调整下拉框尺寸 */
.search-dropdown {
  max-height: 300px;
  overflow-y: auto;
}
</style>

<style scoped>
/* 针对对应作品字段组 */
.field-group:first-child {
  margin-bottom: 30px; /* 增加底部间距 */
}

.field-group:first-child .form-input {
  height: 48px;
  font-size: 16px;
}
</style>