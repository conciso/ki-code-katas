// @vitest-environment jsdom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HpHearts from './HpHearts.vue'

describe('HpHearts', () => {
  it('renders the correct number of hearts', () => {
    const wrapper = mount(HpHearts, { props: { hp: 3 } })
    expect(wrapper.text()).toBe('♥♥♥')
  })

  it('renders no hearts when hp is 0', () => {
    const wrapper = mount(HpHearts, { props: { hp: 0 } })
    expect(wrapper.text()).toBe('')
  })

  it('renders one heart when hp is 1', () => {
    const wrapper = mount(HpHearts, { props: { hp: 1 } })
    expect(wrapper.text()).toBe('♥')
  })
})
