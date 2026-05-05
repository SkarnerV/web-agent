import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
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
  RefreshCw,
  CheckCircle,
  Clock,
  AlertCircle,
  BookOpen,
  Loader2
} from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { Input } from '../components/ui/Input'
import { getKnowledgeBase, listDocuments, uploadDocument, deleteDocument, reindexDocument } from '../api/knowledge'
import type { KnowledgeBaseDetailVO, KbDocumentVO } from '../api/types'

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}

type DocStatus = 'processed' | 'processing' | 'error'

function mapIndexStatus(indexStatus?: string): DocStatus {
  switch (indexStatus) {
    case 'COMPLETED': return 'processed'
    case 'INDEXING': return 'processing'
    case 'FAILED': return 'error'
    default: return 'processing'
  }
}

const KBDocumentsPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [kbInfo, setKbInfo] = useState<KnowledgeBaseDetailVO | null>(null)
  const [documents, setDocuments] = useState<KbDocumentVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [isDragging, setIsDragging] = useState(false)
  const [uploading, setUploading] = useState(false)

  const fetchData = useCallback(async () => {
    if (!id) return
    try {
      setLoading(true)
      setError(null)
      const [kb, docs] = await Promise.all([
        getKnowledgeBase(id),
        listDocuments(id),
      ])
      setKbInfo(kb)
      setDocuments(docs.data)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { fetchData() }, [fetchData])

  const handleUpload = async (files: FileList | null) => {
    if (!files || files.length === 0 || !id) return
    try {
      setUploading(true)
      for (const file of Array.from(files)) {
        await uploadDocument(id, file)
      }
      fetchData()
    } catch (err) {
      alert(err instanceof Error ? err.message : '上传失败')
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (doc: KbDocumentVO) => {
    if (!id) return
    if (!window.confirm(`确定要删除文档「${doc.filename}」吗？`)) return
    try {
      await deleteDocument(id, doc.id)
      fetchData()
    } catch (err) {
      alert(err instanceof Error ? err.message : '删除失败')
    }
  }

  const handleReindex = async (doc: KbDocumentVO) => {
    if (!id) return
    try {
      await reindexDocument(id, doc.id)
      fetchData()
    } catch (err) {
      alert(err instanceof Error ? err.message : '重新索引失败')
    }
  }

  const getStatusBadge = (status: DocStatus) => {
    switch (status) {
      case 'processed':
        return <Badge variant="published">已处理</Badge>
      case 'processing':
        return <Badge variant="info">处理中</Badge>
      case 'error':
        return <Badge variant="error">错误</Badge>
    }
  }

  const getStatusIcon = (status: DocStatus) => {
    switch (status) {
      case 'processed':
        return <CheckCircle className="w-4 h-4 text-success-500" />
      case 'processing':
        return <Clock className="w-4 h-4 text-brand-500" />
      case 'error':
        return <AlertCircle className="w-4 h-4 text-error-500" />
    }
  }

  const getTypeIcon = (mimeType?: string) => {
    if (!mimeType) return <File className="w-4 h-4 text-gray-500" />
    if (mimeType.includes('pdf')) return <FileText className="w-4 h-4 text-red-500" />
    if (mimeType.includes('markdown') || mimeType.includes('text')) return <FileText className="w-4 h-4 text-brand-500" />
    if (mimeType.includes('word') || mimeType.includes('document')) return <FileText className="w-4 h-4 text-blue-500" />
    if (mimeType.includes('image')) return <File className="w-4 h-4 text-green-500" />
    if (mimeType.includes('sheet') || mimeType.includes('excel')) return <File className="w-4 h-4 text-emerald-500" />
    return <File className="w-4 h-4 text-gray-500" />
  }

  const filteredDocs = documents.filter(doc => {
    if (searchQuery && !doc.filename.toLowerCase().includes(searchQuery.toLowerCase())) {
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
    handleUpload(e.dataTransfer.files)
  }

  if (loading) {
    return (
      <Layout breadcrumb={[{ label: '知识库' }, { label: '加载中...' }]}>
        <div className="flex items-center justify-center py-32">
          <Loader2 className="w-8 h-8 text-brand-500 animate-spin" />
        </div>
      </Layout>
    )
  }

  if (error || !kbInfo) {
    return (
      <Layout breadcrumb={[{ label: '知识库' }, { label: '错误' }]}>
        <div className="flex flex-col items-center justify-center py-32">
          <p className="text-error-500 mb-4">{error || '知识库不存在'}</p>
          <Button variant="secondary" onClick={() => navigate('/knowledge')}>返回列表</Button>
        </div>
      </Layout>
    )
  }

  return (
    <Layout breadcrumb={[{ label: '知识库' }, { label: kbInfo.name }]}>
      <div className="p-8">
        <button 
          onClick={() => navigate('/knowledge')}
          className="flex items-center gap-2 text-sm text-text-tertiary hover:text-text-secondary mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          返回知识库列表
        </button>

        <div className="p-6 bg-white rounded-lg border border-border-subtle mb-6">
          <div className="flex items-start justify-between">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 rounded-lg bg-brand-50 flex items-center justify-center">
                <BookOpen className="w-6 h-6 text-brand-500" />
              </div>
              <div className="flex-1">
                <h1 className="text-lg font-semibold text-text-primary mb-1">{kbInfo.name}</h1>
                <p className="text-sm text-text-secondary mb-3">{kbInfo.description || '暂无描述'}</p>
                <div className="flex items-center gap-4 text-sm text-text-tertiary">
                  <span>{kbInfo.docCount} 个文档</span>
                  <span>· 总大小 {formatBytes(kbInfo.totalSizeBytes)}</span>
                </div>
              </div>
            </div>
            <Button variant="secondary" icon={<MoreHorizontal className="w-4 h-4" />} iconOnly />
          </div>
        </div>

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
          <p className="text-sm font-medium text-text-primary mb-1">
            {uploading ? '上传中...' : '拖拽文件上传或点击选择'}
          </p>
          <p className="text-xs text-text-tertiary mb-4">支持 PDF, Markdown, Word, Excel, 图片等格式</p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            className="hidden"
            onChange={(e) => handleUpload(e.target.files)}
          />
          <Button 
            variant="primary" 
            size="sm" 
            icon={<Upload className="w-4 h-4" />}
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
          >
            选择文件
          </Button>
        </div>

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
        </div>

        <div className="bg-white rounded-xl border border-border-subtle overflow-hidden">
          <div className="grid grid-cols-6 gap-4 px-6 py-4 bg-gray-50 border-b border-border-subtle text-sm font-medium text-text-secondary">
            <div className="col-span-2">名称</div>
            <div>类型</div>
            <div>大小</div>
            <div>状态</div>
            <div>操作</div>
          </div>

          {filteredDocs.map((doc) => {
            const status = mapIndexStatus(doc.indexStatus)
            return (
              <div 
                key={doc.id}
                className="grid grid-cols-6 gap-4 px-6 py-4 border-b border-border-subtle text-sm hover:bg-bg-hover transition-colors items-center"
              >
                <div className="col-span-2 flex items-center gap-3">
                  {getTypeIcon(doc.mimeType)}
                  <span className="font-medium text-text-primary truncate">{doc.filename}</span>
                </div>

                <div className="text-text-secondary">
                  <span className="px-2 py-0.5 bg-gray-100 rounded text-xs">{doc.mimeType?.split('/').pop() || '未知'}</span>
                </div>

                <div className="text-text-tertiary">{formatBytes(doc.fileSize)}</div>

                <div className="flex items-center gap-2">
                  {getStatusIcon(status)}
                  {getStatusBadge(status)}
                </div>

                <div className="flex gap-1">
                  <Button variant="ghost" size="sm" iconOnly icon={<Download className="w-4 h-4" />} />
                  {status === 'error' && (
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      iconOnly 
                      icon={<RefreshCw className="w-4 h-4" />}
                      onClick={() => handleReindex(doc)}
                    />
                  )}
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    iconOnly 
                    icon={<Trash2 className="w-4 h-4" />}
                    onClick={() => handleDelete(doc)}
                  />
                </div>
              </div>
            )
          })}

          {filteredDocs.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12">
              <FileText className="w-10 h-10 text-text-tertiary mb-3" />
              <p className="text-sm text-text-secondary">
                {searchQuery ? '没有找到匹配的文档' : '暂无文档'}
              </p>
            </div>
          )}
        </div>

        <div className="mt-4 flex items-center gap-4 text-sm text-text-tertiary">
          <span>显示 {filteredDocs.length} 个文档</span>
          <span>· 共 {documents.length} 个</span>
        </div>
      </div>
    </Layout>
  )
}

export default KBDocumentsPage
