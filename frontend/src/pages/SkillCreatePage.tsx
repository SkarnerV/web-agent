import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Play, Wand, Code, CheckCircle } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { Input } from '../components/ui/Input'

// SkillCreatePage Component
const SkillCreatePage: React.FC = () => {
  const navigate = useNavigate()
  const [skillName, setSkillName] = useState('')
  const [description, setDescription] = useState('')
  const [triggerCondition, setTriggerCondition] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [isTesting, setIsTesting] = useState(false)
  const [testResult, setTestResult] = useState<string | null>(null)

  const handleSave = () => {
    if (!skillName) return
    setIsSaving(true)
    // Simulate save operation
    setTimeout(() => {
      setIsSaving(false)
      navigate('/skills')
    }, 500)
  }

  const handleTest = () => {
    if (!skillName) return
    setIsTesting(true)
    setTestResult(null)
    // Simulate test operation
    setTimeout(() => {
      setIsTesting(false)
      setTestResult('测试通过！Skill 执行成功。')
    }, 1000)
  }

  return (
    <Layout breadcrumb={[{ label: 'Skill' }, { label: '创建 Skill' }]}>
      {/* TopBar */}
      <div className="h-14 bg-white border-b border-border-subtle flex items-center justify-between px-6">
        <Input
          placeholder="Skill 名称"
          value={skillName}
          onChange={setSkillName}
          className="w-[300px]"
        />
        <Button variant="primary" disabled={!skillName || isSaving} onClick={handleSave}>
          {isSaving ? '保存中...' : '保存'}
        </Button>
      </div>

      {/* Body */}
      <div className="flex gap-6 p-6">
        {/* Left: Form Column */}
        <div className="flex-1 flex flex-col gap-6">
          {/* Skill 名称 */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-text-primary">Skill 名称</label>
            <Input
              placeholder="输入 Skill 名称"
              value={skillName}
              onChange={setSkillName}
            />
          </div>

          {/* 描述 */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-text-primary">描述</label>
            <textarea
              placeholder="描述这个 Skill 的用途和行为..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full h-[100px] px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary placeholder:text-text-tertiary resize-none focus:outline-none focus:border-brand-500"
            />
          </div>

          {/* 触发条件配置 */}
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

          {/* 代码编辑器占位 */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-text-primary">Skill 代码</label>
            <div className="w-full h-[300px] bg-gray-900 rounded-lg border border-border-subtle flex flex-col overflow-hidden">
              {/* Editor Header */}
              <div className="h-10 bg-gray-800 border-b border-gray-700 flex items-center px-4 gap-2">
                <div className="flex gap-1.5">
                  <div className="w-3 h-3 rounded-full bg-red-500" />
                  <div className="w-3 h-3 rounded-full bg-yellow-500" />
                  <div className="w-3 h-3 rounded-full bg-green-500" />
                </div>
                <span className="text-xs text-gray-400 ml-2">skill.js</span>
              </div>
              {/* Editor Body */}
              <div className="flex-1 p-4 font-mono text-sm">
                <div className="text-gray-500">// 在此处编写 Skill 代码</div>
                <div className="text-brand-400 mt-2">export async function</div>
                <div className="text-yellow-400 ml-4">execute</div>
                <div className="text-gray-300">(context) {`{`}</div>
                <div className="text-gray-500 ml-6">// 你的实现代码</div>
                <div className="text-gray-300">{`}`}</div>
              </div>
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
          {/* Preview Card */}
          <div className="p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3">
            <div className="text-sm font-medium text-text-secondary mb-2">预览</div>
            
            {/* Preview Header Row */}
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

            {/* Preview Description */}
            <p className="text-sm text-text-secondary leading-relaxed">
              {description || '暂无描述'}
            </p>

            {/* Preview Meta */}
            <div className="flex items-center gap-3 text-sm text-text-tertiary">
              <span>触发条件: {triggerCondition || '未设置'}</span>
            </div>
          </div>

          {/* Test Button */}
          <Button 
            variant="secondary" 
            icon={<Play className="w-4 h-4" />}
            className="w-full"
            disabled={!skillName || isTesting}
            onClick={handleTest}
          >
            {isTesting ? '测试中...' : '测试 Skill'}
          </Button>

          {/* Test Result */}
          {testResult && (
            <div className="p-4 bg-success-50 rounded-lg border border-success-500 flex items-center gap-2">
              <CheckCircle className="w-5 h-5 text-success-500" />
              <span className="text-sm text-success-500">{testResult}</span>
            </div>
          )}

          {/* Tips */}
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