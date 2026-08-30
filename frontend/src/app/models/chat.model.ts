export type ChatRole = 'user' | 'assistant';
export interface ChatMessage { id: string; role: ChatRole; content: string; createdAt: string; status?: 'sending' | 'sent' | 'error'; }
export interface ChatRequest { resumeId: number | null; message: string; conversationId?: string; }
export interface ChatResponse { messageId: number; content: string; timestamp: string; }
