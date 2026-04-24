import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { 
  ArrowLeft, 
  Upload, 
  UploadCloud, 
  Filter, 
  MoreHorizontal,
  FileText,
  File,
  Trash2,
  Download,
  Eye,
  CheckCircle,
  Clock,
  AlertCircle,
  Folder
} from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { Input } from '../components/ui/Input'

// Mock data for documents
const documentsData = [
  {
    id: '1',
    name: '产品使用说明书.pdf',
    type: 'PDF',
    size: '2.4 MB',
    status: 'processed' as const,
    uploadedAt: '2小时前',
  },
  {
    id: '2',
    name: 'API接口文档.md',
    type: 'Markdown',
    size: '156 KB',
    status: 'processed' as const,
    uploadedAt: '1天前',
  },
  {
    id: '3',
    name: '用户培训手册.docx',
    type: 'Word',
    size: '8.2 MB',
    status: 'processing' as const,
    uploadedAt: '3小时前',
  },
  {
    id: '4',
    name: '系统架构设计图.png',
    type: 'Image',
    size: '1.1 MB',
    status: 'processed' as const,
    uploadedAt: '2天前',
  },
  {
    id: '5',
    name: '数据分析报告.xlsx',
    type: 'Excel',
    size: '4.5 MB',
    status: 'error' as const,
    uploadedAt: '5天前',
  },
  {
    id: '6',
    name: '常见问题解答.txt',
    type: 'Text',
    size: '45 KB',
    status: 'processed' as const,
    uploadedAt: '1周前',
  },
]

// KB info
const kbInfo = {
  id: '1',
  name: '产品文档库',
  description: '包含产品说明书、API文档、用户指南等核心产品文档',
  docCount: 45,
  totalSize: '128 MB',
}

