import React, { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, RefreshCw, Search, ChevronLeft, ChevronRight, ChevronDown } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { AssetCard } from '../components/ui/AssetCard'
import { listAgents } from '../api/agent'
import type { AgentSummaryVO, AgentStatus } from '../api/types'

type CardStatus = 'draft' | 'published' | 'debugging' | 'error' | 'info'

const statusLabel = (s: AgentStatus): CardStatus => {
  if (s === 'DRAFT') return 'draft'
  if (s === 'PUBLISHED') return 'published'
  if (s === 'ARCHIVED') return 'info'
  return 'info'
}

const timeAgo = (iso: string): string => {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return `${Math.floor(days / 30)}月前`
}

type TabType = 'all' | 'created' | 'collab' | 'published'

interface TabConfig {
  key: TabType
  label: string
}

const AgentListPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabType>('all')
  const [currentPage, setCurrentPage] = useState(1)
  const [searchQuery, setSearchQuery] = useState('')
  const [showFilterDropdown, setShowFilterDropdown] = useState(false)
  const [filterStatus, setFilterStatus] = useState<string | null>(null)
  const [agents, setAgents] = useState<AgentSummaryVO[]>([])
  const [total, setTotal] = useState(0)
  const [draftCount, setDraftCount] = useState(0)
  const [publishedCount, setPublishedCount] = useState(0)
  const [archivedCount, setArchivedCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const pageSize = 6

  const fetchAgents = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params: { status?: AgentStatus; page: number; page_size: number } = {
        page: currentPage,
        page_size: pageSize,
      }
      if (activeTab === 'published') {
        params.status = 'PUBLISHED'
      } else if (filterStatus) {
        params.status = filterStatus as AgentStatus
      }
      const result = await listAgents(params)
      setAgents(result.data)
      setTotal(result.total)

      // Fetch counts for each status tab (in a real app, these would come from the API)
      if (activeTab === 'all' && currentPage === 1) {
        try {
          const [draftRes, pubRes, archRes] = await Promise.all([
            listAgents({ status: 'DRAFT', page: 1, page_size: 1 }),
            listAgents({ status: 'PUBLISHED', page: 1, page_size: 1 }),
            listAgents({ status: 'ARCHIVED', page: 1, page_size: 1 }),
          ])
          setDraftCount(draftRes.total)
          setPublishedCount(pubRes.total)
          setArchivedCount(archRes.total)
        } catch {
          // Ignore count fetch errors
        }
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load agents')
    } finally {
      setLoading(false)
    }
  }, [currentPage, activeTab])

  useEffect(() => {
    fetchAgents()
  }, [fetchAgents])

  useEffect(() => {
    setCurrentPage(1)
  }, [activeTab])

  const handleUseAgent = (id: string) => navigate(`/chat?agentId=${id}`)
  const handleEditAgent = (id: string) => navigate(`/agents/edit/${id}`)
  const handleCreateAgent = () => navigate('/agents/create')

  const totalPages = Math.ceil(total / pageSize)

  const tabs: (TabConfig & { count: number })[] = [
    { key: 'all', label: '全部', count: total },
    { key: 'created', label: '草稿', count: draftCount },
    { key: 'published', label: '已发布', count: publishedCount },
    { key: 'collab', label: '已归档', count: archivedCount },
  ]

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }]}>
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        {/* Title Row */}
        <div className="flex items-center gap-3">
          <div className="flex flex-col gap-1 flex-1">
            <h1 className="text-2xl font-bold text-text-primary">我的智能体</h1>
            <p className="text-[13px] text-text-tertiary">共 {total} 个智能体</p>
          </div>
          <div className="flex items-center gap-2">
            {error && (
              <button
                onClick={fetchAgents}
                className="flex items-center gap-1 text-sm text-error-500 hover:text-error-600"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                重试
              </button>
            )}
            {/* Search */}
            <div className="flex items-center gap-2 px-3 py-2 bg-white border border-border-subtle rounded-md w-[220px]">
              <Search className="w-3.5 h-3.5 text-text-tertiary flex-shrink-0" />
              <input
                type="text"
                placeholder="搜索智能体"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="flex-1 bg-transparent text-[13px] outline-none placeholder:text-text-tertiary"
              />
            </div>
            <Button
              variant="primary"
              icon={<Plus className="w-3.5 h-3.5" />}
              onClick={handleCreateAgent}
            >
              创建智能体
            </Button>
          </div>
        </div>

        {/* Tabs and Filters */}
        <div className="flex items-center">
          {/* Pill Tabs */}
          <div className="flex items-center gap-1 flex-1">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`px-3.5 py-2 rounded-md text-[13px] font-medium transition-colors ${
                  activeTab === tab.key
                    ? 'bg-brand-50 text-brand-500 font-semibold'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                {tab.label} {tab.count}
              </button>
            ))}
          </div>
          {/* Filter Dropdown */}
          <div className="relative">
            <button
              onClick={() => setShowFilterDropdown(!showFilterDropdown)}
              className="flex items-center gap-1 px-2.5 py-1.5 bg-white border border-border-subtle rounded text-xs text-text-secondary hover:border-border-strong transition-colors"
            >
              筛选{filterStatus ? '中' : ''}
              <ChevronDown className="w-3 h-3 text-text-tertiary" />
            </button>
            {showFilterDropdown && (
              <div className="absolute top-full right-0 mt-1 w-[160px] bg-white border border-border-subtle rounded-md shadow-lg z-10">
                {[
                  { label: '全部状态', value: null },
                  { label: '已发布', value: 'PUBLISHED' },
                  { label: '草稿', value: 'DRAFT' },
                  { label: '已归档', value: 'ARCHIVED' },
                ].map((opt) => (
                  <button
                    key={opt.label}
                    onClick={() => {
                      setFilterStatus(opt.value)
                      setShowFilterDropdown(false)
                    }}
                    className={`w-full px-3 py-2 text-left text-[13px] hover:bg-gray-50 first:rounded-t-md last:rounded-b-md ${
                      filterStatus === opt.value ? 'bg-brand-50 text-brand-500' : 'text-text-secondary'
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Content */}
        {loading && (
          <div className="flex items-center justify-center py-16">
            <div className="w-6 h-6 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && !loading && (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <p className="text-error-500 text-sm">{error}</p>
            <Button variant="secondary" onClick={fetchAgents}>重新加载</Button>
          </div>
        )}

        {!loading && !error && (
          <>
            <div className="grid grid-cols-3 gap-4">
              {agents.map((agent) => (
                <AssetCard
                  key={agent.id}
                  id={agent.id}
                  name={agent.name}
                  description={agent.description ?? ''}
                  iconText={agent.avatar ?? agent.name[0]}
                  status={statusLabel(agent.status)}
                  toolCount={0}
                  collabCount={0}
                  updatedAt={timeAgo(agent.updatedAt)}
                  onUse={() => handleUseAgent(agent.id)}
                  onEdit={() => handleEditAgent(agent.id)}
                />
              ))}
            </div>

            {agents.length === 0 && (
              <div className="flex flex-col items-center justify-center py-16">
                <p className="text-text-secondary mb-4">暂无智能体</p>
                <Button variant="primary" icon={<Plus className="w-4 h-4" />} onClick={handleCreateAgent}>
                  创建第一个智能体
                </Button>
              </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-1.5 pt-4">
                <button
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage(currentPage - 1)}
                  className="w-8 h-8 rounded-md flex items-center justify-center bg-white border border-border-subtle text-text-secondary hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronLeft className="w-3.5 h-3.5" />
                </button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`w-8 h-8 rounded-md flex items-center justify-center text-[13px] font-semibold transition-colors ${
                      currentPage === page
                        ? 'bg-brand-500 text-white'
                        : 'bg-white border border-border-subtle text-text-secondary hover:bg-gray-50'
                    }`}
                  >
                    {page}
                  </button>
                ))}
                <button
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage(currentPage + 1)}
                  className="w-8 h-8 rounded-md flex items-center justify-center bg-white border border-border-subtle text-text-secondary hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  )
}

export default AgentListPage
