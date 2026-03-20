import { useEffect, useRef } from 'react'
import { autocomplete } from '@algolia/autocomplete-js'
import type SearchResult from '../../api/entities/SearchResult'

type AutocompleteSearchProps = Readonly<{
  search: (query: string) => Promise<SearchResult[]>
  onSelect: (id: string) => void
  placeholder: string
  noResultsLabel: string
}>

type AutocompleteItem = {
  type: string
  identifier: string
  explain: string
  value: unknown
  [key: string]: unknown
}

const HIGHLIGHT_SPAN_START = "<span class='highlighted'>"
const HIGHLIGHT_SPAN_START_DOUBLE_QUOTES = '<span class="highlighted">'
const HIGHLIGHT_SPAN_END = '</span>'
const HIGHLIGHT_MARK_START = '<mark>'
const HIGHLIGHT_MARK_END = '</mark>'

type HighlightSegment = {
  text: string
  highlighted: boolean
  className?: string
}

const toHighlightSegments = (value: string): HighlightSegment[] => {
  if (!value) return []
  const normalized = value
    .replaceAll(HIGHLIGHT_SPAN_START_DOUBLE_QUOTES, HIGHLIGHT_SPAN_START)
  const result: HighlightSegment[] = []
  let index = 0
  while (index < normalized.length) {
    const spanStart = normalized.indexOf(HIGHLIGHT_SPAN_START, index)
    const markStart = normalized.indexOf(HIGHLIGHT_MARK_START, index)
    let start = -1
    let startToken = ''
    let endToken = ''
    let className: string | undefined
    if (spanStart !== -1 && (markStart === -1 || spanStart < markStart)) {
      start = spanStart
      startToken = HIGHLIGHT_SPAN_START
      endToken = HIGHLIGHT_SPAN_END
      className = 'highlighted'
    } else if (markStart !== -1) {
      start = markStart
      startToken = HIGHLIGHT_MARK_START
      endToken = HIGHLIGHT_MARK_END
    }
    if (start === -1) {
      result.push({ text: normalized.slice(index), highlighted: false })
      break
    }
    if (start > index) {
      result.push({ text: normalized.slice(index, start), highlighted: false })
    }
    const end = normalized.indexOf(endToken, start + startToken.length)
    if (end === -1) {
      result.push({ text: normalized.slice(start), highlighted: false })
      break
    }
    const text = normalized.slice(start + startToken.length, end)
    result.push({ text, highlighted: true, className })
    index = end + endToken.length
  }
  return result
}

export function AutocompleteSearch({ search, onSelect, placeholder, noResultsLabel }: AutocompleteSearchProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!containerRef.current) return

    const instance = autocomplete<AutocompleteItem>({
      container: containerRef.current,
      placeholder,
      openOnFocus: true,
      detachedMediaQuery: '',
      getSources({ query }) {
        const trimmed = query.trim()
        if (!trimmed) return []
        return [
          {
            sourceId: 'store-search',
            async getItems() {
              const results = await search(trimmed).catch(() => [])
              return results.map((item) => ({
                type: item.type,
                identifier: item.identifier,
                explain: item.explain,
                value: item.value,
              }))
            },
            onSelect({ item, setQuery, setIsOpen }) {
              setQuery('')
              setIsOpen(false)
              onSelect(item.identifier)
              },
              templates: {
              item({ item, html }) {
                const title = item.identifier ?? ''
                const meta = item.type ?? ''
                const explainSegments = toHighlightSegments(item.explain ?? '')
                return html`
                  <div class="mbyte-search-item">
                    <div class="mbyte-search-item__header">
                      <div class="mbyte-search-item__title" title="${title}">${title}</div>
                      ${meta ? html`<div class="mbyte-search-item__meta">${meta}</div>` : ''}
                    </div>
                    ${explainSegments.length
                      ? html`<div class="mbyte-search-item__snippet">
                          ${explainSegments.map((segment) =>
                            segment.highlighted
                              ? html`<span class="${segment.className ?? ''}">${segment.text}</span>`
                              : segment.text,
                          )}
                        </div>`
                      : ''}
                  </div>
                `
              },
              noResults({ html }) {
                return html`<div class="mbyte-search-empty">${noResultsLabel}</div>`
              },
            },
          },
        ]
      },
    })

    return () => {
      instance.destroy()
    }
  }, [onSelect, placeholder, search])

  return <div className="mbyte-search" ref={containerRef} />
}
