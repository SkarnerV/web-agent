import { agentHandlers } from './agent'
import { chatHandlers } from './chat'

export const handlers = [...agentHandlers, ...chatHandlers]
