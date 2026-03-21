// @vitest-environment jsdom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseButton from './BaseButton.vue'

describe('BaseButton', () => {
  it('renders slot content', () => {
    const wrapper = mount(BaseButton, { slots: { default: 'Klick mich' } })
    expect(wrapper.text()).toBe('Klick mich')
  })

  it('renders a button element', () => {
    const wrapper = mount(BaseButton)
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('forwards disabled attribute', () => {
    const wrapper = mount(BaseButton, { attrs: { disabled: true } })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('emits click when not disabled', async () => {
    const wrapper = mount(BaseButton)
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mount(BaseButton, { attrs: { disabled: true } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })
})
