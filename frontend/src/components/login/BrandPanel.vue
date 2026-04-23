<template>
  <div class="brand-panel hidden lg:flex lg:w-1/2 relative overflow-hidden flex-col items-center justify-center bg-gradient-to-br from-indigo-600 via-indigo-700 to-purple-700">
    <!-- 装饰性几何图形 -->
    <div class="geometric-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
      <div class="shape shape-5"></div>
      <div class="shape shape-6"></div>
      <div class="shape shape-7"></div>
      <div class="shape shape-8"></div>
    </div>

    <!-- 浮动粒子 -->
    <div class="particles">
      <div class="particle" v-for="n in 20" :key="n" :style="particleStyle(n)"></div>
    </div>

    <!-- 品牌内容 -->
    <div class="brand-content relative z-10 text-center px-12">
      <!-- AI 图标 -->
      <div class="brand-icon mb-8">
        <div class="icon-ring">
          <div class="icon-inner">
            <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
              <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/>
              <path d="M12 2v1"/>
              <circle cx="12" cy="6" r="1" fill="white" stroke="none"/>
              <path d="M8 6H4"/>
              <path d="M16 6h4"/>
              <path d="M12 10v2"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- 产品名称 -->
      <h1 class="brand-title text-5xl font-bold text-white mb-4 tracking-tight">
        AI Agent Station
      </h1>

      <!-- 产品描述 -->
      <p class="brand-description text-lg text-indigo-100 leading-relaxed max-w-md mx-auto">
        {{ t('login.brandDescription') || '智能体管理与编排平台，赋能团队高效构建、部署和管理 AI 智能体应用' }}
      </p>

      <!-- 特性标签 -->
      <div class="brand-features mt-10 flex flex-wrap justify-center gap-3">
        <span class="feature-tag">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
          {{ t('login.feature1') }}
        </span>
        <span class="feature-tag">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          {{ t('login.feature2') }}
        </span>
        <span class="feature-tag">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          {{ t('login.feature3') }}
        </span>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="absolute bottom-8 left-0 right-0 text-center text-indigo-200 text-sm opacity-60">
      &copy; 2025 AI Agent Station. All rights reserved.
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

function particleStyle(_n: number) {
  const size = Math.random() * 4 + 2
  const left = Math.random() * 100
  const delay = Math.random() * 8
  const duration = Math.random() * 6 + 8
  const opacity = Math.random() * 0.3 + 0.1
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    opacity: opacity,
  }
}
</script>

<style scoped>
.brand-panel {
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 30%, #7c3aed 70%, #7e22ce 100%);
  position: relative;
}

/* 装饰性几何图形 */
.geometric-shapes {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  background: white;
}

.shape-1 {
  width: 300px;
  height: 300px;
  top: -80px;
  right: -60px;
  animation: floatShape 12s ease-in-out infinite;
}

.shape-2 {
  width: 200px;
  height: 200px;
  bottom: 10%;
  left: -40px;
  border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
  animation: floatShape 15s ease-in-out infinite reverse;
}

.shape-3 {
  width: 120px;
  height: 120px;
  top: 30%;
  right: 15%;
  border-radius: 50%;
  opacity: 0.05;
  animation: floatShape 10s ease-in-out infinite;
}

.shape-4 {
  width: 80px;
  height: 80px;
  top: 60%;
  left: 20%;
  border-radius: 20%;
  transform: rotate(45deg);
  opacity: 0.06;
  animation: floatShape 8s ease-in-out infinite;
}

.shape-5 {
  width: 160px;
  height: 160px;
  bottom: -40px;
  right: 20%;
  border-radius: 50%;
  opacity: 0.04;
  animation: floatShape 14s ease-in-out infinite;
}

.shape-6 {
  width: 60px;
  height: 60px;
  top: 15%;
  left: 30%;
  border-radius: 50%;
  opacity: 0.1;
  animation: floatShape 9s ease-in-out infinite reverse;
}

.shape-7 {
  width: 240px;
  height: 240px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  opacity: 0.03;
  animation: pulseShape 6s ease-in-out infinite;
}

.shape-8 {
  width: 40px;
  height: 40px;
  bottom: 30%;
  right: 30%;
  border-radius: 50%;
  opacity: 0.12;
  animation: floatShape 7s ease-in-out infinite;
}

@keyframes floatShape {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  33% {
    transform: translateY(-20px) rotate(5deg);
  }
  66% {
    transform: translateY(10px) rotate(-3deg);
  }
}

@keyframes pulseShape {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.03;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.15);
    opacity: 0.06;
  }
}

/* 浮动粒子 */
.particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.particle {
  position: absolute;
  bottom: -10px;
  background: white;
  border-radius: 50%;
  animation: particleRise linear infinite;
}

@keyframes particleRise {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) translateX(30px);
    opacity: 0;
  }
}

/* 品牌图标 */
.brand-icon {
  display: flex;
  justify-content: center;
}

.icon-ring {
  width: 100px;
  height: 100px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: iconFloat 4s ease-in-out infinite;
}

.icon-inner {
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes iconFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

/* 品牌标题 */
.brand-title {
  letter-spacing: -0.02em;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.15);
}

/* 特性标签 */
.feature-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.feature-tag:hover {
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-2px);
}
</style>
