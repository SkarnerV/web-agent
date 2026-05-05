import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Filter, Plug, ArrowRight, Loader2, AlertCircle } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { listMarketItems, importMarketItem } from '../api/market'
import type { MarketItemVO } from '../api/types'

const MCPCard: React.FC<{
  item: MarketItemVO
  importing: boolean
  onUse: () => void
}> = ({ item, importing, onUse }) => {
  const tags = item.tags?.split(',').filter(Boolean).map(t => t.trim()) ?? []

  return (
    <div className="p-5 bg-white rounded-xl border border-border-subtle flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center">
          <Plug className="w-5 h-5 text-brand-500" />
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-text-primary">{item.assetId}</span>
            {item.status === 'PUBLISHED' && (
              <Badge variant="published">官方</Badge>
            )}
          </div>
          {item.authorName && (
            <span className="text-xs text-text-tertiary">{item.authorName}</span>
          )}
        </div>
      </div>

      {tags.length > 0 && (
        <div className="flex items-center gap-1.5">
          {tags.slice(0, 2).map((tag) => (
            <span key={tag} className="px-2 py-0.5 bg-gray-100 rounded text-xs text-text-tertiary">
              {tag}
            </span>
          ))}
          {tags.length > 2 && (
            <span className="text-xs text-text-tertiary">+{tags.length - 2}</span>
          )}
        </div>
      )}

      <div className="flex items-center gap-2 text-xs text-text-tertiary">
        {item.avgRating && (
          <>
            <span className="text-warning-500">⭐ {item.avgRating}</span>
            <span>·</span>
          </>
        )}
        <span>{item.useCount}+ 使用</span>
      </div>

      <div className="h-px bg-border-subtle" />

      <Button
        variant="primary"
        onClick={onUse}
        disabled={importing}
        icon={importing ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
      >
        {importing ? '接入中...' : '接入'}
      </Button>
    </div>
  )
}

const CategoryItem: React.FC<{
  name: string
  count: number
  active?: boolean
  onClick?: () => void
}> = ({ name, count, active = false, onClick }) => (
  <button
    onClick={onClick}
    className={`p-4 rounded-xl border text-sm ${
      active
        ? 'bg-brand-50 border-brand-500'
        : 'bg-white border-border-subtle hover:bg-gray-50'
    }`}
  >
    <div className="flex flex-col gap-1">
      <span className={`font-semibold ${active ? 'text-brand-500' : 'text-text-primary'}`}>
        {name}
      </span>
      <span className={`text-xs ${active ? 'text-brand-500' : 'text-text-tertiary'}`}>
        {count} 个服务器
      </span>
    </div>
  </button>
)

const MCPMarketPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')
  const [activeCategory, setActiveCategory] = useState('全部')
  const [items, setItems] = useState<MarketItemVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [importingId, setImportingId] = useState<string | null>(null)

  const fetchItems = useCallback(async (search?: string) => {
    setLoading(true)
    setError(null)
    try {
      const result = await listMarketItems({ type: 'MCP', search: search || undefined })
      setItems(result.data)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const delay = searchQuery ? 300 : 0
    const timer = setTimeout(() => {
      fetchItems(searchQuery || undefined)
    }, delay)
    return () => clearTimeout(timer)
  }, [searchQuery, fetchItems])

  const categories = useMemo(() => {
    const catMap = new Map<string, number>()
    for (const item of items) {
      const cat = item.category || '未分类'
      catMap.set(cat, (catMap.get(cat) ?? 0) + 1)
    }
    return [
      { name: '全部', count: items.length },
      ...Array.from(catMap, ([name, count]) => ({ name, count })),
    ]
  }, [items])

  const filteredItems = useMemo(() => {
    if (activeCategory === '全部') return items
    return items.filter(item => (item.category || '未分类') === activeCategory)
  }, [items, activeCategory])

  useEffect(() => {
    setActiveCategory('全部')
  }, [searchQuery])

  const handleImport = async (itemId: string) => {
    setImportingId(itemId)
    try {
      await importMarketItem(itemId)
      navigate('/mcp')
    } catch (err) {
      setError(err instanceof Error ? err.message : '接入失败')
    } finally {
      setImportingId(null)
    }
  }

  return (
    <Layout breadcrumb={[{ label: '市场' }, { label: 'MCP 市场' }]}>
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold text-text-primary">MCP 市场</h1>
          <p className="text-sm text-text-secondary">官方认证的 MCP Server 集合 · 一键接入</p>
        </div>

        <div className="flex items-center gap-2.5">
          <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-white border border-border-subtle rounded-md">
            <Search className="w-4 h-4 text-text-tertiary" />
            <input
              type="text"
              placeholder="搜索 MCP Server..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1 bg-transparent text-sm outline-none placeholder:text-text-tertiary"
            />
          </div>
          <Button variant="secondary" icon={<Filter className="w-4 h-4" />} iconOnly />
        </div>

        {error && (
          <div className="p-3 bg-error-50 text-error-500 rounded-lg flex items-center gap-2 text-sm">
            <AlertCircle className="w-4 h-4 shrink-0" />
            {error}
          </div>
        )}

        <div className="flex gap-4 h-full">
          <div className="w-[200px] p-4 rounded-xl bg-white border border-border-subtle flex flex-col gap-2">
            {categories.map((cat) => (
              <CategoryItem
                key={cat.name}
                name={cat.name}
                count={cat.count}
                active={activeCategory === cat.name}
                onClick={() => setActiveCategory(cat.name)}
              />
            ))}
          </div>

          <div className="flex-1">
            {loading ? (
              <div className="flex items-center justify-center py-20">
                <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
              </div>
            ) : filteredItems.length === 0 ? (
              <div className="text-center py-20 text-text-tertiary text-sm">
                {searchQuery ? '没有找到匹配的 MCP Server' : '暂无可用的 MCP Server'}
              </div>
            ) : (
              <div className="grid grid-cols-3 gap-4">
                {filteredItems.map((item) => (
                  <MCPCard
                    key={item.id}
                    item={item}
                    importing={importingId === item.id}
                    onUse={() => handleImport(item.id)}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default MCPMarketPage
