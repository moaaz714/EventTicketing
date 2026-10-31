import type { ElementType } from 'react'

type PixelSize = 24 | 32 | 48 | 64 | 96

interface Props {
  text: string
  size: PixelSize
  as?: ElementType
  className?: string
  /** Past this length the pixel face stops reading well; fall back to grotesk. */
  maxChars?: number
  /** Settled content prints smooth, like everything else that has resolved. */
  smooth?: boolean
}

const ARABIC_RANGES = [
  [0x0600, 0x06ff],
  [0x0750, 0x077f],
  [0xfb50, 0xfdff],
  [0xfe70, 0xfefc],
]

const hasArabic = (text: string) =>
  [...text].some((ch) => {
    const c = ch.codePointAt(0) ?? 0
    return ARABIC_RANGES.some(([lo, hi]) => c >= lo && c <= hi)
  })

/**
 * Display text in the bitmap face, snapped to whole-pixel sizes. Long titles
 * and scripts the pixel face has no glyphs for render in the text face.
 */
export function PixelTitle({ text, size, as: Tag = 'h1', className = '', maxChars = 28, smooth = false }: Props) {
  const pixel = !smooth && text.length <= maxChars && !hasArabic(text)
  return (
    <Tag
      className={`title title-${size} ${pixel ? 'pixel' : 'title-smooth'} ${className}`}
      dir={hasArabic(text) ? 'auto' : undefined}
    >
      {text}
    </Tag>
  )
}
