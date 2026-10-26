<template>
  <div class="acgn-filters">
    <el-select v-model="genre" placeholder="题材" clearable class="sel" @change="fire">
      <el-option label="不限题材" value="" />
      <el-option v-for="g in props.genres" :key="g" :label="g" :value="g" />
    </el-select>
    <el-select v-if="props.showStatus" v-model="status" placeholder="状态" clearable class="sel" @change="fire">
      <el-option label="不限状态" value="" />
      <el-option label="连载" value="连载" />
      <el-option label="完结" value="完结" />
    </el-select>
    <el-select v-model="decade" placeholder="年代" clearable class="sel" @change="fire">
      <el-option label="不限年代" value="" />
      <el-option label="2020年代" value="2020s" />
      <el-option label="2010年代" value="2010s" />
      <el-option label="2000年代" value="2000s" />
      <el-option label="1990年代" value="1990s" />
      <el-option label="更早" value="earlier" />
    </el-select>
    <el-select v-model="sortBy" placeholder="排序" class="sel" @change="fire">
      <el-option v-for="opt in props.sortOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
    </el-select>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ACGN_GENRES } from '@/utils/acgnConstants'

const props = defineProps({
  genres: {
    type: Array,
    default: () => ACGN_GENRES,
  },
  showStatus: {
    type: Boolean,
    default: true,
  },
  sortOptions: {
    type: Array,
    default: () => [
      { label: '默认', value: 'default' },
      { label: '热度（评分）', value: 'hot' },
      { label: '更新时间', value: 'update' },
    ],
  },
})

const genre = ref('')
const status = ref('')
const decade = ref('')
const sortBy = ref(props.sortOptions[0]?.value || 'default')

const emit = defineEmits(['change'])

function fire() {
  emit('change', {
    genre: genre.value,
    status: status.value,
    decade: decade.value,
    sortBy: sortBy.value || 'default',
  })
}

onMounted(() => fire())
</script>

<style scoped>
.acgn-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 16px 0 24px;
  padding: 16px;
  background: rgba(248, 249, 250, 0.95);
  border-radius: 8px;
  border: 1px solid #e9ecef;
}
.sel {
  width: 150px;
}
</style>
