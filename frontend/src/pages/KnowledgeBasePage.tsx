import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, BookOpen, FileText, Loader2, X } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { listKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase } from '../api/knowledge'
import type { KnowledgeBaseSummaryVO } from '../api/types'

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}

const KBCard: React.FC<{
  kb: KnowledgeBaseSummaryVO
  onClick?: () => void
  onDelete?: () => void
}> = ({ kb, onClick, onDelete }) => {
  return (
    <div 
      className="w-full p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3 hover:shadow-md transition-shadow cursor-pointer"
      onClick={onClick}
    >
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-lg flex items-center justify-center bg-brand-50">
          <BookOpen className="w-5 h-5 text-brand-500" />
        </div>
        <div className="flex-1 flex flex-col gap-0.5">
          <span className="text-sm font-semibold text-text-primary">{kb.name}</span>
        </div>
      </div>

      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {kb.description || '暂无描述'}
      </p>

      <div className="flex items-center gap-3 text-sm text-text-tertiary">
        <div className="flex items-center gap-1">
          <FileText className="w-4 h-4" />
          <span>{kb.docCount} 个文档</span>
        </div>
        <span>· {formatBytes(kb.totalSizeBytes)}</span>
      </div>

      <div className="h-px bg-border-subtle" />

      <div className="flex gap-2">
        <Button variant="primary" size="sm" onClick={() => onClick?.()}>查看</Button>
        <Button variant="ghost" size="sm" onClick={() => onDelete?.()}>删除</Button>
      </div>
    </div>
  )
}

const KnowledgeBasePage: React.FC = () => {
  const navigate = useNavigate()
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseSummaryVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [createName, setCreateName] = useState('')
  const [createDescription, setCreateDescription] = useState('')
  const [creating, setCreating] = useState(false)

  const fetchData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await listKnowledgeBases()
      setKnowledgeBases(result.data)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchData() }, [fetchData])

  const handleCreate = async () => {
    if (!createName.trim()) return
    try {
      setCreating(true)
      await createKnowledgeBase({ name: createName.trim(), description: createDescription.trim() || undefined })
      setShowCreateDialog(false)
      setCreateName('')
      setCreateDescription('')
      fetchData()
    } catch (err) {
      alert(err instanceof Error ? err.message : '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const handleDelete = async (kb: KnowledgeBaseSummaryVO) => {
    if (!window.confirm(`确定要删除知识库「${kb.name}」吗？此操作不可恢复。`)) return
    try {
      await deleteKnowledgeBase(kb.id)
      fetchData()
    } catch (err) {
      alert(err instanceof Error ? err.message : '删除失败')
    }
  }

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '知识库' }]}>
      <div className="p-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-text-primary">知识库</h1>
          <Button 
            variant="primary" 
            icon={<Plus className="w-4 h-4" />}
            onClick={() => setShowCreateDialog(true)}
          >
            新建知识库
          </Button>
        </div>

        {loading && (
          <div className="flex items-center justify-center py-16">
            <Loader2 className="w-8 h-8 text-brand-500 animate-spin" />
          </div>
        )}

        {error && (
          <div className="flex flex-col items-center justify-center py-16">
            <p className="text-error-500 mb-4">{error}</p>
            <Button variant="secondary" onClick={fetchData}>重试</Button>
          </div>
        )}

        {!loading && !error && knowledgeBases.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16">
            <BookOpen className="w-12 h-12 text-text-tertiary mb-4" />
            <p className="text-text-secondary mb-4">还没有创建任何知识库</p>
            <Button 
              variant="primary" 
              icon={<Plus className="w-4 h-4" />}
              onClick={() => setShowCreateDialog(true)}
            >
              创建第一个知识库
            </Button>
          </div>
        )}

        {!loading && !error && knowledgeBases.length > 0 && (
          <>
            <div className="grid grid-cols-3 gap-6">
              {knowledgeBases.map((kb) => (
                <KBCard
                  key={kb.id}
                  kb={kb}
                  onClick={() => navigate(`/knowledge/${kb.id}`)}
                  onDelete={() => handleDelete(kb)}
                />
              ))}
            </div>

            <div className="mt-6 flex items-center gap-4 text-sm text-text-tertiary">
              <span>共 {knowledgeBases.length} 个知识库</span>
              <span>· {knowledgeBases.reduce((sum, kb) => sum + kb.docCount, 0)} 个文档</span>
            </div>
          </>
        )}

        {showCreateDialog && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-text-primary">新建知识库</h2>
                <button onClick={() => setShowCreateDialog(false)} className="text-text-tertiary hover:text-text-primary">
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="flex flex-col gap-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">名称</label>
                  <input
                    type="text"
                    className="w-full px-3 py-2 border border-border-subtle rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                    placeholder="输入知识库名称"
                    value={createName}
                    onChange={(e) => setCreateName(e.target.value)}
                    autoFocus
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">描述（可选）</label>
                  <textarea
                    className="w-full px-3 py-2 border border-border-subtle rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 resize-none"
                    placeholder="输入知识库描述"
                    rows={3}
                    value={createDescription}
                    onChange={(e) => setCreateDescription(e.target.value)}
                  />
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-6">
                <Button variant="secondary" onClick={() => setShowCreateDialog(false)}>取消</Button>
                <Button variant="primary" onClick={handleCreate} disabled={!createName.trim() || creating}>
                  {creating ? '创建中...' : '创建'}
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}

export default KnowledgeBasePage
