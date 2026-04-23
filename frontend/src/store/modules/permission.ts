import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TreeNode } from '@/types'
import type { ApiResponse } from '@/types/common'
import request from '@/utils/request'

interface Permission {
  id: number
  name: string
  code: string
  type: string
  parentId?: number
  path?: string
  icon?: string
  sort?: number
  visible?: boolean
  children?: Permission[]
}

interface Role {
  id: number
  name: string
  code: string
  description?: string
  status?: string
  permissions?: string[]
  createdAt?: string
}

export const usePermissionStore = defineStore('permission', () => {
  // State
  const permissions = ref<Permission[]>([])
  const roles = ref<Role[]>([])
  const currentRole = ref<Role | null>(null)
  const menuTree = ref<TreeNode[]>([])

  // Getters
  const hasPermission = computed(() => {
    const permCodes = new Set<string>()
    function collect(perms: Permission[]) {
      perms.forEach((p) => {
        permCodes.add(p.code)
        if (p.children) collect(p.children)
      })
    }
    collect(permissions.value)

    return (code: string | string[]): boolean => {
      if (Array.isArray(code)) {
        return code.some((c) => permCodes.has(c))
      }
      return permCodes.has(code)
    }
  })

  const menuItems = computed(() => menuTree.value)

  // Actions
  async function fetchPermissions() {
    const res = await request.get('/v1/permissions/current') as ApiResponse<Permission[]>
    permissions.value = res.data || []
    menuTree.value = buildMenuTree(permissions.value)
  }

  async function fetchRoles() {
    const res = await request.get('/v1/roles') as ApiResponse<Role[]>
    roles.value = res.data || []
  }

  async function checkPermission(code: string): Promise<boolean> {
    try {
      const res = await request.get('/v1/permissions/check', { params: { code } }) as ApiResponse<boolean>
      return res.data === true
    } catch {
      return false
    }
  }

  function buildMenuTree(perms: Permission[]): TreeNode[] {
    const map = new Map<number, Permission>()
    perms.forEach((p) => map.set(p.id, p))

    const roots: TreeNode[] = []

    perms
      .filter((p) => !p.parentId || !map.has(p.parentId))
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
      .forEach((p) => {
        const node: TreeNode = {
          key: p.code,
          title: p.name,
          icon: p.icon,
        }
        const children = buildChildTree(p.id, perms)
        if (children.length > 0) {
          node.children = children
        } else {
          node.isLeaf = true
        }
        roots.push(node)
      })

    return roots
  }

  function buildChildTree(parentId: number, perms: Permission[]): TreeNode[] {
    return perms
      .filter((p) => p.parentId === parentId)
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
      .map((p) => {
        const node: TreeNode = {
          key: p.code,
          title: p.name,
          icon: p.icon,
        }
        const children = buildChildTree(p.id, perms)
        if (children.length > 0) {
          node.children = children
        } else {
          node.isLeaf = true
        }
        return node
      })
  }

  return {
    // State
    permissions,
    roles,
    currentRole,
    menuTree,
    // Getters
    hasPermission,
    menuItems,
    // Actions
    fetchPermissions,
    fetchRoles,
    checkPermission,
  }
})
