import { Modal } from 'ant-design-vue'

export interface UseConfirmOptions {
  /** Dialog title */
  title?: string
  /** Dialog content */
  content?: string
  /** OK button text */
  okText?: string
  /** Cancel button text */
  cancelText?: string
  /** OK button type (e.g., 'danger', 'primary') */
  okType?: 'danger' | 'primary' | 'default' | 'dashed' | 'link' | 'text'
}

/**
 * useConfirm composable
 *
 * Provides a `confirm()` function that returns a Promise<boolean>.
 * Wraps Ant Design's Modal.confirm in a promise-based API for
 * cleaner async/await usage in component logic.
 *
 * @example
 * const { confirm } = useConfirm()
 *
 * async function handleDelete(id: number) {
 *   const ok = await confirm({
 *     title: 'Confirm Delete',
 *     content: 'Are you sure you want to delete this item?',
 *     okText: 'Delete',
 *     okType: 'danger',
 *   })
 *   if (ok) {
 *     await deleteItem(id)
 *   }
 * }
 */
export function useConfirm() {
  /**
   * Show a confirmation dialog and return a Promise<boolean>.
   * Resolves to `true` if user clicks OK, `false` if cancelled.
   */
  function confirm(options: UseConfirmOptions = {}): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: options.title ?? '',
        content: options.content ?? '',
        okText: options.okText,
        cancelText: options.cancelText,
        okType: options.okType ?? 'primary',
        onOk() {
          resolve(true)
        },
        onCancel() {
          resolve(false)
        },
      })
    })
  }

  return {
    confirm,
  }
}

export type UseConfirmReturn = ReturnType<typeof useConfirm>
