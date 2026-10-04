import { test, expect, Page } from '@playwright/test'

const BASE_URL = 'http://127.0.0.1:5174'
const VALID_USERNAME = 'demo'
const VALID_PASSWORD = 'demo123'

/** 打开登录页面 */
async function gotoLogin(page: Page) {
  await page.goto(BASE_URL)
  await page.waitForLoadState('domcontentloaded')
  await expect(page.getByTestId('login-username')).toBeVisible()
}

/** 执行登录操作 */
async function doLogin(page: Page, username: string, password: string) {
  await page.getByTestId('login-username').fill(username)
  await page.getByTestId('login-password').fill(password)
  await page.getByTestId('login-submit-btn').click()
}

/** 断言登录成功 */
async function expectLoginSuccess(page: Page, username: string) {
  await expect(page.getByTestId('welcome-user')).toBeVisible()
  await expect(page.getByTestId('welcome-user')).toContainText(username)
  await expect(page.getByTestId('logout-btn')).toBeVisible()
}

/** 断言登录失败（显示通用错误） */
async function expectLoginFailure(page: Page) {
  await expect(page.getByTestId('login-general-error')).toBeVisible()
}

// ========== 测试用例 ==========

test.describe('用户登录功能', () => {

  test('用户名和密码正确 - 登录成功', async ({ page }) => {
    await gotoLogin(page)
    await doLogin(page, VALID_USERNAME, VALID_PASSWORD)
    await expectLoginSuccess(page, VALID_USERNAME)
  })

  test('用户名错误 - 登录失败', async ({ page }) => {
    await gotoLogin(page)
    await doLogin(page, 'wronguser', VALID_PASSWORD)
    await expectLoginFailure(page)
  })

  test('密码错误 - 登录失败', async ({ page }) => {
    await gotoLogin(page)
    await doLogin(page, VALID_USERNAME, 'wrongpass')
    await expectLoginFailure(page)
  })

  test('用户名为空 - 显示提示', async ({ page }) => {
    await gotoLogin(page)
    await doLogin(page, '', VALID_PASSWORD)
    await expect(page.getByTestId('login-username-error')).toBeVisible()
    await expect(page.getByTestId('login-username-error')).toContainText('请输入用户名')
  })

  test('密码为空 - 显示提示', async ({ page }) => {
    await gotoLogin(page)
    await doLogin(page, VALID_USERNAME, '')
    await expect(page.getByTestId('login-password-error')).toBeVisible()
    await expect(page.getByTestId('login-password-error')).toContainText('请输入密码')
  })

  test('用户名和密码均为空 - 显示提示', async ({ page }) => {
    await gotoLogin(page)
    await doLogin(page, '', '')
    await expect(page.getByTestId('login-username-error')).toBeVisible()
    await expect(page.getByTestId('login-password-error')).toBeVisible()
  })

  test('点击立即注册 - 跳转到注册页', async ({ page }) => {
    await gotoLogin(page)
    await page.getByTestId('go-to-register').click()
    await expect(page.getByTestId('register-username')).toBeVisible()
    await expect(page.getByTestId('register-submit-btn')).toBeVisible()
  })

  test('登录成功后退出登录 - 返回登录页', async ({ page }) => {
    await gotoLogin(page)
    await doLogin(page, VALID_USERNAME, VALID_PASSWORD)
    await expectLoginSuccess(page, VALID_USERNAME)
    await page.getByTestId('logout-btn').click()
    await expect(page.getByTestId('login-username')).toBeVisible()
    await expect(page.getByTestId('login-submit-btn')).toBeVisible()
  })
})
