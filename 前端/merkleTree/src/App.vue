<template>
  <div id="app">
    <router-view />
  </div>
</template>
<script>
import { ref, computed } from 'vue'
import store from '@/store'

// 控制弹窗显示：未登录时显示
const showAuthModal = computed(() => !userStore.isLoggedIn)

export default {

mounted() {
  // 修改页头标题
  document.title = "MerkleTree可视化平台";
  // 修改页头图标（favicon）
  this.changeFavicon("/m1.svg");
}, methods: {
  changeFavicon(href) {
    // 查找现有的favicon元素
    let link = document.querySelector("link[rel*='icon']") || document.createElement('link');
    link.type = 'image/svg+xml'; // 根据图标类型调整
    link.rel = 'shortcut icon';
    link.href = href;

    // 添加或替换favicon元素
    document.getElementsByTagName('head')[0].appendChild(link);
  }
}
};
</script>
<style>
html,body {
 overflow-y: hidden !important;;
}
</style>