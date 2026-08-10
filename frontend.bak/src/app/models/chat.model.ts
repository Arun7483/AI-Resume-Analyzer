export type ChatRole = 'user' | 'assistant';
export interface ChatMessage { id: string; role: ChatRole; content: string; createdAt: string; status?: 'sending' | 'sent' | 'error'; }
export interface ChatRequest { resumeId: string; message: string; conversationId?: string; }
export interface ChatResponse { conversationId: string; message: ChatMessage; suggestedActions?: string[]; }
