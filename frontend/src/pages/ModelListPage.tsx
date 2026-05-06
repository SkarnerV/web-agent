import React, { useState, useEffect } from 'react'
import { Cpu, Plus, Trash2, Pencil, Loader2, AlertCircle, X } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import {
  listCustomModels,
  createCustomModel,
  updateCustomModel,
  deleteCustomModel,
  getModelAgents,
} from '../api/model'
import type { CustomModelVO, CustomModelCreateRequest, CustomModelUpdateRequest } from '../api/types'

type DisplayStatus = 'connected' | 'failed' | 'unknown'

const statusConfig: Record<DisplayStatus, { dot: string; label: string }> = {
  connected: { dot: 'bg-success-500', label: '已连接' },
  failed: { dot: 'bg-error-500', label: '连接失败' },
  unknown: { dot: 'bg-gray-300', label: '未知' },
}

function mapStatus(raw?: string): DisplayStatus {
  if (raw === 'connected') return 'connected'
  if (raw === 'failed') return 'failed'
  return 'unknown'
}

interface ModelFormData {
  name: string
  apiUrl: string
  apiKey: string
}

const ModelListPage: React.FC = () => {
  const [models, setModels] = useState<CustomModelVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Modal state
  const [modalOpen, setModalOpen] = useState(false)
  const [editingModel, setEditingModel] = useState<CustomModelVO | null>(null)
  const [formData, setFormData] = useState<ModelFormData>({ name: '', apiUrl: '', apiKey: '' })
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Delete confirmation state
  const [deleteTarget, setDeleteTarget] = useState<CustomModelVO | null>(null)
  const [affectedAgents, setAffectedAgents] = useState<string[]>([])
  const [deleting, setDeleting] = useState(false)

  const fetchModels = async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await listCustomModels()
      setModels(result)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchModels()
  }, [])

  const openAddModal = () => {
    setEditingModel(null)
    setFormData({ name: '', apiUrl: '', apiKey: '' })
    setFormError(null)
    setModalOpen(true)
  }

  const openEditModal = (model: CustomModelVO) => {
    setEditingModel(model)
    setFormData({ name: model.name, apiUrl: model.apiUrl || '', apiKey: '' })
    setFormError(null)
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      setFormError('请输入模型名称')
      return
    }
    if (!editingModel && !formData.apiUrl.trim()) {
      setFormError('请输入 API 地址')
      return
    }
    if (!editingModel && !formData.apiKey.trim()) {
      setFormError('请输入 API Key')
      return
    }

    setSubmitting(true)
    setFormError(null)
    try {
      if (editingModel) {
        const payload: CustomModelUpdateRequest = {}
        if (formData.name !== editingModel.name) payload.name = formData.name
        if (formData.apiUrl && formData.apiUrl !== editingModel.apiUrl) payload.apiUrl = formData.apiUrl
        if (formData.apiKey) payload.apiKey = formData.apiKey
        const updated = await updateCustomModel(editingModel.id, payload)
        setModels(prev => prev.map(m => m.id === editingModel.id ? { ...m, ...updated } : m))
      } else {
        const payload: CustomModelCreateRequest = {
          name: formData.name.trim(),
          apiUrl: formData.apiUrl.trim(),
          apiKey: formData.apiKey.trim(),
        }
        const created = await createCustomModel(payload)
        setModels(prev => [...prev, created])
      }
      setModalOpen(false)
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : '操作失败，请检查配置')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDeleteClick = async (model: CustomModelVO) => {
    setDeleteTarget(model)
    if (model.agentCount > 0) {
      try {
        const result = await getModelAgents(model.id)
        setAffectedAgents(result.agents)
      } catch {
        setAffectedAgents([])
      }
    } else {
      setAffectedAgents([])
    }
  }

  const confirmDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteCustomModel(deleteTarget.id)
      setModels(prev => prev.filter(m => m.id !== deleteTarget.id))
      setDeleteTarget(null)
    } catch {
      // deletion failed silently
    } finally {
      setDeleting(false)
    }
  }

  const connectedCount = models.filter(m => mapStatus(m.connectionStatus) === 'connected').length

  if (loading) {
    return (
      <Layout breadcrumb={[{ label: '设置' }, { label: '模型管理' }]}>
        <div className="p-8 flex items-center justify-center min-h-[400px]">
          <Loader2 className="w-8 h-8 animate-spin text-text-tertiary" />
        </div>
      </Layout>
    )
  }

  if (error) {
    return (
      <Layout breadcrumb={[{ label: '设置' }, { label: '模型管理' }]}>
        <div className="p-8 flex flex-col items-center justify-center min-h-[400px] gap-4">
          <AlertCircle className="w-10 h-10 text-error-500" />
          <p className="text-text-secondary">{error}</p>
          <Button variant="primary" onClick={fetchModels}>重试</Button>
        </div>
      </Layout>
    )
  }

  return (
    <Layout breadcrumb={[{ label: '设置' }, { label: '模型管理' }]}>
      <div className="p-8">
        {/* Title Row */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-text-primary">模型管理</h1>
          <Button
            variant="primary"
            icon={<Plus className="w-4 h-4" />}
            onClick={openAddModal}
          >
            添加自定义模型
          </Button>
        </div>

        {/* Table */}
        <div className="bg-white rounded-xl border border-border-subtle overflow-hidden">
          {/* Table Header */}
          <div className="grid grid-cols-[2fr_2fr_1fr_1fr_1fr] gap-4 px-6 py-3 bg-gray-50 border-b border-border-subtle text-[13px] font-medium text-text-secondary">
            <div>模型名称</div>
            <div>API 地址</div>
            <div>连接状态</div>
            <div>绑定智能体</div>
            <div>操作</div>
          </div>

          {/* Table Body */}
          {models.map((model) => {
            const status = mapStatus(model.connectionStatus)
            const st = statusConfig[status]
            return (
              <div
                key={model.id}
                className="grid grid-cols-[2fr_2fr_1fr_1fr_1fr] gap-4 px-6 py-3.5 border-b border-border-subtle text-sm hover:bg-bg-hover transition-colors items-center"
              >
                {/* Name + masked key */}
                <div className="flex flex-col gap-0.5 min-w-0">
                  <span className="font-medium text-text-primary truncate">{model.name}</span>
                  {model.apiKeyMasked && (
                    <span className="text-[11px] text-text-tertiary truncate">{model.apiKeyMasked}</span>
                  )}
                </div>

                {/* API URL */}
                <div className="text-text-secondary truncate">{model.apiUrl || '-'}</div>

                {/* Status */}
                <div className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${st.dot}`} />
                  <span className="text-text-secondary">{st.label}</span>
                </div>

                {/* Agent count */}
                <div className="text-text-secondary">{model.agentCount} 个</div>

                {/* Actions */}
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => openEditModal(model)}
                    className="px-2.5 py-1.5 bg-white border border-border-subtle rounded text-xs text-text-secondary hover:border-border-strong transition-colors"
                  >
                    <span className="flex items-center gap-1">
                      <Pencil className="w-3 h-3" />
                      编辑
                    </span>
                  </button>
                  <button
                    onClick={() => handleDeleteClick(model)}
                    className="w-8 h-8 rounded-md flex items-center justify-center text-text-tertiary hover:text-error-500 hover:bg-error-50 transition-colors"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )
          })}

          {/* Empty state */}
          {models.length === 0 && (
            <div className="flex flex-col items-center justify-center py-16">
              <Cpu className="w-12 h-12 text-text-tertiary mb-4" />
              <p className="text-text-secondary mb-4">还没有添加自定义模型</p>
              <Button variant="primary" icon={<Plus className="w-4 h-4" />} onClick={openAddModal}>
                添加第一个模型
              </Button>
            </div>
          )}
        </div>

        {/* Summary Footer */}
        {models.length > 0 && (
          <div className="mt-4 flex items-center gap-4 text-[13px] text-text-tertiary">
            <span>共 {models.length} 个自定义模型</span>
            <span>· {connectedCount} 个已连接</span>
          </div>
        )}
      </div>

      {/* Add/Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={() => setModalOpen(false)} />
          <div className="relative bg-white rounded-xl shadow-xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-lg font-semibold text-text-primary">
                {editingModel ? '编辑模型' : '添加自定义模型'}
              </h2>
              <button onClick={() => setModalOpen(false)} className="text-text-tertiary hover:text-text-primary">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1.5">模型名称</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={e => setFormData(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="例如: GPT-4o (my key)"
                  className="w-full px-3 py-2 border border-border-subtle rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1.5">API 地址</label>
                <input
                  type="text"
                  value={formData.apiUrl}
                  onChange={e => setFormData(prev => ({ ...prev, apiUrl: e.target.value }))}
                  placeholder="https://api.openai.com/v1"
                  className="w-full px-3 py-2 border border-border-subtle rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1.5">
                  API Key
                  {editingModel && <span className="text-text-tertiary font-normal ml-1">(留空则不更新)</span>}
                </label>
                <input
                  type="password"
                  value={formData.apiKey}
                  onChange={e => setFormData(prev => ({ ...prev, apiKey: e.target.value }))}
                  placeholder={editingModel ? (editingModel.apiKeyMasked || '••••••••') : 'sk-...'}
                  className="w-full px-3 py-2 border border-border-subtle rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
                />
              </div>

              {formError && (
                <div className="flex items-center gap-2 text-sm text-error-500 bg-error-50 px-3 py-2 rounded-md">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{formError}</span>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <Button variant="secondary" onClick={() => setModalOpen(false)}>取消</Button>
              <Button variant="primary" onClick={handleSubmit} disabled={submitting}>
                {submitting ? '验证连接中...' : (editingModel ? '保存' : '添加')}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={() => setDeleteTarget(null)} />
          <div className="relative bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
            <h2 className="text-lg font-semibold text-text-primary mb-3">确认删除</h2>
            <p className="text-sm text-text-secondary mb-4">
              确定要删除模型「{deleteTarget.name}」吗？
            </p>

            {deleteTarget.agentCount > 0 && (
              <div className="bg-warning-50 border border-warning-200 rounded-md p-3 mb-4">
                <p className="text-sm text-warning-700 font-medium mb-1">
                  以下 {deleteTarget.agentCount} 个智能体将被重置为平台默认模型：
                </p>
                <ul className="text-sm text-warning-600 list-disc list-inside">
                  {affectedAgents.map((name, i) => (
                    <li key={i}>{name}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className="flex justify-end gap-3">
              <Button variant="secondary" onClick={() => setDeleteTarget(null)}>取消</Button>
              <Button variant="danger" onClick={confirmDelete} disabled={deleting}>
                {deleting ? '删除中...' : '确认删除'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  )
}

export default ModelListPage
