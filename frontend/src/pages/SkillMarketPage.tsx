import React, { useEffect, useState, useCallback } from 'react'

import { Wand2, ArrowRight, Search, Loader2, AlertCircle } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { listMarketItems, importMarketItem } from '../api/market'
import type { MarketItemVO } from '../api/types'

const CATEGORY_TABS = ['全部', '代码生成', '数据处理', '文件操作', '网络请求']

const SkillCard: React.FC<{
  item: MarketItemVO
  onUse: () => void
  importing: boolean
}> = ({ item, onUse, importing }) => {
  return (
    <div className="p-5 bg-white rounded-xl border border-border-subtle flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-lg bg-purple-50 flex items-center justify-center">
          <Wand2 className="w-5 h-5 text-purple-500" />
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-sm font-semibold text-text-primary">{item.assetId}</span>
          <span className="text-xs text-text-tertiary">{item.category ?? '未分类'}</span>
        </div>
      </div>

      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {item.tags ?? '暂无描述'}
      </p>

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
        {importing ? '导入中…' : '使用'}
      </Button>
    </div>
  )
}

const CategoryTab: React.FC<{ name: string; active?: boolean; onClick?: () => void }> = ({
  name,
  active = false,
  onClick,
}) => {
  return (
    <button
      onClick={onClick}
      className={`px-3.5 py-1.5 rounded-md text-sm ${
        active
          ? 'bg-text-primary text-white'
          : 'bg-white text-text-secondary border border-border-subtle hover:bg-gray-50'
      }`}
    >
      {name}
    </button>
  )
}

const SkillMarketPage: React.FC = () => {
  const [items, setItems] = useState<MarketItemVO[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeCategory, setActiveCategory] = useState('全部')
  const [search, setSearch] = useState('')
  const [importingId, setImportingId] = useState<string | null>(null)

  const fetchItems = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await listMarketItems({
        type: 'SKILL',
        category: activeCategory === '全部' ? undefined : activeCategory,
        search: search || undefined,
      })
      setItems(result.data)
      setTotal(result.total)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载失败，请重试')
    } finally {
      setLoading(false)
    }
  }, [activeCategory, search])

  useEffect(() => {
    fetchItems()
  }, [fetchItems])

  const handleImport = async (itemId: string) => {
    setImportingId(itemId)
    try {
      await importMarketItem(itemId)
    } catch {
      // silently handled — could add toast later
    } finally {
      setImportingId(null)
    }
  }

  return (
    <Layout
      breadcrumb={[
        { label: '市场' },
        { label: 'Skill 市场' },
      ]}
    >
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        <div className="rounded-xl bg-[#8B5CF6] p-6 flex flex-col gap-1.5">
          <h1 className="text-[22px] font-bold text-white">发现可复用的 Skill</h1>
          <p className="text-[13px] text-white/80">
            为你的智能体添加即插即用的原子能力 · 收录 {total} 个社区 Skill
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-tertiary" />
            <input
              type="text"
              placeholder="搜索 Skill…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 pr-3 py-1.5 rounded-md text-sm border border-border-subtle bg-white text-text-primary placeholder:text-text-tertiary focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>
          <div className="flex items-center gap-2">
            {CATEGORY_TABS.map((tab) => (
              <CategoryTab
                key={tab}
                name={tab}
                active={activeCategory === tab}
                onClick={() => setActiveCategory(tab)}
              />
            ))}
          </div>
        </div>

        {loading && (
          <div className="flex-1 flex items-center justify-center">
            <Loader2 className="w-6 h-6 animate-spin text-text-tertiary" />
          </div>
        )}

        {error && (
          <div className="flex-1 flex flex-col items-center justify-center gap-3 text-text-secondary">
            <AlertCircle className="w-8 h-8 text-error-500" />
            <p className="text-sm">{error}</p>
            <Button variant="secondary" onClick={fetchItems}>
              重试
            </Button>
          </div>
        )}

        {!loading && !error && items.length === 0 && (
          <div className="flex-1 flex items-center justify-center">
            <p className="text-sm text-text-tertiary">暂无 Skill</p>
          </div>
        )}

        {!loading && !error && items.length > 0 && (
          <div className="grid grid-cols-4 gap-4">
            {items.map((item) => (
              <SkillCard
                key={item.id}
                item={item}
                importing={importingId === item.id}
                onUse={() => handleImport(item.id)}
              />
            ))}
          </div>
        )}
      </div>
    </Layout>
  )
}

export default SkillMarketPage