// KBDocumentsPage Component
const KBDocumentsPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')
  const [isDragging, setIsDragging] = useState(false)

  const getStatusBadge = (status: 'processed' | 'processing' | 'error') => {
    switch (status) {
      case 'processed':
        return <Badge variant="published">已处理</Badge>
      case 'processing':
        return <Badge variant="info">处理中</Badge>
      case 'error':
        return <Badge variant="error">错误</Badge>
      default:
        return <Badge variant="draft">未知</Badge>
    }
  }

  const getStatusIcon = (status: 'processed' | 'processing' | 'error') => {
    switch (status) {
      case 'processed':
        return <CheckCircle className="w-4 h-4 text-success-500" />
      case 'processing':
        return <Clock className="w-4 h-4 text-brand-500" />
      case 'error':
        return <AlertCircle className="w-4 h-4 text-error-500" />
      default:
        return null
    }
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'PDF':
        return <FileText className="w-4 h-4 text-red-500" />
      case 'Markdown':
        return <FileText className="w-4 h-4 text-brand-500" />
      case 'Word':
        return <FileText className="w-4 h-4 text-blue-500" />
      case 'Image':
        return <File className="w-4 h-4 text-green-500" />
      case 'Excel':
        return <File className="w-4 h-4 text-emerald-500" />
      default:
        return <File className="w-4 h-4 text-gray-500" />
    }
  }

  // Filter documents
  const filteredDocs = documentsData.filter(doc => {
    if (searchQuery && !doc.name.toLowerCase().includes(searchQuery.toLowerCase())) {
      return false
    }
    return true
  })

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = () => {
    setIsDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    // Handle file upload logic here
  }

  return (
    <Layout breadcrumb={[{ label: '知识库' }, { label: kbInfo.name }]}>
      <div className="p-8">
        {/* Back Button */}
        <button 
          onClick={() => navigate('/knowledge')}
          className="flex items-center gap-2 text-sm text-text-tertiary hover:text-text-secondary mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          返回知识库列表
        </button>

        {/* Header Card */}
        <div className="p-6 bg-white rounded-lg border border-border-subtle mb-6">
          <div className="flex items-start justify-between">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 rounded-lg bg-brand-50 flex items-center justify-center">
                <Folder className="w-6 h-6 text-brand-500" />
              </div>
              <div className="flex-1">
                <h1 className="text-lg font-semibold text-text-primary mb-1">{kbInfo.name}</h1>
                <p className="text-sm text-text-secondary mb-3">{kbInfo.description}</p>
                <div className="flex items-center gap-4 text-sm text-text-tertiary">
                  <span>{kbInfo.docCount} 个文档</span>
                  <span>· 总大小 {kbInfo.totalSize}</span>
                </div>
              </div>
            </div>
            <Button variant="secondary" icon={<MoreHorizontal className="w-4 h-4" />} iconOnly />
          </div>
        </div>

        {/* Drop Zone */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`p-8 bg-white rounded-lg border-2 mb-6 flex flex-col items-center justify-center transition-colors ${
            isDragging 
              ? 'border-brand-500 bg-brand-50' 
              : 'border-border-subtle hover:border-border-strong'
          }`}
        >
          <UploadCloud className={`w-12 h-12 mb-4 ${isDragging ? 'text-brand-500' : 'text-text-tertiary'}`} />
          <p className="text-sm font-medium text-text-primary mb-1">拖拽文件上传或点击选择</p>
          <p className="text-xs text-text-tertiary mb-4">支持 PDF, Markdown, Word, Excel, 图片等格式</p>
          <Button variant="primary" size="sm" icon={<Upload className="w-4 h-4" />}>
            选择文件
          </Button>
        </div>

        {/* Toolbar */}
        <div className="flex items-center gap-4 mb-4">
          <div className="flex-1 max-w-[300px]">
            <Input
              type="search"
              placeholder="搜索文档..."
              value={searchQuery}
              onChange={setSearchQuery}
            />
          </div>
          <Button variant="secondary" size="sm" icon={<Filter className="w-4 h-4" />}>
            筛选
          </Button>
          <Button variant="secondary" size="sm" icon={<Upload className="w-4 h-4" />}>
            批量上传
          </Button>
          <Button variant="ghost" size="sm" icon={<Trash2 className="w-4 h-4" />}>
            清空
          </Button>
        </div>

        {/* Document Table */}
        <div className="bg-white rounded-xl border border-border-subtle overflow-hidden">
          {/* Table Header */}
          <div className="grid grid-cols-6 gap-4 px-6 py-4 bg-gray-50 border-b border-border-subtle text-sm font-medium text-text-secondary">
            <div className="col-span-2">名称</div>
            <div>类型</div>
            <div>大小</div>
            <div>状态</div>
            <div>操作</div>
          </div>

          {/* Table Body */}
          {filteredDocs.map((doc) => (
            <div 
              key={doc.id}
              className="grid grid-cols-6 gap-4 px-6 py-4 border-b border-border-subtle text-sm hover:bg-bg-hover transition-colors items-center"
            >
              {/* Name */}
              <div className="col-span-2 flex items-center gap-3">
                {getTypeIcon(doc.type)}
                <span className="font-medium text-text-primary truncate">{doc.name}</span>
              </div>

              {/* Type */}
              <div className="text-text-secondary">
                <span className="px-2 py-0.5 bg-gray-100 rounded text-xs">{doc.type}</span>
              </div>

              {/* Size */}
              <div className="text-text-tertiary">{doc.size}</div>

              {/* Status */}
              <div className="flex items-center gap-2">
                {getStatusIcon(doc.status)}
                {getStatusBadge(doc.status)}
              </div>

              {/* Actions */}
              <div className="flex gap-1">
                <Button variant="ghost" size="sm" iconOnly icon={<Eye className="w-4 h-4" />} />
                <Button variant="ghost" size="sm" iconOnly icon={<Download className="w-4 h-4" />} />
                <Button variant="ghost" size="sm" iconOnly icon={<Trash2 className="w-4 h-4" />} />
              </div>
            </div>
          ))}

          {/* Empty state */}
          {filteredDocs.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12">
              <FileText className="w-10 h-10 text-text-tertiary mb-3" />
              <p className="text-sm text-text-secondary">
                {searchQuery ? '没有找到匹配的文档' : '暂无文档'}
              </p>
            </div>
          )}
        </div>

        {/* Summary */}
        <div className="mt-4 flex items-center gap-4 text-sm text-text-tertiary">
          <span>显示 {filteredDocs.length} 个文档</span>
          <span>· 共 {documentsData.length} 个</span>
        </div>
      </div>
    </Layout>
  )
}

export default KBDocumentsPage