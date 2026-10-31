// Icons drawn on a 16px pixel grid to match the bitmap face.
const base = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'currentColor',
  shapeRendering: 'crispEdges' as const,
  'aria-hidden': true,
}

export const SearchIcon = () => (
  <svg {...base}>
    <path d="M5 1h5v1h2v2h1v5h-1v2h-2v1H5v-1H3V9H2V4h1V2h2zm0 2v1H4v5h1v1h5V9h1V4h-1V3z" />
    <path d="M12 11h1v1h1v1h1v2h-2v-1h-1v-1h-1v-1h1z" />
  </svg>
)

export const BackIcon = () => (
  <svg {...base}>
    <path d="M6 3h2v2H7v1h7v4H7v1h1v2H6v-1H5v-1H4V9H3V7h1V6h1V5h1z" />
  </svg>
)

export const CaretIcon = ({ down = false }: { down?: boolean }) => (
  <svg {...base} width={10} height={10} style={down ? { transform: 'scaleY(-1)' } : undefined}>
    <path d="M7 4h2v2h2v2h2v2h2v2H1v-2h2V8h2V6h2z" />
  </svg>
)

export const CloseIcon = () => (
  <svg {...base}>
    <path d="M3 3h2v1h1v1h1v1h2V5h1V4h1V3h2v2h-1v1h-1v1H9v2h1v1h1v1h1v2h-2v-1H9v-1H7v1H6v1H5v1H3v-2h1v-1h1v-1h1V9H5V7H4V6H3z" />
  </svg>
)
