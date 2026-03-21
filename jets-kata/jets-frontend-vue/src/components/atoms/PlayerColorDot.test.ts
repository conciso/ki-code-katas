// @vitest-environment jsdom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PlayerColorDot from './PlayerColorDot.vue'

describe('PlayerColorDot', () => {
  it('renders a span element', () => {
    const wrapper = mount(PlayerColorDot, { props: { color: '#ff0000' } })
    expect(wrapper.find('span').exists()).toBe(true)
  })

  it('applies the color as background style', () => {
    const wrapper = mount(PlayerColorDot, { props: { color: '#4fc3f7' } })
    expect(wrapper.find('span').attributes('style')).toContain('background: rgb(79, 195, 247)')
  })
})
