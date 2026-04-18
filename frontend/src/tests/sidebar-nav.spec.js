import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import SidebarNav from '../components/SidebarNav.vue'

describe('SidebarNav', () => {
  it('renders five router navigation items including chapters page link', () => {
    const wrapper = mount(SidebarNav, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :data-to="to"><slot /></a>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('数学园')
    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('错题本')
    expect(wrapper.text()).toContain('收藏本')
    expect(wrapper.text()).toContain('待复习')
    expect(wrapper.text()).toContain('章节目录')
    expect(wrapper.find('[data-to="/chapters"]').exists()).toBe(true)
    expect(wrapper.find('.chapter-flyout').exists()).toBe(false)
  })
})
