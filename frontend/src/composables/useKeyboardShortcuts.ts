import { onMounted, onUnmounted } from 'vue'

export function useKeyboardShortcuts(shortcuts: Record<string, () => void>) {
  const handleKeyDown = (e: KeyboardEvent) => {
    // Don't trigger when typing in inputs
    if (['INPUT', 'TEXTAREA', 'SELECT'].includes((e.target as HTMLElement).tagName)) return

    const key = [
      e.ctrlKey && 'Ctrl',
      e.shiftKey && 'Shift',
      e.altKey && 'Alt',
      e.key
    ].filter(Boolean).join('+')

    if (shortcuts[key]) {
      e.preventDefault()
      shortcuts[key]()
    }
  }

  onMounted(() => window.addEventListener('keydown', handleKeyDown))
  onUnmounted(() => window.removeEventListener('keydown', handleKeyDown))
}
