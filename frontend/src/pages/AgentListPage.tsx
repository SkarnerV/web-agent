import React, { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, RefreshCw } from 'lucide-react'
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

const AgentListPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabType>('all')
  const [currentPage, setCurrentPage] = useState(1)
  const [agents, setAgents] = useState<AgentSummaryVO[]>([])
  const [total, setTotal] = useState(0)
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
      }
      const result = await listAgents(params)
      setAgents(result.data)
      setTotal(result.total)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load agents')
    } finally {
      setLoading(false)
    }
  }, [currentPage, activeTab])

  useEffect(() => {
    fetchAgents()
  }, [fetchAgents])

  // Reset to page 1 when tab changes
  useEffect(() => {
    setCurrentPage(1)
  }, [activeTab])

  const handleUseAgent = (id: string) => {
    navigate(`/chat?agentId=${id}`)
  }

  const handleEditAgent = (id: string) => {
    navigate(`/agents/edit/${id}`)
  }

  const handleCreateAgent = () => {
    navigate('/agents/create')
  }

  const totalPages = Math.ceil(total / pageSize)

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }]}>
      <div className="p-8 flex flex-col gap-5">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-semibold text-text-primary">我的智能体</h1>
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
            <Button
              variant="primary"
              icon={<Plus className="w-4 h-4" />}
              onClick={handleCreateAgent}
            >
              创建智能体
            </Button>
          </div>
        </div>

        <div className="flex items-center gap-4">
          {[
            { key: 'all', label: '全部' },
            { key: 'created', label: '我创建的' },
            { key: 'collab', label: '协作的' },
            { key: 'published', label: '已发布' },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as TabType)}
              className={`px-3 py-2 text-sm font-medium transition-colors border-b-2 ${
                activeTab === tab.key
                  ? 'text-brand-500 border-brand-500'
                  : 'text-text-secondary border-transparent hover:text-text-primary'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {loading && (
          <div className="flex items-center justify-center py-16">
            <div className="w-6 h-6 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && (
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
                <Button
                  variant="primary"
                  icon={<Plus className="w-4 h-4" />}
                  onClick={handleCreateAgent}
                >
                  创建第一个智能体
                </Button>
              </div>
            )}

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-1.5 pt-4">
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`w-8 h-8 rounded-md flex items-center justify-center text-sm font-medium transition-colors ${
                      currentPage === page
                        ? 'bg-brand-500 text-white'
                        : 'bg-white border border-border-subtle text-text-secondary hover:bg-gray-50'
                    }`}
                  >
                    {page}
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  )
}

export default AgentListPage
