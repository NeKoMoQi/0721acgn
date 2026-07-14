<template>
  <footer class="bottom">
    <div class="bottom-inner">
      <img class="footer-logo" src="/LOGO.png" alt="0721Acgn" />
      <p class="license-text">
        本站全部内容禁止商业使用。文本内容除另有声明外，均在知识共享
        署名-非商业性使用-相同方式共享 3.0 中国大陆（CC BY-NC-SA 3.0 CN）
        许可协议下提供，附加条款亦可能应用，详细信息参见0721Acgn:著作权信息。
        其他类型作品著作权归属原作者，如有授权遵照授权协议使用。
      </p>
      <div class="divider"></div>
      <div class="beian">网站备案，我还没搞到</div>
    </div>
  </footer>
</template>

<script setup lang="ts"></script>

<style scoped>
.bottom {
  width: 100%;
  min-height: 240px;
  margin-top: auto;
  padding: 24px 20px;
  background: #ff69b4;
  color: #222;
}
.bottom-inner {
  width: min(960px, 100%);
  margin: 0 auto;
  text-align: center;
}
.footer-logo {
  width: 120px;
  height: 90px;
  object-fit: contain;
  display: block;
  margin: 0 auto 12px;
}
.license-text {
  margin: 0;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.75;
  text-align: left;
}
.divider {
  margin: 20px auto 10px;
  width: 60%;
  height: 1px;
  background: linear-gradient(
    to right,
    transparent 0%,
    #333 30%,
    #333 70%,
    transparent 100%
  );
}
.beian {
  font-size: 14px;
}
</style>
