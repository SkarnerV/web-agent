import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, BookOpen, FileText, Folder } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'

// Mock data for knowledge bases
const kbData = [
  {
    id: '1',
    name: '产品文档库',
    description: '包含产品说明书、API文档、用户指南等',
    iconBg: 'bg-brand-50',
    docCount: 45,
    updatedAt: '2小时前',
  },
  {
    id: '2',
    name: '技术规范库',
    description: '技术架构文档、设计规范、代码规范',
    iconBg: 'bg-success-50',
    docCount: 23,
    updatedAt: '1天前',
  },
  {
    id: '3',
    name: '培训资料库',
    description: '员工培训文档、操作手册、视频教程',
    iconBg: 'bg-warning-50',
    docCount: 67,
    updatedAt: '3天前',
  },
  {
    id: '4',
    name: 'FAQ知识库',
    description: '常见问题解答、客服支持文档',
    iconBg: 'bg-error-50',
    docCount: 89,
    updatedAt: '5天前',
  },
  {
    id: '5',
    name: '合同文档库',
    description: '合同模板、法律文档、协议文件',
    iconBg: 'bg-gray-100',
    docCount: 12,
    updatedAt: '1周前',
  },
]

// KB Card Component
const KBCard: React.FC<{
  name: string
  description: string
  iconBg: string
  docCount: number
  updatedAt: string
  onClick?: () => void
}> = ({ name, description, iconBg, docCount, updatedAt, onClick }) => {
  return (
    <div 
      className="w-full p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3 hover:shadow-md transition-shadow cursor-pointer"
      onClick={onClick}
    >
      {/* Header Row */}
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${iconBg}`}>
          <Folder className="w-5 h-5 text-gray-600" />
        </div>
        <div className="flex-1 flex flex-col gap-0.5">
          <span className="text-sm font-semibold text-text-primary">{name}</span>
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {description}
      </p>

      {/* Meta */}
      <div className="flex items-center gap-3 text-sm text-text-tertiary">
        <div className="flex items-center gap-1">
          <FileText className="w-4 h-4" />
          <span>{docCount} 个文档</span>
        </div>
        <span>· {updatedAt}</span>
      </div>

      {/* Divider */}
      <div className="h-px bg-border-subtle" />

      {/* Actions */}
      <div className="flex gap-2">
        <Button variant="primary" size="sm">查看</Button>
        <Button variant="secondary" size="sm">管理</Button>
      </div>
    </div>
  )
}

// KnowledgeBasePage Component
const KnowledgeBasePage: React.FC = () => {
  const navigate = useNavigate()

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '知识库' }]}>
      <div className="p-8">
        {/* Title Row */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-text-primary">知识库</h1>
          <Button 
            variant="primary" 
            icon={<Plus className="w-4 h-4" />}
          >
            新建知识库
          </Button>
        </div>

        {/* Grid - 3 columns */}
        <div className="grid grid-cols-3 gap-6">
          {kbData.map((kb) => (
            <KBCard
              key={kb.id}
              name={kb.name}
              description={kb.description}
              iconBg={kb.iconBg}
              docCount={kb.docCount}
              updatedAt={kb.updatedAt}
              onClick={() => navigate(`/knowledge/${kb.id}`)}
            />
          ))}
        </div>

        {/* Empty state if no KB */}
        {kbData.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16">
            <BookOpen className="w-12 h-12 text-text-tertiary mb-4" />
            <p className="text-text-secondary mb-4">还没有创建任何知识库</p>
            <Button 
              variant="primary" 
              icon={<Plus className="w-4 h-4" />}
            >
              创建第一个知识库
            </Button>
          </div>
        )}

        {/* Summary */}
        <div className="mt-6 flex items-center gap-4 text-sm text-text-tertiary">
          <span>共 {kbData.length} 个知识库</span>
          <span>· {kbData.reduce((sum, kb) => sum + kb.docCount, 0)} 个文档</span>
        </div>
      </div>
    </Layout>
  )
}

export default KnowledgeBasePage