<template>
  <div class="shengdi-unavailable">
    <Headpage></Headpage>
    <div class="sd-card">
      <h2>圣地巡礼</h2>
      <p>该页面的源文件在 GitHub 仓库中已被编码问题损坏（约 300 个字符丢失，模板标签与脚本字符串断裂），无法直接编译。</p>
      <p>站点其余功能不受影响。把原始的 <code>frontend/src/view/shengdi/index.vue</code> 放回即可恢复地图巡礼功能。</p>
      <div class="sd-actions">
        <router-link class="sd-btn" to="/">返回首页</router-link>
        <router-link class="sd-btn" to="/shengdi/addnew">添加巡礼点</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.shengdi-unavailable {
  min-height: 100vh;
  background: #f6f7fb;
}
.sd-card {
  max-width: 720px;
  margin: 80px auto;
  padding: 32px 36px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(20, 30, 60, 0.08);
  color: #333;
  line-height: 1.9;
}
.sd-card h2 {
  margin: 0 0 16px;
  font-size: 22px;
}
.sd-card p {
  margin: 8px 0;
  color: #555;
}
.sd-card code {
  background: #f0f2f7;
  padding: 2px 6px;
  border-radius: 4px;
}
.sd-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}
.sd-btn {
  padding: 8px 18px;
  border-radius: 8px;
  background: #4f7cff;
  color: #fff;
  text-decoration: none;
}
.sd-btn:hover {
  background: #3b64d8;
}
</style>
