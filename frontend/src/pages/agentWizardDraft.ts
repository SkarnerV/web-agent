import type { AgentCreateRequest, ToolBindingRequest } from '../api/types'

export type AgentWizardToolType = 'skill' | 'mcp' | 'kb'

export interface AgentWizardToolDraft {
  id: string
  name: string
  description?: string
  type: AgentWizardToolType
}

export interface AgentWizardDraft {
  name: string
  description: string
  icon: string
  modelId: string
  maxSteps: number
  systemPrompt: string
  tools: AgentWizardToolDraft[]
  collabMode: string
  errorStrategy: string
  visibility: 'private' | 'team' | 'public'
  version: string
  releaseNotes: string
}

const STORAGE_KEY = 'agent-platform.agent-create-draft'

export const emptyAgentWizardDraft: AgentWizardDraft = {
  name: '',
  description: '',
  icon: 'bot',
  modelId: '',
  maxSteps: 10,
  systemPrompt: '',
  tools: [],
  collabMode: 'sequential',
  errorStrategy: 'retry',
  visibility: 'private',
  version: '1.0.0',
  releaseNotes: '',
}

export function readAgentWizardDraft(): AgentWizardDraft {
  if (typeof window === 'undefined') return emptyAgentWizardDraft

  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return emptyAgentWizardDraft
    const parsed = JSON.parse(raw) as Partial<AgentWizardDraft>
    return {
      ...emptyAgentWizardDraft,
      ...parsed,
      tools: Array.isArray(parsed.tools) ? parsed.tools : [],
    }
  } catch {
    return emptyAgentWizardDraft
  }
}

export function saveAgentWizardDraft(partial: Partial<AgentWizardDraft>): AgentWizardDraft {
  const next = {
    ...readAgentWizardDraft(),
    ...partial,
  }

  if (typeof window !== 'undefined') {
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  }

  return next
}

export function clearAgentWizardDraft() {
  if (typeof window !== 'undefined') {
    window.sessionStorage.removeItem(STORAGE_KEY)
  }
}

export function toAgentCreateRequest(draft: AgentWizardDraft): AgentCreateRequest {
  const toolBindings = draft.tools
    .map<ToolBindingRequest | null>((tool) => {
      if (tool.type === 'mcp') {
        return {
          sourceType: 'mcp',
          sourceId: tool.id,
          toolName: tool.name,
          enabled: true,
        }
      }
      return null
    })
    .filter((tool): tool is ToolBindingRequest => tool !== null)

  const skillIds = draft.tools
    .filter((tool) => tool.type === 'skill')
    .map((tool) => tool.id)

  const knowledgeBaseIds = draft.tools
    .filter((tool) => tool.type === 'kb')
    .map((tool) => tool.id)

  return {
    name: draft.name.trim(),
    description: draft.description.trim() || undefined,
    avatar: draft.icon || undefined,
    modelId: draft.modelId || undefined,
    systemPrompt: draft.systemPrompt.trim() || undefined,
    maxSteps: draft.maxSteps,
    toolBindings: toolBindings.length > 0 ? toolBindings : undefined,
    skillIds: skillIds.length > 0 ? skillIds : undefined,
    knowledgeBaseIds: knowledgeBaseIds.length > 0 ? knowledgeBaseIds : undefined,
  }
}
