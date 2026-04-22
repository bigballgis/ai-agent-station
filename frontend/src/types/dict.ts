export interface DictType {
  id: number
  dictName: string
  dictType: string
  status: string
  remark?: string
}

export interface DictItem {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  dictSort: number
  cssClass?: string
  listClass?: string
  isDefault: string
  status: string
}
