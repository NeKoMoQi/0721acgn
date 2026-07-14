<template>
  <div class="root-container">
    <Headpage style=" position: sticky !important; top: 0px ;"></Headpage>
    
    <!-- 单独的背景层 -->
    <div class="background-layer"></div>

    <!-- 内容容器 -->
    <div class="acgn_container">
      <div class="block text-center">
  <span class="demonstration"></span>
  <el-carousel height="400px" motion-blur>
    <el-carousel-item v-for="item in 4" :key="item">
      <h3 class="small justify-center" text="2xl">{{ item }}</h3>
    </el-carousel-item>
  </el-carousel>
</div>
<div style="margin-top: 40px;">
  <h1>猜你喜欢</h1>
</div>
<div class="acgn_show">
  <div v-for="(item,index) in 360" class="item">111</div>
</div>
  </div>

    <Behind class="bottom"></Behind>
  </div>
</template>

<style scoped>
.item{
  width: 100%;
  height: 270px;
  background-color: black;
}
.acgn_show{
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  width:auto;
  height: auto;
  overflow-y: auto;
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
  min-height: 3000px;
  margin: 0px auto 0px; /* 顶部留出Header空间 */
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(211, 211, 211, 0.8);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 40px;
  position: relative; /* 建立新的层叠上下文 */
  margin-bottom: 40px;
}
.bottom {
  position: relative;
  top: 200px;
}
</style>