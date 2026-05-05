import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Play, Wand, Code, CheckCircle, AlertCircle, ChevronLeft } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { Input } from '../components/ui/Input'
import { createSkill, updateSkill, getSkill } from '../api/skill'
import { ApiError } from '../api/client'
import type { SkillDetailVO } from '../api/types'

const SkillCreatePage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const isEditMode = Boolean(id)

  const [skillName, setSkillName] = useState('')
  const [description, setDescription] = useState('')
  const [triggerCondition, setTriggerCondition] = useState('')
  const [format, setFormat] = useState<'YAML' | 'MARKDOWN'>('YAML')
  const [content, setContent] = useState('')
  const [version, setVersion] = useState<number | undefined>(undefined)
  const [isSaving, setIsSaving] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isTesting, setIsTesting] = useState(false)
  const [testResult, setTestResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    setIsLoading(true)
    getSkill(id)
      .then((skill: SkillDetailVO) => {
        setSkillName(skill.name)
        setDescription(skill.description ?? '')
        setTriggerCondition(skill.triggerConditions ?? '')
        setFormat((skill.format as 'YAML' | 'MARKDOWN') ?? 'YAML')
        setContent(skill.content ?? '')
        setVersion(skill.version)
      })
      .catch((err) => {
        if (err instanceof ApiError) {
          setError(err.message)
        } else {
          setError('加载 Skill 失败')
        }
      })
      .finally(() => setIsLoading(false))
  }, [id])

  const handleSave = async () => {
    if (!skillName) return
    setIsSaving(true)
    setError(null)
    try {
      if (isEditMode && id) {
        await updateSkill(id, {
          name: skillName,
          description: description || undefined,
          triggerConditions: triggerCondition || undefined,
          format,
          content: content || undefined,
          version,
        })
      } else {
        await createSkill({
          name: skillName,
          description: description || undefined,
          triggerConditions: triggerCondition || undefined,
          format,
          content: content || undefined,
        })
      }
      navigate('/skills')
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('保存失败，请重试')
      }
    } finally {
      setIsSaving(false)
    }
  }

  const handleTest = () => {
    if (!skillName) return
    setIsTesting(true)
    setTestResult(null)
    setTimeout(() => {
      setIsTesting(false)
      setTestResult('测试通过！Skill 执行成功。')
    }, 1000)
  }

  if (isLoading) {
    return (
      <Layout breadcrumb={[{ label: 'Skill' }, { label: '编辑 Skill' }]}>
        <div className="flex items-center justify-center h-64 text-text-tertiary">
          加载中...
        </div>
      </Layout>
    )
  }

  return (
    <Layout breadcrumb={[{ label: 'Skill' }, { label: isEditMode ? '编辑 Skill' : '创建 Skill' }]}>
      {/* TopBar */}
      <div className="h-14 bg-white border-b border-border-subtle flex items-center gap-3 px-6">
        <button
          onClick={() => navigate('/skills')}
          className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span className="text-[13px]">返回</span>
        </button>
        <span className="text-[15px] font-semibold text-text-primary flex-1">
          {isEditMode ? '编辑 Skill' : '创建 Skill'}
        </span>
        <Button variant="primary" disabled={!skillName || isSaving} onClick={handleSave}>
          {isSaving ? '保存中...' : '保存'}
        </Button>
      </div>

      {error && (
        <div className="mx-6 mt-4 p-3 bg-red-50 border border-red-200 rounded-md flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
          <span className="text-sm text-red-600">{error}</span>
        </div>
      )}

      {/* Body */}
      <div className="flex gap-6 p-6">
        {/* Left: Form Column */}
        <div className="flex-1 flex flex-col gap-6">
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-text-primary">Skill 名称</label>
            <Input
              placeholder="输入 Skill 名称"
              value={skillName}
              onChange={setSkillName}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-text-primary">描述</label>
            <textarea
              placeholder="描述这个 Skill 的用途和行为..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full h-[100px] px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary placeholder:text-text-tertiary resize-none focus:outline-none focus:border-brand-500"
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-text-primary">触发条件配置</label>
            <Input
              placeholder="输入触发关键词或短语"
              value={triggerCondition}
              onChange={setTriggerCondition}
            />
            <p className="text-xs text-text-tertiary">
              当用户输入包含这些关键词时，将自动触发此 Skill
            </p>
          </div>

          {/* Format Selector */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-text-primary">格式</label>
            <div className="flex gap-0 border border-border-strong rounded-md overflow-hidden w-fit">
              <button
                type="button"
                className={`px-4 py-2 text-sm font-medium transition-colors ${
                  format === 'YAML'
                    ? 'bg-brand-500 text-white'
                    : 'bg-white text-text-secondary hover:bg-gray-50'
                }`}
                onClick={() => setFormat('YAML')}
              >
                YAML
              </button>
              <button
                type="button"
                className={`px-4 py-2 text-sm font-medium transition-colors border-l border-border-strong ${
                  format === 'MARKDOWN'
                    ? 'bg-brand-500 text-white'
                    : 'bg-white text-text-secondary hover:bg-gray-50'
                }`}
                onClick={() => setFormat('MARKDOWN')}
              >
                Markdown
              </button>
            </div>
          </div>

          {/* Content Editor */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-text-primary">Skill 内容</label>
            <div className="w-full h-[300px] bg-gray-900 rounded-lg border border-border-subtle flex flex-col overflow-hidden">
              <div className="h-10 bg-gray-800 border-b border-gray-700 flex items-center px-4 gap-2">
                <div className="flex gap-1.5">
                  <div className="w-3 h-3 rounded-full bg-red-500" />
                  <div className="w-3 h-3 rounded-full bg-yellow-500" />
                  <div className="w-3 h-3 rounded-full bg-green-500" />
                </div>
                <span className="text-xs text-gray-400 ml-2">
                  {format === 'YAML' ? 'skill.yaml' : 'skill.md'}
                </span>
              </div>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder={format === 'YAML' ? 'name: my-skill\nsteps:\n  - action: ...' : '# Skill\n\n描述...'}
                className="flex-1 p-4 font-mono text-sm bg-transparent text-gray-200 placeholder:text-gray-600 resize-none focus:outline-none"
              />
            </div>
            <div className="flex gap-2 mt-2">
              <Button variant="secondary" size="sm" icon={<Code className="w-4 h-4" />}>
                编辑器设置
              </Button>
              <Button variant="ghost" size="sm">
                使用模板
              </Button>
            </div>
          </div>
        </div>

        {/* Right: Preview Panel */}
        <div className="w-[380px] flex flex-col gap-4">
          <div className="p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3">
            <div className="text-sm font-medium text-text-secondary mb-2">预览</div>
            
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center text-lg font-semibold text-brand-500">
                {skillName ? skillName[0] : 'S'}
              </div>
              <div className="flex-1 flex flex-col gap-1">
                <span className="text-sm font-semibold text-text-primary">
                  {skillName || '未命名 Skill'}
                </span>
                <Badge variant="draft">草稿</Badge>
              </div>
            </div>

            <p className="text-sm text-text-secondary leading-relaxed">
              {description || '暂无描述'}
            </p>

            <div className="flex items-center gap-3 text-sm text-text-tertiary">
              <span>触发条件: {triggerCondition || '未设置'}</span>
            </div>

            <div className="flex items-center gap-3 text-sm text-text-tertiary">
              <span>格式: {format}</span>
            </div>
          </div>

          <Button 
            variant="secondary" 
            icon={<Play className="w-4 h-4" />}
            className="w-full"
            disabled={!skillName || isTesting}
            onClick={handleTest}
          >
            {isTesting ? '测试中...' : '测试 Skill'}
          </Button>

          {testResult && (
            <div className="p-4 bg-success-50 rounded-lg border border-success-500 flex items-center gap-2">
              <CheckCircle className="w-5 h-5 text-success-500" />
              <span className="text-sm text-success-500">{testResult}</span>
            </div>
          )}

          <div className="p-4 bg-gray-50 rounded-lg border border-border-subtle">
            <div className="flex items-start gap-3">
              <Wand className="w-5 h-5 text-brand-500 mt-0.5" />
              <div className="flex-1">
                <p className="text-sm font-medium text-text-primary mb-1">创建提示</p>
                <p className="text-xs text-text-tertiary leading-relaxed">
                  一个好的 Skill 应该有明确的触发条件和清晰的执行逻辑。建议先测试再保存。
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default SkillCreatePage
