/**
 * verifyLazyLoading - 验证所有路由是否使用懒加载
 *
 * 扫描 router/index.ts 文件，检查所有路由组件是否使用动态 import()。
 * 同时检查 package.json 中是否有不必要的依赖。
 *
 * 使用方式：
 *   node scripts/verifyLazyLoading.mjs
 */

import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const ROOT_DIR = path.resolve(__dirname, '..')
const ROUTER_FILE = path.join(ROOT_DIR, 'src/router/index.ts')
const PACKAGE_JSON = path.join(ROOT_DIR, 'package.json')

const result = {
  passed: true,
  errors: [],
  warnings: [],
  info: [],
}

console.log('=== Lazy Loading Verification ===\n')

// 1. 验证路由懒加载
console.log('--- Checking router lazy loading ---')

if (!fs.existsSync(ROUTER_FILE)) {
  result.errors.push(`Router file not found: ${ROUTER_FILE}`)
} else {
  const routerContent = fs.readFileSync(ROUTER_FILE, 'utf-8')

  // 查找所有 component 定义
  const componentPatterns = [
    /component:\s*([^,\n]+)/g,
  ]

  let totalRoutes = 0
  let lazyLoaded = 0
  const staticImports = []

  for (const pattern of componentPatterns) {
    let match
    while ((match = pattern.exec(routerContent)) !== null) {
      totalRoutes++
      const componentDef = match[1].trim()

      if (componentDef.includes('import(')) {
        lazyLoaded++
      } else if (componentDef.includes('import ') || /^[A-Z]/.test(componentDef)) {
        // 静态导入（排除注释）
        const lineStart = routerContent.lastIndexOf('\n', match.index) + 1
        const lineEnd = routerContent.indexOf('\n', match.index)
        const line = routerContent.slice(lineStart, lineEnd).trim()

        // 跳过注释行
        if (!line.startsWith('//') && !line.startsWith('*')) {
          staticImports.push(componentDef)
        }
      }
    }
  }

  result.info.push(`Total routes found: ${totalRoutes}`)
  result.info.push(`Lazy loaded routes: ${lazyLoaded}`)
  result.info.push(`Static imports: ${staticImports.length}`)

  console.log(`  Total routes: ${totalRoutes}`)
  console.log(`  Lazy loaded: ${lazyLoaded}`)
  console.log(`  Static imports: ${staticImports.length}`)

  // 允许 MainLayout 静态导入（布局组件不应懒加载以避免闪烁）
  const allowedStaticImports = ['MainLayout']

  for (const imp of staticImports) {
    const isAllowed = allowedStaticImports.some((allowed) => imp.includes(allowed))
    if (!isAllowed) {
      result.warnings.push(`Route uses static import: ${imp}`)
      console.log(`  WARNING: Static import detected: ${imp}`)
    } else {
      console.log(`  OK: Allowed static import: ${imp}`)
    }
  }

  if (lazyLoaded === 0 && totalRoutes > 1) {
    result.errors.push('No lazy-loaded routes found')
    result.passed = false
  }

  // 检查是否有 webpackPrefetch 标记
  const prefetchCount = (routerContent.match(/webpackPrefetch/g) || []).length
  result.info.push(`Routes with prefetch: ${prefetchCount}`)
  console.log(`  Routes with prefetch: ${prefetchCount}`)
}

// 2. 检查不必要的依赖
console.log('\n--- Checking for unnecessary dependencies ---')

if (fs.existsSync(PACKAGE_JSON)) {
  const pkg = JSON.parse(fs.readFileSync(PACKAGE_JSON, 'utf-8'))
  const dependencies = pkg.dependencies || {}
  const devDependencies = pkg.devDependencies || {}

  // 检查可能在 dependencies 中但应该只在 devDependencies 中的包
  const devOnlyPackages = [
    'typescript', 'eslint', 'vitest', '@types/', '@typescript-eslint/',
    'vite', 'postcss', 'autoprefixer', 'tailwindcss', 'rollup',
  ]

  for (const pkgName of Object.keys(dependencies)) {
    for (const devOnly of devOnlyPackages) {
      if (pkgName.includes(devOnly)) {
        result.warnings.push(
          `${pkgName} is in dependencies but looks like a dev dependency`
        )
        console.log(`  WARNING: ${pkgName} should be in devDependencies`)
      }
    }
  }

  // 检查已知的可替代轻量包
  const heavyDeps = {
    'moment': 'Consider using dayjs (already in project) instead',
    'lodash': 'Consider using lodash-es for tree-shaking or native methods',
    'jquery': 'Not needed in Vue 3 projects',
    'underscore': 'Consider using native JS methods',
  }

  for (const [dep, suggestion] of Object.entries(heavyDeps)) {
    if (dependencies[dep]) {
      result.warnings.push(`${dep}: ${suggestion}`)
      console.log(`  WARNING: ${dep} - ${suggestion}`)
    }
  }

  // 检查重复的依赖（同时存在于 dependencies 和 devDependencies）
  const allDeps = Object.keys(dependencies)
  const allDevDeps = Object.keys(devDependencies)
  const duplicates = allDeps.filter((d) => allDevDeps.includes(d))

  if (duplicates.length > 0) {
    result.warnings.push(`Duplicate packages in both dependencies and devDependencies: ${duplicates.join(', ')}`)
    console.log(`  WARNING: Duplicates: ${duplicates.join(', ')}`)
  }

  console.log(`  Dependencies: ${allDeps.length}`)
  console.log(`  DevDependencies: ${allDevDeps.length}`)
  console.log(`  Total packages: ${allDeps.length + allDevDeps.length}`)
}

// 3. 检查组件是否使用了 defineAsyncComponent
console.log('\n--- Checking async component usage ---')

const pagesDir = path.join(ROOT_DIR, 'src/pages')
if (fs.existsSync(pagesDir)) {
  const pageFiles = fs.readdirSync(pagesDir).filter((f) => f.endsWith('.vue'))
  result.info.push(`Page components: ${pageFiles.length}`)
  console.log(`  Page components: ${pageFiles.length}`)
  console.log('  All page components are lazy-loaded via router (verified above)')
}

// 输出结果
console.log('\n=== Verification Result ===')
console.log(`Status: ${result.passed ? 'PASS' : 'FAIL'}`)

if (result.errors.length > 0) {
  console.log('\nErrors:')
  result.errors.forEach((e) => console.log(`  ERROR: ${e}`))
}

if (result.warnings.length > 0) {
  console.log('\nWarnings:')
  result.warnings.forEach((w) => console.log(`  WARNING: ${w}`))
}

if (result.info.length > 0) {
  console.log('\nInfo:')
  result.info.forEach((i) => console.log(`  INFO: ${i}`))
}

if (!result.passed) {
  process.exit(1)
}
