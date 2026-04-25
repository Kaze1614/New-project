import { describe, expect, it } from 'vitest'
import fs from 'node:fs'
import path from 'node:path'

const styles = fs.readFileSync(path.resolve(__dirname, '../styles.css'), 'utf8')

describe('admin button styles', () => {
  it('keeps pill radius and hover shadows for shared action buttons', () => {
    expect(styles).toContain('.primary-btn')
    expect(styles).toContain('.outline-btn')
    expect(styles).toContain('.danger-btn')
    expect(styles).toContain('border-radius: 24px;')
    expect(styles).toContain('.outline-btn:hover:not(:disabled)')
    expect(styles).toContain('.danger-btn:hover:not(:disabled)')
    expect(styles).toContain('box-shadow: 0 10px 18px rgba(37, 99, 235, 0.24);')
  })
})
