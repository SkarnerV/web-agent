import React, { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Wand2, Search, ChevronLeft, ChevronRight } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { listSkills, deleteSkill, exportSkill } from '../api/skill'
import type { SkillSummaryVO, SkillListParams } from '../api/types'

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

// ── SkillCard ──

const SkillCard: React.FC<{
  skill: SkillSummaryVO
  onUse?: () => void
  onEdit?: () => void
  onDelete?: () => void
  onExport?: () => void
}> = ({ skill, onUse, onEdit, onDelete, onExport }) => {
  return (
    <div className="p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3 hover:shadow-md transition-shadow">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-lg flex items-center justify-center bg-brand-50">
          <Wand2 className="w-5 h-5 text-brand-500" />
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-sm font-semibold text-text-primary">{skill.name}</span>
          <Badge variant={skill.status === 'PUBLISHED' ? 'published' : 'draft'}>
            {skill.status === 'PUBLISHED' ? '已发布' : '草稿'}
          </Badge>
        </div>
      </div>

      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {skill.description || '暂无描述'}
      </p>

      <div className="flex items-center gap-2 text-xs text-text-tertiary">
        <span>{timeAgo(skill.createdAt)}</span>
      </div>

      <div className="h-px bg-border-subtle" />

      <div className="flex items-center gap-3 text-[13px] text-brand-500">
        <button onClick={onUse} className="hover:underline">使用</button>
        <button onClick={onEdit} className="hover:underline">编辑</button>
        <button onClick={onExport} className="hover:underline">导出</button>
        <button onClick={onDelete} className="hover:underline text-error-500">删除</button>
      </div>
    </div>
  )
}

// ── Page ──

type TabKey = 'all' | 'published' | 'draft'

const statusMap: Record<TabKey, string | undefined> = {
  all: undefined,
  published: 'PUBLISHED',
  draft: 'DRAFT',
}

const SkillListPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabKey>('all')
  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [skills, setSkills] = useState<SkillSummaryVO[]>([])
  const [total, setTotal] = useState(0)
  const [currentPage, setCurrentPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const pageSize = 9

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchQuery), 300)
    return () => clearTimeout(timer)
  }, [searchQuery])

  const fetchSkills = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params: SkillListParams = {
        page: currentPage,
        page_size: pageSize,
      }
      const status = statusMap[activeTab]
      if (status) params.status = status
      if (debouncedSearch) params.search = debouncedSearch
      const result = await listSkills(params)
      setSkills(result.data)
      setTotal(result.total)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载 Skill 列表失败')
    } finally {
      setLoading(false)
    }
  }, [currentPage, activeTab, debouncedSearch])

  useEffect(() => {
    fetchSkills()
  }, [fetchSkills])

  useEffect(() => {
    setCurrentPage(1)
  }, [activeTab, debouncedSearch])

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(`确定要删除 "${name}" 吗？`)) return
    try {
      await deleteSkill(id)
      fetchSkills()
    } catch (e) {
      alert(e instanceof Error ? e.message : '删除失败')
    }
  }

  const handleExport = async (id: string, name: string) => {
    try {
      const data = await exportSkill(id)
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${name}.json`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      alert(e instanceof Error ? e.message : '导出失败')
    }
  }

  const totalPages = Math.ceil(total / pageSize)

  const tabs: { key: TabKey; label: string }[] = [
    { key: 'all', label: '全部' },
    { key: 'published', label: '已发布' },
    { key: 'draft', label: '草稿' },
  ]

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: 'Skill' }]}>
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-semibold text-text-primary">我的 Skills</h1>
          <Button
            variant="primary"
            icon={<Plus className="w-4 h-4" />}
            onClick={() => navigate('/skills/create')}
          >
            创建 Skill
          </Button>
        </div>

        <div className="flex items-center gap-3">
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
                {tab.label}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-2 px-3 py-2 bg-white border border-border-subtle rounded-md w-[220px]">
            <Search className="w-3.5 h-3.5 text-text-tertiary flex-shrink-0" />
            <input
              type="text"
              placeholder="搜索 Skill..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1 bg-transparent text-[13px] outline-none placeholder:text-text-tertiary"
            />
          </div>
        </div>

        {loading && (
          <div className="flex items-center justify-center py-16">
            <div className="w-6 h-6 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && !loading && (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <p className="text-error-500 text-sm">{error}</p>
            <Button variant="secondary" onClick={fetchSkills}>重新加载</Button>
          </div>
        )}

        {!loading && !error && (
          <>
            <div className="grid grid-cols-3 gap-4">
              {skills.map((skill) => (
                <SkillCard
                  key={skill.id}
                  skill={skill}
                  onUse={() => navigate('/chat')}
                  onEdit={() => navigate(`/skills/edit/${skill.id}`)}
                  onDelete={() => handleDelete(skill.id, skill.name)}
                  onExport={() => handleExport(skill.id, skill.name)}
                />
              ))}
              <button
                onClick={() => navigate('/skills/create')}
                className="p-5 bg-white rounded-lg border-2 border-dashed border-border-subtle flex flex-col items-center justify-center gap-2 hover:border-brand-500 hover:bg-brand-50/30 transition-colors min-h-[200px]"
              >
                <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center">
                  <Plus className="w-5 h-5 text-brand-500" />
                </div>
                <span className="text-sm font-medium text-brand-500">创建新 Skill</span>
              </button>
            </div>

            {skills.length === 0 && (
              <div className="flex flex-col items-center justify-center py-16">
                <Wand2 className="w-12 h-12 text-text-tertiary mb-4" />
                <p className="text-text-secondary mb-4">没有匹配的 Skill</p>
                <Button variant="primary" icon={<Plus className="w-4 h-4" />} onClick={() => navigate('/skills/create')}>
                  创建第一个 Skill
                </Button>
              </div>
            )}

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

export default SkillListPage
