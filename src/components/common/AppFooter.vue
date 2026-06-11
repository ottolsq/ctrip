<template>
  <footer class="app-footer">
    <div class="footer-container">
      <div class="footer-links">
        <div class="footer-section">
          <h4>关于我们</h4>
          <router-link to="/about">公司介绍</router-link>
          <a href="#">联系我们</a>
          <a href="#">加入我们</a>
        </div>
        <div class="footer-section">
          <h4>产品服务</h4>
          <router-link to="/destinations">目的地</router-link>
          <router-link to="/guides">旅行攻略</router-link>
          <router-link to="/blind-box">旅行盲盒</router-link>
        </div>
        <div class="footer-section">
          <h4>帮助中心</h4>
          <a href="#">常见问题</a>
          <a href="#">用户协议</a>
          <a href="#">隐私政策</a>
        </div>
        <div class="footer-section">
          <h4>联系方式</h4>
          <p>邮箱: contact@ctrip.com</p>
          <p>电话: 400-888-8888</p>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 捷程旅行网. All rights reserved.</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
</script>

<style scoped>
.app-footer {
  background: #2c3e50;
  color: #ecf0f1;
  margin-top: auto;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px 20px;
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30px;
  margin-bottom: 30px;
}

.footer-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-section h4 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #fff;
}

.footer-section a {
  color: #bdc3c7;
  text-decoration: none;
  font-size: 13px;
  transition: color 0.2s;
}

.footer-section a:hover {
  color: #409eff;
}

.footer-section p {
  color: #bdc3c7;
  font-size: 13px;
  margin: 0;
}

.footer-bottom {
  border-top: 1px solid #3d5166;
  padding-top: 16px;
  text-align: center;
}

.footer-bottom p {
  color: #7f8c8d;
  font-size: 12px;
  margin: 0;
}
</style>
