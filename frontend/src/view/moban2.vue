<template>
    <div class="root-container">
      <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>
      
      <!-- 单独的背景层 -->
      <div class="background-layer"></div>
  
      <!-- 内容容器 -->
      <div class="acgn_container" ref="maintain" @scroll="handscroll"></div>
        
      <Behind class="bottom"></Behind>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  </script>
  <style scoped>
  /* 新增背景层 */
  .background-layer {
    position:fixed;
    left:0;
    top:50%;
    transform:translateY(-45%);
    width:100vw;
    height:0;
    padding-top:56.25%;          /* 1080÷1920 = 0.5625，16:9 高宽比 */
    background:url("/BCPHOTO/acgnbc4.png") no-repeat center/contain;
    overflow:hidden;            /* 如果内部还有绝对定位元素，防止溢出 */
  }
  .acgn_container {
    top: 100px;
    bottom: 50px;
    width: 62%;
    margin: 0px auto 0px; /* 顶部留出Header空间 */
    background: rgba(255, 255, 255, 0.95);
    border: 1px solid rgba(211, 211, 211, 0.8);
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 40px;
    position: relative; /* 建立新的层叠上下文 */
    margin-bottom: 40px;
    overflow-y: auto;
    height: calc(100vh - 150px);
    -ms-overflow-style: none; /* 适用于 Internet Explorer 和旧版 Edge */
    scrollbar-width: none; /* 适用于 Firefox */
  }
  .bottom {
    position: relative;
    top: 200px;
  }
  </style>