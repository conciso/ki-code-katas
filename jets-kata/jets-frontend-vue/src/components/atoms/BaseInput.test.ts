// @vitest-environment jsdom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseInput from './BaseInput.vue'

describe('BaseInput', () => {
  it('renders an input element', () => {
    const wrapper = mount(BaseInput, { props: { modelValue: '' } })
    expect(wrapper.find('input').exists()).toBe(true)
  })

  it('displays the current modelValue', () => {
    const wrapper = mount(BaseInput, { props: { modelValue: 'Hans' } })
    expect((wrapper.find('input').element as HTMLInputElement).value).toBe('Hans')
  })

  it('emits update:modelValue on input', async () => {
    const wrapper = mount(BaseInput, { props: { modelValue: '' } })
    await wrapper.find('input').setValue('Neuer Wert')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['Neuer Wert'])
  })

  it('renders label when prop is set', () => {
    const wrapper = mount(BaseInput, { props: { modelValue: '', label: 'Name', id: 'name' } })
    expect(wrapper.find('label').text()).toBe('Name')
  })

  it('does not render label when prop is omitted', () => {
    const wrapper = mount(BaseInput, { props: { modelValue: '' } })
    expect(wrapper.find('label').exists()).toBe(false)
  })

  it('associates label with input via id', () => {
    const wrapper = mount(BaseInput, { props: { modelValue: '', label: 'Name', id: 'name' } })
    expect(wrapper.find('label').attributes('for')).toBe('name')
    expect(wrapper.find('input').attributes('id')).toBe('name')
  })
})
