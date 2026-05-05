import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Package, Code, Loader2, AlertCircle } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { listMarketItems, importMarketItem } from '../api/market'
import { createMcp } from '../api/mcp'
import type { MarketItemVO, McpCreateRequest } from '../api/types'

const MCPMarketCard: React.FC<{
  item: MarketItemVO
  installingId: string | null
  onInstall: (id: string) => void
}> = ({ item, installingId, onInstall }) => {
  const installing = installingId === item.id
  const tags = item.tags?.split(',').filter(Boolean).map(t => t.trim()) ?? []

  return (
    <div className="p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3 hover:shadow-md transition-shadow">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center text-lg font-semibold text-brand-600">
          {(item.assetId || item.id)[0]?.toUpperCase()}
        </div>
        <div className="flex-1 flex flex-col gap-0.5">
          <span className="text-sm font-semibold text-text-primary">{item.assetId}</span>
          {item.authorName && (
            <span className="text-xs text-text-tertiary">{item.authorName}</span>
          )}
        </div>
      </div>

      {tags.length > 0 && (
        <div className="flex items-center gap-1.5 flex-wrap">
          {tags.slice(0, 3).map((tag) => (
            <span key={tag} className="px-2 py-0.5 bg-gray-100 rounded text-xs text-text-tertiary">
              {tag}
            </span>
          ))}
        </div>
      )}

      <div className="flex items-center gap-3 text-sm text-text-tertiary">
        {item.category && <Badge variant="info">{item.category}</Badge>}
        <span>{item.useCount} 次使用</span>
      </div>

      <div className="h-px bg-border-subtle" />

      <Button
        variant="primary"
        size="sm"
        onClick={() => onInstall(item.id)}
        disabled={installing}
        icon={installing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
      >
        {installing ? '安装中...' : '安装'}
      </Button>
    </div>
  )
}

const MCPAddPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<'market' | 'manual'>('market')

  const [marketItems, setMarketItems] = useState<MarketItemVO[]>([])
  const [marketLoading, setMarketLoading] = useState(false)
  const [marketError, setMarketError] = useState<string | null>(null)
  const [installingId, setInstallingId] = useState<string | null>(null)

  const [formData, setFormData] = useState({
    name: '',
    url: '',
    protocol: 'SSE',
    authHeaders: '',
  })
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const fetchMarketItems = useCallback(async () => {
    setMarketLoading(true)
    setMarketError(null)
    try {
      const result = await listMarketItems({ type: 'MCP' })
      setMarketItems(result.data)
    } catch (err) {
      setMarketError(err instanceof Error ? err.message : '加载市场数据失败')
    } finally {
      setMarketLoading(false)
    }
  }, [])

  useEffect(() => {
    if (activeTab === 'market') {
      fetchMarketItems()
    }
  }, [activeTab, fetchMarketItems])

  const handleInstall = async (itemId: string) => {
    setInstallingId(itemId)
    try {
      await importMarketItem(itemId)
      navigate('/mcp')
    } catch (err) {
      setMarketError(err instanceof Error ? err.message : '安装失败')
    } finally {
      setInstallingId(null)
    }
  }

  const handleCreate = async () => {
    if (!formData.name.trim()) return
    setCreating(true)
    setCreateError(null)
    try {
      const data: McpCreateRequest = {
        name: formData.name.trim(),
        url: formData.url.trim() || undefined,
        protocol: formData.protocol,
        authHeaders: formData.authHeaders.trim() || undefined,
      }
      await createMcp(data)
      navigate('/mcp')
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const updateField = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  return (
    <Layout breadcrumb={[{ label: 'MCP' }, { label: '添加 MCP Server' }]}>
      <div className="p-10">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-text-primary">添加 MCP Server</h1>
          <p className="text-[13px] text-text-secondary">
            从市场安装预配置的 MCP Server，或手动配置自定义连接
          </p>
        </div>

        <div className="inline-flex bg-gray-100 rounded-lg p-1 mt-6 mb-6">
          <button
            onClick={() => setActiveTab('market')}
            className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              activeTab === 'market'
                ? 'bg-white text-text-primary shadow-sm'
                : 'text-text-tertiary hover:text-text-secondary'
            }`}
          >
            <Package className="w-4 h-4" />
            从市场安装
          </button>
          <button
            onClick={() => setActiveTab('manual')}
            className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              activeTab === 'manual'
                ? 'bg-white text-text-primary shadow-sm'
                : 'text-text-tertiary hover:text-text-secondary'
            }`}
          >
            <Code className="w-4 h-4" />
            手动配置
          </button>
        </div>

        {activeTab === 'market' ? (
          <>
            {marketError && (
              <div className="mb-4 p-3 bg-error-50 text-error-500 rounded-lg flex items-center gap-2 text-sm">
                <AlertCircle className="w-4 h-4 shrink-0" />
                {marketError}
              </div>
            )}
            {marketLoading ? (
              <div className="flex items-center justify-center py-20">
                <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
              </div>
            ) : marketItems.length === 0 && !marketError ? (
              <div className="text-center py-20 text-text-tertiary text-sm">
                暂无可用的 MCP Server
              </div>
            ) : (
              <div className="grid grid-cols-3 gap-6">
                {marketItems.map((item) => (
                  <MCPMarketCard
                    key={item.id}
                    item={item}
                    installingId={installingId}
                    onInstall={handleInstall}
                  />
                ))}
              </div>
            )}
          </>
        ) : (
          <div className="max-w-[600px]">
            {createError && (
              <div className="mb-4 p-3 bg-error-50 text-error-500 rounded-lg flex items-center gap-2 text-sm">
                <AlertCircle className="w-4 h-4 shrink-0" />
                {createError}
              </div>
            )}

            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-text-primary">名称 *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => updateField('name', e.target.value)}
                  placeholder="例如：My MCP Server"
                  className="w-full px-3 py-2 bg-white border border-border-strong rounded-lg text-sm text-text-primary placeholder:text-text-tertiary focus:outline-none focus:border-brand-500"
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-text-primary">URL</label>
                <input
                  type="text"
                  value={formData.url}
                  onChange={(e) => updateField('url', e.target.value)}
                  placeholder="例如：https://mcp.example.com/sse"
                  className="w-full px-3 py-2 bg-white border border-border-strong rounded-lg text-sm text-text-primary placeholder:text-text-tertiary focus:outline-none focus:border-brand-500"
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-text-primary">协议</label>
                <select
                  value={formData.protocol}
                  onChange={(e) => updateField('protocol', e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-border-strong rounded-lg text-sm text-text-primary focus:outline-none focus:border-brand-500"
                >
                  <option value="SSE">SSE</option>
                  <option value="STREAMABLE_HTTP">Streamable HTTP</option>
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-text-primary">Auth Headers</label>
                <textarea
                  value={formData.authHeaders}
                  onChange={(e) => updateField('authHeaders', e.target.value)}
                  placeholder='{"Authorization": "Bearer xxx"}'
                  rows={3}
                  className="w-full px-3 py-2 bg-white border border-border-strong rounded-lg text-sm font-mono text-text-primary placeholder:text-text-tertiary resize-none focus:outline-none focus:border-brand-500"
                />
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <Button variant="secondary" onClick={() => navigate('/mcp')}>
                取消
              </Button>
              <Button
                variant="primary"
                onClick={handleCreate}
                disabled={!formData.name.trim() || creating}
              >
                {creating ? '创建中...' : '添加 Server'}
              </Button>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}

export default MCPAddPage
