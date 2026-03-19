import { useEffect, useRef, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { CButton } from '@coreui/react'

type Message = {
  id: string
  role: 'user' | 'assistant'
  content: string
}

type StoreAiChatProps = Readonly<{
  streamConversation: (
    query: string,
    conversationId: string | null,
    onChunk: (chunk: string) => void,
    onConversationId?: (id: string) => void,
  ) => Promise<void>
}>

const newId = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`

export function StoreAiChat({ streamConversation }: StoreAiChatProps) {
  const [open, setOpen] = useState(false)
  const [prompt, setPrompt] = useState('')
  const [conversationId, setConversationId] = useState<string | null>(null)
  const [isStreaming, setIsStreaming] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  const [error, setError] = useState<string | null>(null)
  const bottomRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, open, isStreaming])

  const appendAssistantChunk = (assistantId: string, chunk: string) => {
    setMessages((prev) =>
      prev.map((msg) =>
        msg.id === assistantId
          ? { ...msg, content: `${msg.content}${chunk}` }
          : msg,
      ),
    )
  }

  const handleSubmit = async (event?: FormEvent) => {
    event?.preventDefault()
    const query = prompt.trim()
    if (!query || isStreaming) return

    const userMessage: Message = { id: newId(), role: 'user', content: query }
    const assistantMessage: Message = { id: newId(), role: 'assistant', content: '' }
    setPrompt('')
    setError(null)
    setMessages((prev) => [...prev, userMessage, assistantMessage])
    setIsStreaming(true)

    try {
      await streamConversation(
        query,
        conversationId,
        (chunk) => appendAssistantChunk(assistantMessage.id, chunk),
        (id) => setConversationId(id),
      )
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === assistantMessage.id && !msg.content.trim()
            ? { ...msg, content: 'No answer generated.' }
            : msg,
        ),
      )
    } catch (e) {
      setError((e as Error).message || 'Streaming failed')
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === assistantMessage.id && !msg.content.trim()
            ? { ...msg, content: 'I could not generate an answer.' }
            : msg,
        ),
      )
    } finally {
      setIsStreaming(false)
    }
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      void handleSubmit()
    }
  }

  const handleReset = () => {
    setMessages([])
    setConversationId(null)
    setError(null)
  }

  return (
    <div className={`mbyte-ai-chat ${open ? 'mbyte-ai-chat--open' : ''}`}>
      {!open ? (
        <button
          type="button"
          className="mbyte-ai-chat__fab"
          onClick={() => setOpen(true)}
          aria-label="Open AI chat"
        >
          <span role="img" aria-hidden="true">🤖</span>
        </button>
      ) : (
        <div className="mbyte-ai-chat__panel">
          <div className="mbyte-ai-chat__header">
            <div className="mbyte-ai-chat__title">🤖 AI Search</div>
            <div className="d-flex gap-2">
              <CButton size="sm" color="light" onClick={handleReset} disabled={isStreaming}>Reset</CButton>
              <CButton size="sm" color="light" onClick={() => setOpen(false)} disabled={isStreaming}>✕</CButton>
            </div>
          </div>

          <div className="mbyte-ai-chat__messages">
            {!messages.length && (
              <div className="mbyte-ai-chat__empty">Ask questions about your uploaded files.</div>
            )}
            {messages.map((message) => (
              <div
                key={message.id}
                className={`mbyte-ai-chat__message mbyte-ai-chat__message--${message.role}`}
              >
                {message.content}
              </div>
            ))}
            {isStreaming && <div className="mbyte-ai-chat__typing">Thinking…</div>}
            {error && <div className="mbyte-ai-chat__error">{error}</div>}
            <div ref={bottomRef} />
          </div>

          <form className="mbyte-ai-chat__input" onSubmit={handleSubmit}>
            <textarea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about this store..."
              rows={2}
              disabled={isStreaming}
            />
            <CButton type="submit" color="primary" disabled={isStreaming || !prompt.trim()}>
              Send
            </CButton>
          </form>
        </div>
      )}
    </div>
  )
}
