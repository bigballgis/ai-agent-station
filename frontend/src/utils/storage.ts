const MAX_STORAGE_SIZE = 5 * 1024 * 1024 // 5MB

export function getStorageSize(): number {
  let total = 0
  for (const key in localStorage) {
    if (localStorage.hasOwnProperty(key)) {
      total += localStorage.getItem(key)?.length || 0
    }
  }
  return total * 2 // UTF-16 encoding
}

export function isStorageNearFull(): boolean {
  return getStorageSize() > MAX_STORAGE_SIZE * 0.8
}

export function cleanupOldItems(keepKeys: string[]): void {
  if (!isStorageNearFull()) return
  for (const key in localStorage) {
    if (localStorage.hasOwnProperty(key) && !keepKeys.includes(key)) {
      localStorage.removeItem(key)
      if (!isStorageNearFull()) break
    }
  }
}
