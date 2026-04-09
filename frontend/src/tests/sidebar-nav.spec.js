import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SidebarNav from '../components/SidebarNav.vue'

describe('SidebarNav', () => {
  it('renders core menu labels', () => {
    const wrapper = mount(SidebarNav, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('错题本')
    expect(wrapper.text()).toContain('智能答疑')
  })
})
